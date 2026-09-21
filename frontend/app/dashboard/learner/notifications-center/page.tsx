"use client"

import { useState, useEffect, useCallback } from "react"
import { useAuth } from "@/lib/auth"
import { useRouter } from "next/navigation"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  Bell,
  BookOpen,
  Video,
  FileText,
  Target,
  Clock,
  Award,
  Users,
  AlertCircle,
  CheckCircle,
  Calendar,
  GraduationCap,
  Briefcase,
  Filter,
  Trash2,
  Eye,
  EyeOff,
} from "lucide-react"

type Notification = {
  id: string
  title: string
  message: string
  type: string
  read: boolean
  createdAt: string
  link?: string
}

const STORAGE_KEY = "elmku_notifications_center_read_state"
const DELETED_KEY = "elmku_notifications_center_deleted"

const NOTIFICATION_CONFIG: Record<
  string,
  { icon: typeof Bell; color: string; bg: string; label: string; emoji: string }
> = {
  LIVE: { icon: Bell, color: "text-red-500", bg: "bg-red-500/10", label: "Live", emoji: "\uD83D\uDD34" },
  COURSE: { icon: BookOpen, color: "text-blue-500", bg: "bg-blue-500/10", label: "Course", emoji: "\uD83D\uDCDA" },
  ASSESSMENT: { icon: FileText, color: "text-amber-500", bg: "bg-amber-500/10", label: "Assessment", emoji: "\uD83D\uDCDD" },
  RESEARCH: { icon: Award, color: "text-purple-500", bg: "bg-purple-500/10", label: "Research", emoji: "\uD83D\uDD2C" },
  PROJECT: { icon: Target, color: "text-cyan-500", bg: "bg-cyan-500/10", label: "Project", emoji: "\uD83D\uDCC1" },
  ACADEMIC: { icon: GraduationCap, color: "text-indigo-500", bg: "bg-indigo-500/10", label: "Academic", emoji: "\uD83C\uDF93" },
  REPLAY: { icon: Video, color: "text-emerald-500", bg: "bg-emerald-500/10", label: "Replay", emoji: "\uD83D\uDCFA" },
  CAREER: { icon: Briefcase, color: "text-pink-500", bg: "bg-pink-500/10", label: "Career", emoji: "\uD83D\uDCBC" },
}

const CATEGORIES = ["ALL", "LIVE", "COURSE", "ASSESSMENT", "RESEARCH", "PROJECT", "ACADEMIC", "CAREER"] as const
type CategoryFilter = (typeof CATEGORIES)[number]

const MOCK_NOTIFICATIONS: Notification[] = [
  {
    id: "n1",
    title: "Live Session Starting Soon",
    message: "Advanced Machine Learning — live lecture with Dr. Mensah begins in 15 minutes. Join now to participate in the Q&A segment.",
    type: "LIVE",
    read: false,
    createdAt: new Date(Date.now() - 1000 * 60 * 12).toISOString(),
    link: "/dashboard/learner/live-classes",
  },
  {
    id: "n2",
    title: "New Course Material Available",
    message: "Module 6: Neural Network Architectures — lecture slides, code notebooks, and supplementary readings have been uploaded.",
    type: "COURSE",
    read: false,
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 2).toISOString(),
    link: "/dashboard/learner/courses",
  },
  {
    id: "n3",
    title: "Assignment Due Tomorrow",
    message: "Research Methodology — Literature Review Report is due tomorrow at 23:59 EAT. You have 8 references completed out of 12.",
    type: "ASSESSMENT",
    read: false,
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 5).toISOString(),
    link: "/dashboard/learner/modules",
  },
  {
    id: "n4",
    title: "Supervisor Feedback Received",
    message: "Prof. Ndungu has reviewed your thesis proposal draft and left detailed feedback on your methodology section. 3 comments to address.",
    type: "RESEARCH",
    read: true,
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 8).toISOString(),
    link: "/dashboard/learner/research",
  },
  {
    id: "n5",
    title: "Project Milestone Approaching",
    message: "Capstone Project — Sprint 3 demo is scheduled for Friday. Your team has completed 70% of the deliverables. 2 tasks remaining.",
    type: "PROJECT",
    read: false,
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 12).toISOString(),
    link: "/dashboard/learner/projects",
  },
  {
    id: "n6",
    title: "Timetable Update",
    message: "Your Semester 2 examination timetable has been published. Final exams start on December 8th. Please review your schedule.",
    type: "ACADEMIC",
    read: true,
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 24).toISOString(),
    link: "/dashboard/learner/calendar",
  },
  {
    id: "n7",
    title: "Recording Available",
    message: "Last week's Data Structures & Algorithms live session recording is now available. Duration: 1h 42m. Chapters: Arrays, Trees, Graphs.",
    type: "REPLAY",
    read: true,
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 26).toISOString(),
    link: "/dashboard/learner/media-library",
  },
  {
    id: "n8",
    title: "Career Workshop: Technical Interviews",
    message: "Register for the upcoming workshop on preparing for software engineering technical interviews. Spaces are limited — 25 spots remaining.",
    type: "CAREER",
    read: false,
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 30).toISOString(),
    link: "/dashboard/learner/professional-dev",
  },
  {
    id: "n9",
    title: "Grade Posted",
    message: "Your midterm examination for Database Systems has been graded. You scored 78/100. View detailed breakdown in your results.",
    type: "ASSESSMENT",
    read: true,
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 48).toISOString(),
    link: "/dashboard/learner/modules",
  },
  {
    id: "n10",
    title: "Course Announcement",
    message: "Software Engineering II — Prof. Achieng has extended the group project proposal deadline to next Monday. Updated rubric attached.",
    type: "COURSE",
    read: true,
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 52).toISOString(),
    link: "/dashboard/learner/courses",
  },
  {
    id: "n11",
    title: "Live Session Recording Available",
    message: "Introduction to Artificial Intelligence — Dr. Wanjiku's recorded session on Reinforcement Learning is ready for viewing.",
    type: "LIVE",
    read: true,
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 72).toISOString(),
    link: "/dashboard/learner/media-library",
  },
  {
    id: "n12",
    title: "Industry Partner Visit",
    message: "Safaricom PLC will be conducting a campus recruitment session next week. Prepare your portfolio and resume.",
    type: "CAREER",
    read: true,
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 96).toISOString(),
    link: "/dashboard/learner/professional-dev",
  },
]

function getReadState(): Record<string, boolean> {
  if (typeof window === "undefined") return {}
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function saveReadState(state: Record<string, boolean>) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  } catch {}
}

function getDeletedState(): string[] {
  if (typeof window === "undefined") return []
  try {
    const raw = localStorage.getItem(DELETED_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveDeletedState(ids: string[]) {
  try {
    localStorage.setItem(DELETED_KEY, JSON.stringify(ids))
  } catch {}
}

function getTimeAgo(dateStr: string): string {
  const now = Date.now()
  const then = new Date(dateStr).getTime()
  const diffMs = now - then
  const diffMin = Math.floor(diffMs / 60000)
  const diffHr = Math.floor(diffMs / 3600000)
  const diffDay = Math.floor(diffMs / 86400000)

  if (diffMin < 1) return "Just now"
  if (diffMin < 60) return `${diffMin} minute${diffMin > 1 ? "s" : ""} ago`
  if (diffHr < 24) return `${diffHr} hour${diffHr > 1 ? "s" : ""} ago`
  if (diffDay === 1) return "Yesterday"
  if (diffDay < 7) return `${diffDay} days ago`
  return new Date(dateStr).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })
}

function getCategoryConfig(type: string) {
  return NOTIFICATION_CONFIG[type] || NOTIFICATION_CONFIG.COURSE
}

export default function NotificationsCenterPage() {
  const { user, loading: authLoading } = useAuth()
  const router = useRouter()

  const [readState, setReadState] = useState<Record<string, boolean>>({})
  const [deletedIds, setDeletedIds] = useState<string[]>([])
  const [activeFilter, setActiveFilter] = useState<CategoryFilter>("ALL")
  const [initialized, setInitialized] = useState(false)

  useEffect(() => {
    setReadState(getReadState())
    setDeletedIds(getDeletedState())
    setInitialized(true)
  }, [])

  const notifications = MOCK_NOTIFICATIONS.filter((n) => !deletedIds.includes(n.id))
  const notificationsWithRead = notifications.map((n) => ({
    ...n,
    read: readState[n.id] !== undefined ? readState[n.id] : n.read,
  }))

  const filtered =
    activeFilter === "ALL"
      ? notificationsWithRead
      : notificationsWithRead.filter((n) => n.type === activeFilter)

  const unreadCount = notificationsWithRead.filter((n) => !n.read).length

  const toggleRead = useCallback(
    (id: string) => {
      setReadState((prev) => {
        const next = { ...prev, [id]: !prev[id] }
        saveReadState(next)
        return next
      })
    },
    []
  )

  const markAllRead = useCallback(() => {
    const next: Record<string, boolean> = {}
    notificationsWithRead.forEach((n) => {
      next[n.id] = true
    })
    setReadState(next)
    saveReadState(next)
  }, [notificationsWithRead])

  const deleteNotification = useCallback(
    (id: string) => {
      setDeletedIds((prev) => {
        const next = [...prev, id]
        saveDeletedState(next)
        return next
      })
    },
    []
  )

  if (authLoading || (!user) || (user.role !== "Other Learner" && user.role !== "Student")) {
    return <LoadingState />
  }

  if (!initialized) {
    return <LoadingState />
  }

  const firstName = user.firstName || user.name?.split(" ")[0] || "Student"

  return (
    <div className="mx-auto max-w-4xl space-y-6 pb-12">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <LearnerHeader firstName={firstName} subtitle="Stay updated with your academic notifications" />
        {unreadCount > 0 && (
          <button
            onClick={markAllRead}
            className="inline-flex items-center gap-2 self-start rounded-lg border border-border bg-background px-4 py-2 text-sm font-medium text-foreground transition-colors hover:bg-muted"
          >
            <CheckCircle className="size-4" />
            Mark All Read
          </button>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <Filter className="size-4 text-muted-foreground" />
        {CATEGORIES.map((cat) => {
          const isActive = activeFilter === cat
          const config = cat !== "ALL" ? NOTIFICATION_CONFIG[cat] : null
          const count =
            cat === "ALL"
              ? notificationsWithRead.length
              : notificationsWithRead.filter((n) => n.type === cat).length
          return (
            <button
              key={cat}
              onClick={() => setActiveFilter(cat)}
              className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-medium transition-all ${
                isActive
                  ? "bg-primary text-primary-foreground shadow-sm"
                  : "border border-border bg-background text-muted-foreground hover:bg-muted/50 hover:text-foreground"
              }`}
            >
              {config && <span>{config.emoji}</span>}
              {cat === "ALL" ? "All" : config?.label || cat}
              <span
                className={`ml-0.5 rounded-full px-1.5 py-0.5 text-[10px] font-semibold ${
                  isActive ? "bg-primary-foreground/20 text-primary-foreground" : "bg-muted text-muted-foreground"
                }`}
              >
                {count}
              </span>
            </button>
          )
        })}
      </div>

      {unreadCount > 0 && activeFilter === "ALL" && (
        <div className="flex items-center gap-2 rounded-xl bg-primary/5 px-4 py-2.5 text-sm text-primary">
          <Bell className="size-4" />
          <span>
            You have <strong>{unreadCount}</strong> unread notification{unreadCount > 1 ? "s" : ""}
          </span>
        </div>
      )}

      {filtered.length === 0 ? (
        <div className="space-y-4">
          <EmptyState
            icon={<Bell className="size-8" />}
            title={
              activeFilter === "ALL"
                ? "No notifications yet"
                : `No ${NOTIFICATION_CONFIG[activeFilter]?.label || activeFilter.toLowerCase()} notifications`
            }
            description={
              activeFilter === "ALL"
                ? "You're all caught up! Academic notifications will appear here."
                : `There are no notifications in the ${NOTIFICATION_CONFIG[activeFilter]?.label || activeFilter.toLowerCase()} category right now.`
            }
          />
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((notification) => {
            const config = getCategoryConfig(notification.type)
            const IconComponent = config.icon
            return (
              <div
                key={notification.id}
                className={`group rounded-2xl border p-5 shadow-xs transition-all duration-200 ${
                  notification.read
                    ? "border-border bg-card"
                    : "border-primary/20 bg-primary/5"
                }`}
              >
                <div className="flex items-start gap-4">
                  <div
                    className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${config.bg}`}
                  >
                    <IconComponent className={`size-5 ${config.color}`} />
                  </div>

                  <div className="min-w-0 flex-1">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <h3
                            className={`text-sm leading-snug ${
                              notification.read
                                ? "font-medium text-foreground"
                                : "font-semibold text-foreground"
                            }`}
                          >
                            {notification.title}
                          </h3>
                          {!notification.read && (
                            <span className="size-2 shrink-0 rounded-full bg-blue-500" />
                          )}
                        </div>
                        <p className="mt-1.5 text-xs leading-relaxed text-muted-foreground line-clamp-2">
                          {notification.message}
                        </p>
                      </div>
                    </div>

                    <div className="mt-3 flex flex-wrap items-center gap-3">
                      <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                        <Clock className="size-3" />
                        {getTimeAgo(notification.createdAt)}
                      </span>

                      <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-medium ${config.bg} ${config.color}`}>
                        {config.emoji} {config.label}
                      </span>

                      {notification.link && (
                        <button
                          onClick={() => router.push(notification.link!)}
                          className="inline-flex items-center gap-1 text-[10px] font-medium text-primary hover:underline"
                        >
                          View Details
                          <span className="text-xs">\u2192</span>
                        </button>
                      )}
                    </div>
                  </div>

                  <div className="flex shrink-0 items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100">
                    <button
                      onClick={() => toggleRead(notification.id)}
                      title={notification.read ? "Mark as unread" : "Mark as read"}
                      className="rounded-lg p-1.5 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                    >
                      {notification.read ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                    </button>
                    <button
                      onClick={() => deleteNotification(notification.id)}
                      title="Delete notification"
                      className="rounded-lg p-1.5 text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive"
                    >
                      <Trash2 className="size-4" />
                    </button>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {filtered.length > 0 && (
        <div className="text-center text-xs text-muted-foreground">
          Showing {filtered.length} notification{filtered.length !== 1 ? "s" : ""}
          {activeFilter !== "ALL" && (
            <span>
              {" "}
              in{" "}
              <span className="font-medium text-foreground">
                {NOTIFICATION_CONFIG[activeFilter]?.label || activeFilter}
              </span>
            </span>
          )}
        </div>
      )}
    </div>
  )
}
