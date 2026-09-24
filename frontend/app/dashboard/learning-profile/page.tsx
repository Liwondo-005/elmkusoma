"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { primaryApi, type LearningProfile } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { User, Edit3, X, Star, BookOpen, Target, Award, CheckCircle } from "lucide-react"


const styleColorMap: Record<string, string> = {
  VISUAL: "bg-blue-100 text-blue-700",
  AUDITORY: "bg-purple-100 text-purple-700",
  KINESTHETIC: "bg-green-100 text-green-700",
  READING_WRITING: "bg-amber-100 text-amber-700",
}

export default function LearningProfilePage() {
  const { user } = useRequireAuth()
  const t = useTranslations("primary")
  const learningStyles = [
  { value: "VISUAL", label: t("learnProfile.styleVisual"), description: t("learnProfile.styleVisualDesc"), color: "bg-blue-100 text-blue-700", icon: "eyes" },
  { value: "AUDITORY", label: t("learnProfile.styleAuditory"), description: t("learnProfile.styleAuditoryDesc"), color: "bg-purple-100 text-purple-700", icon: "ear" },
  { value: "KINESTHETIC", label: t("learnProfile.styleKinesthetic"), description: t("learnProfile.styleKinestheticDesc"), color: "bg-green-100 text-green-700", icon: "hand" },
  { value: "READING_WRITING", label: t("learnProfile.styleReading"), description: t("learnProfile.styleReadingDesc"), color: "bg-amber-100 text-amber-700", icon: "book" },
]
  const ts = useTranslations("status")
  const [profile, setProfile] = useState<LearningProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ learningStyle: "VISUAL", strengths: "", interests: "", goals: "" })
  const [strengthInput, setStrengthInput] = useState("")
  const [interestInput, setInterestInput] = useState("")
  const [strengthsList, setStrengthsList] = useState<string[]>([])
  const [interestsList, setInterestsList] = useState<string[]>([])
  const [submitting, setSubmitting] = useState(false)
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  useEffect(() => {
    if (!user) return
    loadProfile()
  }, [user])

  async function loadProfile() {
    try {
      setLoading(true)
      const data = await primaryApi.getLearningProfile().catch(() => null)
      setProfile(data)
      if (data) {
        setForm({
          learningStyle: data.learningStyle || "VISUAL",
          strengths: data.strengths || "",
          interests: data.interests || "",
          goals: data.goals || "",
        })
        setStrengthsList(data.strengths ? data.strengths.split(",").map((s) => s.trim()).filter(Boolean) : [])
        setInterestsList(data.interests ? data.interests.split(",").map((s) => s.trim()).filter(Boolean) : [])
      }
    } catch {
      setProfile(null)
    } finally {
      setLoading(false)
    }
  }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault()
    try {
      setSubmitting(true)
      const updated = await primaryApi.updateLearningProfile({
        learningStyle: form.learningStyle,
        strengths: strengthsList.join(", "),
        interests: interestsList.join(", "),
        goals: form.goals,
      })
      setProfile(updated)
      setShowForm(false)
    } catch {
      // handle silently
    } finally {
      setSubmitting(false)
    }
  }

  function addStrength() {
    if (strengthInput.trim() && !strengthsList.includes(strengthInput.trim())) {
      setStrengthsList((prev) => [...prev, strengthInput.trim()])
      setStrengthInput("")
    }
  }

  function removeStrength(s: string) {
    setStrengthsList((prev) => prev.filter((x) => x !== s))
  }

  function addInterest() {
    if (interestInput.trim() && !interestsList.includes(interestInput.trim())) {
      setInterestsList((prev) => [...prev, interestInput.trim()])
      setInterestInput("")
    }
  }

  function removeInterest(s: string) {
    setInterestsList((prev) => prev.filter((x) => x !== s))
  }

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
          <div className="flex size-10 items-center justify-center rounded-2xl bg-purple-500/10">
            <User className="size-5 text-purple-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("learnProfile.title")}</h1>
            <p className="text-sm text-muted-foreground">{t("learnProfile.subtitle")}
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <User className="size-12 text-muted-foreground/30" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("learnProfile.primaryOnlyTitle")}</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            {t("learnProfile.primaryOnlyDesc")}
          </p>
        </div>
      </div>
    )
  }

  if (!profile && !showForm) {
    return (
      <div className="mx-auto max-w-4xl space-y-6">
        <div className="rounded-2xl border border-border bg-gradient-to-br from-purple-500/5 via-card to-blue-500/5 p-6 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-purple-500/10">
              <User className="size-6 text-purple-500" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("learnProfile.title")}</h1>
              <p className="text-sm text-muted-foreground">{t("learnProfile.subtitle")}
            </div>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10">
            <User className="size-8 text-primary" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("learnProfile.emptyTitle")}</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            {t("learnProfile.emptyDesc")}
          </p>
          <button
            onClick={() => setShowForm(true)}
            className="mt-6 inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            <Edit3 className="size-4" /> {t("learnProfile.createButton")}
          </button>
        </div>
      </div>
    )
  }

  if (showForm) {
    return (
      <div className="mx-auto max-w-4xl space-y-6">
        <div className="rounded-2xl border border-border bg-gradient-to-br from-purple-500/5 via-card to-blue-500/5 p-6 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-purple-500/10">
              <User className="size-6 text-purple-500" />
            </div>
            <div className="flex-1">
              <h1 className="text-2xl font-bold tracking-tight text-foreground">
                {profile ? t("learnProfile.editTitle") : t("learnProfile.createTitle")}
              </h1>
              <p className="text-sm text-muted-foreground">{t("learnProfile.formSubtitle")}
            </div>
            {profile && (
              <button onClick={() => setShowForm(false)} className="text-muted-foreground hover:text-foreground">
                <X className="size-5" />
              </button>
            )}
          </div>
        </div>

        <form onSubmit={handleSave} className="rounded-2xl border border-border bg-card p-6 shadow-xs space-y-6">
          <div>
            <label className="text-sm font-bold text-foreground">{t("learnProfile.styleLabel")}</label>
            <p className="text-xs text-muted-foreground mt-1">{t("learnProfile.styleHint")}</p>
            <div className="mt-3 grid gap-3 sm:grid-cols-2">
              {learningStyles.map((style) => (
                <button
                  key={style.value}
                  type="button"
                  onClick={() => setForm((prev) => ({ ...prev, learningStyle: style.value }))}
                  className={`rounded-xl border p-4 text-left transition-all ${
                    form.learningStyle === style.value
                      ? `${styleColorMap[style.value]} border-current ring-2 ring-primary/20`
                      : "border-border bg-background hover:border-primary/30"
                  }`}
                >
                  <p className="text-sm font-bold">{style.label}</p>
                  <p className="mt-1 text-xs opacity-80">{style.description}</p>
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="text-sm font-bold text-foreground">{t("learnProfile.strengthsLabel")}</label>
            <p className="text-xs text-muted-foreground mt-1">{t("learnProfile.strengthsHint")}</p>
            <div className="mt-2 flex gap-2">
              <input
                type="text"
                value={strengthInput}
                onChange={(e) => setStrengthInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), addStrength())}
                placeholder={t("learnProfile.strengthPlaceholder")}
                className="flex-1 rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring"
              />
              <button type="button" onClick={addStrength} className="rounded-lg bg-muted px-3 py-2 text-sm font-medium hover:bg-muted/80 transition-colors">
                {t("learnProfile.addButton")}
              </button>
            </div>
            {strengthsList.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-2">
                {strengthsList.map((s) => (
                  <span key={s} className="inline-flex items-center gap-1 rounded-full bg-green-100 px-3 py-1 text-xs font-medium text-green-700">
                    {s}
                    <button type="button" onClick={() => removeStrength(s)} className="hover:text-green-900">
                      <X className="size-3" />
                    </button>
                  </span>
                ))}
              </div>
            )}
          </div>

          <div>
            <label className="text-sm font-bold text-foreground">{t("learnProfile.interestsLabel")}</label>
            <p className="text-xs text-muted-foreground mt-1">{t("learnProfile.interestsHint")}</p>
            <div className="mt-2 flex gap-2">
              <input
                type="text"
                value={interestInput}
                onChange={(e) => setInterestInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), addInterest())}
                placeholder={t("learnProfile.interestPlaceholder")}
                className="flex-1 rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring"
              />
              <button type="button" onClick={addInterest} className="rounded-lg bg-muted px-3 py-2 text-sm font-medium hover:bg-muted/80 transition-colors">
                {t("learnProfile.addButton")}
              </button>
            </div>
            {interestsList.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-2">
                {interestsList.map((s) => (
                  <span key={s} className="inline-flex items-center gap-1 rounded-full bg-blue-100 px-3 py-1 text-xs font-medium text-blue-700">
                    {s}
                    <button type="button" onClick={() => removeInterest(s)} className="hover:text-blue-900">
                      <X className="size-3" />
                    </button>
                  </span>
                ))}
              </div>
            )}
          </div>

          <div>
            <label className="text-sm font-bold text-foreground">{t("learnProfile.goalsLabel")}</label>
            <p className="text-xs text-muted-foreground mt-1">{t("learnProfile.goalsHint")}</p>
            <textarea
              value={form.goals}
              onChange={(e) => setForm((prev) => ({ ...prev, goals: e.target.value }))}
              placeholder={t("learnProfile.goalsPlaceholder")}
              rows={4}
              className="mt-2 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
          >
            {submitting ? (
              <div className="size-4 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent" />
            ) : (
              {t("learnProfile.save")}
            )}
          </button>
        </form>
      </div>
    )
  }

  const styleInfo = learningStyles.find((s) => s.value === profile?.learningStyle)
  const strengthsArr = profile?.strengths ? profile.strengths.split(",").map((s) => s.trim()).filter(Boolean) : []
  const interestsArr = profile?.interests ? profile.interests.split(",").map((s) => s.trim()).filter(Boolean) : []

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-purple-500/5 via-card to-blue-500/5 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-purple-500/10">
            <User className="size-6 text-purple-500" />
          </div>
          <div className="flex-1">
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("learnProfile.title")}</h1>
            <p className="text-sm text-muted-foreground">{t("learnProfile.subtitle")}
          </div>
          <button
            onClick={() => setShowForm(true)}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            <Edit3 className="size-4" /> {t("learnProfile.editButton")}
          </button>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-purple-50">
              <Award className="size-5 text-purple-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{profile?.level || 1}</p>
              <p className="text-xs text-muted-foreground">{t("learnProfile.currentLevel")}
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-amber-50">
              <Star className="size-5 text-amber-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{profile?.totalPoints || 0}</p>
              <p className="text-xs text-muted-foreground">{t("learnProfile.totalPoints")}
            </div>
          </div>
        </div>
      </div>

      {styleInfo && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">{t("learnProfile.styleTitle")}
          <div className={`inline-flex items-center gap-2 rounded-xl px-4 py-3 ${styleColorMap[profile?.learningStyle || "VISUAL"]}`}>
            <BookOpen className="size-5" />
            <div>
              <p className="text-sm font-bold">{styleInfo.label}</p>
              <p className="text-xs opacity-80">{styleInfo.description}</p>
            </div>
          </div>
        </div>
      )}

      {strengthsArr.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">{t("learnProfile.strengthsTitle")}
          <div className="flex flex-wrap gap-2">
            {strengthsArr.map((s) => (
              <span key={s} className="inline-flex items-center gap-1 rounded-full bg-green-100 px-3 py-1.5 text-sm font-medium text-green-700">
                <CheckCircle className="size-3" /> {s}
              </span>
            ))}
          </div>
        </div>
      )}

      {interestsArr.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">{t("learnProfile.interestsTitle")}
          <div className="flex flex-wrap gap-2">
            {interestsArr.map((s) => (
              <span key={s} className="inline-flex items-center gap-1 rounded-full bg-blue-100 px-3 py-1.5 text-sm font-medium text-blue-700">
                {s}
              </span>
            ))}
          </div>
        </div>
      )}

      {profile?.goals && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">{t("learnProfile.goalsTitle")}
          <div className="rounded-xl bg-muted/30 p-4">
            <p className="text-sm text-foreground whitespace-pre-wrap">{profile.goals}</p>
          </div>
        </div>
      )}

      {!styleInfo && strengthsArr.length === 0 && interestsArr.length === 0 && !profile?.goals && (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-12 text-center">
          <Target className="size-10 text-muted-foreground/30" />
          <h3 className="mt-3 text-sm font-semibold text-foreground">{t("learnProfile.noData")}
          <p className="mt-1 text-xs text-muted-foreground">{t("learnProfile.noDataDesc")}
        </div>
      )}
    </div>
  )
}
