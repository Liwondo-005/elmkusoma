import Link from "next/link"
import Image from "next/image"
import { buttonVariants } from "@/components/ui/button"
import { stats } from "@/lib/data"
import { cn } from "@/lib/utils"

export function CtaSection() {
  return (
    <section className="pb-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="overflow-hidden rounded-3xl border border-border bg-primary text-primary-foreground">
          <div className="grid items-center gap-8 lg:grid-cols-2">
            <div className="p-8 sm:p-12">
              <h2 className="text-balance text-3xl font-bold tracking-tight sm:text-4xl">
                Join thousands of learners achieving their goals
              </h2>
              <p className="mt-4 max-w-md text-pretty text-primary-foreground/80">
                Get certified, gain skills and advance your future with live, interactive learning built for Africa.
              </p>
              <div className="mt-7 flex flex-wrap gap-3">
                <Link
                  href="/register"
                  className={cn(buttonVariants(), "h-12 bg-background px-6 text-base text-foreground hover:bg-background/90")}
                >
                  Start Learning Free
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

            <div className="relative hidden h-full min-h-80 lg:block">
              <Image
                src="/images/cta-learner.png"
                alt="Happy learner using ELMKUSOMA"
                fill
                className="object-cover"
                sizes="50vw"
              />
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
