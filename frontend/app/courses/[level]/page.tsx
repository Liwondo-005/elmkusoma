import { notFound } from "next/navigation"
import Link from "next/link"
import { ArrowLeft } from "lucide-react"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import {
  getCourseLevelBySlug,
  levelSlugToEducationLevel,
  classesByLevel,
  getSubjectsByLevel,
  classNameToSlug,
} from "@/lib/data"
import type { Metadata } from "next"

export function generateStaticParams() {
  return [
    { level: "nursery" },
    { level: "primary" },
    { level: "lower-secondary" },
    { level: "advanced-secondary" },
  ]
}

export async function generateMetadata({ params }: { params: Promise<{ level: string }> }): Promise<Metadata> {
  const { level } = await params
  const courseLevel = getCourseLevelBySlug(level)
  if (!courseLevel) return { title: "Level Not Found — ELMKUSOMA" }
  return {
    title: `${courseLevel.name} — ELMKUSOMA`,
    description: courseLevel.description,
  }
}

export default async function LevelPage({ params }: { params: Promise<{ level: string }> }) {
  const { level } = await params
  const courseLevel = getCourseLevelBySlug(level)
  if (!courseLevel) notFound()

  const educationLevel = levelSlugToEducationLevel(level)
  if (!educationLevel) notFound()

  const classes = classesByLevel[educationLevel] ?? []
  const subjects = getSubjectsByLevel(educationLevel)

  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <section className="border-b border-border bg-muted/40">
          <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
            <Link
              href="/courses"
              className="inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
            >
              <ArrowLeft className="size-4" />
              All Levels
            </Link>
            <h1 className="mt-4 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
              {courseLevel.name}
            </h1>
            <p className="mt-2 max-w-2xl text-muted-foreground">{courseLevel.description}</p>
            <p className="mt-1 text-sm text-muted-foreground">
              {classes.length} {classes.length === 1 ? "class" : "classes"} &middot; {subjects.length} subjects
            </p>
          </div>
        </section>

        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {classes.map((className) => {
              const slug = classNameToSlug(className)
              const count = subjects.filter((s) => s.className === className).length
              return (
                <Link
                  key={className}
                  href={`/courses/${level}/${slug}`}
                  className="group flex flex-col rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg"
                >
                  <h3 className="text-base font-semibold text-foreground group-hover:text-primary">
                    {className}
                  </h3>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {count} {count === 1 ? "subject" : "subjects"}
                  </p>
                </Link>
              )
            })}
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
