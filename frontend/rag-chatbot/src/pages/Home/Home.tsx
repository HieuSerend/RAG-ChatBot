import ChatBox from "../../components/chat/chatbox";
import MessageList from "../../components/chat/messageList";
import { useNavigate } from "react-router-dom";
import { PencilSquareIcon, MagnifyingGlassIcon, UserCircleIcon } from '@heroicons/react/24/solid';

// React import not required directly in newer JSX transforms; kept out to avoid unused var lint

export default function Home() {
  const navigate = useNavigate();

  return (
    <div>
      <div>
        <div className="flex h-screen">
          {/* Left sidebar */}
          <aside className="w-64 bg-gray-100 p-4 border-r border-black/10 flex flex-col">
            <div>
              <div className="mb-6">
                <h2 className="text-xl font-bold">DocuChat</h2>
              </div>

              <div className="space-y-1">
                <button className="flex items-center w-full text-left px-3 py-2 rounded hover:bg-gray-300">
                  <PencilSquareIcon className="w-5 h-5 mr-2 text-black" aria-hidden="true" />
                  <span>New chat</span>
                </button>
                <button className="flex items-center w-full text-left px-3 py-2 rounded hover:bg-gray-300">
                  <MagnifyingGlassIcon className="w-5 h-5 mr-2 text-black" aria-hidden="true" />
                  <span>Search chat</span>
                </button>
              </div>
            </div>

            {/* Middle: message list scrollable */}
            <div className="flex-1 overflow-auto mt-4 px-1">
              <MessageList />
            </div>

            <div className="mt-auto pt-4">
              <button
                onClick={() => navigate("/profile", { replace: true })}
                className="flex items-center w-full text-left px-3 py-2 rounded border-t border-black/10 hover:bg-gray-300 text-black"
              >
                <UserCircleIcon className="w-5 h-5 mr-2 text-black" aria-hidden="true" />
                <span>Profile</span>
              </button>
            </div>
          </aside>

          {/* Center content */}
          <main className="flex-1 bg-white p-4">
            <ChatBox />
          </main>
        </div>
      </div>
    </div>
  );
}
