"use client"

import { notFound, useParams } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"

import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import {
  getCourseLevelBySlug,
  levelSlugToEducationLevel,
  slugToClassName,
  classesByLevel,
  classesByLevel,
  getSubjectsByLevelAndClass,
  categoriesByLevel,
} from "@/lib/data"
import { SubjectCard } from "@/components/courses/subject-card"
import { ClassSubjectsBrowser } from "@/components/courses/class-subjects-browser"

export default function ClassDetailPage() {
  const t = useTranslations("public")
  const params = useParams()
  const level = params.level as string
  const className = params.className as string
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
                {t("coursesHome.title")}
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
              {t("classDetail.summary", { level: courseLevel.name, count: subjects.length })}
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
