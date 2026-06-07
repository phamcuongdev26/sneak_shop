"use client";

import ProductCard from "@/components/ProductCard";
import type { Product } from "@/lib/types";

export default function HomeFeaturedProducts({ products }: { products: Product[] }) {
  const filteredProducts = products;

  return (
    <section className="mx-auto max-w-7xl px-4 py-14 sm:py-16">
      <div className="mb-8 text-center">
        <h2 className="text-2xl font-bold tracking-[0.08em] text-black sm:text-3xl">
          SẢN PHẨM NỔI BẬT
        </h2>
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
