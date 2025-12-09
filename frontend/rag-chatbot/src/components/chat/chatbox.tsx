import React, { useState, useRef, useEffect, useMemo } from 'react';
import {
  Send,
  Paperclip,
  TrendingUp,
  PieChart,
  FileText,
  Search,
  Menu,
  X,
  Plus,
  Bot,
  User,
  Activity,
  MoreHorizontal,
  AlertTriangle,
  Loader
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import type { ConversationResponse } from '../../types/api';
import { createMessage, getMessages } from '../../services/messageAPI';
import { createConversation, getConversations, generateTitleFromText, generateTitle } from '../../services/conversationAPI';
import { uploadDocument } from '../../services/documentAPI';
import RichTextRenderer from './RichTextRenderer';

// --- Utility for Tailwind ---
function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

import { getRandomSuggestions, iconMap, type SuggestionItem } from '../../data/suggestions';

// ... other imports ...

// ... Types ...
type Message = {
  id: string;
  role: 'user' | 'ai';
  content: string | React.ReactNode;
  timestamp: Date;
};

interface ChatboxProps {
  conversationId: string | null;
  onConversationCreated: (conversation: ConversationResponse) => void;
  onSelectConversation: (id: string | null) => void;
}


import { useNavigate } from 'react-router-dom';
import { getMyInfo } from '../../services/userAPI';
import type { UserProfile } from '../../types/user';

import SearchModal from './SearchModal';

// ... imports ...

export default function Chatbox({ conversationId, onConversationCreated, onSelectConversation }: ChatboxProps) {
  const navigate = useNavigate();
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputValue, setInputValue] = useState('');

  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [isTyping, setIsTyping] = useState(false);
  const [history, setHistory] = useState<ConversationResponse[]>([]);
  const [user, setUser] = useState<UserProfile | null>(null);
  const [suggestions, setSuggestions] = useState<SuggestionItem[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setSuggestions(getRandomSuggestions(4));
  }, []);

  const greeting = useMemo(() => {
    const hour = new Date().getHours();
    let text = "Good afternoon";
    if (hour >= 5 && hour < 12) {
      text = "Good morning";
    } else if (hour >= 18 || hour < 5) {
      text = "Good evening";
    }

    if (user && (user.firstName || user.username)) {
      const name = user.firstName || user.username;
      return `${text}, ${name}`;
    }
    return `${text}`;
  }, [user]);

  // Fetch User Info
  useEffect(() => {
    const fetchUser = async () => {
      try {
        const userData = await getMyInfo();
        setUser(userData);
      } catch (e) {
        console.error("Failed to fetch user info", e);
      }
    }
    fetchUser();
  }, []);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isTyping]);

  // Load Sidebar History
  useEffect(() => {
    const fetchHistory = async () => {
      try {
        const res = await getConversations(1, 20);
        setHistory(res.result);
      } catch (err) {
        console.error("Failed to load history", err);
      }
    };
    fetchHistory();
  }, [conversationId]); // Reload when conversation changes to update order if needed

  // Load Messages for active conversation
  useEffect(() => {
    const loadMessages = async () => {
      if (!conversationId) {
        setMessages([]);
        return;
      }

      try {
        const fetchedMsgs = await getMessages(conversationId);
        // Map API messages to UI format
        const uiMsgs: Message[] = fetchedMsgs.map(m => ({
          id: m.id,
          role: m.role === 'USER' ? 'user' : 'ai',
          content: m.text,
          timestamp: new Date(m.createdAt)
        }));
        // Sort by timestamp if needed, assume API returns chronological? 
        // Usually API returns latest first or specific order. 
        // Let's sort by date just in case to be safe for display
        uiMsgs.sort((a, b) => a.timestamp.getTime() - b.timestamp.getTime());

        setMessages(uiMsgs);
      } catch (err) {
        console.error("Failed to load messages", err);
      }
    };
    loadMessages();
  }, [conversationId]);

  // Helper to safely adding a new message or updating the last one
  const handleSendMessage = async (text: string) => {
    if (!text.trim() || isTyping) return;

    // 1. Optimistic User Message
    const newUserMsg: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: text,
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, newUserMsg]);
    setInputValue('');
    setIsTyping(true);

    try {
      let currentConversationId = conversationId;

      // 2. If no conversation exists, create one
      if (!currentConversationId) {
        const title = await generateTitleFromText(text);
        const newConv = await createConversation(title);
        currentConversationId = newConv.id;
        onConversationCreated(newConv);
      }

      // 3. Create Placeholder AI Message (Typing)
      // We don't need a placeholder message in the list if we just show a typing indicator
      // But preserving the UX of "Answer incoming..." might be desired.
      // However, for standard REST, usually we wait.
      // Let's rely on `isTyping` to show the dots at the bottom.

      // 4. Send Request
      const aiResponse = await createMessage(currentConversationId, text);

      // 5. Append AI Message
      const aiMsg: Message = {
        id: aiResponse.id,
        role: 'ai',
        content: aiResponse.text,
        timestamp: new Date(aiResponse.createdAt),
      };
      setMessages((prev) => [...prev, aiMsg]);
      setIsTyping(false);

      // Check if this is the first exchange (1 user message + 1 AI message = 2 total)
      // Since we just added the AI message, and state update is async, we check if current messages length is 1 (the user message) 
      // OR we just rely on logic that we added 2 messages in this functions scope.
      // Safest heuristic here: if we started with 0 messages.
      if (messages.length === 0) {
        // Run in background
        generateTitle(currentConversationId, text).then((newTitle) => {
          if (newTitle) {
            // Update history state so sidebar reflects it immediately
            setHistory(prev => prev.map(conv =>
              conv.id === currentConversationId ? { ...conv, title: newTitle } : conv
            ));
          }
        });
      }

    } catch (error) {
      console.error("Failed to send message:", error);
      setIsTyping(false);
      // Optional: Add error message to chat
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage(inputValue);
    }
  };

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setIsUploading(true);
    try {
      await uploadDocument(file);

      // Add a system message locally to confirm upload
      const successMsg: Message = {
        id: Date.now().toString(),
        role: 'ai',
        content: `Document "${file.name}" uploaded successfully.`,
        timestamp: new Date()
      };
      setMessages(prev => [...prev, successMsg]);

    } catch (error: any) {
      console.error("Upload failed", error);
      alert(error.message || "Failed to upload document");
    } finally {
      setIsUploading(false);
      // Reset input
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  return (
    <div className="flex h-screen w-full bg-slate-950 text-slate-100 font-sans overflow-hidden">
      {/* --- Sidebar --- */}
      <AnimatePresence mode='wait'>
        {sidebarOpen && (
          <motion.aside
            initial={{ width: 0, opacity: 0 }}
            animate={{ width: 280, opacity: 1 }}
            exit={{ width: 0, opacity: 0 }}
            className="h-full bg-slate-900 border-r border-slate-800 flex flex-col z-20"
          >
            <div className="p-4 border-b border-slate-800 flex items-center justify-between">
              <div className="flex items-center gap-2 font-semibold text-emerald-400 tracking-tight">
                <Activity size={20} />
                <span>DOCUCHAT</span>
              </div>
              <button onClick={() => setSidebarOpen(false)} className="text-slate-500 hover:text-slate-300 md:hidden">
                <X size={20} />
              </button>
            </div>

            <div className="p-4 gap-2 flex">
              <button
                onClick={() => {
                  setMessages([]);
                  onSelectConversation(null);
                  if (window.innerWidth < 768) setSidebarOpen(false);
                }}
                className="flex-1 flex items-center justify-center gap-2 bg-emerald-600/10 text-emerald-400 hover:bg-emerald-600/20 border border-emerald-600/20 p-3 rounded-lg transition-all duration-200 group"
              >
                <Plus size={18} className="group-hover:scale-110 transition-transform" />
                <span className="font-medium text-sm">New Analysis</span>
              </button>
              <button
                onClick={() => setIsSearchOpen(true)}
                className="flex items-center justify-center bg-slate-800 text-slate-400 hover:text-emerald-400 hover:bg-slate-700 border border-slate-700 p-3 rounded-lg transition-all duration-200"
                title="Search Conversations"
              >
                <Search size={18} />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto px-4 py-2 space-y-4">
              <div>
                <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-3">Recent</h3>
                <ul className="space-y-1">
                  {history.map((conv) => (
                    <li key={conv.id}>
                      <button
                        onClick={() => {
                          onSelectConversation(conv.id);
                          if (window.innerWidth < 768) setSidebarOpen(false);
                        }}
                        className={cn(
                          "w-full text-left px-3 py-2 text-sm rounded-md transition-colors truncate flex items-center gap-2",
                          conversationId === conv.id
                            ? "bg-slate-800 text-emerald-400"
                            : "text-slate-400 hover:bg-slate-800 hover:text-slate-200"
                        )}
                      >
                        <TrendingUp size={14} className={cn("shrink-0", conversationId === conv.id ? "opacity-100" : "opacity-50")} />
                        <span className="truncate">{conv.title || "Untitled Analysis"}</span>
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="p-4 border-t border-slate-800">
              <button
                onClick={() => navigate('/profile')}
                className="flex items-center gap-3 px-2 w-full hover:bg-slate-800 p-2 rounded-lg transition-colors text-left group"
              >
                <div className="w-8 h-8 rounded-full bg-slate-700 flex items-center justify-center text-xs font-bold text-slate-300 group-hover:text-emerald-400 transition-colors">
                  {user ? (
                    (user.firstName || user.lastName)
                      ? `${(user.firstName?.[0] || '').toUpperCase()}${(user.lastName?.[0] || '').toUpperCase()} `
                      : (user.username?.[0] || '').toUpperCase()
                  ) : 'JD'}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-slate-200 truncate group-hover:text-emerald-400 transition-colors">
                    {user ? (
                      (user.firstName || user.lastName)
                        ? `${user.firstName || ''} ${user.lastName || ''} `.trim()
                        : user.username
                    ) : 'Loading...'}
                  </p>
                  <p className="text-xs text-slate-500 truncate">Pro Plan</p>
                </div>
                <div className="text-slate-500 hover:text-slate-300">
                  <MoreHorizontal size={18} />
                </div>
              </button>
            </div>
          </motion.aside>
        )}
      </AnimatePresence>

      {/* --- Main Content --- */}
      <main className="flex-1 flex flex-col relative min-w-0">

        {/* Header */}
        <header className="h-16 border-b border-slate-800 bg-slate-950/80 backdrop-blur-md flex items-center justify-between px-6 sticky top-0 z-10 w-full">
          <div className="flex items-center gap-4">
            {!sidebarOpen && (
              <button onClick={() => setSidebarOpen(true)} className="text-slate-400 hover:text-slate-200">
                <Menu size={20} />
              </button>
            )}
            <div>
              <h2 className="font-semibold text-slate-100">Financial Insight Chatbot</h2>
              <div className="flex items-center gap-2">
                <span className="relative flex h-2 w-2">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
                </span>
                <span className="text-xs text-slate-400 font-mono">Market Data: Live</span>
              </div>
            </div>
          </div>
          {/* Right Header Actions */}
          <div className="flex items-center gap-3">
            <button className="px-3 py-1.5 text-xs font-medium bg-slate-900 border border-slate-700 rounded-full text-slate-400 hover:text-slate-200 transition-colors">
              v0.0.1
            </button>
          </div>
        </header>

        {/* Chat Area */}
        <div className="flex-1 overflow-y-auto p-4 md:p-6 pb-32 md:pb-40 space-y-6 scrollbar-thin scrollbar-thumb-slate-800 scrollbar-track-transparent [mask-image:linear-gradient(to_bottom,black_60%,transparent_100%)]">
          {messages.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center max-w-4xl mx-auto w-full animate-fade-in">
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
                className="text-center mb-12"
              >
                <div className="w-16 h-16 bg-gradient-to-br from-emerald-500 to-emerald-700 rounded-2xl flex items-center justify-center mx-auto mb-6 shadow-lg shadow-emerald-500/20">
                  <Activity size={32} className="text-white" />
                </div>
                <h1 className="text-3xl md:text-4xl font-bold text-slate-100 mb-4 tracking-tight">
                  {greeting}
                </h1>
                <p className="text-slate-400 text-lg">
                  How is the market treating you today?
                </p>
              </motion.div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 w-full px-4">
                {suggestions.map((card, idx) => {
                  const Icon = iconMap[card.iconName] || TrendingUp;
                  return (
                    <motion.button
                      key={card.id}
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: idx * 0.1, duration: 0.4 }}
                      onClick={() => handleSendMessage(card.fullPrompt)}
                      className="flex items-start gap-4 p-5 bg-slate-900/50 hover:bg-slate-800/80 border border-slate-800 hover:border-emerald-500/30 rounded-xl text-left transition-all duration-300 group ring-1 ring-transparent hover:ring-emerald-500/10"
                    >
                      <div className="p-3 rounded-lg bg-slate-800 group-hover:bg-slate-700 text-emerald-400 transition-colors">
                        <Icon size={20} />
                      </div>
                      <div>
                        <h3 className="font-semibold text-slate-200 group-hover:text-emerald-400 transition-colors">{card.title}</h3>
                        <p className="text-sm text-slate-500 mt-1 line-clamp-2">{card.description}</p>
                      </div>
                    </motion.button>
                  );
                })}
              </div>
            </div>
          ) : (
            <div className="max-w-3xl mx-auto w-full space-y-6 pb-4">
              {messages.map((msg) => (
                <motion.div
                  key={msg.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className={cn(
                    "flex gap-4 w-full",
                    msg.role === 'user' ? "justify-end" : "justify-start"
                  )}
                >
                  {msg.role === 'ai' && (
                    <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-emerald-600 to-teal-800 flex flex-shrink-0 items-center justify-center mt-1 shadow-md">
                      <Bot size={16} className="text-white" />
                    </div>
                  )}

                  <div className={cn(
                    "relative px-5 py-3.5 max-w-[85%] md:max-w-[75%] space-y-1 shadow-sm",
                    msg.role === 'user'
                      ? "bg-slate-700 text-slate-100 rounded-2xl rounded-tr-sm"
                      : "bg-slate-900 border border-slate-800 text-slate-200 rounded-2xl rounded-tl-sm w-full"
                  )}>
                    {msg.role === 'user' ? (
                      <div className="text-sm leading-relaxed whitespace-pre-wrap font-sans">{msg.content}</div>
                    ) : (
                      <RichTextRenderer content={msg.content as string} />
                    )}

                    <div className={cn(
                      "text-[10px] opacity-50 mt-1 text-right font-mono",
                      msg.role === 'user' ? "text-slate-300" : "text-slate-500"
                    )}>
                      {msg.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </div>
                  </div>

                  {msg.role === 'user' && (
                    <div className="w-8 h-8 rounded-lg bg-slate-600 flex flex-shrink-0 items-center justify-center mt-1">
                      <User size={16} className="text-slate-300" />
                    </div>
                  )}
                </motion.div>
              ))}

              {isTyping && (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="flex gap-4"
                >
                  <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-emerald-600 to-teal-800 flex flex-shrink-0 items-center justify-center mt-1">
                    <Bot size={16} className="text-white" />
                  </div>
                  <div className="bg-slate-900 border border-slate-800 px-4 py-3 rounded-2xl rounded-tl-sm flex gap-1.5 items-center">
                    <span className="w-1.5 h-1.5 bg-slate-500 rounded-full animate-bounce [animation-delay:-0.3s]"></span>
                    <span className="w-1.5 h-1.5 bg-slate-500 rounded-full animate-bounce [animation-delay:-0.15s]"></span>
                    <span className="w-1.5 h-1.5 bg-slate-500 rounded-full animate-bounce"></span>
                  </div>
                </motion.div>
              )}
              <div ref={messagesEndRef} />
            </div>
          )}
        </div>

        {/* Input Area */}
        <div className="p-4 bg-transparent absolute bottom-0 left-0 right-0 pointer-events-none flex justify-center z-10">
          <div className="w-full max-w-3xl pointer-events-auto">
            <div className="relative group">
              <div className="absolute -inset-0.5 bg-gradient-to-r from-emerald-500/20 to-blue-500/20 rounded-xl blur opacity-20 group-hover:opacity-40 transition duration-500"></div>

              <div className="relative flex flex-col gap-2 bg-slate-900/90 backdrop-blur-xl border border-slate-700/50 rounded-xl p-2 shadow-2xl">

                <div className="flex items-center gap-2 overflow-x-auto px-2 py-1 scrollbar-none">
                  <button
                    onClick={() => setInputValue("Summarize the key points of the uploaded PDF.")}
                    className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-800 text-xs font-medium text-emerald-400 border border-slate-700 rounded-full hover:bg-slate-700 transition-colors whitespace-nowrap"
                  >
                    <FileText size={14} />
                    <span>Summarize</span>
                  </button>
                  <button
                    onClick={() => setInputValue("Extract key financial metrics from the document.")}
                    className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-800 text-xs font-medium text-emerald-400 border border-slate-700 rounded-full hover:bg-slate-700 transition-colors whitespace-nowrap"
                  >
                    <TrendingUp size={14} />
                    <span>Key Metrics</span>
                  </button>
                  <button
                    onClick={() => setInputValue("Identify potential risks mentioned in the document.")}
                    className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-800 text-xs font-medium text-emerald-400 border border-slate-700 rounded-full hover:bg-slate-700 transition-colors whitespace-nowrap"
                  >
                    <AlertTriangle size={14} />
                    <span>Risks</span>
                  </button>
                </div>

                <div className="flex items-end gap-2 px-2 pb-1">
                  <input
                    type="file"
                    ref={fileInputRef}
                    className="hidden"
                    accept="application/pdf"
                    onChange={handleFileUpload}
                  />
                  <button
                    onClick={() => !isUploading && fileInputRef.current?.click()}
                    disabled={isUploading}
                    className={cn(
                      "p-2 text-slate-400 hover:text-emerald-400 hover:bg-slate-800 rounded-lg transition-colors mb-0.5",
                      isUploading && "opacity-50 cursor-not-allowed"
                    )}
                  >
                    {isUploading ? <Loader size={20} className="animate-spin text-emerald-400" /> : <Paperclip size={20} />}
                  </button>

                  <textarea
                    ref={(el) => {
                      if (el) {
                        el.style.height = 'auto';
                        el.style.height = el.scrollHeight + 'px';
                      }
                    }}
                    value={inputValue}
                    onChange={(e) => {
                      setInputValue(e.target.value);
                      e.target.style.height = 'auto';
                      e.target.style.height = e.target.scrollHeight + 'px';
                    }}
                    onKeyDown={handleKeyDown}
                    placeholder="Ask about stocks, crypto, or upload a financial report..."
                    className="flex-1 bg-transparent text-slate-200 placeholder-slate-500 focus:outline-none resize-none py-3 max-h-40 min-h-[44px] scrollbar-thin scrollbar-thumb-slate-700 scrollbar-track-transparent leading-relaxed"
                    rows={1}
                  />

                  {inputValue.trim() ? (
                    <button
                      onClick={() => handleSendMessage(inputValue)}
                      className="p-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg shadow-lg shadow-emerald-500/25 transition-all duration-200 mb-0.5"
                    >
                      <Send size={18} />
                    </button>
                  ) : (
                    <button className="p-2 text-slate-500 cursor-not-allowed rounded-lg transition-colors mb-0.5" disabled>
                      <Send size={18} />
                    </button>
                  )}
                </div>
              </div>
            </div>
            <p className="text-center text-[10px] text-slate-600 mt-2 font-mono">
              AI suggestions are for informational purposes only. Not financial advice.
            </p>
          </div>
        </div>
      </main>
      <SearchModal
        isOpen={isSearchOpen}
        onClose={() => setIsSearchOpen(false)}
        conversations={history}
        setActiveConversationId={onSelectConversation}
      />
    </div>
  );
}