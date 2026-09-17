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

const authorityNav: Array<{ label: string; href: string; icon: typeof LayoutDashboard }> = [
  { label: "Overview", href: "/oversight", icon: LayoutDashboard },
  { label: "Schools", href: "/oversight/schools", icon: Building2 },
  { label: "Performance", href: "/oversight/performance", icon: BarChart3 },
  { label: "Attendance", href: "/oversight/attendance", icon: ClipboardList },
  { label: "Curriculum", href: "/oversight/curriculum", icon: BookOpen },
  { label: "Assessments", href: "/oversight/assessments", icon: FileText },
  { label: "Live Classes", href: "/oversight/live-classes", icon: Video },
  { label: "Reports", href: "/oversight/reports", icon: FileBarChart },
  { label: "Alerts", href: "/oversight/alerts", icon: AlertTriangle },
]

export function AuthoritySidebar({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname()
  const { user, logout } = useAuth()
  const router = useRouter()

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
          Education Oversight
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
              <span className="flex-1">{item.label}</span>
            </Link>
          )
        })}

        <div className="my-2 border-t border-border" />
        <p className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
          Account
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
          <span className="flex-1">Profile</span>
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
          <span className="flex-1">Settings</span>
        </Link>
      </nav>

      <div className="border-t border-border p-3">
        <button
          type="button"
          onClick={handleLogout}
          className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        >
          <LogOut className="size-4 shrink-0" />
          Logout
        </button>
      </div>
    </div>
  )
}