"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { primaryApi, type ELmkusomaLab } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { FlaskConical, Leaf, Droplets, Zap, Bug, ArrowLeft, Beaker, CheckCircle, Star } from "lucide-react"


const typeColorMap: Record<string, string> = {
  NATURE: "bg-green-100 text-green-700",
  MATERIALS: "bg-blue-100 text-blue-700",
  FORCES: "bg-orange-100 text-orange-700",
  LIVING_THINGS: "bg-teal-100 text-teal-700",
}

const typeIconMap: Record<string, typeof FlaskConical> = {
  NATURE: Leaf,
  MATERIALS: Droplets,
  FORCES: Zap,
  LIVING_THINGS: Bug,
}

export default function LabsPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("primary")
  const ts = useTranslations("status")
  const labTypes = [
  { value: "All", name: t("labs.filterAll"), icon: FlaskConical, color: "text-gray-600", bg: "bg-gray-50", border: "border-gray-200" },
  { value: "NATURE", name: t("labs.typeNature"), icon: Leaf, color: "text-green-600", bg: "bg-green-50", border: "border-green-200" },
  { value: "MATERIALS", name: t("labs.typeMaterials"), icon: Droplets, color: "text-blue-600", bg: "bg-blue-50", border: "border-blue-200" },
  { value: "FORCES", name: t("labs.typeForces"), icon: Zap, color: "text-orange-600", bg: "bg-orange-50", border: "border-orange-200" },
  { value: "LIVING_THINGS", name: t("labs.typeLiving"), icon: Bug, color: "text-teal-600", bg: "bg-teal-50", border: "border-teal-200" },
]
  const [labs, setLabs] = useState<ELmkusomaLab[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedType, setSelectedType] = useState<string>("All")
  const [selectedLab, setSelectedLab] = useState<ELmkusomaLab | null>(null)
  const [notes, setNotes] = useState("")
  const [submitting, setSubmitting] = useState(false)
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  useEffect(() => {
    if (!user) return
    loadLabs()
  }, [user])

  async function loadLabs() {
    try {
      setLoading(true)
      const data = await primaryApi.getLabs().catch(() => [])
      setLabs(data)
    } catch {
      setLabs([])
    } finally {
      setLoading(false)
    }
  }

  async function handleAttemptLab(labId: string) {
    try {
      setSubmitting(true)
      const score = Math.floor(Math.random() * 30) + 70
      const updated = await primaryApi.attemptLab(labId, { studentNotes: notes, score })
      setLabs((prev) => prev.map((l) => (l.id === labId ? updated : l)))
      setSelectedLab(updated)
    } catch {
      // handle silently
    } finally {
      setSubmitting(false)
    }
  }

  const filtered = selectedType === "All" ? labs : labs.filter((l) => l.labType === selectedType)
  const completedCount = labs.filter((l) => l.isAttempted).length
  const avgScore = labs.length > 0 && completedCount > 0
    ? Math.round(labs.filter((l) => l.isAttempted).reduce((sum, l) => sum + l.score, 0) / completedCount)
    : 0

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
          <div className="flex size-10 items-center justify-center rounded-2xl bg-teal-500/10">
            <FlaskConical className="size-5 text-teal-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("labs.title")}</h1>
            <p className="text-sm text-muted-foreground">{t("labs.subtitle")}</p>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <FlaskConical className="size-12 text-muted-foreground/30" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("labs.primaryOnlyTitle")}</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            {t("labs.primaryOnlyDesc")}
          </p>
        </div>
      </div>
    )
  }

  if (selectedLab) {
    const TypeIcon = typeIconMap[selectedLab.labType] || FlaskConical
    return (
      <div className="mx-auto max-w-4xl space-y-6">
        <button
          onClick={() => { setSelectedLab(null); setNotes("") }}
          className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
        >
          <ArrowLeft className="size-4" />
          {t("labs.backToLabs")}
        </button>

        <div className="rounded-2xl border border-border bg-card p-8 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-teal-500/10">
              <TypeIcon className="size-6 text-teal-500" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-foreground">{selectedLab.labTitle}</h1>
              <span className={`mt-1 inline-block rounded-full px-2 py-0.5 text-xs font-medium ${typeColorMap[selectedLab.labType] || "bg-muted text-muted-foreground"}`}>
                {selectedLab.labType.replace("_", " ")}
              </span>
            </div>
          </div>

          <div className="mt-6 space-y-6">
            <div className="rounded-xl bg-amber-50 border border-amber-200 p-4">
              <h3 className="text-sm font-bold text-amber-800">{t("labs.hypothesis")}</h3>
              <p className="mt-1 text-sm text-amber-700">{selectedLab.hypothesis}</p>
            </div>

            <div>
              <h3 className="text-sm font-bold text-foreground flex items-center gap-2">
                <Beaker className="size-4" /> {t("labs.materials")}
              </h3>
              <ul className="mt-2 space-y-1">
                {(selectedLab.materialsList || []).map((m, i) => (
                  <li key={i} className="flex items-center gap-2 text-sm text-muted-foreground">
                    <span className="size-1.5 rounded-full bg-primary" />
                    {m}
                  </li>
                ))}
              </ul>
            </div>

            <div>
              <h3 className="text-sm font-bold text-foreground">{t("labs.steps")}</h3>
              <ol className="mt-2 space-y-2">
                {(selectedLab.steps || []).map((s, i) => (
                  <li key={i} className="flex gap-3 text-sm text-muted-foreground">
                    <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">
                      {i + 1}
                    </span>
                    <span className="pt-0.5">{s}</span>
                  </li>
                ))}
              </ol>
            </div>

            <div className="rounded-xl bg-green-50 border border-green-200 p-4">
              <h3 className="text-sm font-bold text-green-800">{t("labs.expected")}</h3>
              <p className="mt-1 text-sm text-green-700">{selectedLab.expectedResult}</p>
            </div>

            <div>
              <label className="text-sm font-bold text-foreground">{t("labs.learnedLabel")}</label>
              <textarea
                value={selectedLab.isAttempted ? selectedLab.studentNotes : notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder={t("labs.learnedPlaceholder")}
                rows={4}
                disabled={selectedLab.isAttempted}
                className="mt-2 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none disabled:opacity-60"
              />
            </div>

            <div className="flex items-center gap-4">
              {selectedLab.isAttempted ? (
                <div className="flex items-center gap-3">
                  <span className="inline-flex items-center gap-2 rounded-lg bg-green-100 px-5 py-2.5 text-sm font-medium text-green-700">
                    <CheckCircle className="size-4" /> {ts("completed")}
                  </span>
                  <span className="inline-flex items-center gap-1 text-sm font-medium text-amber-600">
                    <Star className="size-4" /> {t("labs.pointsCount", { count: selectedLab.score })}
                  </span>
                </div>
              ) : (
                <button
                  onClick={() => handleAttemptLab(selectedLab.id)}
                  disabled={submitting}
                  className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
                >
                  {submitting ? (
                    <div className="size-4 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent" />
                  ) : t("labs.markCompleted")}
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-teal-500/5 via-card to-green-500/5 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-teal-500/10">
            <FlaskConical className="size-6 text-teal-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("labs.title")}</h1>
            <p className="text-sm text-muted-foreground">{t("labs.subtitle")}</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-teal-50">
              <FlaskConical className="size-5 text-teal-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{labs.length}</p>
              <p className="text-xs text-muted-foreground">{t("labs.totalLabs")}</p>
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
              <Star className="size-5 text-amber-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{avgScore}</p>
              <p className="text-xs text-muted-foreground">{t("labs.avgScore")}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {labTypes.map((type) => {
          const Icon = type.icon
          return (
            <button
              key={type.value}
              onClick={() => setSelectedType(type.value)}
              className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-medium transition-colors ${
                selectedType === type.value
                  ? "bg-primary text-primary-foreground"
                  : "bg-muted text-muted-foreground hover:bg-muted/80"
              }`}
            >
              <Icon className="size-3" />
              {type.name}
            </button>
          )
        })}
      </div>

      {filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10">
            <FlaskConical className="size-8 text-primary" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("labs.emptyTitle")}</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            {t("labs.emptyDesc")}
          </p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((lab) => {
            const TypeIcon = typeIconMap[lab.labType] || FlaskConical
            const typeColor = labTypes.find((t) => t.value === lab.labType)
            return (
              <button
                key={lab.id}
                onClick={() => { setSelectedLab(lab); setNotes("") }}
                className="rounded-2xl border border-border bg-card p-5 text-left shadow-xs transition-all hover:shadow-md hover:border-primary/30"
              >
                <div className="flex items-start gap-3">
                  <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${typeColor?.bg || "bg-muted"}`}>
                    <TypeIcon className={`size-5 ${typeColor?.color || "text-muted-foreground"}`} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="text-sm font-bold text-foreground line-clamp-2">{lab.labTitle}</h3>
                    <span className={`mt-1 inline-block rounded-full px-2 py-0.5 text-[10px] font-medium ${typeColorMap[lab.labType] || "bg-muted text-muted-foreground"}`}>
                      {lab.labType.replace("_", " ")}
                    </span>
                  </div>
                </div>
                <p className="mt-2 text-xs text-muted-foreground line-clamp-2">{lab.hypothesis}</p>
                <div className="mt-3 flex items-center gap-2">
                  {lab.isAttempted && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-medium text-green-700">
                      <CheckCircle className="size-3" /> {t("labs.doneBadge")}
                    </span>
                  )}
                  {lab.isAttempted && (
                    <span className="flex items-center gap-1 text-[10px] text-amber-600">
                      <Star className="size-3" /> {lab.score}
                    </span>
                  )}
                </div>
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}
