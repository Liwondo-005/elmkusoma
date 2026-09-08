"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { studentApi, type Student } from "@/lib/api"

export default function StudentsPage() {
  const { user } = useAuth()
  const [students, setStudents] = useState<Student[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [search, setSearch] = useState("")
  const [totalCount, setTotalCount] = useState(0)
  const [activeCount, setActiveCount] = useState(0)

  const institutionId = user?.institutionId || ""

  useEffect(() => {
    if (!institutionId) return
    setLoading(true)
    Promise.all([
      studentApi.getStudents(institutionId),
      studentApi.countStudents(institutionId),
      studentApi.countActiveStudents(institutionId),
    ])
      .then(([list, total, active]) => {
        setStudents(list)
        setTotalCount(total)
        setActiveCount(active)
      })
      .catch((e) => setError(e.message || "Failed to load students"))
      .finally(() => setLoading(false))
  }, [institutionId])

  const filtered = search
    ? students.filter(
        (s) =>
          s.admissionNumber.toLowerCase().includes(search.toLowerCase()) ||
          `${s.firstName} ${s.lastName}`.toLowerCase().includes(search.toLowerCase()) ||
          s.email.toLowerCase().includes(search.toLowerCase()),
      )
    : students

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Student Management</h1>
        <p className="text-muted-foreground">View and manage all registered students.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-lg border border-border bg-card p-4">
          <p className="text-sm text-muted-foreground">Total Students</p>
          <p className="text-2xl font-bold">{totalCount}</p>
        </div>
        <div className="rounded-lg border border-border bg-card p-4">
          <p className="text-sm text-muted-foreground">Active Students</p>
          <p className="text-2xl font-bold text-green-600">{activeCount}</p>
        </div>
        <div className="rounded-lg border border-border bg-card p-4">
          <p className="text-sm text-muted-foreground">Inactive Students</p>
          <p className="text-2xl font-bold text-orange-600">{totalCount - activeCount}</p>
        </div>
      </div>

      <div className="flex items-center gap-3">
        <input
          type="text"
          placeholder="Search by name, admission number, or email..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="flex-1 rounded-lg border border-border bg-card px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-primary/20"
        />
      </div>

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : (
        <div className="rounded-lg border border-border bg-card overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/50">
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Admission #</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Name</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Email</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Gender</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Status</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Enrolled</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-muted-foreground">
                    No students found.
                  </td>
                </tr>
              ) : (
                filtered.map((s) => (
                  <tr key={s.id} className="hover:bg-muted/30">
                    <td className="px-4 py-3 font-mono text-xs">{s.admissionNumber}</td>
                    <td className="px-4 py-3 font-medium">{s.firstName} {s.middleName ? `${s.middleName} ` : ""}{s.lastName}</td>
                    <td className="px-4 py-3 text-muted-foreground">{s.email}</td>
                    <td className="px-4 py-3 text-muted-foreground">{s.gender || "-"}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                        s.status === "ACTIVE"
                          ? "bg-green-100 text-green-800"
                          : s.status === "GRADUATED"
                            ? "bg-blue-100 text-blue-800"
                            : "bg-orange-100 text-orange-800"
                      }`}>
                        {s.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">{s.enrollmentDate}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
