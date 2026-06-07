"use client";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { api } from "@/lib/api";
import { formatDate, formatVND } from "@/lib/format";

interface FinancialLog {
  id: number;
  email: string;
  usersId: number | null;
  addressesId: number | null;
  ordersId: number | null;
  transactionsId: number | null;
  productsId: number | null;
  productsShopId: number | null;
  amount: number;
  bankName: string;
  note: string | null;
  createdAt: string;
}

const HEADERS = [
  "ID", "Email", "Mã ND", "Số tiền", "Ngân hàng",
  "Mã đơn", "Mã GD", "Mã địa chỉ", "Mã SP", "Mã shop SP", "Ghi chú", "Thời gian",
];

export default function AdminAuditLogsPage() {
  const [logs, setLogs] = useState<FinancialLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [email, setEmail] = useState("");

  const load = async () => {
    setLoading(true);
    try {
      const r = await api.get("/api/admin/audit-logs", {
        params: { email: email || undefined, page, size: 20 },
      });
      setLogs(r.data.result.content);
      setTotalPages(r.data.result.totalPages);
    } catch {}
    setLoading(false);
  };

  useEffect(() => { load(); }, [page]);

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Nhật ký giao dịch tài chính</h1>

      <div className="flex gap-3">
        <Input value={email} onChange={(e) => setEmail(e.target.value)} className="max-w-xs bg-white" />
        <Button variant="outline" onClick={() => { setPage(0); load(); }}>Lọc</Button>
      </div>

      <div className="bg-white rounded-xl border overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b">
            <tr>
              {HEADERS.map((h) => (
                <th key={h} className="text-left py-3 px-4 text-xs font-semibold text-gray-500 whitespace-nowrap">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 8 }).map((_, i) => (
                <tr key={i}><td colSpan={HEADERS.length} className="px-4 py-3"><Skeleton className="h-5" /></td></tr>
              ))
            ) : logs.length === 0 ? (
              <tr><td colSpan={HEADERS.length} className="text-center py-12 text-gray-400">Chưa có giao dịch</td></tr>
            ) : (
              logs.map((log) => (
                <tr key={log.id} className="border-b last:border-0 hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-400">{log.id}</td>
                  <td className="px-4 py-3 font-medium">{log.email}</td>
                  <td className="px-4 py-3 text-gray-500">{log.usersId ?? "—"}</td>
                  <td className="px-4 py-3 font-bold text-green-700 whitespace-nowrap">{formatVND(log.amount)}</td>
                  <td className="px-4 py-3 text-gray-600">{log.bankName}</td>
                  <td className="px-4 py-3 text-gray-500">{log.ordersId ?? "—"}</td>
                  <td className="px-4 py-3 text-gray-500">{log.transactionsId ?? "—"}</td>
                  <td className="px-4 py-3 text-gray-500">{log.addressesId ?? "—"}</td>
                  <td className="px-4 py-3 text-gray-500">{log.productsId ?? "—"}</td>
                  <td className="px-4 py-3 text-gray-500">{log.productsShopId ?? "—"}</td>
                  <td className="px-4 py-3 text-gray-400 max-w-40 truncate text-xs">{log.note || "—"}</td>
                  <td className="px-4 py-3 text-gray-400 text-xs whitespace-nowrap">{formatDate(log.createdAt)}</td>
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
    </div>
  );
}
