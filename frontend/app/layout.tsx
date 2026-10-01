import { Analytics } from '@vercel/analytics/next'
import type { Metadata, Viewport } from 'next'
import './globals.css'
import { Providers } from './providers'

// NOTE: Google Fonts (next/font/google) removed — the build/dev environment has
// no reliable access to fonts.googleapis.com, which made Turbopack emit CSS
// referencing @vercel/turbopack-next/internal/font/google/font and crash with
// "Module not found". The font objects were also never applied (body uses
// font-sans), so system font stacks in globals.css take over with zero visual
// regression for the current UI. Reintroduce via next/font/local (self-hosted
// woff2) if branded webfonts are required.

// Canonical site URL (shared with app/sitemap.ts and app/robots.ts).
const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL || 'https://elmkusoma.co.tz'

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: 'ELMKUSOMA — Learn. Connect. Succeed, Live.',
  description:
    'ELMKUSOMA is a modern African EdTech platform for live classes, courses, recorded lessons and a digital library — learn from expert teachers and connect with students across Africa.',
  generator: 'v0.app',
  applicationName: 'ELMKUSOMA',
  openGraph: {
    type: 'website',
    url: SITE_URL,
    siteName: 'ELMKUSOMA',
    title: 'ELMKUSOMA — Learn. Connect. Succeed, Live.',
    description:
      'A modern African EdTech platform for live classes, courses, recorded lessons and a digital library.',
    locale: 'en_US',
  },
  twitter: {
    card: 'summary',
    title: 'ELMKUSOMA — Learn. Connect. Succeed, Live.',
    description:
      'A modern African EdTech platform for live classes, courses, recorded lessons and a digital library.',
  },
  icons: {
    icon: [
      {
        url: '/icon-light-32x32.png',
        media: '(prefers-color-scheme: light)',
      },
      {
        url: '/icon-dark-32x32.png',
        media: '(prefers-color-scheme: dark)',
      },
      {
        url: '/icon.svg',
        type: 'image/svg+xml',
      },
    ],
    apple: '/apple-icon.png',
  },
}

export const viewport: Viewport = {
  colorScheme: 'light dark',
  themeColor: [
    { media: '(prefers-color-scheme: light)', color: 'white' },
    { media: '(prefers-color-scheme: dark)', color: 'black' },
  ],
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en" className="light bg-background">
      <body className="antialiased font-sans">
        <Providers>{children}</Providers>
        {process.env.NODE_ENV === 'production' && <Analytics />}
      </body>
    </html>
  )
}
