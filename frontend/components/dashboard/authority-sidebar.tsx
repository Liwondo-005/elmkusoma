"use client"

import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import {
  LayoutDashboard,
  Building2,
  BarChart3,
  ClipboardList,
  BookOpen,
  FileText,
  Users,
  AlertTriangle,
  Video,
  FileBarChart,
  User,
  Settings,
  LogOut,
  TrendingUp,
  GraduationCap,
  Award,
  Clock,
  Target,
  Activity,
} from "lucide-react"
import { Logo } from "@/components/logo"
import { cn } from "@/lib/utils"
import { useAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"

const authorityNav: Array<{ label: string; labelKey?: string; href: string; icon: typeof LayoutDashboard }> = [
  { label: "Overview", labelKey: "overview", href: "/oversight", icon: LayoutDashboard },
  { label: "Schools", labelKey: "schools", href: "/oversight/schools", icon: Building2 },
  { label: "Performance", labelKey: "performance", href: "/oversight/performance", icon: BarChart3 },
  { label: "Attendance", labelKey: "attendance", href: "/oversight/attendance", icon: ClipboardList },
  { label: "Curriculum", labelKey: "curriculum", href: "/oversight/curriculum", icon: BookOpen },
  { label: "Assessments", labelKey: "assessments", href: "/oversight/assessments", icon: FileText },
  { label: "Live Classes", labelKey: "liveClasses", href: "/oversight/live-classes", icon: Video },
  { label: "Reports", labelKey: "reports", href: "/oversight/reports", icon: FileBarChart },
  { label: "Alerts", labelKey: "alerts", href: "/oversight/alerts", icon: AlertTriangle },
]

export function AuthoritySidebar({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname()
  const { user, logout } = useAuth()
  const router = useRouter()
  const t = useTranslations("nav")
  const tc = useTranslations("common")

  function handleLogout() {
    logout()
    router.push("/login")
  }

  const isAuthority =
    user?.role === "National Admin" ||
    user?.role === "Regional Admin" ||
    user?.role === "District Admin"

  if (!isAuthority) {
    return null
  }

  return (
    <div className="flex h-full flex-col bg-card">
      <div className="flex h-16 items-center border-b border-border px-5">
        <Logo />
      </div>

      <nav className="flex-1 space-y-1 overflow-y-auto p-3">
        <p className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
          {t("educationOversight")}
        </p>
        {authorityNav.map((item) => {
          const active = pathname === item.href || (item.href !== "/oversight" && pathname.startsWith(item.href))
          return (
            <Link
              key={item.href}
              href={item.href}
              onClick={onNavigate}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
                active
                  ? "bg-primary text-primary-foreground shadow-xs"
                  : "text-muted-foreground hover:bg-muted hover:text-foreground",
              )}
            >
              <item.icon className="size-4 shrink-0" />
              <span className="flex-1">{item.labelKey ? t(item.labelKey) : item.label}</span>
            </Link>
          )
        })}

        <div className="my-2 border-t border-border" />
        <p className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
          {t("account")}
        </p>
        <Link
          href="/oversight/profile"
          className={cn(
            "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
            pathname === "/oversight/profile" || pathname.startsWith("/oversight/profile")
              ? "bg-primary text-primary-foreground shadow-xs"
              : "text-muted-foreground hover:bg-muted hover:text-foreground",
          )}
        >
          <User className="size-4 shrink-0" />
          <span className="flex-1">{t("profile")}</span>
        </Link>
        <Link
          href="/oversight/settings"
          className={cn(
            "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
            pathname === "/oversight/settings" || pathname.startsWith("/oversight/settings")
              ? "bg-primary text-primary-foreground shadow-xs"
              : "text-muted-foreground hover:bg-muted hover:text-foreground",
          )}
        >
          <Settings className="size-4 shrink-0" />
          <span className="flex-1">{t("settings")}</span>
        </Link>
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