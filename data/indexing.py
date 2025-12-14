import os
import uuid
import json
import glob
import time
import requests  # Cần thêm thư viện này
from typing import List, Dict, Any, Tuple
from dotenv import load_dotenv
from sqlalchemy import create_engine, text

# Import LangChain
from langchain_text_splitters import MarkdownHeaderTextSplitter, RecursiveCharacterTextSplitter
from langchain_core.documents import Document
from langchain_core.embeddings import Embeddings  # Import base class
from langchain_postgres import PGVector

# --- 1. CẤU HÌNH ---
load_dotenv()

# ⚠️ QUAN TRỌNG: Thay link Ngrok của bạn vào đây
# Link phải có dạng: https://xxxx-xxxx.ngrok-free.app/embed_batch
COLAB_API_URL = "https://domelike-ora-gorgedly.ngrok-free.dev/embed_batch"

DB_CONNECTION = os.getenv("CONNECTION_STRING")
DB_COLLECTION_NAME = os.getenv("COLLECTION_NAME")
DATA_DIR = "investopedia_terms"

if not DB_CONNECTION or not DB_COLLECTION_NAME:
    raise ValueError("❌ Lỗi cấu hình: Kiểm tra file .env")

engine = create_engine(DB_CONNECTION)


# --- 2. ĐỊNH NGHĨA CUSTOM EMBEDDING CLASS ---
class ColabEmbeddings(Embeddings):
    """
    Class này giúp LangChain nói chuyện với API Colab của bạn.
    Nó đóng vai trò thay thế cho GoogleGenerativeAIEmbeddings.
    """

    def __init__(self, api_url: str):
        self.api_url = api_url

    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        """Hàm này được PGVector gọi để embed một danh sách văn bản."""
        try:
            # Thêm timeout=120 (chờ tối đa 2 phút) để an toàn cho batch lớn
            response = requests.post(self.api_url, json={"texts": texts}, timeout=120)

            if response.status_code == 200:
                data = response.json()
                return data['embeddings']
            else:
                # In ra lỗi chi tiết nếu Server trả về 500 hoặc 422
                print(f"⚠️ Server Response: {response.text}")
                raise ValueError(f"API Error {response.status_code}")
        except Exception as e:
            print(f"❌ Lỗi khi gọi API Colab: {e}")
            raise e

    def embed_query(self, text: str) -> List[float]:
        """Hàm này được dùng khi bạn Search (Retrieval)."""
        # Tận dụng luôn hàm embed_documents cho tiện
        return self.embed_documents([text])[0]


def init_db():
    """Tạo bảng 'doc_parents' nếu chưa có."""
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
    print("✅ Đã kiểm tra/tạo bảng 'doc_parents'.")


def clean_text(text: str) -> str:
    lines = text.split('\n')
    unique_lines = []
    prev_line = ""
    for line in lines:
        stripped = line.strip()
        if stripped and stripped != prev_line:
            unique_lines.append(line)
            prev_line = stripped
        elif stripped == "":
            unique_lines.append(line)
    return "\n".join(unique_lines)


# --- 3. XỬ LÝ DỮ LIỆU ---
def process_document_hybrid(raw_text: str, source_filename: str) -> Tuple[List[Dict], List[Document]]:
    cleaned_text = clean_text(raw_text)

    # A. Parent Chunking
    headers_to_split_on = [("#", "Header 1"), ("##", "Header 2"), ("###", "Header 3")]
    markdown_splitter = MarkdownHeaderTextSplitter(headers_to_split_on=headers_to_split_on)
    parent_docs = markdown_splitter.split_text(cleaned_text)

    # B. Child Chunking
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=500,  # Tăng lên xíu vì BGE-M3 chịu được context dài
        chunk_overlap=150,
        separators=["\n\n", "\n", ". ", " ", ""]
    )

    parents_data = []
    child_docs = []

    for i, parent in enumerate(parent_docs):
        parent_id = str(uuid.uuid4())

        # Chuẩn hóa metadata
        clean_metadata = {}
        for key, value in parent.metadata.items():
            clean_key = key.replace(" ", "_").lower()
            clean_metadata[clean_key] = value
        clean_metadata["source"] = source_filename

        parents_data.append({
            "parent_id": parent_id,
            "content": parent.page_content,
            "metadata": json.dumps(clean_metadata)
        })

        child_chunks = text_splitter.split_text(parent.page_content)

        for j, chunk_text in enumerate(child_chunks):
            child_metadata = {
                "chunk_id": str(uuid.uuid4()),
                "parent_id": parent_id,
                "chunk_index": j,
                "level": "child",
                "source": source_filename
            }
            child_metadata.update(clean_metadata)

            doc = Document(page_content=chunk_text, metadata=child_metadata)
            child_docs.append(doc)

    return parents_data, child_docs


def load_and_process_folder(folder_path: str):
    if not os.path.exists(folder_path):
        os.makedirs(folder_path)
        print(f"⚠️ Thư mục '{folder_path}' chưa tồn tại.")
        return [], []

    md_files = glob.glob(os.path.join(folder_path, "*.md"))
    if not md_files:
        print(f"⚠️ Không tìm thấy file .md nào.")
        return [], []

    all_parents = []
    all_children = []
    print(f"📂 Tìm thấy {len(md_files)} file Markdown.")

    for file_path in md_files:
        filename = os.path.basename(file_path)
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                raw_text = f.read()
            parents, children = process_document_hybrid(raw_text, filename)
            all_parents.extend(parents)
            all_children.extend(children)
        except Exception as e:
            print(f"   ❌ Lỗi đọc file {filename}: {e}")

    return all_parents, all_children


# --- 4. HÀM LƯU (SỬ DỤNG CUSTOM API) ---
def save_hybrid_data(parents_data: List[Dict], child_docs: List[Document]):
    print(f"\n🚀 Bắt đầu lưu dữ liệu...")

    # BƯỚC 1: Lưu Parents (SQL)
    BATCH_SIZE_SQL = 1000
    if parents_data:
        total_parents = len(parents_data)
        print(f"💾 Đang lưu {total_parents} Parents vào SQL...")

        insert_query = text("""
            INSERT INTO doc_parents (parent_id, content, metadata)
            VALUES (:parent_id, :content, :metadata)
            ON CONFLICT (parent_id) DO NOTHING;
        """)

        with engine.connect() as conn:
            for i in range(0, total_parents, BATCH_SIZE_SQL):
                batch = parents_data[i: i + BATCH_SIZE_SQL]
                conn.execute(insert_query, batch)
                conn.commit()
                print(f"   ✅ SQL Batch {i} -> {min(i + BATCH_SIZE_SQL, total_parents)}")

    # BƯỚC 2: Lưu Children (Vector Store qua API Colab)
    # Vì API Colab xử lý rất nhanh, ta có thể tăng batch size lên 
    BATCH_SIZE_VECTOR = 1000

    if child_docs:
        total_children = len(child_docs)
        print(f"\n💾 Đang embed và lưu {total_children} Children qua API Colab...")

        # --- KHỞI TẠO CUSTOM EMBEDDING ---
        embeddings_model = ColabEmbeddings(api_url=COLAB_API_URL)

        try:
            # Khởi tạo Vector Store
            # Lưu ý: PGVector sẽ tự động gọi embeddings_model.embed_documents()
            vector_store = PGVector(
                embeddings=embeddings_model,
                collection_name=DB_COLLECTION_NAME,
                connection=DB_CONNECTION,
                use_jsonb=True,
            )

            # Chia nhỏ để gửi API
            for i in range(0, total_children, BATCH_SIZE_VECTOR):
                batch = child_docs[i: i + BATCH_SIZE_VECTOR]
                try:
                    # Dòng này sẽ kích hoạt ColabEmbeddings.embed_documents
                    # Nó gửi 50 câu lên Colab -> Colab trả về 50 vector -> Lưu vào DB
                    vector_store.add_documents(batch)

                    percent = ((i + len(batch)) / total_children) * 100
                    print(f"   ✅ Vector Batch {i} -> {min(i + BATCH_SIZE_VECTOR, total_children)} ({percent:.1f}%)")

                except Exception as e:
                    print(f"   ❌ Lỗi Batch {i}: {e}")
                    time.sleep(2)  # Nghỉ xíu rồi chạy tiếp

            print("✅ Đã lưu TOÀN BỘ Vectors thành công!")

        except Exception as e:
            print(f"❌ Lỗi khởi tạo Vector Store: {e}")


# --- MAIN ---
if __name__ == "__main__":
    init_db()

    # Kiểm tra URL trước khi chạy
    if "ngrok-free.dev" not in COLAB_API_URL:
        print("⛔ LỖI: Bạn chưa dán link Ngrok vào biến COLAB_API_URL!")
    else:
        final_parents, final_children = load_and_process_folder(DATA_DIR)
        if final_parents and final_children:
            save_hybrid_data(final_parents, final_children)
            print("\n🎉 HOÀN TẤT!")
        else:
            print("\n⚠️ Không có dữ liệu.")