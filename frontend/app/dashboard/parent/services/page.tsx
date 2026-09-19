"use client"

import { useEffect, useState } from "react"
import { School, Bus, BookOpen, Users, Calendar, MessageSquare, ChevronRight, Loader2, Shield } from "lucide-react"
import Link from "next/link"
import { parentApi, type EntitlementItem } from "@/lib/parent-api"

interface ServiceItem {
  icon: typeof School
  label: string
  description: string
  href: string
  available: boolean
}

const BASE_SERVICES: ServiceItem[] = [
  { icon: Bus, label: "Transport", description: "Transport routes and schedules", href: "#", available: false },
  { icon: BookOpen, label: "Library", description: "Borrowed resources and digital library", href: "/dashboard/parent/library", available: true },
  { icon: Users, label: "Clubs & Activities", description: "Extracurricular activities and participation", href: "#", available: false },
  { icon: Calendar, label: "School Events", description: "Upcoming events and meetings", href: "/dashboard/parent/calendar", available: true },
  { icon: MessageSquare, label: "Announcements", description: "School announcements and notices", href: "/dashboard/parent/notifications", available: true },
  { icon: School, label: "Parent Meetings", description: "Parent-teacher meeting schedule", href: "/dashboard/parent/calendar", available: true },
]

export default function ParentServicesPage() {
  const [entitlements, setEntitlements] = useState<EntitlementItem[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    parentApi.getChildren().then(async (kids) => {
      if (kids.length === 0) return []
      const allEnts = await Promise.all(kids.map(k => parentApi.getChildEntitlements(k.studentId).catch(() => [])))
      return allEnts.flat()
    }).then(setEntitlements).catch(() => setEntitlements([])).finally(() => setLoading(false))
  }, [])

  const services: ServiceItem[] = BASE_SERVICES.map(s => {
    if (s.label === "Transport" && entitlements.some(e => e.serviceType === "TRANSPORT")) return { ...s, available: true }
    if (s.label === "Clubs & Activities" && entitlements.some(e => e.serviceType === "CLUB")) return { ...s, available: true }
    return s
  })

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">School Services</h1>
        <p className="mt-1 text-sm text-muted-foreground">Access available school services</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        {services.map((service) => (
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

      {entitlements.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">Your Entitlements</h2>
          <div className="mt-3 space-y-2">
            {entitlements.map((ent) => (
              <div key={ent.id} className="flex items-center gap-3 rounded-xl border border-border bg-muted/30 p-3">
                <Shield className="size-4 shrink-0 text-green-600" />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-foreground">{ent.serviceType}</p>
                  <p className="text-xs text-muted-foreground">
                    {ent.status === "ACTIVE" ? "Active" : ent.status}
                    {ent.expiresAt && ` · Expires ${new Date(ent.expiresAt).toLocaleDateString("en-GB")}`}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
