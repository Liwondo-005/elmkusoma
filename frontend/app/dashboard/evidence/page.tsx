"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { primaryApi, type StudentBadge, type PortfolioItem, type LearningEvidence } from "@/lib/api"
import { type LearningLevel, primarySubjects } from "@/lib/learner-config"
import { Award, Plus, BookOpen, Trophy, Star, Clock, CheckCircle, FileText, Palette, Wrench, Camera, Mic, GraduationCap, Send, Filter, ChevronDown, X, Loader2 } from "lucide-react"

interface EvidenceItem {
  id: string
  type: string
  title: string
  description: string
  subject: string
  points: number
  date: string
  importance: "low" | "medium" | "high"
}


const typeFilters = ["All", "LESSON_COMPLETE", "PROJECT", "QUIZ_SCORE", "PORTFOLIO", "TEACHER_NOTE", "ATTENDANCE"]

export default function EvidencePage() {
  const { user } = useRequireAuth()
  const t = useTranslations("primary")
  const evidenceTypeConfig: Record<string, { icon: typeof Award; color: string; bgColor: string; label: string }> = {
  LESSON_COMPLETE: { icon: CheckCircle, color: "text-green-600", bgColor: "bg-green-50", label: t("evidence.typeLesson") },
  PROJECT: { icon: Wrench, color: "text-blue-600", bgColor: "bg-blue-50", label: t("evidence.typeProject") },
  QUIZ_SCORE: { icon: Star, color: "text-amber-600", bgColor: "bg-amber-50", label: t("evidence.typeQuiz") },
  PORTFOLIO: { icon: Palette, color: "text-purple-600", bgColor: "bg-purple-50", label: t("evidence.typePortfolio") },
  TEACHER_NOTE: { icon: GraduationCap, color: "text-teal-600", bgColor: "bg-teal-50", label: t("evidence.typeTeacher") },
  ATTENDANCE: { icon: Clock, color: "text-orange-600", bgColor: "bg-orange-50", label: t("evidence.typeAttendance") },
}
  const ts = useTranslations("status")
  const tc = useTranslations("common")
  const [badges, setBadges] = useState<StudentBadge[]>([])
  const [portfolioItems, setPortfolioItems] = useState<PortfolioItem[]>([])
  const [evidenceRecords, setEvidenceRecords] = useState<LearningEvidence[]>([])
  const [loading, setLoading] = useState(true)
  const [typeFilter, setTypeFilter] = useState("All")
  const [subjectFilter, setSubjectFilter] = useState("All")
  const [showAddForm, setShowAddForm] = useState(false)
  const [newEvidence, setNewEvidence] = useState({ title: "", evidenceType: "LESSON_COMPLETE", description: "", subjectName: "" })
  const [submitting, setSubmitting] = useState(false)
  const level = user?.learningLevel as LearningLevel | null

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const [badgeData, portfolioData, evidenceData] = await Promise.all([
        primaryApi.getBadges().catch(() => []),
        primaryApi.getPortfolio().catch(() => []),
        primaryApi.getLearningEvidence().catch(() => []),
      ])
      setBadges(badgeData)
      setPortfolioItems(portfolioData)
      setEvidenceRecords(evidenceData)
    } catch {
      setBadges([])
      setPortfolioItems([])
      setEvidenceRecords([])
    } finally {
      setLoading(false)
    }
  }

  const evidenceItems: EvidenceItem[] = [
    ...evidenceRecords.map((record) => ({
      id: record.id,
      type: record.evidenceType || "LESSON_COMPLETE",
      title: record.title,
      description: record.description || "",
      subject: record.subjectName || t("evidence.generalSubject"),
      points: 20,
      date: record.createdAt || new Date().toISOString(),
      importance: "medium" as const,
    })),
    ...badges.map((badge) => ({
      id: badge.id,
      type: "LESSON_COMPLETE",
      title: badge.badgeName,
      description: badge.description,
      subject: badge.badgeType || t("evidence.generalSubject"),
      points: badge.points,
      date: badge.awardedAt,
      importance: badge.points >= 50 ? "high" as const : badge.points >= 20 ? "medium" as const : "low" as const,
    })),
    ...portfolioItems.map((item) => ({
      id: item.id,
      type: item.portfolioType === "PROJECT" ? "PROJECT" : "PORTFOLIO",
      title: item.title,
      description: item.description || "",
      subject: item.subjectName || t("evidence.generalSubject"),
      points: item.portfolioType === "PROJECT" ? 30 : 15,
      date: item.createdAt,
      importance: item.isFeatured ? "high" as const : "medium" as const,
    })),
  ].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())

  const filteredEvidence = evidenceItems.filter((item) => {
    if (typeFilter !== "All" && item.type !== typeFilter) return false
    if (subjectFilter !== "All" && item.subject !== subjectFilter) return false
    return true
  })

  const totalPoints = evidenceItems.reduce((sum, item) => sum + item.points, 0)

  const uniqueSubjects = ["All", ...new Set(evidenceItems.map((item) => item.subject).filter(Boolean))]

  async function handleAddEvidence() {
    if (!newEvidence.title.trim()) return
    setSubmitting(true)
    try {
      await primaryApi.addLearningEvidence({
        title: newEvidence.title,
        evidenceType: newEvidence.evidenceType,
        description: newEvidence.description,
        subjectName: newEvidence.subjectName || undefined,
      })
      setShowAddForm(false)
      setNewEvidence({ title: "", evidenceType: "LESSON_COMPLETE", description: "", subjectName: "" })
      loadData()
    } catch {
      // Failed to add evidence
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-primary/10">
            <Award className="size-6 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("evidence.title")}</h1>
            <p className="text-sm text-muted-foreground">{t("evidence.subtitle")}</p>
          </div>
        </div>
        <button
          type="button"
          onClick={() => setShowAddForm(!showAddForm)}
          className={`inline-flex items-center gap-2 rounded-lg border px-4 py-2.5 text-sm font-medium transition-colors ${
            showAddForm ? "border-primary bg-primary/5 text-primary" : "border-border bg-card text-muted-foreground hover:bg-muted"
          }`}
        >
          <Filter className="size-4" />
          {t("evidence.filters")}
        </button>
        <button
          type="button"
          onClick={() => setShowAddForm(!showAddForm)}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
        >
          <Plus className="size-4" />
          {t("evidence.addButton")}
        </button>
      </div>

      {showAddForm && (
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-semibold text-foreground">{t("evidence.filterTitle")}</h3>
            <button type="button" onClick={() => { setTypeFilter("All"); setSubjectFilter("All") }} className="text-xs text-muted-foreground hover:text-foreground">
              {t("evidence.reset")}
            </button>
          </div>
          <div className="space-y-3">
            <div>
              <label className="mb-1.5 block text-xs font-medium text-muted-foreground">{t("evidence.typeLabel")}</label>
              <div className="flex flex-wrap gap-1.5">
                {typeFilters.map((f) => (
                  <button
                    key={f}
                    type="button"
                    onClick={() => setTypeFilter(f)}
                    className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                      typeFilter === f
                        ? "bg-primary text-primary-foreground"
                        : "bg-muted text-muted-foreground hover:bg-muted/80"
                    }`}
                  >
                    {f === "All" ? t("evidence.filterAll") : evidenceTypeConfig[f]?.label || f}
                  </button>
                ))}
              </div>
            </div>
            <div>
              <label className="mb-1.5 block text-xs font-medium text-muted-foreground">{t("evidence.subjectLabel")}</label>
              <div className="flex flex-wrap gap-1.5">
                {uniqueSubjects.map((s) => (
                  <button
                    key={s === "All" ? t("evidence.filterAll") : s}
                    type="button"
                    onClick={() => setSubjectFilter(s)}
                    className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                      subjectFilter === s
                        ? "bg-primary text-primary-foreground"
                        : "bg-muted text-muted-foreground hover:bg-muted/80"
                    }`}
                  >
                    {s === "All" ? t("evidence.filterAll") : s}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {showAddForm && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-foreground">{t("evidence.formTitle")}</h3>
            <button type="button" onClick={() => setShowAddForm(false)} className="text-muted-foreground hover:text-foreground">
              <X className="size-5" />
            </button>
          </div>
          <div className="space-y-4">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-foreground">{t("evidence.titleLabel")}</label>
              <input
                type="text"
                value={newEvidence.title}
                onChange={(e) => setNewEvidence((prev) => ({ ...prev, title: e.target.value }))}
                placeholder={t("evidence.titlePlaceholder")}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring"
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="mb-1.5 block text-sm font-medium text-foreground">{t("evidence.evidenceTypeLabel")}</label>
                <select
                  value={newEvidence.evidenceType}
                  onChange={(e) => setNewEvidence((prev) => ({ ...prev, evidenceType: e.target.value }))}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring"
                >
                  <option value="LESSON_COMPLETE">{t("evidence.typeLesson")}</option>
                  <option value="PROJECT">{t("evidence.typeProject")}</option>
                  <option value="QUIZ_SCORE">{t("evidence.typeQuiz")}</option>
                  <option value="PORTFOLIO">{t("evidence.typePortfolio")}</option>
                  <option value="TEACHER_NOTE">{t("evidence.typeTeacher")}</option>
                  <option value="ATTENDANCE">{t("evidence.typeAttendance")}</option>
                </select>
              </div>
              <div>
                <label className="mb-1.5 block text-sm font-medium text-foreground">{t("evidence.subjectLabel")}</label>
                <select
                  value={newEvidence.subjectName}
                  onChange={(e) => setNewEvidence((prev) => ({ ...prev, subjectName: e.target.value }))}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring"
                >
                  <option value="">{t("evidence.generalSubject")}</option>
                  {primarySubjects.map((s) => (
                    <option key={s.name} value={s.name}>{s.name}</option>
                  ))}
                </select>
              </div>
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-foreground">{t("evidence.descLabel")}</label>
              <textarea
                value={newEvidence.description}
                onChange={(e) => setNewEvidence((prev) => ({ ...prev, description: e.target.value }))}
                placeholder={t("evidence.descPlaceholder")}
                rows={3}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
              />
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setShowAddForm(false)}
                className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-muted-foreground hover:bg-muted transition-colors"
              >
                {tc("cancel")}
              </button>
              <button
                type="button"
                onClick={handleAddEvidence}
                disabled={!newEvidence.title.trim() || submitting}
                className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
              >
                {submitting ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />}
                {t("evidence.submit")}
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-gradient-to-br from-primary/5 via-card to-teal/5 p-6 shadow-xs">
        <div className="flex items-center gap-4">
          <div className="flex size-14 items-center justify-center rounded-2xl bg-primary/10">
            <Trophy className="size-7 text-primary" />
          </div>
          <div>
            <p className="text-3xl font-extrabold text-foreground">{totalPoints}</p>
            <p className="text-sm text-muted-foreground">{t("evidence.totalPoints")}</p>
          </div>
        </div>
        <div className="mt-4 grid grid-cols-3 gap-4">
          <div className="text-center">
            <p className="text-xl font-bold text-foreground">{evidenceItems.length}</p>
            <p className="text-xs text-muted-foreground">{t("evidence.totalItems")}</p>
          </div>
          <div className="text-center">
            <p className="text-xl font-bold text-foreground">{badges.length}</p>
            <p className="text-xs text-muted-foreground">{t("evidence.badges")}</p>
          </div>
          <div className="text-center">
            <p className="text-xl font-bold text-foreground">{portfolioItems.length}</p>
            <p className="text-xs text-muted-foreground">{t("evidence.creations")}</p>
          </div>
        </div>
      </div>

      {filteredEvidence.length === 0 ? (
        <div className="flex flex-col items-center rounded-2xl border border-dashed border-border py-16 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10">
            <Award className="size-8 text-primary" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">
            {evidenceItems.length === 0 ? t("evidence.emptyStart") : t("evidence.emptyFiltered")}
          </h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            {evidenceItems.length === 0
              ? t("evidence.emptyStartDesc")
              : t("evidence.emptyFilteredDesc")}
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {filteredEvidence.map((item) => {
            const config = evidenceTypeConfig[item.type] || evidenceTypeConfig.LESSON_COMPLETE
            const Icon = config.icon
            return (
              <div
                key={item.id}
                className={`group rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:shadow-md ${
                  item.importance === "high" ? "border-l-4 border-l-primary" : ""
                }`}
              >
                <div className="flex items-start gap-4">
                  <div className={`flex size-12 shrink-0 items-center justify-center rounded-xl ${config.bgColor}`}>
                    <Icon className={`size-6 ${config.color}`} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <h3 className="text-base font-semibold text-foreground">{item.title}</h3>
                        {item.description && (
                          <p className="mt-1 text-sm text-muted-foreground line-clamp-2">{item.description}</p>
                        )}
                      </div>
                      {item.importance === "high" && (
                        <span className="shrink-0 rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold text-primary">
                          {t("evidence.notable")}
                        </span>
                      )}
                    </div>
                    <div className="mt-3 flex flex-wrap items-center gap-2">
                      <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-[11px] font-semibold ${config.bgColor} ${config.color}`}>
                        {config.label}
                      </span>
                      {item.subject && item.subject !== "General" && (
                        <span className="inline-flex items-center rounded-full bg-muted px-2.5 py-0.5 text-[11px] font-medium text-muted-foreground">
                          {item.subject}
                        </span>
                      )}
                      <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2.5 py-0.5 text-[11px] font-semibold text-amber-600">
                        <Star className="size-3" />
                        {t("quests.ptsCount", { count: item.points })}
                      </span>
                      <span className="text-[11px] text-muted-foreground">
                        {new Date(item.date).toLocaleDateString()}
                      </span>
                    </div>
                  </div>
                  <button
                    type="button"
                    className="shrink-0 rounded-lg border border-border p-2 text-muted-foreground opacity-0 transition-all group-hover:opacity-100 hover:bg-primary/5 hover:text-primary"
                    title={t("evidence.shareTitle")}
                  >
                    <Send className="size-4" />
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
