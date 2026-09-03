import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { CollegeHero } from "@/components/schools/college-hero"
import { CollegeDepartments } from "@/components/schools/college-departments"
import { CollegeList } from "@/components/schools/college-list"
import { CollegeCta } from "@/components/schools/college-cta"

export default function CollegesPage() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <CollegeHero />
        <CollegeDepartments />
        <CollegeList />
        <CollegeCta />
      </main>
      <SiteFooter />
    </div>
  )
}
