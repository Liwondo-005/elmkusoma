import { notFound } from "next/navigation"
import Link from "next/link"
import { ArrowLeft } from "lucide-react"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { InstitutionDetail } from "@/components/schools/institution-detail"
import { getInstitutionById, getInstitutionsByLevel } from "@/lib/data"
import type { Metadata } from "next"

const EDUCATION_LEVEL = "Secondary"

export function generateStaticParams() {
  return getInstitutionsByLevel(EDUCATION_LEVEL).map((inst) => ({ id: inst.id }))
}

export async function generateMetadata({ params }: { params: Promise<{ id: string }> }): Promise<Metadata> {
  const { id } = await params
  const institution = getInstitutionById(id)
  if (!institution) return { title: "Not Found — ELMKUSOMA" }
  return {
    title: `${institution.name} — ${EDUCATION_LEVEL} — ELMKUSOMA`,
    description: institution.description,
  }
}

export default async function InstitutionPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params
  const institution = getInstitutionById(id)
  if (!institution) notFound()

  const levelRoutes: Record<string, string> = {
    Nursery: "/schools/nursery",
    Primary: "/schools/primary",
    Secondary: "/schools/secondary",
    Colleges: "/schools/colleges",
    Vocational: "/schools/vocational",
    Universities: "/schools/universities",
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <section className="border-b border-border bg-muted/40">
          <div className="mx-auto max-w-7xl px-4 py-4 sm:px-6 lg:px-8">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Link href={levelRoutes[institution.educationLevel]} className="transition-colors hover:text-foreground">
                {institution.educationLevel}
              </Link>
              <span>/</span>
              <span className="text-foreground">{institution.name}</span>
            </div>
          </div>
        </section>

        <InstitutionDetail institution={institution} />

        <section className="border-t border-border bg-muted/40 py-8">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 text-center">
            <Link
              href={levelRoutes[institution.educationLevel]}
              className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:underline"
            >
              <ArrowLeft className="size-4" />
              Browse More {institution.educationLevel} Schools
            </Link>
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  )
}
