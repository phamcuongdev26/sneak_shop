"use client";
import { useCallback, useEffect, useMemo, useRef, useState, Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import ProductCard from "@/components/ProductCard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { productsApi } from "@/lib/api/products";
import { categoriesApi } from "@/lib/api/categories";
import type { Product, Category } from "@/lib/types";
import { Search, X } from "lucide-react";

export default function ProductsPage() {
  return (
    <Suspense>
      <ProductsContent />
    </Suspense>
  );
}

function ProductsContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const [products, setProducts] = useState<Product[]>([]);
  const [suggestions, setSuggestions] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const initialKeyword = searchParams.get("keyword") || "";
  const [searchOpen, setSearchOpen] = useState(Boolean(initialKeyword));

  const [keywordInput, setKeywordInput] = useState(initialKeyword);
  const [keyword, setKeyword] = useState(initialKeyword);
  const [categoryId, setCategoryId] = useState(searchParams.get("categoryId") || "");
  const [sort, setSort] = useState("newest");
  const [page, setPage] = useState(0);
  const [suggestionOpen, setSuggestionOpen] = useState(false);
  const searchInputRef = useRef<HTMLInputElement | null>(null);
  const searchWrapRef = useRef<HTMLFormElement | null>(null);

  useEffect(() => {
    categoriesApi.getAll().then((r) => setCategories(r.data.result)).catch(() => {});
    productsApi.search({ status: "active", page: 0, size: 24, sort: "newest" })
      .then((r) => setSuggestions(r.data.result.content))
      .catch(() => setSuggestions([]));
  }, []);

  useEffect(() => {
    if (searchOpen) {
      searchInputRef.current?.focus();
    }
  }, [searchOpen]);

  const loadProducts = useCallback(async () => {
    setLoading(true);
    try {
      const res = await productsApi.search({
        keyword: keyword || undefined,
        categoryId: categoryId ? Number(categoryId) : undefined,
        status: "active",
        sort,
        page,
        size: 12,
      });
      setProducts(res.data.result.content);
      setTotalPages(res.data.result.totalPages);
    } catch {
      setProducts([]);
    } finally {
      setLoading(false);
    }
  }, [keyword, categoryId, sort, page]);

  useEffect(() => { loadProducts(); }, [loadProducts]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setKeyword(keywordInput.trim());
    setPage(0);
    setSuggestionOpen(false);
  };

  const suggestionList = useMemo(() => {
    const q = keywordInput.trim().toLowerCase();
    if (!q) return suggestions.slice(0, 8);
    return suggestions
      .filter((p) => p.name.toLowerCase().includes(q) || p.slug.toLowerCase().includes(q))
      .slice(0, 8);
  }, [keywordInput, suggestions]);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (searchWrapRef.current && !searchWrapRef.current.contains(e.target as Node)) {
        setSuggestionOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-6">Sản phẩm</h1>

      {/* Filters */}
      <div className="flex flex-wrap gap-3 mb-8 items-start">
        <form ref={searchWrapRef} onSubmit={handleSearch} className="relative flex gap-2 flex-1 min-w-[280px] max-w-xl">
          {searchOpen ? (
            <>
              <div className="relative flex-1">
                <Input
                  ref={searchInputRef}
                  value={keywordInput}
                  onChange={(e) => {
                    setKeywordInput(e.target.value);
                    setSuggestionOpen(true);
                  }}
                  onFocus={() => setSuggestionOpen(true)}
                  className="pr-10"
                />
                {keywordInput ? (
                  <button
                    type="button"
                    onClick={() => {
                      setKeywordInput("");
                      setKeyword("");
                      setSuggestionOpen(true);
                      setPage(0);
                    }}
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                    aria-label="Xóa tìm kiếm"
                  >
                    <X className="h-4 w-4" />
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => {
                      setSearchOpen(false);
                      setSuggestionOpen(false);
                    }}
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                    aria-label="Đóng tìm kiếm"
                  >
                    <X className="h-4 w-4" />
                  </button>
                )}
              </div>
              <Button type="submit" size="icon" variant="outline" aria-label="Tìm kiếm">
                <Search className="w-4 h-4" />
              </Button>

              {suggestionOpen && suggestionList.length > 0 && (
                <div className="absolute left-0 top-full z-30 mt-2 w-full overflow-hidden rounded-xl border bg-white shadow-lg">
                  <div className="border-b px-3 py-2 text-xs font-semibold uppercase tracking-wide text-gray-400">
                    Gợi ý tìm kiếm
                  </div>
                  <div className="max-h-72 overflow-auto">
                    {suggestionList.map((item) => (
                      <button
                        key={item.id}
                        type="button"
                        onClick={() => {
                          setKeywordInput(item.name);
                          setKeyword(item.name);
                          setPage(0);
                          setSuggestionOpen(false);
                        }}
                        className="flex w-full items-center gap-3 px-3 py-2 text-left hover:bg-gray-50"
                      >
                        <div className="flex h-10 w-10 items-center justify-center overflow-hidden rounded-lg bg-gray-100 text-[10px] text-gray-400">
                          {item.coverImageUrl ? (
                            <img src={item.coverImageUrl} alt={item.name} className="h-full w-full object-cover" />
                          ) : (
                            "👟"
                          )}
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-medium text-gray-900">{item.name}</p>
                          <p className="truncate text-xs text-gray-400">{item.shop?.name || "Sneak Shop"}</p>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </>
          ) : (
            <Button
              type="button"
              variant="outline"
              className="gap-2"
              onClick={() => {
                setSearchOpen(true);
                setSuggestionOpen(true);
              }}
            >
              <Search className="w-4 h-4" />
              Tìm kiếm
            </Button>
          )}
        </form>

        <Select value={categoryId || "all"} onValueChange={(v) => { setCategoryId(!v || v === "all" ? "" : v); setPage(0); }}>
          <SelectTrigger className="w-48">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Tất cả danh mục</SelectItem>
            {categories.map((c) => (
              <SelectItem key={c.id} value={String(c.id)}>{c.name}</SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select value={sort} onValueChange={(v) => { setSort(v ?? "newest"); setPage(0); }}>
          <SelectTrigger className="w-44">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="newest">Mới nhất</SelectItem>
            <SelectItem value="price_asc">Giá tăng dần</SelectItem>
            <SelectItem value="price_desc">Giá giảm dần</SelectItem>
            <SelectItem value="sold">Bán chạy</SelectItem>
            <SelectItem value="rating">Đánh giá cao</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Products */}
      {loading ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="aspect-[3/4] rounded-xl" />
          ))}
        </div>
      ) : products.length === 0 ? (
        <div className="text-center py-20 text-gray-400">
          <div className="text-5xl mb-3">🔍</div>
          <p>Không tìm thấy sản phẩm nào</p>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {products.map((p) => <ProductCard key={p.id} product={p} />)}
          </div>

          {totalPages > 1 && (
            <div className="flex justify-center gap-2 mt-10">
              <Button variant="outline" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                Trước
              </Button>
              <span className="flex items-center px-4 text-sm text-gray-600">
                {page + 1} / {totalPages}
              </span>
              <Button variant="outline" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
                Tiếp
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
