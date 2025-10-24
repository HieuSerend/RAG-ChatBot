import ChatBox from "../../components/chat/ChatBox";
import { logout } from "../../services/authAPI";
import { useNavigate } from "react-router-dom";
// React import not required directly in newer JSX transforms; kept out to avoid unused var lint

export default function Home() {
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/auth/login", { replace: true });
  };

  return (
    <div className="min-h-screen bg-slate-100">
      <div className="max-w-screen-xl mx-auto px-4 py-6">
        <div className="flex gap-6">
          {/* Left sidebar */}
          <aside className="w-64 bg-white rounded-lg shadow-sm p-4 flex flex-col">
            <div className="mb-6">
              <h2 className="text-xl font-bold">DocuChat</h2>
              <p className="text-sm text-slate-500">RAG Chatbot</p>
            </div>
            <nav className="flex-1">
              <ul className="space-y-2">
                <li>
                  <button className="w-full text-left px-3 py-2 rounded hover:bg-slate-50">
                    New chat
                  </button>
                </li>
                <li>
                  <button className="w-full text-left px-3 py-2 rounded hover:bg-slate-50">
                    History
                  </button>
                </li>
                <li>
                  <button className="w-full text-left px-3 py-2 rounded hover:bg-slate-50">
                    Settings
                  </button>
                </li>
              </ul>
            </nav>
            <div className="mt-4">
              <button
                onClick={handleLogout}
                className="w-full bg-red-500 hover:bg-red-600 text-white py-2 rounded-md"
              >
                Đăng xuất
              </button>
            </div>
          </aside>

          {/* Center content */}
          <main className="flex-1 bg-white rounded-lg shadow-sm p-6 min-h-[70vh]">
            <header className="mb-6">
              <h1 className="text-2xl font-semibold">
                Chào mừng đến với DocuChat
              </h1>
              <p className="text-sm text-slate-600 mt-2">
                Bắt đầu chat với tài liệu của bạn ngay hôm nay. Tải lên tài liệu
                hoặc tạo cuộc trò chuyện mới.
              </p>
            </header>

            <section className="h-full flex items-center justify-center">
              <div className="text-center text-slate-500">
                <p className="mb-4">Chưa có cuộc trò chuyện nào</p>
                <button className="px-4 py-2 bg-indigo-600 text-white rounded-md">
                  Bắt đầu cuộc trò chuyện mới
                </button>
              </div>
            </section>
          </main>

          {/* Right chatbox placeholder */}
          <aside className="w-[420px] bg-white rounded-lg shadow-sm p-4 h-[70vh]">
            <ChatBox />
          </aside>
        </div>
      </div>
    </div>
  );
}
