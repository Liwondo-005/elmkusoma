import Link from "next/link"
import { ChevronRight } from "lucide-react"
import type { CourseLevel } from "@/lib/data"
import { cn } from "@/lib/utils"

export function LevelCard({ level }: { level: CourseLevel }) {
  return (
    <Link
      href={level.comingSoon ? "/courses" : `/courses/${level.slug}`}
      className={cn(
        "group flex flex-col rounded-2xl border border-border bg-card p-6 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg",
        level.comingSoon && "pointer-events-none opacity-60"
      )}
    >
      <div className="flex items-start justify-end">
        {!level.comingSoon && (
          <span className="flex size-8 items-center justify-center rounded-full bg-muted text-muted-foreground transition-all group-hover:bg-primary group-hover:text-primary-foreground">
            <ChevronRight className="size-4" />
          </span>
        )}
      </div>

      <h3 className="text-base font-semibold text-foreground">{level.name}</h3>
      <p className="mt-1.5 flex-1 text-sm leading-relaxed text-muted-foreground">{level.description}</p>

      <div className="mt-4 flex items-center justify-between border-t border-border pt-4">
        {level.comingSoon ? (
          <span className="text-xs font-medium text-muted-foreground">Coming Soon</span>
        ) : (
          <span className="text-xs font-medium text-muted-foreground">
            {level.subjectCount} {level.subjectCount === 1 ? "subject" : "subjects"}
          </span>
        )}
      </div>
    </Link>
  )
}
