"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { School, Bus, BookOpen, Users, Calendar, MessageSquare, ChevronRight, Loader2, Shield } from "lucide-react"
import Link from "next/link"
import { parentApi, type EntitlementItem } from "@/lib/parent-api"

interface ServiceItem {
  key: string
  icon: typeof School
  label: string
  description: string
  href: string
  available: boolean
}

export default function ParentServicesPage() {
  const t = useTranslations("parent")
  const tn = useTranslations("nav")
  const ts = useTranslations("status")
  const [entitlements, setEntitlements] = useState<EntitlementItem[]>([])
  const [loading, setLoading] = useState(true)

  const BASE_SERVICES: ServiceItem[] = [
    { key: "transport", icon: Bus, label: t("services.transport"), description: t("services.transportDesc"), href: "#", available: false },
    { key: "library", icon: BookOpen, label: tn("library"), description: t("services.libraryDesc"), href: "/dashboard/parent/library", available: true },
    { key: "clubs", icon: Users, label: t("services.clubs"), description: t("services.clubsDesc"), href: "#", available: false },
    { key: "events", icon: Calendar, label: t("services.schoolEvents"), description: t("services.schoolEventsDesc"), href: "/dashboard/parent/calendar", available: true },
    { key: "announcements", icon: MessageSquare, label: tn("announcements"), description: t("services.announcementsDesc"), href: "/dashboard/parent/notifications", available: true },
    { key: "meetings", icon: School, label: t("services.parentMeetings"), description: t("services.parentMeetingsDesc"), href: "/dashboard/parent/calendar", available: true },
  ]

  useEffect(() => {
    parentApi.getChildren().then(async (kids) => {
      if (kids.length === 0) return []
      const allEnts = await Promise.all(kids.map(k => parentApi.getChildEntitlements(k.studentId).catch(() => [])))
      return allEnts.flat()
    }).then(setEntitlements).catch(() => setEntitlements([])).finally(() => setLoading(false))
  }, [])

  const services: ServiceItem[] = BASE_SERVICES.map(s => {
    if (s.key === "transport" && entitlements.some(e => e.serviceType === "TRANSPORT")) return { ...s, available: true }
    if (s.key === "clubs" && entitlements.some(e => e.serviceType === "CLUB")) return { ...s, available: true }
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
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("services.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("services.subtitle")}</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        {services.map((service) => (
          <div key={service.key} className={`rounded-2xl border border-border bg-card p-5 shadow-xs ${!service.available ? "opacity-60" : ""}`}>
            <div className="flex items-start gap-3">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                <service.icon className="size-5 text-primary" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-foreground">{service.label}</p>
                <p className="mt-1 text-xs text-muted-foreground">{service.description}</p>
                {service.available ? (
                  <Link href={service.href} className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                    {t("services.access")} <ChevronRight className="size-3" />
                  </Link>
                ) : (
                  <p className="mt-3 text-xs text-muted-foreground italic">{t("services.notAvailable")}</p>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {entitlements.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">{t("services.entitlementsTitle")}</h2>
          <div className="mt-3 space-y-2">
            {entitlements.map((ent) => (
              <div key={ent.id} className="flex items-center gap-3 rounded-xl border border-border bg-muted/30 p-3">
                <Shield className="size-4 shrink-0 text-green-600" />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-foreground">{ent.serviceType}</p>
                  <p className="text-xs text-muted-foreground">
                    {ent.status === "ACTIVE" ? ts("active") : ent.status}
                    {ent.expiresAt && ` · ${t("services.expiresLabel", { date: new Date(ent.expiresAt).toLocaleDateString("en-GB") })}`}
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
