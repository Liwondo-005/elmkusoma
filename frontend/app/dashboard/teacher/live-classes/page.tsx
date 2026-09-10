"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { teacherApi, type TeacherClassGroup } from "@/lib/api"
import { Video, Plus, Calendar, Clock, Users, AlertCircle } from "lucide-react"
import { Button } from "@/components/ui/button"

export default function TeacherLiveClassesPage() {
  const { user } = useAuth()
  const [classes, setClasses] = useState<TeacherClassGroup[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const data = await teacherApi.getClasses()
      setClasses(data)
    } catch {
      setError("Failed to load classes")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Live Classes</h1>
          <p className="mt-1 text-sm text-muted-foreground">Schedule and manage your live classes.</p>
        </div>
        <Button className="gap-2" disabled>
          <Plus className="size-4" />
          Schedule Class
        </Button>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />{error}
          </div>
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : classes.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <Video className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Classes Available</h3>
          <p className="mt-2 text-sm text-muted-foreground">You need class assignments to schedule live classes.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {classes.map((cls) => (
            <div key={cls.classGroupId} className="flex items-center gap-4 rounded-2xl border border-border bg-card p-4 transition-all hover:shadow-sm">
              <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 shrink-0">
                <Video className="size-5 text-primary" />
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="text-sm font-semibold text-foreground">{cls.className}</h3>
                <p className="text-xs text-muted-foreground">{cls.subjectName}</p>
                <div className="mt-1.5 flex items-center gap-3 text-xs text-muted-foreground">
                  <span className="flex items-center gap-1"><Users className="size-3" />{cls.enrolledStudents} students</span>
                  <span className="flex items-center gap-1"><Calendar className="size-3" />{cls.totalLessons} lessons</span>
                </div>
              </div>
              <Button size="sm" variant="outline" disabled>
                Start
              </Button>
            </div>
          ))}
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-base font-semibold text-foreground">Live Class Information</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Live class scheduling will be available once video conferencing integration is configured.
          You can use your assigned classes below to organize live sessions.
        </p>
      </div>
    </div>
  )
}
