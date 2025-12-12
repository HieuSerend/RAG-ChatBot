import { useState, useMemo, useEffect } from "react";
import type { ConversationResponse as Conversation } from "../../types/api";
import { Search, X, MessageSquare, Calendar } from "lucide-react";

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
      return conversations;
    }
    return conversations.filter((conv) =>
      (conv.title || "Untitled").toLowerCase().includes(searchQuery.toLowerCase())
    );
  }, [conversations, searchQuery]);

  const handleConversationClick = (id: string) => {
    setActiveConversationId(id);
    onClose();
    setSearchQuery("");
  };

  if (!isOpen) {
    return null;
  }

  return (
    <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex justify-center items-center p-4 animate-in fade-in duration-200">
      <div
        className="bg-slate-900 border border-slate-700/50 rounded-2xl shadow-2xl w-full max-w-2xl transform transition-all animate-in zoom-in-95 duration-200 overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="p-4 border-b border-slate-800">
          <div className="relative">
            <Search
              className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500"
              aria-hidden="true"
            />
            <input
              type="text"
              placeholder="Search conversations..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-10 py-3 rounded-xl border border-slate-800 bg-slate-950 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500/50 transition-all font-sans"
              autoFocus
            />
            <button
              onClick={onClose}
              className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-slate-500 hover:text-slate-300 transition-colors bg-slate-800/50 hover:bg-slate-800 rounded-md"
            >
              <X size={16} />
            </button>
          </div>
        </div>

        <div className="max-h-[60vh] overflow-y-auto scrollbar-thin scrollbar-thumb-slate-700 scrollbar-track-transparent p-2">
          {filteredConversations.length > 0 ? (
            <div className="space-y-1">
              {filteredConversations.length > 0 && searchQuery && (
                <p className="px-4 py-2 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Search Results
                </p>
              )}
              {filteredConversations.map((conv) => (
                <button
                  key={conv.id}
                  onClick={() => handleConversationClick(conv.id)}
                  className="w-full text-left p-3 rounded-xl hover:bg-slate-800 border border-transparent hover:border-slate-700/50 transition-all group flex items-start gap-4"
                >
                  <div className="p-2 bg-slate-800 group-hover:bg-slate-700 text-emerald-500/80 group-hover:text-emerald-400 rounded-lg transition-colors">
                    <MessageSquare size={20} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-semibold text-slate-200 truncate group-hover:text-emerald-400 transition-colors">
                      {conv.title || "Untitled Analysis"}
                    </p>
                    <div className="flex items-center gap-2 mt-1">
                      <Calendar size={12} className="text-slate-500" />
                      <p className="text-xs text-slate-500 font-medium">
                        {new Date(conv.createdDate).toLocaleDateString(undefined, {
                          year: 'numeric',
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit'
                        })}
                      </p>
                    </div>
                  </div>
                </button>
              ))}
            </div>
          ) : (
            <div className="text-center py-12">
              <div className="w-16 h-16 bg-slate-800/50 rounded-2xl flex items-center justify-center mx-auto mb-4 text-slate-600">
                <Search size={32} />
              </div>
              {searchQuery ? (
                <p className="text-slate-400">No results found for "<span className="text-slate-200 font-medium">{searchQuery}</span>"</p>
              ) : (
                <p className="text-slate-500">Search your conversation history</p>
              )}
            </div>
          )}
        </div>

        <div className="bg-slate-900 border-t border-slate-800 px-4 py-2 text-[10px] text-slate-500 flex justify-between items-center">
          <span><span className="font-mono bg-slate-800 px-1 py-0.5 rounded text-slate-400">ESC</span> to close</span>
          <span>{conversations.length} items</span>
        </div>
      </div>
    </div>
  );
}
