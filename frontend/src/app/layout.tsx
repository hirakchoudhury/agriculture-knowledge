import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import { AuthProvider } from "@/lib/auth-context";
import { ThemeProvider, themeInitScript } from "@/lib/theme";
import { SiteHeader } from "@/components/site-header";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Agriculture Knowledge",
  description:
    "Articles, video lessons and practice questions for agriculture competitive exams.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
      suppressHydrationWarning
    >
      <head>
        {/*
          Applies the stored theme before first paint. Without it the page
          renders light and then corrects itself on hydration, which is the
          flash of wrong theme. suppressHydrationWarning above is required
          because this script legitimately changes <html> before React sees it.
        */}
        <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body className="min-h-full flex flex-col">
        <ThemeProvider>
          <AuthProvider>
            <SiteHeader />
            {children}
          </AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
