"use client"

import { useState, useEffect, useCallback } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  AlertCircle, Brain, Plus, BookOpen, FileText, Video,
  GraduationCap, Clock, CheckCircle, Filter, Trash2, Eye,
} from "lucide-react"

type DeepContent = {
  id: string; studentId: string; courseId?: string; moduleId?: string;
  title: string; contentType: string; contentText?: string; fileUrl?: string;
  difficultyLevel?: string; tags?: string; isCompleted?: boolean;
  timeSpentMinutes?: number; notes?: string;
}

const CONTENT_TYPE_OPTIONS = [
  { value: "Article", icon: BookOpen }, { value: "Video", icon: Video },
  { value: "Paper", icon: FileText }, { value: "Case Study", icon: AlertCircle },
  { value: "Tutorial", icon: GraduationCap }, { value: "Reference", icon: BookOpen },
  { value: "Practical", icon: Brain }, { value: "Assessment", icon: CheckCircle },
]
const DIFFICULTY_OPTIONS = ["Beginner", "Intermediate", "Advanced"]
const CONTENT_TYPE_COLORS: Record<string, string> = {
  Article: "bg-blue-100 text-blue-700", Video: "bg-purple-100 text-purple-700",
  Paper: "bg-indigo-100 text-indigo-700", "Case Study": "bg-amber-100 text-amber-700",
  Tutorial: "bg-teal-100 text-teal-700", Reference: "bg-slate-100 text-slate-700",
  Practical: "bg-orange-100 text-orange-700", Assessment: "bg-red-100 text-red-700",
}
const DIFFICULTY_COLORS: Record<string, string> = {
  Beginner: "bg-emerald-100 text-emerald-700",
  Intermediate: "bg-amber-100 text-amber-700",
  Advanced: "bg-red-100 text-red-700",
}

export default function DeepLearningPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [contents, setContents] = useState<DeepContent[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [filterType, setFilterType] = useState("All")
  const [filterDifficulty, setFilterDifficulty] = useState("All")
  const [submitting, setSubmitting] = useState(false)
  const [form, setForm] = useState({ title: "", contentType: "Article", contentText: "", difficultyLevel: "Intermediate", tags: "", notes: "" })

  const loadContents = useCallback(async () => {
    if (!user?.id) return
    try {
      setLoading(true); setError(null)
      const res = await collegeApi.getLearnerDeepContent(user.id)
      setContents(res.data || [])
    } catch { setError(tc("error.load")) }
    finally { setLoading(false) }
  }, [user?.id])

  useEffect(() => { if (user?.id) loadContents() }, [user?.id, loadContents])

  async function handleCreate() {
    if (!form.title.trim() || !user?.id) return
    setSubmitting(true)
    try {
      await collegeApi.createDeepContent({ studentId: user.id, title: form.title, contentType: form.contentType, contentText: form.contentText || undefined, difficultyLevel: form.difficultyLevel, tags: form.tags || undefined, notes: form.notes || undefined })
      setForm({ title: "", contentType: "Article", contentText: "", difficultyLevel: "Intermediate", tags: "", notes: "" })
      setShowForm(false); await loadContents()
    } catch { setError(tc("error.create")) } finally { setSubmitting(false) }
  }

  async function toggleComplete(id: string, isCompleted: boolean) {
    try { await collegeApi.updateDeepContent(id, { isCompleted: !isCompleted }); await loadContents() } catch { setError(tc("error.update")) }
  }

  async function handleDelete(id: string) {
    if (!confirm(tc("confirmDelete"))) return
    try { await collegeApi.deleteDeepContent(id); await loadContents() } catch { setError(tc("error.delete")) }
  }

  const filtered = contents.filter(c => {
    if (filterType !== "All" && c.contentType !== filterType) return false
    if (filterDifficulty !== "All" && c.difficultyLevel !== filterDifficulty) return false
    return true
  })
  const totalTimeSpent = contents.reduce((sum, c) => sum + (c.timeSpentMinutes || 0), 0)
  const completedCount = contents.filter(c => c.isCompleted).length
  const contentTypeCounts: Record<string, number> = {}
  contents.forEach(c => { contentTypeCounts[c.contentType] = (contentTypeCounts[c.contentType] || 0) + 1 })

  if (authLoading || loading) return <div role="main"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>
  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Student"

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle={t("deepLearning.subtitle")} />

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" /><span>{error}</span>
          <button onClick={loadContents} aria-label={tc("retry")} className="ml-auto text-xs underline font-medium hover:text-red-900">{tc("retry")}</button>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-primary/10"><Brain className="size-5 text-primary" /></div>
            <div><p className="text-xs font-medium text-muted-foreground">{t("deepLearning.totalContent")}</p><p className="text-2xl font-extrabold">{contents.length}</p></div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-emerald-100"><CheckCircle className="size-5 text-emerald-600" /></div>
            <div><p className="text-xs font-medium text-muted-foreground">{t("deepLearning.completed")}</p><p className="text-2xl font-extrabold">{completedCount}</p></div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-amber-100"><Clock className="size-5 text-amber-600" /></div>
            <div><p className="text-xs font-medium text-muted-foreground">{t("deepLearning.timeSpent")}</p><p className="text-2xl font-extrabold">{(totalTimeSpent / 60).toFixed(1)}h</p></div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground mb-3">{t("deepLearning.contentTypes")}</p>
          <div className="space-y-1.5">
            {CONTENT_TYPE_OPTIONS.map(opt => { const count = contentTypeCounts[opt.value] || 0; if (!count) return null; return (<div key={opt.value} className="flex items-center justify-between text-xs"><span>{opt.value}</span><span className="font-semibold">{count}</span></div>) })}
            {!Object.keys(contentTypeCounts).length && <p className="text-xs text-muted-foreground">{t("deepLearning.noContent")}</p>}
          </div>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <button onClick={() => setShowForm(!showForm)} aria-label={t("deepLearning.addContent")} className="flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
          <Plus className="size-4" /> {t("deepLearning.addContent")}
        </button>
        <div className="flex items-center gap-2"><Filter className="size-4 text-muted-foreground" />
          <select value={filterType} onChange={e => setFilterType(e.target.value)} aria-label={t("deepLearning.filterType")} className="rounded-lg border border-border bg-card px-3 py-1.5 text-sm">
            <option value="All">{tc("allTypes")}</option>
            {CONTENT_TYPE_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.value}</option>)}
          </select>
          <select value={filterDifficulty} onChange={e => setFilterDifficulty(e.target.value)} aria-label={t("deepLearning.filterDifficulty")} className="rounded-lg border border-border bg-card px-3 py-1.5 text-sm">
            <option value="All">{tc("allDifficulties")}</option>
            {DIFFICULTY_OPTIONS.map(d => <option key={d} value={d}>{d}</option>)}
          </select>
        </div>
      </div>

      {showForm && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <h3 className="font-semibold">{t("deepLearning.addNewContent")}</h3>
          <input placeholder={t("deepLearning.title")} value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} aria-label={t("deepLearning.title")} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm" />
          <div className="grid grid-cols-2 gap-3">
            <select value={form.contentType} onChange={e => setForm(f => ({ ...f, contentType: e.target.value }))} aria-label={t("deepLearning.contentType")} className="rounded-lg border border-border bg-background px-3 py-2 text-sm">
              {CONTENT_TYPE_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.value}</option>)}
            </select>
            <select value={form.difficultyLevel} onChange={e => setForm(f => ({ ...f, difficultyLevel: e.target.value }))} aria-label={t("deepLearning.difficultyLevel")} className="rounded-lg border border-border bg-background px-3 py-2 text-sm">
              {DIFFICULTY_OPTIONS.map(d => <option key={d} value={d}>{d}</option>)}
            </select>
          </div>
          <textarea placeholder={t("deepLearning.contentPlaceholder")} value={form.contentText} onChange={e => setForm(f => ({ ...f, contentText: e.target.value }))} aria-label={t("deepLearning.contentText")} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm min-h-[100px]" />
          <input placeholder={t("deepLearning.tagsPlaceholder")} value={form.tags} onChange={e => setForm(f => ({ ...f, tags: e.target.value }))} aria-label={t("deepLearning.tags")} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm" />
          <input placeholder={t("deepLearning.notesPlaceholder")} value={form.notes} onChange={e => setForm(f => ({ ...f, notes: e.target.value }))} aria-label={t("deepLearning.notes")} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm" />
          <div className="flex justify-end gap-2">
            <button onClick={() => setShowForm(false)} aria-label={tc("cancel")} className="rounded-lg border border-border px-4 py-2 text-sm">{tc("cancel")}</button>
            <button onClick={handleCreate} disabled={!form.title.trim() || submitting} aria-label={tc("create")} className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">{submitting ? tc("saving") : tc("create")}</button>
          </div>
        </div>
      )}

      {filtered.length === 0 ? (
        <EmptyState title={t("deepLearning.noContentFound")} description={filterType !== "All" || filterDifficulty !== "All" ? t("deepLearning.tryFilters") : t("deepLearning.addFirst")} />
      ) : (
        <div className="space-y-3">
          {filtered.map(c => (
            <div key={c.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-start justify-between">
                <div className="flex-1 cursor-pointer" onClick={() => setExpandedId(expandedId === c.id ? null : c.id)}>
                  <div className="flex items-center gap-2 flex-wrap">
                    <h4 className="font-medium">{c.title}</h4>
                    <span className={`text-xs px-2 py-0.5 rounded-full ${CONTENT_TYPE_COLORS[c.contentType] || "bg-gray-100 text-gray-700"}`}>{c.contentType}</span>
                    {c.difficultyLevel && <span className={`text-xs px-2 py-0.5 rounded-full ${DIFFICULTY_COLORS[c.difficultyLevel] || "bg-gray-100 text-gray-700"}`}>{c.difficultyLevel}</span>}
                  </div>
                  {c.tags && <p className="text-xs text-muted-foreground mt-1">{t("deepLearning.tags")}: {c.tags}</p>}
                </div>
                <div className="flex items-center gap-2">
                  <label className="flex items-center gap-1.5 text-xs cursor-pointer">
                    <input type="checkbox" checked={!!c.isCompleted} onChange={() => toggleComplete(c.id, !!c.isCompleted)} aria-label={c.isCompleted ? tc("done") : tc("markDone")} className="rounded" />
                    {c.isCompleted ? tc("done") : tc("markDone")}
                  </label>
                  <button onClick={() => handleDelete(c.id)} aria-label={tc("delete")} className="p-1 rounded hover:bg-destructive/10 text-muted-foreground hover:text-destructive"><Trash2 className="size-4" /></button>
                </div>
              </div>
              {expandedId === c.id && (
                <div className="mt-3 pt-3 border-t border-border space-y-2 text-sm">
                  {c.contentText && <div><span className="font-medium">{t("deepLearning.contentLabel")}: </span><span className="text-muted-foreground whitespace-pre-wrap">{c.contentText}</span></div>}
                  {c.notes && <div><span className="font-medium">{t("deepLearning.notesLabel")}: </span><span className="text-muted-foreground">{c.notes}</span></div>}
                  {c.fileUrl && <a href={c.fileUrl} target="_blank" rel="noopener noreferrer" aria-label={t("deepLearning.viewFile")} className="inline-flex items-center gap-1 text-primary hover:underline"><Eye className="size-3" /> {t("deepLearning.viewFile")}</a>}
                  <p className="text-xs text-muted-foreground">{t("deepLearning.timeSpentLabel")}: {c.timeSpentMinutes || 0} {tc("minutes")}</p>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
