import Link from "next/link"
import { levels } from "@/lib/data"

export function LevelsSection() {
  return (
    <section className="bg-muted/50 py-16 lg:py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl">
          <h2 className="text-balance text-3xl font-bold tracking-tight text-foreground">
            One platform for every stage of learning
          </h2>
          <p className="mt-3 text-pretty text-muted-foreground">
            From nursery games to university degrees — discover the right experience for every education level across
            Africa.
          </p>
        </div>

        <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {levels.map((level, i) => {
            return (
              <Link
                key={level.name}
                href={level.href}
                className="group flex items-center gap-4 rounded-2xl border border-border bg-card p-5 transition-all hover:border-primary/40 hover:shadow-md"
              >
                <div className="min-w-0 flex-1">
                  <h3 className="text-base font-semibold text-foreground">{level.name}</h3>
                  <p className="truncate text-sm text-muted-foreground">{level.desc}</p>
                </div>
              </Link>
            )
          })}
        </div>
      </div>
    </section>
  )
}
