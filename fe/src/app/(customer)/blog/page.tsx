import Link from "next/link";
import Image from "next/image";
import { blogApi } from "@/lib/api/blog";
import { formatDateOnly } from "@/lib/format";

export const revalidate = 60;

export default async function BlogListPage() {
  let posts: Awaited<ReturnType<typeof blogApi.getAll>>["data"]["result"]["content"] = [];
  try {
    const r = await blogApi.getAll({ page: 0, size: 12 });
    posts = r.data.result.content;
  } catch {}

  return (
    <div className="max-w-5xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold mb-8">Bài viết</h1>
      {posts.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <div className="text-4xl mb-3">📝</div>
          <p>Chưa có bài viết nào</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {posts.map((post) => (
            <Link key={post.id} href={`/blog/${post.slug}`}>
              <div className="bg-white border rounded-xl overflow-hidden hover:shadow-md transition-shadow">
                {post.coverImageUrl && (
                  <div className="relative h-48 bg-gray-50">
                    <Image src={post.coverImageUrl} alt={post.title} fill className="object-cover" />
                  </div>
                )}
                <div className="p-4">
                  <p className="text-xs text-gray-400 mb-1">{formatDateOnly(post.createdAt)}</p>
                  <h2 className="font-bold text-gray-900 line-clamp-2 mb-2">{post.title}</h2>
                  {post.summary && <p className="text-sm text-gray-500 line-clamp-3">{post.summary}</p>}
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
