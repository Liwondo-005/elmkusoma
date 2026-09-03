import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { VocationalHero } from "@/components/schools/vocational-hero"
import { VocationalTrades } from "@/components/schools/vocational-trades"
import { VocationalCenters } from "@/components/schools/vocational-centers"
import { VocationalCta } from "@/components/schools/vocational-cta"

export default function VocationalPage() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <VocationalHero />
        <VocationalTrades />
        <VocationalCenters />
        <VocationalCta />
      </main>
      <SiteFooter />
    </div>
  )
}
