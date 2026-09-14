"use client"

import { MessageSquare, Construction } from "lucide-react"

export default function TeacherMessagesPage() {
  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Messages</h1>
        <p className="mt-1 text-sm text-muted-foreground">Communicate with parents, students, and colleagues.</p>
      </div>

      <div className="rounded-2xl border border-dashed border-border bg-card p-12 text-center">
        <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-muted">
          <Construction className="size-7 text-muted-foreground" />
        </div>
        <h3 className="mt-4 text-lg font-semibold text-foreground">Messaging Coming Soon</h3>
        <p className="mt-2 max-w-md mx-auto text-sm text-muted-foreground">
          The messaging system is under development. You will be able to communicate directly with parents, students, and colleagues once this feature is launched.
        </p>
        <div className="mt-6 rounded-xl border border-border bg-muted/40 p-4 max-w-sm mx-auto">
          <p className="text-xs text-muted-foreground">
            <strong>What to expect:</strong> Direct messages, announcements, read receipts, and group conversations.
          </p>
        </div>
      </div>
    </div>
  )
}
