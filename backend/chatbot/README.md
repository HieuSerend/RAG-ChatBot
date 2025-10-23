# 🤖 RAG Chatbot Backend

Hệ thống backend cho RAG Chatbot sử dụng Spring Boot 3.5.6 với JWT Authentication và PostgreSQL.

## 📋 Mục lục

- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Cài đặt](#cài-đặt)
- [Cấu hình](#cấu-hình)
- [Chạy ứng dụng](#chạy-ứng-dụng)


## 🔧 Yêu cầu hệ thống

Trước khi bắt đầu, đảm bảo máy tính của bạn đã cài đặt:

- **Java Development Kit (JDK)**: 21 hoặc cao hơn
  - [Download JDK 21](https://www.oracle.com/java/technologies/downloads/#java21)
  - Kiểm tra phiên bản: `java -version`

- **Apache Maven**: 3.8+ hoặc sử dụng Maven Wrapper đã có trong dự án
  - [Download Maven](https://maven.apache.org/download.cgi)
  - Kiểm tra phiên bản: `mvn -version`

- **PostgreSQL**: 12+ (khuyến nghị 14+)
  - [Download PostgreSQL](https://www.postgresql.org/download/)
  - Kiểm tra phiên bản: `psql --version`

- **Git**: Để clone repository
  - [Download Git](https://git-scm.com/downloads)

- **IDE** (Tùy chọn nhưng khuyến nghị):
  - IntelliJ IDEA
  - Eclipse
  - Visual Studio Code với Java Extension Pack

## 📥 Cài đặt

### Bước 1: Clone Repository

```bash
git clone <repository-url>
cd RAG-ChatBot/backend/chatbot
```

### Bước 2: Cài đặt PostgreSQL

#### Windows:
1. Download PostgreSQL từ [official website](https://www.postgresql.org/download/windows/)
2. Chạy installer và làm theo hướng dẫn
3. Ghi nhớ password của user `postgres`

#### macOS:
```bash
# Sử dụng Homebrew
brew install postgresql@14
brew services start postgresql@14
```

#### Linux (Ubuntu/Debian):
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### Bước 3: Tạo Database

```bash
# Đăng nhập vào PostgreSQL
psql -U postgres

# Tạo database
CREATE DATABASE rag_chatbot;

# Thoát
\q
```

Hoặc sử dụng pgAdmin (GUI tool):
1. Mở pgAdmin
2. Right-click trên "Databases" → Create → Database
3. Đặt tên: `rag_chatbot`
4. Click "Save"

### Bước 4: Cài đặt Dependencies

```bash
# Nếu sử dụng Maven
mvn clean install

# Hoặc sử dụng Maven Wrapper (không cần cài Maven)
# Windows
.\mvnw.cmd clean install

# Linux/macOS
./mvnw clean install
```


## 🚀 Chạy ứng dụng

### Cách 1: Sử dụng Maven

```bash
# Chạy với Maven
mvn spring-boot:run

# Hoặc với Maven Wrapper
# Windows
.\mvnw.cmd spring-boot:run

# Linux/macOS
./mvnw spring-boot:run
```

### Cách 2: Build JAR và chạy

```bash
# Build JAR file
mvn clean package

# Chạy JAR
java -jar target/chatbot-0.0.1-SNAPSHOT.jar
```

### Cách 3: Chạy từ IDE

#### IntelliJ IDEA:
1. Import project as Maven project
2. Chờ Maven download dependencies
3. Tìm file `ChatbotApplication.java`
4. Right-click → Run 'ChatbotApplication'

#### Eclipse:
1. Import → Existing Maven Projects
2. Chọn thư mục project
3. Right-click trên project → Run As → Spring Boot App


