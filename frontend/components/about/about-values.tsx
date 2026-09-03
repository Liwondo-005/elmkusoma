import { Target, Eye, Heart } from "lucide-react"

const values = [
  {
    icon: Target,
    title: "Our Mission",
    desc: "To make quality education accessible and affordable for every learner in Africa through technology — bridging the gap between students and expert teachers regardless of location or background.",
  },
  {
    icon: Eye,
    title: "Our Vision",
    desc: "To become Africa's most trusted and comprehensive digital learning platform — where every student has the tools, community and support they need to succeed academically and professionally.",
  },
  {
    icon: Heart,
    title: "Our Values",
    desc: "We believe in inclusion, quality, integrity and innovation. Every feature we build is designed with African learners in mind — from live interactive classes to verifiable certificates.",
  },
]

export function AboutValues() {
  return (
    <section className="bg-muted/50 py-16 lg:py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-balance text-3xl font-bold tracking-tight text-foreground">What drives us</h2>
          <p className="mt-3 text-pretty text-muted-foreground">
            Our mission, vision and values shape every decision we make — from the courses we host to the features we build.
          </p>
        </div>

        <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {values.map((v) => (
            <div key={v.title} className="rounded-2xl border border-border bg-card p-6">
              <div className="flex size-12 items-center justify-center rounded-xl bg-accent text-primary">
                <v.icon className="size-6" />
              </div>
              <h3 className="mt-4 text-base font-semibold text-foreground">{v.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{v.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
