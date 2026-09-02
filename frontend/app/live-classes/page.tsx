import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { LiveClassesBrowser } from "@/components/live/live-classes-browser"

export default function LiveClassesPage() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <section className="border-b border-border bg-muted/40">
          <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
            <span className="inline-flex items-center gap-1.5 rounded-md bg-teal/10 px-2.5 py-1 text-xs font-semibold text-teal">
              <span className="size-1.5 animate-pulse rounded-full bg-teal" />
              Live Learning
            </span>
            <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">Live Classes</h1>
            <p className="mt-2 max-w-2xl text-muted-foreground">
              Join live classes, interact with teachers and students in real time. Browse what&apos;s live now, coming
              up, or catch up on past sessions.
            </p>
          </div>
        </section>
        <LiveClassesBrowser />
      </main>
      <SiteFooter />
    </div>
  )
}
