"use client"

import Link from "next/link"
import { Logo } from "@/components/logo"
import { useTranslations } from "next-intl"

export function SiteFooter() {
  const tNav = useTranslations("nav")
  const tCommon = useTranslations("common")

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
              <li className="text-sm text-muted-foreground">
                Dar es Salaam, Tanzania
              </li>
              <li>
                <a
                  href="tel:+255700000000"
                  className="text-sm text-muted-foreground transition-colors hover:text-primary"
                >
                  +255 700 000 000
                </a>
              </li>
              <li>
                <a
                  href="mailto:info@elmkusoma.co.tz"
                  className="text-sm text-muted-foreground transition-colors hover:text-primary"
                >
                  info@elmkusoma.co.tz
                </a>
              </li>
              <li>
                <Link
                  href="/contact"
                  className="text-sm text-muted-foreground transition-colors hover:text-primary"
                >
                  {tCommon("sendMessage")}
                </Link>
              </li>
            </ul>
            <div className="mt-5 flex flex-wrap gap-4">
              <a href="https://facebook.com" target="_blank" rel="noopener noreferrer" className="text-sm text-muted-foreground transition-colors hover:text-primary" aria-label="Facebook">
                Facebook
              </a>
              <a href="https://instagram.com" target="_blank" rel="noopener noreferrer" className="text-sm text-muted-foreground transition-colors hover:text-primary" aria-label="Instagram">
                Instagram
              </a>
              <a href="https://wa.me/255700000000" target="_blank" rel="noopener noreferrer" className="text-sm text-muted-foreground transition-colors hover:text-primary" aria-label="WhatsApp">
                WhatsApp
              </a>
              <a href="https://twitter.com" target="_blank" rel="noopener noreferrer" className="text-sm text-muted-foreground transition-colors hover:text-primary" aria-label="Twitter">
                Twitter
              </a>
              <a href="https://linkedin.com" target="_blank" rel="noopener noreferrer" className="text-sm text-muted-foreground transition-colors hover:text-primary" aria-label="LinkedIn">
                LinkedIn
              </a>
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
