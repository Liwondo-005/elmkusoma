"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { primaryApi, type QuestChallenge } from "@/lib/api"
import { Zap, Trophy, Star, Filter, CheckCircle, Clock, Target, ArrowLeft } from "lucide-react"

type Difficulty = "ALL" | "EASY" | "MEDIUM" | "HARD"

export default function ChallengeZonePage() {
  const { user } = useRequireAuth()
  const t = useTranslations("primary")
  const ts = useTranslations("status")
  const [challenges, setChallenges] = useState<QuestChallenge[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState<Difficulty>("ALL")
  const [selected, setSelected] = useState<QuestChallenge | null>(null)
  const [answer, setAnswer] = useState("")
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!user) return
    loadChallenges()
  }, [user])

  async function loadChallenges() {
    try {
      setLoading(true)
      const data = await primaryApi.getQuestChallenges().catch(() => [])
      setChallenges(data.filter(q => q.questType === "CHALLENGE" || q.difficulty))
    } catch (e) { console.error("Failed to load challenges:", e) } finally {
      setLoading(false)
    }
  }

  async function handleSubmit() {
    if (!selected || !answer.trim()) return
    try {
      setSubmitting(true)
      await primaryApi.completeQuest(selected.id, answer.length > 10 ? 100 : 50)
      setChallenges(prev => prev.map(c => c.id === selected.id ? { ...c, isCompleted: true, score: answer.length > 10 ? 100 : 50 } : c))
      setSelected(null)
      setAnswer("")
    } catch (e) { console.error("Failed to submit challenge:", e) } finally {
      setSubmitting(false)
    }
  }

  const filtered = filter === "ALL" ? challenges : challenges.filter(c => c.difficulty === filter)
  const completed = challenges.filter(c => c.isCompleted).length
  const totalPoints = challenges.reduce((sum, c) => sum + (c.isCompleted ? c.score : 0), 0)

  if (loading) {
    return <div className="flex min-h-[50vh] items-center justify-center"><div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>
  }

  if (selected) {
    return (
      <div className="mx-auto max-w-3xl space-y-6">
        <button onClick={() => { setSelected(null); setAnswer("") }} className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" /> {t("challenge.backToList")}
        </button>
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <div className="flex items-center gap-3 mb-4">
            <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10"><Zap className="size-5 text-primary" /></div>
            <div>
              <h2 className="text-lg font-bold text-foreground">{selected.title}</h2>
              <p className="text-xs text-muted-foreground">{selected.subjectName || t("challenge.fallbackSubject")}
            </div>
          </div>
          <p className="text-sm text-muted-foreground mb-4">{selected.description}</p>
          <div className="flex gap-2 mb-4">
            <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${selected.difficulty === "EASY" ? "bg-green-100 text-green-700" : selected.difficulty === "MEDIUM" ? "bg-amber-100 text-amber-700" : "bg-red-100 text-red-700"}`}>
              {selected.difficulty}
            </span>
            <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">{t("quests.ptsCount", { count: selected.totalPoints })}</span>
          </div>
          <textarea
            value={answer}
            onChange={(e) => setAnswer(e.target.value)}
            placeholder={t("challenge.answerPlaceholder")}
            className="w-full rounded-xl border border-border bg-background p-4 text-sm min-h-[150px] focus:outline-none focus:ring-2 focus:ring-primary/30"
            aria-label={t("challenge.answerLabel")}
          />
          <div className="mt-4 flex justify-end">
            <button
              onClick={handleSubmit}
              disabled={!answer.trim() || submitting}
              className="rounded-xl bg-primary px-6 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              {submitting ? t("challenge.submitting") : t("challenge.submit")}
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-primary/5 via-card to-amber-50/5 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-primary/10"><Zap className="size-6 text-primary" /></div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("challenge.title")}</h1>
            <p className="text-sm text-muted-foreground">{t("challenge.subtitle")}</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-3">
        <div className="rounded-2xl border border-border bg-card p-4 text-center shadow-xs">
          <Trophy className="mx-auto size-6 text-amber-500" />
          <p className="mt-1 text-2xl font-bold text-foreground">{completed}</p>
          <p className="text-xs text-muted-foreground">{ts("completed")}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 text-center shadow-xs">
          <Star className="mx-auto size-6 text-yellow-500" />
          <p className="mt-1 text-2xl font-bold text-foreground">{totalPoints}</p>
          <p className="text-xs text-muted-foreground">{t("challenge.points")}
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 text-center shadow-xs">
          <Target className="mx-auto size-6 text-primary" />
          <p className="mt-1 text-2xl font-bold text-foreground">{challenges.length}</p>
          <p className="text-xs text-muted-foreground">{t("challenge.total")}
        </div>
      </div>

      <div className="flex items-center gap-2">
        <Filter className="size-4 text-muted-foreground" />
        {(["ALL", "EASY", "MEDIUM", "HARD"] as Difficulty[]).map(d => (
          <button key={d} onClick={() => setFilter(d)} className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${filter === d ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground hover:bg-muted/80"}`}>
            {d}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <div className="mx-auto flex size-16 items-center justify-center rounded-2xl bg-primary/10"><Zap className="size-8 text-primary" /></div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("challenge.emptyTitle")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">{t("challenge.emptyDesc")}</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map(c => (
            <button key={c.id} onClick={() => setSelected(c)} className={`rounded-2xl border border-border bg-card p-5 shadow-xs text-left transition-all hover:shadow-md ${c.isCompleted ? "opacity-75" : "hover:border-primary/30"}`}>
              <div className="flex items-start justify-between">
                <h3 className="text-sm font-semibold text-foreground">{c.title}</h3>
                {c.isCompleted && <CheckCircle className="size-4 text-green-500 shrink-0" />}
              </div>
              <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{c.description}</p>
              <div className="mt-3 flex items-center gap-2">
                <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${c.difficulty === "EASY" ? "bg-green-100 text-green-700" : c.difficulty === "MEDIUM" ? "bg-amber-100 text-amber-700" : "bg-red-100 text-red-700"}`}>
                  {c.difficulty}
                </span>
                {c.subjectName && <span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">{c.subjectName}</span>}
              </div>
              <div className="mt-3 flex items-center justify-between">
                <span className="text-xs font-medium text-amber-600">{t("quests.ptsCount", { count: c.totalPoints })}</span>
                {c.isCompleted ? (
                  <span className="text-xs font-medium text-green-600">{t("quests.scoreLine", { score: c.score, total: c.totalPoints })}
                ) : (
                  <span className="text-xs font-medium text-primary">{t("challenge.start")}
                )}
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
