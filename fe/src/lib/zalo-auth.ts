const ZALO_APP_ID = process.env.NEXT_PUBLIC_ZALO_APP_ID ?? "";
const ZALO_REDIRECT_URI = process.env.NEXT_PUBLIC_ZALO_REDIRECT_URI ?? "http://localhost:3000/zalo/callback";

async function sha256(plain: string): Promise<ArrayBuffer> {
  const encoder = new TextEncoder();
  return crypto.subtle.digest("SHA-256", encoder.encode(plain));
}

function base64url(buffer: ArrayBuffer): string {
  return btoa(String.fromCharCode(...new Uint8Array(buffer)))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=/g, "");
}

function generateVerifier(): string {
  const arr = new Uint8Array(32);
  crypto.getRandomValues(arr);
  return base64url(arr.buffer);
}

export async function redirectToZaloLogin() {
  if (!ZALO_APP_ID) {
    alert("Chưa cấu hình NEXT_PUBLIC_ZALO_APP_ID");
    return;
  }
  const verifier = generateVerifier();
  const challenge = base64url(await sha256(verifier));
  sessionStorage.setItem("zalo_code_verifier", verifier);

  const params = new URLSearchParams({
    app_id: ZALO_APP_ID,
    redirect_uri: ZALO_REDIRECT_URI,
    code_challenge: challenge,
    state: crypto.randomUUID(),
  });
  window.location.href = `https://oauth.zaloapp.com/v4/permission?${params.toString()}`;
}
