"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { CareerProfile } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { Briefcase, TrendingUp, Award, GraduationCap, Building2, Wrench, Star, Save, AlertCircle } from "lucide-react"

export default function CareerPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [profile, setProfile] = useState<CareerProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [objective, setObjective] = useState("")
  const [industry, setIndustry] = useState("")
  const [role, setRole] = useState("")
  const [skills, setSkills] = useState("")
  const [certs, setCerts] = useState("")
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadProfile()
  }, [user])

  async function loadProfile() {
    try {
      setLoading(true)
      const studentId = user?.id || ""
      const res = await collegeApi.getCareerProfile(studentId)
      const p = res.data || null
      setProfile(p)
      if (p) {
        setObjective(p.careerObjective || "")
        setIndustry(p.targetIndustry || "")
        setRole(p.targetRole || "")
        setSkills(p.skills || "")
        setCerts(p.certifications || "")
      }
    } catch {
      setError(tc("error.load"))
    } finally {
      setLoading(false)
    }
  }

  async function handleSave() {
    try {
      setSaving(true)
      const studentId = user?.id || ""
      const res = await collegeApi.upsertCareerProfile(studentId, {
        careerObjective: objective,
        targetIndustry: industry,
        targetRole: role,
        skills,
        certifications: certs,
      })
      setProfile(res.data || null)
    } catch {
      setError(tc("error.save"))
    } finally {
      setSaving(false)
    }
  }

  if (authLoading || loading) return <div role="main"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Student"

  const industries = [
    { name: "Technology & IT", description: "Software engineering, data science, cybersecurity, and emerging technologies." },
    { name: "Healthcare", description: "Clinical practice, public health, biomedical research, and health administration." },
    { name: "Engineering", description: "Civil, mechanical, electrical, and industrial engineering roles." },
    { name: "Business & Finance", description: "Management, accounting, banking, investment, and entrepreneurship." },
    { name: "Education", description: "Teaching, curriculum development, educational leadership, and training." },
    { name: "Agriculture", description: "Agricultural science, food security, agribusiness, and sustainable farming." },
  ]

  const professionalSkills = [
    { name: "Leadership & Management", icon: TrendingUp },
    { name: "Communication & Presentation", icon: Star },
    { name: "Critical Thinking & Problem Solving", icon: Wrench },
    { name: "Data Analysis & Research", icon: Award },
    { name: "Project Management", icon: Briefcase },
    { name: "Digital Literacy", icon: GraduationCap },
  ]

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      {error && (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadProfile() }} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      )}

      <LearnerHeader firstName={firstName} subtitle={t("career.subtitle")} />

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-center gap-3 mb-4">
          <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
            <Briefcase className="size-5 text-primary" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-foreground">{t("career.myProfile")}</h2>
            <p className="text-sm text-muted-foreground">{t("career.myProfileDesc")}</p>
          </div>
        </div>
        <div className="space-y-4">
          <div>
            <label className="text-sm font-medium text-foreground">{t("career.objective")}</label>
            <textarea
              value={objective}
              onChange={(e) => setObjective(e.target.value)}
              placeholder={t("career.objectivePlaceholder")}
              aria-label={t("career.objective")}
              className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-primary/20"
              rows={3}
            />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="text-sm font-medium text-foreground">{t("career.targetIndustry")}</label>
              <input
                value={industry}
                onChange={(e) => setIndustry(e.target.value)}
                placeholder={t("career.targetIndustryPlaceholder")}
                aria-label={t("career.targetIndustry")}
                className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>
            <div>
              <label className="text-sm font-medium text-foreground">{t("career.targetRole")}</label>
              <input
                value={role}
                onChange={(e) => setRole(e.target.value)}
                placeholder={t("career.targetRolePlaceholder")}
                aria-label={t("career.targetRole")}
                className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>
          </div>
          <div>
            <label className="text-sm font-medium text-foreground">{t("career.skills")}</label>
            <input
              value={skills}
              onChange={(e) => setSkills(e.target.value)}
              placeholder={t("career.skillsPlaceholder")}
              aria-label={t("career.skills")}
              className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <div>
            <label className="text-sm font-medium text-foreground">{t("career.certifications")}</label>
            <input
              value={certs}
              onChange={(e) => setCerts(e.target.value)}
              placeholder={t("career.certificationsPlaceholder")}
              aria-label={t("career.certifications")}
              className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <button
            onClick={handleSave}
            disabled={saving}
            aria-label={tc("save")}
            className="inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition disabled:opacity-50"
          >
            <Save className="size-4" />
            {saving ? tc("saving") : tc("save")}
          </button>
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-center gap-3 mb-4">
          <div className="flex size-10 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30">
            <Building2 className="size-5 text-blue-600 dark:text-blue-400" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-foreground">{t("career.industries")}</h2>
            <p className="text-sm text-muted-foreground">{t("career.industriesDesc")}</p>
          </div>
        </div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {industries.map((ind) => (
            <div key={ind.name} className="rounded-xl border border-border bg-muted/30 p-4">
              <h3 className="font-semibold text-foreground text-sm">{ind.name}</h3>
              <p className="mt-1 text-xs text-muted-foreground">{ind.description}</p>
            </div>
          ))}
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-center gap-3 mb-4">
          <div className="flex size-10 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30">
            <Wrench className="size-5 text-emerald-600 dark:text-emerald-400" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-foreground">{t("career.professionalSkills")}</h2>
            <p className="text-sm text-muted-foreground">{t("career.professionalSkillsDesc")}</p>
          </div>
        </div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {professionalSkills.map((skill) => (
            <div key={skill.name} className="flex items-center gap-3 rounded-xl border border-border bg-muted/30 p-4">
              <skill.icon className="size-5 text-primary shrink-0" />
              <span className="text-sm font-medium text-foreground">{skill.name}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
