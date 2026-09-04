import { notFound } from "next/navigation"
import Link from "next/link"

import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import {
  colleges,
  getCollegeBySlug,
  getFacultyBySlug,
  getProgrammeBySlug,
} from "@/lib/data"
import type { Metadata } from "next"

export function generateStaticParams() {
  const params: { college: string; faculty: string; programme: string }[] = []
  for (const c of colleges) {
    for (const f of c.faculties) {
      for (const p of f.programmes) {
        params.push({ college: c.id, faculty: f.id, programme: p.id })
      }
    }
  }
  return params
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ college: string; faculty: string; programme: string }>
}): Promise<Metadata> {
  const { college, faculty, programme } = await params
  const c = getCollegeBySlug(college)
  if (!c) return { title: "Not Found — ELMKUSOMA" }
  const f = getFacultyBySlug(c, faculty)
  if (!f) return { title: "Not Found — ELMKUSOMA" }
  const p = getProgrammeBySlug(f, programme)
  if (!p) return { title: "Not Found — ELMKUSOMA" }
  return {
    title: `${p.name} — ${c.shortName} — ELMKUSOMA`,
    description: p.description,
  }
}

export default async function ProgrammePage({
  params,
}: {
  params: Promise<{ college: string; faculty: string; programme: string }>
}) {
  const { college, faculty, programme } = await params
  const collegeData = getCollegeBySlug(college)
  if (!collegeData) notFound()

  const facultyData = getFacultyBySlug(collegeData, faculty)
  if (!facultyData) notFound()

  const programmeData = getProgrammeBySlug(facultyData, programme)
  if (!programmeData) notFound()

  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <section className="border-b border-border bg-muted/40">
          <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Link href="/courses" className="transition-colors hover:text-foreground">Courses</Link>
              <span>/</span>
              <Link href="/courses/colleges-universities" className="transition-colors hover:text-foreground">
                Colleges & Universities
              </Link>
              <span>/</span>
              <Link
                href={`/courses/colleges-universities/${college}`}
                className="transition-colors hover:text-foreground"
              >
                {collegeData.shortName}
              </Link>
              <span>/</span>
              <Link
                href={`/courses/colleges-universities/${college}/${faculty}`}
                className="transition-colors hover:text-foreground"
              >
                {facultyData.name}
              </Link>
              <span>/</span>
              <span className="text-foreground">{programmeData.name}</span>
            </div>

            <div className="mt-4 flex flex-wrap items-center gap-2">
              <span className="inline-flex items-center rounded-md bg-primary/10 px-2.5 py-1 text-xs font-semibold text-primary">
                {programmeData.degreeType}
              </span>
              <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                {programmeData.duration}
              </span>
            </div>

            <h1 className="mt-3 text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
              {programmeData.name}
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {facultyData.name} &middot; {collegeData.name}
            </p>
            <p className="mt-2 max-w-2xl text-muted-foreground">{programmeData.description}</p>
          </div>
        </section>

        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <h2 className="text-lg font-semibold text-foreground">
            Courses ({programmeData.courses.length})
          </h2>
          <div className="mt-5 space-y-3">
            {programmeData.courses.map((course, index) => (
              <div
                key={course.id}
                className="flex items-center gap-4 rounded-2xl border border-border bg-card p-4 shadow-xs sm:p-5"
              >
                <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-sm font-bold text-primary">
                  {index + 1}
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <h3 className="text-sm font-semibold text-foreground">{course.name}</h3>
                    <span className="inline-flex items-center rounded-md bg-muted px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground">
                      {course.code}
                    </span>
                  </div>
                  <p className="mt-0.5 text-xs text-muted-foreground">{course.description}</p>
                </div>
                <div className="shrink-0 text-right">
                  <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                    {course.credits} credits
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
