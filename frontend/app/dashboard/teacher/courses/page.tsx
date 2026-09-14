"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { teacherApi, type TeacherClassGroup } from "@/lib/api"
import { BookOpen, Users, FileText, ArrowRight, AlertCircle } from "lucide-react"

export default function TeacherCoursesPage() {
  const { user } = useAuth()
  const [classes, setClasses] = useState<TeacherClassGroup[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const data = await teacherApi.getClasses()
      setClasses(data)
    } catch {
      setError("Failed to load your classes")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">My Classes</h1>
        <p className="mt-1 text-sm text-muted-foreground">View your assigned classes and their details.</p>
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          <AlertCircle className="size-4 shrink-0" />{error}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : classes.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <BookOpen className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Classes Assigned</h3>
          <p className="mt-2 text-sm text-muted-foreground">You will see your assigned classes here once an administrator assigns you.</p>
        </div>
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {classes.map((cls) => (
            <Link
              key={cls.classGroupId}
              href={`/dashboard/teacher/classes/${cls.classGroupId}`}
              className="group rounded-2xl border border-border bg-card p-5 transition-all hover:-translate-y-0.5 hover:shadow-md"
            >
              <div className="flex items-start justify-between">
                <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                  <BookOpen className="size-5 text-primary" />
                </div>
                <ArrowRight className="size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5 group-hover:text-primary" />
              </div>
              <h3 className="mt-3 text-sm font-semibold text-foreground line-clamp-1">{cls.className}</h3>
              <p className="mt-0.5 text-xs text-muted-foreground">{cls.subjectName}</p>
              <div className="mt-4 flex items-center gap-4 text-xs text-muted-foreground">
                <span className="flex items-center gap-1"><Users className="size-3" />{cls.enrolledStudents} students</span>
                <span className="flex items-center gap-1"><FileText className="size-3" />{cls.totalAssignments} assignments</span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
