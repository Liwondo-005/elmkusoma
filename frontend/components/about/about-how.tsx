const steps = [
  {
    title: "Sign Up for Free",
    desc: "Create your account in seconds and pick your education level — from nursery to university — for a personalised experience.",
  },
  {
    title: "Browse & Enrol",
    desc: "Explore live classes, self-paced courses, and your institution's learning content. Enrol in what suits your goals.",
  },
  {
    title: "Learn Live & Interactively",
    desc: "Join real-time sessions, ask questions, participate in quizzes and collaborate with peers — all from one platform.",
  },
  {
    title: "Earn Verified Certificates",
    desc: "Complete courses and receive certificates of achievement that can be verified online by employers and institutions.",
  },
]

export function AboutHow() {
  return (
    <section className="py-16 lg:py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-balance text-3xl font-bold tracking-tight text-foreground">
            How ELMKUSOMA works
          </h2>
          <p className="mt-3 text-pretty text-muted-foreground">
            Getting started takes minutes — from sign-up to your first live class.
          </p>
        </div>

        <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {steps.map((step, i) => (
            <div key={step.title} className="relative rounded-2xl border border-border bg-card p-6">
              <span className="absolute right-5 top-5 text-2xl font-extrabold text-muted/70">{i + 1}</span>
              <h3 className="mt-4 text-base font-semibold text-foreground">{step.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{step.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
