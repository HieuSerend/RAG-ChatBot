import type { ConversationResponse as Conversation } from "../../types/api";

interface ConversationListProps {
  conversations: Conversation[];
  activeConversationId: string | null;
  setActiveConversationId: (id: string) => void;
}

export default function ConversationList({
  conversations,
  activeConversationId,
  setActiveConversationId,
}: ConversationListProps) {
  return (
    <div className="space-y-1">
      {conversations.map((conv) => (
        <button
          key={conv.id}
          onClick={() => setActiveConversationId(conv.id)}
          className={`flex items-center w-full text-left px-3 py-2.5 rounded-lg text-sm transition-all duration-200 ${conv.id === activeConversationId
              ? "bg-blue-600/10 text-blue-400 border-l-2 border-blue-500 pl-2.5 font-medium"
              : "text-slate-400 hover:bg-slate-800 hover:text-slate-200 pl-3 border-l-2 border-transparent"
            }`}
        >
          <span className="truncate">{conv.title}</span>
        </button>
      ))}
    </div>
  );
}
