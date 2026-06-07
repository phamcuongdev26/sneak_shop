"use client";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { blogApi } from "@/lib/api/blog";
import { formatDateOnly } from "@/lib/format";
import type { BlogPost } from "@/lib/types";
import { Pencil, Trash2, Plus } from "lucide-react";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

const empty: Partial<BlogPost> = { title: "", slug: "", summary: "", content: "", coverImageUrl: "", status: "draft" };

export default function AdminBlogPage() {
  const [posts, setPosts] = useState<BlogPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<Partial<BlogPost>>(empty);
  const [editing, setEditing] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const r = await blogApi.adminGetAll({ page, size: 20 });
      setPosts(r.data.result.content);
      setTotalPages(r.data.result.totalPages);
    } catch {}
    setLoading(false);
  };

  useEffect(() => { load(); }, [page]);

  const set = (k: string) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }));

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      if (editing !== null) {
        await blogApi.adminUpdate(editing, form);
        toast.success("Đã cập nhật bài viết");
      } else {
        await blogApi.adminCreate(form);
        toast.success("Đã tạo bài viết");
      }
      setOpen(false);
      load();
    } catch { toast.error("Có lỗi xảy ra"); }
    setSaving(false);
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Xóa bài viết này?")) return;
    try {
      await blogApi.adminDelete(id);
      toast.success("Đã xóa");
      load();
    } catch { toast.error("Không thể xóa"); }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Quản lý bài viết</h1>
        <Button onClick={() => { setForm(empty); setEditing(null); setOpen(true); }} className="gap-2">
          <Plus className="w-4 h-4" />Viết bài mới
        </Button>
      </div>

      <div className="bg-white rounded-xl border overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b">
            <tr>
              {["Tiêu đề", "Slug", "Trạng thái", "Ngày tạo", ""].map((h) => (
                <th key={h} className="text-left py-3 px-4 text-xs font-semibold text-gray-500">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={5} className="px-4 py-3"><Skeleton className="h-5" /></td></tr>
              ))
            ) : posts.length === 0 ? (
              <tr><td colSpan={5} className="text-center py-12 text-gray-400">Chưa có bài viết</td></tr>
            ) : (
              posts.map((p) => (
                <tr key={p.id} className="border-b last:border-0 hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium max-w-48 truncate">{p.title}</td>
                  <td className="px-4 py-3 text-gray-400">{p.slug}</td>
                  <td className="px-4 py-3">
                    <Badge variant={p.status === "published" ? "default" : "secondary"} className="text-xs">
                      {p.status === "published" ? "Đã đăng" : "Nháp"}
                    </Badge>
                  </td>
                  <td className="px-4 py-3 text-gray-400 text-xs">{formatDateOnly(p.createdAt)}</td>
                  <td className="px-4 py-3">
                    <div className="flex gap-1">
                      <Button size="sm" variant="outline" onClick={() => { setForm(p); setEditing(p.id); setOpen(true); }}>
                        <Pencil className="w-3 h-3" />
                      </Button>
                      <Button size="sm" variant="outline" className="text-red-500" onClick={() => handleDelete(p.id)}>
                        <Trash2 className="w-3 h-3" />
                      </Button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <Button variant="outline" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Trước</Button>
          <span className="flex items-center px-3 text-sm">{page + 1}/{totalPages}</span>
          <Button variant="outline" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>Tiếp</Button>
        </div>
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader><DialogTitle>{editing !== null ? "Sửa bài viết" : "Viết bài mới"}</DialogTitle></DialogHeader>
          <form onSubmit={handleSave} className="space-y-3 max-h-[70vh] overflow-y-auto pr-1">
            {[
              { id: "title", label: "Tiêu đề", component: "input" },
              { id: "slug", label: "Slug", component: "input" },
              { id: "coverImageUrl", label: "URL ảnh bìa", component: "input" },
              { id: "summary", label: "Tóm tắt", component: "textarea" },
              { id: "content", label: "Nội dung (HTML)", component: "textarea", rows: 8 },
            ].map(({ id, label, component, rows }) => (
              <div key={id}>
                <p className="text-sm font-medium mb-1">{label}</p>
                {component === "textarea" ? (
                  <Textarea rows={rows || 3} value={(form as Record<string, string>)[id] || ""} onChange={set(id)} />
                ) : (
                  <Input value={(form as Record<string, string>)[id] || ""} onChange={set(id)} required={id === "title" || id === "slug"} />
                )}
              </div>
            ))}
            <div>
              <p className="text-sm font-medium mb-1">Trạng thái</p>
              <Select value={form.status || "draft"} onValueChange={(v) => setForm((f) => ({ ...f, status: v ?? "draft" }))}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="draft">Nháp</SelectItem>
                  <SelectItem value="published">Đăng</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="flex gap-2 justify-end pt-2">
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>Hủy</Button>
              <Button type="submit" disabled={saving}>{saving ? "Đang lưu..." : "Lưu"}</Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
