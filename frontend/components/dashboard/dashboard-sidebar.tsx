"use client"

import { useState, useEffect, useCallback } from "react"
import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { LayoutDashboard, BookOpen, Video, FileText, BarChart3, MessageSquare, Award, Bookmark, User, Settings, LogOut, ClipboardList, GraduationCap, PenTool, School, Users, Shield, ShieldCheck, ClipboardCheck, Calendar, CalendarDays, Bell, Clock, TrendingUp, Library, HeartPulse, FileBarChart, Trophy, Target, Activity, Film, Compass, Backpack, Map, Lightbulb, FlaskConical, Mic, Swords, Zap, AlertCircle, Home, Palette, Globe, Eye, Radio, Search, Brain, ChevronLeft, ChevronRight, Play } from "lucide-react"
import { useTranslations } from "next-intl"
import { Logo } from "@/components/logo"
import { cn } from "@/lib/utils"
import { useAuth } from "@/lib/auth"

const nurseryNav: Array<{ label: string; labelKey?: string; href: string; icon: typeof LayoutDashboard }> = [
  { label: "My World", labelKey: "myWorld", href: "/dashboard/nursery", icon: Home },
  { label: "Learning Journey", labelKey: "learningJourney", href: "/dashboard/nursery/learning-journey", icon: GraduationCap },
  { label: "Play & Learn", labelKey: "playLearn", href: "/dashboard/nursery/play", icon: Activity },
  { label: "Stories", labelKey: "stories", href: "/dashboard/nursery/stories", icon: BookOpen },
  { label: "Discovery", labelKey: "discovery", href: "/dashboard/nursery/discovery", icon: TrendingUp },
  { label: "Create Studio", labelKey: "createStudio", href: "/dashboard/nursery/create", icon: PenTool },
  { label: "Music & Movement", labelKey: "musicMovement", href: "/dashboard/nursery/music", icon: Activity },
  { label: "Speak & Listen", labelKey: "speakListen", href: "/dashboard/nursery/speak-listen", icon: MessageSquare },
  { label: "My Teacher", labelKey: "myTeacher", href: "/dashboard/nursery/my-teacher", icon: User },
  { label: "Daily Quest", labelKey: "dailyQuest", href: "/dashboard/nursery/daily-quest", icon: Trophy },
  { label: "Milestones", labelKey: "milestones", href: "/dashboard/nursery/milestones", icon: Award },
  { label: "Feelings", labelKey: "feelings", href: "/dashboard/nursery/feelings", icon: HeartPulse },
  { label: "Movement Breaks", labelKey: "movementBreaks", href: "/dashboard/nursery/movement", icon: Activity },
  { label: "Tanzania", labelKey: "tanzania", href: "/dashboard/nursery/tanzania", icon: Globe },
  { label: "Evidence", labelKey: "evidence", href: "/dashboard/nursery/evidence", icon: Award },
  { label: "Portfolio", labelKey: "portfolio", href: "/dashboard/nursery/portfolio", icon: FileText },
  { label: "Learn Together", labelKey: "learnTogether", href: "/dashboard/nursery/learn-together", icon: Users },
  { label: "Missions", labelKey: "missions", href: "/dashboard/nursery/missions", icon: Target },
  { label: "Family Learning", labelKey: "familyLearning", href: "/dashboard/nursery/family-learning", icon: Users },
  { label: "My Progress", labelKey: "myProgress", href: "/dashboard/nursery/progress", icon: BarChart3 },
  { label: "Backpack", labelKey: "backpack", href: "/dashboard/nursery/backpack", icon: Award },
  { label: "Live Class", labelKey: "liveClass", href: "/dashboard/nursery/live", icon: Video },
  { label: "Notifications", labelKey: "notifications", href: "/dashboard/nursery/notifications", icon: Bell },
  { label: "Profile", labelKey: "profile", href: "/dashboard/nursery/profile", icon: User },
]

type PrimaryNavSection = {
  group: string; groupKey?: string;
  items: Array<{ label: string; labelKey?: string; href: string; icon: typeof LayoutDashboard; dotColor?: string }>
}

const primaryNavSections: PrimaryNavSection[] = [
  {
    group: "HOME", groupKey: "groupHome",
    items: [
      { label: "My World", labelKey: "myWorld", href: "/dashboard", icon: LayoutDashboard, dotColor: "bg-blue-500" },
    ],
  },
  {
    group: "LEARNING", groupKey: "groupLearning",
    items: [
      { label: "Learn", labelKey: "learn", href: "/dashboard/lessons", icon: BookOpen, dotColor: "bg-teal-500" },
      { label: "Journey", labelKey: "journey", href: "/dashboard/journey", icon: Map, dotColor: "bg-indigo-500" },
      { label: "Read", labelKey: "read", href: "/dashboard/reading", icon: BookOpen, dotColor: "bg-violet-500" },
    ],
  },
  {
    group: "PRACTICE ZONE", groupKey: "groupPracticeZone",
    items: [
      { label: "Practice", labelKey: "practice", href: "/dashboard/assignments", icon: ClipboardList, dotColor: "bg-yellow-500" },
      { label: "Create", labelKey: "create", href: "/dashboard/create", icon: Palette, dotColor: "bg-pink-500" },
      { label: "Assessments", labelKey: "assessments", href: "/dashboard/assessments", icon: PenTool, dotColor: "bg-rose-500" },
      { label: "Discovery", labelKey: "discovery", href: "/dashboard/discovery", icon: Lightbulb, dotColor: "bg-amber-500" },
      { label: "Labs", labelKey: "labs", href: "/dashboard/labs", icon: FlaskConical, dotColor: "bg-emerald-500" },
    ],
  },
  {
    group: "CHALLENGES", groupKey: "groupChallenges",
    items: [
      { label: "Quests", labelKey: "quests", href: "/dashboard/quests", icon: Swords, dotColor: "bg-red-500" },
      { label: "Challenge Zone", labelKey: "challengeZone", href: "/dashboard/challenge-zone", icon: Zap, dotColor: "bg-orange-500" },
      { label: "Mistake Lab", labelKey: "mistakeLab", href: "/dashboard/mistake-lab", icon: AlertCircle, dotColor: "bg-amber-600" },
      { label: "AI Guide", labelKey: "aiGuide", href: "/dashboard/ai-guide", icon: Target, dotColor: "bg-purple-500" },
    ],
  },
  {
    group: "COLLABORATION", groupKey: "groupCollaboration",
    items: [
      { label: "Learn Together", labelKey: "learnTogether", href: "/dashboard/learn-together", icon: Users, dotColor: "bg-cyan-500" },
      { label: "Family", labelKey: "family", href: "/dashboard/family", icon: Home, dotColor: "bg-teal-600" },
    ],
  },
  {
    group: "TRACKING", groupKey: "groupTracking",
    items: [
      { label: "Live Learning", labelKey: "liveLearning", href: "/dashboard/live-classes", icon: Video, dotColor: "bg-red-500" },
      { label: "Progress", labelKey: "progress", href: "/dashboard/progress", icon: BarChart3, dotColor: "bg-cyan-500" },
      { label: "Evidence", labelKey: "evidence", href: "/dashboard/evidence", icon: Award, dotColor: "bg-green-500" },
      { label: "Passport", labelKey: "passport", href: "/dashboard/passport", icon: Map, dotColor: "bg-amber-500" },
      { label: "Attendance", labelKey: "attendance", href: "/dashboard/attendance", icon: ClipboardCheck, dotColor: "bg-emerald-600" },
    ],
  },
  {
    group: "ACCOUNT", groupKey: "groupAccount",
    items: [
      { label: "My Teacher", labelKey: "myTeacher", href: "/dashboard/my-teachers", icon: GraduationCap, dotColor: "bg-purple-500" },
      { label: "Messages", labelKey: "messages", href: "/dashboard/messages", icon: MessageSquare, dotColor: "bg-blue-600" },
      { label: "Notifications", labelKey: "notifications", href: "/dashboard/notifications", icon: Bell, dotColor: "bg-orange-500" },
      { label: "Portfolio", labelKey: "portfolio", href: "/dashboard/portfolio", icon: Backpack, dotColor: "bg-violet-500" },
      { label: "Learning Profile", labelKey: "learningProfile", href: "/dashboard/learning-profile", icon: User, dotColor: "bg-indigo-500" },
      { label: "Profile", labelKey: "profile", href: "/dashboard/profile", icon: User, dotColor: "bg-slate-500" },
      { label: "Settings", labelKey: "settings", href: "/dashboard/settings", icon: Settings, dotColor: "bg-gray-500" },
    ],
  },
]

const secondaryNav: Array<{ label: string; labelKey?: string; href: string; icon: typeof LayoutDashboard }> = [
  { label: "My Academic World", labelKey: "myAcademicWorld", href: "/dashboard/secondary", icon: LayoutDashboard },
  { label: "Learn", labelKey: "learn", href: "/dashboard/secondary/learn", icon: BookOpen },
  { label: "Practice", labelKey: "practice", href: "/dashboard/secondary/practice", icon: PenTool },
  { label: "Assess", labelKey: "assess", href: "/dashboard/secondary/assess", icon: Award },
  { label: "Revision", labelKey: "revision", href: "/dashboard/secondary/revision", icon: TrendingUp },
  { label: "Live", labelKey: "live", href: "/dashboard/secondary/live", icon: Video },
  { label: "Projects", labelKey: "projects", href: "/dashboard/secondary/projects", icon: FileText },
  { label: "Research", labelKey: "research", href: "/dashboard/secondary/learn/research", icon: BookOpen },
  { label: "Science Lab", labelKey: "scienceLab", href: "/dashboard/secondary/learn/science-lab", icon: Library },
  { label: "Portfolio", labelKey: "portfolio", href: "/dashboard/secondary/portfolio", icon: FileBarChart },
  { label: "Progress", labelKey: "progress", href: "/dashboard/secondary/progress", icon: BarChart3 },
  { label: "Future World", labelKey: "futureWorld", href: "/dashboard/secondary/future", icon: Target },
  { label: "My Teachers", labelKey: "myTeachers", href: "/dashboard/secondary/teachers", icon: Users },
  { label: "Events", labelKey: "events", href: "/dashboard/learner/events", icon: Calendar },
  { label: "Notifications", labelKey: "notifications", href: "/dashboard/secondary/notifications", icon: Bell },
]

type CollegeNavSection = {
  group: string; groupKey?: string;
  items: Array<{ label: string; labelKey?: string; href: string; icon: typeof LayoutDashboard; dotColor?: string; badge?: number }>
}

const collegeNavSections: CollegeNavSection[] = [
  {
    group: "OVERVIEW", groupKey: "groupOverview",
    items: [
      { label: "Dashboard", labelKey: "dashboard", href: "/dashboard/learner", icon: LayoutDashboard, dotColor: "bg-blue-500" },
    ],
  },
  {
    group: "MY COLLEGE", groupKey: "groupMyCollege",
    items: [
      { label: "My Learning", labelKey: "myLearning", href: "/dashboard/learner/my-learning", icon: BookOpen, dotColor: "bg-teal-500" },
      { label: "Module Workspace", labelKey: "moduleWorkspace", href: "/dashboard/learner/module-workspace", icon: BookOpen, dotColor: "bg-indigo-500" },
      { label: "Competencies", labelKey: "competencies", href: "/dashboard/learner/competencies", icon: Target, dotColor: "bg-violet-500" },
    ],
  },
  {
    group: "LIVE & MEDIA", groupKey: "groupLiveMedia",
    items: [
      { label: "Live Campus", labelKey: "liveCampus", href: "/dashboard/learner/live-campus", icon: Radio, dotColor: "bg-red-500" },
      { label: "Media Library", labelKey: "mediaLibrary", href: "/dashboard/learner/media-library", icon: Film, dotColor: "bg-pink-500" },
      { label: "Resources", labelKey: "resources", href: "/dashboard/learner/resources", icon: Library, dotColor: "bg-amber-500" },
      { label: "Events", labelKey: "events", href: "/dashboard/learner/events", icon: Calendar, dotColor: "bg-purple-500" },
    ],
  },
  {
    group: "BUILD & DISCOVER", groupKey: "groupBuildDiscover",
    items: [
      { label: "Projects", labelKey: "projects", href: "/dashboard/learner/projects", icon: Activity, dotColor: "bg-emerald-500" },
      { label: "Collaborations", labelKey: "collaborations", href: "/dashboard/learner/collaborations", icon: Users, dotColor: "bg-blue-600" },
    ],
  },
  {
    group: "PRACTICAL", groupKey: "groupPractical",
    items: [
      { label: "Practical Lab", labelKey: "practicalLab", href: "/dashboard/learner/practical-lab", icon: FlaskConical, dotColor: "bg-green-500" },
      { label: "Workshops & Labs", labelKey: "workshopsLabs", href: "/dashboard/learner/workshops", icon: FlaskConical, dotColor: "bg-teal-600" },
      { label: "Software & Tools", labelKey: "softwareTools", href: "/dashboard/learner/software-tools", icon: Zap, dotColor: "bg-yellow-500" },
      { label: "Assessments", labelKey: "assessments", href: "/dashboard/learner/assessments", icon: ClipboardList, dotColor: "bg-rose-500" },
      { label: "Fieldwork", labelKey: "fieldwork", href: "/dashboard/learner/fieldwork", icon: Bookmark, dotColor: "bg-teal-500" },
    ],
  },
  {
    group: "ACADEMICS", groupKey: "groupAcademics",
    items: [
      { label: "Academic Progress", labelKey: "academicProgress", href: "/dashboard/learner/academic-progress", icon: BarChart3, dotColor: "bg-blue-500" },
      { label: "Academic Record", labelKey: "academicRecord", href: "/dashboard/learner/academic-record", icon: FileBarChart, dotColor: "bg-indigo-500" },
      { label: "Study Planner", labelKey: "studyPlanner", href: "/dashboard/learner/study-planner", icon: Clock, dotColor: "bg-violet-500" },
    ],
  },
  {
    group: "MY WORLD", groupKey: "groupMyWorld",
    items: [
      { label: "Portfolio", labelKey: "portfolio", href: "/dashboard/learner/portfolio", icon: Award, dotColor: "bg-amber-500" },
      { label: "My Evidence", labelKey: "myEvidence", href: "/dashboard/learner/evidence", icon: ClipboardList, dotColor: "bg-green-500" },
      { label: "Show What I Can Do", labelKey: "showWhatICanDo", href: "/dashboard/learner/demonstrations", icon: Trophy, dotColor: "bg-orange-500" },
      { label: "Professional Dev", labelKey: "professionalDev", href: "/dashboard/learner/professional-dev", icon: TrendingUp, dotColor: "bg-blue-600" },
      { label: "Career World", labelKey: "careerWorld", href: "/dashboard/learner/career", icon: Target, dotColor: "bg-indigo-600" },
    ],
  },
  {
    group: "CONNECT", groupKey: "groupConnect",
    items: [
      { label: "Academic Search", labelKey: "academicSearch", href: "/dashboard/learner/search", icon: Search, dotColor: "bg-slate-500" },
      { label: "Messages", labelKey: "messages", href: "/dashboard/messages", icon: MessageSquare, dotColor: "bg-blue-500" },
      { label: "Notifications", labelKey: "notifications", href: "/dashboard/learner/notifications-center", icon: Bell, dotColor: "bg-orange-500" },
    ],
  },
  {
    group: "ACCOUNT", groupKey: "groupAccount",
    items: [
      { label: "My Learning Kit", labelKey: "myLearningKit", href: "/dashboard/learner/my-learning-kit", icon: Backpack, dotColor: "bg-amber-500" },
      { label: "Profile", labelKey: "profile", href: "/dashboard/profile", icon: User, dotColor: "bg-slate-500" },
      { label: "Settings", labelKey: "settings", href: "/dashboard/settings", icon: Settings, dotColor: "bg-gray-500" },
    ],
  },
]

interface NavSection {
  label: string
  labelKey?: string
  icon: typeof LayoutDashboard
  children?: Array<{ label: string; labelKey?: string; href: string; icon: typeof LayoutDashboard }>
  href?: string
  dotColor?: string
}

const universityNavSections: NavSection[] = [
  { label: "Dashboard", labelKey: "dashboard", href: "/dashboard/learner", icon: LayoutDashboard, dotColor: "bg-blue-500" },
  {
    label: "My Learning", labelKey: "myLearning",
    icon: BookOpen,
    dotColor: "bg-teal-500",
    children: [
      { label: "My Courses", labelKey: "myCourses", href: "/dashboard/learner/courses", icon: GraduationCap },
      { label: "Continue Learning", labelKey: "continueLearning", href: "/dashboard/learner/my-learning", icon: Play },
      { label: "Resources", labelKey: "resources", href: "/dashboard/learner/resources", icon: Library },
      { label: "Practical Lab", labelKey: "practicalLab", href: "/dashboard/learner/practical-lab", icon: FlaskConical },
      { label: "Assessments", labelKey: "assessments", href: "/dashboard/learner/assessments", icon: ClipboardList },
    ],
  },
  {
    label: "Live Campus", labelKey: "liveCampus",
    icon: Radio,
    dotColor: "bg-red-500",
    children: [
      { label: "Live Now", labelKey: "liveNow", href: "/dashboard/learner/live-campus", icon: Radio },
      { label: "Live Classes", labelKey: "liveClasses", href: "/dashboard/learner/live-classes", icon: Video },
      { label: "Media Library", labelKey: "mediaLibrary", href: "/dashboard/learner/media-library", icon: Film },
      { label: "Workshops & Labs", labelKey: "workshopsLabs", href: "/dashboard/learner/workshops", icon: FlaskConical },
    ],
  },
  {
    label: "Build & Discover", labelKey: "buildDiscover",
    icon: Activity,
    dotColor: "bg-emerald-500",
    children: [
      { label: "Projects", labelKey: "projects", href: "/dashboard/learner/projects", icon: Activity },
      { label: "Research", labelKey: "research", href: "/dashboard/learner/research", icon: Target },
      { label: "Thesis", labelKey: "thesis", href: "/dashboard/learner/thesis", icon: FileText },
      { label: "Software & Tools", labelKey: "softwareTools", href: "/dashboard/learner/software-tools", icon: Zap },
      { label: "Collaborations", labelKey: "collaborations", href: "/dashboard/learner/collaborations", icon: Users },
    ],
  },
  {
    label: "Academics", labelKey: "academics",
    icon: BarChart3,
    dotColor: "bg-blue-500",
    children: [
      { label: "Academic Progress", labelKey: "academicProgress", href: "/dashboard/learner/academic-progress", icon: BarChart3 },
      { label: "Academic Record", labelKey: "academicRecord", href: "/dashboard/learner/academic-record", icon: FileBarChart },
      { label: "Study Planner", labelKey: "studyPlanner", href: "/dashboard/learner/study-planner", icon: Clock },
      { label: "Calendar", labelKey: "calendar", href: "/dashboard/learner/calendar", icon: CalendarDays },
      { label: "Competencies", labelKey: "competencies", href: "/dashboard/learner/competencies", icon: Target },
      { label: "Events", labelKey: "events", href: "/dashboard/learner/events", icon: CalendarDays },
    ],
  },
  {
    label: "My World", labelKey: "myWorld",
    icon: Award,
    dotColor: "bg-amber-500",
    children: [
      { label: "Portfolio", labelKey: "portfolio", href: "/dashboard/learner/portfolio", icon: Award },
      { label: "My Evidence", labelKey: "myEvidence", href: "/dashboard/learner/evidence", icon: ClipboardList },
      { label: "Demonstrations", labelKey: "demonstrations", href: "/dashboard/learner/demonstrations", icon: Trophy },
      { label: "Fieldwork", labelKey: "fieldwork", href: "/dashboard/learner/fieldwork", icon: Bookmark },
      { label: "Professional Dev", labelKey: "professionalDev", href: "/dashboard/learner/professional-dev", icon: TrendingUp },
      { label: "Career World", labelKey: "careerWorld", href: "/dashboard/learner/career", icon: Target },
      { label: "Certificates", labelKey: "certificates", href: "/dashboard/learner/certificates", icon: Award },
    ],
  },
  {
    label: "Connect", labelKey: "connect",
    icon: Search,
    dotColor: "bg-slate-500",
    children: [
      { label: "Academic Search", labelKey: "academicSearch", href: "/dashboard/learner/search", icon: Search },
      { label: "Knowledge Discovery", labelKey: "knowledgeDiscovery", href: "/dashboard/learner/knowledge-discovery", icon: Brain },
      { label: "Messages", labelKey: "messages", href: "/dashboard/messages", icon: MessageSquare },
      { label: "Notifications", labelKey: "notifications", href: "/dashboard/learner/notifications-center", icon: Bell },
    ],
  },
  { label: "Deep Learning", labelKey: "deepLearning", href: "/dashboard/learner/deep-learning", icon: Brain, dotColor: "bg-purple-500" },
  { label: "Course Workspace", labelKey: "courseWorkspace", href: "/dashboard/learner/course-workspace", icon: BookOpen, dotColor: "bg-violet-500" },
  { label: "My Learning Kit", labelKey: "myLearningKit", href: "/dashboard/learner/my-learning-kit", icon: Library, dotColor: "bg-amber-500" },
  { label: "Academic Assistant", labelKey: "academicAssistant", href: "/dashboard/learner/academic-assistant", icon: Brain, dotColor: "bg-violet-500" },
]

type LearnerNavSection = {
  group: string; groupKey?: string;
  items: Array<{ label: string; labelKey?: string; href: string; icon: typeof LayoutDashboard; dotColor?: string }>
}

const learnerNavSections: LearnerNavSection[] = [
  {
    group: "OVERVIEW", groupKey: "groupOverview",
    items: [
      { label: "My Learning World", labelKey: "myLearningWorld", href: "/dashboard/learner", icon: LayoutDashboard },
    ],
  },
  {
    group: "LEARNING", groupKey: "groupLearning",
    items: [
      { label: "My Learning", labelKey: "myLearning", href: "/dashboard/learner/my-learning", icon: GraduationCap },
      { label: "Resources", labelKey: "resources", href: "/dashboard/learner/resources", icon: Library },
      { label: "Video Library", labelKey: "videoLibrary", href: "/dashboard/learner/video-library", icon: Film },
      { label: "History", labelKey: "history", href: "/dashboard/learner/history", icon: Clock },
    ],
  },
  {
    group: "DISCOVER", groupKey: "groupDiscover",
    items: [
      { label: "Explore Courses", labelKey: "exploreCourses", href: "/dashboard/learner/courses", icon: BookOpen },
      { label: "Live Classes", labelKey: "liveClasses", href: "/dashboard/learner/live-classes", icon: Video },
      { label: "Events & Workshops", labelKey: "eventsWorkshops", href: "/dashboard/learner/events", icon: Calendar },
      { label: "My Registrations", labelKey: "myRegistrations", href: "/dashboard/learner/events/registered", icon: ClipboardList },
      { label: "Replays", labelKey: "replays", href: "/dashboard/learner/replays", icon: Film },
    ],
  },
  {
    group: "KNOWLEDGE", groupKey: "groupKnowledge",
    items: [
      { label: "Search", labelKey: "search", href: "/dashboard/learner/search", icon: Search },
      { label: "Knowledge Discovery", labelKey: "knowledgeDiscovery", href: "/dashboard/learner/knowledge-discovery", icon: Brain },
    ],
  },
  {
    group: "MY PROGRESS", groupKey: "groupMyProgress",
    items: [
      { label: "Progress", labelKey: "progress", href: "/dashboard/learner/progress", icon: BarChart3 },
      { label: "Assessments", labelKey: "assessments", href: "/dashboard/learner/assessments", icon: ClipboardCheck },
      { label: "Learning Paths", labelKey: "learningPaths", href: "/dashboard/learner/learning-paths", icon: Map },
      { label: "Goals", labelKey: "goals", href: "/dashboard/learner/goals", icon: Target },
      { label: "Bookmarks", labelKey: "bookmarks", href: "/dashboard/learner/bookmarks", icon: Bookmark },
      { label: "Certificates", labelKey: "certificates", href: "/dashboard/learner/certificates", icon: Award },
      { label: "Announcements", labelKey: "announcements", href: "/dashboard/learner/announcements", icon: MessageSquare },
    ],
  },
  {
    group: "ACCOUNT", groupKey: "groupAccount",
    items: [
      { label: "Notifications", labelKey: "notifications", href: "/dashboard/learner/notifications-center", icon: Bell },
      { label: "Profile", labelKey: "profile", href: "/dashboard/learner/profile", icon: User },
      { label: "Settings", labelKey: "settings", href: "/dashboard/learner/settings", icon: Settings },
    ],
  },
]

type TeacherNavSection = {
  group: string; groupKey?: string;
  items: Array<{ label: string; labelKey?: string; href: string; icon: typeof LayoutDashboard; badge?: number }>
}

const teacherNavSections: TeacherNavSection[] = [
  {
    group: "OVERVIEW", groupKey: "groupOverview",
    items: [{ label: "Dashboard", labelKey: "dashboard", href: "/dashboard/teacher", icon: LayoutDashboard }],
  },
  {
    group: "WORKSPACE", groupKey: "groupWorkspace",
    items: [
      { label: "My Classes", labelKey: "myClasses", href: "/dashboard/teacher/classes", icon: BookOpen },
      { label: "Courses", labelKey: "courses", href: "/dashboard/teacher/courses", icon: GraduationCap },
      { label: "Students", labelKey: "students", href: "/dashboard/teacher/students", icon: Users },
      { label: "Learner Support", labelKey: "learnerSupport", href: "/dashboard/teacher/learner-support", icon: HeartPulse },
    ],
  },
  {
    group: "TEACHING", groupKey: "groupTeaching",
    items: [
      { label: "Lessons", labelKey: "lessons", href: "/dashboard/teacher/lessons", icon: BookOpen },
      { label: "Assignments", labelKey: "assignments", href: "/dashboard/teacher/assignments", icon: FileText },
      { label: "Assessments", labelKey: "assessments", href: "/dashboard/teacher/assessments", icon: PenTool },
      { label: "Grading", labelKey: "grading", href: "/dashboard/teacher/grading", icon: Award },
    ],
  },
  {
    group: "CLASS MANAGEMENT", groupKey: "groupClassManagement",
    items: [
      { label: "Attendance", labelKey: "attendance", href: "/dashboard/teacher/attendance", icon: ClipboardCheck },
      { label: "Gradebook", labelKey: "gradebook", href: "/dashboard/teacher/gradebook", icon: BarChart3 },
      { label: "Schedule", labelKey: "schedule", href: "/dashboard/teacher/schedule", icon: Calendar },
    ],
  },
  {
    group: "COMMUNICATION", groupKey: "groupCommunication",
    items: [
      { label: "Live Classes", labelKey: "liveClasses", href: "/dashboard/teacher/live-classes", icon: Video },
      { label: "Media Library", labelKey: "mediaLibrary", href: "/dashboard/teacher/media-library", icon: Film },
      { label: "Messages", labelKey: "messages", href: "/dashboard/teacher/messages", icon: MessageSquare },
      { label: "Announcements", labelKey: "announcements", href: "/dashboard/teacher/announcements", icon: Bell },
      { label: "Notifications", labelKey: "notifications", href: "/dashboard/teacher/notifications", icon: Bell },
    ],
  },
  {
    group: "INSIGHTS", groupKey: "groupInsights",
    items: [
      { label: "Analytics", labelKey: "analytics", href: "/dashboard/teacher/analytics", icon: TrendingUp },
      { label: "Reports", labelKey: "reports", href: "/dashboard/teacher/reports", icon: FileBarChart },
    ],
  },
  {
    group: "ACCOUNT", groupKey: "groupAccount",
    items: [{ label: "Settings", labelKey: "settings", href: "/dashboard/teacher/settings", icon: Settings }],
  },
]

const parentNav: Array<{ label: string; labelKey?: string; href: string; icon: typeof LayoutDashboard; badge?: number }> = [
  { label: "Dashboard", labelKey: "dashboard", href: "/dashboard/parent", icon: LayoutDashboard },
  { label: "Learning", labelKey: "learning", href: "/dashboard/parent/learning", icon: BookOpen },
  { label: "Progress", labelKey: "progress", href: "/dashboard/parent/reports", icon: BarChart3 },
  { label: "Assessments", labelKey: "assessments", href: "/dashboard/parent/assessments", icon: PenTool },
  { label: "Activity", labelKey: "activity", href: "/dashboard/parent/activity", icon: Clock },
  { label: "Attendance", labelKey: "attendance", href: "/dashboard/parent/attendance", icon: ClipboardList },
  { label: "Assignments", labelKey: "assignments", href: "/dashboard/parent/assignments", icon: FileText },
  { label: "Results", labelKey: "results", href: "/dashboard/parent/results", icon: Award },
  { label: "Achievements", labelKey: "achievements", href: "/dashboard/parent/achievements", icon: Trophy },
  { label: "Goals", labelKey: "goals", href: "/dashboard/parent/goals", icon: Target },
  { label: "Teachers", labelKey: "teachers", href: "/dashboard/parent/teachers", icon: Users },
  { label: "Calendar", labelKey: "calendar", href: "/dashboard/parent/calendar", icon: Calendar },
  { label: "Live Classes", labelKey: "liveClasses", href: "/dashboard/parent/live-classes", icon: Video },
  { label: "Library", labelKey: "library", href: "/dashboard/parent/library", icon: Library },
  { label: "Payments", labelKey: "payments", href: "/dashboard/parent/payments", icon: FileText },
  { label: "Services", labelKey: "services", href: "/dashboard/parent/services", icon: School },
  { label: "Support", labelKey: "support", href: "/dashboard/parent/support", icon: HeartPulse },
  { label: "Notifications", labelKey: "notifications", href: "/dashboard/parent/notifications", icon: Bell },
  { label: "Settings", labelKey: "settings", href: "/dashboard/parent/settings", icon: Settings },
]

const adminNav: Array<{ label: string; labelKey?: string; href: string; icon: typeof LayoutDashboard; badge?: number }> = [
  { label: "Platform Admin", labelKey: "platformAdmin", href: "/dashboard/platform-admin", icon: ShieldCheck },
  { label: "Administration", labelKey: "administration", href: "/dashboard/admin", icon: ShieldCheck },
  { label: "People", labelKey: "people", href: "/dashboard/admin/people", icon: Users },
  { label: "Organization", labelKey: "organization", href: "/dashboard/admin/profile", icon: School },
  { label: "Institutions", labelKey: "institutions", href: "/dashboard/admin/institutions", icon: School },
  { label: "Programmes", labelKey: "programmes", href: "/dashboard/admin/programmes", icon: GraduationCap },
  { label: "Departments", labelKey: "departments", href: "/dashboard/admin/departments", icon: Users },
  { label: "Competencies", labelKey: "competencies", href: "/dashboard/admin/competencies", icon: Target },
  { label: "Roles", labelKey: "roles", href: "/dashboard/admin/roles", icon: Shield },
  { label: "Audit Log", labelKey: "auditLog", href: "/dashboard/admin/audit", icon: Eye },
  { label: "Events", labelKey: "events", href: "/dashboard/admin/events", icon: Calendar },
  { label: "Data Import", labelKey: "dataImport", href: "/dashboard/admin/import", icon: FileText },
  { label: "Settings", labelKey: "settings", href: "/dashboard/admin/settings", icon: Settings },
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
  const t = useTranslations("nav")
  const tc = useTranslations("common")

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
  const [expandedSections, setExpandedSections] = useState<Record<string, boolean>>({})
  const [badges, setBadges] = useState<Record<string, number>>({})

  const fetchBadges = useCallback(async () => {
    if (!user?.id) return
    const newBadges: Record<string, number> = {}
    try {
      const token = localStorage.getItem("elmkusoma_access_token") || ""
      const headers = { Authorization: `Bearer ${token}` }
      const [countRes, liveRes, assessRes] = await Promise.allSettled([
        fetch(`/api/v1/notifications/unread-count`, { headers }),
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

  function renderNavItems(items: Array<{ label: string; labelKey?: string; href: string; icon: typeof LayoutDashboard; badge?: number; dotColor?: string }>) {
    return items.map((item) => {
      const active = pathname === item.href || (item.href !== "/dashboard" && item.href !== "/dashboard/teacher" && pathname.startsWith(item.href))
      const realBadge = badges[item.href] || item.badge || 0
      const isLive = item.href === "/dashboard/learner/live-campus" || item.href === "/dashboard/learner/live-classes" || (badges[item.href] || 0) > 0
      return (
        <Link
          key={item.href}
          href={item.href}
          onClick={onNavigate}
          title={collapsed ? (item.labelKey ? t(item.labelKey) : item.label) : undefined}
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
          {!collapsed && <span className="flex-1">{item.labelKey ? t(item.labelKey) : item.label}</span>}
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
          {section.groupKey ? t(section.groupKey) : section.group}
        </p>
        {renderNavItems(section.items)}
      </div>
    ))
  }

  function renderUniversitySections(sections: NavSection[]) {
    return sections.map((item) => {
      const isExpanded = expandedSections[item.label]
      const hasChildren = item.children && item.children.length > 0
      const isParentActive = hasChildren && item.children!.some(
        (child) => pathname === child.href || pathname.startsWith(child.href + "/")
      )
      const isActive = !hasChildren && (pathname === item.href || (item.href !== "/dashboard/learner" && pathname.startsWith(item.href || "")))
      const realBadge = badges[item.href || ""] || 0

      if (hasChildren) {
        return (
          <div key={item.label}>
            <button
              type="button"
              onClick={() => setExpandedSections(prev => ({ ...prev, [item.label]: !prev[item.label] }))}
              className={cn(
                "flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all",
                isParentActive
                  ? "bg-primary/10 text-primary"
                  : "text-muted-foreground hover:bg-muted hover:text-foreground",
              )}
            >
              {isPrimary && !isParentActive && item.dotColor && (
                <span className={`size-1.5 shrink-0 rounded-full ${item.dotColor}`} />
              )}
              {(!isPrimary || isParentActive) && <item.icon className="size-4 shrink-0" />}
              <span className="flex-1 text-left">{item.labelKey ? t(item.labelKey) : item.label}</span>
              <ChevronRight className={cn("size-3.5 transition-transform", isExpanded && "rotate-90")} />
            </button>
            {isExpanded && (
              <div className="ml-4 mt-0.5 space-y-0.5 border-l border-border pl-3">
                {item.children!.map((child) => {
                  const childActive = pathname === child.href || pathname.startsWith(child.href + "/")
                  return (
                    <Link
                      key={child.href}
                      href={child.href}
                      onClick={onNavigate}
                      className={cn(
                        "flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition-all",
                        childActive
                          ? "bg-primary text-primary-foreground font-medium shadow-xs"
                          : "text-muted-foreground hover:bg-muted hover:text-foreground",
                      )}
                    >
                      <child.icon className="size-3.5 shrink-0" />
                      <span>{child.labelKey ? t(child.labelKey) : child.label}</span>
                      {badges[child.href] && badges[child.href]! > 0 && (
                        <span className="ml-auto inline-flex size-5 items-center justify-center rounded-full bg-orange text-[10px] font-bold text-orange-foreground">
                          {badges[child.href]}
                        </span>
                      )}
                    </Link>
                  )
                })}
              </div>
            )}
          </div>
        )
      }

      return (
        <Link
          key={item.href}
          href={item.href!}
          onClick={onNavigate}
          title={collapsed ? (item.labelKey ? t(item.labelKey) : item.label) : undefined}
          className={cn(
            "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all",
            collapsed && "justify-center px-2",
            isActive
              ? "bg-primary text-primary-foreground shadow-xs"
              : "text-muted-foreground hover:bg-muted hover:text-foreground",
          )}
        >
          {isPrimary && !isActive && item.dotColor && (
            <span className={`size-1.5 shrink-0 rounded-full ${item.dotColor}`} />
          )}
          {(!isPrimary || isActive) && <item.icon className="size-4 shrink-0" />}
          {!collapsed && <span className="flex-1">{item.labelKey ? t(item.labelKey) : item.label}</span>}
          {!collapsed && realBadge > 0 && (
            <span className={cn(
              "inline-flex size-5 items-center justify-center rounded-full text-[10px] font-bold",
              isActive ? "bg-primary-foreground text-primary" : "bg-orange text-orange-foreground",
            )}>
              {realBadge}
            </span>
          )}
        </Link>
      )
    })
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
          title={tc(collapsed ? "expandSidebar" : "collapseSidebar")}
        >
          <ChevronLeft className={cn("size-4 transition-transform", collapsed && "rotate-180")} />
        </button>
      </div>

      <nav className="flex-1 space-y-1 overflow-y-auto p-3" aria-label={tc("mainNavigation")} role="navigation">
        {isTeacher ? (
          teacherNavSections.map((section, si) => (
            <div key={section.group}>
              {si > 0 && <div className="my-2 border-t border-border" />}
              <p className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                {section.groupKey ? t(section.groupKey) : section.group}
              </p>
              {renderNavItems(section.items)}
            </div>
          ))
        ) : isLearner ? (
          learnerNavSections.map((section, si) => (
            <div key={section.group}>
              {si > 0 && <div className="my-2 border-t border-border" />}
              <p className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                {section.groupKey ? t(section.groupKey) : section.group}
              </p>
              {renderNavItems(section.items)}
            </div>
          ))
        ) : isPrimary && !isParent ? (
          renderPrimarySections(primaryNavSections)
        ) : (user?.learningLevel || "").toUpperCase() === "UNIVERSITY" ? (
          renderUniversitySections(universityNavSections)
        ) : universitySections ? (
          (universitySections as CollegeNavSection[]).map((section, si) => (
            <div key={section.group}>
              {si > 0 && <div className="my-2 border-t border-border" />}
              <p className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                {section.groupKey ? t(section.groupKey) : section.group}
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
                  {t("administration")}
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
          {tc("logout")}
        </button>
      </div>
    </div>
  )
}
