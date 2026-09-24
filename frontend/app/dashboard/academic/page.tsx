"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { academicApi, type AcademicYear, type Term, type Grade, type Subject, type ClassGroup } from "@/lib/api"

type Tab = "years" | "terms" | "grades" | "subjects" | "classes"

export default function AcademicPage() {
  const { user } = useAuth()
  const t = useTranslations("primary")
  const tabs: { key: Tab; label: string; count: number }[] = [
    { key: "years", label: t("academicTab.years"), count: years.length },
    { key: "terms", label: t("academicTab.terms"), count: terms.length },
    { key: "grades", label: t("academicTab.grades"), count: grades.length },
    { key: "subjects", label: t("academicTab.subjects"), count: subjects.length },
    { key: "classes", label: t("academicTab.classes"), count: classes.length },
  ]
  const ts = useTranslations("status")
  const tc = useTranslations("common")
  const [tab, setTab] = useState<Tab>("years")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  const [years, setYears] = useState<AcademicYear[]>([])
  const [terms, setTerms] = useState<Term[]>([])
  const [grades, setGrades] = useState<Grade[]>([])
  const [subjects, setSubjects] = useState<Subject[]>([])
  const [classes, setClasses] = useState<ClassGroup[]>([])

  const institutionId = user?.institutionId || ""

  useEffect(() => {
    if (!institutionId) return
    setLoading(true)
    setError("")

    const fetchers: Record<Tab, () => Promise<void>> = {
      years: async () => { setYears(await academicApi.getAcademicYears(institutionId)) },
      terms: async () => {
        if (years.length === 0) setYears(await academicApi.getAcademicYears(institutionId))
        const allTerms: Term[] = []
        for (const y of years.length > 0 ? years : await academicApi.getAcademicYears(institutionId)) {
          try { allTerms.push(...await academicApi.getTerms(y.id)) } catch {}
        }
        setTerms(allTerms)
      },
      grades: async () => { setGrades(await academicApi.getGrades(institutionId)) },
      subjects: async () => { setSubjects(await academicApi.getSubjects(institutionId)) },
      classes: async () => { setClasses(await academicApi.getClassGroups(institutionId)) },
    }

    fetchers[tab]()
      .catch((e) => setError(e.message || tc("error.load")))
      .finally(() => setLoading(false))
  }, [tab, institutionId])


  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">{t("academicTab.title")}</h1>
        <p className="text-muted-foreground">{t("academicTab.subtitle")}</p>
      </div>

      <div className="flex gap-2 overflow-x-auto border-b border-border pb-px">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`whitespace-nowrap px-4 py-2.5 text-sm font-medium transition-colors border-b-2 ${
              tab === t.key
                ? "border-primary text-primary"
                : "border-transparent text-muted-foreground hover:text-foreground"
            }`}
          >
            {t.label}
            <span className="ml-1.5 inline-flex size-5 items-center justify-center rounded-full bg-muted text-xs">
              {t.count}
            </span>
          </button>
        ))}
      </div>

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : (
        <div className="rounded-lg border border-border bg-card">
          {tab === "years" && (
            <div className="divide-y divide-border">
              {years.length === 0 ? (
                <p className="p-6 text-center text-muted-foreground">{t("academicTab.emptyYears")}</p>
              ) : (
                years.map((y) => (
                  <div key={y.id} className="flex items-center justify-between p-4">
                    <div>
                      <p className="font-medium">{y.yearLabel}</p>
                      <p className="text-sm text-muted-foreground">{y.educationLevel} &middot; {y.startDate} to {y.endDate}</p>
                    </div>
                    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                      y.isCurrent ? "bg-green-100 text-green-800" : "bg-muted text-muted-foreground"
                    }`}>
                      {y.isCurrent ? t("academicTab.current") : ts("inactive")}
                    </span>
                  </div>
                ))
              )}
            </div>
          )}

          {tab === "terms" && (
            <div className="divide-y divide-border">
              {terms.length === 0 ? (
                <p className="p-6 text-center text-muted-foreground">{t("academicTab.emptyTerms")}</p>
              ) : (
                terms.map((t) => (
                  <div key={t.id} className="flex items-center justify-between p-4">
                    <div>
                      <p className="font-medium">{t.name}</p>
                      <p className="text-sm text-muted-foreground">{t("academicTab.termLine", { n: t.termNumber, start: t.startDate, end: t.endDate })}</p>
                    </div>
                    <span className="text-xs text-muted-foreground">{t.isActive ? ts("active") : ts("inactive")}</span>
                  </div>
                ))
              )}
            </div>
          )}

          {tab === "grades" && (
            <div className="divide-y divide-border">
              {grades.length === 0 ? (
                <p className="p-6 text-center text-muted-foreground">{t("academicTab.emptyGrades")}</p>
              ) : (
                grades.map((g) => (
                  <div key={g.id} className="flex items-center justify-between p-4">
                    <div>
                      <p className="font-medium">{g.name}</p>
                      <p className="text-sm text-muted-foreground">{g.educationLevel} {g.code ? `(${g.code})` : ""}</p>
                    </div>
                    <span className="text-xs text-muted-foreground">{t("academicTab.orderLine", { n: g.sortOrder })}</span>
                  </div>
                ))
              )}
            </div>
          )}

          {tab === "subjects" && (
            <div className="divide-y divide-border">
              {subjects.length === 0 ? (
                <p className="p-6 text-center text-muted-foreground">{t("academicTab.emptySubjects")}</p>
              ) : (
                subjects.map((s) => (
                  <div key={s.id} className="flex items-center justify-between p-4">
                    <div>
                      <p className="font-medium">{s.name}</p>
                      <p className="text-sm text-muted-foreground">{s.educationLevel} {s.code ? `(${s.code})` : ""} {s.category ? `- ${s.category}` : ""}</p>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {tab === "classes" && (
            <div className="divide-y divide-border">
              {classes.length === 0 ? (
                <p className="p-6 text-center text-muted-foreground">{t("academicTab.emptyClasses")}</p>
              ) : (
                classes.map((c) => (
                  <div key={c.id} className="flex items-center justify-between p-4">
                    <div>
                      <p className="font-medium">{c.name}</p>
                      <p className="text-sm text-muted-foreground">
                        {c.section ? t("academicTab.sectionLine", { s: c.section }) : ""} {c.capacity ? t("academicTab.capacityLine", { n: c.capacity }) : ""}
                      </p>
                    </div>
                    <span className="text-xs text-muted-foreground">{c.isActive ? ts("active") : ts("inactive")}</span>
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
