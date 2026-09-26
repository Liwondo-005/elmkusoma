"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { Users, ChevronRight, Loader2, BookOpen, AlertCircle } from "lucide-react"
import { useAuth } from "@/lib/auth"
import type { ClassGroupInfo, TeacherAssignment } from "@/lib/teacher-api"

export default function TeacherClassesPage() {
  const { user } = useAuth()
  const t = useTranslations("teacher")
  const tn = useTranslations("nav")
  const [classes, setClasses] = useState<ClassGroupInfo[]>([])
  const [assignments, setAssignments] = useState<TeacherAssignment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function load() {
      try {
        const { teacherApi } = await import("@/lib/teacher-api")
        const myClasses = await teacherApi.getClasses()
        setClasses(myClasses.map((c) => ({
          id: c.classGroupId,
          name: c.className,
          gradeName: c.subjectName,
          studentCount: c.enrolledStudents,
          educationLevel: null,
        })))
        setAssignments(myClasses.map((c) => ({
          id: c.classGroupId,
          classGroupId: c.classGroupId,
          classGroupName: c.className,
          subjectId: c.subjectId || "",
          subjectName: c.subjectName,
          academicYearId: null,
          academicYearName: c.academicYear || null,
        })))
      } catch {
        setError(t("classes.loadError"))
      }
      finally { setLoading(false) }
    }
    load()
  }, [user?.email])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("myClasses")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("classes.subtitle")}</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      {classes.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-16 text-center">
          <Users className="mx-auto mb-3 size-8 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">{t("classes.emptyTitle")}</p>
          <p className="mt-1 text-xs text-muted-foreground">{t("classes.emptyDesc")}</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {classes.map((cls) => {
            const classSubjects = assignments
              .filter((a) => a.classGroupId === cls.id)
              .map((a) => a.subjectName)
              .filter(Boolean)
            return (
              <Link
                key={cls.id}
                href={`/dashboard/teacher/classes/${cls.id}`}
                className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
              >
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-sm font-semibold text-foreground">{cls.name}</p>
                    {cls.gradeName && (
                      <p className="mt-0.5 text-xs text-muted-foreground">{cls.gradeName}</p>
                    )}
                  </div>
                  {cls.educationLevel && (
                    <span className="rounded bg-primary/10 px-1.5 py-0.5 text-[10px] font-semibold text-primary">
                      {cls.educationLevel}
                    </span>
                  )}
                </div>
                <div className="mt-4 flex items-center gap-4">
                  <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
                    <Users className="size-3.5" />
                    <span>{t("courses.students", { count: cls.studentCount })}</span>
                  </div>
                  {classSubjects.length > 0 && (
                    <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
                      <BookOpen className="size-3.5" />
                      <span>{classSubjects.join(", ")}</span>
                    </div>
                  )}
                </div>
                <div className="mt-4 flex items-center gap-1 text-xs font-medium text-primary">
                  {t("classes.viewClass")} <ChevronRight className="size-3" />
                </div>
              </Link>
            )
          })}
        </div>
      )}
    </div>
  )
}
