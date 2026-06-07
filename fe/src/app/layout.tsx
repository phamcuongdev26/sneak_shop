import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Toaster } from "@/components/ui/sonner";
import { GoogleOAuthProvider } from "@react-oauth/google";

const inter = Inter({ subsets: ["latin"] });
const googleClientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
const googleOAuthEnabled =
  !!googleClientId && googleClientId !== "YOUR_GOOGLE_CLIENT_ID";

export const metadata: Metadata = {
  title: "Sneak Shop",
  description: "Shop giày sneaker chính hãng",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="vi" suppressHydrationWarning>
      <body className={`${inter.className} min-h-full antialiased`}>
        {googleOAuthEnabled ? (
          <GoogleOAuthProvider clientId={googleClientId!}>{children}</GoogleOAuthProvider>
        ) : (
          children
        )}
        <Toaster richColors position="top-right" duration={7000} />
      </body>
    </html>
  );
}
