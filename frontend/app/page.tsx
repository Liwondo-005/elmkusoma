import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { HeroSection } from "@/components/home/hero-section"
import { LevelsSection } from "@/components/home/levels-section"
import { LivePreviewSection } from "@/components/home/live-preview-section"
import { CoursesSection } from "@/components/home/courses-section"
import { NewsSection } from "@/components/home/news-section"
import { HowItWorks } from "@/components/home/how-it-works"
import { CtaSection } from "@/components/home/cta-section"
import { HeadquartersSection } from "@/components/home/headquarters-section"
import { ContactSection } from "@/components/home/contact-section"

export default function HomePage() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <HeroSection />
        <LevelsSection />
        <LivePreviewSection />
        <CoursesSection />
        <NewsSection />
        <HowItWorks />
        <CtaSection />
        {/* Headquarters sits directly after the contact form and immediately before the footer:
            a visitor who has just asked a question is the one most likely to want to know where
            the organisation actually is. */}
        <HeadquartersSection />
        <ContactSection />
      </main>
      <SiteFooter />
    </div>
  )
}
