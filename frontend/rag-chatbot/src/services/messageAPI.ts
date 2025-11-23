import api from "./api";
import type { Message } from "../types/message";
import { getAccessToken } from "./authService";

export const getMessages = async (
  conversationId: string,
): Promise<Message[]> => {
  const response = await api.get(`/message/list/${conversationId}`);
  return response.data.data;
};

export const streamMessage = async (
  conversationId: string,
  message: string,
  onChunk: (chunk: string) => void,
  onStreamEnd: () => void,
) => {
  const token = getAccessToken();
  const response = await fetch(`/api/message/stream-create`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ conversationId, text: message }),
  });

  if (!response.body) {
    throw new Error("No response body from stream");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }
      buffer += decoder.decode(value, { stream: true });

      const lines = buffer.split("\n");
      buffer = lines.pop() || ""; // Keep the last partial line in the buffer

      for (const line of lines) {
        if (line.startsWith("data:")) {
          const data = line.substring(5);
          if (data) {
            onChunk(data);
          }
        }
      }
    }
  } catch (error) {
    console.error("Error reading stream:", error);
  } finally {
    reader.releaseLock();
    onStreamEnd();
  }
};
