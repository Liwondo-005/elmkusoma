import { GraduationCap, BookOpen, Layers } from "lucide-react"

export function SecondaryHero() {
  return (
    <section className="relative overflow-hidden border-b border-border bg-gradient-to-br from-accent via-background to-accent/50">
      <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20 lg:px-8">
        <div className="max-w-3xl">
          <span className="inline-flex items-center gap-1.5 rounded-md bg-teal/10 px-2.5 py-1 text-xs font-semibold text-teal">
            <GraduationCap className="size-3.5" />
            Secondary Level
          </span>
          <h1 className="mt-4 text-4xl font-bold tracking-tight text-foreground sm:text-5xl">
            O-Level & A-Level
          </h1>
          <p className="mt-4 max-w-xl text-lg text-muted-foreground">
            Complete secondary education from Form I to Form VI. O-Level builds
            foundations across 15+ subjects, A-Level specializes through combination
            groups like PCM, PCB, HGL and ECA.
          </p>

          <div className="mt-8 flex flex-wrap gap-6">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <div className="flex size-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <BookOpen className="size-4" />
              </div>
              <span className="font-medium text-foreground">6 Forms</span>
              <span>Form I – VI</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <div className="flex size-8 items-center justify-center rounded-lg bg-teal/10 text-teal">
                <Layers className="size-4" />
              </div>
              <span className="font-medium text-foreground">6 Combinations</span>
              <span>A-Level groups</span>
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
