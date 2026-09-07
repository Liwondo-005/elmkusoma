import Link from "next/link"
import { BookOpen, GraduationCap, Wrench, Building2, Baby, Beaker } from "lucide-react"

import type { CourseLevel } from "@/lib/data"
import { cn } from "@/lib/utils"

const colorMap: Record<string, string> = {
  nursery: "bg-pink-50 text-pink-600 group-hover:bg-pink-100",
  primary: "bg-blue-50 text-blue-600 group-hover:bg-blue-100",
  "lower-secondary": "bg-emerald-50 text-emerald-600 group-hover:bg-emerald-100",
  "advanced-secondary": "bg-purple-50 text-purple-600 group-hover:bg-purple-100",
  veta: "bg-amber-50 text-amber-600 group-hover:bg-amber-100",
  "colleges-universities": "bg-sky-50 text-sky-600 group-hover:bg-sky-100",
}

const iconMap: Record<string, React.ReactNode> = {
  nursery: <Baby className="size-5" />,
  primary: <BookOpen className="size-5" />,
  "lower-secondary": <Beaker className="size-5" />,
  "advanced-secondary": <GraduationCap className="size-5" />,
  veta: <Wrench className="size-5" />,
  "colleges-universities": <Building2 className="size-5" />,
}

export function LevelCard({ level }: { level: CourseLevel }) {
  return (
    <Link
      href={level.comingSoon ? "/courses" : `/courses/${level.slug}`}
      className={cn(
        "group flex flex-col rounded-2xl border border-border bg-card p-6 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg",
        level.comingSoon && "pointer-events-none opacity-60"
      )}
    >
      <div className="flex items-start justify-between">
        <span
          className={cn(
            "flex size-11 items-center justify-center rounded-xl transition-colors",
            colorMap[level.slug] ?? "bg-muted text-muted-foreground"
          )}
        >
          {iconMap[level.slug] ?? <BookOpen className="size-5" />}
        </span>
      </div>

      <h3 className="mt-4 text-base font-semibold text-foreground">{level.name}</h3>
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
