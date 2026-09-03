import Link from "next/link"
import { Video, BookOpen, Library, ArrowRight, Baby, School, Building2, Wrench, GraduationCap } from "lucide-react"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { buttonVariants } from "@/components/ui/button"
import { stats, levels } from "@/lib/data"
import { cn } from "@/lib/utils"

const levelIcons = [Baby, School, School, Building2, Wrench, GraduationCap]

const features = [
  {
    icon: Video,
    title: "Live & Interactive Classes",
    desc: "Join real-time sessions with expert teachers across Africa. Ask questions, participate in discussions and learn collaboratively.",
  },
  {
    icon: BookOpen,
    title: "Self-Paced Courses",
    desc: "Access hundreds of structured courses — from primary school subjects to university-level programs — learn at your own pace.",
  },
  {
    icon: Library,
    title: "Digital Library",
    desc: "Explore a growing collection of notes, textbooks and study materials curated by experienced educators.",
  },
]

export default function AboutPage() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        {/* Hero */}
        <section className="relative overflow-hidden bg-muted/40 py-16 lg:py-24">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="mx-auto max-w-3xl text-center">
              <span className="inline-flex items-center gap-2 rounded-full border border-border bg-background px-3 py-1 text-xs font-medium text-muted-foreground">
                <span className="size-1.5 rounded-full bg-teal" />
                About ELMKUSOMA
              </span>
              <h1 className="mt-5 text-balance text-4xl font-extrabold leading-[1.05] tracking-tight text-foreground sm:text-5xl">
                Empowering learners across{" "}
                <span className="text-teal">Africa</span>
              </h1>
              <p className="mt-5 text-pretty text-lg leading-relaxed text-muted-foreground">
                ELMKUSOMA is a unified EdTech platform bringing live classes, courses and digital learning
                to students at every education level — from nursery to university.
              </p>
            </div>
          </div>
        </section>

        {/* Stats */}
        <section className="border-y border-border bg-background py-12">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="grid grid-cols-2 gap-8 sm:grid-cols-4">
              {stats.map((s) => (
                <div key={s.label} className="text-center">
                  <p className="text-3xl font-extrabold text-foreground sm:text-4xl">{s.value}</p>
                  <p className="mt-1 text-sm text-muted-foreground">{s.label}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Mission */}
        <section className="py-16 lg:py-20">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="grid items-center gap-10 lg:grid-cols-2">
              <div>
                <h2 className="text-balance text-3xl font-bold tracking-tight text-foreground">
                  Our Mission
                </h2>
                <p className="mt-4 text-pretty text-muted-foreground leading-relaxed">
                  We believe every student in Africa deserves access to quality education, regardless of
                  location or background. ELMKUSOMA bridges the gap between learners and expert teachers
                  through technology — making education accessible, interactive and engaging.
                </p>
                <p className="mt-4 text-pretty text-muted-foreground leading-relaxed">
                  From live interactive classes to self-paced courses and a comprehensive digital library,
                  we provide everything learners need to succeed — all in one platform.
                </p>
              </div>
              <div className="rounded-3xl border border-border bg-muted/50 p-8">
                <h3 className="text-lg font-bold text-foreground">Our Vision</h3>
                <p className="mt-3 text-muted-foreground leading-relaxed">
                  To become Africa&apos;s leading digital education platform — connecting millions of learners
                  with world-class teachers and resources, and transforming how education is delivered
                  across the continent.
                </p>
                <div className="mt-6 space-y-3">
                  {["Accessible to all education levels", "Built for the African context", "Trusted by learners and institutions"].map((item) => (
                    <div key={item} className="flex items-center gap-3">
                      <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-teal/10 text-teal">
                        <svg className="size-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                        </svg>
                      </span>
                      <span className="text-sm text-foreground">{item}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Features */}
        <section className="bg-muted/50 py-16 lg:py-20">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="mx-auto max-w-2xl text-center">
              <h2 className="text-balance text-3xl font-bold tracking-tight text-foreground">
                What we offer
              </h2>
              <p className="mt-3 text-pretty text-muted-foreground">
                Everything you need for a complete learning experience — live, recorded and self-paced.
              </p>
            </div>
            <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {features.map((f) => (
                <div key={f.title} className="rounded-2xl border border-border bg-card p-6">
                  <div className="flex size-12 items-center justify-center rounded-xl bg-accent text-primary">
                    <f.icon className="size-6" />
                  </div>
                  <h3 className="mt-4 text-base font-semibold text-foreground">{f.title}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{f.desc}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Education Levels */}
        <section className="py-16 lg:py-20">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="mx-auto max-w-2xl text-center">
              <h2 className="text-balance text-3xl font-bold tracking-tight text-foreground">
                For every stage of learning
              </h2>
              <p className="mt-3 text-pretty text-muted-foreground">
                From nursery to university — we support learners at every education level across Africa.
              </p>
            </div>
            <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {levels.map((level, i) => {
                const Icon = levelIcons[i]
                return (
                  <Link
                    key={level.name}
                    href={level.href}
                    className="group flex items-center gap-4 rounded-2xl border border-border bg-card p-5 transition-all hover:border-primary/40 hover:shadow-md"
                  >
                    <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-accent text-primary transition-colors group-hover:bg-primary group-hover:text-primary-foreground">
                      <Icon className="size-6" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <h3 className="text-base font-semibold text-foreground">{level.name}</h3>
                      <p className="truncate text-sm text-muted-foreground">{level.desc}</p>
                    </div>
                    <ArrowRight className="size-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-1 group-hover:text-primary" />
                  </Link>
                )
              })}
            </div>
          </div>
        </section>

        {/* CTA */}
        <section className="pb-20">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="overflow-hidden rounded-3xl border border-border bg-primary p-8 text-primary-foreground sm:p-12">
              <div className="mx-auto max-w-2xl text-center">
                <h2 className="text-balance text-3xl font-bold tracking-tight sm:text-4xl">
                  Ready to start learning?
                </h2>
                <p className="mt-4 text-pretty text-primary-foreground/80">
                  Join thousands of students already learning on ELMKUSOMA. Create your free account
                  and start your learning journey today.
                </p>
                <div className="mt-7 flex flex-wrap justify-center gap-3">
                  <Link
                    href="/register"
                    className={cn(
                      buttonVariants(),
                      "h-12 bg-background px-6 text-base text-foreground hover:bg-background/90",
                    )}
                  >
                    Create Free Account
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
              </div>
            </div>
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  )
}
