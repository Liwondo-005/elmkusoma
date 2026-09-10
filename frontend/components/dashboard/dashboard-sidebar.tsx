"use client"

import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { LayoutDashboard, BookOpen, Video, FileText, BarChart3, MessageSquare, Award, Bookmark, User, Settings, LogOut, ClipboardList, GraduationCap, PenTool, School, Users, Shield, ShieldCheck, ClipboardCheck } from "lucide-react"
import { Logo } from "@/components/logo"
import { cn } from "@/lib/utils"
import { useAuth } from "@/lib/auth"

const studentNav = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "Academic", href: "/dashboard/academic", icon: School },
  { label: "Students", href: "/dashboard/students", icon: Users },
  { label: "All Courses", href: "/dashboard/admin/courses", icon: BookOpen },
  { label: "Lessons", href: "/dashboard/lessons", icon: GraduationCap },
  { label: "Enrollments", href: "/dashboard/enrollment", icon: ClipboardList },
  { label: "Assignments", href: "/dashboard/assignments", icon: FileText },
  { label: "Assessments", href: "/dashboard/assessments", icon: PenTool },
  { label: "Live Classes", href: "/live-classes", icon: Video },
  { label: "Progress", href: "/dashboard/progress", icon: BarChart3 },
  { label: "Messages", href: "/dashboard/messages", icon: MessageSquare, badge: 2 },
  { label: "Certificates", href: "/dashboard/certificates", icon: Award },
  { label: "Templates", href: "/dashboard/certificates/templates", icon: FileText },
  { label: "Issue / Revoke", href: "/dashboard/certificates/generate", icon: Award },
  { label: "Transcripts", href: "/dashboard/certificates/transcripts", icon: ClipboardList },
  { label: "Bookmarks", href: "/dashboard/bookmarks", icon: Bookmark },
  { label: "Profile", href: "/dashboard/profile", icon: User },
  { label: "Settings", href: "/dashboard/settings", icon: Settings },
]

const teacherNav = [
  { label: "Dashboard", href: "/dashboard/teacher", icon: LayoutDashboard },
  { label: "My Courses", href: "/dashboard/teacher/courses", icon: BookOpen },
  { label: "Students", href: "/dashboard/teacher/students", icon: Users },
  { label: "Assignments", href: "/dashboard/teacher/assignments", icon: FileText },
  { label: "Assessments", href: "/dashboard/teacher/assessments", icon: PenTool },
  { label: "Live Classes", href: "/dashboard/teacher/live-classes", icon: Video },
  { label: "Messages", href: "/dashboard/messages", icon: MessageSquare, badge: 2 },
  { label: "Profile", href: "/dashboard/profile", icon: User },
  { label: "Settings", href: "/dashboard/settings", icon: Settings },
]

const adminNav = [
  { label: "Administration", href: "/dashboard/admin", icon: ShieldCheck },
  { label: "Institutions", href: "/dashboard/admin/institutions", icon: School },
  { label: "Roles", href: "/dashboard/admin/roles", icon: Shield },
  { label: "Data Import", href: "/dashboard/admin/import", icon: FileText },
  { label: "Audit", href: "/dashboard/audit", icon: Shield },
]

export function DashboardSidebar({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname()
  const { user, logout } = useAuth()
  const router = useRouter()

  const isTeacher = user?.role === "Teacher" || user?.role === "Instructor"
  const isAdmin = user?.role === "Admin" || user?.role === "Institution Admin"
  const isStudent = !isTeacher && !isAdmin

  const activeNav = isTeacher ? teacherNav : studentNav

  function handleLogout() {
    logout()
    router.push("/login")
  }

  return (
    <div className="flex h-full flex-col bg-card">
      <div className="flex h-16 items-center border-b border-border px-5">
        <Logo />
      </div>

      <nav className="flex-1 space-y-1 overflow-y-auto p-3">
        {activeNav.map((item) => {
          const active = pathname === item.href || (item.href !== "/dashboard" && item.href !== "/dashboard/teacher" && pathname.startsWith(item.href))
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
              {item.badge ? (
                <span
                  className={cn(
                    "inline-flex size-5 items-center justify-center rounded-full text-[10px] font-bold",
                    active ? "bg-primary-foreground text-primary" : "bg-orange text-orange-foreground",
                  )}
                >
                  {item.badge}
                </span>
              ) : null}
            </Link>
          )
        })}

        {isAdmin && (
          <>
            <div className="my-2 border-t border-border" />
            <p className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
              Administration
            </p>
            {adminNav.map((item) => {
              const active = pathname === item.href || pathname.startsWith(item.href)
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
          </>
        )}
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
