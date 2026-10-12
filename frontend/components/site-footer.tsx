"use client"

import Link from "next/link"
import { Logo } from "@/components/logo"
import { useTranslations } from "next-intl"
import { SocialLinks } from "@/components/social-links"
import { telHref, useSiteSettings } from "@/hooks/use-site-settings"

export function SiteFooter() {
  const tNav = useTranslations("nav")
  const tCommon = useTranslations("common")
  const { value } = useSiteSettings()

  const contactEmail = value("public.contact.email")
  const contactPhone = value("public.contact.phone")
  const contactAddress = value("public.contact.address")
  const phoneHref = telHref(contactPhone)

  const columns = [
    {
      title: tNav("learn"),
      links: [
        { label: tNav("courses"), href: "/courses" },
        { label: tNav("liveClasses"), href: "/live-classes" },
        { label: tCommon("recordedClasses"), href: "/live-classes" },
        { label: tCommon("digitalLibrary"), href: "/notes-library" },
      ],
    },
    {
      title: tNav("discover"),
      links: [
        { label: tCommon("primarySchools"), href: "/schools/primary" },
        { label: tCommon("secondarySchools"), href: "/schools/secondary" },
        { label: tCommon("colleges"), href: "/schools/colleges" },
        { label: tCommon("universities"), href: "/schools/universities" },
      ],
    },
    {
      title: tCommon("platform"),
      links: [
        { label: tNav("about"), href: "/about" },
        { label: tNav("news"), href: "/news" },
        { label: tCommon("verifyCertificate"), href: "/certificates/verify" },
        { label: tCommon("becomeInstructor"), href: "/register" },
        { label: tCommon("forInstitutions"), href: "/register" },
      ],
    },
  ]

  return (
    <footer className="border-t border-border bg-muted/50">
      <div className="mx-auto max-w-7xl px-4 py-14 sm:px-6 lg:px-8">
        <div className="grid gap-10 md:grid-cols-[1.5fr_1fr_1fr_1fr_1.3fr]">
          <div className="max-w-xs">
            <Logo />
            <p className="mt-4 text-sm leading-relaxed text-muted-foreground">
              {tCommon("tagline")}
            </p>
          </div>
          {columns.map((col) => (
            <div key={col.title}>
              <h4 className="text-sm font-semibold text-foreground">{col.title}</h4>
              <ul className="mt-4 space-y-3">
                {col.links.map((link) => (
                  <li key={link.label}>
                    <Link
                      href={link.href}
                      className="text-sm text-muted-foreground transition-colors hover:text-primary"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
          <div>
            <h4 className="text-sm font-semibold text-foreground">{tCommon("contactUs")}</h4>
            <ul className="mt-4 space-y-3">
              {contactAddress && (
                <li className="text-sm text-muted-foreground">{contactAddress}</li>
              )}
              {contactPhone && phoneHref && (
                <li>
                  <a
                    href={phoneHref}
                    className="text-sm text-muted-foreground transition-colors hover:text-primary"
                  >
                    {contactPhone}
                  </a>
                </li>
              )}
              {contactEmail && (
                <li>
                  <a
                    href={`mailto:${contactEmail}`}
                    className="text-sm text-muted-foreground transition-colors hover:text-primary"
                  >
                    {contactEmail}
                  </a>
                </li>
              )}
              <li>
                <Link
                  href="/contact"
                  className="text-sm text-muted-foreground transition-colors hover:text-primary"
                >
                  {tCommon("sendMessage")}
                </Link>
              </li>
            </ul>
            {/* Social and WhatsApp come from one shared component so the footer, contact page
                and support page can never drift apart or disagree on which platforms exist.
                Each link renders only when an administrator has configured a real, validated
                URL — nothing here invents an account or links to a bare domain root. */}
            <div className="mt-5">
              <SocialLinks variant="icon" includeWhatsApp />
            </div>
          </div>
        </div>
        <div className="mt-12 flex flex-col items-center justify-between gap-4 border-t border-border pt-6 sm:flex-row">
          <p className="text-sm text-muted-foreground">{tCommon("copyright", { year: new Date().getFullYear() })}</p>
          <div className="flex gap-6">
            <Link href="/privacy" className="text-sm text-muted-foreground hover:text-primary">
              {tCommon("privacy")}
            </Link>
            <Link href="/terms" className="text-sm text-muted-foreground hover:text-primary">
              {tCommon("terms")}
            </Link>
            <Link href="/support" className="text-sm text-muted-foreground hover:text-primary">
              {tCommon("support")}
            </Link>
          </div>
        </div>
      </div>
    </footer>
  )
}
