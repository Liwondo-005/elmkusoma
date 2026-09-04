import { nurseryLevels } from "@/lib/data"

export function NurseryLevels() {
  return (
    <section className="py-12 lg:py-16">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl">
          <h2 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
            Baby Levels
          </h2>
          <p className="mt-2 text-muted-foreground">
            Three progressive levels designed for children aged 2 to 5 years.
          </p>
        </div>

        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {nurseryLevels.map((level, i) => {
            return (
              <div
                key={level.id}
                className="group rounded-2xl border border-border bg-card p-6 shadow-xs transition-all hover:border-primary/40 hover:shadow-md"
              >
                <div className="mt-4">
                  <div className="flex items-center gap-2">
                    <h3 className="text-lg font-bold text-foreground">{level.name}</h3>
                    <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
                      {level.ages}
                    </span>
                  </div>
                  <p className="mt-1.5 text-sm text-muted-foreground">{level.desc}</p>
                </div>
                <div className="mt-4 flex flex-wrap gap-2">
                  {["Reading", "Writing", "Counting", "Drawing"].map((s) => (
                    <span
                      key={s}
                      className="rounded-md bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground"
                    >
                      {s}
                    </span>
                  ))}
                </div>
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
