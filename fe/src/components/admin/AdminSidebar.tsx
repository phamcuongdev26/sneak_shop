"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  LayoutDashboard, Package, ShoppingCart, Users, Star,
  Tag, Image as ImageIcon, FileText, LogOut, ChevronLeft,
  ClipboardList, Banknote, MessageSquare,
} from "lucide-react";
import { useAuthStore } from "@/store/auth";
import { chatApi } from "@/lib/api/chat";
import { cn } from "@/lib/utils";

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
  { href: "/admin/contacts", label: "Liên hệ", icon: MessageSquare },
  { href: "/admin/audit-logs", label: "Nhật ký tài chính", icon: Banknote },
];

export default function AdminSidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuthStore();
  const router = useRouter();
  const [chatUnread, setChatUnread] = useState(0);

  useEffect(() => {
    let mounted = true;
    const loadUnread = async () => {
      try {
        const r = await chatApi.adminUnreadCount();
        if (mounted) setChatUnread(r.data.result ?? 0);
      } catch {
        if (mounted) setChatUnread(0);
      }
    };
    loadUnread();
    const timer = setInterval(loadUnread, 5000);
    return () => {
      mounted = false;
      clearInterval(timer);
    };
  }, []);

  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  return (
    <aside className="w-56 flex-shrink-0 bg-gray-900 text-gray-300 flex flex-col min-h-screen">
      <div className="px-4 py-5 border-b border-gray-700">
        <h1 className="font-black text-white text-lg">SNEAK ADMIN</h1>
        {user && <p className="text-xs text-gray-400 mt-0.5 truncate">{user.email}</p>}
      </div>

      <nav className="flex-1 py-4 px-2 space-y-0.5">
        {navItems.map(({ href, label, icon: Icon, exact }) => {
          const active = exact ? pathname === href : pathname.startsWith(href);
          const showUnread = href === "/admin/chat" && chatUnread > 0;
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
              {showUnread && (
                <span className="ml-auto inline-flex min-w-5 h-5 items-center justify-center rounded-full bg-red-500 px-1.5 text-[10px] font-bold text-white">
                  {chatUnread > 99 ? "99+" : chatUnread}
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
