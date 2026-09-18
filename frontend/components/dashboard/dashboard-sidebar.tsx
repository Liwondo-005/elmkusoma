"use client"

import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { LayoutDashboard, BookOpen, Video, FileText, BarChart3, MessageSquare, Award, Bookmark, User, Settings, LogOut, ClipboardList, GraduationCap, PenTool, School, Users, Shield, ShieldCheck, ClipboardCheck, Calendar, Bell, Clock, TrendingUp, Library, HeartPulse, FileBarChart, Trophy, Target, Activity, Film } from "lucide-react"
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
  { label: "Competencies", href: "/dashboard/learner/competencies", icon: Target },
  { label: "Projects", href: "/dashboard/learner/projects", icon: Activity },
  { label: "Fieldwork", href: "/dashboard/learner/fieldwork", icon: Bookmark },
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
  { label: "Competencies", href: "/dashboard/learner/competencies", icon: Target },
  { label: "Projects", href: "/dashboard/learner/projects", icon: Activity },
  { label: "Fieldwork", href: "/dashboard/learner/fieldwork", icon: Bookmark },
  { label: "Attendance", href: "/dashboard/attendance", icon: ClipboardList },
  { label: "Live Classes", href: "/dashboard/live-classes", icon: Video },
  { label: "Progress", href: "/dashboard/progress", icon: BarChart3 },
  { label: "Messages", href: "/dashboard/messages", icon: MessageSquare },
  { label: "Notifications", href: "/dashboard/notifications", icon: Bell },
  { label: "Profile", href: "/dashboard/profile", icon: User },
  { label: "Settings", href: "/dashboard/settings", icon: Settings },
]

const learnerNav: Array<{ label: string; href: string; icon: typeof LayoutDashboard }> = [
  { label: "Dashboard", href: "/dashboard/learner", icon: LayoutDashboard },
  { label: "Explore Courses", href: "/dashboard/learner/courses", icon: BookOpen },
  { label: "My Learning", href: "/dashboard/learner/my-learning", icon: GraduationCap },
  { label: "Resources", href: "/dashboard/learner/resources", icon: Library },
  { label: "Live Classes", href: "/dashboard/learner/live-classes", icon: Video },
  { label: "Media Library", href: "/dashboard/learner/media-library", icon: Film },
  { label: "Events & Workshops", href: "/dashboard/learner/events", icon: Calendar },
  { label: "My Registrations", href: "/dashboard/learner/events/registered", icon: ClipboardList },
  { label: "Video Library", href: "/dashboard/learner/video-library", icon: Video },
  { label: "Bookmarks", href: "/dashboard/learner/bookmarks", icon: Bookmark },
  { label: "History", href: "/dashboard/learner/history", icon: Clock },
  { label: "Certificates", href: "/dashboard/learner/certificates", icon: Award },
  { label: "Notifications", href: "/dashboard/learner/notifications", icon: Bell },
  { label: "Profile", href: "/dashboard/learner/profile", icon: User },
  { label: "Settings", href: "/dashboard/learner/settings", icon: Settings },
]

type TeacherNavSection = {
  group: string
  items: Array<{ label: string; href: string; icon: typeof LayoutDashboard; badge?: number }>
}

const teacherNavSections: TeacherNavSection[] = [
  {
    group: "OVERVIEW",
    items: [{ label: "Dashboard", href: "/dashboard/teacher", icon: LayoutDashboard }],
  },
  {
    group: "WORKSPACE",
    items: [
      { label: "My Classes", href: "/dashboard/teacher/classes", icon: BookOpen },
      { label: "Courses", href: "/dashboard/teacher/courses", icon: GraduationCap },
      { label: "Students", href: "/dashboard/teacher/students", icon: Users },
      { label: "Learner Support", href: "/dashboard/teacher/learner-support", icon: HeartPulse },
    ],
  },
  {
    group: "TEACHING",
    items: [
      { label: "Lessons", href: "/dashboard/teacher/lessons", icon: BookOpen },
      { label: "Assignments", href: "/dashboard/teacher/assignments", icon: FileText },
      { label: "Assessments", href: "/dashboard/teacher/assessments", icon: PenTool },
      { label: "Grading", href: "/dashboard/teacher/grading", icon: Award },
    ],
  },
  {
    group: "CLASS MANAGEMENT",
    items: [
      { label: "Attendance", href: "/dashboard/teacher/attendance", icon: ClipboardCheck },
      { label: "Gradebook", href: "/dashboard/teacher/gradebook", icon: BarChart3 },
      { label: "Schedule", href: "/dashboard/teacher/schedule", icon: Calendar },
    ],
  },
  {
    group: "COMMUNICATION",
    items: [
      { label: "Live Classes", href: "/dashboard/teacher/live-classes", icon: Video },
      { label: "Media Library", href: "/dashboard/teacher/media-library", icon: Film },
      { label: "Messages", href: "/dashboard/teacher/messages", icon: MessageSquare },
      { label: "Announcements", href: "/dashboard/teacher/announcements", icon: Bell },
      { label: "Notifications", href: "/dashboard/teacher/notifications", icon: Bell },
    ],
  },
  {
    group: "INSIGHTS",
    items: [
      { label: "Analytics", href: "/dashboard/teacher/analytics", icon: TrendingUp },
      { label: "Reports", href: "/dashboard/teacher/reports", icon: FileBarChart },
    ],
  },
  {
    group: "ACCOUNT",
    items: [{ label: "Settings", href: "/dashboard/teacher/settings", icon: Settings }],
  },
]

const parentNav: Array<{ label: string; href: string; icon: typeof LayoutDashboard; badge?: number }> = [
  { label: "Dashboard", href: "/dashboard/parent", icon: LayoutDashboard },
  { label: "Learning", href: "/dashboard/parent/learning", icon: BookOpen },
  { label: "Progress", href: "/dashboard/parent/reports", icon: BarChart3 },
  { label: "Assessments", href: "/dashboard/parent/assessments", icon: PenTool },
  { label: "Activity", href: "/dashboard/parent/activity", icon: Clock },
  { label: "Attendance", href: "/dashboard/parent/attendance", icon: ClipboardList },
  { label: "Assignments", href: "/dashboard/parent/assignments", icon: FileText },
  { label: "Results", href: "/dashboard/parent/results", icon: Award },
  { label: "Achievements", href: "/dashboard/parent/achievements", icon: Trophy },
  { label: "Goals", href: "/dashboard/parent/goals", icon: Target },
  { label: "Teachers", href: "/dashboard/parent/teachers", icon: Users },
  { label: "Calendar", href: "/dashboard/parent/calendar", icon: Calendar },
  { label: "Live Classes", href: "/dashboard/parent/live-classes", icon: Video },
  { label: "Library", href: "/dashboard/parent/library", icon: Library },
  { label: "Payments", href: "/dashboard/parent/payments", icon: FileText },
  { label: "Services", href: "/dashboard/parent/services", icon: School },
  { label: "Support", href: "/dashboard/parent/support", icon: HeartPulse },
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
  const isLearner = user?.role === "Other Learner"

  const activeNav = isParent
    ? parentNav
    : isLearner
      ? learnerNav
      : getStudentNav(user)

  function renderNavItems(items: Array<{ label: string; href: string; icon: typeof LayoutDashboard; badge?: number }>) {
    return items.map((item) => {
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
    })
  }

  return (
    <div className="flex h-full flex-col bg-card">
      <div className="flex h-16 items-center border-b border-border px-5">
        <Logo />
      </div>

      <nav className="flex-1 space-y-1 overflow-y-auto p-3">
        {isTeacher ? (
          teacherNavSections.map((section, si) => (
            <div key={section.group}>
              {si > 0 && <div className="my-2 border-t border-border" />}
              <p className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                {section.group}
              </p>
              {renderNavItems(section.items)}
            </div>
          ))
        ) : (
          <>
            {renderNavItems(activeNav)}
            {isAdmin && (
              <>
                <div className="my-2 border-t border-border" />
                <p className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                  Administration
                </p>
                {renderNavItems(adminNav)}
              </>
            )}
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
