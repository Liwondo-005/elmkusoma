const steps = [
  { title: "Create your account", desc: "Sign up and pick your education level to get a personalized experience." },
  { title: "Find your classes", desc: "Browse live classes, courses and your institution's learning content." },
  { title: "Learn live", desc: "Join interactive live sessions, chat with teachers and track your progress." },
  { title: "Earn certificates", desc: "Complete courses and receive verifiable certificates of achievement." },
]

export function HowItWorks() {
  return (
    <section className="py-16 lg:py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-balance text-3xl font-bold tracking-tight text-foreground">How ELMKUSOMA works</h2>
          <p className="mt-3 text-pretty text-muted-foreground">
            Getting started takes minutes — from sign up to your first live class.
          </p>
        </div>

        <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {steps.map((step, i) => (
            <div key={step.title} className="relative rounded-2xl border border-border bg-card p-6">
              <span className="absolute right-5 top-5 text-2xl font-extrabold text-muted/70">{i + 1}</span>
              <h3 className="text-base font-semibold text-foreground">{step.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{step.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
