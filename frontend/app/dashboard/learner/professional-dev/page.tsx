"use client"

import { useState, useEffect, useCallback } from "react"
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

function ProgressBar({ percent }: { percent: number }) {
  return (
    <div className="w-full">
      <div className="flex items-center justify-between text-xs">
        <span className="text-muted-foreground">Progress</span>
        <span className="font-semibold text-foreground">{percent}%</span>
      </div>
      <div className="mt-1.5 h-2 w-full overflow-hidden rounded-full bg-muted">
        <div
          className="h-full rounded-full bg-primary transition-all"
          style={{ width: `${percent}%` }}
        />
      </div>
    </div>
  )
}

export default function ProfessionalDevPage() {
  const { user, loading: authLoading } = useAuth()
  const [goals, setGoals] = useState<ProfessionalDevGoal[]>([])
  const [loading, setLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState("ALL")
  const [typeFilter, setTypeFilter] = useState("ALL")
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingGoal, setEditingGoal] = useState<ProfessionalDevGoal | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

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
      // silent
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
      // silent
    }
  }

  async function handleDelete(id: string) {
    try {
      await collegeApi.deleteProfessionalDev(id)
      setDeletingId(null)
      await loadGoals()
    } catch {
      // silent
    }
  }

  async function handleProgressChange(id: string, percent: number) {
    try {
      await collegeApi.updateProfessionalDev(id, { progressPercent: percent })
      setGoals((prev) =>
        prev.map((g) => (g.id === id ? { ...g, progressPercent: percent } : g))
      )
    } catch {
      // silent
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
      // silent
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

  if (authLoading || loading) return <LoadingState />

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-start justify-between">
        <LearnerHeader
          firstName={firstName}
          subtitle="Track and manage your professional development goals."
        />
        <button
          onClick={() => {
            resetForm()
            setShowForm(true)
          }}
          className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition shrink-0"
        >
          <Plus className="size-4" />
          Add Goal
        </button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
              <Target className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">Total Goals</p>
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
              <p className="text-xs font-medium text-muted-foreground">In Progress</p>
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
              <p className="text-xs font-medium text-muted-foreground">Completed</p>
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
              <p className="text-xs font-medium text-muted-foreground">Avg Progress</p>
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
            {editingGoal ? "Edit Goal" : "New Professional Development Goal"}
          </h3>
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium text-foreground">Title *</label>
              <input
                type="text"
                value={formTitle}
                onChange={(e) => setFormTitle(e.target.value)}
                placeholder="e.g. AWS Solutions Architect Certification"
                className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="text-sm font-medium text-foreground">Goal Type</label>
                <select
                  value={formType}
                  onChange={(e) => setFormType(e.target.value)}
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
                <label className="text-sm font-medium text-foreground">Target Date</label>
                <input
                  type="date"
                  value={formTargetDate}
                  onChange={(e) => setFormTargetDate(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>
            </div>
            <div>
              <label className="text-sm font-medium text-foreground">Description</label>
              <textarea
                value={formDescription}
                onChange={(e) => setFormDescription(e.target.value)}
                placeholder="Describe your goal..."
                rows={3}
                className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="text-sm font-medium text-foreground">Category</label>
                <input
                  type="text"
                  value={formCategory}
                  onChange={(e) => setFormCategory(e.target.value)}
                  placeholder="e.g. Cloud Computing"
                  className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>
              <div>
                <label className="text-sm font-medium text-foreground">Notes</label>
                <input
                  type="text"
                  value={formNotes}
                  onChange={(e) => setFormNotes(e.target.value)}
                  placeholder="Additional notes..."
                  className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={handleSubmitForm}
                disabled={!formTitle.trim()}
                className="inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition disabled:opacity-50"
              >
                <CheckCircle className="size-4" />
                {editingGoal ? "Update Goal" : "Create Goal"}
              </button>
              <button
                onClick={resetForm}
                className="inline-flex items-center gap-2 rounded-xl border border-border px-5 py-2.5 text-sm font-medium text-foreground hover:bg-muted transition"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {filtered.length === 0 ? (
        <EmptyState
          icon={<Target className="size-8" />}
          title="No professional development goals found"
          description="Start tracking your career growth by adding your first goal."
          action={
            <button
              onClick={() => {
                resetForm()
                setShowForm(true)
              }}
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition"
            >
              <Plus className="size-4" />
              Add Goal
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
                        <span>Target: {new Date(goal.targetDate).toLocaleDateString()}</span>
                      </div>
                    )}

                    <div className="max-w-md">
                      <ProgressBar percent={goal.progressPercent || 0} />
                    </div>

                    {goal.category && (
                      <p className="mt-2 text-xs text-muted-foreground">
                        Category: <span className="font-medium text-foreground">{goal.category}</span>
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
                      className="w-24 accent-primary"
                      title={`Progress: ${goal.progressPercent || 0}%`}
                    />
                    <button
                      onClick={() => setExpandedId(isExpanded ? null : goal.id)}
                      className="rounded-lg p-2 text-muted-foreground hover:bg-muted transition"
                      title="Expand details"
                    >
                      <Edit className="size-4" />
                    </button>
                    {goal.status !== "COMPLETED" && (
                      <button
                        onClick={() => handleMarkComplete(goal.id)}
                        className="rounded-lg p-2 text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-900/20 transition"
                        title="Mark as completed"
                      >
                        <CheckCircle className="size-4" />
                      </button>
                    )}
                    {!isDeleting ? (
                      <button
                        onClick={() => setDeletingId(goal.id)}
                        className="rounded-lg p-2 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 transition"
                        title="Delete goal"
                      >
                        <Trash2 className="size-4" />
                      </button>
                    ) : (
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => handleDelete(goal.id)}
                          className="rounded-lg bg-red-500 px-2.5 py-1 text-xs font-medium text-white hover:bg-red-600 transition"
                        >
                          Confirm
                        </button>
                        <button
                          onClick={() => setDeletingId(null)}
                          className="rounded-lg border border-border px-2.5 py-1 text-xs font-medium text-foreground hover:bg-muted transition"
                        >
                          Cancel
                        </button>
                      </div>
                    )}
                  </div>
                </div>

                {isExpanded && (
                  <div className="mt-4 pt-4 border-t border-border space-y-3">
                    {goal.description && (
                      <div>
                        <p className="text-xs font-medium text-muted-foreground mb-1">Description</p>
                        <p className="text-sm text-foreground">{goal.description}</p>
                      </div>
                    )}
                    {goal.evidenceUrl && (
                      <div>
                        <p className="text-xs font-medium text-muted-foreground mb-1">Evidence</p>
                        <a
                          href={goal.evidenceUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-sm text-primary hover:underline break-all"
                        >
                          {goal.evidenceUrl}
                        </a>
                      </div>
                    )}
                    {goal.completedDate && (
                      <div>
                        <p className="text-xs font-medium text-muted-foreground mb-1">Completed Date</p>
                        <p className="text-sm text-foreground">
                          {new Date(goal.completedDate).toLocaleDateString()}
                        </p>
                      </div>
                    )}
                    {goal.notes && (
                      <div>
                        <p className="text-xs font-medium text-muted-foreground mb-1">Notes</p>
                        <p className="text-sm text-foreground">{goal.notes}</p>
                      </div>
                    )}
                    <button
                      onClick={() => openEditForm(goal)}
                      className="inline-flex items-center gap-2 rounded-xl border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-muted transition"
                    >
                      <Edit className="size-4" />
                      Edit Goal
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
