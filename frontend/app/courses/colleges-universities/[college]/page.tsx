import { notFound } from "next/navigation"
import Link from "next/link"
import { BookOpen, GraduationCap } from "lucide-react"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { colleges, getCollegeBySlug } from "@/lib/data"
import type { Metadata } from "next"

export function generateStaticParams() {
  return colleges.map((c) => ({ college: c.id }))
}

export async function generateMetadata({ params }: { params: Promise<{ college: string }> }): Promise<Metadata> {
  const { college } = await params
  const c = getCollegeBySlug(college)
  if (!c) return { title: "Institution Not Found — ELMKUSOMA" }
  return {
    title: `${c.name} — Colleges & Universities — ELMKUSOMA`,
    description: c.description,
  }
}

export default async function CollegePage({ params }: { params: Promise<{ college: string }> }) {
  const { college } = await params
  const collegeData = getCollegeBySlug(college)
  if (!collegeData) notFound()

  const totalProgrammes = collegeData.faculties.reduce((a, f) => a + f.programmes.length, 0)

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
              <span className="text-foreground">{collegeData.name}</span>
            </div>
            <h1 className="mt-4 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
              {collegeData.name}
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">{collegeData.location}</p>
            <p className="mt-2 max-w-2xl text-muted-foreground">{collegeData.description}</p>
            <div className="mt-3 flex flex-wrap items-center gap-4 text-sm text-muted-foreground">
              <span className="inline-flex items-center gap-1.5">
                <BookOpen className="size-4" /> {collegeData.faculties.length} faculties
              </span>
              <span className="inline-flex items-center gap-1.5">
                <GraduationCap className="size-4" /> {totalProgrammes} programmes
              </span>
            </div>
          </div>
        </section>

        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <h2 className="text-lg font-semibold text-foreground">Faculties & Schools</h2>
          <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {collegeData.faculties.map((faculty) => (
              <Link
                key={faculty.id}
                href={`/courses/colleges-universities/${college}/${faculty.id}`}
                className="group flex flex-col rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg"
              >
                <div className="flex items-start justify-between">
                  <span className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <BookOpen className="size-4" />
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {faculty.programmes.length} {faculty.programmes.length === 1 ? "programme" : "programmes"}
                  </span>
                </div>
                <h3 className="mt-3 text-base font-semibold text-foreground group-hover:text-primary">
                  {faculty.name}
                </h3>
                <p className="mt-1 flex-1 text-sm text-muted-foreground">{faculty.description}</p>
              </Link>
            ))}
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
