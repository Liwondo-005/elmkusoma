import Link from "next/link"
import type { CurriculumSubject } from "@/lib/data"
import { educationLevelToSlug, classNameToSlug } from "@/lib/data"
import { cn } from "@/lib/utils"

const categoryColors: Record<string, string> = {
  Core: "bg-teal/10 text-teal",
  "Social Science": "bg-amber-100 text-amber-700",
  "Language and Literature": "bg-sky-100 text-sky-700",
  "Natural Science": "bg-green-100 text-green-700",
  Mathematics: "bg-indigo-100 text-indigo-700",
  Technology: "bg-cyan-100 text-cyan-700",
  "Business and Economics": "bg-orange-100 text-orange-700",
  "Culture, Arts and Sports": "bg-rose-100 text-rose-700",
}

export function SubjectCard({
  subject,
  levelSlug,
  classNameSlug,
}: {
  subject: CurriculumSubject
  levelSlug?: string
  classNameSlug?: string
}) {
  const href =
    levelSlug && classNameSlug
      ? `/courses/${levelSlug}/${classNameSlug}/${subject.id}`
      : `/courses/${educationLevelToSlug(subject.level)}/${classNameToSlug(subject.className)}/${subject.id}`

  return (
    <Link
      href={href}
      className="group flex flex-col overflow-hidden rounded-2xl border border-border bg-card shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg"
    >
      <div className="flex flex-1 flex-col p-5">
        <div className="flex items-start gap-2">
          <div className="min-w-0 flex-1">
            <h3 className="text-sm font-semibold leading-snug text-foreground group-hover:text-primary">
              {subject.name}
            </h3>
            <p className="mt-0.5 text-xs text-muted-foreground">{subject.className}</p>
          </div>
        </div>

        <p className="mt-3 line-clamp-2 text-xs leading-relaxed text-muted-foreground">
          {subject.description}
        </p>

        <div className="mt-auto flex flex-wrap items-center gap-2 pt-4">
          <span
            className={cn(
              "inline-flex items-center rounded-md px-2 py-0.5 text-[10px] font-semibold",
              categoryColors[subject.category] ?? "bg-muted text-muted-foreground"
            )}
          >
            {subject.category}
          </span>
          <span className="ml-auto inline-flex items-center gap-1 text-[10px] text-muted-foreground">
            {subject.enrolled.toLocaleString()} enrolled
          </span>
        </div>
      </div>
    </Link>
  )
}
