"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { learnerApi, type EventItem } from "@/lib/learner-api"
import { CalendarDays, Clock, MapPin, Users, Search, Filter, Loader2, CalendarOff, Tag } from "lucide-react"

const EVENT_TYPES = [
  { value: "", label: "All Types" },
  { value: "SEMINAR", label: "Seminars" },
  { value: "WORKSHOP", label: "Workshops" },
  { value: "WEBINAR", label: "Webinars" },
  { value: "TRAINING", label: "Training" },
  { value: "CONFERENCE", label: "Conferences" },
  { value: "LECTURE", label: "Lectures" },
]

const EVENT_CATEGORIES = [
  { value: "", label: "All Categories" },
  { value: "ACADEMIC", label: "Academic" },
  { value: "TECHNICAL", label: "Technical" },
  { value: "PROFESSIONAL", label: "Professional" },
  { value: "COMMUNITY", label: "Community" },
  { value: "CULTURAL", label: "Cultural" },
  { value: "SPORTS", label: "Sports" },
]

function formatDate(d: string) {
  return new Date(d).toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric", year: "numeric" })
}

function formatTime(d: string) {
  return new Date(d).toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" })
}

function getTypeColor(type: string) {
  const m: Record<string, string> = {
    SEMINAR: "bg-blue-100 text-blue-800", WORKSHOP: "bg-green-100 text-green-800",
    WEBINAR: "bg-purple-100 text-purple-800", TRAINING: "bg-orange-100 text-orange-800",
    CONFERENCE: "bg-red-100 text-red-800", LECTURE: "bg-teal-100 text-teal-800",
  }
  return m[type] || "bg-gray-100 text-gray-800"
}

function getStatusBadge(event: EventItem) {
  const now = new Date()
  const start = new Date(event.startsAt)
  const end = event.endsAt ? new Date(event.endsAt) : new Date(start.getTime() + (event.durationMinutes || 60) * 60000)
  if (event.status === "CANCELLED") return { label: "Cancelled", className: "bg-red-100 text-red-700" }
  if (event.status === "COMPLETED" || now > end) return { label: "Past", className: "bg-gray-100 text-gray-600" }
  if (now >= start && now <= end) return { label: "Live Now", className: "bg-red-100 text-red-700 animate-pulse" }
  if (event.isRegistered) return { label: "Registered", className: "bg-green-100 text-green-700" }
  return { label: "Upcoming", className: "bg-blue-100 text-blue-700" }
}

export default function EventsPage() {
  const [events, setEvents] = useState<EventItem[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [typeFilter, setTypeFilter] = useState("")
  const [categoryFilter, setCategoryFilter] = useState("")
  const [error, setError] = useState("")

  useEffect(() => {
    loadEvents()
  }, [typeFilter, categoryFilter])

  async function loadEvents() {
    setLoading(true)
    setError("")
    try {
      const data = await learnerApi.getEvents({
        eventType: typeFilter || undefined,
        category: categoryFilter || undefined,
        search: search || undefined,
      })
      setEvents(data)
    } catch (e: any) {
      setError(e.message || "Failed to load events")
    } finally {
      setLoading(false)
    }
  }

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    loadEvents()
  }

  const upcoming = events.filter((ev) => {
    const now = new Date()
    const end = ev.endsAt ? new Date(ev.endsAt) : new Date(new Date(ev.startsAt).getTime() + (ev.durationMinutes || 60) * 60000)
    return ev.status === "PUBLISHED" && now <= end
  })

  const past = events.filter((ev) => {
    const now = new Date()
    const end = ev.endsAt ? new Date(ev.endsAt) : new Date(new Date(ev.startsAt).getTime() + (ev.durationMinutes || 60) * 60000)
    return ev.status === "COMPLETED" || (ev.status === "PUBLISHED" && now > end)
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Events &amp; Workshops</h1>
          <p className="text-muted-foreground">Discover seminars, workshops, webinars, and training sessions</p>
        </div>
        <Link href="/dashboard/learner/events/registered"
          className="inline-flex items-center gap-2 rounded-lg border border-border bg-card px-4 py-2 text-sm font-medium hover:bg-muted">
          My Registrations
        </Link>
      </div>

      <form onSubmit={handleSearch} className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search events..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="h-10 w-full rounded-lg border border-border bg-card pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-primary/20"
          />
        </div>
        <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}
          className="h-10 rounded-lg border border-border bg-card px-3 text-sm outline-none">
          {EVENT_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
        </select>
        <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)}
          className="h-10 rounded-lg border border-border bg-card px-3 text-sm outline-none">
          {EVENT_CATEGORIES.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
        </select>
      </form>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}

      {loading ? (
        <div className="flex items-center justify-center py-16">
          <Loader2 className="size-8 animate-spin text-primary" />
        </div>
      ) : events.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <CalendarOff className="size-12 text-muted-foreground/40" />
          <p className="mt-4 text-lg font-medium">No events found</p>
          <p className="text-sm text-muted-foreground">Check back later for upcoming events</p>
        </div>
      ) : (
        <>
          {upcoming.length > 0 && (
            <section>
              <h2 className="mb-4 text-lg font-semibold">Upcoming Events ({upcoming.length})</h2>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {upcoming.map((event) => {
                  const badge = getStatusBadge(event)
                  return (
                    <Link key={event.id} href={`/dashboard/learner/events/${event.id}`}
                      className="group rounded-xl border border-border bg-card p-5 transition-all hover:border-primary/30 hover:shadow-md">
                      <div className="flex items-start justify-between gap-2">
                        <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${getTypeColor(event.eventType)}`}>
                          {event.eventType}
                        </span>
                        <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${badge.className}`}>
                          {badge.label}
                        </span>
                      </div>
                      <h3 className="mt-3 text-base font-semibold group-hover:text-primary transition-colors line-clamp-2">{event.title}</h3>
                      {event.description && <p className="mt-1.5 text-sm text-muted-foreground line-clamp-2">{event.description}</p>}
                      <div className="mt-3 space-y-1.5 text-sm text-muted-foreground">
                        <div className="flex items-center gap-2">
                          <CalendarDays className="size-3.5 shrink-0" />
                          <span>{formatDate(event.startsAt)}</span>
                          <span className="text-xs">at {formatTime(event.startsAt)}</span>
                        </div>
                        {event.location && (
                          <div className="flex items-center gap-2">
                            <MapPin className="size-3.5 shrink-0" />
                            <span className="truncate">{event.location}</span>
                          </div>
                        )}
                        <div className="flex items-center gap-2">
                          <Clock className="size-3.5 shrink-0" />
                          <span>{event.durationMinutes || 60} minutes</span>
                        </div>
                        {event.maxParticipants && (
                          <div className="flex items-center gap-2">
                            <Users className="size-3.5 shrink-0" />
                            <span>{event.registeredCount}/{event.maxParticipants} registered</span>
                          </div>
                        )}
                      </div>
                      <div className="mt-3 flex items-center justify-between">
                        <span className="text-sm font-medium">{event.isFree ? "Free" : "Paid"}</span>
                        {event.hasRecording && <span className="text-xs text-green-600 font-medium">Recording available</span>}
                      </div>
                    </Link>
                  )
                })}
              </div>
            </section>
          )}

          {past.length > 0 && (
            <section>
              <h2 className="mb-4 text-lg font-semibold">Past Events ({past.length})</h2>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {past.map((event) => {
                  const badge = getStatusBadge(event)
                  return (
                    <Link key={event.id} href={`/dashboard/learner/events/${event.id}`}
                      className="group rounded-xl border border-border bg-card p-5 opacity-80 transition-all hover:border-primary/30 hover:opacity-100 hover:shadow-md">
                      <div className="flex items-start justify-between gap-2">
                        <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${getTypeColor(event.eventType)}`}>
                          {event.eventType}
                        </span>
                        <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${badge.className}`}>
                          {badge.label}
                        </span>
                      </div>
                      <h3 className="mt-3 text-base font-semibold group-hover:text-primary transition-colors line-clamp-2">{event.title}</h3>
                      <div className="mt-3 space-y-1.5 text-sm text-muted-foreground">
                        <div className="flex items-center gap-2">
                          <CalendarDays className="size-3.5 shrink-0" />
                          <span>{formatDate(event.startsAt)}</span>
                        </div>
                        {event.hasRecording && <span className="text-xs text-green-600 font-medium">Recording available</span>}
                        {event.materialCount > 0 && <span className="text-xs text-blue-600 font-medium">{event.materialCount} materials</span>}
                      </div>
                    </Link>
                  )
                })}
              </div>
            </section>
          )}
        </>
      )}
    </div>
  )
}
