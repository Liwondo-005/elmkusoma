"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { primaryApi, type QuestChallenge, type RealWorldMission } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { Swords, Calculator, FlaskConical, Brain, Compass, Trophy, Star, CheckCircle, Globe, MapPin, Target, X, BookOpen } from "lucide-react"


const difficultyColors: Record<string, string> = {
  Easy: "bg-green-100 text-green-700",
  Medium: "bg-amber-100 text-amber-700",
  Hard: "bg-red-100 text-red-700",
}

const missionTypeConfig: Record<string, { icon: typeof Compass; color: string; bg: string }> = {
  TANZANIA: { icon: Compass, color: "text-teal-600", bg: "bg-teal-50" },
  WORLD: { icon: Globe, color: "text-blue-600", bg: "bg-blue-50" },
  SCIENCE: { icon: FlaskConical, color: "text-green-600", bg: "bg-green-50" },
  MATH: { icon: Calculator, color: "text-purple-600", bg: "bg-purple-50" },
  LANGUAGE: { icon: BookOpen, color: "text-amber-600", bg: "bg-amber-50" },
}

export default function QuestsPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("primary")
  const ts = useTranslations("status")
  const questCategories = [
  { type: "MATH", name: t("quests.catMath"), icon: Calculator, color: "text-purple-600", bg: "bg-purple-50", border: "border-purple-200" },
  { type: "SCIENCE", name: t("quests.catScience"), icon: FlaskConical, color: "text-green-600", bg: "bg-green-50", border: "border-green-200" },
  { type: "LOGIC", name: t("quests.catLogic"), icon: Brain, color: "text-blue-600", bg: "bg-blue-50", border: "border-blue-200" },
  { type: "TANZANIA", name: t("quests.catTanzania"), icon: Compass, color: "text-teal-600", bg: "bg-teal-50", border: "border-teal-200" },
]
  const [quests, setQuests] = useState<QuestChallenge[]>([])
  const [missions, setMissions] = useState<RealWorldMission[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null)
  const [selectedQuest, setSelectedQuest] = useState<QuestChallenge | null>(null)
  const [selectedMission, setSelectedMission] = useState<RealWorldMission | null>(null)
  const [evidence, setEvidence] = useState("")
  const [submittingMission, setSubmittingMission] = useState(false)
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const [qData, mData] = await Promise.all([
        primaryApi.getQuestChallenges().catch(() => []),
        primaryApi.getRealWorldMissions().catch(() => []),
      ])
      setQuests(qData)
      setMissions(mData)
    } catch {
      setQuests([])
      setMissions([])
    } finally {
      setLoading(false)
    }
  }

  async function handleCompleteQuest(questId: string) {
    try {
      const score = Math.floor(Math.random() * 40) + 60
      const updated = await primaryApi.completeQuest(questId, score)
      setQuests((prev) => prev.map((q) => (q.id === questId ? updated : q)))
      setSelectedQuest(null)
    } catch {
      // handle silently
    }
  }

  async function handleCompleteMission(missionId: string) {
    if (!evidence.trim()) return
    try {
      setSubmittingMission(true)
      const updated = await primaryApi.completeMission(missionId, evidence)
      setMissions((prev) => prev.map((m) => (m.id === missionId ? updated : m)))
      setSelectedMission(null)
      setEvidence("")
    } catch {
      // handle silently
    } finally {
      setSubmittingMission(false)
    }
  }

  const filtered = selectedCategory
    ? quests.filter((q) => q.questType === selectedCategory)
    : quests

  const completedCount = quests.filter((q) => q.isCompleted).length
  const totalPoints = quests.reduce((sum, q) => sum + (q.isCompleted ? q.score : 0), 0)

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
            <Swords className="size-5 text-purple-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("quests.title")}</h1>
            <p className="text-sm text-muted-foreground">{t("quests.subtitle")}</p>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <Swords className="size-12 text-muted-foreground/30" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("quests.primaryOnlyTitle")}</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            {t("quests.primaryOnlyDesc")}
          </p>
        </div>
      </div>
    )
  }

  if (selectedQuest) {
    const cat = questCategories.find((c) => c.type === selectedQuest.questType)
    const CatIcon = cat?.icon || Swords
    return (
      <div className="mx-auto max-w-4xl space-y-6">
        <button
          onClick={() => setSelectedQuest(null)}
          className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
        >
          {t("quests.backToQuests")}
        </button>

        <div className="rounded-2xl border border-border bg-card p-8 shadow-xs">
          <div className="flex items-center gap-3">
            <div className={`flex size-12 items-center justify-center rounded-2xl ${cat?.bg || "bg-muted"}`}>
              <CatIcon className={`size-6 ${cat?.color || "text-muted-foreground"}`} />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-foreground">{selectedQuest.title}</h1>
              <div className="mt-1 flex items-center gap-2">
                <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${difficultyColors[selectedQuest.difficulty] || "bg-muted text-muted-foreground"}`}>
                  {selectedQuest.difficulty}
                </span>
                <span className="flex items-center gap-1 text-xs text-muted-foreground">
                  <Star className="size-3" /> {t("labs.pointsCount", { count: selectedQuest.totalPoints })}
                </span>
              </div>
            </div>
          </div>

          <div className="mt-6">
            <p className="text-sm text-muted-foreground">{selectedQuest.description}</p>
          </div>

          <div className="mt-8">
            {selectedQuest.isCompleted ? (
              <div className="rounded-xl bg-green-50 border border-green-200 p-6 text-center">
                <CheckCircle className="mx-auto size-10 text-green-500" />
                <h3 className="mt-2 text-lg font-bold text-green-800">{t("quests.completedTitle")}</h3>
                <p className="mt-1 text-sm text-green-700">
                  {t("quests.scoreLine", { score: selectedQuest.score, total: selectedQuest.totalPoints })}
                </p>
                {selectedQuest.completedAt && (
                  <p className="mt-1 text-xs text-green-600">
                    {t("quests.completedOn", { date: new Date(selectedQuest.completedAt).toLocaleDateString() })}
                  </p>
                )}
              </div>
            ) : (
              <button
                onClick={() => handleCompleteQuest(selectedQuest.id)}
                className="inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
              >
                <Swords className="size-4" />
                {t("quests.startQuest")}
              </button>
            )}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-purple-500/5 via-card to-primary/5 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-purple-500/10">
            <Swords className="size-6 text-purple-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("quests.title")}</h1>
            <p className="text-sm text-muted-foreground">{t("quests.subtitle")}</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-purple-50">
              <Trophy className="size-5 text-purple-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{completedCount}</p>
              <p className="text-xs text-muted-foreground">{t("quests.statCompleted")}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-amber-50">
              <Star className="size-5 text-amber-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{totalPoints}</p>
              <p className="text-xs text-muted-foreground">{t("quests.statPoints")}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-green-50">
              <Swords className="size-5 text-green-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{quests.length}</p>
              <p className="text-xs text-muted-foreground">{t("quests.statTotal")}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        <button
          onClick={() => setSelectedCategory(null)}
          className={`rounded-full px-3 py-1.5 text-xs font-medium transition-colors ${
            !selectedCategory
              ? "bg-primary text-primary-foreground"
              : "bg-muted text-muted-foreground hover:bg-muted/80"
          }`}
        >
          {t("quests.filterAll")}
        </button>
        {questCategories.map((cat) => (
          <button
            key={cat.type}
            onClick={() => setSelectedCategory(selectedCategory === cat.type ? null : cat.type)}
            className={`rounded-full px-3 py-1.5 text-xs font-medium transition-colors ${
              selectedCategory === cat.type
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground hover:bg-muted/80"
            }`}
          >
            {cat.name}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10">
            <Swords className="size-8 text-primary" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("quests.emptyTitle")}</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            {t("quests.emptyDesc")}
          </p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((quest) => {
            const cat = questCategories.find((c) => c.type === quest.questType)
            const CatIcon = cat?.icon || Swords
            return (
              <button
                key={quest.id}
                onClick={() => setSelectedQuest(quest)}
                className="rounded-2xl border border-border bg-card p-5 text-left shadow-xs transition-all hover:shadow-md hover:border-primary/30"
              >
                <div className="flex items-start gap-3">
                  <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${cat?.bg || "bg-muted"}`}>
                    <CatIcon className={`size-5 ${cat?.color || "text-muted-foreground"}`} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="text-sm font-bold text-foreground line-clamp-1">{quest.title}</h3>
                    <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{quest.description}</p>
                  </div>
                </div>
                <div className="mt-3 flex items-center gap-2">
                  <span className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${difficultyColors[quest.difficulty] || "bg-muted text-muted-foreground"}`}>
                    {quest.difficulty}
                  </span>
                  <span className="flex items-center gap-1 text-[10px] text-muted-foreground">
                    <Star className="size-3" /> {t("quests.ptsCount", { count: quest.totalPoints })}
                  </span>
                  {quest.isCompleted && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-medium text-green-700">
                      <CheckCircle className="size-3" /> {t("labs.doneBadge")}
                    </span>
                  )}
                </div>
              </button>
            )
          })}
        </div>
      )}

      {selectedMission && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-lg rounded-2xl border border-border bg-card p-6 shadow-xl">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-foreground">{selectedMission.missionTitle}</h2>
              <button onClick={() => { setSelectedMission(null); setEvidence("") }} className="text-muted-foreground hover:text-foreground">
                <X className="size-5" />
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <p className="text-sm text-muted-foreground">{selectedMission.description}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-foreground">{t("quests.locationLine", { location: selectedMission.location })}</p>
              </div>
              <div className="rounded-xl bg-muted/30 p-4">
                <p className="text-sm font-medium text-foreground">{t("quests.instructions")}
                <p className="mt-1 text-sm text-muted-foreground">{selectedMission.instructions}</p>
              </div>
              {selectedMission.isCompleted ? (
                <div className="rounded-xl bg-green-50 border border-green-200 p-4 text-center">
                  <CheckCircle className="mx-auto size-8 text-green-500" />
                  <p className="mt-2 text-sm font-bold text-green-800">{t("quests.missionDone")}
                  {selectedMission.evidence && (
                    <p className="mt-1 text-xs text-green-700">{t("quests.evidenceLine", { text: selectedMission.evidence })}</p>
                  )}
                </div>
              ) : (
                <>
                  <div>
                    <label className="text-sm font-medium text-foreground">{t("quests.evidenceLabel")}
                    <textarea
                      value={evidence}
                      onChange={(e) => setEvidence(e.target.value)}
                      placeholder={t("quests.evidencePlaceholder")}
                      rows={3}
                      className="mt-2 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
                    />
                  </div>
                  <button
                    onClick={() => handleCompleteMission(selectedMission.id)}
                    disabled={submittingMission || !evidence.trim()}
                    className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
                  >
                    {submittingMission ? (
                      <div className="size-4 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent" />
                    ) : (
                      {t("quests.completeMission")}
                    )}
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-gradient-to-br from-amber-500/5 via-card to-orange-500/5 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-500/10">
            <MapPin className="size-6 text-amber-500" />
          </div>
          <div>
            <h2 className="text-xl font-bold tracking-tight text-foreground">{t("quests.missionsTitle")}</h2>
            <p className="text-sm text-muted-foreground">{t("quests.missionsSubtitle")}</p>
          </div>
        </div>
      </div>

      {missions.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-12 text-center">
          <MapPin className="size-10 text-muted-foreground/30" />
          <h3 className="mt-3 text-sm font-semibold text-foreground">{t("quests.noMissions")}
          <p className="mt-1 text-xs text-muted-foreground">{t("quests.noMissionsDesc")}</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {missions.map((mission) => {
            const mConfig = missionTypeConfig[mission.missionType] || missionTypeConfig.TANZANIA
            const MIcon = mConfig.icon
            return (
              <button
                key={mission.id}
                onClick={() => { setSelectedMission(mission); setEvidence("") }}
                className="rounded-2xl border border-border bg-card p-5 text-left shadow-xs transition-all hover:shadow-md hover:border-primary/30"
              >
                <div className="flex items-start gap-3">
                  <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${mConfig.bg}`}>
                    <MIcon className={`size-5 ${mConfig.color}`} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="text-sm font-bold text-foreground line-clamp-1">{mission.missionTitle}</h3>
                    <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{mission.description}</p>
                  </div>
                </div>
                <div className="mt-3 flex items-center gap-2">
                  <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                    <MapPin className="size-3" /> {mission.location}
                  </span>
                  <span className="flex items-center gap-1 text-[10px] text-amber-600">
                    <Star className="size-3" /> {t("quests.ptsCount", { count: mission.points })}
                  </span>
                  {mission.isCompleted && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-medium text-green-700">
                      <CheckCircle className="size-3" /> {t("labs.doneBadge")}
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
