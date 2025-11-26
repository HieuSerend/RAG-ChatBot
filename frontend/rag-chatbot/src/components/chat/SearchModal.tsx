import { useState, useMemo, useEffect } from "react";
import type { ConversationResponse as Conversation } from "../../types/api";
import { MagnifyingGlassIcon, XMarkIcon } from "@heroicons/react/24/solid";

interface SearchModalProps {
  isOpen: boolean;
  onClose: () => void;
  conversations: Conversation[];
  setActiveConversationId: (id: string) => void;
}

export default function SearchModal({
  isOpen,
  onClose,
  conversations,
  setActiveConversationId,
}: SearchModalProps) {
  const [searchQuery, setSearchQuery] = useState("");

  useEffect(() => {
    if (!isOpen) return;

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    document.addEventListener("keydown", handleEscape);
    return () => {
      document.removeEventListener("keydown", handleEscape);
    };
  }, [isOpen, onClose]);

  const filteredConversations = useMemo(() => {
    if (!searchQuery) {
      return conversations; // Display all conversations if search query is empty
    }
    return conversations.filter((conv) =>
      conv.title.toLowerCase().includes(searchQuery.toLowerCase())
    );
  }, [conversations, searchQuery]);

  const handleConversationClick = (id: string) => {
    setActiveConversationId(id);
    onClose();
    setSearchQuery("")
  };

  if (!isOpen) {
    return null;
  }

  return (
    <div className="fixed inset-0 bg-gradient-to-br from-gray-900/50 via-purple-900/50 to-gray-900/50 backdrop-blur-sm z-50 flex justify-center items-center">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl transform transition-all">
        <div className="p-4 border-b border-gray-200">
          <div className="relative">
            <MagnifyingGlassIcon
              className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
              aria-hidden="true"
            />
            <input
              type="text"
              placeholder="Search conversations..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-10 py-2 rounded-lg border bg-gray-50 border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              autoFocus
            />
            <button onClick={onClose} className="absolute right-3 top-1/2 -translate-y-1/2">
                <XMarkIcon className="w-6 h-6 text-gray-500 hover:text-gray-800" />
            </button>
          </div>
        </div>
        <div className="p-4 max-h-[80vh] overflow-y-auto">
          {filteredConversations.length > 0 ? (
            <ul className="space-y-2">
              {filteredConversations.map((conv) => (
                <li key={conv.id}>
                  <button
                    onClick={() => handleConversationClick(conv.id)}
                    className="w-full text-left p-3 rounded-lg hover:bg-gray-100"
                  >
                    <p className="font-semibold">{conv.title}</p>
                    <p className="text-sm text-gray-500">
                      {new Date(conv.createdDate).toLocaleString()}
                    </p>
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <div className="text-center py-8 text-gray-500">
              {searchQuery ? (
                <>No conversations found for "{searchQuery}"</>
              ) : (
                <>No conversations available.</>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
