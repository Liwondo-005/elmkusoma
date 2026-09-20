"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import {
  LayoutDashboard, Users, School, Video, CreditCard, Award,
  Shield, Activity, Settings, Search, Bell, ChevronDown, ChevronRight,
  AlertTriangle, BarChart3, Database, Globe, Zap
} from "lucide-react"
import { Logo } from "@/components/logo"
import { cn } from "@/lib/utils"
import { useState, useEffect } from "react"
import { platformAdminApi, type PlatformHealth } from "@/lib/platform-admin-api"

interface NavItem {
  label: string
  href: string
  icon: typeof LayoutDashboard
}

interface NavSection {
  title: string
  items: NavItem[]
}

const NAV_SECTIONS: NavSection[] = [
  {
    title: "COMMAND CENTER",
    items: [
      { label: "Dashboard", href: "/dashboard/platform-admin", icon: LayoutDashboard },
      { label: "Attention", href: "/dashboard/platform-admin/attention", icon: AlertTriangle },
    ],
  },
  {
    title: "IDENTITY & ACCESS",
    items: [
      { label: "Users", href: "/dashboard/platform-admin/users", icon: Users },
      { label: "Admins", href: "/dashboard/platform-admin/admins", icon: Shield },
    ],
  },
  {
    title: "ECOSYSTEM",
    items: [
      { label: "Institutions", href: "/dashboard/platform-admin/institutions", icon: School },
      { label: "Providers", href: "/dashboard/platform-admin/providers", icon: Globe },
    ],
  },
  {
    title: "LEARNING",
    items: [
      { label: "Live Classes", href: "/dashboard/platform-admin/live-classes", icon: Video },
      { label: "Certificates", href: "/dashboard/platform-admin/certificates", icon: Award },
    ],
  },
  {
    title: "COMMERCE",
    items: [
      { label: "Payments", href: "/dashboard/platform-admin/payments", icon: CreditCard },
    ],
  },
  {
    title: "TRUST & SAFETY",
    items: [
      { label: "Security", href: "/dashboard/platform-admin/security", icon: Shield },
      { label: "Audit Logs", href: "/dashboard/platform-admin/audit", icon: Activity },
    ],
  },
  {
    title: "PLATFORM",
    items: [
      { label: "Settings", href: "/dashboard/platform-admin/settings", icon: Settings },
    ],
  },
]

export function PlatformAdminSidebar() {
  const pathname = usePathname()
  const [expandedSections, setExpandedSections] = useState<Set<string>>(
    new Set(NAV_SECTIONS.map(s => s.title))
  )
  const [health, setHealth] = useState<PlatformHealth | null>(null)

  useEffect(() => {
    platformAdminApi.getHealth().then(setHealth).catch(() => {})
  }, [])

  function toggleSection(title: string) {
    setExpandedSections(prev => {
      const next = new Set(prev)
      if (next.has(title)) next.delete(title)
      else next.add(title)
      return next
    })
  }

  return (
    <div className="flex h-full flex-col">
      <div className="flex h-16 items-center gap-2 border-b border-border px-4">
        <Logo />
        <div className="ml-1 min-w-0">
          <p className="text-xs font-bold tracking-tight text-foreground truncate">PLATFORM ADMIN</p>
          <p className="text-[10px] text-muted-foreground">Operations & Governance</p>
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-3 space-y-1">
        {NAV_SECTIONS.map((section) => {
          const isExpanded = expandedSections.has(section.title)
          const isActive = section.items.some((item) => pathname === item.href || pathname.startsWith(item.href + "/"))

          return (
            <div key={section.title}>
              <button
                onClick={() => toggleSection(section.title)}
                className={cn(
                  "flex w-full items-center gap-1.5 rounded-lg px-2 py-1.5 text-[10px] font-bold tracking-widest uppercase transition-colors",
                  isActive ? "text-primary" : "text-muted-foreground hover:text-foreground"
                )}
              >
                {isExpanded ? <ChevronDown className="size-3" /> : <ChevronRight className="size-3" />}
                {section.title}
              </button>

              {isExpanded && (
                <div className="mt-0.5 space-y-0.5">
                  {section.items.map((item) => {
                    const isActiveItem = pathname === item.href || pathname.startsWith(item.href + "/")
                    return (
                      <Link
                        key={item.href}
                        href={item.href}
                        className={cn(
                          "flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium transition-all",
                          isActiveItem
                            ? "bg-primary/10 text-primary"
                            : "text-muted-foreground hover:bg-muted hover:text-foreground"
                        )}
                      >
                        <item.icon className="size-4 shrink-0" />
                        {item.label}
                      </Link>
                    )
                  })}
                </div>
              )}
            </div>
          )
        })}
      </nav>

      <div className="border-t border-border p-3">
        <div className="rounded-xl bg-muted/50 p-3">
          <div className="flex items-center gap-2">
            <div className="flex size-7 items-center justify-center rounded-lg bg-primary/10">
              <Zap className="size-3.5 text-primary" />
            </div>
            <div className="min-w-0">
              <p className="text-xs font-medium text-foreground">Platform Status</p>
              {health ? (
                <p className={`text-[10px] font-medium ${health.databaseStatus === "Operational" ? "text-green-600" : "text-amber-600"}`}>
                  DB: {health.databaseStatus} | API: {health.apiStatus}
                </p>
              ) : (
                <p className="text-[10px] text-muted-foreground">Checking...</p>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
