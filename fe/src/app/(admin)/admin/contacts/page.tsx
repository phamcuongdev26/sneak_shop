"use client";
import { useEffect, useState } from "react";
import Image from "next/image";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { MessageSquare, Mail, Clock, CheckCircle2 } from "lucide-react";
import { contactsApi, type ContactItem } from "@/lib/api/contacts";
import { formatDate } from "@/lib/format";

const TABS = [
  { value: "", label: "Tất cả" },
  { value: "pending", label: "Chờ xử lý" },
  { value: "replied", label: "Đã trả lời" },
];

export default function AdminContactsPage() {
  const [contacts, setContacts] = useState<ContactItem[]>([]);
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selected, setSelected] = useState<ContactItem | null>(null);
  const [replyText, setReplyText] = useState("");
  const [replying, setReplying] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const r = await contactsApi.adminGetAll({ status: status || undefined, page, size: 20 });
      setContacts(r.data.result.content);
      setTotalPages(r.data.result.totalPages);
    } catch {}
    setLoading(false);
  };

  useEffect(() => { load(); }, [status, page]);

  const openDetail = (c: ContactItem) => {
    setSelected(c);
    setReplyText(c.replyText ?? "");
  };

  const handleReply = async () => {
    if (!selected || !replyText.trim()) { toast.error("Vui lòng nhập nội dung phản hồi"); return; }
    setReplying(true);
    try {
      const r = await contactsApi.adminReply(selected.id, replyText.trim());
      toast.success("Đã gửi phản hồi qua email");
      setSelected(r.data.result);
      setContacts((prev) => prev.map((c) => c.id === r.data.result.id ? r.data.result : c));
    } catch {
      toast.error("Gửi phản hồi thất bại");
    }
    setReplying(false);
  };

  const imageList = (raw: string | null): string[] => {
    if (!raw) return [];
    try { return JSON.parse(raw); } catch { return []; }
  };

  return (
    <div className="p-6 max-w-5xl mx-auto space-y-5">
      <div className="flex items-center gap-3">
        <MessageSquare className="w-6 h-6" />
        <h1 className="text-2xl font-bold">Liên hệ</h1>
      </div>

      {/* Tabs */}
      <div className="flex gap-1">
        {TABS.map((t) => (
          <button key={t.value} onClick={() => { setStatus(t.value); setPage(0); }}
            className={`px-4 py-2 rounded-full text-sm font-medium transition ${status === t.value ? "bg-black text-white" : "bg-gray-100 text-gray-600 hover:bg-gray-200"}`}>
            {t.label}
          </button>
        ))}
      </div>

      {/* List */}
      {loading ? (
        <div className="space-y-3">{[1,2,3,4].map((i) => <Skeleton key={i} className="h-20 rounded-xl" />)}</div>
      ) : contacts.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <MessageSquare className="w-10 h-10 mx-auto mb-2 opacity-20" />
          <p>Không có liên hệ nào</p>
        </div>
      ) : (
        <div className="space-y-2">
          {contacts.map((c) => (
            <button key={c.id} onClick={() => openDetail(c)}
              className="w-full text-left bg-white border rounded-xl px-4 py-3 hover:shadow-sm transition flex items-start gap-4">
              {/* Avatar */}
              <div className="w-9 h-9 rounded-full bg-gray-100 flex items-center justify-center flex-shrink-0 text-sm font-bold text-gray-500 mt-0.5">
                {c.name?.[0]?.toUpperCase() ?? "?"}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-semibold text-sm text-gray-900">{c.name}</span>
                  <span className="text-xs text-gray-400">{c.email}</span>
                  <Badge variant={c.status === "replied" ? "default" : "secondary"} className="text-[10px] h-4 px-1.5">
                    {c.status === "replied" ? "Đã trả lời" : "Chờ xử lý"}
                  </Badge>
                </div>
                <p className="text-sm font-medium text-gray-700 mt-0.5 truncate">{c.subject}</p>
                <p className="text-xs text-gray-400 truncate mt-0.5">{c.message}</p>
              </div>
              <span className="text-xs text-gray-400 flex-shrink-0 mt-0.5">{formatDate(c.createdAt)}</span>
            </button>
          ))}

          {totalPages > 1 && (
            <div className="flex justify-center gap-2 pt-2">
              <Button variant="outline" disabled={page === 0} onClick={() => setPage((p) => p - 1)} className="rounded-xl">Trước</Button>
              <span className="flex items-center px-3 text-sm text-gray-600">{page + 1} / {totalPages}</span>
              <Button variant="outline" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)} className="rounded-xl">Tiếp</Button>
            </div>
          )}
        </div>
      )}

      {/* Detail Dialog */}
      <Dialog open={!!selected} onOpenChange={(o) => !o && setSelected(null)}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          {selected && (
            <>
              <DialogHeader>
                <DialogTitle className="flex items-center gap-2">
                  <span>{selected.subject}</span>
                  <Badge variant={selected.status === "replied" ? "default" : "secondary"}>
                    {selected.status === "replied" ? "Đã trả lời" : "Chờ xử lý"}
                  </Badge>
                </DialogTitle>
              </DialogHeader>

              {/* Sender info */}
              <div className="flex items-center gap-3 py-2 border-b">
                <div className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center font-bold text-gray-500">
                  {selected.name?.[0]?.toUpperCase()}
                </div>
                <div>
                  <p className="font-semibold text-sm">{selected.name}</p>
                  <p className="text-xs text-gray-500 flex items-center gap-1"><Mail className="w-3 h-3" />{selected.email}</p>
                </div>
                <span className="ml-auto text-xs text-gray-400 flex items-center gap-1"><Clock className="w-3 h-3" />{formatDate(selected.createdAt)}</span>
              </div>

              {/* Message */}
              <div className="bg-gray-50 rounded-xl p-4 text-sm text-gray-800 whitespace-pre-wrap leading-relaxed">
                {selected.message}
              </div>

              {/* Images */}
              {imageList(selected.imageUrls).length > 0 && (
                <div className="flex flex-wrap gap-2">
                  {imageList(selected.imageUrls).map((url, i) => (
                    <a key={i} href={url} target="_blank" rel="noreferrer">
                      <Image src={url} alt="" width={80} height={80} className="rounded-lg border object-cover w-20 h-20" />
                    </a>
                  ))}
                </div>
              )}

              {/* Existing reply */}
              {selected.replyText && (
                <div className="bg-blue-50 border border-blue-100 rounded-xl p-4">
                  <p className="text-xs font-medium text-blue-600 flex items-center gap-1 mb-2">
                    <CheckCircle2 className="w-3.5 h-3.5" /> Phản hồi đã gửi — {formatDate(selected.repliedAt!)}
                  </p>
                  <p className="text-sm text-gray-700 whitespace-pre-wrap">{selected.replyText}</p>
                </div>
              )}

              {/* Reply box */}
              <div className="space-y-2 pt-2 border-t">
                <label className="text-sm font-medium text-gray-700">
                  {selected.replyText ? "Gửi phản hồi mới" : "Phản hồi"}
                </label>
                <Textarea
                  value={replyText}
                  onChange={(e) => setReplyText(e.target.value)}
                  rows={4}
                  placeholder="Nhập nội dung phản hồi..."
                />
                <Button onClick={handleReply} disabled={replying || !replyText.trim()} className="w-full">
                  {replying ? "Đang gửi..." : "Gửi phản hồi qua email"}
                </Button>
              </div>
            </>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
