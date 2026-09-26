"use client"

import { useTranslations } from "next-intl"

export function SkipToContent() {
  const t = useTranslations("ui")
  return (
    <a
      href="#main-content"
      className="sr-only focus:not-sr-only focus:absolute focus:z-50 focus:left-4 focus:top-4 focus:rounded-lg focus:bg-primary focus:px-4 focus:py-2 focus:text-primary-foreground focus:shadow-lg"
    >
      {t("a11y.skipToContent")}
    </a>
  )
}
