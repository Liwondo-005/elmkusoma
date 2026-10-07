import Image from "next/image"

// Real ELMKUSOMA team. Only verified name/role/photo data — no invented
// bios, social accounts or contact details are added here.
const team = [
  {
    name: "Eng. Arthur C. A. Assenga",
    role: "Founder",
    image: "/images/team/founder.webp",
  },
  {
    name: "Asimwe A. Manyusi",
    role: "Full-Stack Engineer",
    image: "/images/team/asimwe.webp",
  },
  {
    name: "Suleji R. Issa",
    role: "IT",
    image: "/images/team/suleji.webp",
  },
  {
    name: "Johnson M. Wilson",
    role: "Computer Engineer",
    image: "/images/team/johnson.webp",
  },
  {
    name: "Idda S. Ngaiza",
    role: "Frontend Developer",
    image: "/images/team/idda.webp",
  },
  {
    name: "Twalhiya Amour Ally",
    role: "Computer Engineer",
    image: "/images/team/twally.webp",
  },
  {
    name: "Mwanaidi S. Shabani",
    role: "IT",
    image: "/images/team/naah.webp",
  },
]

function TeamPhoto({ image, name }: { image: string; name: string }) {
  return (
    // Every supplied photo is a 4:5 portrait, so the container matches the
    // source ratio exactly — object-cover crops rather than distorts faces.
    <div className="relative aspect-[4/5] overflow-hidden bg-muted">
      <Image
        src={image}
        alt={name}
        fill
        className="object-cover"
        sizes="(max-width: 640px) 70vw, 260px"
      />
    </div>
  )
}

function TeamCard({ member }: { member: (typeof team)[number] }) {
  return (
    // Fixed basis + grow keeps every card the same width while letting them
    // share leftover space, so nothing stretches or collapses when wrapping.
    <div className="w-full max-w-[260px] flex-1 basis-[240px]">
      <div className="group h-full overflow-hidden rounded-2xl border border-border bg-card shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg">
        <TeamPhoto image={member.image} name={member.name} />
        <div className="p-5">
          <h3 className="text-balance text-base font-semibold text-foreground">{member.name}</h3>
          <p className="mt-1 text-sm font-medium text-primary">{member.role}</p>
        </div>
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

        {/* Stationary wrapping layout. flex-wrap + justify-center centres
            every row, including a final row with fewer cards, which a
            column grid could not do. justify-center is what keeps the
            leftover 3 cards centred under the first 4 on desktop. */}
        <div className="mt-12 flex flex-wrap justify-center gap-6">
          {team.map((member) => (
            <TeamCard key={member.name} member={member} />
          ))}
        </div>
      </div>
    </section>
  )
}