"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { useState } from "react";
import { GoogleLogin } from "@react-oauth/google";
import { authApi } from "@/lib/api/auth";
import { useAuthStore } from "@/store/auth";
import { getError } from "@/lib/api";

export default function RegisterPage() {
  const googleClientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
  const googleOAuthEnabled = !!googleClientId && googleClientId !== "YOUR_GOOGLE_CLIENT_ID";

  const [loading, setLoading] = useState(false);
  const { setAuth } = useAuthStore();
  const router = useRouter();

  const handleGoogleSuccess = async (credential: string) => {
    setLoading(true);
    try {
      const res = await authApi.googleLogin(credential);
      setAuth(res.data.result);
      toast.success("Đăng ký thành công! Chào mừng đến Sneak Shop");
      router.push("/");
    } catch (err) {
      toast.error(getError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-md space-y-6">
      <div className="text-center">
        <Link href="/" className="inline-block text-2xl font-black tracking-tight text-gray-900">
          SNEAK SHOP
        </Link>
        <p className="mt-2 text-sm text-gray-500">Tạo tài khoản để mua sắm dễ dàng hơn</p>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 space-y-6">
        <h1 className="text-xl font-bold text-gray-900 text-center">Đăng ký tài khoản</h1>

        {googleOAuthEnabled ? (
          <div className="space-y-4">
            <p className="text-sm text-gray-500 text-center">
              Đăng ký bằng tài khoản Google Gmail.
            </p>
            <div className={`flex justify-center ${loading ? "opacity-50 pointer-events-none" : ""}`}>
              <GoogleLogin
                onSuccess={(res) => res.credential && handleGoogleSuccess(res.credential)}
                onError={() => toast.error("Đăng ký Google thất bại, vui lòng thử lại")}
                text="signup_with"
                shape="rectangular"
                size="large"
                width="320"
              />
            </div>
            {loading && (
              <div className="flex items-center justify-center gap-2 text-sm text-gray-500">
                <span className="w-4 h-4 border-2 border-gray-400 border-t-transparent rounded-full animate-spin" />
                Đang xử lý...
              </div>
            )}
          </div>
        ) : (
          <div className="text-center text-sm text-red-500 bg-red-50 rounded-xl px-4 py-3">
            Google OAuth chưa được cấu hình. Vui lòng liên hệ quản trị viên.
          </div>
        )}

        <div className="pt-2 border-t border-gray-100 text-center text-sm text-gray-500">
          Đã có tài khoản?{" "}
          <Link href="/login" className="font-semibold text-gray-900 hover:underline">
            Đăng nhập
          </Link>
        </div>
      </div>
    </div>
  );
}
