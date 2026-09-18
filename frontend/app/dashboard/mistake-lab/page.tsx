"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { primaryApi, type MistakeLabEntry } from "@/lib/api"
import { type LearningLevel, primarySubjects } from "@/lib/learner-config"
import { AlertCircle, Plus, X, CheckCircle, Filter, BookOpen } from "lucide-react"

export default function MistakeLabPage() {
  const { user } = useRequireAuth()
  const [entries, setEntries] = useState<MistakeLabEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ question: "", wrongAnswer: "", correctAnswer: "", explanation: "", subjectName: "" })
  const [submitting, setSubmitting] = useState(false)
  const [filter, setFilter] = useState<"all" | "not_reviewed" | string>("all")
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await primaryApi.getMistakeLabEntries().catch(() => [])
      setEntries(data)
    } catch {
      setEntries([])
    } finally {
      setLoading(false)
    }
  }

  async function handleAdd(e: React.FormEvent) {
    e.preventDefault()
    if (!form.question.trim() || !form.correctAnswer.trim()) return
    try {
      setSubmitting(true)
      const entry = await primaryApi.addMistakeLabEntry({
        question: form.question,
        wrongAnswer: form.wrongAnswer,
        correctAnswer: form.correctAnswer,
        explanation: form.explanation,
        subjectName: form.subjectName || undefined,
      })
      setEntries((prev) => [entry, ...prev])
      setForm({ question: "", wrongAnswer: "", correctAnswer: "", explanation: "", subjectName: "" })
      setShowForm(false)
    } catch {
      // handle silently
    } finally {
      setSubmitting(false)
    }
  }

  async function handleReview(id: string) {
    try {
      const updated = await primaryApi.reviewMistake(id)
      setEntries((prev) => prev.map((e) => (e.id === id ? updated : e)))
    } catch {
      // handle silently
    }
  }

  const filtered = entries.filter((e) => {
    if (filter === "not_reviewed") return !e.isReviewed
    if (filter !== "all") return e.subjectName === filter
    return true
  })

  const reviewedCount = entries.filter((e) => e.isReviewed).length

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
            <AlertCircle className="size-5 text-red-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Mistake Lab</h1>
            <p className="text-sm text-muted-foreground">Learn from your mistakes</p>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <AlertCircle className="size-12 text-muted-foreground/30" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">Mistake Lab is for Primary learners</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Switch to a primary learner account to start learning from mistakes.
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-red-500/5 via-card to-primary/5 p-6 shadow-xs">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-red-500/10">
              <AlertCircle className="size-6 text-red-500" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-foreground">Mistake Lab</h1>
              <p className="text-sm text-muted-foreground">Mistakes help us learn! Review your mistakes and grow stronger.</p>
            </div>
          </div>
          <button
            onClick={() => setShowForm(true)}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            <Plus className="size-4" />
            Add Mistake
          </button>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-red-50">
              <AlertCircle className="size-5 text-red-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{entries.length}</p>
              <p className="text-xs text-muted-foreground">Total Mistakes</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-green-50">
              <CheckCircle className="size-5 text-green-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{reviewedCount}</p>
              <p className="text-xs text-muted-foreground">Reviewed</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
              <BookOpen className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{entries.length - reviewedCount}</p>
              <p className="text-xs text-muted-foreground">To Review</p>
            </div>
          </div>
        </div>
      </div>

      {showForm && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-foreground">Add a Mistake to Learn From</h2>
            <button onClick={() => setShowForm(false)} className="text-muted-foreground hover:text-foreground">
              <X className="size-5" />
            </button>
          </div>
          <form onSubmit={handleAdd} className="space-y-4">
            <div>
              <label className="text-sm font-medium text-foreground">The Question / Problem</label>
              <textarea
                value={form.question}
                onChange={(e) => setForm((f) => ({ ...f, question: e.target.value }))}
                placeholder="What was the question?"
                rows={2}
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
                required
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="text-sm font-medium text-foreground">Your Wrong Answer</label>
                <input
                  type="text"
                  value={form.wrongAnswer}
                  onChange={(e) => setForm((f) => ({ ...f, wrongAnswer: e.target.value }))}
                  placeholder="What did you answer?"
                  className="mt-1 h-10 w-full rounded-lg border border-red-200 bg-red-50 px-3 text-sm outline-none focus:border-red-400"
                />
              </div>
              <div>
                <label className="text-sm font-medium text-foreground">The Correct Answer</label>
                <input
                  type="text"
                  value={form.correctAnswer}
                  onChange={(e) => setForm((f) => ({ ...f, correctAnswer: e.target.value }))}
                  placeholder="What is the right answer?"
                  className="mt-1 h-10 w-full rounded-lg border border-green-200 bg-green-50 px-3 text-sm outline-none focus:border-green-400"
                  required
                />
              </div>
            </div>
            <div>
              <label className="text-sm font-medium text-foreground">Explanation</label>
              <textarea
                value={form.explanation}
                onChange={(e) => setForm((f) => ({ ...f, explanation: e.target.value }))}
                placeholder="Why is this the correct answer?"
                rows={3}
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
              />
            </div>
            <div>
              <label className="text-sm font-medium text-foreground">Subject (optional)</label>
              <select
                value={form.subjectName}
                onChange={(e) => setForm((f) => ({ ...f, subjectName: e.target.value }))}
                className="mt-1 h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              >
                <option value="">No specific subject</option>
                {primarySubjects.map((s) => (
                  <option key={s.name} value={s.name}>{s.name}</option>
                ))}
              </select>
            </div>
            <button
              type="submit"
              disabled={submitting || !form.question.trim() || !form.correctAnswer.trim()}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
            >
              {submitting ? (
                <div className="size-4 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent" />
              ) : (
                <Plus className="size-4" />
              )}
              Add Mistake
            </button>
          </form>
        </div>
      )}

      <div className="flex flex-wrap items-center gap-2">
        <Filter className="size-4 text-muted-foreground" />
        <button
          onClick={() => setFilter("all")}
          className={`rounded-full px-3 py-1.5 text-xs font-medium transition-colors ${
            filter === "all"
              ? "bg-primary text-primary-foreground"
              : "bg-muted text-muted-foreground hover:bg-muted/80"
          }`}
        >
          All
        </button>
        <button
          onClick={() => setFilter("not_reviewed")}
          className={`rounded-full px-3 py-1.5 text-xs font-medium transition-colors ${
            filter === "not_reviewed"
              ? "bg-primary text-primary-foreground"
              : "bg-muted text-muted-foreground hover:bg-muted/80"
          }`}
        >
          Not Reviewed
        </button>
        {primarySubjects.map((s) => (
          <button
            key={s.name}
            onClick={() => setFilter(filter === s.name ? "all" : s.name)}
            className={`rounded-full px-3 py-1.5 text-xs font-medium transition-colors ${
              filter === s.name
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground hover:bg-muted/80"
            }`}
          >
            {s.name}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-green-50">
            <CheckCircle className="size-8 text-green-500" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">No mistakes yet - you are doing great!</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            But when you make one, it will help you learn and grow stronger.
          </p>
          <button
            onClick={() => setShowForm(true)}
            className="mt-6 inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            <Plus className="size-4" /> Add First Mistake
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {filtered.map((entry) => (
            <div key={entry.id} className="rounded-2xl border border-border bg-card p-6 shadow-xs transition-all hover:shadow-md">
              <div className="flex items-start justify-between">
                <h3 className="text-sm font-bold text-foreground">{entry.question}</h3>
                <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold ${entry.isReviewed ? "bg-green-100 text-green-700" : "bg-amber-100 text-amber-700"}`}>
                  {entry.isReviewed ? "Reviewed" : "To Review"}
                </span>
              </div>

              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <div className="rounded-xl bg-red-50 border border-red-200 p-3">
                  <p className="text-[10px] font-bold text-red-600 uppercase">Your Answer</p>
                  <p className="mt-1 text-sm text-red-700">{entry.wrongAnswer}</p>
                </div>
                <div className="rounded-xl bg-green-50 border border-green-200 p-3">
                  <p className="text-[10px] font-bold text-green-600 uppercase">Correct Answer</p>
                  <p className="mt-1 text-sm text-green-700">{entry.correctAnswer}</p>
                </div>
              </div>

              {entry.explanation && (
                <div className="mt-3 rounded-xl bg-blue-50 border border-blue-200 p-3">
                  <p className="text-[10px] font-bold text-blue-600 uppercase">Explanation</p>
                  <p className="mt-1 text-sm text-blue-700">{entry.explanation}</p>
                </div>
              )}

              <div className="mt-3 flex items-center gap-3">
                {entry.subjectName && (
                  <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-medium text-primary">
                    {entry.subjectName}
                  </span>
                )}
                {!entry.isReviewed && (
                  <button
                    onClick={() => handleReview(entry.id)}
                    className="text-[10px] font-medium text-primary hover:underline"
                  >
                    Mark as Reviewed
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
