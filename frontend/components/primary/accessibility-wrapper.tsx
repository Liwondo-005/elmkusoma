"use client"

import { useEffect, useRef } from "react"
import { useTranslations } from "next-intl"

interface AccessibilityWrapperProps {
  children: React.ReactNode
  ariaLabel?: string
  ariaRole?: string
  skipToId?: string
}

export function AccessibilityWrapper({
  children,
  ariaLabel,
  ariaRole,
  skipToId,
}: AccessibilityWrapperProps) {
  const t = useTranslations("ui")
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (containerRef.current) {
      const firstInteractive = containerRef.current.querySelector<HTMLElement>(
        'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
      )
      if (firstInteractive) {
        firstInteractive.focus()
      }
    }
  }, [])

  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") {
        const modal = containerRef.current?.querySelector<HTMLElement>(
          '[role="dialog"]'
        )
        if (modal) {
          const closeBtn = modal.querySelector<HTMLElement>(
            'button[aria-label="Close"], button[aria-label="close"]'
          )
          closeBtn?.click()
        }
      }
    }

    document.addEventListener("keydown", handleKeyDown)
    return () => document.removeEventListener("keydown", handleKeyDown)
  }, [])

  return (
    <>
      {skipToId && (
        <a
          href={`#${skipToId}`}
          className="sr-only focus:not-sr-only focus:absolute focus:z-50 focus:left-4 focus:top-4 focus:rounded-lg focus:bg-primary focus:px-4 focus:py-2 focus:text-primary-foreground focus:shadow-lg"
        >
          {t("a11y.skipToContent")}
        </a>
      )}
      <div
        ref={containerRef}
        role={ariaRole}
        aria-label={ariaLabel}
      >
        {children}
      </div>
    </>
  )
}
