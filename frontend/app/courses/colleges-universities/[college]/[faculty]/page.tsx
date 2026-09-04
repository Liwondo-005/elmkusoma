import { notFound } from "next/navigation"
import Link from "next/link"

import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import {
  colleges,
  getCollegeBySlug,
  getFacultyBySlug,
} from "@/lib/data"
import type { Metadata } from "next"

export function generateStaticParams() {
  const params: { college: string; faculty: string }[] = []
  for (const c of colleges) {
    for (const f of c.faculties) {
      params.push({ college: c.id, faculty: f.id })
    }
  }
  return params
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ college: string; faculty: string }>
}): Promise<Metadata> {
  const { college, faculty } = await params
  const c = getCollegeBySlug(college)
  if (!c) return { title: "Not Found — ELMKUSOMA" }
  const f = getFacultyBySlug(c, faculty)
  if (!f) return { title: "Not Found — ELMKUSOMA" }
  return {
    title: `${f.name} — ${c.name} — ELMKUSOMA`,
    description: f.description,
  }
}

export default async function FacultyPage({
  params,
}: {
  params: Promise<{ college: string; faculty: string }>
}) {
  const { college, faculty } = await params
  const collegeData = getCollegeBySlug(college)
  if (!collegeData) notFound()

  const facultyData = getFacultyBySlug(collegeData, faculty)
  if (!facultyData) notFound()

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
                {collegeData.name}
              </Link>
              <span>/</span>
              <span className="text-foreground">{facultyData.name}</span>
            </div>
            <h1 className="mt-4 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
              {facultyData.name}
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">{collegeData.name}</p>
            <p className="mt-2 max-w-2xl text-muted-foreground">{facultyData.description}</p>
            <p className="mt-1 text-sm text-muted-foreground">
              {facultyData.programmes.length} {facultyData.programmes.length === 1 ? "programme" : "programmes"}
            </p>
          </div>
        </section>

        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <h2 className="text-lg font-semibold text-foreground">Programmes</h2>
          <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {facultyData.programmes.map((programme) => (
              <Link
                key={programme.id}
                href={`/courses/colleges-universities/${college}/${faculty}/${programme.id}`}
                className="group flex flex-col rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg"
              >
                <div className="flex items-start justify-between">
                  <span className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                  </span>
                  <span className="inline-flex items-center rounded-md bg-muted px-2 py-0.5 text-[10px] font-semibold text-muted-foreground">
                    {programme.degreeType}
                  </span>
                </div>
                <h3 className="mt-3 text-base font-semibold text-foreground group-hover:text-primary">
                  {programme.name}
                </h3>
                <p className="mt-1 flex-1 text-sm text-muted-foreground">{programme.description}</p>
                <div className="mt-3 flex items-center gap-3 text-xs text-muted-foreground border-t border-border pt-3">
                  <span className="inline-flex items-center gap-1">
                    {programme.duration}
                  </span>
                  <span>{programme.courses.length} courses</span>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
