import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { HeroSection } from "@/components/home/hero-section"
import { LevelsSection } from "@/components/home/levels-section"
import { LivePreviewSection } from "@/components/home/live-preview-section"
import { CoursesSection } from "@/components/home/courses-section"
import { HowItWorks } from "@/components/home/how-it-works"
import { CtaSection } from "@/components/home/cta-section"

export default function HomePage() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <HeroSection />
        <LevelsSection />
        <LivePreviewSection />
        <CoursesSection />
        <HowItWorks />
        <CtaSection />
      </main>
      <SiteFooter />
    </div>
  )
}
