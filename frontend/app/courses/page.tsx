import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { LevelCard } from "@/components/courses/level-card"
import { courseLevels } from "@/lib/data"

export const metadata = {
  title: "Courses — ELMKUSOMA",
  description: "Browse courses and subjects across all education levels — Nursery, Primary, Secondary, VETA, and Universities.",
}

export default function CoursesPage() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <section className="border-b border-border bg-muted/40">
          <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
            <span className="inline-flex items-center gap-1.5 rounded-md bg-primary/10 px-2.5 py-1 text-xs font-semibold text-primary">
              Tanzanian Curriculum
            </span>
            <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">Courses</h1>
            <p className="mt-2 max-w-2xl text-muted-foreground">
              Choose an education level to explore classes, forms, and subjects.
            </p>
          </div>
        </section>

        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {courseLevels.map((level) => (
              <LevelCard key={level.slug} level={level} />
            ))}
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
