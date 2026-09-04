import Link from "next/link"
import { Logo } from "@/components/logo"

const columns = [
  {
    title: "Learn",
    links: [
      { label: "Education", href: "/courses" },
      { label: "Live Classes", href: "/live-classes" },
      { label: "Notes & Materials", href: "/notes-library" },
    ],
  },
  {
    title: "Schools",
    links: [
      { label: "Nursery Schools", href: "/schools/nursery" },
      { label: "Primary Schools", href: "/schools/primary" },
      { label: "Secondary Schools", href: "/schools/secondary" },
      { label: "Colleges", href: "/schools/colleges" },
      { label: "Universities", href: "/schools/universities" },
      { label: "Vocational (VETA)", href: "/schools/vocational" },
    ],
  },
  {
    title: "Platform",
    links: [
      { label: "About ELMKUSOMA", href: "/about" },
      { label: "Verify Certificate", href: "/certificates/verify" },
      { label: "Register", href: "/register" },
    ],
  },
]

export function SiteFooter() {
  return (
    <footer className="border-t border-border bg-muted/50">
      <div className="mx-auto max-w-7xl px-4 py-14 sm:px-6 lg:px-8">
        <div className="grid gap-10 md:grid-cols-[1.5fr_1fr_1fr_1fr_1.3fr]">
          <div className="max-w-xs">
            <Logo />
            <p className="mt-4 text-sm leading-relaxed text-muted-foreground">
              A unified African EdTech platform for live classes, courses and digital learning — helping students learn,
              connect and succeed.
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
            <h4 className="text-sm font-semibold text-foreground">Contact Us</h4>
            <ul className="mt-4 space-y-3">
              <li className="text-sm text-muted-foreground">
                Dar es Salaam, Tanzania
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
                  Send us a message
                </Link>
              </li>
            </ul>
            <div className="mt-5 flex gap-4">
              <a href="https://facebook.com" target="_blank" rel="noopener noreferrer" className="text-sm text-muted-foreground transition-colors hover:text-primary" aria-label="Facebook">
                Facebook
              </a>
              <a href="https://twitter.com" target="_blank" rel="noopener noreferrer" className="text-sm text-muted-foreground transition-colors hover:text-primary" aria-label="Twitter">
                Twitter
              </a>
              <a href="https://instagram.com" target="_blank" rel="noopener noreferrer" className="text-sm text-muted-foreground transition-colors hover:text-primary" aria-label="Instagram">
                Instagram
              </a>
              <a href="https://linkedin.com" target="_blank" rel="noopener noreferrer" className="text-sm text-muted-foreground transition-colors hover:text-primary" aria-label="LinkedIn">
                LinkedIn
              </a>
            </div>
          </div>
        </div>
        <div className="mt-12 flex flex-col items-center justify-between gap-4 border-t border-border pt-6 sm:flex-row">
          <p className="text-sm text-muted-foreground">&copy; {new Date().getFullYear()} ELMKUSOMA. All rights reserved.</p>
          <div className="flex gap-6">
            <Link href="/about" className="text-sm text-muted-foreground hover:text-primary">
              About
            </Link>
            <Link href="/about" className="text-sm text-muted-foreground hover:text-primary">
            <Link href="/about" className="text-sm text-muted-foreground hover:text-primary">
              Terms
            </Link>
            <Link href="/contact" className="text-sm text-muted-foreground hover:text-primary">
              Support
            </Link>
            </Link>
          </div>
        </div>
      </div>
    </footer>
  )
}
