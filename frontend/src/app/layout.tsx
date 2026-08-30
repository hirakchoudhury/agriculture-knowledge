import type { Metadata } from "next";
import { Montserrat, Geist_Mono } from "next/font/google";
import { AuthProvider } from "@/lib/auth-context";
import { ThemeProvider, themeInitScript } from "@/lib/theme";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import { fetchPublic } from "@/lib/public-api";
import type { ExamSummary } from "@/lib/types";
import "./globals.css";

// Montserrat, matching the reference site. Loaded through next/font so the
// files are self-hosted and there is no render-blocking request to Google.
const montserrat = Montserrat({
  variable: "--font-sans-face",
  subsets: ["latin"],
  display: "swap",
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

export default async function RootLayout({ children }: LayoutProps<"/">) {
  // Fetched here rather than inside the header so the Exams menu is populated in
  // the first paint, and so it costs one request per page instead of one per
  // page plus one from every client-side header mount.
  const exams = (await fetchPublic<ExamSummary[]>("/api/v1/exams")) ?? [];

  return (
    <html
      lang="en"
      className={`${montserrat.variable} ${geistMono.variable} h-full antialiased`}
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
            <SiteHeader exams={exams} />
            {children}
            <SiteFooter />
          </AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
