"use client"

import { Video, Plus, Calendar, Clock, Users } from "lucide-react"
import { Button } from "@/components/ui/button"

const upcomingClasses: Array<{ id: string; title: string; subject: string; date: string; time: string; students: number }> = []

export default function TeacherLiveClassesPage() {
  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Live Classes</h1>
          <p className="mt-1 text-sm text-muted-foreground">Schedule and manage your live classes.</p>
        </div>
        <Button className="gap-2">
          <Plus className="size-4" />
          Schedule Class
        </Button>
      </div>

      {upcomingClasses.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <Video className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Live Classes Scheduled</h3>
          <p className="mt-2 text-sm text-muted-foreground">Schedule a live class to get started.</p>
          <Button className="mt-4 gap-2">
            <Plus className="size-4" />
            Schedule First Class
          </Button>
        </div>
      ) : (
        <div className="space-y-3">
          {upcomingClasses.map((cls) => (
            <div key={cls.id} className="flex items-center gap-4 rounded-2xl border border-border bg-card p-4 transition-all hover:shadow-sm">
              <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 shrink-0">
                <Video className="size-5 text-primary" />
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="text-sm font-semibold text-foreground">{cls.title}</h3>
                <p className="text-xs text-muted-foreground">{cls.subject}</p>
                <div className="mt-1.5 flex items-center gap-3 text-xs text-muted-foreground">
                  <span className="flex items-center gap-1"><Calendar className="size-3" />{cls.date}</span>
                  <span className="flex items-center gap-1"><Clock className="size-3" />{cls.time}</span>
                  <span className="flex items-center gap-1"><Users className="size-3" />{cls.students} students</span>
                </div>
              </div>
              <Button size="sm" variant="outline">Start</Button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
