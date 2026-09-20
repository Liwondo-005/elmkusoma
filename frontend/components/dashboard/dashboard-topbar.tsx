"use client"

import { useState, useEffect, useRef } from "react"
import { useRouter } from "next/navigation"
import { Menu, X, Bell, Search, Globe } from "lucide-react"
import { Button } from "@/components/ui/button"
import { DashboardSidebar } from "@/components/dashboard/dashboard-sidebar"
import { useAuth } from "@/lib/auth"

function GlobalSearchDropdown({ onClose }: { onClose: () => void }) {
  const [query, setQuery] = useState("")
  const [results, setResults] = useState<Array<{ type: string; id: string; title: string; subtitle: string }>>([])
  const [loading, setLoading] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)
  const router = useRouter()

  useEffect(() => { inputRef.current?.focus() }, [])

  useEffect(() => {
    if (!query || query.length < 2) { setResults([]); return }
    const timer = setTimeout(async () => {
      setLoading(true)
      try {
        const token = document.cookie.match(/elmkusoma_access_token=([^;]+)/)?.[1]
        const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/v1/platform-admin/search?q=${encodeURIComponent(query)}&limit=10`, {
          headers: { Authorization: `Bearer ${decodeURIComponent(token || "")}` },
        })
        if (res.ok) { const json = await res.json(); setResults(json.data || []) }
      } catch {} finally { setLoading(false) }
    }, 300)
    return () => clearTimeout(timer)
  }, [query])

  function navigateTo(type: string, id: string) {
    if (type === "USER") router.push(`/dashboard/platform-admin/users/${id}`)
    else if (type === "INSTITUTION") router.push(`/dashboard/platform-admin/institutions/${id}`)
    onClose()
  }

  const typeColors: Record<string, string> = { USER: "bg-blue-100 text-blue-700", INSTITUTION: "bg-emerald-100 text-emerald-700" }

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center pt-20">
      <div className="absolute inset-0 bg-foreground/40" onClick={onClose} />
      <div className="relative w-full max-w-lg rounded-2xl border border-border bg-card shadow-xl">
        <div className="flex items-center gap-3 border-b border-border px-4">
          <Search className="size-4 text-muted-foreground" />
          <input ref={inputRef} type="text" placeholder="Search users, institutions..."
            value={query} onChange={(e) => setQuery(e.target.value)}
            className="flex-1 bg-transparent py-3.5 text-sm outline-none placeholder:text-muted-foreground" />
          <kbd className="hidden rounded-md border border-border px-1.5 py-0.5 text-[10px] text-muted-foreground sm:inline">ESC</kbd>
        </div>
        <div className="max-h-80 overflow-y-auto p-2">
          {loading && <p className="px-3 py-6 text-center text-sm text-muted-foreground">Searching...</p>}
          {!loading && query.length >= 2 && results.length === 0 && (
            <p className="px-3 py-6 text-center text-sm text-muted-foreground">No results found</p>
          )}
          {results.map((r) => (
            <button key={`${r.type}-${r.id}`} onClick={() => navigateTo(r.type, r.id)}
              className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left hover:bg-muted transition-colors">
              <span className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-bold ${typeColors[r.type] || "bg-gray-100 text-gray-700"}`}>{r.type}</span>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-foreground truncate">{r.title}</p>
                <p className="text-xs text-muted-foreground truncate">{r.subtitle}</p>
              </div>
            </button>
          ))}
          {query.length < 2 && (
            <div className="px-3 py-6 text-center">
              <p className="text-sm text-muted-foreground">Type to search across the platform</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export function DashboardTopbar() {
  const [open, setOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const { user, logout } = useAuth()
  const router = useRouter()

  useEffect(() => {
    function handleKey(e: KeyboardEvent) {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") { e.preventDefault(); setSearchOpen(true) }
      if (e.key === "Escape") setSearchOpen(false)
    }
    document.addEventListener("keydown", handleKey)
    return () => document.removeEventListener("keydown", handleKey)
  }, [])

  function handleLogout() {
    logout()
    router.push("/login")
  }

  function toggleLocale() {
    const current = document.cookie.split('; ').find(c => c.startsWith('NEXT_LOCALE='))?.split('=')[1] || 'en'
    const next = current === 'en' ? 'sw' : 'en'
    document.cookie = `NEXT_LOCALE=${next}; path=/; max-age=31536000`
    window.location.reload()
  }

  const locale = typeof document !== 'undefined'
    ? (document.cookie.split('; ').find(c => c.startsWith('NEXT_LOCALE='))?.split('=')[1] || 'en')
    : 'en'

  const initials = user?.name
    ? user.name.split(" ").map((n) => n[0]).join("").toUpperCase().slice(0, 2)
    : "U"

  const isAdmin = user?.role === "Admin"

  return (
    <>
      <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-border bg-background/90 px-4 backdrop-blur sm:px-6" role="banner" aria-label="Top navigation">
        <Button
          variant="ghost"
          size="icon"
          className="h-10 w-10 lg:hidden"
          aria-label="Open menu"
          onClick={() => setOpen(true)}
        >
          <Menu className="size-5" />
        </Button>

        {isAdmin ? (
          <button onClick={() => setSearchOpen(true)}
            className="hidden items-center gap-2 rounded-lg border border-border bg-muted/60 px-3 py-2 text-sm text-muted-foreground transition-colors hover:bg-muted sm:flex">
            <Search className="size-4" />
            <span>Search platform...</span>
            <kbd className="ml-4 rounded-md border border-border px-1.5 py-0.5 text-[10px]">Ctrl+K</kbd>
          </button>
        ) : (
          <label className="relative hidden items-center sm:flex">
            <Search className="absolute left-3 size-4 text-muted-foreground" />
            <input
              type="search"
              placeholder="Search courses, lessons..."
              className="h-10 w-64 rounded-lg border border-border bg-muted/60 pl-9 pr-3 text-sm outline-none placeholder:text-muted-foreground focus:border-ring focus:bg-background"
            />
          </label>
        )}

        <div className="ml-auto flex items-center gap-3">
          <button
            type="button"
            onClick={toggleLocale}
            className="flex items-center gap-1.5 rounded-lg border border-border px-2.5 py-1.5 text-xs font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
            aria-label="Switch language"
          >
            <Globe className="size-3.5" />
            {locale === 'en' ? 'SW' : 'EN'}
          </button>
          <button
            type="button"
            className="relative flex size-10 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
            aria-label="Notifications"
          >
            <Bell className="size-5" />
            <span className="absolute right-2 top-2 size-2 rounded-full bg-orange" />
          </button>
          <div className="relative">
            <button
              type="button"
              className="flex items-center gap-2 rounded-lg p-1 hover:bg-muted"
              onClick={() => setProfileOpen((v) => !v)}
            >
              <span className="flex size-9 items-center justify-center rounded-full bg-accent text-primary text-sm font-semibold">
                {initials}
              </span>
              <div className="hidden text-left sm:block">
                <p className="text-sm font-semibold leading-tight text-foreground">
                  {user?.name || "Student"}
                </p>
                <p className="text-xs text-muted-foreground capitalize">{user?.role || "Student"}</p>
              </div>
            </button>

            {profileOpen && (
              <div className="absolute right-0 top-full mt-2 w-48 rounded-xl border border-border bg-card py-1 shadow-lg z-50">
                <button
                  type="button"
                  onClick={handleLogout}
                  className="flex w-full items-center gap-2 px-4 py-2.5 text-sm text-muted-foreground hover:bg-muted hover:text-foreground"
                >
                  Logout
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      {searchOpen && <GlobalSearchDropdown onClose={() => setSearchOpen(false)} />}

      {open && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div className="absolute inset-0 bg-foreground/40" onClick={() => setOpen(false)} />
          <div className="absolute inset-y-0 left-0 w-72 border-r border-border shadow-xl">
            <Button
              variant="ghost"
              size="icon"
              className="absolute right-2 top-3 z-10 h-9 w-9"
              aria-label="Close menu"
              onClick={() => setOpen(false)}
            >
              <X className="size-5" />
            </Button>
            <DashboardSidebar onNavigate={() => setOpen(false)} />
          </div>
        </div>
      )}
    </>
  )
}
