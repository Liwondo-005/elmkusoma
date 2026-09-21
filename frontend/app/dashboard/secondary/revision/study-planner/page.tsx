"use client"

import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"
import { ArrowLeft, Calendar, Plus, Clock, BookOpen, Trash2 } from "lucide-react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type SecondaryStudyPlan } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"

export default function StudyPlannerPage() {
  const t = useTranslations("secondary")
  const tc = useTranslations("common")
  const { user } = useRequireAuth()
  const [plans, setPlans] = useState<SecondaryStudyPlan[]>([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ topicName: "", plannedDate: "", durationMinutes: 30, priority: "MEDIUM" as const, notes: "" })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user?.id) { setLoading(false); return }
    secondaryApi.getStudyPlans(user.id)
      .then(setPlans)
      .catch(() => setError(t("errorLoading")))
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  if (error) {
    return (
      <div className="mx-auto max-w-5xl p-4 pb-24" role="main">
        <div className="rounded-2xl border border-red-100 bg-red-50 p-8 text-center">
          <p className="text-sm text-red-600">{error}</p>
          <button
            onClick={() => { setError(null); setLoading(true); }}
            className="mt-3 rounded-xl bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700"
            aria-label={tc("retry")}
          >
            {tc("retry")}
          </button>
        </div>
      </div>
    )
  }

  const planned = plans.filter(s => s.status === "PLANNED" || s.status === "IN_PROGRESS")
  const done = plans.filter(s => s.status === "COMPLETED")

  async function addSession() {
    if (!form.topicName || !form.plannedDate || !user) return
    setSaving(true)
    try {
      const newPlan = await secondaryApi.createStudyPlan({
        studentId: user.id,
        classGroupId: user.classGroupId || "",
        topicName: form.topicName,
        plannedDate: form.plannedDate,
        durationMinutes: form.durationMinutes,
        priority: form.priority,
        notes: form.notes,
      })
      setPlans(prev => [...prev, newPlan])
      setForm({ topicName: "", plannedDate: "", durationMinutes: 30, priority: "MEDIUM", notes: "" })
      setShowForm(false)
    } catch {}
    setSaving(false)
  }

  async function markDone(id: string) {
    try {
      await secondaryApi.updateStudyPlan(id, { status: "COMPLETED" })
      setPlans(prev => prev.map(p => p.id === id ? { ...p, status: "COMPLETED" as const, completedDate: new Date().toISOString().split("T")[0] } : p))
    } catch {}
  }

  async function markPlanned(id: string) {
    try {
      await secondaryApi.updateStudyPlan(id, { status: "PLANNED" })
      setPlans(prev => prev.map(p => p.id === id ? { ...p, status: "PLANNED" as const, completedDate: null } : p))
    } catch {}
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary/revision" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={tc("goBack")}><ArrowLeft className="size-5 text-gray-600" /></Link>
        <div className="flex-1">
          <h1 className="text-xl font-bold text-gray-900">{t("studyPlanner")}</h1>
          <p className="text-sm text-gray-500">{t("planYourStudySessions")}</p>
        </div>
        <button onClick={() => setShowForm(!showForm)} className="flex items-center gap-1.5 rounded-xl bg-indigo-600 px-3 py-2 text-sm font-medium text-white hover:bg-indigo-700" aria-label={t("addSession")}><Plus className="size-4" /> {t("addSession")}</button>
      </div>

      <div className="rounded-2xl bg-gradient-to-r from-indigo-600 to-purple-500 p-5 text-white">
        <h2 className="font-bold">{t("thisWeek")}</h2>
        <div className="mt-3 grid grid-cols-3 gap-3">
          <div className="rounded-xl bg-white/10 p-3 text-center"><p className="text-2xl font-bold">{planned.length}</p><p className="text-xs text-white/70">{t("planned")}</p></div>
          <div className="rounded-xl bg-white/10 p-3 text-center"><p className="text-2xl font-bold">{done.length}</p><p className="text-xs text-white/70">{t("completed")}</p></div>
          <div className="rounded-xl bg-white/10 p-3 text-center"><p className="text-2xl font-bold">{planned.reduce((sum, s) => sum + (s.durationMinutes || 0), 0)}</p><p className="text-xs text-white/70">{t("minutesPlanned")}</p></div>
        </div>
      </div>

      {showForm && (
        <div className="rounded-2xl border border-indigo-200 bg-indigo-50 p-5">
          <h3 className="mb-3 font-semibold text-gray-900">{t("newStudySession")}</h3>
          <div className="grid gap-3 sm:grid-cols-2">
            <input type="text" placeholder={t("topic")} value={form.topicName} onChange={e => setForm(f => ({ ...f, topicName: e.target.value }))} className="rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm" aria-label={t("topic")} />
            <input type="date" value={form.plannedDate} onChange={e => setForm(f => ({ ...f, plannedDate: e.target.value }))} className="rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm" aria-label={t("plannedDate")} />
            <input type="number" placeholder={t("durationMin")} value={form.durationMinutes} onChange={e => setForm(f => ({ ...f, durationMinutes: parseInt(e.target.value) || 30 }))} className="rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm" aria-label={t("durationMin")} />
            <select value={form.priority} onChange={e => setForm(f => ({ ...f, priority: e.target.value as any }))} className="rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm" aria-label={t("priority")}>
              <option value="HIGH">{t("highPriority")}</option><option value="MEDIUM">{t("medium")}</option><option value="LOW">{t("low")}</option>
            </select>
          </div>
          <div className="mt-3 flex gap-2">
            <button onClick={addSession} disabled={saving} className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50" aria-label={t("saveSession")}>{saving ? tc("saving") : t("saveSession")}</button>
            <button onClick={() => setShowForm(false)} className="rounded-xl bg-gray-100 px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-200" aria-label={tc("cancel")}>{tc("cancel")}</button>
          </div>
        </div>
      )}

      {planned.length > 0 && (
        <section>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">{t("planned")}</h2>
          <div className="space-y-2">
            {planned.map(s => (
              <div key={s.id} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-indigo-50"><BookOpen className="size-5 text-indigo-600" /></div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-900">{s.topicName}</p>
                  <p className="text-xs text-gray-400">{new Date(s.plannedDate).toLocaleDateString([], { weekday: "short", month: "short", day: "numeric" })} · {s.durationMinutes} {t("min")} · {s.priority}</p>
                </div>
                <button onClick={() => markDone(s.id)} className="rounded-lg bg-green-50 px-3 py-1.5 text-xs font-medium text-green-600 hover:bg-green-100" aria-label={`${t("markDone")} ${s.topicName}`}>{t("done")}</button>
              </div>
            ))}
          </div>
        </section>
      )}

      {done.length > 0 && (
        <section>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">{t("completed")}</h2>
          <div className="space-y-2">
            {done.map(s => (
              <div key={s.id} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 opacity-60">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-green-50"><Clock className="size-5 text-green-500" /></div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-600 line-through">{s.topicName}</p>
                  <p className="text-xs text-gray-400">{s.durationMinutes} {t("min")}</p>
                </div>
                <button onClick={() => markPlanned(s.id)} className="rounded-lg bg-gray-100 px-3 py-1.5 text-xs font-medium text-gray-500 hover:bg-gray-200" aria-label={`${t("undo")} ${s.topicName}`}>{t("undo")}</button>
              </div>
            ))}
          </div>
        </section>
      )}

      {plans.length === 0 && !showForm && (
        <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
          <Calendar className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">{t("noSessionsPlanned")}</h3>
          <p className="mt-1 text-sm text-gray-500">{t("clickAddSession")}</p>
        </div>
      )}
    </div>
  )
}
