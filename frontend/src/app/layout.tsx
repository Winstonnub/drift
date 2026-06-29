import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Drift",
  description: "Daily micro-learning that adapts to your feedback.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
} // whatever page is being shown goes here