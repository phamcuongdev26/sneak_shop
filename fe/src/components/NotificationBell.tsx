"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { Bell, CheckCheck, Loader2 } from "lucide-react";
import { notificationsApi } from "@/lib/api/notifications";
import type { Notification } from "@/lib/types";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/store/auth";
import { useRealtimeSocket } from "@/lib/useRealtimeSocket";

type Props = {
  variant?: "light" | "dark";
  className?: string;
};

const formatTime = (value: string) =>
  new Intl.DateTimeFormat("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
  }).format(new Date(value));

export default function NotificationBell({ variant = "light", className }: Props) {
  const [open, setOpen] = useState(false);
  const [mounted, setMounted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unread, setUnread] = useState(0);
  const panelRef = useRef<HTMLDivElement>(null);
  const token = useAuthStore((s) => s.token);

  const load = async () => {
    setLoading(true);
    try {
      const [listRes, countRes] = await Promise.all([
        notificationsApi.getAll({ page: 0, size: 6 }),
        notificationsApi.unreadCount(),
      ]);
      setNotifications(listRes.data.result?.content ?? []);
      setUnread(countRes.data.result?.count ?? 0);
    } catch {
      setNotifications([]);
      setUnread(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setMounted(true);
    void load();
    const timer = setInterval(() => void load(), 30000);
    return () => clearInterval(timer);
  }, []);

  useRealtimeSocket(Boolean(token), (event) => {
    if (event.channel === "notification") {
      void load();
    }
  });

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    if (open) document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [open]);

  const markAll = async () => {
    try {
      await notificationsApi.markAllRead();
      await load();
    } catch {
      // ignore
    }
  };

  const markOne = async (id: number) => {
    try {
      await notificationsApi.markRead(id);
      setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)));
      setUnread((prev) => Math.max(0, prev - 1));
    } catch {
      // ignore
    }
  };

  const buttonClass = useMemo(() => {
    if (variant === "dark") {
      return "relative inline-flex h-10 w-10 items-center justify-center rounded-xl border border-gray-700 bg-gray-800 text-gray-200 hover:bg-gray-700";
    }
    return "relative inline-flex h-10 w-10 items-center justify-center rounded-full border border-black/10 bg-white text-black hover:bg-gray-50";
  }, [variant]);

  const panelClass = variant === "dark"
    ? "absolute right-0 top-full mt-2 w-[22rem] overflow-hidden rounded-2xl border border-gray-700 bg-gray-900 text-gray-100 shadow-2xl"
    : "absolute right-0 top-full mt-2 w-[22rem] overflow-hidden rounded-2xl border border-black/10 bg-white text-gray-900 shadow-2xl";

  return (
    <div className={cn("relative", className)} ref={panelRef}>
      <button type="button" onClick={() => setOpen((v) => !v)} className={buttonClass} aria-label="Thông báo">
        <Bell className="h-4.5 w-4.5" />
        {mounted && unread > 0 && (
          <span className="absolute -right-1 -top-1 flex h-4.5 min-w-4.5 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">
            {unread > 9 ? "9+" : unread}
          </span>
        )}
      </button>

      {open && (
        <div className={panelClass}>
          <div className={cn("flex items-center justify-between border-b px-4 py-3", variant === "dark" ? "border-gray-700" : "border-black/5")}>
            <div>
              <p className="text-sm font-semibold">Thông báo</p>
              <p className={cn("text-xs", variant === "dark" ? "text-gray-400" : "text-gray-500")}>
                {unread > 0 ? `${unread} chưa đọc` : "Không có thông báo mới"}
              </p>
            </div>
            <button
              type="button"
              onClick={() => void markAll()}
              className={cn(
                "inline-flex items-center gap-1 text-xs font-semibold",
                variant === "dark" ? "text-orange-300 hover:text-orange-200" : "text-orange-600 hover:text-orange-700",
              )}
            >
              <CheckCheck className="h-3.5 w-3.5" />
              Đọc hết
            </button>
          </div>

          <div className={cn("max-h-[24rem] overflow-y-auto", variant === "dark" ? "bg-gray-900" : "bg-white")}>
            {loading ? (
              <div className="flex items-center justify-center py-10">
                <Loader2 className="h-5 w-5 animate-spin text-gray-400" />
              </div>
            ) : notifications.length === 0 ? (
              <div className={cn("px-4 py-10 text-center text-sm", variant === "dark" ? "text-gray-400" : "text-gray-500")}>
                Chưa có thông báo
              </div>
            ) : (
              <div className={cn("divide-y", variant === "dark" ? "divide-gray-700" : "divide-black/5")}>
                {notifications.map((n) => (
                  <button
                    key={n.id}
                    type="button"
                    onClick={() => void markOne(n.id)}
                    className={cn(
                      "block w-full px-4 py-3 text-left transition",
                      n.isRead
                        ? variant === "dark"
                          ? "bg-gray-900 hover:bg-gray-800"
                          : "bg-white hover:bg-gray-50"
                        : variant === "dark"
                          ? "bg-gray-800/60 hover:bg-gray-800"
                          : "bg-orange-50/60 hover:bg-orange-50",
                    )}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <p className="line-clamp-1 text-sm font-semibold">{n.title}</p>
                        <p className={cn("mt-1 line-clamp-2 text-xs", variant === "dark" ? "text-gray-300" : "text-gray-600")}>
                          {n.body}
                        </p>
                      </div>
                      {!n.isRead && <span className="mt-1 h-2.5 w-2.5 rounded-full bg-orange-500" />}
                    </div>
                    <div className={cn("mt-2 text-[11px]", variant === "dark" ? "text-gray-500" : "text-gray-400")}>
                      {formatTime(n.createdAt)}
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
