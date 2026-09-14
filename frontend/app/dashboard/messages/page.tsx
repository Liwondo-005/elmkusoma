"use client"

import { MessageSquare } from "lucide-react"
import Link from "next/link"

export default function DashboardMessagesPage() {
  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Messages</h1>
        <p className="mt-1 text-sm text-muted-foreground">Chat with instructors and support.</p>
      </div>

      <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
        <MessageSquare className="size-10 text-muted-foreground/50" />
        <p className="mt-4 text-sm font-medium text-foreground">No messages yet</p>
        <p className="mt-1 max-w-sm text-sm text-muted-foreground">
          Messages from teachers, instructors, and support will appear here.
        </p>
        <Link
          href="/dashboard/courses"
          className="mt-4 inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90"
        >
          Browse Courses
        </Link>
      </div>
    </div>
  )
}
