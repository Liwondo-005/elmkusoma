"use client"

import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { LayoutDashboard, BookOpen, Video, FileText, BarChart3, MessageSquare, Award, Bookmark, User, Settings, LogOut, ClipboardList, GraduationCap, PenTool, School, Users, Shield, ShieldCheck, ClipboardCheck, Calendar, Bell, Clock, TrendingUp, Library, HeartPulse, FileBarChart, Trophy, Target, Activity, Film, Compass, Backpack, Map, Lightbulb, FlaskConical, Mic, Swords, Zap, AlertCircle, Home, Palette, Globe, Eye } from "lucide-react"
import { Logo } from "@/components/logo"
import { cn } from "@/lib/utils"
import { useAuth } from "@/lib/auth"

const nurseryNav: Array<{ label: string; href: string; icon: typeof LayoutDashboard }> = [
  { label: "My World", href: "/dashboard/nursery", icon: Home },
  { label: "Learning Journey", href: "/dashboard/nursery/learning-journey", icon: GraduationCap },
  { label: "Play & Learn", href: "/dashboard/nursery/play", icon: Activity },
  { label: "Stories", href: "/dashboard/nursery/stories", icon: BookOpen },
  { label: "Discovery", href: "/dashboard/nursery/discovery", icon: TrendingUp },
  { label: "Create Studio", href: "/dashboard/nursery/create", icon: PenTool },
  { label: "Music & Movement", href: "/dashboard/nursery/music", icon: Activity },
  { label: "Speak & Listen", href: "/dashboard/nursery/speak-listen", icon: MessageSquare },
  { label: "My Teacher", href: "/dashboard/nursery/my-teacher", icon: User },
  { label: "Daily Quest", href: "/dashboard/nursery/daily-quest", icon: Trophy },
  { label: "Milestones", href: "/dashboard/nursery/milestones", icon: Award },
  { label: "Feelings", href: "/dashboard/nursery/feelings", icon: HeartPulse },
  { label: "Movement Breaks", href: "/dashboard/nursery/movement", icon: Activity },
  { label: "Tanzania", href: "/dashboard/nursery/tanzania", icon: Globe },
  { label: "Evidence", href: "/dashboard/nursery/evidence", icon: Award },
  { label: "Portfolio", href: "/dashboard/nursery/portfolio", icon: FileText },
  { label: "Learn Together", href: "/dashboard/nursery/learn-together", icon: Users },
  { label: "Missions", href: "/dashboard/nursery/missions", icon: Target },
  { label: "Family Learning", href: "/dashboard/nursery/family-learning", icon: Users },
  { label: "My Progress", href: "/dashboard/nursery/progress", icon: BarChart3 },
  { label: "Backpack", href: "/dashboard/nursery/backpack", icon: Award },
  { label: "Live Class", href: "/dashboard/nursery/live", icon: Video },
  { label: "Notifications", href: "/dashboard/nursery/notifications", icon: Bell },
  { label: "Profile", href: "/dashboard/nursery/profile", icon: User },
]

type PrimaryNavSection = {
  group: string
  items: Array<{ label: string; href: string; icon: typeof LayoutDashboard; dotColor?: string }>
}

const primaryNavSections: PrimaryNavSection[] = [
  {
    group: "HOME",
    items: [
      { label: "My World", href: "/dashboard", icon: LayoutDashboard, dotColor: "bg-blue-500" },
    ],
  },
  {
    group: "LEARNING",
    items: [
      { label: "Learn", href: "/dashboard/lessons", icon: BookOpen, dotColor: "bg-teal-500" },
      { label: "Journey", href: "/dashboard/journey", icon: Map, dotColor: "bg-indigo-500" },
      { label: "Read", href: "/dashboard/reading", icon: BookOpen, dotColor: "bg-violet-500" },
    ],
  },
  {
    group: "PRACTICE ZONE",
    items: [
      { label: "Practice", href: "/dashboard/assignments", icon: ClipboardList, dotColor: "bg-yellow-500" },
      { label: "Create", href: "/dashboard/create", icon: Palette, dotColor: "bg-pink-500" },
      { label: "Assessments", href: "/dashboard/assessments", icon: PenTool, dotColor: "bg-rose-500" },
      { label: "Discovery", href: "/dashboard/discovery", icon: Lightbulb, dotColor: "bg-amber-500" },
      { label: "Labs", href: "/dashboard/labs", icon: FlaskConical, dotColor: "bg-emerald-500" },
    ],
  },
  {
    group: "CHALLENGES",
    items: [
      { label: "Quests", href: "/dashboard/quests", icon: Swords, dotColor: "bg-red-500" },
      { label: "Challenge Zone", href: "/dashboard/challenge-zone", icon: Zap, dotColor: "bg-orange-500" },
      { label: "Mistake Lab", href: "/dashboard/mistake-lab", icon: AlertCircle, dotColor: "bg-amber-600" },
      { label: "AI Guide", href: "/dashboard/ai-guide", icon: Target, dotColor: "bg-purple-500" },
    ],
  },
  {
    group: "COLLABORATION",
    items: [
      { label: "Learn Together", href: "/dashboard/learn-together", icon: Users, dotColor: "bg-cyan-500" },
      { label: "Family", href: "/dashboard/family", icon: Home, dotColor: "bg-teal-600" },
    ],
  },
  {
    group: "TRACKING",
    items: [
      { label: "Live Learning", href: "/dashboard/live-classes", icon: Video, dotColor: "bg-red-500" },
      { label: "Progress", href: "/dashboard/progress", icon: BarChart3, dotColor: "bg-cyan-500" },
      { label: "Evidence", href: "/dashboard/evidence", icon: Award, dotColor: "bg-green-500" },
      { label: "Passport", href: "/dashboard/passport", icon: Map, dotColor: "bg-amber-500" },
      { label: "Attendance", href: "/dashboard/attendance", icon: ClipboardCheck, dotColor: "bg-emerald-600" },
    ],
  },
  {
    group: "ACCOUNT",
    items: [
      { label: "My Teacher", href: "/dashboard/my-teachers", icon: GraduationCap, dotColor: "bg-purple-500" },
      { label: "Messages", href: "/dashboard/messages", icon: MessageSquare, dotColor: "bg-blue-600" },
      { label: "Notifications", href: "/dashboard/notifications", icon: Bell, dotColor: "bg-orange-500" },
      { label: "Portfolio", href: "/dashboard/portfolio", icon: Backpack, dotColor: "bg-violet-500" },
      { label: "Learning Profile", href: "/dashboard/learning-profile", icon: User, dotColor: "bg-indigo-500" },
      { label: "Profile", href: "/dashboard/profile", icon: User, dotColor: "bg-slate-500" },
      { label: "Settings", href: "/dashboard/settings", icon: Settings, dotColor: "bg-gray-500" },
    ],
  },
]

const secondaryNav: Array<{ label: string; href: string; icon: typeof LayoutDashboard }> = [
  { label: "My Academic World", href: "/dashboard/secondary", icon: LayoutDashboard },
  { label: "Learn", href: "/dashboard/secondary/learn", icon: BookOpen },
  { label: "Practice", href: "/dashboard/secondary/practice", icon: PenTool },
  { label: "Assess", href: "/dashboard/secondary/assess", icon: Award },
  { label: "Revision", href: "/dashboard/secondary/revision", icon: TrendingUp },
  { label: "Live", href: "/dashboard/secondary/live", icon: Video },
  { label: "Projects", href: "/dashboard/secondary/projects", icon: FileText },
  { label: "Research", href: "/dashboard/secondary/learn/research", icon: BookOpen },
  { label: "Science Lab", href: "/dashboard/secondary/learn/science-lab", icon: Library },
  { label: "Portfolio", href: "/dashboard/secondary/portfolio", icon: FileBarChart },
  { label: "Progress", href: "/dashboard/secondary/progress", icon: BarChart3 },
  { label: "Future World", href: "/dashboard/secondary/future", icon: Target },
  { label: "My Teachers", href: "/dashboard/secondary/teachers", icon: Users },
  { label: "Notifications", href: "/dashboard/secondary/notifications", icon: Bell },
]

const collegeNav: Array<{ label: string; href: string; icon: typeof LayoutDashboard }> = [
  { label: "Dashboard", href: "/dashboard/learner", icon: LayoutDashboard },
  { label: "Module Workspace", href: "/dashboard/learner/module-workspace", icon: BookOpen },
  { label: "Competencies", href: "/dashboard/learner/competencies", icon: Target },
  { label: "Projects", href: "/dashboard/learner/projects", icon: Activity },
  { label: "Fieldwork", href: "/dashboard/learner/fieldwork", icon: Bookmark },
  { label: "Portfolio", href: "/dashboard/learner/portfolio", icon: Award },
  { label: "Show What I Can Do", href: "/dashboard/learner/demonstrations", icon: Trophy },
  { label: "My Evidence", href: "/dashboard/learner/evidence", icon: ClipboardList },
  { label: "Collaborations", href: "/dashboard/learner/collaborations", icon: Users },
  { label: "Workshops & Labs", href: "/dashboard/learner/workshops", icon: FlaskConical },
  { label: "Professional Dev", href: "/dashboard/learner/professional-dev", icon: TrendingUp },
  { label: "Study Planner", href: "/dashboard/learner/study-planner", icon: Clock },
  { label: "Live Campus", href: "/dashboard/learner/live-classes", icon: Video },
  { label: "Career World", href: "/dashboard/learner/career", icon: Target },
  { label: "Messages", href: "/dashboard/messages", icon: MessageSquare },
  { label: "Notifications", href: "/dashboard/notifications", icon: Bell },
  { label: "Profile", href: "/dashboard/profile", icon: User },
  { label: "Settings", href: "/dashboard/settings", icon: Settings },
]

const universityNav: Array<{ label: string; href: string; icon: typeof LayoutDashboard }> = [
  { label: "Dashboard", href: "/dashboard/learner", icon: LayoutDashboard },
  { label: "Course Workspace", href: "/dashboard/learner/course-workspace", icon: BookOpen },
  { label: "Research", href: "/dashboard/learner/research", icon: Target },
  { label: "Thesis", href: "/dashboard/learner/thesis", icon: FileText },
  { label: "Deep Learning", href: "/dashboard/learner/deep-learning", icon: GraduationCap },
  { label: "Projects", href: "/dashboard/learner/projects", icon: Activity },
  { label: "Collaborations", href: "/dashboard/learner/collaborations", icon: Users },
  { label: "Portfolio", href: "/dashboard/learner/portfolio", icon: Award },
  { label: "Fieldwork", href: "/dashboard/learner/fieldwork", icon: Bookmark },
  { label: "Show What I Can Do", href: "/dashboard/learner/demonstrations", icon: Trophy },
  { label: "My Evidence", href: "/dashboard/learner/evidence", icon: ClipboardList },
  { label: "Workshops & Labs", href: "/dashboard/learner/workshops", icon: FlaskConical },
  { label: "Professional Dev", href: "/dashboard/learner/professional-dev", icon: TrendingUp },
  { label: "Study Planner", href: "/dashboard/learner/study-planner", icon: Clock },
  { label: "Calendar", href: "/dashboard/learner/calendar", icon: Calendar },
  { label: "Live Campus", href: "/dashboard/learner/live-classes", icon: Video },
  { label: "Career World", href: "/dashboard/learner/career", icon: TrendingUp },
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
  { label: "Platform Admin", href: "/dashboard/platform-admin", icon: ShieldCheck },
  { label: "Administration", href: "/dashboard/admin", icon: ShieldCheck },
  { label: "People", href: "/dashboard/admin/people", icon: Users },
  { label: "Organization", href: "/dashboard/admin/profile", icon: School },
  { label: "Institutions", href: "/dashboard/admin/institutions", icon: School },
  { label: "Programmes", href: "/dashboard/admin/programmes", icon: GraduationCap },
  { label: "Departments", href: "/dashboard/admin/departments", icon: Users },
  { label: "Competencies", href: "/dashboard/admin/competencies", icon: Target },
  { label: "Roles", href: "/dashboard/admin/roles", icon: Shield },
  { label: "Audit Log", href: "/dashboard/admin/audit", icon: Eye },
  { label: "Data Import", href: "/dashboard/admin/import", icon: FileText },
  { label: "Settings", href: "/dashboard/admin/settings", icon: Settings },
]

function getStudentNav(user: { learningLevel?: string | null } | null) {
  const level = (user?.learningLevel || "").toUpperCase()
  if (level === "NURSERY") return nurseryNav
  if (level === "PRIMARY") return primaryNavSections
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
  const isPrimary = (user?.learningLevel || "").toUpperCase() === "PRIMARY"

  const activeNav = isParent
    ? parentNav
    : isLearner
      ? learnerNav
      : getStudentNav(user)

  function renderNavItems(items: Array<{ label: string; href: string; icon: typeof LayoutDashboard; badge?: number; dotColor?: string }>) {
    return items.map((item) => {
      const active = pathname === item.href || (item.href !== "/dashboard" && item.href !== "/dashboard/teacher" && pathname.startsWith(item.href))
      return (
        <Link
          key={item.href}
          href={item.href}
          onClick={onNavigate}
          className={cn(
            "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all",
            active
              ? "bg-primary text-primary-foreground shadow-xs"
              : "text-muted-foreground hover:bg-muted hover:text-foreground",
          )}
        >
          {isPrimary && !active && item.dotColor && (
            <span className={`size-1.5 shrink-0 rounded-full ${item.dotColor}`} />
          )}
          {(!isPrimary || active) && <item.icon className="size-4 shrink-0" />}
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

  function renderPrimarySections(sections: PrimaryNavSection[]) {
    return sections.map((section, si) => (
      <div key={section.group}>
        {si > 0 && <div className="my-2 border-t border-border" />}
        <p className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
          {section.group}
        </p>
        {renderNavItems(section.items)}
      </div>
    ))
  }

  return (
    <div className="flex h-full flex-col bg-card">
      <div className="flex h-16 items-center border-b border-border px-5">
        <Logo />
      </div>

      <nav className="flex-1 space-y-1 overflow-y-auto p-3" aria-label="Main navigation" role="navigation">
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
        ) : isPrimary && !isParent && !isLearner ? (
          renderPrimarySections(primaryNavSections)
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
