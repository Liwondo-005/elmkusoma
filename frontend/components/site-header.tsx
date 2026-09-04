"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { useState } from "react"
import { Logo } from "@/components/logo"
import { Button, buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

const nav = [
  { label: "Home", href: "/" },
  { label: "Courses", href: "/courses" },
  { label: "Live Classes", href: "/live-classes" },
  { label: "About", href: "/about" },
]

export function SiteHeader() {
  const pathname = usePathname()
  const [open, setOpen] = useState(false)

  return (
    <header className="sticky top-0 z-50 border-b border-border bg-background/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-7xl items-center gap-4 px-4 sm:px-6 lg:px-8">
        <Logo />

        <nav className="ml-4 hidden items-center gap-1 md:flex">
          {nav.map((item) => {
            const active = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href)
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "rounded-md px-3 py-2 text-sm font-medium transition-colors",
                  active ? "text-primary" : "text-muted-foreground hover:text-foreground",
                )}
              >
                {item.label}
              </Link>
            )
          })}
        </nav>

        <div className="ml-auto hidden items-center gap-3 lg:flex">
          <label className="relative flex items-center">
            <input
              type="search"
              placeholder="Search for courses, topics..."
              className="h-10 w-64 rounded-lg border border-border bg-muted/60 px-3 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
            />
          </label>
        </div>

        <div className="ml-auto flex items-center gap-2 lg:ml-3">
          <Link href="/login" className={cn(buttonVariants({ variant: "outline" }), "hidden h-10 px-4 sm:inline-flex")}>
            Login
          </Link>
          <Link href="/register" className={cn(buttonVariants(), "hidden h-10 px-4 sm:inline-flex")}>
            Register
          </Link>
          <Button
            variant="ghost"
            size="icon"
            className="h-10 w-10 md:hidden"
            aria-label="Toggle menu"
            onClick={() => setOpen((v) => !v)}
          >
            {open ? "Close" : "Menu"}
          </Button>
        </div>
      </div>

      {open && (
        <div className="border-t border-border bg-background md:hidden">
          <nav className="mx-auto flex max-w-7xl flex-col gap-1 px-4 py-3">
            {nav.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setOpen(false)}
                className="rounded-md px-3 py-2.5 text-sm font-medium text-foreground hover:bg-muted"
              >
                {item.label}
              </Link>
            ))}
            <div className="mt-2 flex gap-2">
              <Link href="/login" className={cn(buttonVariants({ variant: "outline" }), "h-10 flex-1")}>
                Login
              </Link>
              <Link href="/register" className={cn(buttonVariants(), "h-10 flex-1")}>
                Register
              </Link>
            </div>
          </nav>
        </div>
      )}
    </header>
  )
}
