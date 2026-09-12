"use client"

import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { LayoutDashboard, BookOpen, Video, FileText, BarChart3, MessageSquare, Award, Bookmark, User, Settings, LogOut, ClipboardList, GraduationCap, PenTool, School, Users, Shield, ShieldCheck, ClipboardCheck, Calendar, Bell, Clock, TrendingUp } from "lucide-react"
import { Logo } from "@/components/logo"
import { cn } from "@/lib/utils"
import { useAuth } from "@/lib/auth"

const nurseryNav: Array<{ label: string; href: string; icon: typeof LayoutDashboard }> = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "My Activities", href: "/dashboard/courses", icon: BookOpen },
  { label: "Fun Lessons", href: "/dashboard/lessons", icon: GraduationCap },
  { label: "My Drawings", href: "/dashboard/progress", icon: BarChart3 },
  { label: "Attendance", href: "/dashboard/attendance", icon: ClipboardList },
  { label: "Live Classes", href: "/dashboard/live-classes", icon: Video },
  { label: "Messages", href: "/dashboard/messages", icon: MessageSquare },
  { label: "Notifications", href: "/dashboard/notifications", icon: Bell },
  { label: "Profile", href: "/dashboard/profile", icon: User },
  { label: "Settings", href: "/dashboard/settings", icon: Settings },
]

const primaryNav: Array<{ label: string; href: string; icon: typeof LayoutDashboard }> = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "My Courses", href: "/dashboard/courses", icon: BookOpen },
  { label: "Lessons", href: "/dashboard/lessons", icon: GraduationCap },
  { label: "Assignments", href: "/dashboard/assignments", icon: FileText },
  { label: "Results", href: "/dashboard/results", icon: Award },
  { label: "Attendance", href: "/dashboard/attendance", icon: ClipboardList },
  { label: "Live Classes", href: "/dashboard/live-classes", icon: Video },
  { label: "Progress", href: "/dashboard/progress", icon: BarChart3 },
  { label: "Messages", href: "/dashboard/messages", icon: MessageSquare },
  { label: "Notifications", href: "/dashboard/notifications", icon: Bell },
  { label: "Profile", href: "/dashboard/profile", icon: User },
  { label: "Settings", href: "/dashboard/settings", icon: Settings },
]

const secondaryNav: Array<{ label: string; href: string; icon: typeof LayoutDashboard }> = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "My Courses", href: "/dashboard/courses", icon: BookOpen },
  { label: "Lessons", href: "/dashboard/lessons", icon: GraduationCap },
  { label: "Assignments", href: "/dashboard/assignments", icon: FileText },
  { label: "Assessments", href: "/dashboard/assessments", icon: PenTool },
  { label: "Results", href: "/dashboard/results", icon: Award },
  { label: "Attendance", href: "/dashboard/attendance", icon: ClipboardList },
  { label: "Live Classes", href: "/dashboard/live-classes", icon: Video },
  { label: "Progress", href: "/dashboard/progress", icon: BarChart3 },
  { label: "Messages", href: "/dashboard/messages", icon: MessageSquare },
  { label: "Notifications", href: "/dashboard/notifications", icon: Bell },
  { label: "Profile", href: "/dashboard/profile", icon: User },
  { label: "Settings", href: "/dashboard/settings", icon: Settings },
]

const collegeNav: Array<{ label: string; href: string; icon: typeof LayoutDashboard }> = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "My Courses", href: "/dashboard/courses", icon: BookOpen },
  { label: "Lessons", href: "/dashboard/lessons", icon: GraduationCap },
  { label: "Assignments", href: "/dashboard/assignments", icon: FileText },
  { label: "Assessments", href: "/dashboard/assessments", icon: PenTool },
  { label: "Results", href: "/dashboard/results", icon: Award },
  { label: "Attendance", href: "/dashboard/attendance", icon: ClipboardList },
  { label: "Live Classes", href: "/dashboard/live-classes", icon: Video },
  { label: "Progress", href: "/dashboard/progress", icon: BarChart3 },
  { label: "Messages", href: "/dashboard/messages", icon: MessageSquare },
  { label: "Notifications", href: "/dashboard/notifications", icon: Bell },
  { label: "Profile", href: "/dashboard/profile", icon: User },
  { label: "Settings", href: "/dashboard/settings", icon: Settings },
]

const universityNav: Array<{ label: string; href: string; icon: typeof LayoutDashboard }> = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "My Courses", href: "/dashboard/courses", icon: BookOpen },
  { label: "Lessons", href: "/dashboard/lessons", icon: GraduationCap },
  { label: "Assignments", href: "/dashboard/assignments", icon: FileText },
  { label: "Assessments", href: "/dashboard/assessments", icon: PenTool },
  { label: "Results", href: "/dashboard/results", icon: Award },
  { label: "Attendance", href: "/dashboard/attendance", icon: ClipboardList },
  { label: "Live Classes", href: "/dashboard/live-classes", icon: Video },
  { label: "Progress", href: "/dashboard/progress", icon: BarChart3 },
  { label: "Messages", href: "/dashboard/messages", icon: MessageSquare },
  { label: "Notifications", href: "/dashboard/notifications", icon: Bell },
  { label: "Profile", href: "/dashboard/profile", icon: User },
  { label: "Settings", href: "/dashboard/settings", icon: Settings },
]

const teacherNav: Array<{ label: string; href: string; icon: typeof LayoutDashboard; badge?: number }> = [
  { label: "Dashboard", href: "/dashboard/teacher", icon: LayoutDashboard },
  { label: "My Classes", href: "/dashboard/teacher/classes", icon: BookOpen },
  { label: "Students", href: "/dashboard/teacher/students", icon: Users },
  { label: "Assignments", href: "/dashboard/teacher/assignments", icon: FileText },
  { label: "Assessments", href: "/dashboard/teacher/assessments", icon: PenTool },
  { label: "Attendance", href: "/dashboard/teacher/attendance", icon: ClipboardCheck },
  { label: "Gradebook", href: "/dashboard/teacher/gradebook", icon: BarChart3 },
  { label: "Grading", href: "/dashboard/teacher/grading", icon: Award },
  { label: "Schedule", href: "/dashboard/teacher/schedule", icon: Calendar },
  { label: "Live Classes", href: "/dashboard/teacher/live-classes", icon: Video },
  { label: "Announcements", href: "/dashboard/teacher/announcements", icon: Bell },
  { label: "Analytics", href: "/dashboard/teacher/analytics", icon: TrendingUp },
  { label: "Notifications", href: "/dashboard/teacher/notifications", icon: Bell },
  { label: "Settings", href: "/dashboard/teacher/settings", icon: Settings },
]

const parentNav: Array<{ label: string; href: string; icon: typeof LayoutDashboard }> = [
  { label: "Dashboard", href: "/dashboard/parent", icon: LayoutDashboard },
  { label: "My Children", href: "/dashboard/parent/children", icon: Users },
  { label: "Attendance", href: "/dashboard/parent/attendance", icon: ClipboardList },
  { label: "Assignments", href: "/dashboard/parent/assignments", icon: FileText },
  { label: "Results", href: "/dashboard/parent/results", icon: BarChart3 },
  { label: "Calendar", href: "/dashboard/parent/calendar", icon: Clock },
  { label: "Live Classes", href: "/live-classes", icon: Video },
  { label: "Messages", href: "/dashboard/parent/messages", icon: MessageSquare },
  { label: "Notifications", href: "/dashboard/parent/notifications", icon: Bell },
  { label: "Settings", href: "/dashboard/parent/settings", icon: Settings },
]

const adminNav: Array<{ label: string; href: string; icon: typeof LayoutDashboard; badge?: number }> = [
  { label: "Administration", href: "/dashboard/admin", icon: ShieldCheck },
  { label: "Institutions", href: "/dashboard/admin/institutions", icon: School },
  { label: "Roles", href: "/dashboard/admin/roles", icon: Shield },
  { label: "Data Import", href: "/dashboard/admin/import", icon: FileText },
  { label: "Audit", href: "/dashboard/audit", icon: Shield },
]

function getStudentNav(user: { learningLevel?: string | null } | null) {
  const level = (user?.learningLevel || "").toUpperCase()
  if (level === "NURSERY") return nurseryNav
  if (level === "PRIMARY") return primaryNav
  if (level === "COLLEGE") return collegeNav
  if (level === "UNIVERSITY") return universityNav
  return secondaryNav
}

export function DashboardSidebar({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname()
  const { user, logout } = useAuth()
  const router = useRouter()

  function handleLogout() {
    logout()
    router.push("/login")
  }

  const isTeacher = user?.role === "Teacher" || user?.role === "Instructor"
  const isAdmin = user?.role === "Admin" || user?.role === "Institution Admin"
  const isParent = user?.role === "Parent"

  const activeNav = isParent
    ? parentNav
    : isTeacher
      ? teacherNav
      : getStudentNav(user)

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
              {"badge" in item && item.badge ? (
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
