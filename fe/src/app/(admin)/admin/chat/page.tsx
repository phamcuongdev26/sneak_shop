"use client";
import { useEffect, useRef, useState } from "react";
import { Loader2, MessageSquare, Send } from "lucide-react";
import { toast } from "sonner";
import { Skeleton } from "@/components/ui/skeleton";
import { chatApi, type ChatMessage, type Conversation } from "@/lib/api/chat";
import { formatDate } from "@/lib/format";
import { useRealtimeSocket } from "@/lib/useRealtimeSocket";

export default function AdminChatPage() {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [loadingConvs, setLoadingConvs] = useState(true);
  const [loadingMsgs, setLoadingMsgs] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const loadConversations = async () => {
    try {
      const r = await chatApi.adminGetConversations();
      setConversations(r.data.result ?? []);
    } catch {}
    setLoadingConvs(false);
  };

  const loadMessages = async (orderCode: string, silent = false) => {
    if (!silent) setLoadingMsgs(true);
    try {
      const r = await chatApi.adminGetMessages(orderCode);
      setMessages(r.data.result ?? []);
      // Mark read → refresh unread counts
      if (!silent) loadConversations();
    } catch {}
    if (!silent) setLoadingMsgs(false);
  };

  useRealtimeSocket(true, (event) => {
    if (event.channel !== "chat" || !event.orderCode) return;
    void loadConversations();
    if (selected === event.orderCode) {
      void loadMessages(event.orderCode, true);
    }
  });

  useEffect(() => { loadConversations(); }, []);

  useEffect(() => {
    if (!selected) return;
    loadMessages(selected);
    pollRef.current = setInterval(() => {
      loadMessages(selected, true);
      loadConversations();
    }, 3000);
    return () => { if (pollRef.current) clearInterval(pollRef.current); };
  }, [selected]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSelect = (orderCode: string) => {
    if (pollRef.current) clearInterval(pollRef.current);
    setMessages([]);
    setSelected(orderCode);
  };

  const handleSend = async () => {
    if (!input.trim() || !selected || sending) return;
    setSending(true);
    try {
      await chatApi.adminSendMessage(selected, input.trim());
      setInput("");
      await loadMessages(selected, true);
      loadConversations();
    } catch { toast.error("Gửi thất bại"); }
    setSending(false);
  };

  return (
    <div className="flex h-[calc(100vh-64px)]">
      {/* Left: Conversation list */}
      <div className="w-72 border-r flex flex-col bg-white flex-shrink-0">
        <div className="px-4 py-4 border-b">
          <h1 className="font-bold text-lg flex items-center gap-2">
            <MessageSquare className="w-5 h-5" /> Chat hỗ trợ
          </h1>
        </div>
        <div className="flex-1 overflow-y-auto">
          {loadingConvs ? (
            <div className="p-3 space-y-2">{[1,2,3].map((i) => <Skeleton key={i} className="h-14 rounded-xl" />)}</div>
          ) : conversations.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-gray-400 text-sm gap-2 py-10">
              <MessageSquare className="w-8 h-8 opacity-20" />
              Chưa có cuộc trò chuyện
            </div>
          ) : (
            conversations.map((c) => (
              <button key={c.orderCode} onClick={() => handleSelect(c.orderCode)}
                className={`w-full text-left px-4 py-3 flex items-start gap-3 hover:bg-gray-50 transition border-b border-gray-50 ${selected === c.orderCode ? "bg-blue-50" : ""}`}>
                <div className="w-9 h-9 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0 text-blue-600 font-bold text-sm mt-0.5">
                  {c.displayName.trim().slice(0, 2).toUpperCase()}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-gray-800 truncate">{c.displayName}</span>
                    {c.unreadCount > 0 && (
                      <span className="flex-shrink-0 ml-1 w-5 h-5 rounded-full bg-blue-600 text-white text-[10px] font-bold flex items-center justify-center">
                        {c.unreadCount}
                      </span>
                    )}
                  </div>
                  <p className="text-[10px] text-gray-400 truncate">{c.orderCode}</p>
                  <p className="text-xs text-gray-500 truncate mt-0.5">
                    {c.lastSenderRole === "ADMIN" ? "Bạn: " : ""}{c.lastContent}
                  </p>
                  <p className="text-[10px] text-gray-400 mt-0.5">{formatDate(c.lastTime)}</p>
                </div>
              </button>
            ))
          )}
        </div>
      </div>

      {/* Right: Messages */}
      <div className="flex-1 flex flex-col bg-gray-50">
        {!selected ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-400 gap-3">
            <MessageSquare className="w-12 h-12 opacity-20" />
            <p className="text-sm">Chọn một cuộc trò chuyện để bắt đầu</p>
          </div>
        ) : (
          <>
            {/* Header */}
            <div className="px-5 py-3.5 bg-white border-b flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-bold text-xs">
                {conversations.find((c) => c.orderCode === selected)?.displayName.trim().slice(0, 2).toUpperCase() || "CS"}
              </div>
              <div>
                <p className="font-semibold text-sm">{conversations.find((c) => c.orderCode === selected)?.displayName || selected}</p>
                <p className="text-xs text-gray-400">{selected}</p>
              </div>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto px-4 py-4 space-y-2">
              {loadingMsgs ? (
                <div className="flex items-center justify-center h-full"><Loader2 className="w-5 h-5 animate-spin text-gray-400" /></div>
              ) : messages.map((msg) => {
                const isAdmin = msg.senderRole === "ADMIN";
                return (
                  <div key={msg.id} className={`flex items-end gap-2 ${isAdmin ? "flex-row-reverse" : "flex-row"}`}>
                    {!isAdmin && (
                      <div className="w-6 h-6 rounded-full bg-gray-300 flex items-center justify-center text-[9px] font-bold text-white flex-shrink-0 mb-0.5">U</div>
                    )}
                    <div className={`max-w-[65%] px-3 py-2 rounded-2xl text-sm leading-snug ${isAdmin ? "bg-blue-600 text-white rounded-br-sm" : "bg-white text-gray-800 border border-gray-200 rounded-bl-sm"}`}>
                      <p>{msg.content}</p>
                      <p className={`text-[10px] mt-1 ${isAdmin ? "text-blue-200" : "text-gray-400"}`}>{formatDate(msg.createdAt)}</p>
                    </div>
                  </div>
                );
              })}
              <div ref={bottomRef} />
            </div>

            {/* Input */}
            <div className="px-4 py-3 bg-white border-t flex items-center gap-2">
              <input
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); void handleSend(); } }}
                placeholder="Nhập phản hồi..."
                className="flex-1 text-sm bg-gray-100 rounded-full px-4 py-2.5 outline-none focus:ring-2 focus:ring-blue-400 transition"
              />
              <button onClick={() => void handleSend()} disabled={!input.trim() || sending}
                className="w-10 h-10 rounded-full bg-blue-600 hover:bg-blue-700 disabled:opacity-40 flex items-center justify-center text-white transition flex-shrink-0">
                {sending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
