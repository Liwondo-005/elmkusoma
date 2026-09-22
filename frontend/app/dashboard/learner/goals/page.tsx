"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { learnerApi, type Enrollment } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import {
  Target,
  Plus,
  Trash2,
  CheckCircle,
  Clock,
  ArrowRight,
  BookOpen,
  AlertCircle,
  X,
  Pencil,
} from "lucide-react"

interface LearningGoal {
  id: string
  title: string
  description: string
  courseId: string | null
  courseName: string | null
  targetDate: string | null
  status: "active" | "completed"
  progress: number
  createdAt: string
}

function getGoals(): LearningGoal[] {
  if (typeof window === "undefined") return []
  try {
    const raw = localStorage.getItem("elmkusoma_learning_goals")
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveGoals(goals: LearningGoal[]) {
  localStorage.setItem("elmkusoma_learning_goals", JSON.stringify(goals))
}

export default function GoalsPage() {
  const { user, loading: authLoading } = useAuth()
  const [goals, setGoals] = useState<LearningGoal[]>([])
  const [enrollments, setEnrollments] = useState<Enrollment[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [editingGoal, setEditingGoal] = useState<LearningGoal | null>(null)
  const [formTitle, setFormTitle] = useState("")
  const [formDescription, setFormDescription] = useState("")
  const [formCourseId, setFormCourseId] = useState("")
  const [formTargetDate, setFormTargetDate] = useState("")

  useEffect(() => {
    if (!user || (user.role !== "Other Learner" && user.role !== "Student")) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const enrollmentsData = await learnerApi.getEnrollments().catch(() => [])
      setEnrollments(enrollmentsData)
      setGoals(getGoals())
    } catch {
    } finally {
      setLoading(false)
    }
  }

  function openCreateForm() {
    setEditingGoal(null)
    setFormTitle("")
    setFormDescription("")
    setFormCourseId("")
    setFormTargetDate("")
    setShowForm(true)
  }

  function openEditForm(goal: LearningGoal) {
    setEditingGoal(goal)
    setFormTitle(goal.title)
    setFormDescription(goal.description)
    setFormCourseId(goal.courseId || "")
    setFormTargetDate(goal.targetDate || "")
    setShowForm(true)
  }

  function handleSave() {
    if (!formTitle.trim()) return
    const enrolled = enrollments.find((e) => e.courseId === formCourseId)

    if (editingGoal) {
      const updated = goals.map((g) =>
        g.id === editingGoal.id
          ? {
              ...g,
              title: formTitle.trim(),
              description: formDescription.trim(),
              courseId: formCourseId || null,
              courseName: enrolled?.courseTitle || null,
              targetDate: formTargetDate || null,
            }
          : g
      )
      setGoals(updated)
      saveGoals(updated)
    } else {
      const newGoal: LearningGoal = {
        id: Date.now().toString(),
        title: formTitle.trim(),
        description: formDescription.trim(),
        courseId: formCourseId || null,
        courseName: enrolled?.courseTitle || null,
        targetDate: formTargetDate || null,
        status: "active",
        progress: 0,
        createdAt: new Date().toISOString(),
      }
      const updated = [newGoal, ...goals]
      setGoals(updated)
      saveGoals(updated)
    }
    setShowForm(false)
  }

  function toggleGoalStatus(goalId: string) {
    const updated = goals.map((g) =>
      g.id === goalId
        ? {
            ...g,
            status: g.status === "active" ? ("completed" as const) : ("active" as const),
            progress: g.status === "active" ? 100 : 0,
          }
        : g
    )
    setGoals(updated)
    saveGoals(updated)
  }

  function deleteGoal(goalId: string) {
    const updated = goals.filter((g) => g.id !== goalId)
    setGoals(updated)
    saveGoals(updated)
  }

  if (authLoading || (user?.role !== "Other Learner" && user?.role !== "Student")) {
    return <LoadingState />
  }

  const activeGoals = goals.filter((g) => g.status === "active")
  const completedGoals = goals.filter((g) => g.status === "completed")

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Learning Goals</h1>
          <p className="mt-1 text-sm text-muted-foreground">Set targets and track your learning progress.</p>
        </div>
        <button
          onClick={openCreateForm}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-4" />
          New Goal
        </button>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">Active Goals</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{activeGoals.length}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">Completed</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{completedGoals.length}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">Enrolled Courses</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{enrollments.length}</p>
        </div>
      </div>

      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-lg">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-foreground">
                {editingGoal ? "Edit Goal" : "New Learning Goal"}
              </h2>
              <button onClick={() => setShowForm(false)} className="rounded-lg p-1 hover:bg-muted">
                <X className="size-4" />
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-foreground">Goal Title *</label>
                <input
                  type="text"
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  placeholder="e.g. Complete Java Programming"
                  className="mt-1.5 h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-foreground">Description</label>
                <textarea
                  value={formDescription}
                  onChange={(e) => setFormDescription(e.target.value)}
                  placeholder="What do you want to achieve?"
                  rows={3}
                  className="mt-1.5 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-foreground">Related Course</label>
                <select
                  value={formCourseId}
                  onChange={(e) => setFormCourseId(e.target.value)}
                  className="mt-1.5 h-10 w-full appearance-none rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                >
                  <option value="">None</option>
                  {enrollments.map((e) => (
                    <option key={e.courseId} value={e.courseId}>
                      {e.courseTitle}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-foreground">Target Date</label>
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
                  Cancel
                </button>
                <button
                  onClick={handleSave}
                  disabled={!formTitle.trim()}
                  className="flex-1 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                >
                  {editingGoal ? "Save Changes" : "Create Goal"}
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
          title="No learning goals yet"
          description="Set a goal to stay focused on what matters most in your learning journey."
          action={
            <button
              onClick={openCreateForm}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Create Your First Goal <ArrowRight className="size-4" />
            </button>
          }
        />
      ) : (
        <div className="space-y-6">
          {activeGoals.length > 0 && (
            <div>
              <h2 className="text-sm font-semibold text-foreground mb-3">Active Goals</h2>
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
                          {goal.courseName && (
                            <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold text-primary">
                              <BookOpen className="size-3" /> {goal.courseName}
                            </span>
                          )}
                          {goal.targetDate && (
                            <span className="inline-flex items-center gap-1 text-[10px] text-muted-foreground">
                              <Clock className="size-3" /> Target: {new Date(goal.targetDate).toLocaleDateString()}
                            </span>
                          )}
                        </div>
                      </div>
                      <div className="flex items-center gap-1 shrink-0">
                        <button
                          onClick={() => openEditForm(goal)}
                          className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground"
                          title="Edit"
                        >
                          <Pencil className="size-3.5" />
                        </button>
                        <button
                          onClick={() => toggleGoalStatus(goal.id)}
                          className="rounded-lg p-1.5 text-muted-foreground hover:bg-green-500/10 hover:text-green-600"
                          title="Mark complete"
                        >
                          <CheckCircle className="size-3.5" />
                        </button>
                        <button
                          onClick={() => deleteGoal(goal.id)}
                          className="rounded-lg p-1.5 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                          title="Delete"
                        >
                          <Trash2 className="size-3.5" />
                        </button>
                      </div>
                    </div>
                    {goal.courseId && (
                      <div className="mt-3">
                        <Link
                          href={`/dashboard/learner/courses/${goal.courseId}`}
                          className="inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                        >
                          View Course <ArrowRight className="size-3" />
                        </Link>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {completedGoals.length > 0 && (
            <div>
              <h2 className="text-sm font-semibold text-foreground mb-3">Completed Goals</h2>
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
                          onClick={() => toggleGoalStatus(goal.id)}
                          className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground"
                          title="Reopen"
                        >
                          <Clock className="size-3.5" />
                        </button>
                        <button
                          onClick={() => deleteGoal(goal.id)}
                          className="rounded-lg p-1.5 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                          title="Delete"
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
