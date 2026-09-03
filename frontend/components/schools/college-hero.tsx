import { Building2, BookOpen, Users } from "lucide-react"

export function CollegeHero() {
  return (
    <section className="relative overflow-hidden border-b border-border bg-gradient-to-br from-accent via-background to-accent/50">
      <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20 lg:px-8">
        <div className="max-w-3xl">
          <span className="inline-flex items-center gap-1.5 rounded-md bg-orange/10 px-2.5 py-1 text-xs font-semibold text-orange">
            <Building2 className="size-3.5" />
            College Level
          </span>
          <h1 className="mt-4 text-4xl font-bold tracking-tight text-foreground sm:text-5xl">
            Diploma & Degree Programs
          </h1>
          <p className="mt-4 max-w-xl text-lg text-muted-foreground">
            Higher education institutions offering diploma and degree programs across
            engineering, business, ICT, health, education and agriculture.
          </p>

          <div className="mt-8 flex flex-wrap gap-6">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <div className="flex size-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <BookOpen className="size-4" />
              </div>
              <span className="font-medium text-foreground">6 Departments</span>
              <span>Engineering to Health</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <div className="flex size-8 items-center justify-center rounded-lg bg-teal/10 text-teal">
                <Building2 className="size-4" />
              </div>
              <span className="font-medium text-foreground">30+ Programs</span>
              <span>Diploma & Degree</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <div className="flex size-8 items-center justify-center rounded-lg bg-orange/10 text-orange">
                <Users className="size-4" />
              </div>
              <span className="font-medium text-foreground">5 Institutions</span>
              <span>Across Tanzania</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
