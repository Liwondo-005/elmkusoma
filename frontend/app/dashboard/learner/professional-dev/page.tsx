"use client"

import { useState, useEffect, useCallback } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  AlertCircle,
  TrendingUp,
  Plus,
  Target,
  Award,
  BookOpen,
  Briefcase,
  Users,
  Calendar,
  Trash2,
  Edit,
  CheckCircle,
} from "lucide-react"

type ProfessionalDevGoal = {
  id: string
  studentId: string
  title: string
  description?: string
  goalType: string
  targetDate?: string
  completedDate?: string
  status: string
  progressPercent?: number
  evidenceUrl?: string
  notes?: string
  category?: string
}

const STATUS_OPTIONS = [
  { value: "ALL", label: "All Status" },
  { value: "NOT_STARTED", label: "Not Started" },
  { value: "IN_PROGRESS", label: "In Progress" },
  { value: "COMPLETED", label: "Completed" },
  { value: "ON_HOLD", label: "On Hold" },
  { value: "CANCELLED", label: "Cancelled" },
]

const TYPE_OPTIONS = [
  { value: "ALL", label: "All Types" },
  { value: "SKILL_DEVELOPMENT", label: "Skill Development" },
  { value: "CERTIFICATION", label: "Certification" },
  { value: "INTERNSHIP", label: "Internship" },
  { value: "RESEARCH", label: "Research" },
  { value: "CONFERENCE", label: "Conference" },
  { value: "PUBLICATION", label: "Publication" },
  { value: "NETWORKING", label: "Networking" },
  { value: "LEADERSHIP", label: "Leadership" },
  { value: "COMMUNICATION", label: "Communication" },
  { value: "TECHNICAL", label: "Technical" },
]

const GOAL_TYPE_OPTIONS = [
  { value: "SKILL_DEVELOPMENT", label: "Skill Development" },
  { value: "CERTIFICATION", label: "Certification" },
  { value: "INTERNSHIP", label: "Internship" },
  { value: "RESEARCH", label: "Research" },
  { value: "CONFERENCE", label: "Conference" },
  { value: "PUBLICATION", label: "Publication" },
  { value: "NETWORKING", label: "Networking" },
  { value: "LEADERSHIP", label: "Leadership" },
  { value: "COMMUNICATION", label: "Communication" },
  { value: "TECHNICAL", label: "Technical" },
]

const statusColors: Record<string, string> = {
  NOT_STARTED: "bg-muted text-muted-foreground",
  IN_PROGRESS: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
  COMPLETED: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
  ON_HOLD: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400",
  CANCELLED: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
}

const typeColors: Record<string, string> = {
  SKILL_DEVELOPMENT: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400",
  CERTIFICATION: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
  INTERNSHIP: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
  RESEARCH: "bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-400",
  CONFERENCE: "bg-cyan-100 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-400",
  PUBLICATION: "bg-pink-100 text-pink-700 dark:bg-pink-900/30 dark:text-pink-400",
  NETWORKING: "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400",
  LEADERSHIP: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400",
  COMMUNICATION: "bg-teal-100 text-teal-700 dark:bg-teal-900/30 dark:text-teal-400",
  TECHNICAL: "bg-slate-100 text-slate-700 dark:bg-slate-900/30 dark:text-slate-400",
}

const typeIconMap: Record<string, React.ReactNode> = {
  SKILL_DEVELOPMENT: <BookOpen className="size-4" />,
  CERTIFICATION: <Award className="size-4" />,
  INTERNSHIP: <Briefcase className="size-4" />,
  RESEARCH: <Target className="size-4" />,
  CONFERENCE: <Users className="size-4" />,
  PUBLICATION: <BookOpen className="size-4" />,
  NETWORKING: <Users className="size-4" />,
  LEADERSHIP: <TrendingUp className="size-4" />,
  COMMUNICATION: <Users className="size-4" />,
  TECHNICAL: <Target className="size-4" />,
}

function StatusBadge({ status }: { status: string }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ${
        statusColors[status] || "bg-muted text-muted-foreground"
      }`}
    >
      {status.replace(/_/g, " ")}
    </span>
  )
}

function TypeBadge({ goalType }: { goalType: string }) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium ${
        typeColors[goalType] || "bg-muted text-muted-foreground"
      }`}
    >
      {typeIconMap[goalType]}
      {goalType.replace(/_/g, " ")}
    </span>
  )
}

function ProgressBar({ percent, label }: { percent: number; label?: string }) {
  return (
    <div className="w-full">
      <div className="flex items-center justify-between text-xs">
        <span className="text-muted-foreground">{label || "Progress"}</span>
        <span className="font-semibold text-foreground">{percent}%</span>
      </div>
      <div
        role="progressbar"
        aria-valuenow={percent}
        aria-valuemin={0}
        aria-valuemax={100}
        className="mt-1.5 h-2 w-full overflow-hidden rounded-full bg-muted"
      >
        <div
          className="h-full rounded-full bg-primary transition-all"
          style={{ width: `${percent}%` }}
        />
      </div>
    </div>
  )
}

export default function ProfessionalDevPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [goals, setGoals] = useState<ProfessionalDevGoal[]>([])
  const [loading, setLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState("ALL")
  const [typeFilter, setTypeFilter] = useState("ALL")
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingGoal, setEditingGoal] = useState<ProfessionalDevGoal | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const [formTitle, setFormTitle] = useState("")
  const [formType, setFormType] = useState("SKILL_DEVELOPMENT")
  const [formDescription, setFormDescription] = useState("")
  const [formTargetDate, setFormTargetDate] = useState("")
  const [formCategory, setFormCategory] = useState("")
  const [formNotes, setFormNotes] = useState("")

  const loadGoals = useCallback(async () => {
    if (!user) return
    try {
      setLoading(true)
      const studentId = user.id || ""
      const res = await collegeApi.getLearnerProfessionalDev(studentId)
      setGoals((res.data as ProfessionalDevGoal[]) || [])
    } catch {
      setError(tc("error.load"))
    } finally {
      setLoading(false)
    }
  }, [user])

  useEffect(() => {
    if (user) loadGoals()
  }, [user, loadGoals])

  function resetForm() {
    setFormTitle("")
    setFormType("SKILL_DEVELOPMENT")
    setFormDescription("")
    setFormTargetDate("")
    setFormCategory("")
    setFormNotes("")
    setEditingGoal(null)
    setShowForm(false)
  }

  function openEditForm(goal: ProfessionalDevGoal) {
    setEditingGoal(goal)
    setFormTitle(goal.title)
    setFormType(goal.goalType)
    setFormDescription(goal.description || "")
    setFormTargetDate(goal.targetDate ? goal.targetDate.split("T")[0] : "")
    setFormCategory(goal.category || "")
    setFormNotes(goal.notes || "")
    setShowForm(true)
  }

  async function handleSubmitForm() {
    if (!user || !formTitle.trim()) return
    const studentId = user.id || ""
    const dto: Record<string, any> = {
      studentId,
      title: formTitle.trim(),
      goalType: formType,
      description: formDescription.trim() || undefined,
      targetDate: formTargetDate || undefined,
      category: formCategory.trim() || undefined,
      notes: formNotes.trim() || undefined,
    }
    try {
      if (editingGoal) {
        await collegeApi.updateProfessionalDev(editingGoal.id, dto)
      } else {
        await collegeApi.createProfessionalDev(dto)
      }
      resetForm()
      await loadGoals()
    } catch {
      setError(tc("error.save"))
    }
  }

  async function handleDelete(id: string) {
    try {
      await collegeApi.deleteProfessionalDev(id)
      setDeletingId(null)
      await loadGoals()
    } catch {
      setError(tc("error.delete"))
    }
  }

  async function handleProgressChange(id: string, percent: number) {
    try {
      await collegeApi.updateProfessionalDev(id, { progressPercent: percent })
      setGoals((prev) =>
        prev.map((g) => (g.id === id ? { ...g, progressPercent: percent } : g))
      )
    } catch {
      setError(tc("error.update"))
    }
  }

  async function handleMarkComplete(id: string) {
    try {
      await collegeApi.updateProfessionalDev(id, {
        status: "COMPLETED",
        progressPercent: 100,
        completedDate: new Date().toISOString(),
      })
      await loadGoals()
    } catch {
      setError(tc("error.update"))
    }
  }

  const filtered = goals.filter((g) => {
    const matchStatus = statusFilter === "ALL" || g.status === statusFilter
    const matchType = typeFilter === "ALL" || g.goalType === typeFilter
    return matchStatus && matchType
  })

  const totalGoals = goals.length
  const inProgress = goals.filter((g) => g.status === "IN_PROGRESS").length
  const completed = goals.filter((g) => g.status === "COMPLETED").length
  const avgProgress =
    totalGoals > 0
      ? Math.round(
          goals.reduce((sum, g) => sum + (g.progressPercent || 0), 0) / totalGoals
        )
      : 0

  if (authLoading || loading) return <div role="main"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      {error && (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadGoals() }} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      )}

      <div className="flex items-start justify-between">
        <LearnerHeader
          firstName={firstName}
          subtitle={t("professionalDev.subtitle")}
        />
        <button
          onClick={() => { resetForm(); setShowForm(true) }}
          aria-label={t("professionalDev.addGoal")}
          className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition shrink-0"
        >
          <Plus className="size-4" />
          {t("professionalDev.addGoal")}
        </button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
              <Target className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("professionalDev.totalGoals")}</p>
              <p className="text-2xl font-extrabold text-foreground">{totalGoals}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30">
              <TrendingUp className="size-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("professionalDev.inProgress")}</p>
              <p className="text-2xl font-extrabold text-foreground">{inProgress}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30">
              <CheckCircle className="size-5 text-emerald-600 dark:text-emerald-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("professionalDev.completed")}</p>
              <p className="text-2xl font-extrabold text-foreground">{completed}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/30">
              <Award className="size-5 text-amber-600 dark:text-amber-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("professionalDev.avgProgress")}</p>
              <p className="text-2xl font-extrabold text-foreground">{avgProgress}%</p>
            </div>
          </div>
        </div>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative">
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            aria-label={t("professionalDev.filterStatus")}
            className="appearance-none rounded-xl border border-border bg-card py-2.5 pl-4 pr-8 text-sm outline-none focus:ring-2 focus:ring-primary/20"
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
        <div className="relative">
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            aria-label={t("professionalDev.filterType")}
            className="appearance-none rounded-xl border border-border bg-card py-2.5 pl-4 pr-8 text-sm outline-none focus:ring-2 focus:ring-primary/20"
          >
            {TYPE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {showForm && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h3 className="text-lg font-bold text-foreground mb-4">
            {editingGoal ? t("professionalDev.editGoal") : t("professionalDev.newGoal")}
          </h3>
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium text-foreground">{t("professionalDev.title")} *</label>
              <input
                type="text"
                value={formTitle}
                onChange={(e) => setFormTitle(e.target.value)}
                placeholder="e.g. AWS Solutions Architect Certification"
                aria-label={t("professionalDev.title")}
                className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="text-sm font-medium text-foreground">{t("professionalDev.goalType")}</label>
                <select
                  value={formType}
                  onChange={(e) => setFormType(e.target.value)}
                  aria-label={t("professionalDev.goalType")}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-primary/20"
                >
                  {GOAL_TYPE_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="text-sm font-medium text-foreground">{t("professionalDev.targetDate")}</label>
                <input
                  type="date"
                  value={formTargetDate}
                  onChange={(e) => setFormTargetDate(e.target.value)}
                  aria-label={t("professionalDev.targetDate")}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>
            </div>
            <div>
              <label className="text-sm font-medium text-foreground">{t("professionalDev.description")}</label>
              <textarea
                value={formDescription}
                onChange={(e) => setFormDescription(e.target.value)}
                placeholder="Describe your goal..."
                rows={3}
                aria-label={t("professionalDev.description")}
                className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="text-sm font-medium text-foreground">{t("professionalDev.category")}</label>
                <input
                  type="text"
                  value={formCategory}
                  onChange={(e) => setFormCategory(e.target.value)}
                  placeholder="e.g. Cloud Computing"
                  aria-label={t("professionalDev.category")}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>
              <div>
                <label className="text-sm font-medium text-foreground">{t("professionalDev.notes")}</label>
                <input
                  type="text"
                  value={formNotes}
                  onChange={(e) => setFormNotes(e.target.value)}
                  placeholder="Additional notes..."
                  aria-label={t("professionalDev.notes")}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={handleSubmitForm}
                disabled={!formTitle.trim()}
                aria-label={editingGoal ? tc("update") : tc("create")}
                className="inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition disabled:opacity-50"
              >
                <CheckCircle className="size-4" />
                {editingGoal ? tc("update") : tc("create")}
              </button>
              <button
                onClick={resetForm}
                aria-label={tc("cancel")}
                className="inline-flex items-center gap-2 rounded-xl border border-border px-5 py-2.5 text-sm font-medium text-foreground hover:bg-muted transition"
              >
                {tc("cancel")}
              </button>
            </div>
          </div>
        </div>
      )}

      {filtered.length === 0 ? (
        <EmptyState
          icon={<Target className="size-8" />}
          title={t("professionalDev.noGoals")}
          description={t("professionalDev.noGoalsDesc")}
          action={
            <button
              onClick={() => { resetForm(); setShowForm(true) }}
              aria-label={t("professionalDev.addGoal")}
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition"
            >
              <Plus className="size-4" />
              {t("professionalDev.addGoal")}
            </button>
          }
        />
      ) : (
        <div className="space-y-4">
          {filtered.map((goal) => {
            const isExpanded = expandedId === goal.id
            const isDeleting = deletingId === goal.id

            return (
              <div
                key={goal.id}
                className="rounded-2xl border border-border bg-card p-5 shadow-xs"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex flex-wrap items-center gap-2 mb-2">
                      <h3 className="text-base font-semibold text-foreground truncate">
                        {goal.title}
                      </h3>
                      <TypeBadge goalType={goal.goalType} />
                      <StatusBadge status={goal.status} />
                    </div>

                    {goal.targetDate && (
                      <div className="flex items-center gap-1 text-xs text-muted-foreground mb-2">
                        <Calendar className="size-3.5" />
                        <span>{t("professionalDev.target")}: {new Date(goal.targetDate).toLocaleDateString()}</span>
                      </div>
                    )}

                    <div className="max-w-md">
                      <ProgressBar percent={goal.progressPercent || 0} label={tc("progress")} />
                    </div>

                    {goal.category && (
                      <p className="mt-2 text-xs text-muted-foreground">
                        {t("professionalDev.categoryLabel")}: <span className="font-medium text-foreground">{goal.category}</span>
                      </p>
                    )}

                    {goal.notes && !isExpanded && (
                      <p className="mt-1 text-xs text-muted-foreground truncate max-w-lg">
                        {goal.notes}
                      </p>
                    )}
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <input
                      type="range"
                      min={0}
                      max={100}
                      value={goal.progressPercent || 0}
                      onChange={(e) => handleProgressChange(goal.id, Number(e.target.value))}
                      aria-label={`${tc("progress")}: ${goal.progressPercent || 0}%`}
                      className="w-24 accent-primary"
                      title={`${tc("progress")}: ${goal.progressPercent || 0}%`}
                    />
                    <button
                      onClick={() => setExpandedId(isExpanded ? null : goal.id)}
                      aria-label={isExpanded ? tc("collapse") : tc("expand")}
                      className="rounded-lg p-2 text-muted-foreground hover:bg-muted transition"
                    >
                      <Edit className="size-4" />
                    </button>
                    {goal.status !== "COMPLETED" && (
                      <button
                        onClick={() => handleMarkComplete(goal.id)}
                        aria-label={t("professionalDev.markComplete")}
                        className="rounded-lg p-2 text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-900/20 transition"
                      >
                        <CheckCircle className="size-4" />
                      </button>
                    )}
                    {!isDeleting ? (
                      <button
                        onClick={() => setDeletingId(goal.id)}
                        aria-label={tc("delete")}
                        className="rounded-lg p-2 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 transition"
                      >
                        <Trash2 className="size-4" />
                      </button>
                    ) : (
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => handleDelete(goal.id)}
                          aria-label={tc("confirm")}
                          className="rounded-lg bg-red-500 px-2.5 py-1 text-xs font-medium text-white hover:bg-red-600 transition"
                        >
                          {tc("confirm")}
                        </button>
                        <button
                          onClick={() => setDeletingId(null)}
                          aria-label={tc("cancel")}
                          className="rounded-lg border border-border px-2.5 py-1 text-xs font-medium text-foreground hover:bg-muted transition"
                        >
                          {tc("cancel")}
                        </button>
                      </div>
                    )}
                  </div>
                </div>

                {isExpanded && (
                  <div className="mt-4 pt-4 border-t border-border space-y-3">
                    {goal.description && (
                      <div>
                        <p className="text-xs font-medium text-muted-foreground mb-1">{t("professionalDev.description")}</p>
                        <p className="text-sm text-foreground">{goal.description}</p>
                      </div>
                    )}
                    {goal.evidenceUrl && (
                      <div>
                        <p className="text-xs font-medium text-muted-foreground mb-1">{t("professionalDev.evidence")}</p>
                        <a
                          href={goal.evidenceUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          aria-label={t("professionalDev.viewEvidence")}
                          className="text-sm text-primary hover:underline break-all"
                        >
                          {goal.evidenceUrl}
                        </a>
                      </div>
                    )}
                    {goal.completedDate && (
                      <div>
                        <p className="text-xs font-medium text-muted-foreground mb-1">{t("professionalDev.completedDate")}</p>
                        <p className="text-sm text-foreground">
                          {new Date(goal.completedDate).toLocaleDateString()}
                        </p>
                      </div>
                    )}
                    {goal.notes && (
                      <div>
                        <p className="text-xs font-medium text-muted-foreground mb-1">{t("professionalDev.notes")}</p>
                        <p className="text-sm text-foreground">{goal.notes}</p>
                      </div>
                    )}
                    <button
                      onClick={() => openEditForm(goal)}
                      aria-label={t("professionalDev.editGoal")}
                      className="inline-flex items-center gap-2 rounded-xl border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-muted transition"
                    >
                      <Edit className="size-4" />
                      {t("professionalDev.editGoal")}
                    </button>
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
