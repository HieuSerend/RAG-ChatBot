import os
import uuid
import json
import time
import requests
import fitz  # PyMuPDF
from typing import List, Dict, Tuple
from dotenv import load_dotenv
from sqlalchemy import create_engine, text
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_core.documents import Document
from langchain_core.embeddings import Embeddings
from langchain_postgres import PGVector

# --- 1. CẤU HÌNH ---
load_dotenv()

# Tên file PDF của bạn
INPUT_PDF_PATH = "glossary.pdf" 
START_PAGE = 12  # Index trang (Trang 11 là index 10)
END_PAGE = 603  

# Cấu hình cứng Source theo yêu cầu
FIXED_SOURCE_NAME = "OECD Glossary of Statistical Terms"

COLAB_API_URL = os.getenv("COLAB_API_URL", "https://unapprovable-bryon-subpeltately.ngrok-free.dev/embed_batch")
DB_CONNECTION = os.getenv("CONNECTION_STRING", "postgresql+psycopg://postgres:password@localhost:5433/rag_chatbot")
DB_COLLECTION_NAME = os.getenv("COLLECTION_NAME", "gemini_knowledge_base")

engine = create_engine(DB_CONNECTION)

# --- 2. EMBEDDING CLASS (GIỮ NGUYÊN) ---
class ColabEmbeddings(Embeddings):
    def __init__(self, api_url: str):
        self.api_url = api_url

    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        try:
            # Timeout cao hơn cho an toàn
            response = requests.post(self.api_url, json={"texts": texts}, timeout=120)
            if response.status_code == 200:
                return response.json()['embeddings']
            else:
                print(f"⚠️ API Error: {response.text}")
                return []
        except Exception as e:
            print(f"❌ Lỗi API: {e}")
            return []

    def embed_query(self, text: str) -> List[float]:
        return self.embed_documents([text])[0]

def init_db():
    create_table_sql = """
    CREATE TABLE IF NOT EXISTS doc_parents (
        parent_id TEXT PRIMARY KEY,
        content TEXT,
        metadata JSONB,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
    """
    with engine.connect() as conn:
        conn.execute(text(create_table_sql))
        conn.commit()
    print("✅ Đã kết nối Database.")

# --- 3. XỬ LÝ PDF (TỐI GIẢN) ---
# ... (Giữ nguyên import và config DB/API) ...

# --- CẤU HÌNH LOGIC NHẬN DIỆN ---
# Bạn hãy điền con số bạn soi được ở Bước 1 vào đây.
# Ví dụ: Chữ thường size 9, Chữ Term size 11 -> Thì đặt ngưỡng là 10.0
TERM_THRESHOLD_SIZE = 8.0 

def is_real_term(span) -> bool:
    """
    Logic mới:
    1. Check Size: Phải lớn hơn ngưỡng quy định.
    2. Check Font: Vẫn nên check Bold để chắc chắn (hoặc bỏ nếu PDF này Term không bold).
    3. Check Header Rác: Loại bỏ các chữ cái cái to đùng (A, B, C...) đầu mục lục.
    """
    text = span["text"].strip()
    size = span["size"]
    font_name = span["font"].lower()
    
    # 1. Điều kiện tiên quyết: SIZE PHẢI TO
    if size <= TERM_THRESHOLD_SIZE:
        return False
        
    # 2. Loại bỏ Header Mục lục (Chữ A, B, C... to đùng đứng một mình)
    # Thường mấy chữ cái đầu mục lục size rất to (ví dụ > 20)
    if size > 20:
        return False
    if len(text) == 1 and text.isupper(): # Bỏ qua chữ cái đơn lẻ kiểu "A", "B"
        return False

    # 3. (Tùy chọn) Vẫn check Bold cho chắc ăn, tránh trường hợp text thường bị lỗi font size
    is_bold = "bold" in font_name or (span["flags"] & 16)
    if not is_bold:
        return False

    # Nếu thỏa mãn size to + bold -> Là Term xịn
    return True

def parse_pdf_data(pdf_path: str, start: int, end: int) -> List[Dict]:
    doc = fitz.open(pdf_path)
    extracted_data = []
    
    current_term = None
    current_def_parts = []
    
    print(f"📖 Đang quét PDF (Theo Size > {TERM_THRESHOLD_SIZE}) từ trang {start+1} -> {end}...")
    end = min(end, len(doc))
    
    for page_num in range(start, end):
        page = doc[page_num]
        blocks = page.get_text("dict", sort=True)["blocks"]

        for block in blocks:
            if "lines" not in block: continue
            
            for line in block["lines"]:
                # Gom text dòng để xử lý (tránh PDF tách ký tự)
                line_text = " ".join([s["text"] for s in line["spans"]]).strip()
                
                for span in line["spans"]:
                    text = span["text"].strip()
                    if not text: continue
                    
                    # --- LOGIC MỚI ---
                    if is_real_term(span):
                        # 1. Lưu Term cũ
                        if current_term:
                            full_def = " ".join(current_def_parts).strip()
                            if full_def:
                                extracted_data.append({
                                    "term": current_term,
                                    "definition": full_def
                                })
                        
                        # 2. Bắt đầu Term mới
                        current_term = text
                        current_def_parts = []
                    
                    else:
                        # Nội dung Definition (Bao gồm cả Source, Context... vì chúng size nhỏ)
                        # Code này sẽ gom hết "Source: ABC" vào làm một phần của definition luôn
                        # đúng như ý bạn muốn "term: ..., definition: ... (kèm source)"
                        if current_term:
                            current_def_parts.append(text)
        
        if (page_num + 1) % 50 == 0:
            print(f"   -> Xong trang {page_num + 1}")

    if current_term and current_def_parts:
        extracted_data.append({
            "term": current_term,
            "definition": " ".join(current_def_parts).strip()
        })
        
    return extracted_data

def prepare_documents(raw_data: List[Dict]) -> Tuple[List[Dict], List[Document]]:
    """
    Format dữ liệu theo yêu cầu: "term: ..., definition: ..."
    """
    # Splitter chỉ dùng nếu 1 definition quá dài (vượt quá context window)
    # Nếu không muốn cắt, có thể set chunk_size thật lớn (vd: 2000)
    text_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=100)

    parents_data = []
    child_docs = []

    for item in raw_data:
        term = item["term"]
        definition = item["definition"]
        
        # 1. FORMAT CHUỖI CONTENT DUY NHẤT
        formatted_content = f"term: {term}, definition: {definition}"
        
        # 2. METADATA TỐI GIẢN
        metadata = {
            "source": FIXED_SOURCE_NAME
        }

        # Parent ID
        parent_id = str(uuid.uuid4())

        # Tạo Parent Data (SQL)
        parents_data.append({
            "parent_id": parent_id,
            "content": formatted_content,
            "metadata": json.dumps(metadata)
        })

        # Tạo Child Documents (Vector)
        # Nếu đoạn text ngắn, splitter sẽ giữ nguyên cả cụm "term: ..., definition: ..."
        chunks = text_splitter.split_text(formatted_content)
        
        for i, chunk_text in enumerate(chunks):
            chunk_meta = metadata.copy()
            chunk_meta.update({
                "parent_id": parent_id,
                "chunk_id": str(uuid.uuid4())
            })
            
            # Lưu ý: chunk_text ở đây đã mang định dạng "term: ..., definition: ..."
            # trừ khi definition quá dài bị cắt đôi, phần sau sẽ chỉ còn text definition
            # Nhưng với chunk_size=1000 thì hầu hết glossary sẽ nằm trọn trong 1 chunk.
            doc = Document(page_content=chunk_text, metadata=chunk_meta)
            child_docs.append(doc)

    return parents_data, child_docs

# --- 4. LƯU VÀO DB ---
def save_to_db(parents_data, child_docs):
    if not parents_data: return

    print(f"\n🚀 Đang lưu {len(parents_data)} thuật ngữ...")

    # 1. Lưu SQL
    with engine.connect() as conn:
        stmt = text("""
            INSERT INTO doc_parents (parent_id, content, metadata)
            VALUES (:parent_id, :content, :metadata)
            ON CONFLICT (parent_id) DO NOTHING;
        """)
        # Batch insert SQL
        for i in range(0, len(parents_data), 2000):
            conn.execute(stmt, parents_data[i:i+2000])
            conn.commit()
    print("✅ Đã lưu parent data.")

    # 2. Lưu Vector
    embeddings = ColabEmbeddings(api_url=COLAB_API_URL)
    vector_store = PGVector(
        embeddings=embeddings,
        collection_name=DB_COLLECTION_NAME,
        connection=DB_CONNECTION,
        use_jsonb=True,
    )
    
    # Batch insert Vector
    batch_size = 50
    for i in range(0, len(child_docs), batch_size):
        try:
            vector_store.add_documents(child_docs[i : i + batch_size])
            print(f"   -> Vector Batch {i} OK")
        except Exception as e:
            print(f"   ❌ Vector Batch {i} Lỗi: {e}")
            time.sleep(2)

    print("🎉 HOÀN TẤT TOÀN BỘ!")

# --- MAIN ---
if __name__ == "__main__":
    init_db()
    
    # 1. Đọc PDF
    raw_data = parse_pdf_data(INPUT_PDF_PATH, START_PAGE, END_PAGE)
    
    if raw_data:
        # 2. Format dữ liệu
        parents, children = prepare_documents(raw_data)
        
        # 3. Lưu
        save_to_db(parents, children)
    else:
        print("⚠️ Không tìm thấy dữ liệu.")