import Image from "next/image"

const highlights = [
  { label: "8,500+ Learners" },
  { label: "500+ Instructors" },
]

export function AboutHero() {
  return (
    <section className="relative overflow-hidden">
      <div className="mx-auto max-w-7xl px-4 pb-12 pt-12 sm:px-6 lg:px-8 lg:pb-20 lg:pt-16">
        <div className="grid items-center gap-10 lg:grid-cols-2">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full border border-border bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
              <span className="size-1.5 rounded-full bg-teal" />
              About ELMKUSOMA
            </span>
            <h1 className="mt-5 text-balance text-4xl font-extrabold leading-[1.05] tracking-tight text-foreground sm:text-5xl lg:text-6xl">
              Empowering Africa&apos;s
              <br className="hidden sm:block" />
              Next Generation of <span className="text-teal">Learners</span>
            </h1>
            <p className="mt-5 max-w-lg text-pretty text-base leading-relaxed text-muted-foreground">
              ELMKUSOMA is a modern African EdTech platform built to deliver live classes, courses, recorded lessons
              and a digital library — connecting students with expert teachers across the continent.
            </p>

            <div className="mt-7 flex flex-wrap gap-3">
              {highlights.map((h) => (
                <span
                  key={h.label}
                  className="inline-flex items-center gap-2 rounded-lg border border-border bg-card px-4 py-2.5 text-sm font-medium text-foreground shadow-xs"
                >
                  {h.label}
                </span>
              ))}
            </div>
          </div>

          <div className="relative">
            <div className="relative overflow-hidden rounded-3xl border border-border bg-muted shadow-xl">
              <Image
                src="/images/hero-student.png"
                alt="Students learning together on ELMKUSOMA"
                width={720}
                height={620}
                priority
                className="h-full w-full object-cover"
              />
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
