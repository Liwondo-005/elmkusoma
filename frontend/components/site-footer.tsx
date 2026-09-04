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
        <div className="grid gap-10 md:grid-cols-[1.5fr_1fr_1fr_1fr]">
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
        </div>
        <div className="mt-12 flex flex-col items-center justify-between gap-4 border-t border-border pt-6 sm:flex-row">
          <p className="text-sm text-muted-foreground">© {new Date().getFullYear()} ELMKUSOMA. All rights reserved.</p>
          <div className="flex gap-6">
            <Link href="/about" className="text-sm text-muted-foreground hover:text-primary">
              About
            </Link>
            <Link href="/about" className="text-sm text-muted-foreground hover:text-primary">
              Contact
            </Link>
          </div>
        </div>
      </div>
    </footer>
  )
}
