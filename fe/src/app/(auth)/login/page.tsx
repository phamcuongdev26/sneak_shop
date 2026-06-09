"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { LogIn } from "lucide-react";
import { GoogleLogin } from "@react-oauth/google";
import { authApi } from "@/lib/api/auth";
import { useAuthStore } from "@/store/auth";
import { getError } from "@/lib/api";

export default function LoginPage() {
  const googleClientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
  const googleOAuthEnabled =
    !!googleClientId && googleClientId !== "YOUR_GOOGLE_CLIENT_ID";
  const [loading, setLoading] = useState(false);
  const { setAuth, user, token } = useAuthStore();

  useEffect(() => {
    if (!user || !token) return;
    window.location.replace(user.role === "admin" ? "/admin" : "/");
  }, [user, token]);

  const handleGoogleSuccess = async (credential: string) => {
    setLoading(true);
    try {
      const res = await authApi.googleLogin(credential);
      setAuth(res.data.result);
      toast.success("Đăng nhập thành công!");
      window.location.replace(res.data.result.role === "admin" ? "/admin" : "/");
    } catch (err) {
      toast.error(getError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-md space-y-6">
      {/* Brand */}
      <div className="text-center">
        <Link href="/" className="inline-block text-2xl font-black tracking-tight text-gray-900">
          SNEAK SHOP
        </Link>
        <p className="mt-1 text-sm text-gray-500">Đăng nhập để tiếp tục mua sắm</p>
      </div>

      {/* Card */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
        <h1 className="text-xl font-bold text-gray-900 mb-6">Đăng nhập</h1>

        <div className="mt-5 pt-5 border-t border-gray-100 space-y-3">
          <p className="text-center text-xs text-gray-400">Đăng nhập bằng Google Gmail</p>
          <div className="flex flex-col gap-2 items-stretch">
            {googleOAuthEnabled ? (
              <div className={`flex justify-center ${loading ? "opacity-50 pointer-events-none" : ""}`}>
                <GoogleLogin
                  onSuccess={(res) => res.credential && handleGoogleSuccess(res.credential)}
                  onError={() => toast.error("Đăng nhập Google thất bại")}
                  text="signin_with"
                  shape="rectangular"
                />
              </div>
            ) : (
              <div className="text-center text-sm text-red-500 bg-red-50 rounded-xl px-4 py-3">
                Google OAuth chưa được cấu hình. Vui lòng liên hệ quản trị viên.
              </div>
            )}
          </div>
        </div>

        <div className="mt-5 pt-5 border-t border-gray-100 text-center text-sm text-gray-500">
          Chưa có tài khoản Google?{" "}
          <Link href="/register" className="font-semibold text-gray-900 hover:underline">
            Đăng ký ngay
          </Link>
        </div>
      </div>

    </div>
  );
}
