import Link from "next/link"
import { buttonVariants } from "@/components/ui/button"
import { stats } from "@/lib/data"
import { cn } from "@/lib/utils"

export function CtaSection() {
  return (
    <section className="pb-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="overflow-hidden rounded-3xl border border-border bg-primary text-primary-foreground">
          <div className="p-8 sm:p-12">
            <h2 className="text-balance text-3xl font-bold tracking-tight sm:text-4xl">
              Ready to start learning?
            </h2>
            <p className="mt-4 max-w-md text-pretty text-primary-foreground/80">
              Join thousands of learners across Africa. Choose your education level and begin today.
            </p>
            <div className="mt-7 flex flex-wrap gap-3">
              <Link
                href="/courses"
                className={cn(buttonVariants(), "h-12 bg-background px-6 text-base text-foreground hover:bg-background/90")}
              >
                Browse Education Levels
              </Link>
              <Link
                href="/live-classes"
                className={cn(
                  buttonVariants({ variant: "outline" }),
                  "h-12 border-primary-foreground/30 bg-transparent px-6 text-base text-primary-foreground hover:bg-primary-foreground/10 hover:text-primary-foreground",
                )}
              >
                Explore Live Classes
              </Link>
            </div>

            <dl className="mt-10 grid grid-cols-2 gap-6 sm:grid-cols-4">
              {stats.map((s) => (
                <div key={s.label}>
                  <dt className="text-2xl font-extrabold">{s.value}</dt>
                  <dd className="mt-1 text-xs text-primary-foreground/70">{s.label}</dd>
                </div>
              ))}
            </dl>
          </div>
        </div>
      </div>
    </section>
  )
}
