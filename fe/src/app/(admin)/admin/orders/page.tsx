"use client";
import { useEffect, useState } from "react";
import Image from "next/image";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { toast } from "sonner";
import { ordersApi } from "@/lib/api/orders";
import {
  formatVND, formatDate,
  ORDER_STATUS_LABEL, ORDER_STATUS_COLOR,
  PAYMENT_METHOD_LABEL,
} from "@/lib/format";
import type { Order } from "@/lib/types";

const ORDER_STATUSES = ["pending", "confirmed", "shipping", "delivered", "completed", "cancelled"];

const ALLOWED_NEXT: Record<string, string[]> = {
  pending:   ["confirmed", "cancelled"],
  confirmed: ["shipping",  "cancelled"],
  shipping:  ["delivered", "cancelled"],
  delivered: ["completed"],
  completed: [],
  cancelled: [],
};

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [status, setStatus] = useState("");
  const [keyword, setKeyword] = useState("");
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selected, setSelected] = useState<Order | null>(null);
  const [newStatus, setNewStatus] = useState("");
  const [cancelReason, setCancelReason] = useState("");
  const [updating, setUpdating] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const r = await ordersApi.adminGetAll({ status: status || undefined, keyword: keyword.trim() || undefined, page, size: 20 });
      setOrders(r.data.result.content);
      setTotalPages(r.data.result.totalPages);
    } catch {}
    setLoading(false);
  };

  useEffect(() => { load(); }, [status, keyword, page]);

  const handleUpdateStatus = async () => {
    if (!selected || !newStatus) return;
    if (newStatus === "cancelled" && !cancelReason.trim()) {
      toast.error("Vui lòng nhập lý do hủy đơn");
      return;
    }
    setUpdating(true);
    try {
      const r = await ordersApi.adminUpdateStatus(selected.orderCode, {
        status: newStatus,
        cancelReason: newStatus === "cancelled" ? cancelReason.trim() : undefined,
      });
      setOrders((prev) => prev.map((o) => o.id === r.data.result.id ? r.data.result : o));
      setSelected(null);
      setCancelReason("");
      toast.success("Cập nhật trạng thái thành công");
    } catch {
      toast.error("Không thể cập nhật");
    }
    setUpdating(false);
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">Quản lý đơn hàng</h1>
        <div className="flex items-center gap-2">
          <input
            type="text"
            className="border rounded-lg px-3 py-2 text-sm bg-white w-64"
            value={keyword}
            onChange={(e) => { setKeyword(e.target.value); setPage(0); }}
          />
          <Select value={status || "all"} onValueChange={(v) => { setStatus(!v || v === "all" ? "" : v); setPage(0); }}>
            <SelectTrigger className="w-44 bg-white">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả</SelectItem>
              {ORDER_STATUSES.map((s) => (
                <SelectItem key={s} value={s}>{ORDER_STATUS_LABEL[s] || s}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="bg-white rounded-xl border overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b">
            <tr>
              {["Mã đơn", "Khách hàng", "Sản phẩm", "Tổng tiền", "Phương thức", "Trạng thái", "Thời gian", ""].map((h) => (
                <th key={h} className="text-left py-3 px-4 text-xs font-semibold text-gray-500">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 8 }).map((_, i) => (
                <tr key={i}><td colSpan={8} className="px-4 py-3"><Skeleton className="h-5" /></td></tr>
              ))
            ) : orders.length === 0 ? (
              <tr><td colSpan={8} className="text-center py-12 text-gray-400">Không có đơn hàng</td></tr>
            ) : (
              orders.map((order) => (
                <tr key={order.id} className="border-b last:border-0 hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium">#{order.orderCode}</td>
                  <td className="px-4 py-3">
                    <p className="font-medium">{order.recipientName}</p>
                    <p className="text-gray-400 text-xs">{order.recipientPhone}</p>
                  </td>
                  <td className="px-4 py-3 text-gray-500">{order.items.length} sản phẩm</td>
                  <td className="px-4 py-3 font-medium">{formatVND(order.totalAmount)}</td>
                  <td className="px-4 py-3 text-gray-500">{PAYMENT_METHOD_LABEL[order.paymentMethod] || order.paymentMethod}</td>
                  <td className="px-4 py-3">
                    <Badge variant={ORDER_STATUS_COLOR[order.status]}>
                      {ORDER_STATUS_LABEL[order.status] || order.status}
                    </Badge>
                  </td>
                  <td className="px-4 py-3 text-gray-400 text-xs">{formatDate(order.createdAt)}</td>
                  <td className="px-4 py-3">
                    <Button size="sm" variant="outline" onClick={() => { setSelected(order); setNewStatus(ALLOWED_NEXT[order.status]?.[0] ?? order.status); setCancelReason(""); }}>
                      Cập nhật
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
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Đơn hàng #{selected?.orderCode}</DialogTitle>
          </DialogHeader>

          {selected && (
            <div className="space-y-5">
              {/* Thông tin người nhận */}
              <div className="grid grid-cols-2 gap-3 text-sm bg-gray-50 rounded-lg p-4">
                <div><span className="text-gray-400">Người nhận:</span> <span className="font-medium">{selected.recipientName}</span></div>
                <div><span className="text-gray-400">SĐT:</span> <span className="font-medium">{selected.recipientPhone}</span></div>
                <div className="col-span-2"><span className="text-gray-400">Địa chỉ:</span> {selected.shippingAddress}, {selected.shippingCity}</div>
                <div><span className="text-gray-400">Thanh toán:</span> {PAYMENT_METHOD_LABEL[selected.paymentMethod] || selected.paymentMethod}</div>
                <div><span className="text-gray-400">Trạng thái TT:</span> {selected.paymentStatus === "paid" ? "Đã thanh toán" : "Chưa thanh toán"}</div>
                {selected.note && (
                  <div className="col-span-2"><span className="text-gray-400">Ghi chú:</span> <span className="text-gray-700">{selected.note}</span></div>
                )}
              </div>

              {/* Danh sách sản phẩm */}
              <div className="space-y-2">
                <p className="text-sm font-medium">Sản phẩm ({selected.items.length})</p>
                {selected.items.map((item) => (
                  <div key={item.id} className="flex items-center gap-3 border rounded-lg p-3">
                    {item.productImage && (
                      <Image src={item.productImage} alt={item.productName} width={48} height={48} className="rounded object-cover w-12 h-12 flex-shrink-0" />
                    )}
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-sm truncate">{item.productName}</p>
                      <p className="text-xs text-gray-400">
                        {[item.variantName, item.colorName].filter(Boolean).join(" · ")}
                      </p>
                    </div>
                    <div className="text-right text-sm flex-shrink-0">
                      <p className="font-medium">{formatVND(item.finalPrice)}</p>
                      <p className="text-gray-400 text-xs">x{item.quantity}</p>
                    </div>
                  </div>
                ))}
              </div>

              {/* Tổng tiền */}
              <div className="border-t pt-3 space-y-1 text-sm">
                <div className="flex justify-between text-gray-500"><span>Tạm tính</span><span>{formatVND(selected.subtotal)}</span></div>
                <div className="flex justify-between text-gray-500"><span>Phí ship</span><span>{formatVND(selected.shippingFee)}</span></div>
                {selected.discountAmount > 0 && <div className="flex justify-between text-green-600"><span>Giảm giá</span><span>-{formatVND(selected.discountAmount)}</span></div>}
                <div className="flex justify-between font-bold text-base pt-1 border-t"><span>Tổng cộng</span><span>{formatVND(selected.totalAmount)}</span></div>
              </div>

              {/* Cập nhật trạng thái */}
              {(ALLOWED_NEXT[selected.status] ?? []).length > 0 && (
                <div className="border-t pt-4 space-y-3">
                  <div className="flex items-center gap-3">
                    <Select value={newStatus} onValueChange={(v) => { setNewStatus(v ?? ""); setCancelReason(""); }}>
                      <SelectTrigger className="flex-1">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {(ALLOWED_NEXT[selected.status] ?? []).map((s) => (
                          <SelectItem key={s} value={s}>{ORDER_STATUS_LABEL[s] || s}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <Button onClick={handleUpdateStatus} disabled={updating || (newStatus === "cancelled" && !cancelReason.trim())}>
                      {updating ? "Đang lưu..." : "Cập nhật"}
                    </Button>
                  </div>
                  {newStatus === "cancelled" && (
                    <input
                      className="w-full border rounded-lg px-3 py-2 text-sm"
                      value={cancelReason}
                      onChange={(e) => setCancelReason(e.target.value)}
                      autoFocus
                    />
                  )}
                </div>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
