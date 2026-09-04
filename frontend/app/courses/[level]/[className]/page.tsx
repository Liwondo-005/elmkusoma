import { notFound } from "next/navigation"
import Link from "next/link"

import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import {
  getCourseLevelBySlug,
  levelSlugToEducationLevel,
  slugToClassName,
  getSubjectsByLevelAndClass,
  categoriesByLevel,
} from "@/lib/data"
import { SubjectCard } from "@/components/courses/subject-card"
import { ClassSubjectsBrowser } from "@/components/courses/class-subjects-browser"
import type { Metadata } from "next"

export function generateStaticParams() {
  const { classesByLevel } = require("@/lib/data")
  const params: { level: string; className: string }[] = []
  const levelMap: Record<string, string> = {
    Nursery: "nursery",
    Primary: "primary",
    "Lower Secondary": "lower-secondary",
    "Advanced Secondary": "advanced-secondary",
  }
  for (const [level, classes] of Object.entries(classesByLevel) as [string, string[]][]) {
    const slug = levelMap[level]
    if (!slug) continue
    for (const cls of classes) {
      params.push({ level: slug, className: cls.toLowerCase().replace(/\s+/g, "-") })
    }
  }
  return params
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ level: string; className: string }>
}): Promise<Metadata> {
  const { level, className } = await params
  const courseLevel = getCourseLevelBySlug(level)
  if (!courseLevel) return { title: "Not Found — ELMKUSOMA" }
  const educationLevel = levelSlugToEducationLevel(level)
  if (!educationLevel) return { title: "Not Found — ELMKUSOMA" }
  const displayName = slugToClassName(className, educationLevel)
  if (!displayName) return { title: "Not Found — ELMKUSOMA" }
  return {
    title: `${displayName} — ${courseLevel.name} — ELMKUSOMA`,
    description: `Browse all subjects in ${displayName} under ${courseLevel.name} education.`,
  }
}

export default async function ClassPage({
  params,
}: {
  params: Promise<{ level: string; className: string }>
}) {
  const { level, className } = await params
  const courseLevel = getCourseLevelBySlug(level)
  if (!courseLevel) notFound()

  const educationLevel = levelSlugToEducationLevel(level)
  if (!educationLevel) notFound()

  const displayName = slugToClassName(className, educationLevel)
  if (!displayName) notFound()

  const subjects = getSubjectsByLevelAndClass(educationLevel, displayName)
  const categories = categoriesByLevel[educationLevel] ?? []

  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <section className="border-b border-border bg-muted/40">
          <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Link href="/courses" className="transition-colors hover:text-foreground">
                Courses
              </Link>
              <span>/</span>
              <Link href={`/courses/${level}`} className="transition-colors hover:text-foreground">
                {courseLevel.name}
              </Link>
              <span>/</span>
              <span className="text-foreground">{displayName}</span>
            </div>
            <h1 className="mt-4 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
              {displayName}
            </h1>
            <p className="mt-2 text-muted-foreground">
              {courseLevel.name} &middot; {subjects.length} {subjects.length === 1 ? "subject" : "subjects"}
            </p>
          </div>
        </section>

        <ClassSubjectsBrowser
          subjects={subjects}
          categories={categories}
          levelSlug={level}
          classNameSlug={className}
          displayName={displayName}
        />
      </main>
      <SiteFooter />
    </div>
  )
}
