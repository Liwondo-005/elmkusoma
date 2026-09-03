import { Baby, BookOpen, Palette } from "lucide-react"

export function NurseryHero() {
  return (
    <section className="relative overflow-hidden border-b border-border bg-gradient-to-br from-accent via-background to-accent/50">
      <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20 lg:px-8">
        <div className="max-w-3xl">
          <span className="inline-flex items-center gap-1.5 rounded-md bg-orange/10 px-2.5 py-1 text-xs font-semibold text-orange">
            <Baby className="size-3.5" />
            Nursery Level
          </span>
          <h1 className="mt-4 text-4xl font-bold tracking-tight text-foreground sm:text-5xl">
            Learn Through Play
          </h1>
          <p className="mt-4 max-w-xl text-lg text-muted-foreground">
            Fun, interactive classes for the youngest learners. Explore nursery schools with
            Baby 1, Baby 2 and Baby 3 levels — taught through games, stories and creative activities.
          </p>

          <div className="mt-8 flex flex-wrap gap-6">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <div className="flex size-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <Baby className="size-4" />
              </div>
              <span className="font-medium text-foreground">3 Levels</span>
              <span>Baby 1–3</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <div className="flex size-8 items-center justify-center rounded-lg bg-teal/10 text-teal">
                <BookOpen className="size-4" />
              </div>
              <span className="font-medium text-foreground">4 Subjects</span>
              <span>Kusoma, Kuandika & more</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <div className="flex size-8 items-center justify-center rounded-lg bg-orange/10 text-orange">
                <Palette className="size-4" />
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
