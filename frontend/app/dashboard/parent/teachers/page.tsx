"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { Users, Loader2, Mail, Phone, BookOpen } from "lucide-react"
import { useAuth } from "@/lib/auth"
import { parentApi, type ChildOverview, type ParentTeachers, type TeacherItem } from "@/lib/parent-api"

export default function ParentTeachersPage() {
  const { user } = useAuth()
  const t = useTranslations("parent")
  const tn = useTranslations("nav")
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [selectedChildId, setSelectedChildId] = useState<string | null>(null)
  const [teachers, setTeachers] = useState<ParentTeachers | null>(null)
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
    setTeachers(null)
    parentApi.getChildTeachers(selectedChildId).then(setTeachers).catch(() => setTeachers(null))
  }, [selectedChildId])

  if (loading) return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>

  const teacherList = teachers?.teachers || []

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("teachers")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("teachers.subtitle")}</p>
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

      {teacherList.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-16 text-center">
          <Users className="mx-auto mb-3 size-10 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">{t("teachers.emptyTitle")}</p>
          <p className="mt-1 text-xs text-muted-foreground">{t("teachers.emptyDesc")}</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {teacherList.map((teacher) => (
            <TeacherCard key={teacher.id} teacher={teacher} />
          ))}
        </div>
      )}
    </div>
  )
}

function TeacherCard({ teacher }: { teacher: TeacherItem }) {
  const t = useTranslations("parent")
  const initials = teacher.fullName?.split(" ").map((n) => n[0]).join("").toUpperCase().slice(0, 2) || "??"

  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md">
      <div className="flex items-start gap-4">
        <div className="flex size-12 shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-bold text-primary">
          {initials}
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-sm font-semibold text-foreground">{teacher.fullName}</p>
          <div className="mt-1 flex items-center gap-1.5 text-xs text-muted-foreground">
            <BookOpen className="size-3" />
            {teacher.subject || t("teachers.generalSubject")}
          </div>
          {teacher.specialization && (
            <p className="mt-0.5 text-xs text-muted-foreground">{teacher.specialization}</p>
          )}
        </div>
      </div>
      <div className="mt-4 space-y-2">
        {teacher.email && (
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <Mail className="size-3.5 shrink-0" />
            <a href={`mailto:${teacher.email}`} className="hover:text-primary hover:underline">{teacher.email}</a>
          </div>
        )}
        {teacher.phone && (
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <Phone className="size-3.5 shrink-0" />
            <a href={`tel:${teacher.phone}`} className="hover:text-primary hover:underline">{teacher.phone}</a>
          </div>
        )}
      </div>
    </div>
  )
}
