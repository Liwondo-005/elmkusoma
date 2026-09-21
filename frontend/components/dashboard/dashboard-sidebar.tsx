"use client"

import { useState, useEffect, useCallback } from "react"
import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { LayoutDashboard, BookOpen, Video, FileText, BarChart3, MessageSquare, Award, Bookmark, User, Settings, LogOut, ClipboardList, GraduationCap, PenTool, School, Users, Shield, ShieldCheck, ClipboardCheck, Calendar, Bell, Clock, TrendingUp, Library, HeartPulse, FileBarChart, Trophy, Target, Activity, Film, Compass, Backpack, Map, Lightbulb, FlaskConical, Mic, Swords, Zap, AlertCircle, Home, Palette, Globe, Eye, Radio, Search, Brain, ChevronLeft } from "lucide-react"
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
  { label: "Events", href: "/dashboard/learner/events", icon: Calendar },
  { label: "Notifications", href: "/dashboard/secondary/notifications", icon: Bell },
]

type CollegeNavSection = {
  group: string
  items: Array<{ label: string; href: string; icon: typeof LayoutDashboard; dotColor?: string; badge?: number }>
}

const collegeNavSections: CollegeNavSection[] = [
  {
    group: "OVERVIEW",
    items: [
      { label: "Dashboard", href: "/dashboard/learner", icon: LayoutDashboard, dotColor: "bg-blue-500" },
    ],
  },
  {
    group: "MY COLLEGE",
    items: [
      { label: "My Learning", href: "/dashboard/learner/my-learning", icon: BookOpen, dotColor: "bg-teal-500" },
      { label: "Module Workspace", href: "/dashboard/learner/module-workspace", icon: BookOpen, dotColor: "bg-indigo-500" },
      { label: "Competencies", href: "/dashboard/learner/competencies", icon: Target, dotColor: "bg-violet-500" },
    ],
  },
  {
    group: "LIVE & MEDIA",
    items: [
      { label: "Live Campus", href: "/dashboard/learner/live-campus", icon: Radio, dotColor: "bg-red-500" },
      { label: "Media Library", href: "/dashboard/learner/media-library", icon: Film, dotColor: "bg-pink-500" },
      { label: "Resources", href: "/dashboard/learner/resources", icon: Library, dotColor: "bg-amber-500" },
      { label: "Events", href: "/dashboard/learner/events", icon: Calendar, dotColor: "bg-purple-500" },
    ],
  },
  {
    group: "BUILD & DISCOVER",
    items: [
      { label: "Projects", href: "/dashboard/learner/projects", icon: Activity, dotColor: "bg-emerald-500" },
      { label: "Collaborations", href: "/dashboard/learner/collaborations", icon: Users, dotColor: "bg-blue-600" },
    ],
  },
  {
    group: "PRACTICAL",
    items: [
      { label: "Practical Lab", href: "/dashboard/learner/practical-lab", icon: FlaskConical, dotColor: "bg-green-500" },
      { label: "Workshops & Labs", href: "/dashboard/learner/workshops", icon: FlaskConical, dotColor: "bg-teal-600" },
      { label: "Software & Tools", href: "/dashboard/learner/software-tools", icon: Zap, dotColor: "bg-yellow-500" },
      { label: "Assessments", href: "/dashboard/learner/assessments", icon: ClipboardList, dotColor: "bg-rose-500" },
      { label: "Fieldwork", href: "/dashboard/learner/fieldwork", icon: Bookmark, dotColor: "bg-teal-500" },
    ],
  },
  {
    group: "ACADEMICS",
    items: [
      { label: "Academic Progress", href: "/dashboard/learner/academic-progress", icon: BarChart3, dotColor: "bg-blue-500" },
      { label: "Academic Record", href: "/dashboard/learner/academic-record", icon: FileBarChart, dotColor: "bg-indigo-500" },
      { label: "Study Planner", href: "/dashboard/learner/study-planner", icon: Clock, dotColor: "bg-violet-500" },
    ],
  },
  {
    group: "MY WORLD",
    items: [
      { label: "Portfolio", href: "/dashboard/learner/portfolio", icon: Award, dotColor: "bg-amber-500" },
      { label: "My Evidence", href: "/dashboard/learner/evidence", icon: ClipboardList, dotColor: "bg-green-500" },
      { label: "Show What I Can Do", href: "/dashboard/learner/demonstrations", icon: Trophy, dotColor: "bg-orange-500" },
      { label: "Professional Dev", href: "/dashboard/learner/professional-dev", icon: TrendingUp, dotColor: "bg-blue-600" },
      { label: "Career World", href: "/dashboard/learner/career", icon: Target, dotColor: "bg-indigo-600" },
    ],
  },
  {
    group: "CONNECT",
    items: [
      { label: "Academic Search", href: "/dashboard/learner/search-academic", icon: Search, dotColor: "bg-slate-500" },
      { label: "Messages", href: "/dashboard/messages", icon: MessageSquare, dotColor: "bg-blue-500" },
      { label: "Notifications", href: "/dashboard/notifications", icon: Bell, dotColor: "bg-orange-500" },
    ],
  },
  {
    group: "ACCOUNT",
    items: [
      { label: "My Learning Kit", href: "/dashboard/learner/my-learning-kit", icon: Backpack, dotColor: "bg-amber-500" },
      { label: "Profile", href: "/dashboard/profile", icon: User, dotColor: "bg-slate-500" },
      { label: "Settings", href: "/dashboard/settings", icon: Settings, dotColor: "bg-gray-500" },
    ],
  },
]

type UniversityNavSection = {
  group: string
  items: Array<{ label: string; href: string; icon: typeof LayoutDashboard; dotColor?: string; badge?: number }>
}

const universityNavSections: UniversityNavSection[] = [
  {
    group: "OVERVIEW",
    items: [
      { label: "Dashboard", href: "/dashboard/learner", icon: LayoutDashboard, dotColor: "bg-blue-500" },
    ],
  },
  {
    group: "MY UNIVERSITY",
    items: [
      { label: "My Learning", href: "/dashboard/learner/my-learning", icon: BookOpen, dotColor: "bg-teal-500" },
      { label: "My Courses", href: "/dashboard/learner/courses", icon: GraduationCap, dotColor: "bg-indigo-500" },
      { label: "Course Workspace", href: "/dashboard/learner/course-workspace", icon: BookOpen, dotColor: "bg-violet-500" },
      { label: "Deep Learning", href: "/dashboard/learner/deep-learning", icon: Brain, dotColor: "bg-purple-500" },
    ],
  },
  {
    group: "LIVE & MEDIA",
    items: [
      { label: "Live Campus", href: "/dashboard/learner/live-campus", icon: Radio, dotColor: "bg-red-500" },
      { label: "Media Library", href: "/dashboard/learner/media-library", icon: Film, dotColor: "bg-pink-500" },
      { label: "Resources", href: "/dashboard/learner/resources", icon: Library, dotColor: "bg-amber-500" },
      { label: "Events", href: "/dashboard/learner/events", icon: Calendar, dotColor: "bg-purple-500" },
    ],
  },
  {
    group: "BUILD & DISCOVER",
    items: [
      { label: "Projects", href: "/dashboard/learner/projects", icon: Activity, dotColor: "bg-emerald-500" },
      { label: "Research", href: "/dashboard/learner/research", icon: Target, dotColor: "bg-cyan-500" },
      { label: "Thesis", href: "/dashboard/learner/thesis", icon: FileText, dotColor: "bg-orange-500" },
      { label: "Collaborations", href: "/dashboard/learner/collaborations", icon: Users, dotColor: "bg-blue-600" },
    ],
  },
  {
    group: "PRACTICAL",
    items: [
      { label: "Practical Lab", href: "/dashboard/learner/practical-lab", icon: FlaskConical, dotColor: "bg-green-500" },
      { label: "Workshops & Labs", href: "/dashboard/learner/workshops", icon: FlaskConical, dotColor: "bg-teal-600" },
      { label: "Software & Tools", href: "/dashboard/learner/software-tools", icon: Zap, dotColor: "bg-yellow-500" },
      { label: "Assessments", href: "/dashboard/learner/assessments", icon: ClipboardList, dotColor: "bg-rose-500" },
    ],
  },
  {
    group: "ACADEMICS",
    items: [
      { label: "Academic Progress", href: "/dashboard/learner/academic-progress", icon: BarChart3, dotColor: "bg-blue-500" },
      { label: "Academic Record", href: "/dashboard/learner/academic-record", icon: FileBarChart, dotColor: "bg-indigo-500" },
      { label: "Study Planner", href: "/dashboard/learner/study-planner", icon: Clock, dotColor: "bg-violet-500" },
      { label: "Calendar", href: "/dashboard/learner/calendar", icon: Calendar, dotColor: "bg-purple-500" },
      { label: "Competencies", href: "/dashboard/learner/competencies", icon: Target, dotColor: "bg-cyan-500" },
    ],
  },
  {
    group: "MY WORLD",
    items: [
      { label: "Portfolio", href: "/dashboard/learner/portfolio", icon: Award, dotColor: "bg-amber-500" },
      { label: "My Evidence", href: "/dashboard/learner/evidence", icon: ClipboardList, dotColor: "bg-green-500" },
      { label: "Show What I Can Do", href: "/dashboard/learner/demonstrations", icon: Trophy, dotColor: "bg-orange-500" },
      { label: "Fieldwork", href: "/dashboard/learner/fieldwork", icon: Bookmark, dotColor: "bg-teal-500" },
      { label: "Professional Dev", href: "/dashboard/learner/professional-dev", icon: TrendingUp, dotColor: "bg-blue-600" },
      { label: "Career World", href: "/dashboard/learner/career", icon: Target, dotColor: "bg-indigo-600" },
    ],
  },
  {
    group: "CONNECT",
    items: [
      { label: "Academic Search", href: "/dashboard/learner/search-academic", icon: Search, dotColor: "bg-slate-500" },
      { label: "Knowledge Discovery", href: "/dashboard/learner/knowledge-discovery", icon: Brain, dotColor: "bg-purple-500" },
      { label: "Messages", href: "/dashboard/messages", icon: MessageSquare, dotColor: "bg-blue-500" },
      { label: "Notifications", href: "/dashboard/learner/notifications-center", icon: Bell, dotColor: "bg-orange-500" },
    ],
  },
  {
    group: "ACCOUNT",
    items: [
      { label: "My Learning Kit", href: "/dashboard/learner/my-learning-kit", icon: Backpack, dotColor: "bg-amber-500" },
      { label: "Academic Assistant", href: "/dashboard/learner/academic-assistant", icon: Brain, dotColor: "bg-violet-500" },
      { label: "Profile", href: "/dashboard/profile", icon: User, dotColor: "bg-slate-500" },
      { label: "Settings", href: "/dashboard/settings", icon: Settings, dotColor: "bg-gray-500" },
    ],
  },
]

type LearnerNavSection = {
  group: string
  items: Array<{ label: string; href: string; icon: typeof LayoutDashboard; dotColor?: string }>
}

const learnerNavSections: LearnerNavSection[] = [
  {
    group: "OVERVIEW",
    items: [
      { label: "My Learning World", href: "/dashboard/learner", icon: LayoutDashboard },
    ],
  },
  {
    group: "LEARNING",
    items: [
      { label: "My Learning", href: "/dashboard/learner/my-learning", icon: GraduationCap },
      { label: "Resources", href: "/dashboard/learner/resources", icon: Library },
      { label: "Video Library", href: "/dashboard/learner/video-library", icon: Film },
      { label: "History", href: "/dashboard/learner/history", icon: Clock },
    ],
  },
  {
    group: "DISCOVER",
    items: [
      { label: "Explore Courses", href: "/dashboard/learner/courses", icon: BookOpen },
      { label: "Live Classes", href: "/dashboard/learner/live-classes", icon: Video },
      { label: "Events & Workshops", href: "/dashboard/learner/events", icon: Calendar },
      { label: "My Registrations", href: "/dashboard/learner/events/registered", icon: ClipboardList },
    ],
  },
  {
    group: "MY PROGRESS",
    items: [
      { label: "Progress", href: "/dashboard/learner/progress", icon: BarChart3 },
      { label: "Assessments", href: "/dashboard/learner/assessments", icon: ClipboardCheck },
      { label: "Learning Paths", href: "/dashboard/learner/learning-paths", icon: Map },
      { label: "Goals", href: "/dashboard/learner/goals", icon: Target },
      { label: "Bookmarks", href: "/dashboard/learner/bookmarks", icon: Bookmark },
      { label: "Certificates", href: "/dashboard/learner/certificates", icon: Award },
    ],
  },
  {
    group: "ACCOUNT",
    items: [
      { label: "Notifications", href: "/dashboard/learner/notifications", icon: Bell },
      { label: "Profile", href: "/dashboard/learner/profile", icon: User },
      { label: "Settings", href: "/dashboard/learner/settings", icon: Settings },
    ],
  },
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

function getStudentNavSections(user: { learningLevel?: string | null } | null) {
  const level = (user?.learningLevel || "").toUpperCase()
  if (level === "NURSERY") return null
  if (level === "PRIMARY") return null
  if (level === "COLLEGE" || level === "VETA") return collegeNavSections
  if (level === "UNIVERSITY") return universityNavSections
  return null
}

function getStudentNavFlat(user: { learningLevel?: string | null } | null) {
  const level = (user?.learningLevel || "").toUpperCase()
  if (level === "NURSERY") return nurseryNav
  if (level === "PRIMARY") return primaryNavSections.flatMap(s => s.items)
  if (level === "SECONDARY" || !level) return secondaryNav
  return []
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

  const [collapsed, setCollapsed] = useState(false)
  const [badges, setBadges] = useState<Record<string, number>>({})

  const fetchBadges = useCallback(async () => {
    if (!user?.id) return
    const newBadges: Record<string, number> = {}
    try {
      const token = localStorage.getItem("elmkusoma_access_token") || ""
      const headers = { Authorization: `Bearer ${token}` }
      const [countRes, liveRes, assessRes] = await Promise.allSettled([
        fetch(`/api/v1/notifications/${user.id}/unread-count`, { headers }),
        fetch(`/api/v1/student/live-classes/live-now`, { headers }),
        fetch(`/api/v1/student/events`, { headers })
      ])
      if (countRes.status === "fulfilled" && countRes.value.ok) {
        const d = await countRes.value.json()
        const count = d?.data?.count || 0
        if (count > 0) {
          newBadges["/dashboard/learner/notifications-center"] = count
          newBadges["/dashboard/notifications"] = count
        }
      }
      if (liveRes.status === "fulfilled" && liveRes.value.ok) {
        const d = await liveRes.value.json()
        const liveCount = Array.isArray(d?.data) ? d.data.length : 0
        if (liveCount > 0) {
          newBadges["/dashboard/learner/live-campus"] = liveCount
          newBadges["/dashboard/learner/live-classes"] = liveCount
        }
      }
    } catch {}
    setBadges(newBadges)
  }, [user?.id])

  useEffect(() => {
    fetchBadges()
    const interval = setInterval(fetchBadges, 30000)
    return () => clearInterval(interval)
  }, [fetchBadges])

  const universitySections = !isTeacher && !isLearner && !isParent ? getStudentNavSections(user) : null
  const universityFlat = !isTeacher && !isLearner && !isParent ? getStudentNavFlat(user) : null

  function renderNavItems(items: Array<{ label: string; href: string; icon: typeof LayoutDashboard; badge?: number; dotColor?: string }>) {
    return items.map((item) => {
      const active = pathname === item.href || (item.href !== "/dashboard" && item.href !== "/dashboard/teacher" && pathname.startsWith(item.href))
      const realBadge = badges[item.href] || item.badge || 0
      const isLive = item.href === "/dashboard/learner/live-campus" || item.href === "/dashboard/learner/live-classes" || (badges[item.href] || 0) > 0
      return (
        <Link
          key={item.href}
          href={item.href}
          onClick={onNavigate}
          title={collapsed ? item.label : undefined}
          className={cn(
            "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all",
            collapsed && "justify-center px-2",
            active
              ? "bg-primary text-primary-foreground shadow-xs"
              : "text-muted-foreground hover:bg-muted hover:text-foreground",
          )}
        >
          {isPrimary && !active && item.dotColor && (
            <span className={`size-1.5 shrink-0 rounded-full ${item.dotColor}`} />
          )}
          {(!isPrimary || active) && <item.icon className="size-4 shrink-0" />}
          {!collapsed && <span className="flex-1">{item.label}</span>}
          {!collapsed && realBadge > 0 && (
            <span
              className={cn(
                "inline-flex size-5 items-center justify-center rounded-full text-[10px] font-bold",
                active ? "bg-primary-foreground text-primary" : "bg-orange text-orange-foreground",
              )}
            >
              {realBadge}
            </span>
          )}
          {collapsed && realBadge > 0 && (
            <span className="absolute right-1 top-1 size-2 rounded-full bg-red-500 animate-pulse" />
          )}
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
        {!collapsed && <Logo />}
        {collapsed && <span className="mx-auto text-lg font-bold text-primary">E</span>}
        <button
          type="button"
          onClick={() => setCollapsed(!collapsed)}
          className="ml-auto rounded-md p-1 text-muted-foreground hover:bg-muted hover:text-foreground lg:flex hidden"
          title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          <ChevronLeft className={cn("size-4 transition-transform", collapsed && "rotate-180")} />
        </button>
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
        ) : isLearner ? (
          learnerNavSections.map((section, si) => (
            <div key={section.group}>
              {si > 0 && <div className="my-2 border-t border-border" />}
              <p className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                {section.group}
              </p>
              {renderNavItems(section.items)}
            </div>
          ))
        ) : isPrimary && !isParent ? (
          renderPrimarySections(primaryNavSections)
        ) : universitySections ? (
          universitySections.map((section, si) => (
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
            {universityFlat && renderNavItems(universityFlat)}
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
