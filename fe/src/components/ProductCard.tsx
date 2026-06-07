import Link from "next/link";
import Image from "next/image";
import { Star } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { formatRating, formatVND } from "@/lib/format";
import type { Product } from "@/lib/types";

export default function ProductCard({ product }: { product: Product }) {
  const discounted =
    product.discountPercent > 0
      ? product.price * (1 - product.discountPercent / 100)
      : null;

  return (
    <Link href={`/products/${product.slug}`}>
      <div className="group rounded-xl border bg-white overflow-hidden hover:shadow-lg transition-all duration-200">
        <div className="relative aspect-square bg-gray-50 overflow-hidden">
          {product.coverImageUrl ? (
            <Image
              src={product.coverImageUrl}
              alt={product.name}
              fill
              className="object-cover group-hover:scale-105 transition-transform duration-300"
              sizes="(max-width: 768px) 50vw, 25vw"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-gray-300 text-4xl">
              👟
            </div>
          )}
          {product.discountPercent > 0 && (
            <Badge className="absolute top-2 left-2 bg-red-500 text-white">
              -{product.discountPercent}%
            </Badge>
          )}
        </div>
        <div className="p-3">
          <h3 className="font-medium text-sm line-clamp-2 text-gray-900 mb-1">
            {product.name}
          </h3>
          <div className="flex items-center gap-1 mb-2">
            <Star className="w-3 h-3 fill-yellow-400 text-yellow-400" />
            <span className="text-xs text-gray-500">
              {formatRating(product.ratingAverage)} ({product.reviewCount})
            </span>
          </div>
          <p className="mb-2 text-[11px] text-gray-400">
            Đã bán: {product.soldCount ?? 0}
          </p>
          <div className="flex items-baseline gap-2">
            <span className="font-bold text-black text-sm">
              {formatVND(discounted ?? product.price)}
            </span>
            {discounted && (
              <span className="text-gray-400 text-xs line-through">
                {formatVND(product.price)}
              </span>
            )}
          </div>
        </div>
      </div>
    </Link>
  );
}
