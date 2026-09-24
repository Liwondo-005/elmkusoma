"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { ChevronLeft, ChevronRight, Clock, Loader2, Video, FileText, AlertTriangle, BookOpen } from "lucide-react"
import { useAuth } from "@/lib/auth"
import { parentApi, type ChildOverview, type ParentCalendar } from "@/lib/parent-api"

const typeColor: Record<string, string> = {
  LIVE_CLASS: "bg-blue-100 text-blue-700",
  ASSIGNMENT: "bg-orange/10 text-orange",
  ASSESSMENT: "bg-purple-100 text-purple-700",
  SCHOOL_EVENT: "bg-teal/10 text-teal",
  MEETING: "bg-primary/10 text-primary",
  EXAM: "bg-red-500/10 text-red-500",
}

const typeIcons: Record<string, typeof Clock> = {
  LIVE_CLASS: Video,
  ASSIGNMENT: FileText,
  ASSESSMENT: AlertTriangle,
  SCHOOL_EVENT: BookOpen,
  MEETING: Clock,
  EXAM: AlertTriangle,
}

function getDaysInMonth(year: number, month: number) {
  return new Date(year, month + 1, 0).getDate()
}

function getFirstDayOfMonth(year: number, month: number) {
  return new Date(year, month, 1).getDay()
}

export default function ParentCalendarPage() {
  const { user } = useAuth()
  const t = useTranslations("parent")
  const today = new Date()
  const [currentMonth, setCurrentMonth] = useState(today.getMonth())
  const [currentYear, setCurrentYear] = useState(today.getFullYear())
  const [selectedDate, setSelectedDate] = useState<string | null>(null)
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [selectedChildId, setSelectedChildId] = useState<string | null>(null)
  const [calendar, setCalendar] = useState<ParentCalendar | null>(null)
  const [loading, setLoading] = useState(true)

  const weekdays = [
    t("calendar.weekdaySun"),
    t("calendar.weekdayMon"),
    t("calendar.weekdayTue"),
    t("calendar.weekdayWed"),
    t("calendar.weekdayThu"),
    t("calendar.weekdayFri"),
    t("calendar.weekdaySat"),
  ]

  useEffect(() => {
    parentApi.getChildren().then((kids) => {
      setChildren(kids)
      if (kids.length > 0) {
        const primary = kids.find((c) => c.isPrimary) || kids[0]
        setSelectedChildId(primary.studentId)
      }
      setLoading(false)
    }).catch(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (!selectedChildId) return
    parentApi.getChildCalendar(selectedChildId, 60).then(setCalendar).catch(() => setCalendar(null))
  }, [selectedChildId])

  const daysInMonth = getDaysInMonth(currentYear, currentMonth)
  const firstDay = getFirstDayOfMonth(currentYear, currentMonth)
  const monthName = new Date(currentYear, currentMonth).toLocaleString("en-US", { month: "long", year: "numeric" })

  function prevMonth() {
    if (currentMonth === 0) { setCurrentMonth(11); setCurrentYear((y) => y - 1) } else { setCurrentMonth((m) => m - 1) }
  }
  function nextMonth() {
    if (currentMonth === 11) { setCurrentMonth(0); setCurrentYear((y) => y + 1) } else { setCurrentMonth((m) => m + 1) }
  }

  const events = calendar?.events || []
  const eventsByDate = events.reduce<Record<string, typeof events>>((acc, e) => {
    const dateStr = e.start?.substring(0, 10)
    if (dateStr) { (acc[dateStr] = acc[dateStr] || []).push(e) }
    return acc
  }, {})

  const selectedEvents = selectedDate ? eventsByDate[selectedDate] || [] : []

  if (loading) {
    return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <h1 className="text-xl font-bold text-foreground">{t("calendar.title")}</h1>

      {children.length > 1 && (
        <div className="flex gap-2 overflow-x-auto pb-1">
          {children.map((child) => (
            <button key={child.studentId} onClick={() => setSelectedChildId(child.studentId)}
              className={`shrink-0 rounded-xl border px-4 py-2 text-sm font-medium transition-colors ${selectedChildId === child.studentId ? "border-primary bg-primary text-primary-foreground" : "border-border bg-card text-foreground hover:bg-muted"}`}>
              {child.studentName}
            </button>
          ))}
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs lg:col-span-2">
          <div className="flex items-center justify-between">
            <button onClick={prevMonth} aria-label={t("calendar.prevMonth")} className="rounded-lg p-2 hover:bg-muted"><ChevronLeft className="size-4" /></button>
            <h2 className="text-base font-semibold text-foreground">{monthName}</h2>
            <button onClick={nextMonth} aria-label={t("calendar.nextMonth")} className="rounded-lg p-2 hover:bg-muted"><ChevronRight className="size-4" /></button>
          </div>
          <div className="mt-4 grid grid-cols-7 gap-1 text-center">
            {weekdays.map((d) => (
              <div key={d} className="py-1 text-[10px] font-semibold uppercase text-muted-foreground">{d}</div>
            ))}
            {Array.from({ length: firstDay }).map((_, i) => <div key={`empty-${i}`} />)}
            {Array.from({ length: daysInMonth }).map((_, i) => {
              const day = i + 1
              const dateStr = `${currentYear}-${String(currentMonth + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`
              const hasEvents = !!eventsByDate[dateStr]
              const isToday = day === today.getDate() && currentMonth === today.getMonth() && currentYear === today.getFullYear()
              const isSelected = selectedDate === dateStr
              return (
                <button key={day} onClick={() => setSelectedDate(dateStr)}
                  className={`relative rounded-lg py-2 text-sm font-medium transition-colors ${isSelected ? "bg-primary text-primary-foreground" : isToday ? "bg-muted text-foreground font-bold" : "text-foreground hover:bg-muted"}`}>
                  {day}
                  {hasEvents && !isSelected && <span className="absolute bottom-1 left-1/2 size-1.5 -translate-x-1/2 rounded-full bg-primary" />}
                </button>
              )
            })}
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">
            {selectedDate ? new Date(selectedDate + "T00:00:00").toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric" }) : t("calendar.upcomingEvents")}
          </h2>
          <div className="mt-4 space-y-3">
            {(selectedDate ? selectedEvents : events.slice(0, 6)).length === 0 ? (
              <p className="py-4 text-center text-xs text-muted-foreground">{t("calendar.noEventsDay")}</p>
            ) : (
              (selectedDate ? selectedEvents : events.slice(0, 6)).map((event) => {
                const Icon = typeIcons[event.type] || Clock
                return (
                  <div key={event.id} className="rounded-xl border border-border p-3">
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-2">
                        <Icon className="size-4 shrink-0 text-muted-foreground" />
                        <p className="text-sm font-medium text-foreground">{event.title}</p>
                      </div>
                      <span className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] font-semibold ${typeColor[event.type] || "bg-muted text-muted-foreground"}`}>
                        {event.type?.replace("_", " ")}
                      </span>
                    </div>
                    <div className="mt-2 space-y-1 text-xs text-muted-foreground">
                      {event.start && (
                        <div className="flex items-center gap-1.5">
                          <Clock className="size-3" />
                          {new Date(event.start).toLocaleString("en-GB", { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" })}
                          {event.end && ` – ${new Date(event.end).toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" })}`}
                        </div>
                      )}
                      {event.status && (
                        <span className={`inline-block rounded px-1.5 py-0.5 text-[10px] font-semibold ${event.status === "IN_PROGRESS" || event.status === "LIVE" ? "bg-green-100 text-green-700" : "bg-muted text-muted-foreground"}`}>
                          {event.status}
                        </span>
                      )}
                    </div>
                  </div>
                )
              })
            )}
          </div>
        </section>
      </div>
    </div>
  )
}
