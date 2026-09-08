"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { learningApi, type Assignment } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { FileText, Clock, CheckCircle, Send, ArrowRight } from "lucide-react"

export default function AssignmentsPage() {
  const { user } = useRequireAuth()
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await learningApi.getAssignments(user!.classGroupId || "")
      setAssignments(data)
    } catch {
      setAssignments([])
    } finally {
      setLoading(false)
    }
  }

  async function handleSubmit(assignmentId: string) {
    try {
      await learningApi.submitAssignment(assignmentId)
      loadData()
    } catch {
      // ignore
    }
  }

  function isOverdue(dueDate?: string) {
    if (!dueDate) return false
    return new Date(dueDate) < new Date()
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Assignments</h1>
        <p className="text-sm text-muted-foreground">View and submit your assignments.</p>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : assignments.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <FileText className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Assignments</h3>
          <p className="mt-2 text-sm text-muted-foreground">No assignments have been posted yet.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {assignments.map((a) => {
            const overdue = isOverdue(a.dueDate)
            return (
              <div
                key={a.id}
                className="flex items-center gap-4 rounded-2xl border border-border bg-card p-4 transition-all hover:shadow-sm"
              >
                <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 shrink-0">
                  <FileText className="size-5 text-primary" />
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-sm font-semibold text-foreground">{a.title}</h3>
                  {a.description && (
                    <p className="mt-0.5 text-xs text-muted-foreground line-clamp-1">{a.description}</p>
                  )}
                  <div className="mt-1.5 flex items-center gap-3 text-xs text-muted-foreground">
                    <span>Total: {a.totalMarks} marks</span>
                    {a.dueDate && (
                      <span className={`flex items-center gap-1 ${overdue ? "text-destructive" : ""}`}>
                        <Clock className="size-3" />
                        Due: {new Date(a.dueDate).toLocaleDateString()}
                      </span>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  {overdue ? (
                    <Badge variant="destructive">Overdue</Badge>
                  ) : (
                    <Badge variant="outline">Active</Badge>
                  )}
                  <Button size="sm" onClick={() => handleSubmit(a.id)} className="gap-1">
                    <Send className="size-3" /> Submit
                  </Button>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
