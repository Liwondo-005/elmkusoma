import Link from "next/link"
import { liveClasses } from "@/lib/data"
import { LiveClassCard } from "@/components/live-class-card"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

export function LivePreviewSection() {
  return (
    <section className="py-16 lg:py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <span className="inline-flex items-center gap-1.5 rounded-md bg-teal/10 px-2.5 py-1 text-xs font-semibold text-teal">
                <span className="size-1.5 animate-pulse rounded-full bg-teal" />
                Live Now
              </span>
              <span className="text-sm text-muted-foreground">Real-time classes in session</span>
            </div>
            <h2 className="mt-3 text-3xl font-bold tracking-tight text-foreground">Live Classes</h2>
            <p className="mt-2 max-w-xl text-muted-foreground">
              Join live classes, interact with teachers and students in real time.
            </p>
          </div>
          <Link
            href="/live-classes"
            className={cn(buttonVariants({ variant: "outline" }), "h-10 gap-2 px-4")}
          >
            View All Live Classes
          </Link>
        </div>

        <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {liveClasses.slice(0, 4).map((item) => (
            <LiveClassCard key={item.id} item={item} />
          ))}
        </div>
      </div>
    </section>
  )
}
