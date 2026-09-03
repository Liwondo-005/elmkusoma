import Link from "next/link"
import { MapPin, Users, Star, ArrowRight } from "lucide-react"
import { universities } from "@/lib/data"
import { cn } from "@/lib/utils"
import { buttonVariants } from "@/components/ui/button"

export function UniversityList() {
  return (
    <section className="bg-muted/40 py-12 lg:py-16">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl">
          <h2 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
            Partner Universities
          </h2>
          <p className="mt-2 text-muted-foreground">
            Browse Tanzania's top universities. Each institution offers programs across multiple degree levels.
          </p>
        </div>

        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {universities.map((uni) => (
            <div
              key={uni.id}
              className="group flex flex-col rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:border-primary/40 hover:shadow-md"
            >
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="text-base font-bold text-foreground">{uni.name}</h3>
                  <div className="mt-1 flex items-center gap-3 text-xs text-muted-foreground">
                    <span className="flex items-center gap-1">
                      <MapPin className="size-3" />
                      {uni.location}
                    </span>
                    <span className="flex items-center gap-1">
                      <Users className="size-3" />
                      {uni.students.toLocaleString()} students
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-1 rounded-md bg-orange/10 px-2 py-0.5">
                  <Star className="size-3 fill-orange text-orange" />
                  <span className="text-xs font-semibold text-orange">{uni.rating}</span>
                </div>
              </div>

              <div className="mt-3 flex flex-wrap gap-1.5">
                <span className="rounded-md bg-primary/10 px-2 py-0.5 text-xs font-semibold text-primary">
                  {uni.abbreviation}
                </span>
                {uni.degreeLevels.map((level) => (
                  <span
                    key={level}
                    className="rounded-md bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground capitalize"
                  >
                    {level.replace("-", " ")}
                  </span>
                ))}
              </div>

              <div className="mt-3 flex flex-wrap gap-1">
                {uni.programs.slice(0, 4).map((prog) => (
                  <span
                    key={prog}
                    className="rounded bg-muted/60 px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground"
                  >
                    {prog}
                  </span>
                ))}
                {uni.programs.length > 4 && (
                  <span className="rounded bg-muted/60 px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground">
                    +{uni.programs.length - 4} more
                  </span>
                )}
              </div>

              <p className="mt-3 flex-1 text-sm text-muted-foreground">{uni.description}</p>

              <div className="mt-4 pt-1">
                <Link
                  href={`/schools/universities/${uni.id}`}
                  className={cn(
                    buttonVariants({ variant: "outline" }),
                    "h-9 w-full gap-2 border-primary/30 text-primary hover:bg-accent",
                  )}
                >
                  View Programs
                  <ArrowRight className="size-3.5" />
                </Link>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
