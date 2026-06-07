import { api } from "@/lib/api";
import type { ApiResponse, Banner } from "@/lib/types";

export const bannersApi = {
  getActive: () => api.get<ApiResponse<Banner[]>>("/api/banners"),

  adminGetAll: () => api.get<ApiResponse<Banner[]>>("/api/admin/banners"),

  adminCreate: (data: Partial<Banner>) =>
    api.post<ApiResponse<Banner>>("/api/admin/banners", data),

  adminUpdate: (id: number, data: Partial<Banner>) =>
    api.put<ApiResponse<Banner>>(`/api/admin/banners/${id}`, data),

  adminDelete: (id: number) =>
    api.delete<ApiResponse<void>>(`/api/admin/banners/${id}`),
};
