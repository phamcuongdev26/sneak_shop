"use client";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Star } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";
import { reviewsApi } from "@/lib/api/reviews";
import { formatDate } from "@/lib/format";
import type { Review } from "@/lib/types";

export default function AdminReviewsPage() {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selected, setSelected] = useState<Review | null>(null);
  const [reply, setReply] = useState("");
  const [replying, setReplying] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const r = await reviewsApi.adminGetAll({ page, size: 20 });
      setReviews(r.data.result.content);
      setTotalPages(r.data.result.totalPages);
    } catch {}
    setLoading(false);
  };

  useEffect(() => { load(); }, [page]);

  const handleReply = async () => {
    if (!selected || !reply.trim()) return;
    setReplying(true);
    try {
      await reviewsApi.shopReply(selected.id, reply);
      toast.success("Đã gửi phản hồi");
      setSelected(null);
      load();
    } catch { toast.error("Có lỗi xảy ra"); }
    setReplying(false);
  };

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Quản lý đánh giá</h1>

      <div className="bg-white rounded-xl border overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b">
            <tr>
              {["Người dùng", "Sản phẩm", "Đánh giá", "Bình luận", "Phản hồi", "Ngày", ""].map((h) => (
                <th key={h} className="text-left py-3 px-4 text-xs font-semibold text-gray-500">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 8 }).map((_, i) => (
                <tr key={i}><td colSpan={7} className="px-4 py-3"><Skeleton className="h-5" /></td></tr>
              ))
            ) : reviews.length === 0 ? (
              <tr><td colSpan={7} className="text-center py-12 text-gray-400">Chưa có đánh giá</td></tr>
            ) : (
              reviews.map((r) => (
                <tr key={r.id} className="border-b last:border-0 hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium">{r.userName}</td>
                  <td className="px-4 py-3 text-gray-500">#{r.productId}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-1">
                      {[1, 2, 3, 4, 5].map((s) => (
                        <Star key={s} className={`w-3 h-3 ${s <= r.rating ? "fill-yellow-400 text-yellow-400" : "text-gray-200"}`} />
                      ))}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-gray-600 max-w-48 truncate">{r.comment || "—"}</td>
                  <td className="px-4 py-3">
                    {r.shopReply ? (
                      <span className="text-green-600 text-xs">✓ Đã trả lời</span>
                    ) : (
                      <span className="text-gray-400 text-xs">Chưa trả lời</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-gray-400 text-xs">{formatDate(r.createdAt)}</td>
                  <td className="px-4 py-3">
                    <Button size="sm" variant="outline" onClick={() => { setSelected(r); setReply(r.shopReply || ""); }}>
                      {r.shopReply ? "Sửa" : "Trả lời"}
                    </Button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <Button variant="outline" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Trước</Button>
          <span className="flex items-center px-3 text-sm">{page + 1}/{totalPages}</span>
          <Button variant="outline" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>Tiếp</Button>
        </div>
      )}

      <Dialog open={!!selected} onOpenChange={() => setSelected(null)}>
        <DialogContent>
          <DialogHeader><DialogTitle>Phản hồi đánh giá</DialogTitle></DialogHeader>
          {selected && (
            <div className="space-y-4">
              <div className="bg-gray-50 p-3 rounded-lg text-sm">
                <p className="font-medium">{selected.userName}</p>
                <div className="flex gap-0.5 my-1">
                  {[1, 2, 3, 4, 5].map((s) => (
                    <Star key={s} className={`w-3 h-3 ${s <= selected.rating ? "fill-yellow-400 text-yellow-400" : "text-gray-200"}`} />
                  ))}
                </div>
                <p className="text-gray-600">{selected.comment}</p>
              </div>
              <Textarea
                value={reply}
                onChange={(e) => setReply(e.target.value)}
                rows={4}
              />
              <div className="flex gap-2 justify-end">
                <Button variant="outline" onClick={() => setSelected(null)}>Hủy</Button>
                <Button onClick={handleReply} disabled={replying}>{replying ? "Đang gửi..." : "Gửi"}</Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
