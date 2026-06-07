"use client";
import { useEffect, useState } from "react";
import Image from "next/image";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { bannersApi } from "@/lib/api/banners";
import type { Banner } from "@/lib/types";
import { Pencil, Trash2, Plus } from "lucide-react";

const empty: Partial<Banner> = { title: "", imageUrl: "", linkUrl: "", position: "hero", isActive: true, sortOrder: 0 };

export default function AdminBannersPage() {
  const [banners, setBanners] = useState<Banner[]>([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<Partial<Banner>>(empty);
  const [editing, setEditing] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);

  const load = () => {
    setLoading(true);
    bannersApi.adminGetAll()
      .then((r) => setBanners(r.data.result))
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const set = (k: string) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value }));

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      if (editing !== null) {
        await bannersApi.adminUpdate(editing, form);
        toast.success("Đã cập nhật banner");
      } else {
        await bannersApi.adminCreate(form);
        toast.success("Đã tạo banner");
      }
      setOpen(false);
      load();
    } catch { toast.error("Có lỗi xảy ra"); }
    setSaving(false);
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Xóa banner này?")) return;
    try {
      await bannersApi.adminDelete(id);
      toast.success("Đã xóa");
      load();
    } catch { toast.error("Không thể xóa"); }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Quản lý Banner</h1>
        <Button onClick={() => { setForm(empty); setEditing(null); setOpen(true); }} className="gap-2">
          <Plus className="w-4 h-4" />Thêm banner
        </Button>
      </div>

      {loading ? (
        <div className="grid grid-cols-2 gap-4">
          {[1, 2].map((i) => <Skeleton key={i} className="h-40 rounded-xl" />)}
        </div>
      ) : banners.length === 0 ? (
        <div className="text-center py-12 text-gray-400">Chưa có banner</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {banners.map((b) => (
            <div key={b.id} className="bg-white border rounded-xl overflow-hidden">
              <div className="relative h-36 bg-gray-50">
                {b.imageUrl ? (
                  <Image src={b.imageUrl} alt={b.title || "Ảnh banner"} fill className="object-cover" />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-gray-300">Chưa có ảnh</div>
                )}
              </div>
              <div className="p-3 flex items-center justify-between">
                <div>
                  <p className="font-medium text-sm">{b.title || "Không tiêu đề"}</p>
                  <div className="flex gap-2 mt-1">
                    <Badge variant="outline" className="text-xs">{b.position}</Badge>
                    <Badge variant={b.isActive ? "default" : "secondary"} className="text-xs">
                      {b.isActive ? "Hiện" : "Ẩn"}
                    </Badge>
                  </div>
                </div>
                <div className="flex gap-1">
                  <Button size="sm" variant="outline" onClick={() => { setForm(b); setEditing(b.id); setOpen(true); }}>
                    <Pencil className="w-3 h-3" />
                  </Button>
                  <Button size="sm" variant="outline" className="text-red-500" onClick={() => handleDelete(b.id)}>
                    <Trash2 className="w-3 h-3" />
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>{editing !== null ? "Sửa banner" : "Thêm banner"}</DialogTitle></DialogHeader>
          <form onSubmit={handleSave} className="space-y-3">
            {[
              { id: "title", label: "Tiêu đề" },
              { id: "imageUrl", label: "URL ảnh", required: true },
              { id: "linkUrl", label: "Đường dẫn liên kết" },
              { id: "position", label: "Vị trí (hero, sub, ...)", required: true },
            ].map(({ id, label, required }) => (
              <div key={id}>
                <p className="text-sm font-medium mb-1">{label}</p>
                <Input value={(form as Record<string, string>)[id] || ""} onChange={set(id)} required={required} />
              </div>
            ))}
            <div className="flex items-center gap-2">
              <input type="checkbox" id="isActive" checked={!!form.isActive} onChange={set("isActive")} />
              <label htmlFor="isActive" className="text-sm">Hiển thị</label>
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
