import Link from "next/link"
import Image from "next/image"
import { Eye, Clock, Users } from "lucide-react"
import type { LiveClass } from "@/lib/data"
import { Button, buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

function StatusBadge({ status, badge }: { status: LiveClass["status"]; badge: string }) {
  if (status === "live") {
    return (
      <span className="inline-flex items-center gap-1.5 rounded-md bg-teal px-2.5 py-1 text-xs font-semibold text-teal-foreground shadow-sm">
        <span className="size-1.5 animate-pulse rounded-full bg-white" />
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
  return (
    <div className="group flex flex-col overflow-hidden rounded-2xl border border-border bg-card shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg">
      <div className="relative aspect-video overflow-hidden">
        <Image
          src={item.image || "/placeholder.svg"}
          alt={`${item.title} class with ${item.instructor}`}
          fill
          className="object-cover transition-transform duration-500 group-hover:scale-105"
          sizes="(max-width: 768px) 100vw, 25vw"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/25 to-transparent" />
        <div className="absolute left-3 top-3">
          <StatusBadge status={item.status} badge={item.badge} />
        </div>
        {isLive && (
          <span className="absolute right-3 top-3 inline-flex items-center gap-1 rounded-md bg-black/60 px-2 py-1 text-[10px] font-bold uppercase tracking-wide text-white backdrop-blur">
            <span className="size-1.5 rounded-full bg-red-500" />
            Live
          </span>
        )}
      </div>

      <div className="flex flex-1 flex-col p-4">
        <h3 className="text-sm font-semibold leading-snug text-foreground">
          {item.title} <span className="text-muted-foreground">{item.subtitle}</span>
        </h3>
        <p className="mt-1 text-xs text-muted-foreground">{item.instructor}</p>

        <div className="mt-3 flex items-center gap-1.5 text-xs text-muted-foreground">
          {isLive ? (
            <>
              <Eye className="size-3.5 text-teal" />
              <span>{item.watching} watching</span>
            </>
          ) : (
            <>
              <Clock className="size-3.5" />
              <span>{item.time}</span>
              {item.going ? (
                <span className="ml-auto inline-flex items-center gap-1">
                  <Users className="size-3.5" /> {item.going} going
                </span>
              ) : null}
            </>
          )}
        </div>

        <div className="mt-4 pt-1">
          {isLive ? (
            <Link
              href={`/live-classes/${item.id}`}
              className={cn(buttonVariants(), "h-9 w-full bg-teal text-teal-foreground hover:bg-teal/90")}
            >
              Join Live Class
            </Link>
          ) : (
            <Button variant="outline" className="h-9 w-full border-primary/30 text-primary hover:bg-accent">
              Set Reminder
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
