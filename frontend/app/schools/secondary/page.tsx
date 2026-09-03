import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { SecondaryHero } from "@/components/schools/secondary-hero"
import { SecondaryLevels } from "@/components/schools/secondary-levels"
import { SecondarySchools } from "@/components/schools/secondary-schools"
import { SecondaryCta } from "@/components/schools/secondary-cta"

export default function SecondaryPage() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <SecondaryHero />
        <SecondaryLevels />
        <SecondarySchools />
        <SecondaryCta />
      </main>
      <SiteFooter />
    </div>
  )
}
