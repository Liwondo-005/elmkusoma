import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { UniversityHero } from "@/components/schools/university-hero"
import { UniversityLevels } from "@/components/schools/university-levels"
import { UniversityList } from "@/components/schools/university-list"
import { UniversityCta } from "@/components/schools/university-cta"

export default function UniversitiesPage() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <UniversityHero />
        <UniversityLevels />
        <UniversityList />
        <UniversityCta />
      </main>
      <SiteFooter />
    </div>
  )
}
