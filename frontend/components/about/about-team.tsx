import Image from "next/image"

const team = [
  {
    name: "Dr. Amina Juma",
    role: "Founder & CEO",
    image: "/images/team-1.png",
    bio: "Education technologist with 12+ years of experience building digital learning solutions across East Africa.",
    social: { linkedin: "#", twitter: "#" },
  },
  {
    name: "David Ochieng",
    role: "Head of Product",
    image: "/images/team-2.png",
    bio: "Product leader passionate about creating intuitive learning experiences that scale to millions.",
    social: { linkedin: "#", twitter: "#" },
  },
  {
    name: "Grace Nkomo",
    role: "Head of Content",
    image: "/images/team-3.png",
    bio: "Curriculum specialist dedicated to curating world-class courses and live class experiences.",
    social: { linkedin: "#", twitter: "#" },
  },
  {
    name: "Samuel Mwangi",
    role: "CTO",
    image: "/images/team-4.png",
    bio: "Full-stack engineer focused on building reliable, fast and accessible educational technology.",
    social: { linkedin: "#", twitter: "#" },
  },
]

function TeamAvatar({ image, name, initials }: { image: string; name: string; initials: string }) {
  return (
    <div className="relative aspect-square overflow-hidden bg-accent">
      <Image
        src={image}
        alt={name}
        fill
        className="object-cover"
        sizes="(max-width: 768px) 100vw, 25vw"
      />
      <div className="absolute inset-0 flex items-center justify-center bg-accent">
        <span className="text-4xl font-extrabold text-primary/30">{initials}</span>
      </div>
    </div>
  )
}

export function AboutTeam() {
  return (
    <section className="bg-muted/50 py-16 lg:py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-balance text-3xl font-bold tracking-tight text-foreground">Meet the team</h2>
          <p className="mt-3 text-pretty text-muted-foreground">
            The people building ELMKUSOMA — educators, engineers and dreamers united by a shared mission.
          </p>
        </div>

        <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {team.map((member) => {
            const initials = member.name
              .split(" ")
              .filter((_, i, arr) => i === 0 || i === arr.length - 1)
              .map((n) => n[0])
              .join("")
            return (
              <div
                key={member.name}
                className="group overflow-hidden rounded-2xl border border-border bg-card shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg"
              >
                <TeamAvatar image={member.image} name={member.name} initials={initials} />
                <div className="p-5">
                  <h3 className="text-base font-semibold text-foreground">{member.name}</h3>
                  <p className="mt-0.5 text-sm font-medium text-primary">{member.role}</p>
                  <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{member.bio}</p>
                  <div className="mt-3 flex gap-3">
                    <a href={member.social.linkedin} aria-label={`${member.name} on LinkedIn`} className="text-sm text-muted-foreground transition-colors hover:text-primary">
                      LinkedIn
                    </a>
                    <a href={member.social.twitter} aria-label={`${member.name} on Twitter`} className="text-sm text-muted-foreground transition-colors hover:text-primary">
                      Twitter
                    </a>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
