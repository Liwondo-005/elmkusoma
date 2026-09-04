"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { Menu, X, Bell, Search } from "lucide-react"
import { Button } from "@/components/ui/button"
import { DashboardSidebar } from "@/components/dashboard/dashboard-sidebar"
import { useAuth } from "@/lib/auth"

export function DashboardTopbar() {
  const [open, setOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const { user, logout } = useAuth()
  const router = useRouter()

  function handleLogout() {
    logout()
    router.push("/login")
  }

  const initials = user?.name
    ? user.name.split(" ").map((n) => n[0]).join("").toUpperCase().slice(0, 2)
    : "U"

  return (
    <>
      <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-border bg-background/90 px-4 backdrop-blur sm:px-6">
        <Button
          variant="ghost"
          size="icon"
          className="h-10 w-10 lg:hidden"
          aria-label="Open menu"
          onClick={() => setOpen(true)}
        >
          <Menu className="size-5" />
        </Button>

          <label className="relative hidden items-center sm:flex">
          <Search className="absolute left-3 size-4 text-muted-foreground" />
          <input
            type="search"
            placeholder="Search courses, lessons..."
            className="h-10 w-64 rounded-lg border border-border bg-muted/60 pl-9 pr-3 text-sm outline-none placeholder:text-muted-foreground focus:border-ring focus:bg-background"
          />
        </label>

        <div className="ml-auto flex items-center gap-3">
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
