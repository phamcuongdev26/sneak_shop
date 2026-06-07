import { api } from "@/lib/api";
import type { ApiResponse, PageResponse, Review } from "@/lib/types";

export const reviewsApi = {
  getByProduct: (productId: number, params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PageResponse<Review>>>(
      `/api/reviews/product/${productId}`,
      { params }
    ),

  getMyReviews: (params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PageResponse<Review>>>("/api/reviews/me", { params }),

  create: (data: {
    orderItemId: number;
    rating: number;
    comment?: string;
    productImageIds?: number[];
  }) => api.post<ApiResponse<Review>>("/api/reviews", data),

  // Admin
  adminGetAll: (params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PageResponse<Review>>>("/api/admin/reviews", { params }),

  shopReply: (reviewId: number, reply: string) =>
    api.post<ApiResponse<Review>>(`/api/admin/reviews/${reviewId}/reply`, {
      reply,
    }),
};
