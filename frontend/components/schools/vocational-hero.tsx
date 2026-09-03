import { Wrench, BookOpen, MapPin } from "lucide-react"

export function VocationalHero() {
  return (
    <section className="relative overflow-hidden border-b border-border bg-gradient-to-br from-accent via-background to-accent/50">
      <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20 lg:px-8">
        <div className="max-w-3xl">
          <span className="inline-flex items-center gap-1.5 rounded-md bg-teal/10 px-2.5 py-1 text-xs font-semibold text-teal">
            <Wrench className="size-3.5" />
            Vocational Level
          </span>
          <h1 className="mt-4 text-4xl font-bold tracking-tight text-foreground sm:text-5xl">
            VETA Training & Modules
          </h1>
          <p className="mt-4 max-w-xl text-lg text-muted-foreground">
            Practical skills training through Tanzania's VETA system. Learn a trade
            from construction to ICT, automotive to hospitality — with hands-on
            experience and trade certificates.
          </p>

          <div className="mt-8 flex flex-wrap gap-6">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <div className="flex size-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <BookOpen className="size-4" />
              </div>
              <span className="font-medium text-foreground">6 Categories</span>
              <span>17 trade programs</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <div className="flex size-8 items-center justify-center rounded-lg bg-teal/10 text-teal">
                <Wrench className="size-4" />
              </div>
              <span className="font-medium text-foreground">Trade Certificates</span>
              <span>1–2 year programs</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <div className="flex size-8 items-center justify-center rounded-lg bg-orange/10 text-orange">
                <MapPin className="size-4" />
              </div>
              <span className="font-medium text-foreground">5 VETA Centers</span>
              <span>Across Tanzania</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
