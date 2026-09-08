"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { enrollmentApi, type Enrollment, type PageResponse } from "@/lib/api"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { BookOpen, Users, ArrowRight, CheckCircle, Clock, XCircle } from "lucide-react"

const statusConfig: Record<string, { label: string; variant: "default" | "secondary" | "destructive" | "outline"; icon: typeof CheckCircle }> = {
  PENDING: { label: "Pending", variant: "outline", icon: Clock },
  ENROLLED: { label: "Enrolled", variant: "default", icon: BookOpen },
  WITHDRAWN: { label: "Withdrawn", variant: "destructive", icon: XCircle },
  COMPLETED: { label: "Completed", variant: "secondary", icon: CheckCircle },
}

export default function EnrollmentPage() {
  const { user } = useRequireAuth()
  const [enrollments, setEnrollments] = useState<Enrollment[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)

  useEffect(() => {
    if (!user) return
    loadEnrollments()
  }, [user, page])

  async function loadEnrollments() {
    try {
      setLoading(true)
      const data = await enrollmentApi.list(page, 10)
      setEnrollments(data.content)
      setTotalPages(data.totalPages)
    } catch {
      setEnrollments([])
    } finally {
      setLoading(false)
    }
  }

  function getStatusBadge(status: string) {
    const config = statusConfig[status] || statusConfig.PENDING
    const Icon = config.icon
    return (
      <Badge variant={config.variant} className="gap-1">
        <Icon className="size-3" />
        {config.label}
      </Badge>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Enrollments</h1>
          <p className="text-sm text-muted-foreground">Manage student enrollments across classes and academic years.</p>
        </div>
        <Button className="gap-2">
          <Users className="size-4" /> New Enrollment
        </Button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : enrollments.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <BookOpen className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Enrollments Yet</h3>
          <p className="mt-2 text-sm text-muted-foreground">Enroll students to get started with the learning platform.</p>
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border bg-muted/50">
                <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground">Student</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground">Class</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground">Academic Year</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground">Status</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground">Enrolled At</th>
                <th className="px-4 py-3 text-right text-xs font-medium text-muted-foreground">Actions</th>
              </tr>
            </thead>
            <tbody>
              {enrollments.map((e) => (
                <tr key={e.id} className="border-b border-border last:border-0 hover:bg-muted/30 transition-colors">
                  <td className="px-4 py-3 text-sm text-foreground font-medium">{e.studentId.slice(0, 8)}...</td>
                  <td className="px-4 py-3 text-sm text-muted-foreground">{e.classGroupId.slice(0, 8)}...</td>
                  <td className="px-4 py-3 text-sm text-muted-foreground">{e.academicYearId.slice(0, 8)}...</td>
                  <td className="px-4 py-3">{getStatusBadge(e.status)}</td>
                  <td className="px-4 py-3 text-sm text-muted-foreground">
                    {new Date(e.enrolledAt).toLocaleDateString()}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Button variant="ghost" size="sm" className="gap-1">
                      View <ArrowRight className="size-3" />
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </Button>
          <span className="text-sm text-muted-foreground">Page {page + 1} of {totalPages}</span>
          <Button variant="outline" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
            Next
          </Button>
        </div>
      )}
    </div>
  )
}
