"use client";
import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  LayoutDashboard, Package, ShoppingCart, Users, Star,
  Tag, Image as ImageIcon, FileText, LogOut, ChevronLeft,
  ClipboardList, Banknote, MessageSquare,
} from "lucide-react";
import { useAuthStore } from "@/store/auth";
import { chatApi } from "@/lib/api/chat";
import { dashboardApi } from "@/lib/api/dashboard";
import { cn } from "@/lib/utils";
import { useRealtimeSocket } from "@/lib/useRealtimeSocket";

const navItems = [
  { href: "/admin", label: "Bảng điều khiển", icon: LayoutDashboard, exact: true },
  { href: "/admin/products", label: "Sản phẩm", icon: Package },
  { href: "/admin/orders", label: "Đơn hàng", icon: ShoppingCart },
  { href: "/admin/users", label: "Người dùng", icon: Users },
  { href: "/admin/reviews", label: "Đánh giá", icon: Star },
  { href: "/admin/categories", label: "Danh mục", icon: Tag },
  { href: "/admin/banners", label: "Ảnh banner", icon: ImageIcon },
  { href: "/admin/blog", label: "Bài viết", icon: FileText },
  { href: "/admin/chat", label: "Chat hỗ trợ", icon: MessageSquare },
  { href: "/admin/audit-logs", label: "Nhật ký tài chính", icon: Banknote },
];

export default function AdminSidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuthStore();
  const router = useRouter();
  const [chatUnread, setChatUnread] = useState(0);
  const [pendingOrders, setPendingOrders] = useState(0);
  const [newUsersToday, setNewUsersToday] = useState(0);

  const loadCounts = useCallback(async () => {
    try {
      const [chatRes, dashRes] = await Promise.all([
        chatApi.adminUnreadCount(),
        dashboardApi.get(7),
      ]);
      setChatUnread(chatRes.data.result ?? 0);
      setPendingOrders(Number(dashRes.data.result?.pendingOrders ?? 0));
      setNewUsersToday(Number(dashRes.data.result?.newUsersToday ?? 0));
    } catch {
      setChatUnread(0);
      setPendingOrders(0);
      setNewUsersToday(0);
    }
  }, []);

  useRealtimeSocket(Boolean(user), (event) => {
    if (event.channel === "notification" || event.channel === "chat" || event.channel === "dashboard") {
      void loadCounts();
    }
  });

  useEffect(() => {
    loadCounts();
    const timer = setInterval(loadCounts, 30000);
    return () => {
      clearInterval(timer);
    };
  }, [loadCounts]);

  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  return (
    <aside className="w-56 flex-shrink-0 bg-gray-900 text-gray-300 flex flex-col min-h-screen">
      <div className="px-4 py-5 border-b border-gray-700">
        <div className="min-w-0">
          <h1 className="font-black text-white text-lg">SNEAK ADMIN</h1>
          {user && <p className="text-xs text-gray-400 mt-0.5 truncate">{user.email}</p>}
        </div>
      </div>

      <nav className="flex-1 py-4 px-2 space-y-0.5">
        {navItems.map(({ href, label, icon: Icon, exact }) => {
          const active = exact ? pathname === href : pathname.startsWith(href);
          const badge =
            href === "/admin/chat" ? chatUnread :
            href === "/admin/orders" ? pendingOrders :
            href === "/admin/users" ? newUsersToday :
            0;
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors relative",
                active
                  ? "bg-white text-gray-900"
                  : "hover:bg-gray-800 hover:text-white"
              )}
              >
                <Icon className="w-4 h-4 flex-shrink-0" />
                {label}
              {badge > 0 && (
                <span className="ml-auto inline-flex min-w-5 h-5 items-center justify-center rounded-full bg-red-500 px-1.5 text-[10px] font-bold text-white">
                  {badge > 99 ? "99+" : badge}
                </span>
              )}
            </Link>
          );
        })}
      </nav>

      <div className="p-3 border-t border-gray-700 space-y-1">
        <Link
          href="/"
          className="flex items-center gap-3 px-3 py-2 rounded-lg text-sm hover:bg-gray-800 hover:text-white transition-colors"
        >
          <ChevronLeft className="w-4 h-4" /> Về trang chủ
        </Link>
        <button
          onClick={handleLogout}
          className="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm hover:bg-red-900 hover:text-red-300 transition-colors"
        >
          <LogOut className="w-4 h-4" /> Đăng xuất
        </button>
      </div>
    </aside>
  );
}
