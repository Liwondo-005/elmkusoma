"use client"

import { useState } from "react"
import { Bell, Menu, X, ChevronDown, Search, User } from "lucide-react"
import { Button } from "@/components/ui/button"
import { DashboardSidebar } from "@/components/dashboard/dashboard-sidebar"

export function DashboardTopbar() {
  const [open, setOpen] = useState(false)

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
          <Search className="pointer-events-none absolute left-3 size-4 text-muted-foreground" />
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
          <div className="flex items-center gap-2">
            <span className="flex size-9 items-center justify-center rounded-full bg-accent text-primary">
              <User className="size-4" />
            </span>
            <div className="hidden text-left sm:block">
              <p className="text-sm font-semibold leading-tight text-foreground">Asha M.</p>
              <p className="text-xs text-muted-foreground">Student</p>
            </div>
            <ChevronDown className="hidden size-4 text-muted-foreground sm:block" />
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
