import { logout } from "../../services/authAPI";
import { useNavigate } from "react-router-dom";

export default function Home() {
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/auth/login", { replace: true });
  };

  return (
    <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", background: "#f7f7fb" }}>
      <div style={{ width: 880, padding: 32, background: "#fff", borderRadius: 12, boxShadow: "0 6px 20px rgba(0,0,0,0.08)" }}>
        <h1 style={{ fontSize: 28, marginBottom: 12 }}>DocuChat — Chatbot đọc hiểu tài liệu</h1>
        <p style={{ color: "#444", marginBottom: 20 }}>
          Bắt đầu chat với tài liệu của bạn ngay hôm nay! Tải lên tài liệu và hỏi bất cứ điều gì bạn muốn biết về nội dung bên trong.
        </p>

        <div style={{display:"flex", flexDirection: "row"}}>
            <div>
                <button onClick={handleLogout} 
                        style={{ padding: "8px 16px", background: "#ff4d4f", color: "#fff", border: "none", borderRadius: 4, cursor: "pointer" }}>
                    Đăng xuất
                </button>
            </div>
            <div>
                <p>Chat box ở đây</p>
            </div>
        </div>
      </div>
    </div>
  );
}
