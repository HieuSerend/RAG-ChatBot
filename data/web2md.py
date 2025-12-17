import requests
from bs4 import BeautifulSoup, NavigableString, Tag
import time
import re
import os
from dotenv import load_dotenv

# --- CẤU HÌNH ---
load_dotenv(dotenv_path="env")

BASE = os.getenv("BASE_URL", "https://www.investopedia.com")
START_URL = os.getenv("START_URL", "https://www.investopedia.com/financial-term-dictionary-4769738")
USER_AGENT = os.getenv("USER_AGENT", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
DATA_DIR = os.getenv("DATA_DIR", "investopedia_terms")

HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8',
    'Accept-Language': 'en-US,en;q=0.9,vi;q=0.8',
    'Referer': 'https://www.google.com/'  # Giả vờ đi từ Google vào
}

# ============================================================
# 1) CLEAN FILE NAME
# ============================================================

def slugify(text: str):
    text = text.lower().strip()
    text = re.sub(r"[^a-z0-9]+", "-", text)
    return text.strip("-")


# ============================================================
# 2) HTML → MARKDOWN
# ============================================================

def html_to_markdown(element):
    md = ""

    for child in element.descendants:
        if isinstance(child, NavigableString):
            text = child.strip()
            if text:
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
                if href.startswith("/"):
                    href = BASE + href
                md += f"[{txt}]({href}) "

            elif child.name == "img":
                alt = child.get("alt", "")
                src = child.get("src", "")
                if src.startswith("/"):
                    src = BASE + src
                md += f"![{alt}]({src})\n\n"

            elif child.name == "table":
                md += "\n\n" + html_table_to_md(child) + "\n\n"

    return md.strip()


def html_table_to_md(table_tag):
    rows = table_tag.find_all("tr")
    md = ""

    for i, row in enumerate(rows):
        cols = [c.get_text(strip=True) for c in row.find_all(["td", "th"])]

        line = "| " + " | ".join(cols) + " |\n"
        md += line

        if i == 0:
            md += "| " + " | ".join(["---"] * len(cols)) + " |\n"

    return md


# ============================================================
# 3) TẠO FOLDER OUTPUT
# ============================================================

os.makedirs(DATA_DIR, exist_ok=True)


# ============================================================
# 4) LẤY LIST TERM
# ============================================================

print("Đang tải danh sách term...")
html = requests.get(START_URL, headers=HEADERS).text
soup = BeautifulSoup(html, "html.parser")

items = soup.select(".dictionary-top24-list__sublist.mntl-text-link")

links = []
for item in items:
    name = item.get_text(strip=True)
    href = item.get("href")
    if href.startswith("/"):
        href = BASE + href
    links.append((name, href))

# links = links[:10]  # Giới hạn 10
print(f"Đã lấy {len(links)} links.\n")


# ============================================================
# 5) SCRAPE TỪNG TERM → GHI FILE RIÊNG
# ============================================================

for idx, (term, link) in enumerate(links, 1):
    print(f"[{idx}] Scrape: {term}")

    time.sleep(1)

    try:
        page = requests.get(link, headers=HEADERS).text
        s = BeautifulSoup(page, "html.parser")

        content = s.select_one("div.mntl-sc-page")
        if not content:
            print("❌ Không tìm thấy nội dung!")
            continue

        md = html_to_markdown(content)

        slug = slugify(term)
        filename = f"{DATA_DIR}/{slug}.md"

        with open(filename, "w", encoding="utf-8") as f:
            f.write(f"# {term}\n\n")
            f.write(f"Source: {link}\n\n")
            f.write(md)

        print(f"  ✔ Đã lưu {filename}")

    except Exception as e:
        print(f"❌ Lỗi: {e}")

print("\n🎉 DONE! Mỗi trang đã được lưu thành 1 file Markdown riêng trong thư mục 'investopedia_terms/'")
