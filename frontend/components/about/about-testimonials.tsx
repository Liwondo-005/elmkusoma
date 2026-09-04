const testimonials = [
  {
    name: "Aisha Hassan",
    role: "University Student, Dar es Salaam",
    initials: "AH",
    quote:
      "ELMKUSOMA changed how I study. The live classes feel like being in a real classroom, and I can ask questions in real time. I've completed three courses and earned certificates that I proudly share on LinkedIn.",
    rating: 5,
  },
  {
    name: "Brian Omondi",
    role: "Secondary School Student, Nairobi",
    initials: "BO",
    quote:
      "The recorded lessons help me revise at my own pace. The teachers are excellent and the platform is so easy to use — even on my phone. I've improved my grades significantly since joining.",
    rating: 5,
  },
  {
    name: "Fatima Ali",
    role: "Vocational Training, VETA",
    initials: "FA",
    quote:
      "As a vocational student, finding quality online learning was hard until ELMKUSOMA. The courses are practical, the instructors are experienced, and I can learn from anywhere in Tanzania.",
    rating: 5,
  },
]

export function AboutTestimonials() {
  return (
    <section className="bg-muted/50 py-16 lg:py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-balance text-3xl font-bold tracking-tight text-foreground">
            What learners say
          </h2>
          <p className="mt-3 text-pretty text-muted-foreground">
            Real stories from students and professionals using ELMKUSOMA to achieve their learning goals.
          </p>
        </div>

        <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {testimonials.map((t) => (
            <div
              key={t.name}
              className="rounded-2xl border border-border bg-card p-6 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg"
            >
              <p className="text-sm leading-relaxed text-muted-foreground">{t.quote}</p>
              <div className="mt-4 flex items-center gap-3 border-t border-border pt-4">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-accent text-sm font-bold text-primary">
                  {t.initials}
                </div>
                <div>
                  <p className="text-sm font-semibold text-foreground">{t.name}</p>
                  <p className="text-xs text-muted-foreground">{t.role}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
