"use client"

import { useEffect, useState } from "react"
import { useParams } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { ArrowLeft, Clock, FileText, BarChart3, ChevronRight, Loader2, PenTool, Video, TrendingUp, BookOpen, Shield } from "lucide-react"
import { parentApi, type ChildOverview, type LearningProgressData, type EntitlementItem } from "@/lib/parent-api"

export default function ChildDetailPage() {
  const t = useTranslations("parent")
  const tn = useTranslations("nav")
  const ts = useTranslations("status")
  const params = useParams()
  const studentId = params.id as string
  const [child, setChild] = useState<ChildOverview | null>(null)
  const [progress, setProgress] = useState<LearningProgressData | null>(null)
  const [entitlements, setEntitlements] = useState<EntitlementItem[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      parentApi.getChild(studentId),
      parentApi.getChildLearningProgress(studentId).catch(() => null),
      parentApi.getChildEntitlements(studentId).catch(() => []),
    ]).then(([c, p, e]) => {
      setChild(c)
      setProgress(p)
      setEntitlements(e)
    }).finally(() => setLoading(false))
  }, [studentId])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (!child) {
    return (
      <div className="py-16 text-center">
        <p className="text-sm text-muted-foreground">{t("childDetail.notFound")}</p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <Link href="/dashboard/parent" className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:underline">
        <ArrowLeft className="size-4" /> {t("assignments.backToDashboard")}
      </Link>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-xl font-bold text-foreground">{child.studentName}</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {child.admissionNumber} &middot; {child.relationshipType}
            </p>
          </div>
          {child.isPrimary && (
            <span className="rounded bg-primary/10 px-2 py-1 text-xs font-semibold text-primary">{t("children.primaryBadge")}</span>
          )}
        </div>

        <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
          <div className="rounded-xl border border-border bg-muted/30 p-4 text-center">
            <p className="text-2xl font-bold text-foreground">
              {child.attendancePercentage !== null ? `${Math.round(child.attendancePercentage)}%` : "—"}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">{tn("attendance")}</p>
          </div>
          <div className="rounded-xl border border-border bg-muted/30 p-4 text-center">
            <p className="text-2xl font-bold text-foreground">{child.latestGrade || "—"}</p>
            <p className="mt-1 text-xs text-muted-foreground">{t("dashboard.latestGrade")}</p>
          </div>
          <div className="rounded-xl border border-border bg-muted/30 p-4 text-center">
            <p className="text-2xl font-bold text-foreground">{child.pendingAssignments}</p>
            <p className="mt-1 text-xs text-muted-foreground">{t("dashboard.pendingTasks")}</p>
          </div>
          <div className="rounded-xl border border-border bg-muted/30 p-4 text-center">
            <p className="text-2xl font-bold text-foreground">
              {child.learningProgress !== null ? `${Math.round(child.learningProgress)}%` : "—"}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">{t("dashboard.learningProgress")}</p>
          </div>
        </div>

        {child.classRank && child.totalStudentsInClass && (
          <div className="mt-4 rounded-xl border border-border bg-muted/30 p-4 text-center">
            <p className="text-sm text-muted-foreground">
              {t("childDetail.classRank", { rank: child.classRank, total: child.totalStudentsInClass })}
            </p>
          </div>
        )}
      </div>

      {progress && progress.courses.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">{t("childDetail.progressBySubject")}</h2>
            <span className="text-sm font-bold text-primary">{progress.overallProgress}%</span>
          </div>
          <div className="mt-4 space-y-3">
            {progress.courses.map((course: any) => (
              <div key={course.courseId || course.subjectId} className="rounded-xl border border-border p-3">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-foreground">{course.courseName || course.subjectName}</p>
                    <p className="text-xs text-muted-foreground">
                      {t("reports.lessonsCount", { completed: course.completedLessons, total: course.totalLessons })}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-lg font-bold text-foreground">{course.completionPercentage}%</p>
                    <p className={`text-[10px] font-semibold ${
                      course.status === "COMPLETED" ? "text-teal" : course.status === "IN_PROGRESS" ? "text-primary" : "text-muted-foreground"
                    }`}>
                      {course.status === "COMPLETED" ? ts("completed") : course.status === "IN_PROGRESS" ? ts("inProgress") : ts("notStarted")}
                    </p>
                  </div>
                </div>
                <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-muted">
                  <div
                    className={`h-full rounded-full transition-all ${
                      course.completionPercentage >= 100 ? "bg-teal" : course.completionPercentage > 0 ? "bg-primary" : "bg-muted"
                    }`}
                    style={{ width: `${Math.min(course.completionPercentage, 100)}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {entitlements.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">{t("childDetail.activeEntitlements")}</h2>
          <div className="mt-3 space-y-2">
            {entitlements.map((ent) => (
              <div key={ent.id} className="flex items-center gap-3 rounded-xl border border-border bg-muted/30 p-3">
                <Shield className="size-4 shrink-0 text-green-600" />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-foreground">{ent.serviceType}</p>
                  <p className="text-xs text-muted-foreground">
                    {ent.status === "ACTIVE" ? ts("active") : ent.status}
                    {ent.expiresAt && ` · ${t("services.expiresLabel", { date: new Date(ent.expiresAt).toLocaleDateString("en-GB") })}`}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      <div className="grid gap-4 sm:grid-cols-2">
        <Link
          href={`/dashboard/parent/attendance?child=${studentId}`}
          className="flex items-center gap-4 rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
        >
          <Clock className="size-8 shrink-0 text-primary" />
          <div className="flex-1">
            <p className="text-sm font-semibold text-foreground">{tn("attendance")}</p>
            <p className="text-xs text-muted-foreground">{t("childDetail.attendanceSummary", { present: child.daysPresent, absent: child.daysAbsent })}</p>
          </div>
          <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
        </Link>
        <Link
          href={`/dashboard/parent/assignments?child=${studentId}`}
          className="flex items-center gap-4 rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
        >
          <FileText className="size-8 shrink-0 text-primary" />
          <div className="flex-1">
            <p className="text-sm font-semibold text-foreground">{tn("assignments")}</p>
            <p className="text-xs text-muted-foreground">{t("childDetail.assignmentsSummary", { pending: child.pendingAssignments, overdue: child.overdueAssignments })}</p>
          </div>
          <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
        </Link>
        <Link
          href={`/dashboard/parent/assessments?child=${studentId}`}
          className="flex items-center gap-4 rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
        >
          <PenTool className="size-8 shrink-0 text-primary" />
          <div className="flex-1">
            <p className="text-sm font-semibold text-foreground">{tn("assessments")}</p>
            <p className="text-xs text-muted-foreground">{t("childDetail.assessmentsDesc")}</p>
          </div>
          <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
        </Link>
        <Link
          href={`/dashboard/parent/results?child=${studentId}`}
          className="flex items-center gap-4 rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
        >
          <BarChart3 className="size-8 shrink-0 text-primary" />
          <div className="flex-1">
            <p className="text-sm font-semibold text-foreground">{tn("results")}</p>
            <p className="text-xs text-muted-foreground">{t("childDetail.resultsDesc")}</p>
          </div>
          <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
        </Link>
        <Link
          href={`/dashboard/parent/live-classes?child=${studentId}`}
          className="flex items-center gap-4 rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
        >
          <Video className="size-8 shrink-0 text-primary" />
          <div className="flex-1">
            <p className="text-sm font-semibold text-foreground">{tn("liveClasses")}</p>
            <p className="text-xs text-muted-foreground">{t("childDetail.liveClassesDesc")}</p>
          </div>
          <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
        </Link>
        <Link
          href={`/dashboard/parent/notifications?child=${studentId}`}
          className="flex items-center gap-4 rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
        >
          <BookOpen className="size-8 shrink-0 text-primary" />
          <div className="flex-1">
            <p className="text-sm font-semibold text-foreground">{tn("announcements")}</p>
            <p className="text-xs text-muted-foreground">{t("childDetail.announcementsDesc")}</p>
          </div>
          <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
        </Link>
      </div>
    </div>
  )
}
