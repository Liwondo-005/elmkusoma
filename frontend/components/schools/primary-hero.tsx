import { School, BookOpen, GraduationCap } from "lucide-react"

export function PrimaryHero() {
  return (
    <section className="relative overflow-hidden border-b border-border bg-gradient-to-br from-accent via-background to-accent/50">
      <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20 lg:px-8">
        <div className="max-w-3xl">
          <span className="inline-flex items-center gap-1.5 rounded-md bg-primary/10 px-2.5 py-1 text-xs font-semibold text-primary">
            <School className="size-3.5" />
            Primary Level
          </span>
          <h1 className="mt-4 text-4xl font-bold tracking-tight text-foreground sm:text-5xl">
            Darasa I — VI
          </h1>
          <p className="mt-4 max-w-xl text-lg text-muted-foreground">
            Complete primary education from Standard 1 to Standard 6. Structured
            classes with the full Tanzanian curriculum — Kiswahili, English, Hisabati,
            Sayansi and more.
          </p>

          <div className="mt-8 flex flex-wrap gap-6">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <div className="flex size-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <School className="size-4" />
              </div>
              <span className="font-medium text-foreground">6 Classes</span>
              <span>Darasa I – VI</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <div className="flex size-8 items-center justify-center rounded-lg bg-teal/10 text-teal">
                <BookOpen className="size-4" />
              </div>
              <span className="font-medium text-foreground">8 Subjects</span>
              <span>Full TZ curriculum</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <div className="flex size-8 items-center justify-center rounded-lg bg-orange/10 text-orange">
                <GraduationCap className="size-4" />
              </div>
              <span className="font-medium text-foreground">5 Schools</span>
              <span>Across Tanzania</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
