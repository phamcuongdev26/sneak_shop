"use client";
import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { AuthResponse } from "@/lib/types";

interface AuthState {
  user: Omit<AuthResponse, "accessToken" | "tokenType"> | null;
  token: string | null;
  setAuth: (data: AuthResponse) => void;
  logout: () => void;
  isAdmin: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      setAuth: (data) => {
        const { accessToken, tokenType, ...user } = data;
        set({ user, token: accessToken });
        if (typeof window !== "undefined") {
          localStorage.setItem("token", accessToken);
        }
      },
      logout: () => {
        set({ user: null, token: null });
        if (typeof window !== "undefined") {
          localStorage.removeItem("token");
        }
      },
      isAdmin: () => get().user?.role === "admin",
    }),
    { name: "auth-store", partialize: (s) => ({ user: s.user, token: s.token }) }
  )
);
