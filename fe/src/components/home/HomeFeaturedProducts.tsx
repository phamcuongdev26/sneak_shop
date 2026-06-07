"use client";

import { useMemo, useState } from "react";
import { Search, X } from "lucide-react";
import ProductCard from "@/components/ProductCard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { Product } from "@/lib/types";

export default function HomeFeaturedProducts({ products }: { products: Product[] }) {
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [categoryId, setCategoryId] = useState("all");

  const categories = useMemo(() => {
    const seen = new Map<number, { id: number; name: string }>();
    products.forEach((product) => {
      product.categories.forEach((category) => {
        if (!seen.has(category.id)) {
          seen.set(category.id, { id: category.id, name: category.name });
        }
      });
    });
    return Array.from(seen.values()).sort((a, b) => a.name.localeCompare(b.name, "vi"));
  }, [products]);

  const matchesCategory = (product: Product) => {
    if (categoryId === "all") return true;
    return product.categories.some((category) => String(category.id) === categoryId);
  };

  const suggestions = useMemo(() => {
    const q = query.trim().toLowerCase();
    const scoped = products.filter(matchesCategory);
    if (!q) return scoped.slice(0, 6);
    return scoped
      .filter((item) => item.name.toLowerCase().includes(q) || item.slug.toLowerCase().includes(q))
      .slice(0, 6);
  }, [products, query, categoryId]);

  const filteredProducts = useMemo(() => {
    const q = query.trim().toLowerCase();
    const scoped = products.filter(matchesCategory);
    if (!q) return scoped;
    return scoped.filter((item) => item.name.toLowerCase().includes(q) || item.slug.toLowerCase().includes(q));
  }, [products, query, categoryId]);

  return (
    <section className="mx-auto max-w-7xl px-4 py-14 sm:py-16">
      <div className="mb-8 text-center">
        <h2 className="text-2xl font-bold tracking-[0.08em] text-black sm:text-3xl">
          SẢN PHẨM NỔI BẬT
        </h2>
      </div>

      <div className="mx-auto mb-10 max-w-2xl">
        <div className="relative">
          <div className="flex flex-col gap-3 rounded-2xl border border-black/10 bg-white px-3 py-3 shadow-sm sm:flex-row sm:items-center">
            <div className="flex flex-1 items-center gap-2">
              <Search className="h-4.5 w-4.5 shrink-0 text-black/45" />
              <Input
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value);
                  setOpen(true);
                }}
                onFocus={() => setOpen(true)}
                className="border-0 bg-transparent px-0 shadow-none ring-0 placeholder:text-black/35 focus-visible:ring-0"
              />
              {query ? (
                <button
                  type="button"
                  onClick={() => {
                    setQuery("");
                    setOpen(false);
                  }}
                  className="inline-flex h-8 w-8 items-center justify-center rounded-full text-black/40 transition hover:bg-black/5 hover:text-black/70"
                  aria-label="Xóa tìm kiếm"
                >
                  <X className="h-4 w-4" />
                </button>
              ) : null}
            </div>

            <div className="sm:w-52">
              <Select
                value={categoryId}
                onValueChange={(v) => {
                  setCategoryId(v ?? "all");
                  setOpen(true);
                }}
              >
                <SelectTrigger className="h-11 w-full rounded-xl border-black/10 bg-white">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả danh mục</SelectItem>
                  {categories.map((category) => (
                    <SelectItem key={category.id} value={String(category.id)}>
                      {category.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {open && suggestions.length > 0 && (
            <div className="absolute left-0 top-full z-20 mt-2 w-full overflow-hidden rounded-2xl border border-black/8 bg-white shadow-xl">
              <div className="max-h-72 overflow-auto">
                {suggestions.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => {
                      setQuery(item.name);
                      setOpen(false);
                    }}
                    className="flex w-full items-center gap-3 px-4 py-3 text-left transition hover:bg-black/[0.03]"
                  >
                    <div className="flex h-12 w-12 items-center justify-center overflow-hidden rounded-xl bg-black/[0.04] text-[10px] text-black/30">
                      {item.coverImageUrl ? (
                        <img src={item.coverImageUrl} alt={item.name} className="h-full w-full object-cover" />
                      ) : (
                        "SNEAK"
                      )}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-black">{item.name}</p>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {filteredProducts.length > 0 ? (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
          {filteredProducts.map((p) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      ) : (
        <div className="py-16 text-center text-gray-400">
          <div className="mb-3 text-5xl">🔍</div>
          <p>Không tìm thấy sản phẩm nào</p>
        </div>
      )}
    </section>
  );
}
