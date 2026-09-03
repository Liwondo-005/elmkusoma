const partners = [
  { name: "University of Dar es Salaam", abbr: "UDSM", color: "#1e40af" },
  { name: "Makerere University", abbr: "MU", color: "#7c3aed" },
  { name: "University of Nairobi", abbr: "UoN", color: "#0d9488" },
  { name: "VETA Tanzania", abbr: "VETA", color: "#dc2626" },
  { name: "African Leadership University", abbr: "ALU", color: "#f59e0b" },
  { name: "Mzumbe University", abbr: "MzU", color: "#0369a1" },
]

function PartnerLogo({ abbr, color }: { abbr: string; color: string }) {
  return (
    <svg viewBox="0 0 120 60" className="h-10 w-auto" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="1" y="1" width="118" height="58" rx="8" fill="white" stroke={color} strokeWidth="2" />
      <text
        x="60"
        y="35"
        textAnchor="middle"
        fill={color}
        fontSize="16"
        fontWeight="800"
        fontFamily="Inter, system-ui, sans-serif"
        letterSpacing="1"
      >
        {abbr}
      </text>
    </svg>
  )
}

export function AboutPartners() {
  return (
    <section className="py-16 lg:py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-balance text-3xl font-bold tracking-tight text-foreground">
            Trusted by leading institutions
          </h2>
          <p className="mt-3 text-pretty text-muted-foreground">
            We partner with universities, colleges and training institutions across Africa to deliver quality education at scale.
          </p>
        </div>

        <div className="mt-12 grid grid-cols-2 gap-6 sm:grid-cols-3 lg:grid-cols-6">
          {partners.map((partner) => (
            <div
              key={partner.name}
              className="flex flex-col items-center justify-center rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:border-primary/40 hover:shadow-md"
            >
              <PartnerLogo abbr={partner.abbr} color={partner.color} />
              <span className="mt-3 text-center text-xs font-medium text-muted-foreground">{partner.name}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
