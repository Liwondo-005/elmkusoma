import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { NurseryHero } from "@/components/schools/nursery-hero"
import { NurseryLevels } from "@/components/schools/nursery-levels"
import { NurserySubjects } from "@/components/schools/nursery-subjects"
import { NurserySchools } from "@/components/schools/nursery-schools"
import { NurseryCta } from "@/components/schools/nursery-cta"

export default function NurseryPage() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <NurseryHero />
        <NurseryLevels />
        <NurserySubjects />
        <NurserySchools />
        <NurseryCta />
      </main>
      <SiteFooter />
    </div>
  )
}
