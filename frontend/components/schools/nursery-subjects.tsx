import { BookOpen, PenTool, Calculator, Palette } from "lucide-react"
import { nurserySubjects } from "@/lib/data"

const iconMap: Record<string, typeof BookOpen> = {
  BookOpen,
  PenTool,
  Calculator,
  Palette,
}

export function NurserySubjects() {
  return (
    <section className="bg-muted/40 py-12 lg:py-16">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl">
          <h2 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
            Subjects
          </h2>
          <p className="mt-2 text-muted-foreground">
            Four core subjects taught through interactive activities.
          </p>
        </div>

        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {nurserySubjects.map((subject) => {
            const Icon = iconMap[subject.icon] || BookOpen
            return (
              <div
                key={subject.id}
                className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:border-primary/40 hover:shadow-md"
              >
                <div className={`flex size-11 items-center justify-center rounded-xl ${subject.color}`}>
                  <Icon className="size-5" />
                </div>
                <h3 className="mt-3 text-base font-bold text-foreground">{subject.name}</h3>
                <p className="mt-1 text-sm text-muted-foreground">{subject.desc}</p>
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
