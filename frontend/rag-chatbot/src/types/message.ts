export interface Message {
  id: string;
  text: string;
  conversationId: string;
  role: "USER" | "ASSISTANT";
  createdAt: string; // Using string for simplicity, can be converted to Date object if needed
}
