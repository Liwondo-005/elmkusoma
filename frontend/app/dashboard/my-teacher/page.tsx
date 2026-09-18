"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { primaryApi, type TeacherInfo } from "@/lib/api"
import { type LearningLevel, primarySubjects } from "@/lib/learner-config"
import { GraduationCap, Mail, MessageSquare, Users, BookOpen, Loader2 } from "lucide-react"
import Link from "next/link"

function getSubjectColor(subjectName: string): string {
  const subject = primarySubjects.find(
    (s) => s.name.toLowerCase() === subjectName?.toLowerCase()
  )
  return subject?.color || "text-primary"
}

function getSubjectBgColor(subjectName: string): string {
  const subject = primarySubjects.find(
    (s) => s.name.toLowerCase() === subjectName?.toLowerCase()
  )
  return subject?.bgColor || "bg-primary/10"
}

function getInitials(firstName: string, lastName: string): string {
  return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase()
}

function getInitialColor(firstName: string): string {
  const colors = [
    "bg-blue-500", "bg-green-500", "bg-purple-500", "bg-amber-500",
    "bg-pink-500", "bg-teal-500", "bg-red-500", "bg-indigo-500",
  ]
  const idx = firstName.charCodeAt(0) % colors.length
  return colors[idx]
}

export default function MyTeacherPage() {
  const { user } = useRequireAuth()
  const [teachers, setTeachers] = useState<TeacherInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const level = user?.learningLevel as LearningLevel | null

  useEffect(() => {
    if (!user) return
    loadTeachers()
  }, [user])

  async function loadTeachers() {
    try {
      setLoading(true)
      setError(null)
      const data = await primaryApi.getTeachers()
      setTeachers(data)
    } catch {
      setError("Failed to load teachers. Please try again.")
      setTeachers([])
    } finally {
      setLoading(false)
    }
  }

  const uniqueSubjects = [...new Set(teachers.map((t) => t.subjectName).filter(Boolean))]

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center gap-3">
        <div className="flex size-12 items-center justify-center rounded-2xl bg-primary/10">
          <GraduationCap className="size-6 text-primary" />
        </div>
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">My Teacher</h1>
          <p className="text-sm text-muted-foreground">Mwalimu Wangu</p>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-blue-50">
              <Users className="size-5 text-blue-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{teachers.length}</p>
              <p className="text-xs text-muted-foreground">Total Teachers</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-green-50">
              <BookOpen className="size-5 text-green-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{uniqueSubjects.length}</p>
              <p className="text-xs text-muted-foreground">Subjects Covered</p>
            </div>
          </div>
        </div>
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4">
          <p className="text-sm text-red-700">{error}</p>
          <button
            type="button"
            onClick={loadTeachers}
            className="mt-2 text-sm font-medium text-red-700 underline hover:no-underline"
          >
            Try again
          </button>
        </div>
      )}

      {teachers.length === 0 && !error ? (
        <div className="flex flex-col items-center rounded-2xl border border-dashed border-border py-16 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10">
            <GraduationCap className="size-8 text-primary" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">No teachers yet</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Your teachers will appear here once assigned to your classes.
          </p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {teachers.map((teacher) => (
            <div
              key={teacher.id}
              className="group rounded-2xl border border-border bg-card p-6 shadow-xs transition-all hover:shadow-md"
            >
              <div className="flex items-start gap-4">
                <div className={`flex size-14 shrink-0 items-center justify-center rounded-full text-lg font-bold text-white ${getInitialColor(teacher.firstName)}`}>
                  {getInitials(teacher.firstName, teacher.lastName)}
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-base font-semibold text-foreground">
                    {teacher.firstName} {teacher.lastName}
                  </h3>
                  {teacher.subjectName && (
                    <span className={`mt-1 inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold ${getSubjectBgColor(teacher.subjectName)} ${getSubjectColor(teacher.subjectName)}`}>
                      {teacher.subjectName}
                    </span>
                  )}
                </div>
              </div>

              {teacher.specialization && (
                <p className="mt-3 text-sm text-muted-foreground">
                  {teacher.specialization}
                </p>
              )}

              <div className="mt-4 flex items-center gap-2">
                {teacher.email && (
                  <a
                    href={`mailto:${teacher.email}`}
                    className="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-muted-foreground transition-colors hover:bg-muted"
                  >
                    <Mail className="size-3" />
                    Email
                  </a>
                )}
                <Link
                  href="/dashboard/messages"
                  className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground transition-colors hover:bg-primary/90"
                >
                  <MessageSquare className="size-3" />
                  Send Message
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
