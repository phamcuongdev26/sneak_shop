"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { ChevronDown, LayoutDashboard, Loader2, LogOut, Menu, Package, Search, ShoppingBag, User, UserCircle, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import Image from "next/image";
import { useAuthStore } from "@/store/auth";
import { useCartStore } from "@/store/cart";
import NotificationBell from "@/components/NotificationBell";
import { categoriesApi } from "@/lib/api/categories";
import { productsApi } from "@/lib/api/products";
import type { Category, Product } from "@/lib/types";

type CategoryNode = Category & { children: CategoryNode[] };

const buildCategoryTree = (items: Category[]): CategoryNode[] => {
  const map = new Map<number, CategoryNode>();
  const roots: CategoryNode[] = [];

  const sorted = [...items].sort((a, b) => {
    const ao = a.sortOrder ?? 0;
    const bo = b.sortOrder ?? 0;
    if (ao !== bo) return ao - bo;
    return a.name.localeCompare(b.name);
  });

  sorted.forEach((item) => {
    map.set(item.id, { ...item, children: [] });
  });

  sorted.forEach((item) => {
    const node = map.get(item.id);
    if (!node) return;
    if (item.parentId == null) {
      roots.push(node);
      return;
    }
    const parent = map.get(item.parentId);
    if (parent) parent.children.push(node);
    else roots.push(node);
  });

  const sortTree = (nodes: CategoryNode[]): CategoryNode[] =>
    nodes
      .sort((a, b) => {
        const ao = a.sortOrder ?? 0;
        const bo = b.sortOrder ?? 0;
        if (ao !== bo) return ao - bo;
        return a.name.localeCompare(b.name);
      })
      .map((node) => ({ ...node, children: sortTree(node.children) }));

  return sortTree(roots);
};

export default function Navbar() {
  const { user, logout } = useAuthStore();
  const count = useCartStore((s) => s.count());
  const router = useRouter();
  const pathname = usePathname();
  const [categories, setCategories] = useState<Category[]>([]);
  const [activeRootId, setActiveRootId] = useState<number | null>(null);
  const [activeChildId, setActiveChildId] = useState<number | null>(null);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [userHover, setUserHover] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [suggestions, setSuggestions] = useState<Product[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [mounted, setMounted] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const userRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const searchPanelRef = useRef<HTMLDivElement>(null);
  const hoverTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const categoryTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    setMobileOpen(false);
    setSearchOpen(false);
  }, [pathname]);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    categoriesApi.getAll()
      .then((r) => setCategories(r.data.result ?? []))
      .catch(() => setCategories([]));
  }, []);

  useEffect(() => {
    return () => {
      if (categoryTimer.current) clearTimeout(categoryTimer.current);
    };
  }, []);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setMobileOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  useEffect(() => {
    if (searchOpen) {
      setTimeout(() => searchInputRef.current?.focus(), 50);
    } else {
      setSearchQuery("");
      setSuggestions([]);
    }
  }, [searchOpen]);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (!searchQuery.trim()) { setSuggestions([]); return; }
    debounceRef.current = setTimeout(async () => {
      setSearchLoading(true);
      try {
        const r = await productsApi.search({ keyword: searchQuery.trim(), size: 6, status: "active" });
        setSuggestions(r.data.result?.content ?? []);
      } catch { setSuggestions([]); }
      setSearchLoading(false);
    }, 300);
  }, [searchQuery]);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (searchPanelRef.current && !searchPanelRef.current.contains(e.target as Node)) {
        setSearchOpen(false);
      }
    };
    if (searchOpen) document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [searchOpen]);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === "Escape") setSearchOpen(false); };
    if (searchOpen) document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, [searchOpen]);

  const handleSearchSubmit = () => {
    if (!searchQuery.trim()) return;
    setSearchOpen(false);
    router.push(`/products?keyword=${encodeURIComponent(searchQuery.trim())}`);
  };

  const handleLogout = () => {
    logout();
    router.push("/");
  };

  const categoryTree = buildCategoryTree(categories);
  const topCategories = categoryTree.filter((c) => c.parentId == null);
  const navLinks = [
    { href: "/products", label: "Mới" },
    { href: "/products?sort=sale", label: "Giảm giá" },
  ];

  const activeRoot = topCategories.find((c) => c.id === activeRootId) ?? null;
  const activeRootChildren = activeRoot?.children ?? [];
  const activeChild = activeRootChildren.find((child) => child.id === activeChildId) ?? null;

  const openCategoryMenu = (category: CategoryNode) => {
    if (categoryTimer.current) clearTimeout(categoryTimer.current);
    if (!category.children.length) {
      setActiveRootId(null);
      setActiveChildId(null);
      return;
    }
    setActiveRootId(category.id);
    setActiveChildId(null);
  };

  const closeCategoryMenu = () => {
    categoryTimer.current = setTimeout(() => {
      setActiveRootId(null);
      setActiveChildId(null);
    }, 100);
  };

  const initials = user?.fullName
    ? user.fullName.split(" ").slice(-2).map((w) => w[0]).join("").toUpperCase()
    : "U";

  return (
    <>
      <header className="sticky top-0 z-50">
        <div className="bg-[#0d2021] text-white text-[11px] sm:text-xs">
          <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-end gap-x-4 gap-y-1 px-4 py-2 text-right">
            <span>Hotline: 0123 456 789</span>
            <span className="opacity-50">|</span>
            <a href="mailto:support@sneakshop.vn" className="hover:underline">
              support@sneakshop.vn
            </a>
          </div>
        </div>

        <nav className="border-b border-black/5 bg-white">
          <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-4">
            <Link href="/" className="shrink-0">
              <div className="flex items-center gap-2 text-black">
                <div className="flex h-9 w-9 items-center justify-center border border-black/10 bg-white">
                  <div className="h-3.5 w-3.5 rotate-45 border border-black/70" />
                </div>
                <div>
                  <div className="font-serif text-[1.45rem] leading-none tracking-[0.08em]">
                    SNEAK SHOP
                  </div>
                </div>
              </div>
            </Link>

            <div className="hidden flex-1 items-center justify-center gap-5 lg:flex">
              {navLinks.map((link) => {
                const active =
                  link.href === "/products"
                    ? pathname.startsWith("/products")
                    : pathname.startsWith(link.href);
                return (
                  <Link
                    key={link.label}
                    href={link.href}
                    className={`inline-flex items-center gap-1 text-[13px] font-medium uppercase tracking-[0.12em] transition-colors ${
                      active ? "text-black" : "text-black/65 hover:text-black"
                    }`}
                  >
                    {link.label}
                  </Link>
                );
              })}

              {topCategories.map((category) => {
                const hasChildren = category.children.length > 0;
                const active = pathname.includes(`categorySlug=${encodeURIComponent(category.slug)}`);
                return (
                  <div
                    key={category.id}
                    className="relative"
                    onMouseEnter={() => openCategoryMenu(category)}
                    onMouseLeave={closeCategoryMenu}
                  >
                    <Link
                      href={`/products?categorySlug=${encodeURIComponent(category.slug)}`}
                      className={`inline-flex items-center gap-1 text-[13px] font-medium uppercase tracking-[0.12em] transition-colors ${
                        active ? "text-black" : "text-black/65 hover:text-black"
                      }`}
                    >
                      {category.name}
                      {hasChildren && <ChevronDown className="h-3.5 w-3.5 opacity-60" />}
                    </Link>

                    {hasChildren && activeRootId === category.id && (
                      <div
                        className="absolute left-0 top-[calc(100%-1px)] z-50 pt-4"
                        onMouseEnter={() => {
                          if (categoryTimer.current) clearTimeout(categoryTimer.current);
                        }}
                        onMouseLeave={closeCategoryMenu}
                      >
                        <div className="relative w-72 rounded-2xl border border-black/8 bg-white shadow-2xl">
                          <div className="max-h-96 overflow-y-auto py-2">
                            {activeRootChildren.map((child) => {
                              const hasGrandChildren = child.children.length > 0;
                              return (
                                <div key={child.id} className="relative px-2 py-1">
                                  {hasGrandChildren ? (
                                    <button
                                      type="button"
                                      onMouseEnter={() => setActiveChildId(child.id)}
                                      className={`flex w-full items-center justify-between rounded-lg px-3 py-2 text-left text-sm transition-colors ${
                                        activeChildId === child.id ? "bg-gray-50 text-black" : "text-gray-800 hover:bg-gray-50 hover:text-black"
                                      }`}
                                    >
                                      <span>{child.name}</span>
                                      <ChevronDown className="h-4 w-4 -rotate-90 opacity-40" />
                                    </button>
                                  ) : (
                                    <Link
                                      href={`/products?categorySlug=${encodeURIComponent(child.slug)}`}
                                      className="flex items-center justify-between rounded-lg px-3 py-2 text-sm text-gray-800 transition-colors hover:bg-gray-50 hover:text-black"
                                    >
                                      <span>{child.name}</span>
                                      <span className="h-4 w-4" />
                                    </Link>
                                  )}
                                </div>
                              );
                            })}
                          </div>

                          {activeChild?.children.length ? (
                            <div className="absolute left-[calc(100%-1px)] top-0 z-50 w-72">
                              <div className="rounded-2xl border border-black/8 bg-white shadow-2xl">
                                <div className="max-h-96 overflow-y-auto py-2">
                                  {activeChild.children.map((grandChild) => (
                                    <Link
                                      key={grandChild.id}
                                      href={`/products?categorySlug=${encodeURIComponent(grandChild.slug)}`}
                                      className="block px-4 py-3 text-sm text-gray-700 transition-colors hover:bg-gray-50 hover:text-black"
                                    >
                                      {grandChild.name}
                                    </Link>
                                  ))}
                                </div>
                              </div>
                            </div>
                          ) : null}
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            <div className="flex items-center gap-2" ref={dropdownRef}>
              <button
                type="button"
                onClick={() => setSearchOpen((v) => !v)}
                className={`inline-flex h-10 w-10 items-center justify-center rounded-full border border-black/10 text-black transition hover:bg-black hover:text-white ${searchOpen ? "bg-black text-white" : ""}`}
                aria-label="Tìm kiếm"
              >
                <Search className="h-4.5 w-4.5" />
              </button>

              {user && <NotificationBell variant="light" />}

              <Link
                href="/cart"
                className="relative inline-flex h-10 w-10 items-center justify-center rounded-full border border-black/10 text-black transition hover:bg-black hover:text-white"
                aria-label="Giỏ hàng"
              >
                <ShoppingBag className="h-4.5 w-4.5" />
                {mounted && count > 0 && (
                  <span className="absolute -right-1 -top-1 flex h-4.5 min-w-4.5 items-center justify-center rounded-full bg-black px-1 text-[10px] font-semibold text-white">
                    {count > 9 ? "9+" : count}
                  </span>
                )}
              </Link>

              {user ? (
                <div
                  ref={userRef}
                  className="relative"
                  onMouseEnter={() => {
                    if (hoverTimer.current) clearTimeout(hoverTimer.current);
                    setUserHover(true);
                  }}
                  onMouseLeave={() => {
                    hoverTimer.current = setTimeout(() => setUserHover(false), 150);
                  }}
                >
                  <button
                    type="button"
                    className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-black/10 text-black transition hover:bg-black hover:text-white"
                    aria-label="Tài khoản"
                  >
                    {user.avatarUrl ? (
                      <span className="relative block h-10 w-10 overflow-hidden rounded-full">
                        <Image
                          src={user.avatarUrl}
                          alt={user.fullName}
                          fill
                          className="object-cover"
                          sizes="40px"
                          unoptimized
                        />
                      </span>
                    ) : (
                      <span className="flex h-10 w-10 items-center justify-center rounded-full bg-black text-[11px] font-bold text-white">
                        {initials}
                      </span>
                    )}
                  </button>

                  {userHover && (
                    <div className="absolute left-1/2 -translate-x-1/2 top-full pt-2 z-50">
                      <div className="w-52 rounded-xl border border-black/8 bg-white shadow-xl overflow-hidden">
                        {/* User info header */}
                        <div className="px-4 py-3 bg-gray-50 border-b border-black/5">
                          <div className="flex items-center gap-3 mb-2">
                            <div className="relative h-10 w-10 overflow-hidden rounded-full bg-gray-900 flex-shrink-0">
                              {user.avatarUrl ? (
                                <Image
                                  src={user.avatarUrl}
                                  alt={user.fullName}
                                  fill
                                  className="object-cover"
                                  sizes="40px"
                                  unoptimized
                                />
                              ) : (
                                <span className="flex h-full w-full items-center justify-center text-[11px] font-bold text-white">
                                  {initials}
                                </span>
                              )}
                            </div>
                            <div className="min-w-0 flex-1">
                              <p className="text-xs font-semibold text-gray-900 truncate">{user.fullName}</p>
                              <p className="text-[11px] text-gray-500 truncate">{user.email}</p>
                            </div>
                          </div>
                        </div>
                        {/* Menu items */}
                        <div className="py-1">
                          <Link
                            href="/profile"
                            className="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                            onClick={() => setUserHover(false)}
                          >
                            <UserCircle className="h-4 w-4 text-gray-400" />
                            Thông tin cá nhân
                          </Link>
                          <Link
                            href="/orders"
                            className="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                            onClick={() => setUserHover(false)}
                          >
                            <Package className="h-4 w-4 text-gray-400" />
                            Đơn hàng của tôi
                          </Link>
                          {user.role === "admin" && (
                            <Link
                              href="/admin"
                              className="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                              onClick={() => setUserHover(false)}
                            >
                              <LayoutDashboard className="h-4 w-4 text-gray-400" />
                              Quản trị
                            </Link>
                          )}
                        </div>
                        <div className="border-t border-black/5 py-1">
                          <button
                            type="button"
                            onClick={() => { setUserHover(false); handleLogout(); }}
                            className="flex w-full items-center gap-3 px-4 py-2.5 text-sm text-red-500 hover:bg-red-50 transition-colors"
                          >
                            <LogOut className="h-4 w-4" />
                            Đăng xuất
                          </button>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <Link
                  href="/login"
                  className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-black/10 text-black transition hover:bg-black hover:text-white"
                  aria-label="Đăng nhập"
                >
                  <User className="h-4 w-4" />
                </Link>
              )}


              <button
                type="button"
                className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-black/10 text-black transition hover:bg-black hover:text-white lg:hidden"
                onClick={() => setMobileOpen((v) => !v)}
                aria-label="Mở menu"
              >
                {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
              </button>
            </div>
          </div>

          {mobileOpen && (
            <div className="border-t border-black/5 bg-white px-4 py-4 lg:hidden">
              <div className="mx-auto max-w-7xl space-y-3">
                {navLinks.map((link) => (
                  <Link
                    key={link.label}
                    href={link.href}
                    className="flex items-center justify-between rounded-xl px-3 py-3 text-sm font-medium uppercase tracking-[0.12em] text-black/80 hover:bg-black/5"
                  >
                    {link.label}
                    <ChevronDown className="h-4 w-4 opacity-50" />
                  </Link>
                ))}
                {topCategories.map((category) => (
                  <details key={category.id} className="rounded-xl border border-black/5 bg-white">
                    <summary className="flex cursor-pointer items-center justify-between px-3 py-3 text-sm font-medium uppercase tracking-[0.12em] text-black/80">
                      {category.name}
                      <ChevronDown className="h-4 w-4 opacity-50" />
                    </summary>
                    <div className="border-t border-black/5 px-2 py-2">
                      <Link
                        href={`/products?categorySlug=${encodeURIComponent(category.slug)}`}
                        className="block rounded-lg px-3 py-2 text-sm text-black/70 hover:bg-black/5"
                      >
                        Xem tất cả {category.name}
                      </Link>
                      {category.children.map((child) => (
                        <div key={child.id} className="mt-1">
                          <Link
                            href={`/products?categorySlug=${encodeURIComponent(child.slug)}`}
                            className="block rounded-lg px-3 py-2 text-sm text-black/70 hover:bg-black/5"
                          >
                            └ {child.name}
                          </Link>
                        </div>
                      ))}
                    </div>
                  </details>
                ))}
                <div className="flex items-center gap-2 pt-2">
                  {user ? (
                    <>
                      <Link href="/profile" className="rounded-full border px-4 py-2 text-sm">
                        Tài khoản
                      </Link>
                      {user.role === "admin" && (
                        <Link href="/admin" className="rounded-full border px-4 py-2 text-sm">
                          Quản trị
                        </Link>
                      )}
                      <button
                        type="button"
                        onClick={handleLogout}
                        className="inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm text-red-600"
                      >
                        <LogOut className="h-4 w-4" />
                        Đăng xuất
                      </button>
                    </>
                  ) : (
                    <>
                      <Link href="/login" className="rounded-full border px-4 py-2 text-sm">
                        Đăng nhập
                      </Link>
                      <Link href="/register" className="rounded-full bg-black px-4 py-2 text-sm text-white">
                        Đăng ký
                      </Link>
                    </>
                  )}
                </div>
              </div>
            </div>
          )}
        </nav>

        {/* Search panel */}
        {searchOpen && (
          <div ref={searchPanelRef} className="border-t border-black/5 bg-white shadow-lg">
            <div className="mx-auto max-w-2xl px-4 py-4">
              {/* Input */}
              <div className="relative flex items-center">
                <Search className="pointer-events-none absolute left-4 h-4 w-4 text-gray-400" />
                <input
                  ref={searchInputRef}
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onKeyDown={(e) => { if (e.key === "Enter") handleSearchSubmit(); }}
                  placeholder="Tìm kiếm sản phẩm..."
                  className="w-full rounded-full border border-gray-200 bg-gray-50 py-3 pl-11 pr-11 text-sm outline-none transition focus:border-black focus:ring-1 focus:ring-black"
                />
                {searchLoading ? (
                  <Loader2 className="absolute right-4 h-4 w-4 animate-spin text-gray-400" />
                ) : searchQuery ? (
                  <button onClick={() => setSearchQuery("")} className="absolute right-4 text-gray-400 hover:text-black">
                    <X className="h-4 w-4" />
                  </button>
                ) : null}
              </div>

              {/* Suggestions */}
              {suggestions.length > 0 && (
                <div className="mt-2 overflow-hidden rounded-2xl border border-black/8 bg-white shadow-xl">
                  {suggestions.map((p) => {
                    const salePrice = p.discountPercent > 0 ? Math.round(p.price * (1 - p.discountPercent / 100)) : null;
                    return (
                      <button
                        key={p.id}
                        type="button"
                        onClick={() => { setSearchOpen(false); router.push(`/products/${p.slug}`); }}
                        className="flex w-full items-center gap-3 px-4 py-3 text-left transition hover:bg-gray-50 border-b border-black/5 last:border-0"
                      >
                        <div className="relative h-12 w-12 flex-shrink-0 overflow-hidden rounded-lg border border-black/8 bg-gray-100">
                          {p.coverImageUrl ? (
                            <Image src={p.coverImageUrl} alt={p.name} fill className="object-cover" sizes="48px" unoptimized />
                          ) : (
                            <div className="flex h-full w-full items-center justify-center text-gray-300">
                              <ShoppingBag className="h-5 w-5" />
                            </div>
                          )}
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-medium text-gray-900">{p.name}</p>
                          <div className="mt-0.5 flex items-center gap-2">
                            {salePrice ? (
                              <>
                                <span className="text-sm font-semibold text-red-600">{salePrice.toLocaleString("vi-VN")}₫</span>
                                <span className="text-xs text-gray-400 line-through">{p.price.toLocaleString("vi-VN")}₫</span>
                              </>
                            ) : (
                              <span className="text-sm font-semibold text-gray-900">{p.price.toLocaleString("vi-VN")}₫</span>
                            )}
                          </div>
                        </div>
                        <Search className="h-3.5 w-3.5 flex-shrink-0 text-gray-300" />
                      </button>
                    );
                  })}
                  <button
                    type="button"
                    onClick={handleSearchSubmit}
                    className="flex w-full items-center justify-center gap-2 bg-gray-50 px-4 py-3 text-sm font-medium text-black/70 transition hover:bg-gray-100"
                  >
                    <Search className="h-3.5 w-3.5" />
                    Xem tất cả kết quả cho &quot;{searchQuery}&quot;
                  </button>
                </div>
              )}

              {!searchLoading && searchQuery.trim() && suggestions.length === 0 && (
                <div className="mt-2 rounded-2xl border border-black/8 bg-white px-4 py-6 text-center text-sm text-gray-400 shadow-xl">
                  Không tìm thấy sản phẩm nào cho &quot;{searchQuery}&quot;
                </div>
              )}
            </div>
          </div>
        )}
      </header>
    </>
  );
}
