from curl_cffi import requests  # <--- DÙNG CÁI NÀY THAY REQUESTS THƯỜNG
from bs4 import BeautifulSoup, NavigableString, Tag
import time
import re
import os
import random # <--- Thêm random để delay tự nhiên
from dotenv import load_dotenv

# --- CẤU HÌNH ---
load_dotenv(dotenv_path="env")

BASE = os.getenv("BASE_URL", "https://www.investopedia.com")
START_URL = os.getenv("START_URL", "https://www.investopedia.com/financial-term-dictionary-4769738")
DATA_DIR = os.getenv("DATA_DIR", "investopedia_terms")

# ============================================================
# 1) CLEAN FILE NAME
# ============================================================

def slugify(text: str):
    text = text.lower().strip()
    text = re.sub(r"[^a-z0-9]+", "-", text)
    return text.strip("-")

# ============================================================
# 2) HTML → MARKDOWN (Giữ nguyên logic của bạn)
# ============================================================

def html_to_markdown(element):
    md = ""
    # Lưu ý: descendants duyệt cả con lẫn cháu, dễ bị lặp text.
    # Mình thêm check parent để hạn chế lặp, nhưng vẫn giữ logic gốc của bạn.
    for child in element.descendants:
        if isinstance(child, NavigableString):
            text = child.strip()
            # Chỉ lấy text nếu thẻ cha không nằm trong danh sách thẻ block (tránh lặp)
            if text and child.parent.name not in ["p", "h1", "h2", "h3", "h4", "li", "strong", "em", "a", "td", "th"]:
                md += text + " "

        elif isinstance(child, Tag):
            if child.name in ["h1", "h2", "h3", "h4"]:
                level = int(child.name[1])
                md += "\n" + ("#" * level) + " " + child.get_text(strip=True) + "\n\n"
            elif child.name == "p":
                md += "\n" + child.get_text(strip=True) + "\n\n"
            elif child.name == "li":
                md += f"- {child.get_text(strip=True)}\n"
            elif child.name == "strong":
                md += f"**{child.get_text(strip=True)}** "
            elif child.name == "em":
                md += f"*{child.get_text(strip=True)}* "
            elif child.name == "a":
                href = child.get("href", "")
                txt = child.get_text(strip=True)
                if href.startswith("/"): href = BASE + href
                md += f"[{txt}]({href}) "
            elif child.name == "img":
                alt = child.get("alt", "")
                src = child.get("src", "")
                if src.startswith("/"): src = BASE + src
                md += f"![{alt}]({src})\n\n"
            elif child.name == "table":
                md += "\n\n" + html_table_to_md(child) + "\n\n"
    return md.strip()

def html_table_to_md(table_tag):
    rows = table_tag.find_all("tr")
    md = ""
    for i, row in enumerate(rows):
        cols = [c.get_text(strip=True) for c in row.find_all(["td", "th"])]
        if not cols: continue
        line = "| " + " | ".join(cols) + " |\n"
        md += line
        if i == 0: md += "| " + " | ".join(["---"] * len(cols)) + " |\n"
    return md

# ============================================================
# 3) TẠO FOLDER OUTPUT
# ============================================================

os.makedirs(DATA_DIR, exist_ok=True)

# ============================================================
# 4) LẤY LIST TERM (ĐÃ SỬA LỖI 403 VÀ SELECTOR)
# ============================================================

print("Đang tải danh sách term...")

try:
    # impersonate="chrome120" giả lập trình duyệt Chrome thật
    # timeout=30 tránh treo máy
    response = requests.get(START_URL, impersonate="chrome120", timeout=30)
    
    if response.status_code != 200:
        print(f"LỖI: Server trả về code {response.status_code}")
        exit()
        
    soup = BeautifulSoup(response.text, "html.parser")

    # --- SỬA SELECTOR ---
    # Selector cũ của bạn: ".dictionary-top24-list__sublist.mntl-text-link" (sai, vì nó tìm thẻ có cả 2 class cùng lúc)
    # Selector mới: Tìm thẻ 'a' nằm BÊN TRONG thẻ list
    items = soup.select(".dictionary-top24-list__sublist a.mntl-text-link")
    
    # Fallback: Nếu web đổi cấu trúc, thử tìm thẻ a trong ID nội dung
    if not items:
         items = soup.select("#dictionary-top24-list__sublist-content_1-0 a")

    links = []
    for item in items:
        name = item.get_text(strip=True)
        href = item.get("href")
        if href and href.startswith("/"):
            href = BASE + href
        links.append((name, href))

    print(f"✅ Đã lấy {len(links)} links.\n")

except Exception as e:
    print(f"❌ Lỗi kết nối ban đầu: {e}")
    exit()

# ============================================================
# 5) SCRAPE TỪNG TERM
# ============================================================

# Tạo session để giữ kết nối, giúp tải nhanh hơn và ít bị chặn hơn
session = requests.Session()

for idx, (term, link) in enumerate(links, 1):
    slug = slugify(term)
    filename = f"{DATA_DIR}/{slug}.md"
    
    # Kiểm tra file tồn tại để resume nếu bị ngắt
    if os.path.exists(filename):
        print(f"[{idx}/{len(links)}] ⏭️ Đã có: {term}")
        continue

    print(f"[{idx}/{len(links)}] ⬇️ Đang tải: {term}")

    # Random delay (quan trọng để tránh bị Cloudflare phát hiện bot hàng loạt)
    time.sleep(random.uniform(2, 5))

    try:
        # Dùng session để tải trang con
        page = session.get(link, impersonate="chrome120", timeout=30)
        
        if page.status_code != 200:
            print(f"   ⚠️ Lỗi tải trang (Code {page.status_code})")
            continue

        s = BeautifulSoup(page.text, "html.parser")

        # Selector nội dung (Update cho chuẩn trang Investopedia hiện tại)
        content = s.select_one("#mntl-sc-page_1-0") 
        if not content:
            content = s.select_one(".mntl-sc-page") # Class dự phòng

        if not content:
            print("   ⚠️ Không tìm thấy nội dung bài viết (HTML khác mẫu)!")
            continue

        md = html_to_markdown(content)

        with open(filename, "w", encoding="utf-8") as f:
            f.write(f"# {term}\n\n")
            f.write(f"Source: {link}\n\n")
            f.write(md)

        print(f"   ✅ Đã lưu file.")

    except Exception as e:
        print(f"   ❌ Lỗi ngoại lệ: {e}")

print("\n🎉 DONE! Kiểm tra thư mục 'investopedia_terms/'")