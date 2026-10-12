"use client"

import { useTranslations } from "next-intl"
import { cn } from "@/lib/utils"
import { whatsappClickToChat, useSiteSettings } from "@/hooks/use-site-settings"
import type { SiteSettings } from "@/lib/public-site-api"

/**
 * The single place ELMKUSOMA renders social and contact links.
 *
 * <p>Deliberately one component. The footer, the contact page and the support page all need the
 * same set of links, and three copies of this logic is how they drift apart and how an icon ends
 * up pointing at the wrong platform.</p>
 *
 * <p>Why the brand marks are inlined here rather than imported: `lucide-react` is the project's
 * icon library and it deliberately ships no brand logos. The usual fix is a second dependency
 * (`react-icons` and friends) whose entire catalogue is thousands of exports for the seven marks
 * needed here. Seven small inline paths cost less than that dependency and cannot drift.</p>
 *
 * <p>Every destination comes from saved configuration. A platform with no configured URL is
 * omitted entirely — not rendered disabled, and never pointed at a bare domain root, because a
 * link to `facebook.com` is not the organisation's page.</p>
 */

type PlatformKey = keyof SiteSettings

/** Inline brand marks. `viewBox` is the brand's own 24×24 grid. */
const BRAND_PATHS: Record<string, { path: string; label: string }> = {
  "public.social.facebook": {
    label: "Facebook",
    path: "M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z",
  },
  "public.social.instagram": {
    label: "Instagram",
    path: "M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zM12 0C8.741 0 8.333.014 7.053.072 2.695.272.273 2.69.073 7.052.014 8.333 0 8.741 0 12c0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98C8.333 23.986 8.741 24 12 24c3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98C15.668.014 15.259 0 12 0zm0 5.838a6.162 6.162 0 100 12.324 6.162 6.162 0 000-12.324zM12 16a4 4 0 110-8 4 4 0 010 8zm6.406-11.845a1.44 1.44 0 100 2.881 1.44 1.44 0 000-2.881z",
  },
  "public.social.youtube": {
    label: "YouTube",
    path: "M23.498 6.186a3.016 3.016 0 00-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 00.502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 002.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 002.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z",
  },
  "public.social.linkedin": {
    label: "LinkedIn",
    path: "M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433a2.062 2.062 0 01-2.063-2.065 2.064 2.064 0 112.063 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z",
  },
  "public.social.tiktok": {
    label: "TikTok",
    path: "M12.525.02c1.31-.02 2.61-.01 3.91-.02.08 1.53.63 3.09 1.75 4.17 1.12 1.11 2.7 1.62 4.24 1.79v4.03c-1.44-.05-2.89-.35-4.2-.97-.57-.26-1.1-.59-1.62-.93-.01 2.92.01 5.84-.02 8.75-.08 1.4-.54 2.79-1.35 3.94-1.31 1.92-3.58 3.17-5.91 3.21-1.43.08-2.86-.31-4.08-1.03-2.02-1.19-3.44-3.37-3.65-5.71-.02-.5-.03-1-.01-1.49.18-1.9 1.12-3.72 2.58-4.96 1.66-1.44 3.98-2.13 6.15-1.72.02 1.48-.04 2.96-.04 4.44-.99-.32-2.15-.23-3.02.37-.63.41-1.11 1.04-1.36 1.75-.21.51-.15 1.07-.14 1.61.24 1.64 1.82 3.02 3.5 2.87 1.12-.01 2.19-.66 2.77-1.61.19-.33.4-.67.41-1.06.1-1.79.06-3.57.07-5.36.01-4.03-.01-8.05.02-12.07z",
  },
  "public.social.x": {
    label: "X",
    path: "M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z",
  },
}

const SOCIAL_KEYS = Object.keys(BRAND_PATHS) as PlatformKey[]

/**
 * A single brand mark, for places that present one platform as a card or tile rather than as a
 * row of links. Exported so the mark has exactly one definition in the codebase: duplicating an
 * SVG path is how a card ends up showing the WhatsApp glyph beside the Facebook label.
 */
export function BrandIcon({
  name,
  className,
}: {
  name: keyof typeof BRAND_PATHS | "whatsapp"
  className?: string
}) {
  const path =
    name === "whatsapp"
      ? "M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884a9.82 9.82 0 016.988 2.896 9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893A11.821 11.821 0 0020.465 3.488"
      : BRAND_PATHS[name].path

  return (
    <svg
      viewBox="0 0 24 24"
      fill="currentColor"
      className={className}
      role="img"
      aria-hidden="true"
      focusable="false"
    >
      <path d={path} />
    </svg>
  )
}


export interface SocialLinksProps {
  /**
   * `icon` shows the brand mark alone; `icon-label` shows the mark with its platform name.
   * Icon-only is for dense rows such as the footer, where the label would wrap.
   */
  variant?: "icon" | "icon-label"
  className?: string
  /** Accessible name for the group. */
  label?: string
  /** Also offer the configured WhatsApp click-to-chat link. */
  includeWhatsApp?: boolean
  /** Message pre-filled into the WhatsApp chat. */
  whatsappMessage?: string
}

/**
 * Renders the configured social/contact platforms, and nothing else.
 *
 * Returns `null` when no platform is configured, so a caller can leave the space empty rather
 * than render an empty bordered box.
 */
export function SocialLinks({
  variant = "icon",
  className,
  label,
  includeWhatsApp = false,
  whatsappMessage,
}: SocialLinksProps) {
  const t = useTranslations("siteSocial")
  const { value } = useSiteSettings()

  const links = SOCIAL_KEYS.map((key) => ({
    key,
    href: value(key),
    brand: BRAND_PATHS[key],
  })).filter((link) => Boolean(link.href))

  const whatsappHref = includeWhatsApp ? whatsappClickToChat(value("public.contact.whatsapp"), whatsappMessage) : undefined
  if (links.length === 0 && !whatsappHref) return null

  const base =
    "inline-flex items-center justify-center gap-2 rounded-lg transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
  const tone = variant === "icon"
    ? "size-10 text-muted-foreground hover:bg-muted hover:text-foreground"
    : "h-10 px-3 text-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground"

  return (
    <ul
      className={cn("flex flex-wrap items-center gap-2", className)}
      aria-label={label ?? t("groupLabel")}
    >
      {links.map(({ key, href, brand }) => (
        <li key={key}>
          <a
            href={href}
            target="_blank"
            rel="noopener noreferrer"
            className={cn(base, tone)}
            // The visible text is the mark, so the name has to come from somewhere.
            aria-label={`${t("followOn", { platform: brand.label })}`}
            title={brand.label}
          >
            <svg
              viewBox="0 0 24 24"
              fill="currentColor"
              className="size-4"
              role="img"
              aria-hidden="true"
              focusable="false"
            >
              <path d={brand.path} />
            </svg>
            {variant === "icon-label" && <span>{brand.label}</span>}
          </a>
        </li>
      ))}

      {whatsappHref && (
        <li>
          <a
            href={whatsappHref}
            target="_blank"
            rel="noopener noreferrer"
            className={cn(base, tone)}
            aria-label={t("whatsapp")}
            title="WhatsApp"
          >
            <svg
              viewBox="0 0 24 24"
              fill="currentColor"
              className="size-4"
              role="img"
              aria-hidden="true"
              focusable="false"
            >
              <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884a9.82 9.82 0 016.988 2.896 9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893A11.821 11.821 0 0020.465 3.488" />
            </svg>
            {variant === "icon-label" && <span>WhatsApp</span>}
          </a>
        </li>
      )}
    </ul>
  )
}
