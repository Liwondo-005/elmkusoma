import {
  BookOpen, FileText, PenTool, BarChart3, Clock, Calendar,
  GraduationCap, Award, Users, Home, ClipboardList, Library, Bookmark,
  Activity, Compass, FlaskConical, Palette, Lightbulb, Target, Star, Trophy,
  Globe, Mic, BookMarked, Brain, Video, Bell, User
} from "lucide-react"
import type { ComponentType } from "react"

export type LearningLevel = "NURSERY" | "PRIMARY" | "SECONDARY" | "COLLEGE" | "UNIVERSITY"

export interface NavItem {
  label: string
  href: string
  icon: ComponentType<{ className?: string }>
  badge?: string
}

export interface SubjectDomain {
  name: string
  icon: ComponentType<{ className?: string }>
  color: string
  bgColor: string
  description: string
}

export interface DashboardConfig {
  greeting: string
  subtitle: string
  navItems: NavItem[]
  sections: string[]
  emptyStateTitle: string
  emptyStateDescription: string
  cardStyle: "colorful" | "academic" | "professional"
  subjects?: SubjectDomain[]
  learningDomains?: string[]
}

export const primarySubjects: SubjectDomain[] = [
  { name: "Mathematics", icon: Brain, color: "text-blue-600", bgColor: "bg-blue-50", description: "Numbers, shapes, and problem solving" },
  { name: "English", icon: BookOpen, color: "text-emerald-600", bgColor: "bg-emerald-50", description: "Reading, writing, and communication" },
  { name: "Kiswahili", icon: Mic, color: "text-orange-600", bgColor: "bg-orange-50", description: "Kusoma, kuandika, na mazungumzo" },
  { name: "Science", icon: FlaskConical, color: "text-teal-600", bgColor: "bg-teal-50", description: "Explore the world around you" },
  { name: "Social Studies", icon: Globe, color: "text-purple-600", bgColor: "bg-purple-50", description: "Tanzania, community, and geography" },
  { name: "Religious Education", icon: BookMarked, color: "text-amber-600", bgColor: "bg-amber-50", description: "Faith and moral values" },
  { name: "Creative Arts", icon: Palette, color: "text-pink-600", bgColor: "bg-pink-50", description: "Drawing, music, and crafts" },
  { name: "Physical Education", icon: Target, color: "text-red-600", bgColor: "bg-red-50", description: "Health, fitness, and movement" },
]

export const primaryLearningDomains = [
  "Learn",
  "Practice",
  "Check",
  "Apply",
  "Create",
  "Reflect",
  "Grow",
]

const baseNav: Record<LearningLevel, NavItem[]> = {
  NURSERY: [
    { label: "My World", href: "/dashboard/nursery", icon: Home },
    { label: "Learning Journey", href: "/dashboard/nursery/learning-journey", icon: GraduationCap },
    { label: "Play & Learn", href: "/dashboard/nursery/play", icon: ClipboardList },
    { label: "Stories", href: "/dashboard/nursery/stories", icon: BookOpen },
    { label: "Discovery", href: "/dashboard/nursery/discovery", icon: BarChart3 },
    { label: "Create Studio", href: "/dashboard/nursery/create", icon: PenTool },
    { label: "Music & Movement", href: "/dashboard/nursery/music", icon: Activity },
    { label: "Speak & Listen", href: "/dashboard/nursery/speak-listen", icon: Users },
    { label: "My Teacher", href: "/dashboard/nursery/my-teacher", icon: Users },
    { label: "Daily Quest", href: "/dashboard/nursery/daily-quest", icon: Award },
    { label: "Feelings", href: "/dashboard/nursery/feelings", icon: ClipboardList },
    { label: "Movement Breaks", href: "/dashboard/nursery/movement", icon: Activity },
    { label: "Tanzania", href: "/dashboard/nursery/tanzania", icon: BookOpen },
    { label: "Evidence", href: "/dashboard/nursery/evidence", icon: Award },
    { label: "Portfolio", href: "/dashboard/nursery/portfolio", icon: FileText },
    { label: "Learn Together", href: "/dashboard/nursery/learn-together", icon: Users },
    { label: "Missions", href: "/dashboard/nursery/missions", icon: Award },
    { label: "Family Learning", href: "/dashboard/nursery/family-learning", icon: Users },
    { label: "My Progress", href: "/dashboard/nursery/progress", icon: BarChart3 },
    { label: "Backpack", href: "/dashboard/nursery/backpack", icon: Award },
    { label: "Live Learning", href: "/dashboard/nursery/live", icon: Video },
    { label: "Notifications", href: "/dashboard/nursery/notifications", icon: Bell },
    { label: "Profile", href: "/dashboard/nursery/profile", icon: User },
  ],
  PRIMARY: [
    { label: "My World", href: "/dashboard", icon: Home },
    { label: "Learn", href: "/dashboard/lessons", icon: BookOpen },
    { label: "Practice", href: "/dashboard/assignments", icon: ClipboardList },
    { label: "Discover", href: "/dashboard/assessments", icon: Compass },
    { label: "Live Learning", href: "/dashboard/live-classes", icon: Video },
    { label: "Progress", href: "/dashboard/progress", icon: BarChart3 },
    { label: "Notifications", href: "/dashboard/notifications", icon: Bell },
    { label: "Profile", href: "/dashboard/profile", icon: User },
  ],
  SECONDARY: [
    { label: "Home", href: "/dashboard", icon: Home },
    { label: "Subjects", href: "/dashboard/lessons", icon: BookOpen },
    { label: "Assignments", href: "/dashboard/assignments", icon: FileText },
    { label: "Assessments", href: "/dashboard/assessments", icon: PenTool },
    { label: "Results", href: "/dashboard/certificates", icon: Award },
    { label: "Progress", href: "/dashboard/progress", icon: BarChart3 },
  ],
  COLLEGE: [
    { label: "Home", href: "/dashboard/learner", icon: Home },
    { label: "Competencies", href: "/dashboard/learner/competencies", icon: Award },
    { label: "Projects", href: "/dashboard/learner/projects", icon: FileText },
    { label: "Fieldwork", href: "/dashboard/learner/fieldwork", icon: Compass },
    { label: "Portfolio", href: "/dashboard/learner/portfolio", icon: Library },
    { label: "Show What I Can Do", href: "/dashboard/learner/demonstrations", icon: Star },
    { label: "My Evidence", href: "/dashboard/learner/evidence", icon: ClipboardList },
    { label: "Study Planner", href: "/dashboard/learner/study-planner", icon: Calendar },
    { label: "Live Campus", href: "/dashboard/learner/live-classes", icon: Video },
    { label: "Career World", href: "/dashboard/learner/career", icon: Compass },
  ],
  UNIVERSITY: [
    { label: "Home", href: "/dashboard/learner", icon: Home },
    { label: "Competencies", href: "/dashboard/learner/competencies", icon: Award },
    { label: "Projects", href: "/dashboard/learner/projects", icon: FileText },
    { label: "Research", href: "/dashboard/learner/research", icon: FlaskConical },
    { label: "Thesis", href: "/dashboard/learner/thesis", icon: BookMarked },
    { label: "Fieldwork", href: "/dashboard/learner/fieldwork", icon: Compass },
    { label: "Portfolio", href: "/dashboard/learner/portfolio", icon: Library },
    { label: "Show What I Can Do", href: "/dashboard/learner/demonstrations", icon: Star },
    { label: "My Evidence", href: "/dashboard/learner/evidence", icon: ClipboardList },
    { label: "Study Planner", href: "/dashboard/learner/study-planner", icon: Calendar },
    { label: "Calendar", href: "/dashboard/learner/calendar", icon: Calendar },
    { label: "Live Campus", href: "/dashboard/learner/live-classes", icon: Video },
    { label: "Career World", href: "/dashboard/learner/career", icon: Compass },
  ],
}

const dashboardConfigs: Record<LearningLevel, DashboardConfig> = {
  NURSERY: {
    greeting: "Welcome to your fun learning world!",
    subtitle: "Let's explore, play, and learn together today.",
    navItems: baseNav.NURSERY,
    sections: ["continue", "today", "activities", "progress", "recent"],
    emptyStateTitle: "Your adventure begins here!",
    emptyStateDescription: "Fun activities, stories, and games will appear here.",
    cardStyle: "colorful",
  },
  PRIMARY: {
    greeting: "Welcome back, Explorer!",
    subtitle: "What would you like to learn today?",
    navItems: baseNav.PRIMARY,
    sections: ["continue", "today", "subjects", "upcoming", "live", "progress", "feedback"],
    emptyStateTitle: "Your learning journey starts here",
    emptyStateDescription: "Your subjects and lessons will appear here once your teacher sets them up.",
    cardStyle: "colorful",
    subjects: primarySubjects,
    learningDomains: primaryLearningDomains,
  },
  SECONDARY: {
    greeting: "Stay focused on your goals.",
    subtitle: "Keep up with your assignments and upcoming assessments.",
    navItems: [
      { label: "My Academic World", href: "/dashboard/secondary", icon: Home },
      { label: "Learn", href: "/dashboard/secondary/learn", icon: BookOpen },
      { label: "Practice", href: "/dashboard/secondary/practice", icon: ClipboardList },
      { label: "Assess", href: "/dashboard/secondary/assess", icon: Award },
      { label: "Revision", href: "/dashboard/secondary/revision", icon: BarChart3 },
      { label: "Live", href: "/dashboard/secondary/live", icon: Video },
      { label: "Projects", href: "/dashboard/secondary/projects", icon: FileText },
      { label: "My Teachers", href: "/dashboard/secondary/teachers", icon: Users },
      { label: "Notifications", href: "/dashboard/secondary/notifications", icon: Bell },
      { label: "Progress", href: "/dashboard/secondary/progress", icon: BarChart3 },
      { label: "Future World", href: "/dashboard/secondary/future", icon: GraduationCap },
    ],
    sections: ["continue", "assessments", "assignments", "subjects", "performance", "recent", "notifications"],
    emptyStateTitle: "No items yet",
    emptyStateDescription: "Your assignments, assessments, and results will appear here.",
    cardStyle: "academic",
  },
  COLLEGE: {
    greeting: "Manage your academic workload.",
    subtitle: "Track your courses, assignments, and progress.",
    navItems: baseNav.COLLEGE,
    sections: ["continue", "courses", "assessments", "assignments", "progress", "recent", "resources"],
    emptyStateTitle: "No courses enrolled",
    emptyStateDescription: "Your enrolled courses and academic progress will appear here.",
    cardStyle: "professional",
  },
  UNIVERSITY: {
    greeting: "Advance your academic career.",
    subtitle: "Stay on top of your courses and research.",
    navItems: baseNav.UNIVERSITY,
    sections: ["continue", "courses", "assessments", "assignments", "progress", "recent", "calendar", "resources"],
    emptyStateTitle: "No courses enrolled",
    emptyStateDescription: "Your enrolled courses, assignments, and academic progress will appear here.",
    cardStyle: "professional",
  },
}

export function getLearnerNavItems(role?: string, level?: string | null): NavItem[] {
  if (role === "Other Learner") {
    return [
      { label: "Dashboard", href: "/dashboard/learner", icon: Home },
      { label: "Explore Courses", href: "/dashboard/learner/courses", icon: BookOpen },
      { label: "My Learning", href: "/dashboard/learner/my-learning", icon: GraduationCap },
      { label: "Resources", href: "/dashboard/learner/resources", icon: Library },
      { label: "Bookmarks", href: "/dashboard/learner/bookmarks", icon: Bookmark },
      { label: "History", href: "/dashboard/learner/history", icon: Clock },
      { label: "Certificates", href: "/dashboard/learner/certificates", icon: Award },
      { label: "Profile", href: "/dashboard/learner/profile", icon: Users },
    ]
  }
  const key = (level?.toUpperCase() || "SECONDARY") as LearningLevel
  return dashboardConfigs[key]?.navItems || dashboardConfigs.SECONDARY.navItems
}

const otherLearnerConfig: DashboardConfig = {
  greeting: "Welcome back!",
  subtitle: "Continue exploring and expanding your knowledge.",
  navItems: [
    { label: "Dashboard", href: "/dashboard/learner", icon: Home },
    { label: "Explore Courses", href: "/dashboard/learner/courses", icon: BookOpen },
    { label: "My Learning", href: "/dashboard/learner/my-learning", icon: GraduationCap },
    { label: "Resources", href: "/dashboard/learner/resources", icon: Library },
    { label: "Bookmarks", href: "/dashboard/learner/bookmarks", icon: Bookmark },
    { label: "History", href: "/dashboard/learner/history", icon: Clock },
    { label: "Certificates", href: "/dashboard/learner/certificates", icon: Award },
    { label: "Profile", href: "/dashboard/learner/profile", icon: Users },
  ],
  sections: ["continue", "enrolled", "recommended", "recent"],
  emptyStateTitle: "Start your learning journey",
  emptyStateDescription: "Explore courses and begin learning at your own pace.",
  cardStyle: "professional",
}

export function getDashboardConfig(level?: string | null, role?: string): DashboardConfig {
  if (role === "Other Learner") return otherLearnerConfig
  const key = (level?.toUpperCase() || "SECONDARY") as LearningLevel
  return dashboardConfigs[key] || dashboardConfigs.SECONDARY
}

export function getLevelLabel(level?: string | null): string {
  const labels: Record<string, string> = {
    NURSERY: "Nursery",
    PRIMARY: "Primary",
    SECONDARY: "Secondary",
    COLLEGE: "College",
    UNIVERSITY: "University",
  }
  return labels[level?.toUpperCase() || ""] || "Student"
}
