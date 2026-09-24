"use client"

import { useState, useEffect, useCallback } from "react"
import { useAuth } from "@/lib/auth"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  BookOpen,
  Users,
  FileText,
  Calendar,
  BarChart3,
  Plus,
  ChevronRight,
  Clock,
  Target,
  Award,
  Search,
  Filter,
  Settings,
  Edit,
  Eye,
  Play,
  Video,
} from "lucide-react"

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"

function getToken(): string | null {
  if (typeof window === "undefined") return null
  return localStorage.getItem("elmkusoma_access_token")
}

type Course = {
  id: string
  title: string
  code: string
  description?: string
  enrolledStudents: number
  modulesCount: number
  upcomingSessions: number
  completedModules: number
  status: string
}

type Module = {
  id: string
  title: string
  courseId: string
  status: string
  lessonCount: number
  assessmentCount: number
  orderIndex: number
}

type Student = {
  id: string
  name: string
  studentId: string
  email: string
  grade: string
  lastActivity: string
}

const MODULE_STATUS_STYLES: Record<string, { bg: string; text: string }> = {
  ACTIVE: { bg: "bg-green-50 dark:bg-green-950/20", text: "text-green-700 dark:text-green-400" },
  DRAFT: { bg: "bg-gray-50 dark:bg-gray-800/20", text: "text-gray-600 dark:text-gray-400" },
  COMPLETED: { bg: "bg-blue-50 dark:bg-blue-950/20", text: "text-blue-700 dark:text-blue-400" },
}

function formatDateTime(iso: string): string {
  if (!iso) return "—"
  return new Date(iso).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  })
}

function getGradeColor(grade: string): string {
  if (grade.startsWith("A")) return "text-green-600 dark:text-green-400"
  if (grade.startsWith("B")) return "text-blue-600 dark:text-blue-400"
  if (grade.startsWith("C")) return "text-yellow-600 dark:text-yellow-400"
  return "text-red-600 dark:text-red-400"
}

async function fetchTeacherCourses(): Promise<Course[]> {
  const token = getToken()
  const res = await fetch(`${API_BASE}/api/v1/teachers/me/courses`, {
    headers: token ? { Authorization: `Bearer ${token}`, "Content-Type": "application/json" } : {},
  })
  if (!res.ok) throw new Error("Failed to fetch courses")
  return res.json()
}

async function fetchTeacherModules(): Promise<Module[]> {
  const token = getToken()
  const res = await fetch(`${API_BASE}/api/v1/teachers/me/modules`, {
    headers: token ? { Authorization: `Bearer ${token}`, "Content-Type": "application/json" } : {},
  })
  if (!res.ok) throw new Error("Failed to fetch modules")
  return res.json()
}

async function fetchCourseStudents(courseId: string): Promise<Student[]> {
  const token = getToken()
  const res = await fetch(`${API_BASE}/api/v1/teachers/me/courses/${courseId}/students`, {
    headers: token ? { Authorization: `Bearer ${token}`, "Content-Type": "application/json" } : {},
  })
  if (!res.ok) throw new Error("Failed to fetch students")
  return res.json()
}

async function reorderModule(moduleId: string, direction: "up" | "down"): Promise<void> {
  const token = getToken()
  const res = await fetch(`${API_BASE}/api/v1/teachers/me/modules/${moduleId}/reorder`, {
    method: "PUT",
    headers: token ? { Authorization: `Bearer ${token}`, "Content-Type": "application/json" } : {},
    body: JSON.stringify({ direction }),
  })
  if (!res.ok) throw new Error("Failed to reorder module")
}

async function publishModule(moduleId: string): Promise<void> {
  const token = getToken()
  const res = await fetch(`${API_BASE}/api/v1/teachers/me/modules/${moduleId}/publish`, {
    method: "POST",
    headers: token ? { Authorization: `Bearer ${token}`, "Content-Type": "application/json" } : {},
  })
  if (!res.ok) throw new Error("Failed to publish module")
}

function SummaryCard({
  label,
  value,
  icon: Icon,
  color,
  bg,
}: {
  label: string
  value: number
  icon: React.ElementType
  color: string
  bg: string
}) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className="flex items-center gap-3">
        <div className={`flex size-9 items-center justify-center rounded-xl ${bg}`}>
          <Icon className={`size-5 ${color}`} />
        </div>
        <div>
          <p className="text-2xl font-extrabold text-foreground">{value}</p>
          <p className="text-xs font-medium text-muted-foreground">{label}</p>
        </div>
      </div>
    </div>
  )
}

function CourseCard({
  course,
  onSelect,
  onNavigate,
}: {
  course: Course
  onSelect: (course: Course) => void
  onNavigate: (path: string) => void
}) {
  const t = useTranslations("learner")
  const completionPct = course.modulesCount > 0
    ? Math.round((course.completedModules / course.modulesCount) * 100)
    : 0

  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:shadow-sm">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <div className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-primary/10">
              <BookOpen className="size-5 text-primary" />
            </div>
            <div className="min-w-0">
              <h3 className="text-sm font-semibold text-foreground truncate">{course.title}</h3>
              <p className="text-xs text-muted-foreground">{course.code}</p>
            </div>
          </div>

          {course.description && (
            <p className="mt-2 text-xs text-muted-foreground line-clamp-2">{course.description}</p>
          )}

          <div className="mt-3 flex items-center gap-4 text-xs text-muted-foreground">
            <span className="flex items-center gap-1">
              <Users className="size-3" />
              {t("lecturerCourses.studentsCount", { count: course.enrolledStudents })}
            </span>
            <span className="flex items-center gap-1">
              <FileText className="size-3" />
              {t("lecturerCourses.modulesCount", { count: course.modulesCount })}
            </span>
            <span className="flex items-center gap-1">
              <Calendar className="size-3" />
              {t("lecturerCourses.sessionsCount", { count: course.upcomingSessions })}
            </span>
          </div>

          <div className="mt-3">
            <div className="flex items-center justify-between text-xs">
              <span className="text-muted-foreground">{t("lecturerCourses.moduleCompletion")}</span>
              <span className="font-semibold text-foreground">{completionPct}%</span>
            </div>
            <div className="mt-1.5 h-2 w-full overflow-hidden rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-primary transition-all"
                style={{ width: `${completionPct}%` }}
              />
            </div>
          </div>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-2">
        <button
          onClick={() => onSelect(course)}
          className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition-colors"
        >
          <FileText className="size-3" />
          {t("lecturerCourses.viewModules")}
        </button>
        <button
          onClick={() => onSelect(course)}
          className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition-colors"
        >
          <Users className="size-3" />
          {t("lecturerCourses.viewStudents")}
        </button>
        <button
          onClick={() => onNavigate("/dashboard/lecturer/live-dashboard")}
          className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition-colors"
        >
          <Calendar className="size-3" />
          {t("lecturerCourses.scheduleSession")}
        </button>
        <button
          onClick={() => onNavigate("/dashboard/lecturer/analytics")}
          className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition-colors"
        >
          <BarChart3 className="size-3" />
          {t("lecturerCourses.viewAnalytics")}
        </button>
      </div>
    </div>
  )
}

function ModulePanel({
  modules,
  onMoveUp,
  onMoveDown,
  onPublish,
  onEdit,
}: {
  modules: Module[]
  onMoveUp: (idx: number) => void
  onMoveDown: (idx: number) => void
  onPublish: (id: string) => void
  onEdit: (id: string) => void
}) {
  const t = useTranslations("learner")
  const tc = useTranslations("common")
  if (modules.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-border py-8 text-center">
        <FileText className="mx-auto size-8 text-muted-foreground" />
        <p className="mt-2 text-sm font-medium text-foreground">{t("lecturerCourses.noModulesYet")}</p>
        <p className="mt-1 text-xs text-muted-foreground">
          {t("lecturerCourses.createFirstModuleHint")}
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-2">
      {modules
        .sort((a, b) => a.orderIndex - b.orderIndex)
        .map((mod, idx) => {
          const style = MODULE_STATUS_STYLES[mod.status] || MODULE_STATUS_STYLES.DRAFT
          return (
            <div
              key={mod.id}
              className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50"
            >
              <div className="flex shrink-0 flex-col gap-0.5">
                <button
                  onClick={() => onMoveUp(idx)}
                  disabled={idx === 0}
                  className="flex size-5 items-center justify-center rounded text-muted-foreground hover:bg-muted disabled:opacity-30 disabled:cursor-not-allowed"
                  title={t("lecturerCourses.moveUp")}
                >
                  ▲
                </button>
                <button
                  onClick={() => onMoveDown(idx)}
                  disabled={idx === modules.length - 1}
                  className="flex size-5 items-center justify-center rounded text-muted-foreground hover:bg-muted disabled:opacity-30 disabled:cursor-not-allowed"
                  title={t("lecturerCourses.moveDown")}
                >
                  ▼
                </button>
              </div>

              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-medium text-muted-foreground">
                    {idx + 1}.
                  </span>
                  <p className="truncate text-sm font-medium text-foreground">{mod.title}</p>
                  <span
                    className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold ${style.bg} ${style.text}`}
                  >
                    {mod.status}
                  </span>
                </div>
                <div className="mt-1 flex items-center gap-3 text-xs text-muted-foreground">
                  <span className="flex items-center gap-1">
                    <BookOpen className="size-3" />
                    {t("lecturerCourses.lessonsCount", { count: mod.lessonCount })}
                  </span>
                  <span className="flex items-center gap-1">
                    <Target className="size-3" />
                    {t("lecturerCourses.assessmentsCount", { count: mod.assessmentCount })}
                  </span>
                </div>
              </div>

              <div className="flex shrink-0 items-center gap-1">
                <button
                  onClick={() => onEdit(mod.id)}
                  className="inline-flex items-center justify-center size-7 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
                  title={tc("edit")}
                >
                  <Edit className="size-3.5" />
                </button>
                <button
                  className="inline-flex items-center justify-center size-7 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
                  title={t("lecturerCourses.previewTitle")}
                >
                  <Eye className="size-3.5" />
                </button>
                {mod.status === "DRAFT" && (
                  <button
                    onClick={() => onPublish(mod.id)}
                    className="inline-flex items-center gap-1 rounded-lg bg-green-600 px-2.5 py-1 text-[10px] font-medium text-white hover:bg-green-700 transition-colors"
                  >
                    <Play className="size-2.5" />
                    {t("lecturerCourses.publish")}
                  </button>
                )}
              </div>
            </div>
          )
        })}
    </div>
  )
}

function StudentRoster({
  students,
  searchQuery,
  onSearchChange,
}: {
  students: Student[]
  searchQuery: string
  onSearchChange: (q: string) => void
}) {
  const t = useTranslations("learner")
  const filtered = students.filter(
    (s) =>
      s.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.studentId.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.email.toLowerCase().includes(searchQuery.toLowerCase())
  )

  if (students.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-border py-8 text-center">
        <Users className="mx-auto size-8 text-muted-foreground" />
        <p className="mt-2 text-sm font-medium text-foreground">{t("lecturerCourses.noStudentsEnrolled")}</p>
        <p className="mt-1 text-xs text-muted-foreground">
          {t("lecturerCourses.studentsAppearHint")}
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder={t("lecturerCourses.searchStudentsPlaceholder")}
            className="h-10 w-full rounded-lg border border-border bg-background pl-9 pr-3 text-sm outline-none focus:border-ring"
          />
        </div>
        <button className="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-2 text-xs font-medium text-foreground hover:bg-muted transition-colors">
          <Award className="size-3.5" />
          {t("lecturerCourses.exportGrades")}
        </button>
      </div>

      <div className="overflow-x-auto rounded-xl border border-border">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-border bg-muted/50">
              <th className="px-4 py-2.5 text-xs font-medium text-muted-foreground">{t("lecturerCourses.colStudent")}</th>
              <th className="px-4 py-2.5 text-xs font-medium text-muted-foreground">{t("lecturerCourses.colId")}</th>
              <th className="px-4 py-2.5 text-xs font-medium text-muted-foreground">{t("lecturerCourses.colEmail")}</th>
              <th className="px-4 py-2.5 text-xs font-medium text-muted-foreground">{t("lecturerCourses.colGrade")}</th>
              <th className="px-4 py-2.5 text-xs font-medium text-muted-foreground">{t("lecturerCourses.colLastActivity")}</th>
              <th className="px-4 py-2.5 text-xs font-medium text-muted-foreground text-right">{t("lecturerCourses.colActions")}</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((student, idx) => (
              <tr
                key={student.id}
                className={`border-b border-border last:border-0 ${
                  idx % 2 === 0 ? "bg-background" : "bg-muted/20"
                }`}
              >
                <td className="px-4 py-2.5">
                  <span className="font-medium text-foreground">{student.name}</span>
                </td>
                <td className="px-4 py-2.5 text-muted-foreground">{student.studentId}</td>
                <td className="px-4 py-2.5 text-muted-foreground">{student.email}</td>
                <td className="px-4 py-2.5">
                  <span className={`font-semibold ${getGradeColor(student.grade)}`}>
                    {student.grade || "—"}
                  </span>
                </td>
                <td className="px-4 py-2.5 text-xs text-muted-foreground">
                  {formatDateTime(student.lastActivity)}
                </td>
                <td className="px-4 py-2.5 text-right">
                  <div className="flex items-center justify-end gap-1">
                    <button
                      className="inline-flex items-center gap-1 rounded-lg border border-border px-2 py-1 text-[10px] font-medium text-foreground hover:bg-muted transition-colors"
                      title={t("lecturerCourses.viewProgress")}
                    >
                      <BarChart3 className="size-2.5" />
                      {t("lecturerCourses.viewProgress")}
                    </button>
                    <button
                      className="inline-flex items-center gap-1 rounded-lg border border-border px-2 py-1 text-[10px] font-medium text-foreground hover:bg-muted transition-colors"
                      title={t("lecturerCourses.messageStudent")}
                    >
                      <FileText className="size-2.5" />
                      {t("lecturerCourses.messageStudent")}
                    </button>
                    <button
                      className="inline-flex items-center gap-1 rounded-lg border border-border px-2 py-1 text-[10px] font-medium text-foreground hover:bg-muted transition-colors"
                      title={t("lecturerCourses.gradeStudent")}
                    >
                      <Award className="size-2.5" />
                      {t("lecturerCourses.gradeStudent")}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {filtered.length === 0 && searchQuery && (
        <p className="text-center text-xs text-muted-foreground py-4">
          {t("lecturerCourses.noStudentsMatch", { query: searchQuery })}
        </p>
      )}
    </div>
  )
}

function QuickActionsGrid({ onNavigate }: { onNavigate: (path: string) => void }) {
  const t = useTranslations("learner")
  const actions = [
    {
      label: t("lecturerCourses.actionCreateModule"),
      description: t("lecturerCourses.actionCreateModuleDesc"),
      icon: Plus,
      color: "text-purple-600 dark:text-purple-400",
      bg: "bg-purple-50 dark:bg-purple-950/20",
      path: "/dashboard/learner/module-workspace",
    },
    {
      label: t("lecturerCourses.actionScheduleLive"),
      description: t("lecturerCourses.actionScheduleLiveDesc"),
      icon: Video,
      color: "text-red-600 dark:text-red-400",
      bg: "bg-red-50 dark:bg-red-950/20",
      path: "/dashboard/lecturer/live-dashboard",
    },
    {
      label: t("lecturerCourses.viewAnalytics"),
      description: t("lecturerCourses.actionAnalyticsDesc"),
      icon: BarChart3,
      color: "text-blue-600 dark:text-blue-400",
      bg: "bg-blue-50 dark:bg-blue-950/20",
      path: "/dashboard/lecturer/analytics",
    },
    {
      label: t("lecturerCourses.actionManageResources"),
      description: t("lecturerCourses.actionManageResourcesDesc"),
      icon: Settings,
      color: "text-orange-600 dark:text-orange-400",
      bg: "bg-orange-50 dark:bg-orange-950/20",
      path: "/dashboard/lecturer/resources",
    },
  ]

  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      {actions.map((action) => (
        <button
          key={action.label}
          onClick={() => onNavigate(action.path)}
          className="rounded-2xl border border-border bg-card p-5 shadow-xs text-left transition-all hover:shadow-sm hover:border-primary/30 group"
        >
          <div className={`flex size-10 items-center justify-center rounded-xl ${action.bg}`}>
            <action.icon className={`size-5 ${action.color}`} />
          </div>
          <p className="mt-3 text-sm font-semibold text-foreground group-hover:text-primary transition-colors">
            {action.label}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">{action.description}</p>
          <ChevronRight className="mt-2 size-4 text-muted-foreground group-hover:text-primary transition-colors" />
        </button>
      ))}
    </div>
  )
}

export default function LecturerCoursesPage() {
  const { user } = useAuth()
  const router = useRouter()
  const t = useTranslations("learner")

  const [courses, setCourses] = useState<Course[]>([])
  const [modules, setModules] = useState<Module[]>([])
  const [students, setStudents] = useState<Student[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedCourse, setSelectedCourse] = useState<Course | null>(null)
  const [activeTab, setActiveTab] = useState<"modules" | "students">("modules")
  const [studentSearch, setStudentSearch] = useState("")
  const [loadingStudents, setLoadingStudents] = useState(false)

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const [coursesData, modulesData] = await Promise.all([
        fetchTeacherCourses(),
        fetchTeacherModules(),
      ])
      setCourses(coursesData)
      setModules(modulesData)
    } catch {
      setError(t("lecturerCourses.loadFailed"))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (user) loadData()
  }, [user, loadData])

  const loadStudents = useCallback(async (courseId: string) => {
    try {
      setLoadingStudents(true)
      const data = await fetchCourseStudents(courseId)
      setStudents(data)
    } catch {
      setStudents([])
    } finally {
      setLoadingStudents(false)
    }
  }, [])

  const handleSelectCourse = useCallback(
    (course: Course) => {
      setSelectedCourse(course)
      setActiveTab("modules")
      setStudents([])
      loadStudents(course.id)
    },
    [loadStudents]
  )

  const handleReorderModule = async (idx: number, direction: "up" | "down") => {
    const sorted = [...modules].sort((a, b) => a.orderIndex - b.orderIndex)
    const targetIdx = direction === "up" ? idx - 1 : idx + 1
    if (targetIdx < 0 || targetIdx >= sorted.length) return

    const tempModules = [...sorted]
    const temp = tempModules[idx]
    tempModules[idx] = tempModules[targetIdx]
    tempModules[targetIdx] = temp
    setModules(tempModules)

    try {
      await reorderModule(sorted[idx].id, direction)
    } catch {
      setModules(sorted)
      setError(t("lecturerCourses.reorderFailed"))
      setTimeout(() => setError(null), 3000)
    }
  }

  const handlePublishModule = async (moduleId: string) => {
    try {
      await publishModule(moduleId)
      setModules((prev) =>
        prev.map((m) => (m.id === moduleId ? { ...m, status: "ACTIVE" } : m))
      )
    } catch {
      setError(t("lecturerCourses.publishFailed"))
      setTimeout(() => setError(null), 3000)
    }
  }

  const handleEditModule = (moduleId: string) => {
    router.push(`/dashboard/learner/module-workspace?moduleId=${moduleId}`)
  }

  const handleNavigate = (path: string) => {
    router.push(path)
  }

  const totalStudents = courses.reduce((sum, c) => sum + c.enrolledStudents, 0)
  const activeModules = modules.filter((m) => m.status === "ACTIVE").length
  const upcomingSessions = courses.reduce((sum, c) => sum + c.upcomingSessions, 0)

  const courseModules = selectedCourse
    ? modules.filter((m) => m.courseId === selectedCourse.id)
    : []

  const summaryCards = [
    {
      label: t("lecturerCourses.summaryCourses"),
      value: courses.length,
      icon: BookOpen,
      color: "text-blue-600 dark:text-blue-400",
      bg: "bg-blue-50 dark:bg-blue-950/20",
    },
    {
      label: t("lecturerCourses.summaryActiveModules"),
      value: activeModules,
      icon: FileText,
      color: "text-green-600 dark:text-green-400",
      bg: "bg-green-50 dark:bg-green-950/20",
    },
    {
      label: t("lecturerCourses.summaryTotalStudents"),
      value: totalStudents,
      icon: Users,
      color: "text-purple-600 dark:text-purple-400",
      bg: "bg-purple-50 dark:bg-purple-950/20",
    },
    {
      label: t("lecturerCourses.summaryUpcomingSessions"),
      value: upcomingSessions,
      icon: Calendar,
      color: "text-orange-600 dark:text-orange-400",
      bg: "bg-orange-50 dark:bg-orange-950/20",
    },
  ]

  if (loading) return <LoadingState />

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader
        firstName={user?.firstName || user?.name || t("lecturerCourses.lecturerFallback")}
        subtitle={t("lecturerCourses.headerSubtitle")}
      />

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            {error}
          </div>
        </div>
      )}

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {summaryCards.map((card) => (
          <SummaryCard key={card.label} {...card} />
        ))}
      </div>

      <QuickActionsGrid onNavigate={handleNavigate} />

      {selectedCourse && (
        <section>
          <div className="flex items-center justify-between gap-3 mb-3">
            <div className="flex items-center gap-2">
              <button
                onClick={() => setSelectedCourse(null)}
                className="text-xs font-medium text-muted-foreground hover:text-foreground transition-colors"
              >
                ← {t("lecturerCourses.backToAllCourses")}
              </button>
              <span className="text-muted-foreground">|</span>
              <h2 className="text-sm font-semibold text-foreground">
                {selectedCourse.title} ({selectedCourse.code})
              </h2>
            </div>
            <div className="flex items-center gap-1">
              <button
                onClick={() => setActiveTab("modules")}
                className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${
                  activeTab === "modules"
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:bg-muted"
                }`}
              >
                <FileText className="mr-1 inline size-3" />
                {t("lecturerCourses.modulesTab", { count: courseModules.length })}
              </button>
              <button
                onClick={() => setActiveTab("students")}
                className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${
                  activeTab === "students"
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:bg-muted"
                }`}
              >
                <Users className="mr-1 inline size-3" />
                {t("lecturerCourses.studentsTab", { count: selectedCourse.enrolledStudents })}
              </button>
            </div>
          </div>

          {activeTab === "modules" && (
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-foreground">
                  {t("lecturerCourses.courseModules")}
                </h3>
                <button
                  onClick={() => handleNavigate("/dashboard/learner/module-workspace")}
                  className="inline-flex items-center gap-1 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
                >
                  <Plus className="size-3" />
                  {t("lecturerCourses.addModule")}
                </button>
              </div>
              <ModulePanel
                modules={courseModules}
                onMoveUp={(idx) => handleReorderModule(idx, "up")}
                onMoveDown={(idx) => handleReorderModule(idx, "down")}
                onPublish={handlePublishModule}
                onEdit={handleEditModule}
              />
            </div>
          )}

          {activeTab === "students" && (
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-foreground">
                  {t("lecturerCourses.studentRoster")}
                </h3>
                {loadingStudents && (
                  <span className="text-xs text-muted-foreground">{t("lecturerCourses.loadingStudents")}</span>
                )}
              </div>
              <StudentRoster
                students={students}
                searchQuery={studentSearch}
                onSearchChange={setStudentSearch}
              />
            </div>
          )}
        </section>
      )}

      {!selectedCourse && (
        <section>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-semibold text-foreground">{t("lecturerCourses.summaryCourses")}</h2>
            <div className="flex items-center gap-2">
              <Filter className="size-4 text-muted-foreground" />
              <span className="text-xs text-muted-foreground">{t("lecturerCourses.coursesCount", { count: courses.length })}</span>
            </div>
          </div>

          {courses.length > 0 ? (
            <div className="grid gap-4 sm:grid-cols-2">
              {courses.map((course) => (
                <CourseCard
                  key={course.id}
                  course={course}
                  onSelect={handleSelectCourse}
                  onNavigate={handleNavigate}
                />
              ))}
            </div>
          ) : (
            <EmptyState
              icon={<BookOpen className="size-10" />}
              title={t("lecturerCourses.noCoursesAssigned")}
              description={t("lecturerCourses.noCoursesHint")}
            />
          )}
        </section>
      )}

      {!selectedCourse && courses.length > 0 && (
        <section>
          <h2 className="mb-3 text-sm font-semibold text-foreground">
            {t("lecturerCourses.recentActivity")}
          </h2>
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="space-y-3">
              {modules
                .filter((m) => m.status === "ACTIVE")
                .slice(0, 5)
                .map((mod) => (
                  <div
                    key={mod.id}
                    className="flex items-center gap-3 rounded-xl border border-border p-3"
                  >
                    <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-green-50 dark:bg-green-950/20">
                      <FileText className="size-4 text-green-600 dark:text-green-400" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-foreground">
                        {mod.title}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {t("lecturerCourses.lessonAssessmentCounts", { lessons: mod.lessonCount, assessments: mod.assessmentCount })}
                      </p>
                    </div>
                    <span className="shrink-0 rounded-full bg-green-50 dark:bg-green-950/20 px-2 py-0.5 text-[10px] font-semibold text-green-700 dark:text-green-400">
                      ACTIVE
                    </span>
                  </div>
                ))}
              {modules.filter((m) => m.status === "ACTIVE").length === 0 && (
                <p className="text-center text-xs text-muted-foreground py-4">
                  {t("lecturerCourses.noActiveModules")}
                </p>
              )}
            </div>
          </div>
        </section>
      )}
    </div>
  )
}
