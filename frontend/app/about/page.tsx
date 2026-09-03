import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { AboutHero } from "@/components/about/about-hero"
import { AboutStats } from "@/components/about/about-stats"
import { AboutValues } from "@/components/about/about-values"
import { AboutHow } from "@/components/about/about-how"
import { AboutTeam } from "@/components/about/about-team"
import { AboutPartners } from "@/components/about/about-partners"
import { AboutTestimonials } from "@/components/about/about-testimonials"
import { AboutCta } from "@/components/about/about-cta"

export const metadata = {
  title: "About — ELMKUSOMA",
  description:
    "Learn about ELMKUSOMA's mission to make quality education accessible for every learner in Africa through live classes, courses and a digital library.",
}

export default function AboutPage() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <AboutHero />
        <AboutStats />
        <AboutValues />
        <AboutHow />
        <AboutTeam />
        <AboutPartners />
        <AboutTestimonials />
        <AboutCta />
      </main>
      <SiteFooter />
    </div>
  )
}
