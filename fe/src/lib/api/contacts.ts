import { api } from "@/lib/api";
import type { ApiResponse } from "@/lib/types";

export interface ContactItem {
  id: number;
  name: string;
  email: string;
  subject: string;
  message: string;
  imageUrls: string | null;
  status: "pending" | "replied";
  replyText: string | null;
  repliedAt: string | null;
  createdAt: string;
  userId: number | null;
}

interface Page<T> { content: T[]; totalPages: number; totalElements: number; }

export const contactsApi = {
  adminGetAll: (params: { status?: string; page?: number; size?: number }) =>
    api.get<ApiResponse<Page<ContactItem>>>("/api/admin/contacts", { params }),

  adminGetOne: (id: number) =>
    api.get<ApiResponse<ContactItem>>(`/api/admin/contacts/${id}`),

  adminReply: (id: number, replyText: string) =>
    api.post<ApiResponse<ContactItem>>(`/api/admin/contacts/${id}/reply`, { replyText }),

  getMyContacts: (params: { page?: number; size?: number }) =>
    api.get<ApiResponse<Page<ContactItem>>>("/api/contact/my", { params }),
};
