"use client"

import Link from "next/link"
import { useTranslations } from "next-intl"
import { cn } from "@/lib/utils"
import { useAuth } from "@/lib/auth"
import { resolveWorkspace } from "@/lib/workspace"

/**
 * The ELMKUSOMA mark is the "where do I belong" control:
 * guest → public homepage, every authenticated role → its own workspace.
 * It only navigates - it never touches the session, so it can not log anyone out
 * and can not bounce an authenticated user to the public site. Leaving for the
 * public homepage is an explicit logout (see the sidebar Logout button).
 *
 * `href` may still be passed to pin a specific destination (e.g. a marketing
 * header that must always point at "/").
 */
export function Logo({
  className,
  href,
  showText = true,
}: {
  className?: string
  href?: string
  showText?: boolean
}) {
  const t = useTranslations("ui")
  const { user, loading: authLoading } = useAuth()

  let destination = href
  if (!destination) {
    if (authLoading) {
      // Session still resolving: keep the neutral target so the click is harmless.
      destination = "/"
    } else if (!user) {
      destination = "/"
    } else {
      // resolveWorkspace returns null for roles whose home is their own
      // /dashboard (e.g. primary students), so fall back to it.
      destination = resolveWorkspace(user) ?? "/dashboard"
    }
  }

  return (
    <Link href={destination} className={cn("flex items-center gap-2", className)} aria-label={t("logo.home")}>
      <span className="relative inline-flex h-9 w-9 items-center justify-center" aria-hidden="true">
        <svg viewBox="0 0 40 40" className="h-9 w-9" fill="none" xmlns="http://www.w3.org/2000/svg">
          {/* sprout leaves */}
          <path
            d="M20 17c-1.2-4.2-5-6.5-9.2-6.2 0 4.3 2.9 7.9 7 8.6"
            fill="#0d9488"
          />
          <path
            d="M20 17c1.2-4.6 5-7.2 9.4-6.9 0 4.6-3 8.4-7.2 9.1"
            fill="#f59e0b"
          />
          {/* open book */}
          <path
            d="M6 20c4.2-2 9.2-2 14 1 4.8-3 9.8-3 14-1v11c-4.2-2-9.2-2-14 1-4.8-3-9.8-3-14-1V20Z"
            fill="#2563eb"
          />
          <path d="M20 22v10" stroke="#ffffff" strokeWidth="1.4" strokeLinecap="round" opacity="0.7" />
        </svg>
      </span>
      {showText && (
        <span className="text-lg font-extrabold tracking-tight text-foreground">
          ELM<span className="text-primary">KUSOMA</span>
        </span>
      )}
    </Link>
  )
}
