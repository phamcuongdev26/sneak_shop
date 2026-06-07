import { api } from "@/lib/api";
import type { ApiResponse, PageResponse, BlogPost } from "@/lib/types";

export const blogApi = {
  getAll: (params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PageResponse<BlogPost>>>("/api/blog", { params }),

  getBySlug: (slug: string) =>
    api.get<ApiResponse<BlogPost>>(`/api/blog/${slug}`),

  adminGetAll: (params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PageResponse<BlogPost>>>("/api/admin/blog", { params }),

  adminCreate: (data: Partial<BlogPost>) =>
    api.post<ApiResponse<BlogPost>>("/api/admin/blog", data),

  adminUpdate: (id: number, data: Partial<BlogPost>) =>
    api.put<ApiResponse<BlogPost>>(`/api/admin/blog/${id}`, data),

  adminDelete: (id: number) =>
    api.delete<ApiResponse<void>>(`/api/admin/blog/${id}`),
};
