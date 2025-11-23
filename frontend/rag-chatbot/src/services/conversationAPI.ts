import api from "./api";
import type { PageResponse, ConversationResponse } from "../types/api"; // Assuming these types exist or will be created
import type { Conversation } from "../types/conversation";

export const createConversation = async (

  title: string,

): Promise<Conversation> => {

  const response = await api.post("/conversation/create", { title });

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
  return response.data.data;

};

export const getConversations = async (): Promise<Conversation[]> => {
  const response = await api.get("/conversation/list");
  console.log(response);
  return response.data.data;
};

// Mock function to generate title from text
export const generateTitleFromText = async (text: string): Promise<string> => {
  // In a real scenario, this would be a call to a generative AI endpoint.
  // For now, we'll just take the first few words.
  const words = text.split(" ");
  const title = words.slice(0, 4).join(" ");
  return words.length > 4 ? `${title}...` : title;
};


