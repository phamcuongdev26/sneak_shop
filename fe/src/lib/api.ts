import axios from "axios";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const api = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
  timeout: 8000,
});

api.interceptors.response.use((res) => {
  if (res.data && res.data.data !== undefined && res.data.result === undefined) {
    res.data.result = res.data.data;
  }
  return res;
});

api.interceptors.request.use((config) => {
  const token =
    typeof window !== "undefined" ? localStorage.getItem("token") : null;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  if (typeof FormData !== "undefined" && config.data instanceof FormData) {
    if (config.headers) {
      delete (config.headers as Record<string, string>)["Content-Type"];
      delete (config.headers as Record<string, string>)["content-type"];
    }
  }
  return config;
});

api.interceptors.response.use(undefined, (err) => {
  if (err.response?.status === 401 && typeof window !== "undefined") {
    localStorage.removeItem("token");
    window.location.href = "/login";
  }
  return Promise.reject(err);
});

export function getError(err: unknown): string {
  if (axios.isAxiosError(err)) {
    return err.response?.data?.message || err.message || "Có lỗi xảy ra";
  }
  return "Có lỗi xảy ra";
}
