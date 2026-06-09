"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Image from "next/image";
import { toast } from "sonner";
import {
  GripVertical,
  ImagePlus,
  Pencil,
  Plus,
  RefreshCw,
  Trash2,
  Upload,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { bannersApi } from "@/lib/api/banners";
import type { Banner } from "@/lib/types";

const MAX_BANNERS = 9;

type BannerDraft = {
  title: string;
  imageUrl: string;
  linkUrl: string;
  position: string;
  isActive: boolean;
  sortOrder: number;
};

const emptyDraft = (sortOrder = 0): BannerDraft => ({
  title: "",
  imageUrl: "",
  linkUrl: "",
  position: "hero",
  isActive: true,
  sortOrder,
});

const sortBanners = (items: Banner[]) =>
  [...items].sort((a, b) => {
    if (a.sortOrder !== b.sortOrder) return a.sortOrder - b.sortOrder;
    return b.id - a.id;
  });

const moveItem = <T,>(items: T[], from: number, to: number) => {
  const next = [...items];
  const [item] = next.splice(from, 1);
  next.splice(to, 0, item);
  return next;
};

export default function AdminBannersPage() {
  const [banners, setBanners] = useState<Banner[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [draft, setDraft] = useState<BannerDraft>(emptyDraft());
  const [preview, setPreview] = useState("");
  const [draggedId, setDraggedId] = useState<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const orderedBanners = useMemo(() => sortBanners(banners), [banners]);
  const canAdd = orderedBanners.length < MAX_BANNERS;

  const load = async () => {
    setLoading(true);
    try {
      const r = await bannersApi.adminGetAll();
      setBanners(sortBanners(r.data.result ?? []));
    } catch {
      toast.error("Không thể tải danh sách banner");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    if (!open) {
      setPreview("");
      setUploading(false);
    }
  }, [open]);

  const openCreate = () => {
    if (!canAdd) {
      toast.error("Chỉ được tối đa 9 banner");
      return;
    }
    const nextSortOrder =
      orderedBanners.reduce((max, banner) => Math.max(max, banner.sortOrder), -1) + 1;
    setEditingId(null);
    setDraft(emptyDraft(nextSortOrder));
    setPreview("");
    setOpen(true);
  };

  const openEdit = (banner: Banner) => {
    setEditingId(banner.id);
    setDraft({
      title: banner.title ?? "",
      imageUrl: banner.imageUrl ?? "",
      linkUrl: banner.linkUrl ?? "",
      position: banner.position || "hero",
      isActive: banner.isActive,
      sortOrder: banner.sortOrder ?? 0,
    });
    setPreview(banner.imageUrl);
    setOpen(true);
  };

  const uploadFile = async (file: File) => {
    if (!file.type.startsWith("image/")) {
      toast.error("Chỉ nhận file ảnh");
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      toast.error("Ảnh tối đa 10MB");
      return;
    }

    setUploading(true);
    try {
      const res = await bannersApi.uploadImage(file);
      const url = res.data.url;
      setDraft((current) => ({ ...current, imageUrl: url }));
      setPreview(url);
      toast.success("Đã tải ảnh lên");
    } catch {
      toast.error("Không thể tải ảnh lên");
    } finally {
      setUploading(false);
    }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    await uploadFile(file);
    e.target.value = "";
  };

  const handleDropFile = async (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    const file = e.dataTransfer.files?.[0];
    if (!file) return;
    await uploadFile(file);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!draft.imageUrl.trim()) {
      toast.error("Vui lòng chọn hoặc tải lên ảnh banner");
      return;
    }

    setSaving(true);
    try {
      const payload = {
        title: draft.title.trim() || null,
        imageUrl: draft.imageUrl.trim(),
        linkUrl: draft.linkUrl.trim() || null,
        position: draft.position.trim() || "hero",
        isActive: draft.isActive,
        sortOrder: draft.sortOrder,
      };

      if (editingId !== null) {
        await bannersApi.adminUpdate(editingId, payload);
        toast.success("Đã cập nhật banner");
      } else {
        await bannersApi.adminCreate(payload);
        toast.success("Đã tạo banner");
      }

      setOpen(false);
      await load();
    } catch {
      toast.error("Có lỗi xảy ra");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Xóa banner này?")) return;
    try {
      await bannersApi.adminDelete(id);
      toast.success("Đã xóa banner");
      await load();
    } catch {
      toast.error("Không thể xóa banner");
    }
  };

  const persistOrder = async (next: Banner[]) => {
    const normalized = next.map((banner, index) => ({ ...banner, sortOrder: index }));
    setBanners(normalized);
    try {
      await bannersApi.adminReorder(normalized.map((banner) => banner.id));
      toast.success("Đã cập nhật thứ tự banner");
    } catch {
      toast.error("Không thể lưu thứ tự banner");
      await load();
    } finally {
      setDraggedId(null);
    }
  };

  const handleDropOnBanner = async (targetId: number) => {
    if (draggedId == null || draggedId === targetId) return;
    const fromIndex = orderedBanners.findIndex((banner) => banner.id === draggedId);
    const toIndex = orderedBanners.findIndex((banner) => banner.id === targetId);
    if (fromIndex < 0 || toIndex < 0) return;
    await persistOrder(moveItem(orderedBanners, fromIndex, toIndex));
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="space-y-1">
          <h1 className="text-2xl font-bold">Quản lý Banner</h1>
          <p className="text-sm text-gray-500">
            Kéo thả để đổi thứ tự, tải ảnh trực tiếp, tối đa {MAX_BANNERS} banner.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="outline" className="h-9 px-3 text-sm">
            {orderedBanners.length}/{MAX_BANNERS}
          </Badge>
          <Button variant="outline" onClick={() => void load()} className="gap-2">
            <RefreshCw className="h-4 w-4" />
            Tải lại
          </Button>
          <Button onClick={openCreate} className="gap-2" disabled={!canAdd}>
            <Plus className="h-4 w-4" />
            Thêm banner
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-72 rounded-2xl" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {orderedBanners.map((banner, index) => (
            <div
              key={banner.id}
              draggable
              onDragStart={() => setDraggedId(banner.id)}
              onDragOver={(e) => e.preventDefault()}
              onDrop={() => void handleDropOnBanner(banner.id)}
              className="group overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm transition hover:shadow-md"
            >
              <div className="relative aspect-[16/10] bg-gray-100">
                <Image
                  src={banner.imageUrl}
                  alt={banner.title || "Ảnh banner"}
                  fill
                  className="object-cover"
                  sizes="(max-width: 1280px) 100vw, 33vw"
                />

                <div className="absolute left-3 top-3 flex items-center gap-2">
                  <span className="inline-flex h-8 w-8 items-center justify-center rounded-full bg-black/70 text-xs font-semibold text-white">
                    {index + 1}
                  </span>
                  <span className="inline-flex items-center gap-1 rounded-full bg-black/60 px-2.5 py-1 text-[11px] font-medium text-white backdrop-blur">
                    <GripVertical className="h-3.5 w-3.5" />
                    Kéo thả
                  </span>
                </div>

                <div className="absolute right-3 top-3 flex gap-2">
                  <Button
                    size="icon"
                    variant="secondary"
                    className="h-8 w-8 rounded-full bg-white/95 shadow-sm"
                    onClick={() => openEdit(banner)}
                  >
                    <Pencil className="h-4 w-4" />
                  </Button>
                  <Button
                    size="icon"
                    variant="secondary"
                    className="h-8 w-8 rounded-full bg-white/95 text-red-600 shadow-sm hover:text-red-700"
                    onClick={() => void handleDelete(banner.id)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>

                <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/80 via-black/30 to-transparent px-4 pb-4 pt-10 text-white">
                  <p className="truncate text-base font-semibold">
                    {banner.title || "Không tiêu đề"}
                  </p>
                  <div className="mt-2 flex flex-wrap gap-2">
                    <Badge className="bg-white/15 text-white hover:bg-white/15">{banner.position}</Badge>
                    <Badge variant={banner.isActive ? "default" : "secondary"}>
                      {banner.isActive ? "Đang hiển thị" : "Đang ẩn"}
                    </Badge>
                  </div>
                </div>
              </div>

              <div className="border-t border-gray-100 p-4">
                <p className="text-xs uppercase tracking-[0.16em] text-gray-400">Link</p>
                <p className="mt-1 truncate text-sm text-gray-700">
                  {banner.linkUrl || "Không có"}
                </p>
              </div>
            </div>
          ))}

          {canAdd && (
            <button
              type="button"
              onClick={openCreate}
              className="flex min-h-[18rem] items-center justify-center rounded-2xl border-2 border-dashed border-gray-300 bg-gray-50 text-gray-500 transition hover:border-gray-400 hover:bg-gray-100"
            >
              <div className="flex flex-col items-center gap-3">
                <span className="flex h-14 w-14 items-center justify-center rounded-full bg-white shadow-sm">
                  <ImagePlus className="h-7 w-7" />
                </span>
                <div className="text-center">
                  <p className="font-medium text-gray-900">Thêm banner mới</p>
                  <p className="text-sm text-gray-500">Tải ảnh và nhập link</p>
                </div>
              </div>
            </button>
          )}
        </div>
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>{editingId !== null ? "Sửa banner" : "Thêm banner"}</DialogTitle>
          </DialogHeader>

          <form onSubmit={handleSave} className="space-y-4">
            <div
              onDragOver={(e) => e.preventDefault()}
              onDrop={handleDropFile}
              className="rounded-2xl border-2 border-dashed border-gray-200 bg-gray-50 p-4"
            >
              <div className="flex flex-col gap-4 md:flex-row">
                <div className="relative aspect-[16/10] w-full overflow-hidden rounded-xl bg-white md:w-72">
                  {preview ? (
                    <Image src={preview} alt="Preview banner" fill className="object-cover" />
                  ) : (
                    <div className="flex h-full flex-col items-center justify-center gap-2 text-gray-400">
                      <Upload className="h-8 w-8" />
                      <p className="text-sm">Kéo thả ảnh vào đây</p>
                    </div>
                  )}
                </div>

                <div className="flex flex-1 flex-col justify-between gap-3">
                  <div className="space-y-2">
                    <p className="text-sm font-medium">Ảnh banner</p>
                    <p className="text-sm text-gray-500">
                      Chọn file ảnh hoặc dán URL. File sẽ được tải lên ngay khi chọn.
                    </p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Button
                      type="button"
                      variant="outline"
                      className="gap-2"
                      onClick={() => fileInputRef.current?.click()}
                      disabled={uploading}
                    >
                      <Upload className="h-4 w-4" />
                      {uploading ? "Đang tải..." : "Chọn ảnh"}
                    </Button>
                    <Button
                      type="button"
                      variant="ghost"
                      onClick={() => {
                        setDraft((current) => ({ ...current, imageUrl: "" }));
                        setPreview("");
                      }}
                    >
                      Bỏ chọn ảnh
                    </Button>
                  </div>
                </div>
              </div>

              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={handleFileChange}
              />
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2 md:col-span-2">
                <p className="text-sm font-medium">Tiêu đề</p>
                <Input
                  value={draft.title}
                  onChange={(e) => setDraft((current) => ({ ...current, title: e.target.value }))}
                  placeholder="Ví dụ: Bộ sưu tập mùa hè"
                />
              </div>

              <div className="space-y-2 md:col-span-2">
                <p className="text-sm font-medium">URL ảnh</p>
                <Input
                  value={draft.imageUrl}
                  onChange={(e) => {
                    const value = e.target.value;
                    setDraft((current) => ({ ...current, imageUrl: value }));
                    setPreview(value);
                  }}
                  placeholder="https://..."
                />
              </div>

              <div className="space-y-2 md:col-span-2">
                <p className="text-sm font-medium">Đường dẫn liên kết</p>
                <Input
                  value={draft.linkUrl}
                  onChange={(e) => setDraft((current) => ({ ...current, linkUrl: e.target.value }))}
                  placeholder="/products?categorySlug=..."
                />
              </div>

              <div className="space-y-2">
                <p className="text-sm font-medium">Vị trí</p>
                <Input
                  value={draft.position}
                  onChange={(e) => setDraft((current) => ({ ...current, position: e.target.value }))}
                  placeholder="hero"
                />
              </div>

              <label className="flex items-center justify-between rounded-xl border border-gray-200 px-4 py-3">
                <div>
                  <p className="text-sm font-medium">Hiển thị</p>
                  <p className="text-xs text-gray-500">Bật để banner xuất hiện trên trang chủ</p>
                </div>
                <input
                  type="checkbox"
                  checked={draft.isActive}
                  onChange={(e) =>
                    setDraft((current) => ({ ...current, isActive: e.target.checked }))
                  }
                  className="h-4 w-4 rounded border-gray-300"
                />
              </label>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                Hủy
              </Button>
              <Button type="submit" disabled={saving || uploading}>
                {saving ? "Đang lưu..." : "Lưu banner"}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
