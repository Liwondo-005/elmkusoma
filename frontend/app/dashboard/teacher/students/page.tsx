"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { studentApi, type Student } from "@/lib/api"
import { Users, Search, Mail, Phone } from "lucide-react"

export default function TeacherStudentsPage() {
  const { user } = useAuth()
  const [students, setStudents] = useState<Student[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await studentApi.getStudents(user?.institutionId || "")
      setStudents(data)
    } catch {
      setStudents([])
    } finally {
      setLoading(false)
    }
  }

  const filtered = students.filter((s) =>
    `${s.firstName} ${s.lastName}`.toLowerCase().includes(search.toLowerCase()) ||
    s.email.toLowerCase().includes(search.toLowerCase()) ||
    s.admissionNumber.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Students</h1>
        <p className="mt-1 text-sm text-muted-foreground">View students in your courses.</p>
      </div>

      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          placeholder="Search students..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="h-10 w-full rounded-lg border border-border bg-background pl-9 pr-3 text-sm outline-none focus:border-ring"
        />
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <Users className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Students Found</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            {search ? "No students match your search." : "No students are enrolled yet."}
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
                <th className="px-4 py-3 font-medium text-muted-foreground">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {filtered.map((s) => (
                <tr key={s.id} className="hover:bg-muted/30">
                  <td className="px-4 py-3">
                    <p className="font-medium text-foreground">{s.firstName} {s.lastName}</p>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">{s.admissionNumber}</td>
                  <td className="px-4 py-3 text-muted-foreground">{s.email}</td>
                  <td className="px-4 py-3">
                    <span className="inline-flex items-center rounded-full bg-teal/10 px-2 py-0.5 text-xs font-medium text-teal">
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
