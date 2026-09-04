import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { CollegeCard } from "@/components/courses/college-card"
import { colleges } from "@/lib/data"
import Link from "next/link"


export const metadata = {
  title: "Colleges & Universities — ELMKUSOMA",
  description: "Explore degree, diploma, and certificate programmes from partner colleges and universities in Tanzania.",
}

export default function CollegesPage() {
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
              All Levels
            </Link>
            <span className="mt-4 inline-flex items-center gap-1.5 rounded-md bg-sky-100 px-2.5 py-1 text-xs font-semibold text-sky-700">
              Higher Education
            </span>
            <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
              Colleges & Universities
            </h1>
            <p className="mt-2 max-w-2xl text-muted-foreground">
              Browse degree, diploma, and certificate programmes from partner institutions.
            </p>
          </div>
        </section>

        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {colleges.map((college) => (
              <CollegeCard key={college.id} college={college} />
            ))}
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
