"use client"

import Link from "next/link"
import Image from "next/image"
import { useTranslations } from "next-intl"
import { Clock, PlayCircle, Users } from "lucide-react"
import type { LiveClass } from "@/lib/data"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

function StatusBadge({ status, badge }: { status: LiveClass["status"]; badge: string }) {
  if (status === "live") {
    return (
      <span className="inline-flex items-center gap-1.5 rounded-md bg-teal px-2.5 py-1 text-xs font-semibold text-teal-foreground shadow-sm">
        <span className="size-1.5 animate-pulse rounded-full bg-teal-foreground motion-reduce:animate-none" aria-hidden />
        {badge}
      </span>
    )
  }
  return (
    <span className="inline-flex items-center rounded-md bg-orange px-2.5 py-1 text-xs font-semibold text-orange-foreground shadow-sm">
      {badge}
    </span>
  )
}

export function LiveClassCard({ item }: { item: LiveClass }) {
  const isLive = item.status === "live"
  const hasRecording = Boolean(item.hasRecording)
  const t = useTranslations("learner")
  const tlc = useTranslations("liveClasses")
  const td = useTranslations("ui.liveBrowser")

  // Past sessions used to link to the calendar ("Set reminder"), which is not what a finished
  // session is for. A recorded session now goes to the Replays library; the recording URL
  // itself is entitlement-checked server side, so no raw URL is shipped to this card.
  const action = isLive
    ? { href: `/live-classes/${item.id}`, label: t("joinLiveClass") }
    : hasRecording
      ? { href: "/dashboard/learner/replays", label: td("watchReplay") }
      : { href: `/live-classes/${item.id}`, label: td("viewSession") }

  return (
    <article className="group flex flex-col overflow-hidden rounded-2xl border border-border bg-card shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg">
      <div className="relative aspect-video overflow-hidden">
        <Image
          src={item.image || "/placeholder.svg"}
          alt={item.instructor
            ? t("liveClassImageAlt", { title: item.title, instructor: item.instructor })
            : t("liveClassImageAltNoTeacher", { title: item.title })}
          fill
          className="object-cover transition-transform duration-500 group-hover:scale-105"
          sizes="(max-width: 768px) 100vw, 25vw"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/25 to-transparent" aria-hidden />
        <div className="absolute left-3 top-3">
          <StatusBadge status={item.status} badge={item.badge} />
        </div>
      </div>

      <div className="flex flex-1 flex-col p-4">
        <h3 className="text-sm font-semibold leading-snug text-foreground">
          {item.title}
          {item.subtitle ? <span className="text-muted-foreground"> {item.subtitle}</span> : null}
        </h3>
        {/* Only rendered when the API actually supplied a teacher name. */}
        {item.instructor ? (
          <p className="mt-1 text-xs text-muted-foreground">{item.instructor}</p>
        ) : null}

        <div className="mt-3 flex items-center gap-1.5 text-xs text-muted-foreground">
          {/* No fabricated viewer count: the discovery endpoint does not return one, so the
              card previously rendered "0 watching" on every live class. */}
          <span><Clock className="inline size-3.5" aria-hidden /> {item.time}</span>
          {item.going ? (
            <span className="ml-auto inline-flex items-center gap-1">
              <Users className="size-3.5" aria-hidden /> {t("liveGoing", { count: item.going })}
            </span>
          ) : null}
        </div>

        <div className="mt-4 flex items-center gap-2 pt-1">
          <Link
            href={action.href}
            aria-label={t("liveClassActionAria", { title: item.title, action: action.label })}
            className={cn(
              buttonVariants(isLive ? { variant: "default" } : { variant: "outline" }),
              "h-9 w-full",
              isLive && "bg-teal text-teal-foreground hover:bg-primary",
            )}
          >
            {hasRecording && !isLive ? <PlayCircle className="mr-1 size-4" aria-hidden /> : null}
            {action.label}
          </Link>
          {/* "Set reminder" only makes sense before a session starts. A finished session with
              no recording has nothing to schedule. */}
          {!isLive && !hasRecording && item.status !== "past" ? (
            <Link
              href="/dashboard/learner/calendar-integration"
              aria-label={t("setReminderAria", { title: item.title })}
              className={cn(buttonVariants({ variant: "ghost" }), "h-9 shrink-0 px-3 hover:bg-primary/10")}
            >
              {tlc("setReminder")}
            </Link>
          ) : null}
        </div>
      </div>
    </article>
  )
}
