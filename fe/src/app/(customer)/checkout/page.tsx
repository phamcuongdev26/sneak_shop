"use client";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuthStore } from "@/store/auth";
import { useCartStore } from "@/store/cart";
import { ordersApi } from "@/lib/api/orders";
import { addressesApi } from "@/lib/api/addresses";
import { formatVND } from "@/lib/format";
import { vnRegions } from "@/lib/vn-regions";
import type { Province, District, Ward } from "@/lib/vn-regions";

type BuyNowItem = {
  productId: number;
  variantId?: number;
  colorId?: number;
  quantity: number;
  productName: string;
  variantName: string | null;
  colorName: string | null;
  productImage: string | null;
  unitPrice: number;
};

export default function CheckoutPage() {
  const { user } = useAuthStore();
  const { items, clear } = useCartStore();
  const router = useRouter();
  const [buyNowItems, setBuyNowItems] = useState<BuyNowItem[]>([]);
  const [ready, setReady] = useState(false);
  const [savingAddress, setSavingAddress] = useState(false);
  const [provinces, setProvinces] = useState<Province[]>([]);
  const [districts, setDistricts] = useState<District[]>([]);
  const [wards, setWards] = useState<Ward[]>([]);
  const [provinceCode, setProvinceCode] = useState<number | null>(null);
  const [districtCode, setDistrictCode] = useState<number | null>(null);
  const [provinceName, setProvinceName] = useState("");
  const [districtName, setDistrictName] = useState("");
  const [regionsLoading, setRegionsLoading] = useState(false);

  const [form, setForm] = useState({
    recipientName: "",
    recipientPhone: "",
    paymentMethod: "cod",
    note: "",
  });
  const [addressForm, setAddressForm] = useState({
    address: "",
    provinceCode: null as number | null,
    districtCode: null as number | null,
    ward: "",
    district: "",
    city: "",
    isDefault: false,
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!user) return;
    setRegionsLoading(true);
    vnRegions.provinces()
      .then((pvs) => setProvinces(pvs))
      .catch(() => toast.error("Không tải được danh sách địa chính"))
      .finally(() => setRegionsLoading(false));
  }, [user]);

  const handleProvinceChange = async (code: number, name: string) => {
    setProvinceCode(code);
    setProvinceName(name);
    setDistrictName("");
    setAddressForm((f) => ({ ...f, provinceCode: code, city: name, district: "", ward: "", districtCode: null }));
    setDistricts([]);
    setWards([]);
    setDistrictCode(null);
    if (!code) return;
    try {
      const dists = await vnRegions.districts(code);
      setDistricts(dists);
    } catch {
      toast.error("Không tải được quận/huyện");
    }
  };

  const handleDistrictChange = async (code: number, name: string) => {
    setDistrictCode(code);
    setDistrictName(name);
    setAddressForm((f) => ({ ...f, districtCode: code, district: name, ward: "" }));
    setWards([]);
    if (!code) return;
    try {
      const ws = await vnRegions.wards(code);
      setWards(ws);
    } catch {
      toast.error("Không tải được phường/xã");
    }
  };

  const handleSaveAddress = async () => {
    if (!addressForm.address.trim()) { toast.error("Vui lòng nhập địa chỉ"); return; }
    if (addressForm.provinceCode == null) { toast.error("Vui lòng chọn tỉnh / thành phố"); return; }
    if (addressForm.districtCode == null) { toast.error("Vui lòng chọn quận / huyện"); return; }
    if (!addressForm.ward.trim()) { toast.error("Vui lòng chọn phường / xã"); return; }

    setSavingAddress(true);
    try {
      await addressesApi.create({
        recipientName: form.recipientName,
        recipientPhone: form.recipientPhone,
        address: addressForm.address,
        provinceCode: addressForm.provinceCode,
        districtCode: addressForm.districtCode,
        ward: addressForm.ward,
        district: addressForm.district,
        city: addressForm.city,
        isDefault: addressForm.isDefault,
      });
      setAddressForm({
        address: "",
        provinceCode: null,
        districtCode: null,
        ward: "",
        district: "",
        city: "",
        isDefault: false,
      });
      setProvinceCode(null);
      setDistrictCode(null);
      setProvinceName("");
      setDistrictName("");
      setDistricts([]);
      setWards([]);
      toast.success("Đã thêm địa chỉ");
    } catch (err: unknown) {
      toast.error((err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Có lỗi xảy ra");
    } finally {
      setSavingAddress(false);
    }
  };

  useEffect(() => {
    const loadItems = (key: string) => {
      const raw = sessionStorage.getItem(key);
      if (!raw) return null;
      try {
        const parsed = JSON.parse(raw) as BuyNowItem[];
        return Array.isArray(parsed) && parsed.length > 0 ? parsed : null;
      } catch {
        sessionStorage.removeItem(key);
        return null;
      }
    };

    const selected = loadItems("sneakshop_selected_cart_items");
    if (selected) {
      setBuyNowItems(selected);
      setReady(true);
      return;
    }

    const raw = loadItems("sneakshop_buy_now_items");
    if (raw) {
      setBuyNowItems(raw);
    }
    setReady(true);
  }, []);

  useEffect(() => {
    if (!ready) return;
    if (!user) {
      router.push("/login");
      return;
    }
    if (buyNowItems.length === 0 && items.length === 0) {
      router.push("/cart");
      return;
    }
    setForm((f) => ({
      ...f,
      recipientName: f.recipientName || user.fullName || "",
      recipientPhone: f.recipientPhone || user.phone || "",
    }));
  }, [ready, user, items.length, buyNowItems.length, router]);

  const set = (k: string) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }));

  const checkoutItems = buyNowItems.length > 0
    ? buyNowItems.map((item) => ({
        id: item.productId,
        productName: item.productName,
        variantName: item.variantName,
        colorName: item.colorName,
        price: item.unitPrice,
        quantity: item.quantity,
        productImage: item.productImage,
      }))
    : items;

  const subtotal = checkoutItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const shippingFee = subtotal >= 500000 ? 0 : 30000;
  const grandTotal = subtotal + shippingFee;
  const isBuyNow = buyNowItems.length > 0;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.recipientName.trim() || !form.recipientPhone.trim() || !addressForm.address.trim() || addressForm.provinceCode == null || addressForm.districtCode == null || !addressForm.ward.trim()) {
      toast.error("Vui lòng điền đầy đủ thông tin giao hàng");
      return;
    }
    setLoading(true);
    try {
      const res = await ordersApi.checkout({
        recipientName: form.recipientName,
        recipientPhone: form.recipientPhone,
        shippingAddress: `${addressForm.address}, ${addressForm.ward}, ${addressForm.district}, ${addressForm.city}`,
        shippingCity: addressForm.city,
        addressId: undefined,
        paymentMethod: form.paymentMethod,
        note: form.note,
        items: isBuyNow
          ? buyNowItems.map((item) => ({
              productId: item.productId,
              variantId: item.variantId,
              colorId: item.colorId,
              quantity: item.quantity,
            }))
          : undefined,
      });
      if (isBuyNow) {
        sessionStorage.removeItem("sneakshop_buy_now_items");
        sessionStorage.removeItem("sneakshop_selected_cart_items");
      } else {
        clear();
      }
      const paymentUrl = res.data.result.paymentUrl;
      if (paymentUrl) {
        const methodLabel = form.paymentMethod === "zalopay" ? "ZaloPay" : "MoMo";
        toast.success(`Đang chuyển sang ${methodLabel}...`);
        window.location.href = paymentUrl;
        return;
      }
      toast.success("Đặt hàng thành công!");
      router.push("/orders");
    } catch (err: unknown) {
      toast.error((err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Có lỗi xảy ra");
    } finally {
      setLoading(false);
    }
  };

  if (!ready || !user || (items.length === 0 && buyNowItems.length === 0)) {
    return <div className="max-w-3xl mx-auto px-4 py-8"><Skeleton className="h-96" /></div>;
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-8">Thanh toán</h1>

      <form onSubmit={handleSubmit}>
        <div className="grid md:grid-cols-3 gap-8">
          <div className="md:col-span-2 space-y-6">
            <div className="bg-white border rounded-xl p-5">
              <h2 className="font-bold text-lg mb-4">Thông tin giao hàng</h2>
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label>Người nhận *</Label>
                    <Input
                      value={form.recipientName}
                      onChange={(e) => setForm((f) => ({ ...f, recipientName: e.target.value }))}
                    />
                  </div>
                  <div className="space-y-1">
                    <Label>Số điện thoại *</Label>
                    <Input
                      value={form.recipientPhone}
                      onChange={(e) => setForm((f) => ({ ...f, recipientPhone: e.target.value }))}
                    />
                  </div>
                </div>
                <label className="flex items-center gap-2 text-sm font-medium text-gray-700">
                  <input
                    type="checkbox"
                    checked={addressForm.isDefault}
                    onChange={(e) => setAddressForm((f) => ({ ...f, isDefault: e.target.checked }))}
                    className="h-4 w-4 rounded border-gray-300"
                  />
                  Đặt làm địa chỉ mặc định
                </label>

                <div className="grid grid-cols-2 gap-4">
                  <div className="col-span-2 space-y-1">
                    <Label>Địa chỉ *</Label>
                    <Input
                      value={addressForm.address}
                      onChange={(e) => setAddressForm((f) => ({ ...f, address: e.target.value }))}
                    />
                  </div>
                  <div className="space-y-1">
                    <Label>Tỉnh / Thành phố *</Label>
                    <Select
                      value={provinceCode != null ? String(provinceCode) : ""}
                      onValueChange={(v) => {
                        const p = provinces.find((x) => x.code === Number(v));
                        if (p) void handleProvinceChange(p.code, p.name);
                      }}
                      disabled={regionsLoading}
                    >
                      <SelectTrigger>
                        <span>{provinceName || "-- Chọn tỉnh / thành phố --"}</span>
                      </SelectTrigger>
                      <SelectContent>
                        {provinces.map((p) => (
                          <SelectItem key={p.code} value={String(p.code)}>
                            {p.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    {provinceName && <p className="text-xs text-gray-500">Đã chọn: {provinceName}</p>}
                  </div>
                  <div className="space-y-1">
                    <Label>Quận / Huyện *</Label>
                    <Select
                      value={districtCode != null ? String(districtCode) : ""}
                      onValueChange={(v) => {
                        const d = districts.find((x) => x.code === Number(v));
                        if (d) void handleDistrictChange(d.code, d.name);
                      }}
                      disabled={!provinceCode || districts.length === 0}
                    >
                      <SelectTrigger>
                        <span>{districtName || "-- Chọn quận / huyện --"}</span>
                      </SelectTrigger>
                      <SelectContent>
                        {districts.map((d) => (
                          <SelectItem key={d.code} value={String(d.code)}>
                            {d.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    {districtName && <p className="text-xs text-gray-500">Đã chọn: {districtName}</p>}
                  </div>
                  <div className="col-span-2 space-y-1">
                    <Label>Phường / Xã</Label>
                    <Select
                      value={addressForm.ward}
                      onValueChange={(v) => setAddressForm((f) => ({ ...f, ward: v ?? "" }))}
                      disabled={!districtCode || wards.length === 0}
                    >
                      <SelectTrigger>
                        <span>{addressForm.ward || "-- Chọn phường / xã --"}</span>
                      </SelectTrigger>
                      <SelectContent>
                        {wards.map((w) => (
                          <SelectItem key={w.code} value={w.name}>
                            {w.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>

                <div className="flex justify-end">
                  <Button type="button" onClick={() => void handleSaveAddress()} disabled={savingAddress}>
                    {savingAddress ? "Đang lưu..." : "Lưu địa chỉ"}
                  </Button>
                </div>
              </div>
            </div>

            {/* Payment */}
            <div className="bg-white border rounded-xl p-5">
              <h2 className="font-bold text-lg mb-4">Phương thức thanh toán</h2>
              <div className="space-y-3">
                {[
                  { value: "cod", label: "💵 COD — Thanh toán khi nhận hàng" },
                  { value: "bank_transfer", label: "🏦 Chuyển khoản ngân hàng" },
                  { value: "momo", label: "📱 MoMo — Thanh toán qua ví MoMo" },
                  { value: "zalopay", label: "💙 ZaloPay — Thanh toán qua ZaloPay" },
                ].map((opt) => (
                  <label key={opt.value} className="flex items-center gap-3 p-3 border rounded-lg cursor-pointer hover:bg-gray-50">
                    <input
                      type="radio"
                      name="paymentMethod"
                      value={opt.value}
                      checked={form.paymentMethod === opt.value}
                      onChange={() => setForm((f) => ({ ...f, paymentMethod: opt.value }))}
                    />
                    <span className="text-sm font-medium">{opt.label}</span>
                  </label>
                ))}
              </div>
            </div>

            {/* Note */}
            <div className="bg-white border rounded-xl p-5">
              <h2 className="font-bold text-lg mb-4">Ghi chú</h2>
              <Textarea value={form.note} onChange={set("note")} rows={3} />
            </div>
          </div>

          {/* Order Summary */}
          <div>
            <div className="bg-white border rounded-xl p-5 sticky top-24">
              <h2 className="font-bold text-lg mb-4">Đơn hàng</h2>
              <div className="space-y-3 mb-4">
                {checkoutItems.map((item) => (
                  <div key={item.id} className="flex justify-between text-sm">
                    <span className="text-gray-600 line-clamp-2 flex-1 pr-2">
                      {item.productName}
                      {item.variantName && <span className="text-gray-400"> · {item.variantName}</span>}
                      {" "}×{item.quantity}
                    </span>
                    <span className="font-medium flex-shrink-0">{formatVND(item.price * item.quantity)}</span>
                  </div>
                ))}
              </div>
              <div className="border-t pt-3 space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-500">Tạm tính</span>
                  <span>{formatVND(subtotal)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Phí vận chuyển</span>
                  <span>{shippingFee === 0 ? <span className="text-green-600">Miễn phí</span> : formatVND(shippingFee)}</span>
                </div>
                <div className="flex justify-between font-bold text-base pt-1 border-t">
                  <span>Tổng cộng</span>
                  <span className="text-black">{formatVND(grandTotal)}</span>
                </div>
              </div>
              <Button type="submit" className="w-full mt-4 h-11 font-bold" disabled={loading}>
                {loading ? "Đang đặt hàng..." : "Đặt hàng"}
              </Button>
            </div>
          </div>
        </div>
      </form>
    </div>
  );
}
