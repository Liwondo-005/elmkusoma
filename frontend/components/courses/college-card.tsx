"use client"

import Link from "next/link"
import { useTranslations } from "next-intl"

import type { College } from "@/lib/data"

export function CollegeCard({ college }: { college: College }) {
  const t = useTranslations("ui")
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
            {college.location}
          </span>
        </div>
      </div>

      <p className="mt-3 line-clamp-2 text-sm leading-relaxed text-muted-foreground">
        {college.description}
      </p>

      <div className="mt-auto flex items-center gap-4 pt-4 border-t border-border">
        <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
          {t("collegeCard.facultyCount", { count: college.faculties.length })}
        </span>
        <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
          {t("collegeCard.programmeCount", { count: totalProgrammes })}
        </span>
      </div>
    </Link>
  )
}
