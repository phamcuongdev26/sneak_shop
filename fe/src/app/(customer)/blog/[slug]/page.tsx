import Image from "next/image";
import { blogApi } from "@/lib/api/blog";
import { formatDateOnly } from "@/lib/format";
import { notFound } from "next/navigation";

export const revalidate = 60;

export default async function BlogDetailPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  let post;
  try {
    const r = await blogApi.getBySlug(slug);
    post = r.data.result;
  } catch {
    notFound();
  }

  return (
    <article className="max-w-3xl mx-auto px-4 py-10">
      {post.coverImageUrl && (
        <div className="relative h-72 rounded-xl overflow-hidden mb-6">
          <Image src={post.coverImageUrl} alt={post.title} fill className="object-cover" />
        </div>
      )}
      <p className="text-sm text-gray-400 mb-2">{formatDateOnly(post.createdAt)}</p>
      <h1 className="text-3xl font-black mb-4 text-gray-900">{post.title}</h1>
      {post.summary && <p className="text-lg text-gray-500 mb-6 leading-relaxed">{post.summary}</p>}
      {post.content && (
        <div
          className="prose prose-gray max-w-none"
          dangerouslySetInnerHTML={{ __html: post.content }}
        />
      )}
    </article>
  );
}
