"use client";
import { useEffect, useRef, useState } from "react";
import { X, Send, Loader2, ChevronDown } from "lucide-react";
import { toast } from "sonner";
import { chatApi, type ChatMessage } from "@/lib/api/chat";
import { useChatStore } from "@/store/chat";
import { useAuthStore } from "@/store/auth";
import { getError } from "@/lib/api";

function MessengerIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 28 28" fill="none" className={className}>
      <path d="M14 2C7.373 2 2 7.03 2 13.2c0 3.388 1.564 6.418 4.04 8.48V26l3.817-2.117C11.2 24.27 12.573 24.5 14 24.5c6.627 0 12-5.03 12-11.3C26 7.03 20.627 2 14 2z" fill="currentColor" />
      <path d="M7 16.5l5-5.5 3.5 3.5 5-3.5-5 5.5-3.5-3.5-5 3.5z" fill="white" />
    </svg>
  );
}

export function ChatWidget() {
  const { isOpen, orderCode, openChat, closeChat } = useChatStore();
  const { user } = useAuthStore();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const effectiveCode = orderCode ?? (user ? `SUPPORT-${user.id}` : null);
  const isSupport = effectiveCode?.startsWith("SUPPORT-");

  const load = async (silent = false) => {
    if (!effectiveCode) return;
    if (!silent) setLoading(true);
    try {
      const r = await chatApi.getMessages(effectiveCode);
      setMessages(r.data.result ?? []);
    } catch (err) { if (!silent) toast.error(getError(err)); }
    if (!silent) setLoading(false);
  };

  useEffect(() => {
    if (!isOpen || !effectiveCode) return;
    load();
    pollRef.current = setInterval(() => load(true), 3000);
    return () => { if (pollRef.current) clearInterval(pollRef.current); };
  }, [isOpen, effectiveCode]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleBubbleClick = () => {
    if (!user) return;
    openChat(`SUPPORT-${user.id}`);
  };

  const handleSend = async () => {
    if (!input.trim() || !effectiveCode || sending) return;
    setSending(true);
    try {
      const r = await chatApi.sendMessage(effectiveCode, input.trim());
      setMessages((prev) => [...prev, r.data.result]);
      setInput("");
    } catch (err) { toast.error(getError(err)); }
    setSending(false);
  };

  const handleKey = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); void handleSend(); }
  };

  if (!user) return null;

  // Bubble khi đóng
  if (!isOpen) {
    return (
      <button
        onClick={handleBubbleClick}
        className="fixed bottom-6 right-6 z-[9998] w-14 h-14 rounded-full bg-blue-600 hover:bg-blue-700 shadow-xl hover:shadow-2xl hover:scale-105 active:scale-95 transition-all flex items-center justify-center text-white"
        aria-label="Chat hỗ trợ"
      >
        <MessengerIcon className="w-7 h-7" />
      </button>
    );
  }

  // Widget chat khi mở
  return (
    <div className="fixed bottom-5 right-5 z-[9999] flex flex-col w-80 h-[480px] rounded-2xl shadow-2xl border border-gray-200 overflow-hidden bg-white">
      {/* Header */}
      <div className="flex items-center gap-2.5 px-4 py-3 bg-gradient-to-r from-blue-600 to-blue-500 text-white flex-shrink-0">
        <div className="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center flex-shrink-0">
          <MessengerIcon className="w-5 h-5" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="font-semibold text-sm leading-none">Sneak Shop</p>
          <p className="text-[10px] text-blue-100 mt-0.5 truncate">
            {isSupport ? "Hỗ trợ trực tuyến" : effectiveCode}
          </p>
        </div>
        <button onClick={closeChat} className="w-7 h-7 rounded-full hover:bg-white/20 flex items-center justify-center transition">
          <ChevronDown className="w-4 h-4" />
        </button>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-3 py-3 space-y-2 bg-gray-50">
        {loading ? (
          <div className="flex items-center justify-center h-full">
            <Loader2 className="w-5 h-5 animate-spin text-gray-400" />
          </div>
        ) : messages.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full gap-3 text-gray-400 px-4 text-center">
            <MessengerIcon className="w-10 h-10 text-blue-200" />
            <div>
              <p className="text-sm font-medium text-gray-600">Chào mừng đến Sneak Shop!</p>
              <p className="text-xs mt-1">Nhắn tin để được hỗ trợ nhanh nhất.</p>
            </div>
          </div>
        ) : (
          messages.map((msg) => {
            const isMe = msg.senderRole === "USER";
            return (
              <div key={msg.id} className={`flex items-end gap-1.5 ${isMe ? "flex-row-reverse" : "flex-row"}`}>
                {!isMe && (
                  <div className="w-6 h-6 rounded-full bg-blue-600 flex items-center justify-center flex-shrink-0 text-white mb-0.5 overflow-hidden">
                    <MessengerIcon className="w-4 h-4" />
                  </div>
                )}
                <div className={`max-w-[75%] px-3 py-2 rounded-2xl text-sm leading-snug ${
                  isMe
                    ? "bg-blue-600 text-white rounded-br-sm"
                    : "bg-white text-gray-800 border border-gray-200 rounded-bl-sm shadow-sm"
                }`}>
                  {msg.content}
                </div>
              </div>
            );
          })
        )}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <div className="flex items-center gap-2 px-3 py-2.5 border-t bg-white flex-shrink-0">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKey}
          placeholder="Nhập tin nhắn..."
          className="flex-1 text-sm bg-gray-100 rounded-full px-4 py-2 outline-none focus:ring-2 focus:ring-blue-400 transition"
        />
        <button
          onClick={() => void handleSend()}
          disabled={!input.trim() || sending}
          className="w-9 h-9 rounded-full bg-blue-600 hover:bg-blue-700 disabled:opacity-40 flex items-center justify-center text-white transition flex-shrink-0"
        >
          {sending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
        </button>
      </div>
    </div>
  );
}
