import api from "./api";
import type { PageResponse, ConversationResponse } from "../types/api";

export const createConversation = async (
  title: string,
): Promise<ConversationResponse> => {
  const response = await api.post("/conversation/create", { title });
  return response.data.data;
};

export const getConversations = async (
  page: number = 1,
  size: number = 10,
): Promise<PageResponse<ConversationResponse>> => {
  try {
    const response = await api.get(`/conversation/list`, {
      params: { page, size },
    });
    return response.data.data;
  } catch (error) {
    console.error("Error fetching conversations:", error);
    throw error;
  }
};

export const generateTitleFromText = async (text: string): Promise<string> => {
  const words = text.split(" ");
  const title = words.slice(0, 4).join(" ");
  return words.length > 4 ? `${title}...` : title;
};

export const generateTitle = async (conversationId: string, userMessage: string): Promise<string> => {
  try {
    const response = await api.post('/api/chat/generate-title', {
      conversationId,
      userMessage
    });
    return response.data.data;
  } catch (error) {
    console.error("Failed to generate title:", error);
    return "Untitled Analysis";
  }
};
