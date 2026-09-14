"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { learnerApi, type EventItem } from "@/lib/learner-api"
import { CalendarDays, Clock, MapPin, ArrowLeft, Loader2, CheckCircle, CalendarOff, ExternalLink } from "lucide-react"

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

export default function RegisteredEventsPage() {
  const [upcoming, setUpcoming] = useState<EventItem[]>([])
  const [past, setPast] = useState<EventItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  useEffect(() => {
    loadRegistrations()
  }, [])

  async function loadRegistrations() {
    setLoading(true)
    try {
      const [up, pa] = await Promise.all([
        learnerApi.getRegisteredEvents(),
        learnerApi.getRegisteredPastEvents(),
      ])
      setUpcoming(up)
      setPast(pa)
    } catch (e: any) {
      setError(e.message || "Failed to load registrations")
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="size-8 animate-spin text-primary" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <Link href="/dashboard/learner/events" className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-2">
          <ArrowLeft className="size-4" /> Back to Events
        </Link>
        <h1 className="text-2xl font-bold">My Registrations</h1>
        <p className="text-muted-foreground">Events you have registered for</p>
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}

      {upcoming.length === 0 && past.length === 0 && !error ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <CalendarOff className="size-12 text-muted-foreground/40" />
          <p className="mt-4 text-lg font-medium">No registered events</p>
          <p className="text-sm text-muted-foreground">Browse events and register to see them here</p>
          <Link href="/dashboard/learner/events"
            className="mt-4 flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
            Browse Events
          </Link>
        </div>
      ) : (
        <>
          {upcoming.length > 0 && (
            <section>
              <h2 className="mb-4 text-lg font-semibold">Upcoming ({upcoming.length})</h2>
              <div className="space-y-3">
                {upcoming.map((event) => (
                  <Link key={event.id} href={`/dashboard/learner/events/${event.id}`}
                    className="flex items-center justify-between rounded-xl border border-border bg-card p-4 transition-all hover:border-primary/30 hover:shadow-sm">
                    <div className="flex items-center gap-4">
                      <div className="flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary">
                        <CalendarDays className="size-5" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="font-semibold">{event.title}</h3>
                          <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${getTypeColor(event.eventType)}`}>
                            {event.eventType}
                          </span>
                        </div>
                        <div className="mt-1 flex items-center gap-4 text-sm text-muted-foreground">
                          <span className="flex items-center gap-1"><CalendarDays className="size-3" /> {formatDate(event.startsAt)}</span>
                          <span className="flex items-center gap-1"><Clock className="size-3" /> {formatTime(event.startsAt)}</span>
                          {event.location && <span className="flex items-center gap-1"><MapPin className="size-3" /> {event.location}</span>}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <CheckCircle className="size-5 text-green-500" />
                      <ExternalLink className="size-4 text-muted-foreground" />
                    </div>
                  </Link>
                ))}
              </div>
            </section>
          )}

          {past.length > 0 && (
            <section>
              <h2 className="mb-4 text-lg font-semibold">Past ({past.length})</h2>
              <div className="space-y-3">
                {past.map((event) => (
                  <Link key={event.id} href={`/dashboard/learner/events/${event.id}`}
                    className="flex items-center justify-between rounded-xl border border-border bg-card p-4 opacity-75 transition-all hover:border-primary/30 hover:opacity-100 hover:shadow-sm">
                    <div className="flex items-center gap-4">
                      <div className="flex size-12 items-center justify-center rounded-xl bg-muted text-muted-foreground">
                        <CalendarDays className="size-5" />
                      </div>
                      <div>
                        <h3 className="font-semibold">{event.title}</h3>
                        <div className="mt-1 flex items-center gap-4 text-sm text-muted-foreground">
                          <span className="flex items-center gap-1"><CalendarDays className="size-3" /> {formatDate(event.startsAt)}</span>
                          {event.hasRecording && <span className="text-xs text-green-600 font-medium">Recording available</span>}
                          {event.materialCount > 0 && <span className="text-xs text-blue-600 font-medium">{event.materialCount} materials</span>}
                        </div>
                      </div>
                    </div>
                    <ExternalLink className="size-4 text-muted-foreground" />
                  </Link>
                ))}
              </div>
            </section>
          )}
        </>
      )}
    </div>
  )
}
