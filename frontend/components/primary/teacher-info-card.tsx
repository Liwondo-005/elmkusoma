"use client"

import { Mail } from "lucide-react"
import type { TeacherInfo } from "@/lib/api"

const SUBJECT_COLORS: Record<string, string> = {
  Mathematics: "bg-blue-500",
  English: "bg-purple-500",
  Kiswahili: "bg-teal-500",
  Science: "bg-green-500",
  "Social Studies": "bg-orange-500",
  "Civic Education": "bg-indigo-500",
  "Religion": "bg-pink-500",
}

function getInitials(teacher: TeacherInfo) {
  return `${teacher.firstName?.[0] || ""}${teacher.lastName?.[0] || ""}`.toUpperCase()
}

function getColorClass(subject: string) {
  const key = Object.keys(SUBJECT_COLORS).find((k) => subject.toLowerCase().includes(k.toLowerCase()))
  return key ? SUBJECT_COLORS[key] : "bg-primary"
}

export function TeacherInfoCard({ teacher }: { teacher: TeacherInfo }) {
  const initials = getInitials(teacher)
  const colorClass = getColorClass(teacher.subjectName)
  const fullName = `${teacher.firstName} ${teacher.lastName}`

  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md">
      <div className="flex items-center gap-4">
        <div className={`flex size-14 shrink-0 items-center justify-center rounded-full text-lg font-bold text-white ${colorClass}`}>
          {initials}
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-[10px] font-semibold uppercase tracking-wider text-primary">My Teacher / Mwalimu Wangu</p>
          <p className="mt-1 truncate text-lg font-bold text-foreground">{fullName}</p>
          <p className="mt-0.5 text-sm text-muted-foreground">{teacher.subjectName}</p>
        </div>
      </div>
      {teacher.specialization && (
        <div className="mt-3 rounded-xl bg-muted/30 px-3 py-2">
          <p className="text-xs font-medium text-muted-foreground">Specialization</p>
          <p className="text-sm font-medium text-foreground">{teacher.specialization}</p>
        </div>
      )}
      {teacher.email && (
        <a
          href={`mailto:${teacher.email}`}
          className="mt-3 flex items-center gap-2 text-sm font-medium text-primary hover:underline"
        >
          <Mail className="size-4" />
          {teacher.email}
        </a>
      )}
    </div>
  )
}
