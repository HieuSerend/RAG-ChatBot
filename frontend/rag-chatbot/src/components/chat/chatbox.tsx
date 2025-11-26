"use client";

import type React from "react";
import { useState, useRef, useEffect } from "react";
import { Send, Upload } from "lucide-react";
import { getMessages, streamMessage } from "../../services/messageAPI";
import { uploadDocument } from "../../services/documentAPI";
import {
  createConversation,
  generateTitleFromText,
} from "../../services/conversationAPI";
import type { Message } from "../../types/message";
import type { ConversationResponse } from "../../types/api";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

interface ChatBoxProps {
  conversationId: string | null;
  onConversationCreated?: (newConversation: ConversationResponse) => void;
}

export default function ChatBox({
  conversationId,
  onConversationCreated,
}: ChatBoxProps) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (conversationId) {
      const loadMessages = async () => {
        try {
          setIsLoading(true);
          const fetchedMessages = await getMessages(conversationId);
          setMessages(fetchedMessages);
        } catch (error) {
          console.error("Failed to load messages", error);
        } finally {
          setIsLoading(false);
        }
      };
      loadMessages();
    } else {
      setMessages([]);
    }
  }, [conversationId]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isLoading]);

  const adjustTextareaHeight = (element: HTMLTextAreaElement) => {
    element.style.height = "auto";
    element.style.height = `${Math.min(element.scrollHeight, 200)}px`;
  };

  const preprocessText = (text: string) => {
    if (!text) return "";
    return text.replace(/([.!?])\s*\*/g, "$1\n\n*");
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + " " + sizes[i];
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);
    adjustTextareaHeight(e.target);
  };

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size === 0) {
      alert("⚠️ Cannot upload an empty file.");
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      alert("⚠️ File too large! Maximum limit is 10MB.");
      if (fileInputRef.current) fileInputRef.current.value = "";
      return;
    }

    try {
      setIsLoading(true);
      await uploadDocument(file);
      setSelectedFile(file);
    } catch (error) {
      console.error("Failed to upload document", error);
      alert("⚠️ Failed to upload document.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleSendMessage = async () => {
    if (!input.trim()) return;

    const currentInput = input;
    setInput("");
    if (textareaRef.current) textareaRef.current.style.height = "auto";

    setIsLoading(true);

    let currentConversationId = conversationId;

    if (!currentConversationId) {
      try {
        const newTitle = await generateTitleFromText(currentInput);
        const newConv = await createConversation(newTitle);
        onConversationCreated?.(newConv);
        currentConversationId = newConv.id;
      } catch (error) {
        console.error("Failed to create new conversation:", error);
        setIsLoading(false);
        alert("Error: Could not start a new conversation. Please try again.");
        setInput(currentInput);
        return;
      }
    }

    if (!currentConversationId) {
      console.error("Conversation ID is still null after creation attempt.");
      setIsLoading(false);
      return;
    }

    const userMessage: Message = {
      id: Date.now().toString(),
      text: currentInput,
      role: "USER",
      createdAt: new Date().toISOString(),
      conversationId: currentConversationId,
    };
    setMessages((prev) => [...prev, userMessage]);

    const botMessagePlaceholder: Message = {
      id: (Date.now() + 1).toString(),
      text: "",
      role: "ASSISTANT",
      createdAt: new Date().toISOString(),
      conversationId: currentConversationId,
    };
    setMessages((prev) => [...prev, botMessagePlaceholder]);

    await streamMessage(
      currentConversationId,
      currentInput,
      (chunk) => {
        setMessages((prev) =>
          prev.map((msg) =>
            msg.id === botMessagePlaceholder.id
              ? { ...msg, text: msg.text + chunk }
              : msg,
          ),
        );
      },
      () => setIsLoading(false),
    );
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  return (
    <div className="flex flex-col h-full bg-white relative">
      <div
        ref={messagesContainerRef}
        className="flex-1 overflow-y-auto p-4 space-y-4 scroll-smooth hide-scrollbar pb-32"
      >
        {messages.map((message) => (
          <div
            key={message.id}
            className={`flex ${
              message.role === "USER" ? "justify-end" : "justify-start"
            }`}
          >
            <div
              className={`prose max-w-3xl px-4 py-3 rounded-2xl leading-relaxed prose-p:my-3 overflow-x-hidden break-words shadow-sm ${
                message.role === "USER"
                  ? "bg-indigo-600 text-white rounded-br-none prose-headings:text-white prose-p:text-white prose-strong:text-white prose-li:text-white"
                  : "bg-slate-100 text-slate-900 rounded-bl-none"
              }`}
            >
              {message.role === "ASSISTANT" ? (
                <ReactMarkdown remarkPlugins={[remarkGfm]}>
                  {preprocessText(message.text)}
                </ReactMarkdown>
              ) : (
                <p className="text-sm">{message.text}</p>
              )}
              <span
                className={`text-xs mt-2 block opacity-70 ${
                  message.role === "USER" ? "text-indigo-100" : "text-slate-500"
                }`}
              >
                {new Date(message.createdAt).toLocaleTimeString("en-US", {
                  hour: "2-digit",
                  minute: "2-digit",
                })}
              </span>
            </div>
          </div>
        ))}

        {isLoading && (
          <div className="flex justify-start">
            <div className="bg-slate-100 px-4 py-2 rounded-2xl rounded-bl-none shadow-sm">
              <div className="flex space-x-2">
                <div className="w-2 h-2 bg-slate-400 rounded-full animate-bounce"></div>
                <div className="w-2 h-2 bg-slate-400 rounded-full animate-bounce delay-100"></div>
                <div className="w-2 h-2 bg-slate-400 rounded-full animate-bounce delay-200"></div>
              </div>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      <div className="absolute bottom-0 left-0 right-0 p-4 bg-gradient-to-t from-white via-white/80 to-transparent">
        <div className="w-full mx-auto">
          {selectedFile && (
            <div className="mb-2 p-2 bg-indigo-50 rounded-lg flex items-center justify-between text-sm">
              <span className="text-indigo-900">
                📎 {selectedFile.name} ({formatFileSize(selectedFile.size)})
              </span>
              <button
                onClick={() => {
                  setSelectedFile(null);
                  if (fileInputRef.current) fileInputRef.current.value = "";
                }}
                className="text-indigo-600 hover:text-indigo-700 font-semibold"
              >
                Remove
              </button>
            </div>
          )}

          <div className="relative flex items-end w-full bg-white border border-slate-300 rounded-[26px] p-2 shadow-lg focus-within:ring-2 focus-within:ring-indigo-500 transition-all">
            <input
              ref={fileInputRef}
              type="file"
              onChange={handleFileSelect}
              className="hidden"
              accept=".pdf"
            />
            <button
              onClick={() => fileInputRef.current?.click()}
              disabled={isLoading}
              className="text-slate-500 hover:text-indigo-600 p-2 rounded-full transition-colors mb-0.5"
              title="Upload document"
            >
              <Upload size={20} />
            </button>

            <textarea
              ref={textareaRef}
              value={input}
              onChange={handleInputChange}
              onKeyDown={handleKeyDown}
              placeholder="Type your message..."
              rows={1}
              className="flex-1 w-full bg-transparent outline-none border-none focus:ring-0 px-3 py-1.5 leading-tight text-slate-900 placeholder:text-slate-500 resize-none overflow-y-auto max-h-[150px] scrollbar-thin scrollbar-thumb-slate-300 scrollbar-track-transparent"
              disabled={isLoading}
            />

            <button
              onClick={handleSendMessage}
              disabled={isLoading || !input.trim()}
              className={`p-2 rounded-full transition-colors mb-0.5 ${
                input.trim()
                  ? "bg-indigo-600 hover:bg-indigo-700 text-white"
                  : "bg-slate-200 text-slate-400 cursor-not-allowed"
              }`}
            >
              <Send size={20} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
