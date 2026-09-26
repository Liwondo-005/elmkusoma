"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { teacherApi, type TeacherStudent } from "@/lib/api"
import { Users, Search, Mail, Phone, AlertCircle } from "lucide-react"

export default function TeacherStudentsPage() {
  const { user } = useAuth()
  const t = useTranslations("teacher")
  const ts = useTranslations("status")
  const [students, setStudents] = useState<TeacherStudent[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const data = await teacherApi.getStudents()
      setStudents(data)
    } catch {
      setError(t("students.loadError"))
      setStudents([])
    } finally {
      setLoading(false)
    }
  }

  const filtered = students.filter((s) =>
    s.fullName.toLowerCase().includes(search.toLowerCase()) ||
    s.email.toLowerCase().includes(search.toLowerCase()) ||
    s.admissionNumber.toLowerCase().includes(search.toLowerCase())
  )

  const classGroups = Array.from(new Set(students.map(s => s.classGroupId)))
  const classNames = Object.fromEntries(
    classGroups.map(cg => [cg, students.find(s => s.classGroupId === cg)?.className || cg.slice(0, 8)])
  )
  const [selectedClass, setSelectedClass] = useState<string>("all")

  const displayed = selectedClass === "all"
    ? filtered
    : filtered.filter(s => s.classGroupId === selectedClass)

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("students.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {t("students.subtitle", { count: students.length })}
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

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder={t("students.searchPlaceholder")}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="h-10 w-full rounded-lg border border-border bg-background pl-9 pr-3 text-sm outline-none focus:border-ring"
          />
        </div>
        {classGroups.length > 1 && (
          <select
            value={selectedClass}
            onChange={(e) => setSelectedClass(e.target.value)}
            className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
          >
            <option value="all">{t("students.allClasses")}</option>
            {classGroups.map((cg) => (
              <option key={cg} value={cg}>{classNames[cg]}</option>
            ))}
          </select>
        )}
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : displayed.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <Users className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("students.emptyTitle")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            {search
              ? t("students.noMatchSearch")
              : students.length === 0
                ? t("students.noStudents")
                : t("students.noMatchFilter")}
          </p>
        </div>
      ) : (
        <div className="overflow-hidden rounded-2xl border border-border bg-card">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-border bg-muted/50">
              <tr>
                <th className="px-4 py-3 font-medium text-muted-foreground">{t("students.colName")}</th>
                <th className="px-4 py-3 font-medium text-muted-foreground">{t("students.colAdmission")}</th>
                <th className="px-4 py-3 font-medium text-muted-foreground">{t("email")}</th>
                <th className="px-4 py-3 font-medium text-muted-foreground hidden sm:table-cell">{t("students.colClass")}</th>
                <th className="px-4 py-3 font-medium text-muted-foreground">{t("students.colStatus")}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {displayed.map((s) => (
                <tr key={s.studentId} className="hover:bg-muted/30">
                  <td className="px-4 py-3">
                    <p className="font-medium text-foreground">{s.fullName}</p>
                    {s.gender && (
                      <p className="text-xs text-muted-foreground">{s.gender}</p>
                    )}
                  </td>
                  <td className="px-4 py-3 text-muted-foreground font-mono text-xs">{s.admissionNumber}</td>
                  <td className="px-4 py-3 text-muted-foreground">{s.email}</td>
                  <td className="px-4 py-3 text-muted-foreground hidden sm:table-cell text-xs">{s.className}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                      s.status === "ACTIVE" ? "bg-teal/10 text-teal" : "bg-muted text-muted-foreground"
                    }`}>
                      {s.status === "ACTIVE" ? ts("active") : s.status === "INACTIVE" ? ts("inactive") : s.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
