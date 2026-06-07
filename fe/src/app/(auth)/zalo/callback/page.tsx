"use client";
import { Suspense, useEffect, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import { authApi } from "@/lib/api/auth";
import { useAuthStore } from "@/store/auth";

function ZaloCallbackInner() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const { setAuth } = useAuthStore();
  const called = useRef(false);

  useEffect(() => {
    if (called.current) return;
    called.current = true;

    const code = searchParams.get("code");
    const codeVerifier = sessionStorage.getItem("zalo_code_verifier");

    if (!code || !codeVerifier) {
      toast.error("Đăng nhập Zalo thất bại");
      router.replace("/login");
      return;
    }

    sessionStorage.removeItem("zalo_code_verifier");

    authApi.zaloLogin(code, codeVerifier)
      .then((res) => {
        setAuth(res.data.result);
        toast.success("Đăng nhập Zalo thành công!");
        window.location.replace(res.data.result.role === "admin" ? "/admin" : "/");
      })
      .catch(() => {
        toast.error("Đăng nhập Zalo thất bại, vui lòng thử lại");
        router.replace("/login");
      });
  }, [searchParams, router, setAuth]);

  return (
    <div className="flex flex-col items-center justify-center min-h-screen gap-3">
      <span className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin" />
      <p className="text-sm text-gray-500">Đang xử lý đăng nhập Zalo...</p>
    </div>
  );
}

export default function ZaloCallbackPage() {
  return (
    <Suspense>
      <ZaloCallbackInner />
    </Suspense>
  );
}
