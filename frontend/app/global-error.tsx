"use client"

import { NextIntlClientProvider, useTranslations } from "next-intl"
import { messagesForLocale, type AppLocale } from "@/components/locale-provider"

// Next 16: global-error replaces the root layout when it fails, so it must
// render its own <html> and <body> (globals.css is not loaded here — styles inline).
// It also replaces every context provider, so it resolves its own locale +
// messages here (NEXT_LOCALE cookie written by LocaleProvider) instead of
// relying on an ancestor NextIntlClientProvider that no longer exists.
function readLocale(): AppLocale {
  try {
    const match = document.cookie.match(/(?:^|;\s*)NEXT_LOCALE=([^;]*)/)
    if (match?.[1] === "sw") return "sw"
  } catch {}
  return "en"
}

function GlobalErrorContent({ error, retry }: { error: Error & { digest?: string }; retry: () => void }) {
  const t = useTranslations("common")
  return (
    <div style={{ maxWidth: "28rem", padding: "2rem", textAlign: "center" }}>
      <h2 style={{ fontSize: "1.25rem", fontWeight: 700, margin: 0 }}>{t("error.generic")}</h2>
      <p style={{ marginTop: "0.5rem", fontSize: "0.875rem", color: "#555" }}>
        {t("error.critical")}
      </p>
      <div style={{ marginTop: "1.5rem", display: "flex", gap: "0.75rem", justifyContent: "center" }}>
        <button
          onClick={() => retry()}
          style={{
            height: "2.5rem",
            padding: "0 1rem",
            borderRadius: "0.5rem",
            border: "none",
            background: "#111",
            color: "#fff",
            fontSize: "0.875rem",
            cursor: "pointer",
          }}
        >
          {t("retry")}
        </button>
        <a
          href="/"
          style={{
            height: "2.5rem",
            padding: "0 1rem",
            borderRadius: "0.5rem",
            border: "1px solid #ddd",
            background: "#fff",
            color: "#111",
            fontSize: "0.875rem",
            display: "inline-flex",
            alignItems: "center",
            textDecoration: "none",
          }}
        >
          {t("error.homepage")}
        </a>
      </div>
      {error.digest && <p style={{ marginTop: "1rem", fontSize: "0.625rem", color: "#999" }}>{t("error.reference", { digest: error.digest })}</p>}
    </div>
  )
}

export default function GlobalError({ error, retry }: { error: Error & { digest?: string }; retry: () => void }) {
  const locale = readLocale()
  return (
    <html lang={locale}>
      <body
        style={{
          margin: 0,
          minHeight: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontFamily: "system-ui, -apple-system, sans-serif",
          background: "#fafafa",
          color: "#111",
        }}
      >
        <NextIntlClientProvider locale={locale} messages={messagesForLocale(locale)}>
          <GlobalErrorContent error={error} retry={retry} />
        </NextIntlClientProvider>
      </body>
    </html>
  )
}
