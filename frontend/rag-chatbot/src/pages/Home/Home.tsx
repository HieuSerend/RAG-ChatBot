import ChatBox from "../../components/chat/chatbox";
import ConversationList from "../../components/chat/conversationList";
import { useNavigate } from "react-router-dom";
import {
  PencilSquareIcon,
  MagnifyingGlassIcon,
  UserCircleIcon,
} from "@heroicons/react/24/solid";
import { useEffect, useState, useCallback } from "react";
import { getConversations } from "../../services/conversationAPI";
import type { ConversationResponse as Conversation } from "../../types/api";

export default function Home() {
  const navigate = useNavigate();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeConversationId, setActiveConversationId] = useState<
    string | null
  >(null);

  const loadConversations = useCallback(async () => {
    try {
      const convs = (await getConversations()).result;
      const sortedConvs = convs.sort(
        (a, b) =>
          new Date(b.createdDate).getTime() - new Date(a.createdDate).getTime(),
      );
      setConversations(sortedConvs);
      if (sortedConvs.length > 0) {
        // Set active conversation only if one isn't already active
        setActiveConversationId((prevId) => prevId ?? sortedConvs[0].id);
      }
    } catch (error) {
      console.error("Failed to load conversations", error);
    }
  }, []);

  useEffect(() => {
    loadConversations();
  }, [loadConversations]);

  const handleNewConversation = () => {
    setActiveConversationId(null);
  };

  const handleConversationCreated = (newConv: Conversation) => {
    setConversations((prev) => [newConv, ...prev]);
    setActiveConversationId(newConv.id);
  };

  return (
    <div>
      <div className="flex h-screen">
        {/* Left sidebar */}
        <aside className="w-64 bg-gray-100 p-4 border-r border-black/10 flex flex-col">
          <div>
            <div className="mb-6">
              <h2 className="text-xl font-bold">DocuChat</h2>
            </div>

            <div className="space-y-1">
              <button
                onClick={handleNewConversation}
                className="flex items-center w-full text-left px-3 py-2 rounded hover:bg-gray-300"
              >
                <PencilSquareIcon
                  className="w-5 h-5 mr-2 text-black"
                  aria-hidden="true"
                />
                <span>New chat</span>
              </button>
              <button className="flex items-center w-full text-left px-3 py-2 rounded hover:bg-gray-300">
                <MagnifyingGlassIcon
                  className="w-5 h-5 mr-2 text-black"
                  aria-hidden="true"
                />
                <span>Search chat</span>
              </button>
            </div>
          </div>

          {/* Middle: conversation list scrollable */}
          <div className="flex-1 overflow-auto mt-4 px-1">
            <ConversationList
              conversations={conversations}
              activeConversationId={activeConversationId}
              setActiveConversationId={setActiveConversationId}
            />
          </div>

          <div className="mt-auto pt-4">
            <button
              onClick={() => navigate("/profile", { replace: true })}
              className="flex items-center w-full text-left px-3 py-2 rounded border-t border-black/10 hover:bg-gray-300 text-black"
            >
              <UserCircleIcon
                className="w-5 h-5 mr-2 text-black"
                aria-hidden="true"
              />
              <span>Profile</span>
            </button>
          </div>
        </aside>

        {/* Center content */}
        <main className="flex-1 bg-white py-4 px-40">
          <ChatBox
            conversationId={activeConversationId}
            onConversationCreated={handleConversationCreated}
          />
        </main>
      </div>
    </div>
  );
}
