"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { learnerApi, type Enrollment, type LearningGoal } from "@/lib/learner-api"
import { announce } from "@/lib/announce"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import {
  Target,
  Plus,
  Trash2,
  CheckCircle,
  Clock,
  ArrowRight,
  AlertCircle,
  X,
  Pencil,
} from "lucide-react"

export default function GoalsPage() {
  const { user, loading: authLoading } = useAuth()
  const t = useTranslations("goals")
  const tc = useTranslations("common")
  const [goals, setGoals] = useState<LearningGoal[]>([])
  const [enrollments, setEnrollments] = useState<Enrollment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingGoal, setEditingGoal] = useState<LearningGoal | null>(null)
  const [formTitle, setFormTitle] = useState("")
  const [formDescription, setFormDescription] = useState("")
  const [formTargetDate, setFormTargetDate] = useState("")
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!user || (user.role !== "Other Learner" && user.role !== "Student")) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const [enrollmentsData, goalsData] = await Promise.all([
        learnerApi.getEnrollments().catch(() => []),
        learnerApi.getGoals(),
      ])
      setEnrollments(enrollmentsData)
      setGoals(goalsData)
    } catch {
      setError(t("loadFailed"))
    } finally {
      setLoading(false)
    }
  }

  function openCreateForm() {
    setEditingGoal(null)
    setFormTitle("")
    setFormDescription("")
    setFormTargetDate("")
    setShowForm(true)
  }

  function openEditForm(goal: LearningGoal) {
    setEditingGoal(goal)
    setFormTitle(goal.title)
    setFormDescription(goal.description || "")
    setFormTargetDate(goal.targetDate || "")
    setShowForm(true)
  }

  async function handleSave() {
    if (!formTitle.trim() || saving) return
    try {
      setSaving(true)
      setError(null)
      const payload = {
        title: formTitle.trim(),
        description: formDescription.trim(),
        targetDate: formTargetDate || null,
      }
      if (editingGoal) {
        const updated = await learnerApi.updateGoal(editingGoal.id, payload)
        setGoals((prev) => prev.map((g) => (g.id === updated.id ? updated : g)))
        announce(t("updated"))
      } else {
        const created = await learnerApi.createGoal(payload)
        setGoals((prev) => [created, ...prev])
        announce(t("created"))
      }
      setShowForm(false)
    } catch {
      setError(t("saveFailed"))
      announce(t("saveFailed"))
    } finally {
      setSaving(false)
    }
  }

  async function toggleGoalStatus(goal: LearningGoal) {
    try {
      setError(null)
      const isCompleted = goal.status === "COMPLETED"
      const updated = await learnerApi.updateGoal(goal.id, {
        status: isCompleted ? "ACTIVE" : "COMPLETED",
        progressPercentage: isCompleted ? 0 : 100,
      })
      setGoals((prev) => prev.map((g) => (g.id === updated.id ? updated : g)))
      announce(isCompleted ? t("updated") : t("markComplete"))
    } catch {
      setError(t("saveFailed"))
    }
  }

  async function deleteGoal(goalId: string) {
    try {
      setError(null)
      await learnerApi.deleteGoal(goalId)
      setGoals((prev) => prev.filter((g) => g.id !== goalId))
      announce(t("deleted"))
    } catch {
      setError(t("saveFailed"))
    }
  }

  if (authLoading || (user?.role !== "Other Learner" && user?.role !== "Student")) {
    return <LoadingState />
  }

  const activeGoals = goals.filter((g) => g.status === "ACTIVE" || g.status === "active")
  const completedGoals = goals.filter((g) => g.status === "COMPLETED" || g.status === "completed")

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("title")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("subtitle")}</p>
        </div>
        <button
          onClick={openCreateForm}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-4" />
          {t("newGoal")}
        </button>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{t("activeGoals")}</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{activeGoals.length}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{t("completed")}</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{completedGoals.length}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{t("enrolledCourses")}</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{enrollments.length}</p>
        </div>
      </div>

      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-lg">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-foreground">
                {editingGoal ? t("editGoal") : t("newLearningGoal")}
              </h2>
              <button onClick={() => setShowForm(false)} className="rounded-lg p-1 hover:bg-muted">
                <X className="size-4" />
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-foreground">{t("goalTitle")} *</label>
                <input
                  type="text"
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  placeholder={t("goalTitlePlaceholder")}
                  className="mt-1.5 h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-foreground">{t("description")}</label>
                <textarea
                  value={formDescription}
                  onChange={(e) => setFormDescription(e.target.value)}
                  placeholder={t("descriptionPlaceholder")}
                  rows={3}
                  className="mt-1.5 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-foreground">{t("targetDate")}</label>
                <input
                  type="date"
                  value={formTargetDate}
                  onChange={(e) => setFormTargetDate(e.target.value)}
                  className="mt-1.5 h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                />
              </div>
              <div className="flex gap-3 pt-2">
                <button
                  onClick={() => setShowForm(false)}
                  className="flex-1 rounded-lg border border-border px-4 py-2.5 text-sm font-medium text-foreground hover:bg-muted"
                >
                  {t("cancel")}
                </button>
                <button
                  onClick={handleSave}
                  disabled={!formTitle.trim() || saving}
                  className="flex-1 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                >
                  {editingGoal ? t("saveChanges") : t("createGoal")}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {loading ? (
        <LoadingState />
      ) : goals.length === 0 ? (
        <EmptyState
          icon={<Target className="size-8" />}
          title={t("noGoalsYet")}
          description={t("noGoalsDesc")}
          action={
            <button
              onClick={openCreateForm}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              {t("createFirstGoal")} <ArrowRight className="size-4" />
            </button>
          }
        />
      ) : (
        <div className="space-y-6">
          {activeGoals.length > 0 && (
            <div>
              <h2 className="text-sm font-semibold text-foreground mb-3">{t("activeSection")}</h2>
              <div className="space-y-3">
                {activeGoals.map((goal) => (
                  <div key={goal.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <Target className="size-4 text-primary shrink-0" />
                          <h3 className="text-sm font-semibold text-foreground">{goal.title}</h3>
                        </div>
                        {goal.description && (
                          <p className="mt-1 text-xs text-muted-foreground">{goal.description}</p>
                        )}
                        <div className="mt-2 flex items-center gap-3 flex-wrap">
                          {goal.targetDate && (
                            <span className="inline-flex items-center gap-1 text-[10px] text-muted-foreground">
                              <Clock className="size-3" /> {t("target", { date: new Date(goal.targetDate).toLocaleDateString() })}
                            </span>
                          )}
                        </div>
                      </div>
                      <div className="flex items-center gap-1 shrink-0">
                        <button
                          onClick={() => openEditForm(goal)}
                          className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground"
                          title={t("edit")}
                        >
                          <Pencil className="size-3.5" />
                        </button>
                        <button
                          onClick={() => toggleGoalStatus(goal)}
                          className="rounded-lg p-1.5 text-muted-foreground hover:bg-green-500/10 hover:text-green-600"
                          title={t("markComplete")}
                        >
                          <CheckCircle className="size-3.5" />
                        </button>
                        <button
                          onClick={() => deleteGoal(goal.id)}
                          className="rounded-lg p-1.5 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                          title={t("delete")}
                        >
                          <Trash2 className="size-3.5" />
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {completedGoals.length > 0 && (
            <div>
              <h2 className="text-sm font-semibold text-foreground mb-3">{t("completedSection")}</h2>
              <div className="space-y-3">
                {completedGoals.map((goal) => (
                  <div key={goal.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs opacity-70">
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <CheckCircle className="size-4 text-green-600 shrink-0" />
                          <h3 className="text-sm font-semibold text-foreground line-through">{goal.title}</h3>
                        </div>
                        {goal.description && (
                          <p className="mt-1 text-xs text-muted-foreground">{goal.description}</p>
                        )}
                      </div>
                      <div className="flex items-center gap-1 shrink-0">
                        <button
                          onClick={() => toggleGoalStatus(goal)}
                          className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground"
                          title={t("reopen")}
                        >
                          <Clock className="size-3.5" />
                        </button>
                        <button
                          onClick={() => deleteGoal(goal.id)}
                          className="rounded-lg p-1.5 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                          title={t("delete")}
                        >
                          <Trash2 className="size-3.5" />
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
