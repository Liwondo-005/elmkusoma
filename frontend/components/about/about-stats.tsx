import { stats } from "@/lib/data"

export function AboutStats() {
  return (
    <section className="py-16 lg:py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {stats.map((s) => (
            <div key={s.label} className="rounded-2xl border border-border bg-card p-6 text-center shadow-xs">
              <p className="text-3xl font-extrabold text-primary">{s.value}</p>
              <p className="mt-2 text-sm font-medium text-foreground">{s.label}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
