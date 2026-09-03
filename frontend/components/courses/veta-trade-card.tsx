import Link from "next/link"
import { ChevronRight, Layers, BookOpen } from "lucide-react"
import type { VetaTrade } from "@/lib/data"
import { cn } from "@/lib/utils"

export function VetaTradeCard({ trade }: { trade: VetaTrade }) {
  const totalModules = trade.levels.reduce((acc, l) => acc + l.modules.length, 0)
  const totalLessons = trade.levels.reduce(
    (acc, l) => acc + l.modules.reduce((a, m) => a + m.lessons.length, 0),
    0
  )

  return (
    <Link
      href={`/courses/veta/${trade.id}`}
      className="group flex flex-col rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg"
    >
      <div className="flex items-start justify-between">
        <h3 className="text-base font-semibold text-foreground group-hover:text-primary">
          {trade.name}
        </h3>
        <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-muted text-muted-foreground transition-all group-hover:bg-primary group-hover:text-primary-foreground">
          <ChevronRight className="size-3.5" />
        </span>
      </div>

      <p className="mt-2 line-clamp-2 text-sm leading-relaxed text-muted-foreground">
        {trade.description}
      </p>

      <div className="mt-auto flex items-center gap-4 pt-4 border-t border-border">
        <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
          <Layers className="size-3.5" />
          {trade.levels.length} {trade.levels.length === 1 ? "level" : "levels"}
        </span>
        <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
          <BookOpen className="size-3.5" />
          {totalModules} {totalModules === 1 ? "module" : "modules"}
        </span>
      </div>
    </Link>
  )
}
