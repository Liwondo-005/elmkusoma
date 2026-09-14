"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { teacherApi, type TeacherStudent } from "@/lib/api"
import { Users, Search, Mail, Phone, AlertCircle } from "lucide-react"

export default function TeacherStudentsPage() {
  const { user } = useAuth()
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
      setError("Failed to load students")
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
  const [selectedClass, setSelectedClass] = useState<string>("all")

  const displayed = selectedClass === "all"
    ? filtered
    : filtered.filter(s => s.classGroupId === selectedClass)

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">My Students</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Students enrolled in your assigned classes. {students.length} total.
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
            placeholder="Search by name, email, or admission #..."
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
            <option value="all">All Classes</option>
            {classGroups.map((cg) => (
              <option key={cg} value={cg}>Class {cg.slice(0, 8)}</option>
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
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Students Found</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            {search
              ? "No students match your search."
              : students.length === 0
                ? "No students are enrolled in your assigned classes yet."
                : "No students match the current filter."}
          </p>
        </div>
      ) : (
        <div className="overflow-hidden rounded-2xl border border-border bg-card">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-border bg-muted/50">
              <tr>
                <th className="px-4 py-3 font-medium text-muted-foreground">Name</th>
                <th className="px-4 py-3 font-medium text-muted-foreground">Admission #</th>
                <th className="px-4 py-3 font-medium text-muted-foreground">Email</th>
                <th className="px-4 py-3 font-medium text-muted-foreground hidden sm:table-cell">Class</th>
                <th className="px-4 py-3 font-medium text-muted-foreground">Status</th>
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
                      {s.status}
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
