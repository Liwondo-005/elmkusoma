"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { primaryApi, type QuestChallenge } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { Swords, Calculator, FlaskConical, Brain, Compass, Trophy, Star, CheckCircle } from "lucide-react"

const questCategories = [
  { type: "MATH", name: "Math Quests", icon: Calculator, color: "text-purple-600", bg: "bg-purple-50", border: "border-purple-200" },
  { type: "SCIENCE", name: "Science Quests", icon: FlaskConical, color: "text-green-600", bg: "bg-green-50", border: "border-green-200" },
  { type: "LOGIC", name: "Logic Quests", icon: Brain, color: "text-blue-600", bg: "bg-blue-50", border: "border-blue-200" },
  { type: "TANZANIA", name: "Tanzania Quests", icon: Compass, color: "text-teal-600", bg: "bg-teal-50", border: "border-teal-200" },
]

const difficultyColors: Record<string, string> = {
  Easy: "bg-green-100 text-green-700",
  Medium: "bg-amber-100 text-amber-700",
  Hard: "bg-red-100 text-red-700",
}

export default function QuestsPage() {
  const { user } = useRequireAuth()
  const [quests, setQuests] = useState<QuestChallenge[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null)
  const [selectedQuest, setSelectedQuest] = useState<QuestChallenge | null>(null)
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await primaryApi.getQuestChallenges().catch(() => [])
      setQuests(data)
    } catch {
      setQuests([])
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
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Problem Solving Quests</h1>
            <p className="text-sm text-muted-foreground">Complete quests and earn rewards</p>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <Swords className="size-12 text-muted-foreground/30" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">Quests is for Primary learners</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Switch to a primary learner account to start your quest journey.
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
          Back to Quests
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
                  <Star className="size-3" /> {selectedQuest.totalPoints} points
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
                <h3 className="mt-2 text-lg font-bold text-green-800">Quest Completed!</h3>
                <p className="mt-1 text-sm text-green-700">
                  Score: {selectedQuest.score} / {selectedQuest.totalPoints}
                </p>
                {selectedQuest.completedAt && (
                  <p className="mt-1 text-xs text-green-600">
                    Completed on {new Date(selectedQuest.completedAt).toLocaleDateString()}
                  </p>
                )}
              </div>
            ) : (
              <button
                onClick={() => handleCompleteQuest(selectedQuest.id)}
                className="inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
              >
                <Swords className="size-4" />
                Start Quest
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
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Problem Solving Quests</h1>
            <p className="text-sm text-muted-foreground">Complete quests and earn rewards</p>
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
              <p className="text-xs text-muted-foreground">Quests Completed</p>
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
              <p className="text-xs text-muted-foreground">Points Earned</p>
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
              <p className="text-xs text-muted-foreground">Total Quests</p>
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
          All Quests
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
          <h3 className="mt-4 text-lg font-semibold text-foreground">No quests yet</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Your teacher will create quests for you. Check back soon for new challenges!
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
                    <Star className="size-3" /> {quest.totalPoints} pts
                  </span>
                  {quest.isCompleted && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-medium text-green-700">
                      <CheckCircle className="size-3" /> Done
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
