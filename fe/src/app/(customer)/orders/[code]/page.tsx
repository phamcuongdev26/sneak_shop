"use client";
import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { cartApi } from "@/lib/api/cart";
import { ordersApi } from "@/lib/api/orders";
import { reviewsApi } from "@/lib/api/reviews";
import { useAuthStore } from "@/store/auth";
import { useCartStore } from "@/store/cart";
import { useChatStore } from "@/store/chat";
import {
  formatVND, formatDate,
  ORDER_STATUS_LABEL, ORDER_STATUS_COLOR,
  PAYMENT_METHOD_LABEL, PAYMENT_STATUS_LABEL,
} from "@/lib/format";
import type { Order, OrderItem } from "@/lib/types";

export default function OrderDetailPage() {
  const { code } = useParams<{ code: string }>();
  const { user } = useAuthStore();
  const router = useRouter();
  const { openChat } = useChatStore();
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState(false);
  const [showCancelDialog, setShowCancelDialog] = useState(false);
  const [cancelReason, setCancelReason] = useState("");
  const [reviewingItem, setReviewingItem] = useState<OrderItem | null>(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState("");
  const [reviewSubmitting, setReviewSubmitting] = useState(false);
  const [reviewedOrderItemIds, setReviewedOrderItemIds] = useState<Set<number>>(new Set());
  const [showCancelInfoDialog, setShowCancelInfoDialog] = useState(false);
  const [repurchasing, setRepurchasing] = useState(false);
  const { setItems } = useCartStore();

  useEffect(() => {
    if (!user) { router.push("/login"); return; }
    Promise.all([
      ordersApi.getMyOrder(code),
      reviewsApi.getMyReviews({ page: 0, size: 100 }),
    ])
      .then(([orderRes, reviewRes]) => {
        const nextOrder = orderRes.data.result;
        setOrder(nextOrder);
        const reviewed = new Set(
          reviewRes.data.result.content
            .map((review) => review.orderItemId)
            .filter((id): id is number => typeof id === "number" && nextOrder.items.some((item) => item.id === id))
        );
        setReviewedOrderItemIds(reviewed);
      })
      .catch(() => toast.error("Không tìm thấy đơn hàng"))
      .finally(() => setLoading(false));
  }, [code, user, router]);

  const handleCancel = async () => {
    if (!cancelReason.trim()) { toast.error("Vui lòng nhập lý do hủy"); return; }
    setCancelling(true);
    try {
      const r = await ordersApi.cancelOrder(code, cancelReason.trim());
      setOrder(r.data.result);
      setShowCancelDialog(false);
      setCancelReason("");
      toast.success("Đã hủy đơn hàng");
    } catch {
      toast.error("Không thể hủy đơn hàng");
    } finally {
      setCancelling(false);
    }
  };

  const openReviewDialog = (item: OrderItem) => {
    setReviewingItem(item);
    setReviewRating(5);
    setReviewComment("");
  };

  const handleSubmitReview = async () => {
    if (!reviewingItem) return;
    setReviewSubmitting(true);
    try {
      await reviewsApi.create({
        orderItemId: reviewingItem.id,
        rating: reviewRating,
        comment: reviewComment.trim() || undefined,
      });
      setReviewedOrderItemIds((prev) => new Set(prev).add(reviewingItem.id));
      setReviewingItem(null);
      toast.success("Đã gửi đánh giá");
    } catch (err) {
      toast.error((err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Không thể gửi đánh giá");
    } finally {
      setReviewSubmitting(false);
    }
  };

  const openOrderChat = () => {
    openChat(order?.orderCode ?? code);
  };

  const handleRepurchase = async () => {
    if (!order) return;
    setRepurchasing(true);
    try {
      await Promise.all(order.items.map((item) => {
        if (!item.productId) {
          throw new Error("Thiếu dữ liệu sản phẩm");
        }
        return cartApi.addOrUpdate({
          productId: item.productId,
          variantId: item.variantId ?? undefined,
          colorId: item.colorId ?? undefined,
          quantity: item.quantity,
        });
      }));
      const cartRes = await cartApi.getCart();
      setItems(cartRes.data.result);
      toast.success("Đã thêm sản phẩm vào giỏ hàng");
      router.push("/cart");
    } catch (err) {
      toast.error((err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Không thể mua lại đơn hàng");
    } finally {
      setRepurchasing(false);
    }
  };

  if (!user) return null;
  if (loading) return <div className="max-w-3xl mx-auto px-4 py-8"><Skeleton className="h-96" /></div>;
  if (!order) return <div className="text-center py-16 text-gray-400">Không tìm thấy đơn hàng</div>;

  const canCancel = ["pending", "confirmed"].includes(order.status);

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => router.back()} className="text-gray-400 hover:text-gray-700">← Quay lại</button>
        <h1 className="text-xl font-bold">Đơn #{order.orderCode}</h1>
        <Badge variant={ORDER_STATUS_COLOR[order.status]}>
          {ORDER_STATUS_LABEL[order.status] || order.status}
        </Badge>
      </div>

      <div className="space-y-4">
        {/* Order Items */}
        <div className="bg-white border rounded-xl p-4">
          <h2 className="font-bold mb-3">Sản phẩm</h2>
          <div className="space-y-3">
            {order.items.map((item) => (
              <div key={item.id} className="flex items-start justify-between gap-4 text-sm">
                <div className="min-w-0">
                  <p className="font-medium">{item.productName}</p>
                  {(item.variantName || item.colorName) && (
                    <p className="text-gray-400 text-xs">
                      {[item.variantName, item.colorName].filter(Boolean).join(" · ")}
                    </p>
                  )}
                  <p className="text-gray-500">×{item.quantity}</p>
                  {order.status === "completed" && (
                    <div className="mt-2">
                      {reviewedOrderItemIds.has(item.id) ? (
                        <span className="inline-flex items-center rounded-full border border-green-200 bg-green-50 px-3 py-1 text-xs font-medium text-green-700">
                          Đã đánh giá
                        </span>
                      ) : (
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          className="rounded-full"
                          onClick={() => openReviewDialog(item)}
                        >
                          Đánh giá
                        </Button>
                      )}
                    </div>
                  )}
                </div>
                <p className="font-medium whitespace-nowrap">{formatVND(item.finalPrice * item.quantity)}</p>
              </div>
            ))}
          </div>
          <div className="border-t mt-3 pt-3 space-y-1 text-sm">
            <div className="flex justify-between text-gray-500">
              <span>Tạm tính</span><span>{formatVND(order.subtotal)}</span>
            </div>
            <div className="flex justify-between text-gray-500">
              <span>Phí vận chuyển</span><span>{formatVND(order.shippingFee)}</span>
            </div>
            {order.discountAmount > 0 && (
              <div className="flex justify-between text-green-600">
                <span>Giảm giá</span><span>-{formatVND(order.discountAmount)}</span>
              </div>
            )}
            <div className="flex justify-between font-bold text-base pt-1 border-t">
              <span>Tổng cộng</span><span>{formatVND(order.totalAmount)}</span>
            </div>
          </div>
        </div>

        {/* Shipping Info */}
        <div className="bg-white border rounded-xl p-4 text-sm">
          <h2 className="font-bold mb-3">Thông tin giao hàng</h2>
          <p className="font-medium">{order.recipientName} — {order.recipientPhone}</p>
          <p className="text-gray-500">{order.shippingAddress}, {order.shippingCity}</p>
        </div>

        {/* Payment */}
        <div className="bg-white border rounded-xl p-4 text-sm">
          <h2 className="font-bold mb-3">Thanh toán</h2>
          <div className="flex justify-between">
            <span className="text-gray-500">Phương thức</span>
            <span>{PAYMENT_METHOD_LABEL[order.paymentMethod] || order.paymentMethod}</span>
          </div>
          <div className="flex justify-between mt-1">
            <span className="text-gray-500">Trạng thái</span>
            <span>{PAYMENT_STATUS_LABEL[order.paymentStatus] || order.paymentStatus}</span>
          </div>
          {order.paidAt && (
            <div className="flex justify-between mt-1">
              <span className="text-gray-500">Thanh toán lúc</span>
              <span>{formatDate(order.paidAt)}</span>
            </div>
          )}
        </div>

        {order.status === "delivered" && (
          <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 space-y-4">
            <p className="text-sm text-amber-900 leading-6">
              Vui lòng chỉ nhấn &quot;Đã nhận được hàng&quot; khi đơn hàng đã được giao đến bạn và sản phẩm nhận được không có vấn đề nào.
            </p>
            <div className="flex flex-col gap-2 sm:flex-row">
              <Button
                className="rounded-xl bg-black text-white hover:bg-gray-800"
                onClick={() => {
                  void ordersApi.confirmReceived(order.orderCode)
                    .then((r) => {
                      setOrder(r.data.result);
                      toast.success("Đã ghi nhận bạn đã nhận hàng");
                    })
                    .catch((err) => {
                      toast.error((err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Không thể xác nhận nhận hàng");
                    });
                }}
              >
                Đã nhận hàng
              </Button>
              <Button
                variant="outline"
                className="rounded-xl"
                onClick={openOrderChat}
              >
                Yêu cầu Trả hàng/Hoàn tiền
              </Button>
              <Button
                variant="outline"
                className="rounded-xl"
                onClick={openOrderChat}
              >
                Liên hệ Người bán
              </Button>
            </div>
          </div>
        )}

        {order.status === "cancelled" && (
          <div className="bg-red-50 border border-red-200 rounded-xl p-4 space-y-4">
            <div className="flex items-center justify-between gap-3">
              <p className="text-sm text-red-700">Đơn hàng đã được hủy.</p>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="rounded-full border-red-200 text-red-700 hover:bg-red-100 hover:text-red-800"
                onClick={() => setShowCancelInfoDialog(true)}
              >
                Xem chi tiết Hủy đơn
              </Button>
            </div>
            <div className="flex flex-col gap-2 sm:flex-row">
              <Button
                type="button"
                className="rounded-xl bg-black text-white hover:bg-gray-800"
                onClick={() => void handleRepurchase()}
                disabled={repurchasing}
              >
                {repurchasing ? "Đang thêm..." : "Mua lại"}
              </Button>
              <Button
                type="button"
                variant="outline"
                className="rounded-xl"
                onClick={openOrderChat}
              >
                Liên hệ Người bán
              </Button>
            </div>
          </div>
        )}

        <div className="flex justify-between text-xs text-gray-400 px-1">
          <span>Đặt hàng: {formatDate(order.createdAt)}</span>
        </div>

        {canCancel && (
          <Button variant="destructive" className="w-full" onClick={() => { setShowCancelDialog(true); setCancelReason(""); }}>
            Hủy đơn hàng
          </Button>
        )}

      </div>

      <Dialog open={showCancelInfoDialog} onOpenChange={setShowCancelInfoDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Chi tiết Hủy đơn</DialogTitle>
          </DialogHeader>
          <div className="space-y-3 text-sm">
            <div className="flex justify-between gap-4">
              <span className="text-gray-500">Lời nhắn</span>
              <span className="font-medium">Đơn hàng đã được hủy.</span>
            </div>
            <div className="flex justify-between gap-4">
              <span className="text-gray-500">Thời gian hủy</span>
              <span className="font-medium">{order.cancelledAt ? formatDate(order.cancelledAt) : "Chưa ghi nhận"}</span>
            </div>
            <div className="pt-2 border-t">
              <p className="text-gray-500 mb-1">Lý do hủy</p>
              <p className="leading-6">{order.cancelReason || "Không có lý do"}</p>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      <Dialog open={Boolean(reviewingItem)} onOpenChange={(open) => {
        if (!open) setReviewingItem(null);
      }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Đánh giá sản phẩm</DialogTitle>
          </DialogHeader>
          {reviewingItem && (
            <div className="space-y-4">
              <div>
                <p className="text-sm font-medium">{reviewingItem.productName}</p>
                {(reviewingItem.variantName || reviewingItem.colorName) && (
                  <p className="text-xs text-gray-500">
                    {[reviewingItem.variantName, reviewingItem.colorName].filter(Boolean).join(" · ")}
                  </p>
                )}
              </div>

              <div>
                <p className="text-sm font-medium mb-2">Số sao</p>
                <div className="flex gap-2">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      type="button"
                      onClick={() => setReviewRating(star)}
                      className={`h-10 w-10 rounded-full border text-sm font-semibold transition ${
                        reviewRating >= star ? "border-black bg-black text-white" : "border-gray-200 bg-white text-gray-400"
                      }`}
                    >
                      ★
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <p className="text-sm font-medium mb-2">Bình luận</p>
                <textarea
                  className="w-full min-h-28 rounded-xl border px-3 py-2 text-sm outline-none focus:border-black"
                  value={reviewComment}
                  onChange={(e) => setReviewComment(e.target.value)}
                />
              </div>

              <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => setReviewingItem(null)}>
                  Hủy
                </Button>
                <Button type="button" onClick={() => void handleSubmitReview()} disabled={reviewSubmitting}>
                  {reviewSubmitting ? "Đang gửi..." : "Gửi đánh giá"}
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      <Dialog open={showCancelDialog} onOpenChange={setShowCancelDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Hủy đơn hàng #{code}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <p className="text-sm text-gray-500">Vui lòng cho chúng tôi biết lý do bạn muốn hủy đơn.</p>
            <textarea
              className="w-full border rounded-lg px-3 py-2 text-sm resize-none"
              rows={3}
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              autoFocus
            />
            <div className="flex gap-2 justify-end">
              <Button variant="outline" onClick={() => setShowCancelDialog(false)}>Không</Button>
              <Button variant="destructive" onClick={handleCancel} disabled={cancelling || !cancelReason.trim()}>
                {cancelling ? "Đang hủy..." : "Xác nhận hủy"}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
