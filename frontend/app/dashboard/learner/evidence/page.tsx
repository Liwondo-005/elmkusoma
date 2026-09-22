"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { Portfolio, PracticalDemonstration, Project, FieldworkPlacement, CompetencyRecord } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { Award, FolderOpen, Target, BookOpen, Bookmark, ChevronRight, AlertCircle } from "lucide-react"

export default function MyEvidencePage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [portfolios, setPortfolios] = useState<Portfolio[]>([])
  const [demonstrations, setDemonstrations] = useState<PracticalDemonstration[]>([])
  const [projects, setProjects] = useState<Project[]>([])
  const [fieldwork, setFieldwork] = useState<FieldworkPlacement[]>([])
  const [competencies, setCompetencies] = useState<CompetencyRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadEvidence()
  }, [user])

  async function loadEvidence() {
    try {
      setLoading(true)
      const studentId = user?.id || ""
      const [portRes, demoRes, projRes, fwRes, compRes] = await Promise.allSettled([
        collegeApi.getStudentPortfolio(studentId),
        collegeApi.getStudentDemonstrations(studentId),
        collegeApi.getStudentProjects(studentId),
        collegeApi.getStudentFieldwork(studentId),
        collegeApi.getStudentCompetencies(studentId),
      ])
      if (portRes.status === "fulfilled") setPortfolios(Array.isArray(portRes.value) ? portRes.value as Portfolio[] : [])
      if (demoRes.status === "fulfilled") setDemonstrations(Array.isArray(demoRes.value) ? demoRes.value as PracticalDemonstration[] : [])
      if (projRes.status === "fulfilled") setProjects(Array.isArray(projRes.value) ? projRes.value as Project[] : [])
      if (fwRes.status === "fulfilled") setFieldwork(Array.isArray(fwRes.value) ? fwRes.value as FieldworkPlacement[] : [])
      if (compRes.status === "fulfilled") setCompetencies(Array.isArray(compRes.value) ? compRes.value as CompetencyRecord[] : [])
    } catch {
      setError(tc("error.load"))
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) return <div role="main"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Student"
  const totalItems = portfolios.reduce((sum, p) => sum + (p.items?.length || 0), 0) + demonstrations.length + projects.length + fieldwork.length + competencies.length

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      {error && (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadEvidence() }} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      )}

      <LearnerHeader firstName={firstName} subtitle={t("evidence.subtitle")} />

      {totalItems === 0 ? (
        <EmptyState
          icon={<Award className="size-8" />}
          title={t("evidence.noEvidence")}
          description={t("evidence.noEvidenceDesc")}
        />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
            {[
              { label: t("evidence.portfolioItems"), count: totalItems, icon: FolderOpen, color: "bg-primary/10 text-primary" },
              { label: t("evidence.demonstrations"), count: demonstrations.length, icon: Award, color: "bg-rose-100 text-rose-600 dark:bg-rose-900/30 dark:text-rose-400" },
              { label: t("evidence.projects"), count: projects.length, icon: Target, color: "bg-amber-100 text-amber-600 dark:bg-amber-900/30 dark:text-amber-400" },
              { label: t("evidence.fieldwork"), count: fieldwork.length, icon: Bookmark, color: "bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400" },
              { label: t("evidence.competencies"), count: competencies.length, icon: BookOpen, color: "bg-violet-100 text-violet-600 dark:bg-violet-900/30 dark:text-violet-400" },
            ].map((stat) => (
              <div key={stat.label} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
                <div className="flex items-center gap-3">
                  <div className={`flex size-10 items-center justify-center rounded-full ${stat.color}`}>
                    <stat.icon className="size-5" />
                  </div>
                  <div>
                    <p className="text-xs font-medium text-muted-foreground">{stat.label}</p>
                    <p className="text-2xl font-extrabold text-foreground">{stat.count}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {portfolios.length > 0 && (
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-2 mb-3">
                <FolderOpen className="size-4 text-primary" />
                <h3 className="font-semibold text-foreground">{t("evidence.portfolio")}</h3>
              </div>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {portfolios.map((p) => (
                  <div key={p.id} className="rounded-xl border border-border bg-muted/30 p-4">
                    <p className="font-medium text-foreground">{p.title}</p>
                    <p className="text-xs text-muted-foreground">{p.items?.length || 0} items · {p.visibility}</p>
                  </div>
                ))}
              </div>
              <Link href="/dashboard/learner/portfolio" aria-label={t("evidence.viewPortfolio")} className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                {t("evidence.viewPortfolio")} <ChevronRight className="size-3" />
              </Link>
            </div>
          )}

          {demonstrations.length > 0 && (
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-2 mb-3">
                <Award className="size-4 text-rose-600" />
                <h3 className="font-semibold text-foreground">{t("evidence.showWhatICanDo")}</h3>
              </div>
              <div className="space-y-2">
                {demonstrations.slice(0, 5).map((d) => (
                  <div key={d.id} className="flex items-center gap-3 rounded-xl border border-border bg-muted/30 p-3">
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-foreground text-sm line-clamp-1">{d.title}</p>
                      <p className="text-xs text-muted-foreground">{d.status.replace(/_/g, " ")}</p>
                    </div>
                  </div>
                ))}
              </div>
              <Link href="/dashboard/learner/demonstrations" aria-label={t("evidence.viewAll")} className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                {tc("viewAll")} <ChevronRight className="size-3" />
              </Link>
            </div>
          )}

          {projects.length > 0 && (
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-2 mb-3">
                <Target className="size-4 text-amber-600" />
                <h3 className="font-semibold text-foreground">{t("evidence.projects")}</h3>
              </div>
              <div className="space-y-2">
                {projects.slice(0, 5).map((p) => (
                  <div key={p.id} className="flex items-center gap-3 rounded-xl border border-border bg-muted/30 p-3">
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-foreground text-sm line-clamp-1">{p.title}</p>
                      <p className="text-xs text-muted-foreground">{p.status?.replace(/_/g, " ")}</p>
                    </div>
                  </div>
                ))}
              </div>
              <Link href="/dashboard/learner/projects" aria-label={t("evidence.viewAllProjects")} className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                {tc("viewAll")} <ChevronRight className="size-3" />
              </Link>
            </div>
          )}

          {competencies.length > 0 && (
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-2 mb-3">
                <BookOpen className="size-4 text-violet-600" />
                <h3 className="font-semibold text-foreground">{t("evidence.competencies")}</h3>
              </div>
              <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                {competencies.slice(0, 6).map((c) => (
                  <div key={c.id} className="rounded-xl border border-border bg-muted/30 p-3">
                    <p className="font-medium text-foreground text-sm line-clamp-1">{c.competencyName || tc("competency")}</p>
                    <p className="text-xs text-muted-foreground">{c.status.replace(/_/g, " ")}</p>
                  </div>
                ))}
              </div>
              <Link href="/dashboard/learner/competencies" aria-label={t("evidence.viewAllCompetencies")} className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                {tc("viewAll")} <ChevronRight className="size-3" />
              </Link>
            </div>
          )}
        </>
      )}
    </div>
  )
}
