"use client";
import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { dashboardApi } from "@/lib/api/dashboard";
import { formatRating, formatVND, formatDate } from "@/lib/format";
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from "recharts";
import type { Dashboard } from "@/lib/types";

export default function AdminDashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    dashboardApi.get(7)
      .then((r) => setData(r.data.result))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold">Bảng điều khiển</h1>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-28 rounded-xl" />)}
        </div>
        <Skeleton className="h-64 rounded-xl" />
      </div>
    );
  }

  if (!data) return <div className="text-gray-400">Không tải được dữ liệu</div>;

  const stats = [
    { label: "Doanh thu hôm nay", value: formatVND(data.revenueToday), color: "text-green-600" },
    { label: "Doanh thu tháng này", value: formatVND(data.revenueThisMonth), color: "text-blue-600" },
    { label: "Đơn hàng hôm nay", value: data.ordersToday, color: "text-purple-600" },
    { label: "Người dùng mới", value: data.newUsersToday, color: "text-orange-600" },
    { label: "Đơn chờ xử lý", value: data.pendingOrders, color: "text-yellow-600" },
    { label: "Tổng sản phẩm", value: data.totalProducts, color: "text-gray-600" },
    { label: "Đánh giá TB", value: `${formatRating(data.avgRating)}⭐`, color: "text-yellow-500" },
    { label: "Tổng đánh giá", value: data.totalReviews, color: "text-gray-600" },
  ];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Bảng điều khiển</h1>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {stats.map((s) => (
          <Card key={s.label}>
            <CardContent className="pt-5 pb-4">
              <p className="text-xs text-gray-400 mb-1">{s.label}</p>
              <p className={`text-2xl font-black ${s.color}`}>{String(s.value)}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Revenue Chart */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Doanh thu 7 ngày gần nhất</CardTitle>
        </CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={data.revenueChart}>
              <XAxis dataKey="date" tick={{ fontSize: 11 }} />
              <YAxis tickFormatter={(v) => `${(v / 1000).toFixed(0)}k`} tick={{ fontSize: 11 }} />
              <Tooltip formatter={(v: unknown) => formatVND(Number(v))} />
              <Bar dataKey="revenue" fill="#111827" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>

      {/* Recent Orders */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Đơn hàng gần đây</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-gray-400 text-xs">
                  <th className="text-left py-2 pr-4">Mã đơn</th>
                  <th className="text-left py-2 pr-4">Khách hàng</th>
                  <th className="text-right py-2 pr-4">Tổng tiền</th>
                  <th className="text-left py-2 pr-4">Trạng thái</th>
                  <th className="text-left py-2">Thời gian</th>
                </tr>
              </thead>
              <tbody>
                {data.recentOrders.map((o) => (
                  <tr key={o.id} className="border-b last:border-0 hover:bg-gray-50">
                    <td className="py-2 pr-4 font-medium">#{o.orderCode}</td>
                    <td className="py-2 pr-4 text-gray-600">{o.recipientName}</td>
                    <td className="py-2 pr-4 text-right font-medium">{formatVND(o.totalAmount)}</td>
                    <td className="py-2 pr-4 text-gray-500 text-xs capitalize">{o.status}</td>
                    <td className="py-2 text-gray-400 text-xs">{o.createdAt}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
