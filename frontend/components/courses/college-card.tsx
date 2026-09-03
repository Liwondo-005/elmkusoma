import Link from "next/link"
import { ChevronRight, MapPin, BookOpen, GraduationCap } from "lucide-react"
import type { College } from "@/lib/data"

export function CollegeCard({ college }: { college: College }) {
  const totalProgrammes = college.faculties.reduce((a, f) => a + f.programmes.length, 0)

  return (
    <Link
      href={`/courses/colleges-universities/${college.id}`}
      className="group flex flex-col rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg"
    >
      <div className="flex items-start justify-between">
        <div>
          <h3 className="text-base font-semibold text-foreground group-hover:text-primary">
            {college.name}
          </h3>
          <span className="mt-0.5 inline-flex items-center gap-1 text-xs text-muted-foreground">
            <MapPin className="size-3" />
            {college.location}
          </span>
        </div>
        <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-muted text-muted-foreground transition-all group-hover:bg-primary group-hover:text-primary-foreground">
          <ChevronRight className="size-3.5" />
        </span>
      </div>

      <p className="mt-3 line-clamp-2 text-sm leading-relaxed text-muted-foreground">
        {college.description}
      </p>

      <div className="mt-auto flex items-center gap-4 pt-4 border-t border-border">
        <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
          <BookOpen className="size-3.5" />
          {college.faculties.length} {college.faculties.length === 1 ? "faculty" : "faculties"}
        </span>
        <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
          <GraduationCap className="size-3.5" />
          {totalProgrammes} {totalProgrammes === 1 ? "programme" : "programmes"}
        </span>
      </div>
    </Link>
  )
}
