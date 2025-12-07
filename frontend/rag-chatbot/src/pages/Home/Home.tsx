import ChatBox from "../../components/chat/chatbox";
import { useEffect, useState, useCallback } from "react";
import { getConversations } from "../../services/conversationAPI";
import type { ConversationResponse as Conversation } from "../../types/api";

// Removed SearchModal and ConversationList for now as we are moving to a full-screen Chatbox UI 
// that manages its own sidebar/state as per the new design.

export default function Home() {
  const [, setConversations] = useState<Conversation[]>([]);
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
        setActiveConversationId((prevId) => prevId ?? sortedConvs[0].id);
      }
    } catch (error) {
      console.error("Failed to load conversations", error);
    }
  }, []);

  useEffect(() => {
    loadConversations();
  }, [loadConversations]);

  const handleConversationCreated = (newConv: Conversation) => {
    setConversations((prev) => [newConv, ...prev]);
    setActiveConversationId(newConv.id);
  };

  return (
    <div className="h-screen w-full bg-slate-900">
      {/* 
         We render the ChatBox directly. 
         The ChatBox component now handles the Sidebar and Layout internally 
         to provide the "Financial AI Assistant" experience.
       */}
      <ChatBox
        conversationId={activeConversationId}
        onConversationCreated={handleConversationCreated}
        onSelectConversation={setActiveConversationId}
      />
    </div>
  );
}
