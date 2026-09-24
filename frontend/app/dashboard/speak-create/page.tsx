"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { primaryApi, type SpeakingActivity, type PortfolioItem } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { Mic, BookOpen, Music, Palette, PenTool, Plus, X, CheckCircle, Clock, Folder } from "lucide-react"


const typeBadgeMap: Record<string, string> = {
  RECORD_STORY: "bg-red-100 text-red-700",
  PRACTICE_READING: "bg-blue-100 text-blue-700",
  SING_SONG: "bg-purple-100 text-purple-700",
  CREATE_ART: "bg-pink-100 text-pink-700",
  WRITE_POEM: "bg-amber-100 text-amber-700",
}

function formatDuration(seconds: number, t: (k: string, p?: Record<string, number>) => string): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  if (m === 0) return t("speak.secs", { n: s })
  return t("speak.minsSecs", { m, s })

export default function SpeakCreatePage() {
  const { user } = useRequireAuth()
  const t = useTranslations("primary")
  const activityTypes = [
  { value: "RECORD_STORY", label: t("speak.typeRecord"), icon: Mic, color: "bg-red-50 text-red-600", border: "border-red-200" },
  { value: "PRACTICE_READING", label: t("speak.typeReading"), icon: BookOpen, color: "bg-blue-50 text-blue-600", border: "border-blue-200" },
  { value: "SING_SONG", label: t("speak.typeSing"), icon: Music, color: "bg-purple-50 text-purple-600", border: "border-purple-200" },
  { value: "CREATE_ART", label: t("speak.typeArt"), icon: Palette, color: "bg-pink-50 text-pink-600", border: "border-pink-200" },
  { value: "WRITE_POEM", label: t("speak.typePoem"), icon: PenTool, color: "bg-amber-50 text-amber-600", border: "border-amber-200" },
]
  const ts = useTranslations("status")
  const [activities, setActivities] = useState<SpeakingActivity[]>([])
  const [portfolio, setPortfolio] = useState<PortfolioItem[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ activityType: "RECORD_STORY", title: "", description: "", subjectName: "" })
  const [submitting, setSubmitting] = useState(false)
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const [actData, portData] = await Promise.all([
        primaryApi.getSpeakingActivities().catch(() => []),
        primaryApi.getPortfolio().catch(() => []),
      ])
      setActivities(actData)
      setPortfolio(portData)
    } catch {
      setActivities([])
      setPortfolio([])
    } finally {
      setLoading(false)
    }
  }

  async function handleAdd(e: React.FormEvent) {
    e.preventDefault()
    if (!form.title.trim()) return
    try {
      setSubmitting(true)
      const act = await primaryApi.addSpeakingActivity({
        activityType: form.activityType,
        title: form.title,
        description: form.description || undefined,
        subjectName: form.subjectName || undefined,
      })
      setActivities((prev) => [act, ...prev])
      setForm({ activityType: "RECORD_STORY", title: "", description: "", subjectName: "" })
      setShowForm(false)
    } catch {
      // handle silently
    } finally {
      setSubmitting(false)
    }
  }

  async function handleComplete(id: string) {
    try {
      const updated = await primaryApi.completeSpeakingActivity(id)
      setActivities((prev) => prev.map((a) => (a.id === id ? updated : a)))
    } catch {
      // handle silently
    }
  }

  const completedCount = activities.filter((a) => a.isCompleted).length
  const totalTime = activities.reduce((sum, a) => sum + a.durationSeconds, 0)

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  if (!isPrimary) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-2xl bg-red-500/10">
            <Mic className="size-5 text-red-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("speak.title")}</h1>
            <p className="text-sm text-muted-foreground">{t("speak.subtitle")}
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <Mic className="size-12 text-muted-foreground/30" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("speak.primaryOnlyTitle")}</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            {t("speak.primaryOnlyDesc")}
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-red-500/5 via-card to-pink-500/5 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-red-500/10">
            <Mic className="size-6 text-red-500" />
          </div>
          <div className="flex-1">
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("speak.title")}</h1>
            <p className="text-sm text-muted-foreground">{t("speak.subtitle")}
          </div>
          <button
            onClick={() => setShowForm(true)}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            <Plus className="size-4" /> {t("speak.newActivity")}
          </button>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-red-50">
              <Mic className="size-5 text-red-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{activities.length}</p>
              <p className="text-xs text-muted-foreground">{t("speak.totalActivities")}
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-green-50">
              <CheckCircle className="size-5 text-green-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{completedCount}</p>
              <p className="text-xs text-muted-foreground">{ts("completed")}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-amber-50">
              <Clock className="size-5 text-amber-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{formatDuration(totalTime, t)}</p>
              <p className="text-xs text-muted-foreground">{t("speak.totalTime")}
            </div>
          </div>
        </div>
      </div>

      {showForm && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-foreground">{t("speak.formTitle")}
            <button onClick={() => setShowForm(false)} className="text-muted-foreground hover:text-foreground">
              <X className="size-5" />
            </button>
          </div>
          <form onSubmit={handleAdd} className="space-y-4">
            <div>
              <label className="text-sm font-medium text-foreground">{t("speak.typeLabel")}</label>
              <div className="mt-2 grid grid-cols-2 sm:grid-cols-3 gap-2">
                {activityTypes.map((type) => {
                  const Icon = type.icon
                  return (
                    <button
                      key={type.value}
                      type="button"
                      onClick={() => setForm((prev) => ({ ...prev, activityType: type.value }))}
                      className={`flex items-center gap-2 rounded-lg border p-3 text-left text-sm transition-all ${
                        form.activityType === type.value
                          ? `${type.border} ${type.color} ring-2 ring-primary/20`
                          : "border-border bg-background hover:border-primary/30"
                      }`}
                    >
                      <Icon className="size-4" />
                      <span className="font-medium">{type.label}</span>
                    </button>
                  )
                })}
              </div>
            </div>
            <div>
              <label className="text-sm font-medium text-foreground">{t("speak.titleLabel")}</label>
              <input
                type="text"
                value={form.title}
                onChange={(e) => setForm((prev) => ({ ...prev, title: e.target.value }))}
                placeholder={t("speak.titlePlaceholder")}
                className="mt-2 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring"
                required
              />
            </div>
            <div>
              <label className="text-sm font-medium text-foreground">{t("speak.descLabel")}</label>
              <textarea
                value={form.description}
                onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
                placeholder={t("speak.descPlaceholder")}
                rows={3}
                className="mt-2 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
              />
            </div>
            <div>
              <label className="text-sm font-medium text-foreground">{t("speak.subjectLabel")}</label>
              <input
                type="text"
                value={form.subjectName}
                onChange={(e) => setForm((prev) => ({ ...prev, subjectName: e.target.value }))}
                placeholder={t("speak.subjectPlaceholder")}
                className="mt-2 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring"
              />
            </div>
            <button
              type="submit"
              disabled={submitting || !form.title.trim()}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
            >
              {submitting ? (
                <div className="size-4 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent" />
              ) : (
                {t("speak.save")}
              )}
            </button>
          </form>
        </div>
      )}

      {activities.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10">
            <Mic className="size-8 text-primary" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("speak.emptyTitle")}</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            {t("speak.emptyDesc")}
          </p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {activities.map((act) => {
            const typeInfo = activityTypes.find((t) => t.value === act.activityType)
            const Icon = typeInfo?.icon || Mic
            return (
              <div
                key={act.id}
                className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:shadow-md hover:border-primary/30"
              >
                <div className="flex items-start gap-3">
                  <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${typeInfo?.color || "bg-muted"}`}>
                    <Icon className="size-5" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="text-sm font-bold text-foreground line-clamp-2">{act.title}</h3>
                    <span className={`mt-1 inline-block rounded-full px-2 py-0.5 text-[10px] font-medium ${typeBadgeMap[act.activityType] || "bg-muted text-muted-foreground"}`}>
                      {act.activityType.replace("_", " ")}
                    </span>
                  </div>
                </div>
                {act.description && (
                  <p className="mt-2 text-xs text-muted-foreground line-clamp-2">{act.description}</p>
                )}
                <div className="mt-3 flex items-center justify-between">
                  <span className="flex items-center gap-1 text-[10px] text-muted-foreground">
                    <Clock className="size-3" /> {formatDuration(act.durationSeconds, t)}
                  </span>
                  {act.isCompleted ? (
                    <span className="inline-flex items-center gap-1 rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-medium text-green-700">
                      <CheckCircle className="size-3" /> {t("labs.doneBadge")}
                    </span>
                  ) : (
                    <button
                      onClick={() => handleComplete(act.id)}
                      className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-medium text-primary hover:bg-primary/20 transition-colors"
                    >
                      {t("speak.markDone")}
                    </button>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-center gap-2 mb-4">
          <Folder className="size-5 text-primary" />
          <h2 className="text-lg font-semibold text-foreground">{t("speak.creationsTitle")}
        </div>
        {portfolio.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/20 py-12 text-center">
            <Palette className="size-10 text-muted-foreground/30" />
            <h3 className="mt-3 text-sm font-semibold text-foreground">{t("speak.portfolioEmpty")}
            <p className="mt-1 text-xs text-muted-foreground">
              {t("speak.portfolioEmptyDesc")}
            </p>
          </div>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {portfolio.slice(0, 6).map((item) => (
              <div key={item.id} className="rounded-xl border border-border p-4 transition-all hover:shadow-sm">
                <h4 className="text-sm font-bold text-foreground line-clamp-1">{item.title}</h4>
                {item.description && (
                  <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{item.description}</p>
                )}
                <div className="mt-2 flex items-center gap-2">
                  <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                    {item.portfolioType.replace("_", " ")}
                  </span>
                  {item.subjectName && (
                    <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-medium text-primary">
                      {item.subjectName}
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
