export function NurseryHero() {
  return (
    <section className="relative overflow-hidden border-b border-border bg-gradient-to-br from-accent via-background to-accent/50">
      <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20 lg:px-8">
        <div className="max-w-3xl">
          <span className="inline-flex items-center gap-1.5 rounded-md bg-orange/10 px-2.5 py-1 text-xs font-semibold text-orange">
            Nursery Level
          </span>
          <h1 className="mt-4 text-4xl font-bold tracking-tight text-foreground sm:text-5xl">
            Learn Through Play
          </h1>
          <p className="mt-4 max-w-xl text-lg text-muted-foreground">
            Fun, interactive classes for the youngest learners. Explore nursery schools with
            Baby, Middle and Top levels — taught through games, stories and creative activities.
          </p>

          <div className="mt-8 flex flex-wrap gap-6">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <span className="font-medium text-foreground">3 Levels</span>
              <span>Baby, Middle & Top</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <span className="font-medium text-foreground">4 Subjects</span>
              <span>Reading, Writing & more</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <span className="font-medium text-foreground">5 Schools</span>
              <span>Across Tanzania</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
