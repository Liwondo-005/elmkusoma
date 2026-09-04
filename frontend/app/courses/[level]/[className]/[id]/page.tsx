import { notFound } from "next/navigation"
import Link from "next/link"

import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import {
  curriculumSubjects,
  getCourseLevelBySlug,
  levelSlugToEducationLevel,
  slugToClassName,
  educationLevelToSlug,
  classNameToSlug,
} from "@/lib/data"
import { SubjectCard } from "@/components/courses/subject-card"
import { cn } from "@/lib/utils"

export function generateStaticParams() {
  return curriculumSubjects.map((s) => ({
    level: educationLevelToSlug(s.level),
    className: classNameToSlug(s.className),
    id: s.id,
  }))
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ level: string; className: string; id: string }>
}) {
  const { level, className, id } = await params
  const subject = curriculumSubjects.find((s) => s.id === id)
  if (!subject) return { title: "Subject Not Found — ELMKUSOMA" }
  return {
    title: `${subject.name} (${subject.className}) — ELMKUSOMA`,
    description: subject.description,
  }
}

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

export default async function SubjectDetailPage({
  params,
}: {
  params: Promise<{ level: string; className: string; id: string }>
}) {
  const { level, className, id } = await params
  const courseLevel = getCourseLevelBySlug(level)
  if (!courseLevel) notFound()

  const educationLevel = levelSlugToEducationLevel(level)
  if (!educationLevel) notFound()

  const displayName = slugToClassName(className, educationLevel)
  if (!displayName) notFound()

  const subject = curriculumSubjects.find((s) => s.id === id)
  if (!subject) notFound()

  const relatedSubjects = curriculumSubjects
    .filter((s) => s.level === subject.level && s.className === subject.className && s.id !== subject.id)
    .slice(0, 3)

  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1 bg-muted/40">
        <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Link href="/courses" className="transition-colors hover:text-foreground">
              Courses
            </Link>
            <span>/</span>
            <Link href={`/courses/${level}`} className="transition-colors hover:text-foreground">
              {courseLevel.name}
            </Link>
            <span>/</span>
            <Link href={`/courses/${level}/${className}`} className="transition-colors hover:text-foreground">
              {displayName}
            </Link>
            <span>/</span>
            <span className="text-foreground">{subject.name}</span>
          </div>

          <div className="mt-6 grid gap-6 lg:grid-cols-[1fr_320px]">
            <div className="space-y-6">
              <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
                <div className="flex flex-wrap items-center gap-2">
                  <span
                    className={cn(
                      "inline-flex items-center rounded-md px-2.5 py-1 text-xs font-semibold",
                      categoryColors[subject.category] ?? "bg-muted text-muted-foreground"
                    )}
                  >
                    {subject.category}
                  </span>
                </div>

                <h1 className="mt-4 text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
                  {subject.name}
                </h1>
                <p className="mt-1 text-sm text-muted-foreground">
                  {courseLevel.name} &middot; {displayName}
                </p>

                <div className="mt-6">
                  <h2 className="text-sm font-semibold text-foreground">About this subject</h2>
                  <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                    {subject.description}
                  </p>
                </div>

                <div className="mt-6 grid grid-cols-3 gap-4 rounded-xl border border-border p-4">
                  <div className="flex items-center gap-3">
                    <span className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    </span>
                    <div>
                      <p className="text-xs text-muted-foreground">Level</p>
                      <p className="text-sm font-semibold text-foreground">{subject.level}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    </span>
                    <div>
                      <p className="text-xs text-muted-foreground">Class</p>
                      <p className="text-sm font-semibold text-foreground">{subject.className}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    </span>
                    <div>
                      <p className="text-xs text-muted-foreground">Enrolled</p>
                      <p className="text-sm font-semibold text-foreground">{subject.enrolled.toLocaleString()}</p>
                    </div>
                  </div>
                </div>
              </div>

              {relatedSubjects.length > 0 && (
                <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
                  <h2 className="text-sm font-semibold text-foreground">
                    Other subjects in {displayName}
                  </h2>
                  <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {relatedSubjects.map((s) => (
                      <SubjectCard key={s.id} subject={s} levelSlug={level} classNameSlug={className} />
                    ))}
                  </div>
                </div>
              )}
            </div>

            <aside className="space-y-4">
              <div className="sticky top-20 rounded-2xl border border-border bg-card p-5 shadow-xs">
                <div className="flex items-center gap-3">
                  <span className="flex size-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                  </span>
                  <div>
                    <p className="text-sm font-semibold text-foreground">{subject.name}</p>
                    <p className="text-xs text-muted-foreground">{subject.className}</p>
                  </div>
                </div>

                <div className="mt-4 space-y-3">
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">Level</span>
                    <span className="font-medium text-foreground">{subject.level}</span>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">Category</span>
                    <span className="font-medium text-foreground">{subject.category}</span>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">Students Enrolled</span>
                    <span className="font-medium text-foreground">{subject.enrolled.toLocaleString()}</span>
                  </div>
                </div>

                <Link
                  href="/register"
                  className="mt-5 flex h-10 items-center justify-center rounded-lg bg-primary text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
                >
                  Enroll Now
                </Link>
              </div>
            </aside>
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
