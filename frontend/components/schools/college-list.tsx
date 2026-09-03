import Link from "next/link"
import { MapPin, Users, Star, ArrowRight } from "lucide-react"
import { collegeList, collegeDepartments } from "@/lib/data"
import { cn } from "@/lib/utils"
import { buttonVariants } from "@/components/ui/button"

export function CollegeList() {
  return (
    <section className="bg-muted/40 py-12 lg:py-16">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl">
          <h2 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
            Institutions
          </h2>
          <p className="mt-2 text-muted-foreground">
            Browse colleges and universities offering diploma and degree programs.
          </p>
        </div>

        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {collegeList.map((college) => {
            const deptNames = college.departments
              .map((dId) => collegeDepartments.find((d) => d.id === dId)?.name)
              .filter(Boolean)

            return (
              <div
                key={college.id}
                className="group flex flex-col rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:border-primary/40 hover:shadow-md"
              >
                <div className="flex items-start justify-between">
                  <div>
                    <h3 className="text-base font-bold text-foreground">{college.name}</h3>
                    <div className="mt-1 flex items-center gap-3 text-xs text-muted-foreground">
                      <span className="flex items-center gap-1">
                        <MapPin className="size-3" />
                        {college.location}
                      </span>
                      <span className="flex items-center gap-1">
                        <Users className="size-3" />
                        {college.students.toLocaleString()} students
                      </span>
                    </div>
                  </div>
                  <div className="flex items-center gap-1 rounded-md bg-orange/10 px-2 py-0.5">
                    <Star className="size-3 fill-orange text-orange" />
                    <span className="text-xs font-semibold text-orange">{college.rating}</span>
                  </div>
                </div>

                <div className="mt-3 flex flex-wrap gap-1.5">
                  <span className="rounded-md bg-primary/10 px-2 py-0.5 text-xs font-semibold text-primary">
                    {college.abbreviation}
                  </span>
                  {deptNames.map((name) => (
                    <span
                      key={name}
                      className="rounded-md bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground"
                    >
                      {name?.split(" ")[0]}
                    </span>
                  ))}
                </div>

                <p className="mt-3 flex-1 text-sm text-muted-foreground">{college.description}</p>

                <div className="mt-4 pt-1">
                  <Link
                    href={`/schools/colleges/${college.id}`}
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
            )
          })}
        </div>
      </div>
    </section>
  )
}
