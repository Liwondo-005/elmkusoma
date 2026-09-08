"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { assessmentApi, type Assessment } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { ClipboardList, Clock, CheckCircle, ArrowRight, Play } from "lucide-react"

export default function AssessmentsPage() {
  const { user } = useRequireAuth()
  const [assessments, setAssessments] = useState<Assessment[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await assessmentApi.getByClass(user!.id)
      setAssessments(data)
    } catch {
      setAssessments([])
    } finally {
      setLoading(false)
    }
  }

  function isAvailable(a: Assessment) {
    if (!a.startsAt) return a.isPublished
    const now = new Date()
    const start = new Date(a.startsAt)
    const end = a.endsAt ? new Date(a.endsAt) : null
    return now >= start && (!end || now <= end)
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Assessments</h1>
        <p className="text-sm text-muted-foreground">Take quizzes and view your results.</p>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : assessments.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <ClipboardList className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Assessments</h3>
          <p className="mt-2 text-sm text-muted-foreground">No assessments have been published yet.</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {assessments.map((a) => {
            const available = isAvailable(a)
            return (
              <div
                key={a.id}
                className="rounded-2xl border border-border bg-card p-5 transition-all hover:shadow-md"
              >
                <div className="flex items-start justify-between">
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                    <ClipboardList className="size-5 text-primary" />
                  </div>
                  {available ? (
                    <Badge variant="default" className="gap-1">
                      <Play className="size-3" /> Available
                    </Badge>
                  ) : (
                    <Badge variant="outline">Upcoming</Badge>
                  )}
                </div>
                <h3 className="mt-3 text-sm font-semibold text-foreground">{a.title}</h3>
                {a.description && (
                  <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{a.description}</p>
                )}
                <div className="mt-3 space-y-1 text-xs text-muted-foreground">
                  <div className="flex items-center justify-between">
                    <span>Total Marks</span>
                    <span className="font-medium text-foreground">{a.totalMarks}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span>Pass Marks</span>
                    <span className="font-medium text-foreground">{a.passMarks}</span>
                  </div>
                  {a.timeLimitMinutes && (
                    <div className="flex items-center justify-between">
                      <span>Time Limit</span>
                      <span className="font-medium text-foreground">{a.timeLimitMinutes} min</span>
                    </div>
                  )}
                </div>
                {available && (
                  <Link href={`/dashboard/assessments/${a.id}`} className="mt-4 block">
                    <Button className="w-full gap-1.5" size="sm">
                      Start Quiz <ArrowRight className="size-3.5" />
                    </Button>
                  </Link>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
