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
          className={`flex items-center w-full text-left px-3 py-2 rounded ${
            conv.id === activeConversationId
              ? "bg-gray-300"
              : "hover:bg-gray-200"
          }`}
        >
          <span className="truncate">{conv.title}</span>
        </button>
      ))}
    </div>
  );
}
