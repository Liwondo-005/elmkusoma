"use client"

import { Clock, Calendar } from "lucide-react"

const daysOfWeek = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"]
const timeSlots = ["07:00", "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00"]

const mockSchedule: Record<string, { time: string; className: string; subject: string }[]> = {
  Monday: [
    { time: "08:00-09:00", className: "Form 2A", subject: "Mathematics" },
    { time: "10:00-11:00", className: "Form 3B", subject: "Mathematics" },
    { time: "14:00-15:00", className: "Form 1C", subject: "Mathematics" },
  ],
  Tuesday: [
    { time: "08:00-09:00", className: "Form 2A", subject: "Mathematics" },
    { time: "09:00-10:00", className: "Form 3B", subject: "Mathematics" },
  ],
  Wednesday: [
    { time: "07:00-08:00", className: "Form 2A", subject: "Mathematics" },
    { time: "11:00-12:00", className: "Form 1C", subject: "Mathematics" },
  ],
  Thursday: [
    { time: "08:00-09:00", className: "Form 3B", subject: "Mathematics" },
    { time: "14:00-15:00", className: "Form 2A", subject: "Mathematics" },
  ],
  Friday: [
    { time: "09:00-10:00", className: "Form 1C", subject: "Mathematics" },
    { time: "10:00-11:00", className: "Form 2A", subject: "Mathematics" },
  ],
}

export default function TeacherSchedulePage() {
  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">My Schedule</h1>
        <p className="mt-1 text-sm text-muted-foreground">Your weekly class timetable.</p>
      </div>

      <div className="overflow-x-auto rounded-2xl border border-border bg-card shadow-xs">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border">
              <th className="px-3 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground w-20">Time</th>
              {daysOfWeek.map((day) => (
                <th key={day} className="px-3 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  {day}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {timeSlots.map((time) => (
              <tr key={time} className="border-b border-border last:border-0">
                <td className="px-3 py-3 text-xs font-medium text-muted-foreground">{time}</td>
                {daysOfWeek.map((day) => {
                  const lessons = (mockSchedule[day] || []).filter((l) => l.time.startsWith(time))
                  return (
                    <td key={day} className="px-2 py-2">
                      {lessons.map((lesson, i) => (
                        <div key={i} className="mb-1 rounded-lg bg-primary/5 border border-primary/10 p-2">
                          <p className="text-xs font-medium text-foreground">{lesson.subject}</p>
                          <p className="text-[10px] text-muted-foreground">{lesson.className}</p>
                          <p className="text-[10px] text-muted-foreground">{lesson.time}</p>
                        </div>
                      ))}
                    </td>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
