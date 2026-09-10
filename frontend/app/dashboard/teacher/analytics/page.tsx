"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { teacherApi, learningApi, assessmentApi, type TeacherClassGroup } from "@/lib/api"
import { BarChart3, TrendingUp, Users, BookOpen, PenTool, AlertCircle } from "lucide-react"

interface ClassAnalytics {
  classGroupId: string
  className: string
  subjectName: string
  enrolledStudents: number
  assignmentCount: number
  assessmentCount: number
}

export default function TeacherAnalyticsPage() {
  const { user } = useAuth()
  const [classes, setClasses] = useState<TeacherClassGroup[]>([])
  const [selectedClass, setSelectedClass] = useState<string>("all")
  const [analytics, setAnalytics] = useState<ClassAnalytics[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadAnalytics()
  }, [user])

  async function loadAnalytics() {
    try {
      setLoading(true)
      setError(null)

      const teacherClasses = await teacherApi.getClasses()
      setClasses(teacherClasses)

      const results: ClassAnalytics[] = []

      for (const cls of teacherClasses) {
        let assignmentCount = 0
        let assessmentCount = 0

        try {
          const assignments = await learningApi.getAssignments(cls.classGroupId)
          assignmentCount = assignments.length
        } catch {
          // skip
        }

        try {
          const assessments = await assessmentApi.getByClass(cls.classGroupId)
          assessmentCount = assessments.length
        } catch {
          // skip
        }

        results.push({
          classGroupId: cls.classGroupId,
          className: cls.className,
          subjectName: cls.subjectName,
          enrolledStudents: cls.enrolledStudents,
          assignmentCount,
          assessmentCount,
        })
      }

      setAnalytics(results)
    } catch {
      setError("Failed to load analytics data")
    } finally {
      setLoading(false)
    }
  }

  const filteredAnalytics =
    selectedClass === "all"
      ? analytics
      : analytics.filter((a) => a.classGroupId === selectedClass)

  const totalStudents = filteredAnalytics.reduce((sum, a) => sum + a.enrolledStudents, 0)
  const totalAssignments = filteredAnalytics.reduce((sum, a) => sum + a.assignmentCount, 0)
  const totalAssessments = filteredAnalytics.reduce((sum, a) => sum + a.assessmentCount, 0)
  const avgCompletion =
    filteredAnalytics.length > 0
      ? filteredAnalytics.reduce((sum, a) => {
          const total = a.assignmentCount + a.assessmentCount
          const totalCapacity = a.enrolledStudents * (a.assignmentCount + a.assessmentCount || 1)
          const completed = totalCapacity > 0 ? (total / totalCapacity) * 100 : 0
          return sum + Math.min(completed, 100)
        }, 0) / filteredAnalytics.length
      : 0

  const maxStudents = Math.max(...filteredAnalytics.map((a) => a.enrolledStudents), 1)
  const maxAssignments = Math.max(...filteredAnalytics.map((a) => a.assignmentCount), 1)
  const maxAssessments = Math.max(...filteredAnalytics.map((a) => a.assessmentCount), 1)

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          Teaching Analytics
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Overview of your classes, assignments, and student engagement.
        </p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      <div className="flex items-center gap-3">
        {loading ? (
          <div className="size-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
        ) : classes.length === 0 ? (
          <p className="text-sm text-muted-foreground">No classes found.</p>
        ) : (
          <select
            value={selectedClass}
            onChange={(e) => setSelectedClass(e.target.value)}
            className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
          >
            <option value="all">All Classes</option>
            {classes.map((c) => (
              <option key={c.classGroupId} value={c.classGroupId}>
                {c.className} - {c.subjectName}
              </option>
            ))}
          </select>
        )}
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : analytics.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <BarChart3 className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">
            No Data Available
          </h3>
          <p className="mt-2 text-sm text-muted-foreground">
            Analytics will appear once you have classes with assignments and assessments.
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <Users className="mb-2 size-5 text-muted-foreground" />
              <p className="text-2xl font-extrabold text-foreground">{totalStudents}</p>
              <p className="text-sm font-medium text-foreground">Total Students</p>
              <p className="text-xs text-muted-foreground">Across selected classes</p>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <BookOpen className="mb-2 size-5 text-muted-foreground" />
              <p className="text-2xl font-extrabold text-foreground">{totalAssignments}</p>
              <p className="text-sm font-medium text-foreground">Total Assignments</p>
              <p className="text-xs text-muted-foreground">All assigned work</p>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <PenTool className="mb-2 size-5 text-muted-foreground" />
              <p className="text-2xl font-extrabold text-foreground">{totalAssessments}</p>
              <p className="text-sm font-medium text-foreground">Total Assessments</p>
              <p className="text-xs text-muted-foreground">Quizzes and exams</p>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <TrendingUp className="mb-2 size-5 text-muted-foreground" />
              <p className="text-2xl font-extrabold text-foreground">
                {avgCompletion.toFixed(0)}%
              </p>
              <p className="text-sm font-medium text-foreground">Avg Completion</p>
              <p className="text-xs text-muted-foreground">Estimated rate</p>
            </div>
          </div>

          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <h2 className="text-base font-semibold text-foreground">Per-Class Breakdown</h2>
            <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {filteredAnalytics.map((cls) => (
                <div
                  key={cls.classGroupId}
                  className="rounded-xl border border-border p-4 hover:bg-muted/30 transition-colors"
                >
                  <p className="text-sm font-medium text-foreground truncate">{cls.className}</p>
                  <p className="text-xs text-muted-foreground truncate">{cls.subjectName}</p>
                  <div className="mt-3 space-y-1.5 text-xs text-muted-foreground">
                    <div className="flex items-center justify-between">
                      <span className="flex items-center gap-1">
                        <Users className="size-3" /> Students
                      </span>
                      <span className="font-medium text-foreground">{cls.enrolledStudents}</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="flex items-center gap-1">
                        <BookOpen className="size-3" /> Assignments
                      </span>
                      <span className="font-medium text-foreground">{cls.assignmentCount}</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="flex items-center gap-1">
                        <PenTool className="size-3" /> Assessments
                      </span>
                      <span className="font-medium text-foreground">{cls.assessmentCount}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <h2 className="text-base font-semibold text-foreground">Visual Overview</h2>
            <div className="mt-4 space-y-6">
              <div>
                <p className="mb-3 text-sm font-medium text-foreground">Students per Class</p>
                <div className="space-y-2">
                  {filteredAnalytics.map((cls) => (
                    <div key={cls.classGroupId} className="flex items-center gap-3">
                      <p className="w-32 truncate text-xs text-muted-foreground">{cls.className}</p>
                      <div className="flex-1 overflow-hidden rounded-full bg-muted/50">
                        <div
                          className="h-2 rounded-full bg-primary"
                          style={{
                            width: `${(cls.enrolledStudents / maxStudents) * 100}%`,
                          }}
                        />
                      </div>
                      <p className="w-8 text-right text-xs font-medium text-foreground">
                        {cls.enrolledStudents}
                      </p>
                    </div>
                  ))}
                </div>
              </div>

              <div>
                <p className="mb-3 text-sm font-medium text-foreground">Assignments per Class</p>
                <div className="space-y-2">
                  {filteredAnalytics.map((cls) => (
                    <div key={cls.classGroupId} className="flex items-center gap-3">
                      <p className="w-32 truncate text-xs text-muted-foreground">{cls.className}</p>
                      <div className="flex-1 overflow-hidden rounded-full bg-muted/50">
                        <div
                          className="h-2 rounded-full bg-emerald-500"
                          style={{
                            width: `${(cls.assignmentCount / maxAssignments) * 100}%`,
                          }}
                        />
                      </div>
                      <p className="w-8 text-right text-xs font-medium text-foreground">
                        {cls.assignmentCount}
                      </p>
                    </div>
                  ))}
                </div>
              </div>

              <div>
                <p className="mb-3 text-sm font-medium text-foreground">Assessments per Class</p>
                <div className="space-y-2">
                  {filteredAnalytics.map((cls) => (
                    <div key={cls.classGroupId} className="flex items-center gap-3">
                      <p className="w-32 truncate text-xs text-muted-foreground">{cls.className}</p>
                      <div className="flex-1 overflow-hidden rounded-full bg-muted/50">
                        <div
                          className="h-2 rounded-full bg-amber-500"
                          style={{
                            width: `${(cls.assessmentCount / maxAssessments) * 100}%`,
                          }}
                        />
                      </div>
                      <p className="w-8 text-right text-xs font-medium text-foreground">
                        {cls.assessmentCount}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
