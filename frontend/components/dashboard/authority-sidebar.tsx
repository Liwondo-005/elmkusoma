"use client"

import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { useEffect, useState } from "react"
import {
  LayoutDashboard,
  Building2,
  BarChart3,
  ClipboardList,
  BookOpen,
  FileText,
  AlertTriangle,
  Video,
  FileBarChart,
  User,
  Settings,
  LogOut,
  ChevronDown,
  Megaphone,
  Bell,
  ShieldCheck,
  Download,
  Send,
  FlaskConical,
} from "lucide-react"
import { Logo } from "@/components/logo"
import { cn } from "@/lib/utils"
import { useAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { oversightApi, notificationsApi } from "@/lib/api"

interface NavItem {
  label: string
  labelKey?: string
  href: string
  icon: typeof LayoutDashboard
  badge?: "attention" | "notifications"
}

interface NavGroup {
  id: string
  labelKey: string
  items: NavItem[]
}

// Nationaladmin.md §7 — grouped command-centre navigation. Every href maps to
// a real route (dead links are a spec violation), and the scope indicator plus
// badges are fed by real endpoints.
// Exported for the generic /dashboard shell (DashboardSidebar): authority
// roles that open Profile/Settings get these same oversight links instead of
// the student fall-through nav.
export const navGroups: NavGroup[] = [
  {
    id: "overview",
    labelKey: "groupOverview",
    items: [{ label: "Command Center", labelKey: "overview", href: "/oversight", icon: LayoutDashboard }],
  },
  {
    id: "education",
    labelKey: "groupEducation",
    items: [
      { label: "Schools", labelKey: "schools", href: "/oversight/schools", icon: Building2 },
      { label: "Performance", labelKey: "performance", href: "/oversight/performance", icon: BarChart3 },
      { label: "Attendance", labelKey: "attendance", href: "/oversight/attendance", icon: ClipboardList },
      { label: "Curriculum", labelKey: "curriculum", href: "/oversight/curriculum", icon: BookOpen },
      { label: "Assessments", labelKey: "assessments", href: "/oversight/assessments", icon: FileText },
      { label: "Live Classes", labelKey: "liveClasses", href: "/oversight/live-classes", icon: Video },
    ],
  },
  {
    id: "governance",
    labelKey: "governance",
    items: [
      { label: "Alerts", labelKey: "alerts", href: "/oversight/alerts", icon: AlertTriangle, badge: "attention" },
      { label: "Data Quality", labelKey: "dataQuality", href: "/oversight/data-quality", icon: FlaskConical },
    ],
  },
  {
    id: "communication",
    labelKey: "groupCommunication",
    items: [
      { label: "Announcements", labelKey: "announcements", href: "/oversight/announcements", icon: Megaphone },
      { label: "Notifications", labelKey: "notifications", href: "/oversight/notifications", icon: Bell, badge: "notifications" },
    ],
  },
  {
    id: "reports",
    labelKey: "reports",
    items: [{ label: "Reports", labelKey: "reports", href: "/oversight/reports", icon: FileBarChart }],
  },
]

const STORAGE_KEY = "oversight.nav.collapsedGroups"

function readCollapsed(): string[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as string[]) : []
  } catch {
    return []
  }
}

export function AuthoritySidebar({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname()
  const { user, logout } = useAuth()
  const router = useRouter()
  const t = useTranslations("nav")
  const tc = useTranslations("common")

  const [collapsedGroups, setCollapsedGroups] = useState<string[]>([])
  const [scopeLabel, setScopeLabel] = useState<string | null>(null)
  const [scopeName, setScopeName] = useState<string | null>(null)
  const [attentionCount, setAttentionCount] = useState(0)
  const [unreadCount, setUnreadCount] = useState(0)

  const isAuthority =
    user?.role === "National Admin" ||
    user?.role === "Regional Admin" ||
    user?.role === "District Admin"
  const isNational = user?.role === "National Admin"
  // Regional Admins can compose as well — always region-scoped server-side.
  const canSendAnnouncement = isNational || user?.role === "Regional Admin"

  useEffect(() => {
    setCollapsedGroups(readCollapsed())
  }, [])

  useEffect(() => {
    if (!isAuthority) return
    let cancelled = false
    const load = async () => {
      try {
        const dash = await oversightApi.dashboard()
        if (!cancelled) {
          const type = dash.jurisdictionSummary?.type
          setScopeLabel(
            type === "region" ? t("regionalScope") : type === "district" ? t("districtScope") : t("nationalScope"),
          )
          setScopeName(
            `${dash.jurisdictionSummary?.name || ""} (${dash.jurisdictionSummary?.code || ""})`.trim(),
          )
        }
      } catch {
        if (!cancelled) {
          setScopeLabel(null)
          setScopeName(null)
        }
      }
      try {
        const attention = await oversightApi.attention()
        if (!cancelled) setAttentionCount(attention.summary?.total ?? 0)
      } catch {
        /* badge stays at 0 */
      }
      try {
        const counts = await notificationsApi.getUnreadCount()
        if (!cancelled) setUnreadCount(counts.count ?? 0)
      } catch {
        /* badge stays at 0 */
      }
    }
    load()
    const interval = setInterval(load, 60000)
    return () => {
      cancelled = true
      clearInterval(interval)
    }
  }, [isAuthority, t])

  function toggleGroup(id: string) {
    setCollapsedGroups((prev) => {
      const next = prev.includes(id) ? prev.filter((g) => g !== id) : [...prev, id]
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
      } catch {}
      return next
    })
  }

  function handleLogout() {
    logout()
    router.push("/login")
  }

  if (!isAuthority) {
    return null
  }

  return (
    <div className="flex h-full flex-col bg-card">
      <div className="flex h-16 items-center border-b border-border px-5">
        <Logo />
      </div>

      {/* Scope indicator — current jurisdiction (Nationaladmin.md §7). */}
      <div className="flex items-center gap-2 border-b border-border px-4 py-2.5">
        <span className="flex size-6 items-center justify-center rounded-md bg-primary/10">
          <ShieldCheck className="size-3.5 text-primary" />
        </span>
        <div className="min-w-0">
          <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
            {scopeLabel ?? t("nationalScope")}
          </p>
          <p className="truncate text-xs font-medium text-foreground">
            {scopeName ?? t("overview")}
          </p>
        </div>
      </div>

      <nav className="flex-1 space-y-4 overflow-y-auto p-3" aria-label={tc("mainNavigation")}>
        {navGroups.map((group) => {
          const isCollapsed = collapsedGroups.includes(group.id)
          const groupActive = group.items.some(
            (item) => pathname === item.href || (item.href !== "/oversight" && pathname.startsWith(item.href)),
          )
          return (
            <div key={group.id}>
              <button
                type="button"
                onClick={() => toggleGroup(group.id)}
                aria-expanded={!isCollapsed}
                className="flex w-full items-center gap-1 px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground transition-colors hover:text-foreground"
              >
                <span className="flex-1 text-left">{group.labelKey ? t(group.labelKey) : group.id}</span>
                <ChevronDown className={cn("size-3 transition-transform", isCollapsed && "-rotate-90")} />
              </button>
              {!isCollapsed &&
                group.items.map((item) => {
                  const active =
                    pathname === item.href || (item.href !== "/oversight" && pathname.startsWith(item.href))
                  const badgeValue = item.badge === "attention" ? attentionCount : item.badge === "notifications" ? unreadCount : 0
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      onClick={onNavigate}
                      className={cn(
                        "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
                        active
                          ? "bg-primary text-primary-foreground shadow-xs"
                          : groupActive
                            ? "text-foreground hover:bg-muted"
                            : "text-muted-foreground hover:bg-muted hover:text-foreground",
                      )}
                    >
                      <item.icon className="size-4 shrink-0" />
                      <span className="flex-1">{item.labelKey ? t(item.labelKey) : item.label}</span>
                      {badgeValue > 0 && (
                        <span
                          className={cn(
                            "inline-flex min-w-5 items-center justify-center rounded-full px-1 py-0.5 text-[10px] font-bold",
                            active ? "bg-primary-foreground/20 text-primary-foreground" : "bg-orange-500/15 text-orange-600",
                          )}
                        >
                          {badgeValue > 99 ? "99+" : badgeValue}
                        </span>
                      )}
                    </Link>
                  )
                })}
            </div>
          )
        })}

        {/* Quick actions (§7) — every one navigates to a real working surface. */}
        <div>
          <p className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
            {t("quickActions")}
          </p>
          <div className="space-y-1">
            {canSendAnnouncement && (
              <Link
                href="/oversight/announcements?action=create"
                onClick={onNavigate}
                className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
              >
                <Send className="size-4 shrink-0" />
                <span className="flex-1">{t("sendAnnouncement")}</span>
              </Link>
            )}
            <Link
              href="/oversight/reports"
              onClick={onNavigate}
              className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
            >
              <Download className="size-4 shrink-0" />
              <span className="flex-1">{t("exportReport")}</span>
            </Link>
            <Link
              href="/oversight/data-quality"
              onClick={onNavigate}
              className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
            >
              <FlaskConical className="size-4 shrink-0" />
              <span className="flex-1">{t("runDataQuality")}</span>
            </Link>
          </div>
        </div>

        <div className="border-t border-border pt-3">
          <p className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
            {t("account")}
          </p>
          <div className="space-y-1">
            <Link
              href="/dashboard/profile"
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
                pathname === "/dashboard/profile"
                  ? "bg-primary text-primary-foreground shadow-xs"
                  : "text-muted-foreground hover:bg-muted hover:text-foreground",
              )}
            >
              <User className="size-4 shrink-0" />
              <span className="flex-1">{t("profile")}</span>
            </Link>
            <Link
              href="/dashboard/settings"
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
                pathname === "/dashboard/settings"
                  ? "bg-primary text-primary-foreground shadow-xs"
                  : "text-muted-foreground hover:bg-muted hover:text-foreground",
              )}
            >
              <Settings className="size-4 shrink-0" />
              <span className="flex-1">{t("settings")}</span>
            </Link>
          </div>
        </div>
      </nav>

      <div className="border-t border-border p-3">
        <button
          type="button"
          onClick={handleLogout}
          className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        >
          <LogOut className="size-4 shrink-0" />
          {tc("logout")}
        </button>
      </div>
    </div>
  )
}
