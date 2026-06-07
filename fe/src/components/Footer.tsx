import Link from "next/link";

export default function Footer() {
  return (
    <footer className="bg-gray-900 text-gray-300 mt-auto">
      <div className="max-w-7xl mx-auto px-4 py-10 grid grid-cols-1 md:grid-cols-3 gap-8">
        <div>
          <h3 className="text-white font-bold text-lg mb-3">SNEAK SHOP</h3>
          <p className="text-sm">Giày sneaker chính hãng, giao hàng toàn quốc.</p>
        </div>
        <div>
          <h4 className="text-white font-semibold mb-3">Liên kết</h4>
          <div className="flex flex-col gap-1 text-sm">
            <Link href="/products" className="hover:text-white transition-colors">Sản phẩm</Link>
            <Link href="/blog" className="hover:text-white transition-colors">Bài viết</Link>
          </div>
        </div>
        <div>
          <h4 className="text-white font-semibold mb-3">Hỗ trợ</h4>
          <div className="flex flex-col gap-1 text-sm">
            <span>Email: support@sneakshop.vn</span>
            <span>Hotline: 0123 456 789</span>
          </div>
        </div>
      </div>
      <div className="border-t border-gray-700 text-center py-4 text-xs text-gray-500">
        © {new Date().getFullYear()} Sneak Shop. Mọi quyền được bảo lưu.
      </div>
    </footer>
  );
}
