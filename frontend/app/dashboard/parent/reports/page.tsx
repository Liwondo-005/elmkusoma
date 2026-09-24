"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { BarChart3, Loader2, TrendingUp, TrendingDown, Minus, BookOpen } from "lucide-react"
import { useAuth } from "@/lib/auth"
import { parentApi, type ChildOverview, type ParentSubjectPerformance, type ParentLearningProgress, type SubjectPerformanceItem, type CourseProgressItem } from "@/lib/parent-api"

export default function ParentReportsPage() {
  const { user } = useAuth()
  const t = useTranslations("parent")
  const tn = useTranslations("nav")
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [selectedChildId, setSelectedChildId] = useState<string | null>(null)
  const [subjectPerf, setSubjectPerf] = useState<ParentSubjectPerformance | null>(null)
  const [learningProgress, setLearningProgress] = useState<ParentLearningProgress | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    parentApi.getChildren().then((kids) => {
      setChildren(kids)
      if (kids.length > 0) {
        const primary = kids.find((c) => c.isPrimary) || kids[0]
        setSelectedChildId(primary.studentId)
      }
      setLoading(false)
    }).catch(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (!selectedChildId) return
    setSubjectPerf(null)
    setLearningProgress(null)
    Promise.all([
      parentApi.getChildSubjectPerformance(selectedChildId).catch(() => null),
      parentApi.getChildLearningProgress(selectedChildId).catch(() => null),
    ]).then(([sp, lp]) => {
      setSubjectPerf(sp)
      setLearningProgress(lp)
    })
  }, [selectedChildId])

  if (loading) return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>

  const subjects = subjectPerf?.subjects || []
  const courses = learningProgress?.courses || []
  const avgGpa = subjects.length > 0 && subjects.some((s) => s.gpa != null)
    ? subjects.filter((s) => s.gpa != null).reduce((sum, s) => sum + (s.gpa || 0), 0) / subjects.filter((s) => s.gpa != null).length
    : null

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("reports")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("reports.subtitle")}</p>
      </div>

      {children.length > 1 && (
        <div className="flex gap-2 overflow-x-auto pb-1">
          {children.map((child) => (
            <button key={child.studentId} onClick={() => setSelectedChildId(child.studentId)}
              className={`shrink-0 rounded-xl border px-4 py-2 text-sm font-medium transition-colors ${selectedChildId === child.studentId ? "border-primary bg-primary text-primary-foreground" : "border-border bg-card text-foreground hover:bg-muted"}`}>
              {child.studentName}
            </button>
          ))}
        </div>
      )}

      {/* Summary Cards */}
      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{t("reports.subjectsLabel")}</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{subjects.length}</p>
          <p className="text-xs text-muted-foreground">{t("reports.activeSubjects")}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{t("reports.avgGpaLabel")}</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{avgGpa != null ? avgGpa.toFixed(2) : "—"}</p>
          <p className="text-xs text-muted-foreground">{t("reports.acrossSubjects")}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{t("reports.coursesLabel")}</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{courses.length}</p>
          <p className="text-xs text-muted-foreground">{t("reports.coursesInProgress")}</p>
        </div>
      </div>

      {/* Subject Performance */}
      {subjects.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">{t("reports.subjectPerfTitle")}</h2>
          <div className="mt-4 space-y-3">
            {subjects.map((s) => (
              <SubjectRow key={s.subjectId} subject={s} />
            ))}
          </div>
        </section>
      )}

      {/* Course Progress */}
      {courses.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">{t("reports.courseProgressTitle")}</h2>
          <div className="mt-4 space-y-3">
            {courses.map((c) => (
              <CourseRow key={c.courseId} course={c} />
            ))}
          </div>
        </section>
      )}

      {subjects.length === 0 && courses.length === 0 && (
        <div className="rounded-2xl border border-dashed border-border py-16 text-center">
          <BarChart3 className="mx-auto mb-3 size-10 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">{t("reports.emptyTitle")}</p>
          <p className="mt-1 text-xs text-muted-foreground">{t("reports.emptyDesc")}</p>
        </div>
      )}
    </div>
  )
}

function SubjectRow({ subject }: { subject: SubjectPerformanceItem }) {
  const TrendIcon = subject.trend === "IMPROVING" ? TrendingUp : subject.trend === "DECLINING" ? TrendingDown : Minus
  const trendColor = subject.trend === "IMPROVING" ? "text-green-600" : subject.trend === "DECLINING" ? "text-red-500" : "text-muted-foreground"
  const gradeColor = subject.grade === "A" ? "bg-green-100 text-green-700" :
    subject.grade === "B" ? "bg-blue-100 text-blue-700" :
    subject.grade === "C" ? "bg-yellow-100 text-yellow-700" :
    subject.grade === "D" ? "bg-orange-100 text-orange-700" :
    subject.grade === "F" ? "bg-red-100 text-red-700" : "bg-muted text-muted-foreground"

  return (
    <div className="flex items-center gap-4 rounded-xl border border-border bg-muted/30 p-4">
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-foreground">{subject.subjectName}</p>
        <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-muted">
          <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${Math.min(100, subject.averageMark || 0)}%` }} />
        </div>
      </div>
      <div className="flex items-center gap-3">
        <div className="text-right">
          <p className="text-sm font-bold text-foreground">{subject.averageMark != null ? `${Math.round(subject.averageMark)}%` : "—"}</p>
          <p className="text-[10px] text-muted-foreground">{subject.completedAssessments}/{subject.totalAssessments}</p>
        </div>
        {subject.grade && (
          <span className={`shrink-0 rounded px-2 py-1 text-xs font-bold ${gradeColor}`}>{subject.grade}</span>
        )}
        <TrendIcon className={`size-4 shrink-0 ${trendColor}`} />
      </div>
    </div>
  )
}

function CourseRow({ course }: { course: CourseProgressItem }) {
  const t = useTranslations("parent")
  const statusColor = course.status === "COMPLETED" ? "bg-green-100 text-green-700" :
    course.status === "IN_PROGRESS" ? "bg-blue-100 text-blue-700" :
    "bg-muted text-muted-foreground"

  return (
    <div className="rounded-xl border border-border bg-muted/30 p-4">
      <div className="flex items-center gap-3">
        <BookOpen className="size-4 shrink-0 text-muted-foreground" />
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <p className="text-sm font-medium text-foreground">{course.courseName}</p>
            <span className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] font-semibold ${statusColor}`}>{course.status?.replace("_", " ")}</span>
          </div>
          <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-muted">
            <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${course.progressPercentage || 0}%` }} />
          </div>
          <div className="mt-1 flex items-center gap-3 text-[10px] text-muted-foreground">
            <span>{t("reports.lessonsCount", { completed: course.completedLessons, total: course.totalLessons })}</span>
            {course.currentLesson && <span>{t("reports.currentLesson", { lesson: course.currentLesson })}</span>}
          </div>
        </div>
        <span className="text-xs font-bold text-foreground">{Math.round(course.progressPercentage || 0)}%</span>
      </div>
    </div>
  )
}
