"use client"

import { School, Bus, BookOpen, Users, Calendar, MessageSquare, ChevronRight } from "lucide-react"
import Link from "next/link"

const SERVICES = [
  { icon: Bus, label: "Transport", description: "Transport routes and schedules", href: "#", available: false },
  { icon: BookOpen, label: "Library", description: "Borrowed resources and digital library", href: "/dashboard/parent/library", available: true },
  { icon: Users, label: "Clubs & Activities", description: "Extracurricular activities and participation", href: "#", available: false },
  { icon: Calendar, label: "School Events", description: "Upcoming events and meetings", href: "/dashboard/parent/calendar", available: true },
  { icon: MessageSquare, label: "Announcements", description: "School announcements and notices", href: "/dashboard/parent/notifications", available: true },
  { icon: School, label: "Parent Meetings", description: "Parent-teacher meeting schedule", href: "/dashboard/parent/calendar", available: true },
]

export default function ParentServicesPage() {
  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">School Services</h1>
        <p className="mt-1 text-sm text-muted-foreground">Access available school services</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        {SERVICES.map((service) => (
          <div key={service.label} className={`rounded-2xl border border-border bg-card p-5 shadow-xs ${!service.available ? "opacity-60" : ""}`}>
            <div className="flex items-start gap-3">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                <service.icon className="size-5 text-primary" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-foreground">{service.label}</p>
                <p className="mt-1 text-xs text-muted-foreground">{service.description}</p>
                {service.available ? (
                  <Link href={service.href} className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                    Access <ChevronRight className="size-3" />
                  </Link>
                ) : (
                  <p className="mt-3 text-xs text-muted-foreground italic">Not available at your institution</p>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
