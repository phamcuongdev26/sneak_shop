import { productsApi } from "@/lib/api/products";
import { bannersApi } from "@/lib/api/banners";
import HomeHeroCarousel from "@/components/home/HomeHeroCarousel";
import HomeFeaturedProducts from "@/components/home/HomeFeaturedProducts";

export const revalidate = 60;

async function getHomeData() {
  try {
    const dataPromise = Promise.allSettled([
      productsApi.search({ status: "active", size: 8, sort: "newest" }),
      bannersApi.getActive(),
    ]);
    const timeoutPromise = new Promise<{
      products: never[];
      banners: never[];
    }>((resolve) => {
      setTimeout(() => resolve({ products: [], banners: [] }), 2000);
    });
    const result = await Promise.race([
      dataPromise,
      timeoutPromise,
    ]);
    if (!Array.isArray(result)) {
      return result;
    }
    const [productsRes, bannersRes] = result;
    return {
      products: productsRes.status === "fulfilled" ? productsRes.value.data.result.content : [],
      banners: bannersRes.status === "fulfilled" ? bannersRes.value.data.result : [],
    };
  } catch {
    return { products: [], banners: [] };
  }
}

export default async function HomePage() {
  const { products, banners } = await getHomeData();

  return (
    <div className="bg-[#f6f4f0]">
      <HomeHeroCarousel banners={banners} />
      <HomeFeaturedProducts products={products} />
    </div>
  );
}
