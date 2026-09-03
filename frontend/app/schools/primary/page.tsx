import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { PrimaryHero } from "@/components/schools/primary-hero"
import { PrimaryClasses } from "@/components/schools/primary-classes"
import { PrimarySchools } from "@/components/schools/primary-schools"
import { PrimaryCta } from "@/components/schools/primary-cta"

export default function PrimaryPage() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <PrimaryHero />
        <PrimaryClasses />
        <PrimarySchools />
        <PrimaryCta />
      </main>
      <SiteFooter />
    </div>
  )
}
