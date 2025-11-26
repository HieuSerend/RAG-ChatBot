export interface Message {
  id: string;
  text: string;
  conversationId: string;
  role: "USER" | "ASSISTANT";
  createdAt: string;
}
