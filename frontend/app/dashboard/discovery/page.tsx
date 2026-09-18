"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { primaryApi, type DiscoveryEntry } from "@/lib/api"
import { type LearningLevel, primarySubjects } from "@/lib/learner-config"
import { Lightbulb, FlaskConical, Eye, BookOpen, Plus, X, CheckCircle, Sparkles } from "lucide-react"

const discoveryTypes = [
  { value: "WONDER", label: "Wonder", icon: Lightbulb, color: "text-amber-500", bg: "bg-amber-50", border: "border-amber-200", badge: "bg-amber-100 text-amber-700" },
  { value: "EXPERIMENT", label: "Experiment", icon: FlaskConical, color: "text-green-500", bg: "bg-green-50", border: "border-green-200", badge: "bg-green-100 text-green-700" },
  { value: "OBSERVATION", label: "Observation", icon: Eye, color: "text-blue-500", bg: "bg-blue-50", border: "border-blue-200", badge: "bg-blue-100 text-blue-700" },
  { value: "RESEARCH", label: "Research", icon: BookOpen, color: "text-purple-500", bg: "bg-purple-50", border: "border-purple-200", badge: "bg-purple-100 text-purple-700" },
]

export default function DiscoveryPage() {
  const { user } = useRequireAuth()
  const [entries, setEntries] = useState<DiscoveryEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ title: "", question: "", discoveryType: "WONDER", subjectName: "" })
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
      const data = await primaryApi.getDiscoveryEntries().catch(() => [])
      setEntries(data)
    } catch {
      setEntries([])
    } finally {
      setLoading(false)
    }
  }

  async function handleAdd(e: React.FormEvent) {
    e.preventDefault()
    if (!form.title.trim() || !form.question.trim()) return
    try {
      setSubmitting(true)
      const entry = await primaryApi.addDiscoveryEntry({
        title: form.title,
        question: form.question,
        discoveryType: form.discoveryType,
        subjectName: form.subjectName || undefined,
      })
      setEntries((prev) => [entry, ...prev])
      setForm({ title: "", question: "", discoveryType: "WONDER", subjectName: "" })
      setShowForm(false)
    } catch {
      // handle error silently
    } finally {
      setSubmitting(false)
    }
  }

  async function handleResolve(id: string) {
    try {
      const updated = await primaryApi.resolveDiscoveryEntry(id)
      setEntries((prev) => prev.map((e) => (e.id === id ? updated : e)))
    } catch {
      // handle error silently
    }
  }

  const grouped = discoveryTypes.map((type) => ({
    ...type,
    entries: entries.filter((e) => e.discoveryType === type.value),
  }))

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
          <div className="flex size-10 items-center justify-center rounded-2xl bg-amber-500/10">
            <Lightbulb className="size-5 text-amber-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Discovery Mode</h1>
            <p className="text-sm text-muted-foreground">Ninashangaa...</p>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <Lightbulb className="size-12 text-muted-foreground/30" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">Discovery Mode is for Primary learners</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Switch to a primary learner account to explore your curiosity.
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-amber-500/5 via-card to-primary/5 p-6 shadow-xs">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-500/10">
              <Lightbulb className="size-6 text-amber-500" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-foreground">I Wonder...</h1>
              <p className="text-sm text-muted-foreground">Ninashangaa...</p>
            </div>
          </div>
          <button
            onClick={() => setShowForm(true)}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            <Plus className="size-4" />
            Ask a Question
          </button>
        </div>
      </div>

      {showForm && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-foreground">What do you wonder about?</h2>
            <button onClick={() => setShowForm(false)} className="text-muted-foreground hover:text-foreground">
              <X className="size-5" />
            </button>
          </div>
          <form onSubmit={handleAdd} className="space-y-4">
            <div>
              <label className="text-sm font-medium text-foreground">Title</label>
              <input
                type="text"
                value={form.title}
                onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                placeholder="Give your discovery a name"
                className="mt-1 h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                required
              />
            </div>
            <div>
              <label className="text-sm font-medium text-foreground">Your Question</label>
              <textarea
                value={form.question}
                onChange={(e) => setForm((f) => ({ ...f, question: e.target.value }))}
                placeholder="What would you like to know?"
                rows={3}
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
                required
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="text-sm font-medium text-foreground">Type</label>
                <select
                  value={form.discoveryType}
                  onChange={(e) => setForm((f) => ({ ...f, discoveryType: e.target.value }))}
                  className="mt-1 h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                >
                  {discoveryTypes.map((t) => (
                    <option key={t.value} value={t.value}>{t.label}</option>
                  ))}
                </select>
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
            </div>
            <button
              type="submit"
              disabled={submitting || !form.title.trim() || !form.question.trim()}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
            >
              {submitting ? (
                <div className="size-4 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent" />
              ) : (
                <Sparkles className="size-4" />
              )}
              Add Discovery
            </button>
          </form>
        </div>
      )}

      {entries.length === 0 && !showForm ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <div className="flex size-20 items-center justify-center rounded-full bg-amber-500/10">
            <Lightbulb className="size-10 text-amber-500" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">What do you wonder about?</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Everyone is curious! Ask questions, make observations, and discover new things about the world.
          </p>
          <button
            onClick={() => setShowForm(true)}
            className="mt-6 inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            <Plus className="size-4" /> Start Wondering
          </button>
        </div>
      ) : (
        <div className="space-y-6">
          {grouped.map((group) => {
            const Icon = group.icon
            return (
              <div key={group.value}>
                <div className="flex items-center gap-2 mb-3">
                  <Icon className={`size-5 ${group.color}`} />
                  <h2 className="text-base font-semibold text-foreground">{group.label}s</h2>
                  <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                    {group.entries.length}
                  </span>
                </div>
                {group.entries.length === 0 ? (
                  <div className={`rounded-2xl border ${group.border} ${group.bg} p-8 text-center`}>
                    <Icon className={`mx-auto size-8 ${group.color} opacity-30`} />
                    <p className="mt-2 text-sm text-muted-foreground">No {group.label.toLowerCase()}s yet</p>
                  </div>
                ) : (
                  <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                    {group.entries.map((entry) => (
                      <div
                        key={entry.id}
                        className={`rounded-2xl border ${group.border} bg-card p-5 shadow-xs transition-all hover:shadow-md`}
                      >
                        <div className="flex items-start justify-between">
                          <h3 className="text-sm font-bold text-foreground">{entry.title}</h3>
                          <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold ${group.badge}`}>
                            {group.label}
                          </span>
                        </div>
                        <p className="mt-2 text-xs text-muted-foreground line-clamp-2">{entry.question}</p>
                        {entry.subjectName && (
                          <span className="mt-2 inline-flex items-center rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                            {entry.subjectName}
                          </span>
                        )}
                        <div className="mt-3 flex items-center justify-between">
                          <span className={`inline-flex items-center gap-1 text-[10px] font-medium ${entry.isResolved ? "text-green-600" : "text-muted-foreground"}`}>
                            {entry.isResolved ? <CheckCircle className="size-3" /> : null}
                            {entry.isResolved ? "Resolved" : "Open"}
                          </span>
                          {!entry.isResolved && (
                            <button
                              onClick={() => handleResolve(entry.id)}
                              className="text-[10px] font-medium text-primary hover:underline"
                            >
                              I resolved this!
                            </button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
