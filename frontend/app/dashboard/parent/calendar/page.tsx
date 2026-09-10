"use client"

import { useState } from "react"
import { ChevronLeft, ChevronRight, Clock, MapPin, Users } from "lucide-react"

const mockEvents = [
  { id: "1", title: "Parent-Teacher Meeting", date: "2026-09-12", time: "15:00", location: "Main Hall", type: "MEETING", children: ["Amina Juma", "Juma Juma"] },
  { id: "2", title: "Mid-Term Exams Begin", date: "2026-09-15", time: "08:00", location: "School", type: "EXAM", children: ["Amina Juma"] },
  { id: "3", title: "Sports Day", date: "2026-09-18", time: "09:00", location: "School Field", type: "EVENT", children: ["Juma Juma"] },
  { id: "4", title: "Term 1 Reports", date: "2026-09-22", time: "12:00", location: "Online", type: "REPORT", children: ["Amina Juma", "Juma Juma"] },
  { id: "5", title: "Fee Payment Deadline", date: "2026-09-25", time: "23:59", location: "Online", type: "PAYMENT", children: ["All"] },
  { id: "6", title: "School Holiday Starts", date: "2026-09-26", time: "14:00", location: "School", type: "HOLIDAY", children: ["All"] },
]

const typeColor: Record<string, string> = {
  MEETING: "bg-primary/10 text-primary",
  EXAM: "bg-red-500/10 text-red-500",
  EVENT: "bg-teal/10 text-teal",
  REPORT: "bg-blue-500/10 text-blue-500",
  PAYMENT: "bg-orange/10 text-orange",
  HOLIDAY: "bg-purple-500/10 text-purple-500",
}

function getDaysInMonth(year: number, month: number) {
  return new Date(year, month + 1, 0).getDate()
}

function getFirstDayOfMonth(year: number, month: number) {
  return new Date(year, month, 1).getDay()
}

export default function ParentCalendarPage() {
  const today = new Date()
  const [currentMonth, setCurrentMonth] = useState(today.getMonth())
  const [currentYear, setCurrentYear] = useState(today.getFullYear())
  const [selectedDate, setSelectedDate] = useState<string | null>(null)

  const daysInMonth = getDaysInMonth(currentYear, currentMonth)
  const firstDay = getFirstDayOfMonth(currentYear, currentMonth)
  const monthName = new Date(currentYear, currentMonth).toLocaleString("en-US", { month: "long", year: "numeric" })

  function prevMonth() {
    if (currentMonth === 0) {
      setCurrentMonth(11)
      setCurrentYear((y) => y - 1)
    } else {
      setCurrentMonth((m) => m - 1)
    }
  }

  function nextMonth() {
    if (currentMonth === 11) {
      setCurrentMonth(0)
      setCurrentYear((y) => y + 1)
    } else {
      setCurrentMonth((m) => m + 1)
    }
  }

  const eventsByDate = mockEvents.reduce<Record<string, typeof mockEvents>>((acc, e) => {
    ;(acc[e.date] = acc[e.date] || []).push(e)
    return acc
  }, {})

  const selectedEvents = selectedDate ? eventsByDate[selectedDate] || [] : []

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <h1 className="text-xl font-bold text-foreground">School Calendar</h1>

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs lg:col-span-2">
          <div className="flex items-center justify-between">
            <button onClick={prevMonth} className="rounded-lg p-2 hover:bg-muted"><ChevronLeft className="size-4" /></button>
            <h2 className="text-base font-semibold text-foreground">{monthName}</h2>
            <button onClick={nextMonth} className="rounded-lg p-2 hover:bg-muted"><ChevronRight className="size-4" /></button>
          </div>

          <div className="mt-4 grid grid-cols-7 gap-1 text-center">
            {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((d) => (
              <div key={d} className="py-1 text-[10px] font-semibold uppercase text-muted-foreground">{d}</div>
            ))}
            {Array.from({ length: firstDay }).map((_, i) => (
              <div key={`empty-${i}`} />
            ))}
            {Array.from({ length: daysInMonth }).map((_, i) => {
              const day = i + 1
              const dateStr = `${currentYear}-${String(currentMonth + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`
              const hasEvents = !!eventsByDate[dateStr]
              const isToday = day === today.getDate() && currentMonth === today.getMonth() && currentYear === today.getFullYear()
              const isSelected = selectedDate === dateStr
              return (
                <button
                  key={day}
                  onClick={() => setSelectedDate(dateStr)}
                  className={`relative rounded-lg py-2 text-sm font-medium transition-colors ${
                    isSelected ? "bg-primary text-primary-foreground" : isToday ? "bg-muted text-foreground font-bold" : "text-foreground hover:bg-muted"
                  }`}
                >
                  {day}
                  {hasEvents && !isSelected && <span className="absolute bottom-1 left-1/2 size-1.5 -translate-x-1/2 rounded-full bg-primary" />}
                </button>
              )
            })}
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">
            {selectedDate ? new Date(selectedDate + "T00:00:00").toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric" }) : "Upcoming Events"}
          </h2>
          <div className="mt-4 space-y-3">
            {(selectedDate ? selectedEvents : mockEvents.slice(0, 4)).length === 0 ? (
              <p className="py-4 text-center text-xs text-muted-foreground">No events on this day.</p>
            ) : (
              (selectedDate ? selectedEvents : mockEvents.slice(0, 4)).map((event) => (
                <div key={event.id} className="rounded-xl border border-border p-3">
                  <div className="flex items-start justify-between">
                    <p className="text-sm font-medium text-foreground">{event.title}</p>
                    <span className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] font-semibold ${typeColor[event.type] || "bg-muted text-muted-foreground"}`}>
                      {event.type}
                    </span>
                  </div>
                  <div className="mt-2 space-y-1 text-xs text-muted-foreground">
                    <div className="flex items-center gap-1.5"><Clock className="size-3" /> {event.time}</div>
                    <div className="flex items-center gap-1.5"><MapPin className="size-3" /> {event.location}</div>
                    <div className="flex items-center gap-1.5"><Users className="size-3" /> {event.children.join(", ")}</div>
                  </div>
                </div>
              ))
            )}
          </div>
        </section>
      </div>
    </div>
  )
}
