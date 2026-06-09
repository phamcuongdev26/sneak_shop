"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import type { Banner } from "@/lib/types";
import { Button } from "@/components/ui/button";

const slideMeta = [
  { eyebrow: "GIỮA MÙA", headline: "GIẢM GIÁ", discount: "70%" },
  { eyebrow: "BỘ SƯU TẬP MỚI", headline: "RA MẮT", discount: "50%" },
  { eyebrow: "SỐ LƯỢNG CÓ HẠN", headline: "BỘ SƯU TẬP", discount: "60%" },
];

export default function HomeHeroCarousel({ banners }: { banners: Banner[] }) {
  const slides = useMemo(() => {
    return banners.slice(0, 3).map((banner, index) => ({
      id: banner.id,
      imageUrl: banner.imageUrl,
      ...slideMeta[index % slideMeta.length],
    }));
  }, [banners]);

  const [current, setCurrent] = useState(0);

  useEffect(() => {
    if (slides.length <= 1) return;
    const timer = window.setInterval(() => {
      setCurrent((prev) => (prev + 1) % slides.length);
    }, 6000);
    return () => window.clearInterval(timer);
  }, [slides.length]);

  const active = slides[current] ?? slides[0];

  if (!active) return null;

  const goTo = (next: number) => setCurrent((next + slides.length) % slides.length);

  return (
    <section className="relative w-full overflow-hidden bg-[#0b1f20] text-white">
      <div className="relative h-[60vh] min-h-[520px] w-full">
        <Image
          src={active.imageUrl}
          alt={active.eyebrow}
          fill
          priority
          className="object-cover"
          sizes="100vw"
        />

        <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(6,27,29,0.96)_0%,rgba(6,27,29,0.9)_34%,rgba(6,27,29,0.52)_62%,rgba(6,27,29,0.18)_100%)]" />

        <div className="absolute inset-0">
          <div className="mx-auto flex h-full max-w-7xl items-center px-4 sm:px-6 lg:px-8">
            <div className="max-w-xl pb-10 pt-16">
              <div className="mb-6 flex items-center gap-3 text-white/90">
                <div className="flex h-10 w-10 items-center justify-center border border-white/20 bg-white/5 backdrop-blur-sm">
                  <div className="h-3.5 w-3.5 rotate-45 border border-white/85" />
                </div>
                <div>
                  <div className="font-serif text-2xl leading-none tracking-[0.1em]">
                    SNEAK SHOP
                  </div>
                </div>
              </div>

              <h1 className="mb-6 text-6xl font-black uppercase leading-[0.9] tracking-[0.02em] sm:text-7xl lg:text-[6.5rem]">
                {active.headline}
              </h1>

              <div className="mb-8 flex items-end gap-4">
                <div className="text-6xl font-black leading-none sm:text-7xl lg:text-[6.2rem]">
                  {active.discount}
                </div>
              </div>

              <Link href="/products">
                <Button
                  variant="outline"
                  className="border-white/80 bg-transparent px-7 py-3 text-sm font-semibold uppercase tracking-[0.24em] text-white hover:bg-white hover:text-black"
                >
                  Mua ngay
                </Button>
              </Link>
            </div>
          </div>
        </div>

        <Button
          type="button"
          variant="outline"
          size="icon"
          onClick={() => goTo(current - 1)}
          className="absolute left-4 top-1/2 z-10 -translate-y-1/2 border-white/30 bg-black/20 text-white backdrop-blur-md hover:bg-white hover:text-black"
          aria-label="Slide trước"
        >
          <ChevronLeft className="h-5 w-5" />
        </Button>

        <Button
          type="button"
          variant="outline"
          size="icon"
          onClick={() => goTo(current + 1)}
          className="absolute right-4 top-1/2 z-10 -translate-y-1/2 border-white/30 bg-black/20 text-white backdrop-blur-md hover:bg-white hover:text-black"
          aria-label="Slide sau"
        >
          <ChevronRight className="h-5 w-5" />
        </Button>

        {slides.length > 1 && (
          <div className="absolute bottom-6 left-1/2 z-10 flex -translate-x-1/2 items-center gap-2">
          {slides.map((slide, index) => (
            <button
              key={slide.id}
              type="button"
              onClick={() => setCurrent(index)}
              className={`h-2.5 rounded-full transition-all ${
                index === current ? "w-8 bg-white" : "w-2.5 bg-white/45 hover:bg-white/70"
              }`}
              aria-label={`Chuyển đến slide ${index + 1}`}
            />
          ))}
          </div>
        )}
      </div>
    </section>
  );
}
