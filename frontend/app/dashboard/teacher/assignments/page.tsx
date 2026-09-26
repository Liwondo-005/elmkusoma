"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"
import {
  FileText,
  Clock,
  CheckCircle,
  Plus,
  Eye,
  X,
  AlertCircle,
  Loader2,
  ChevronDown,
  Calendar,
  Users,
  Paperclip,
  Pencil,
  Trash2,
  Award,
  Save,
} from "lucide-react"

import { appFetch } from "@/lib/fetch"

interface Assignment {
  id: string
  title: string
  description?: string
  instructions?: string
  assignmentType?: string
  dueDate?: string
  totalMarks: number
  status?: string
  attachments?: string
  classGroupId: string
  subjectId: string
  className?: string
  subjectName?: string
  submissionCount?: number
  totalStudents?: number
  createdAt: string
}

interface AssignmentSubmission {
  id: string
  assignmentId: string
  studentId: string
  studentName?: string
  fileUrl?: string
  submittedAt: string
  grade?: number
  obtainedMarks?: number
  feedback?: string
  gradedAt?: string
  createdAt: string
}

interface ClassOption {
  classGroupId: string
  className: string
  subjectId: string
  subjectName: string
}

const initialForm = {
  title: "",
  description: "",
  instructions: "",
  assignmentType: "ESSAY",
  classGroupId: "",
  subjectId: "",
  dueDate: "",
  totalMarks: 100,
  status: "DRAFT",
  attachments: "",
}

export default function TeacherAssignmentsPage() {
  const { user } = useAuth()
  const t = useTranslations("teacher")
  const tn = useTranslations("nav")
  const tc = useTranslations("common")
  const ts = useTranslations("status")
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [classes, setClasses] = useState<ClassOption[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState(initialForm)
  const [submitting, setSubmitting] = useState(false)

  const [selectedClassId, setSelectedClassId] = useState("")
  const [statusFilter, setStatusFilter] = useState("")
  const [selectedAssignment, setSelectedAssignment] = useState<string | null>(null)
  const [submissions, setSubmissions] = useState<AssignmentSubmission[]>([])
  const [loadingSubmissions, setLoadingSubmissions] = useState(false)

  const [gradingSubmissionId, setGradingSubmissionId] = useState<string | null>(null)
  const [gradeValue, setGradeValue] = useState<string>("")
  const [gradeFeedback, setGradeFeedback] = useState("")
  const [gradingLoading, setGradingLoading] = useState(false)

  const assignmentTypes = [
    { value: "ESSAY", label: t("assignments.typeEssay") },
    { value: "MULTIPLE_CHOICE", label: t("assignments.typeMcq") },
    { value: "FILL_IN_THE_BLANK", label: t("assignments.typeFillBlank") },
    { value: "MATCHING", label: t("assignments.typeMatching") },
    { value: "FILE_UPLOAD", label: t("assignments.typeFileUpload") },
  ]

  const statusOptions = [
    { value: "", label: t("assignments.filterAll") },
    { value: "DRAFT", label: t("lessons.draft") },
    { value: "PUBLISHED", label: t("lessons.published") },
  ]

  const statusConfig: Record<string, { label: string; className: string }> = {
    DRAFT: { label: t("lessons.draft"), className: "bg-gray-100 text-gray-700 dark:bg-gray-800/30 dark:text-gray-400" },
    PUBLISHED: { label: t("lessons.published"), className: "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400" },
    ARCHIVED: { label: t("assignments.archived"), className: "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400" },
  }

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const [classesData, assignmentsData] = await Promise.allSettled([
        appFetch<ClassOption[]>("/v1/teachers/me/classes"),
        loadAllAssignments(),
      ])
      if (classesData.status === "fulfilled") {
        setClasses(classesData.value)
      }
      if (assignmentsData.status === "fulfilled") {
        setAssignments(assignmentsData.value)
      }
    } catch {
      setError(tc("error.load"))
    } finally {
      setLoading(false)
    }
  }

  async function loadAllAssignments(): Promise<Assignment[]> {
    const classesRes = await appFetch<ClassOption[]>("/v1/teachers/me/classes")
    const all: Assignment[] = []
    for (const cls of classesRes) {
      try {
        const data = await appFetch<Assignment[]>(`/v1/learning/assignments/class/${cls.classGroupId}`)
        const enriched = data.map((a) => ({
          ...a,
          className: cls.className,
          subjectName: cls.subjectName,
        }))
        all.push(...enriched)
      } catch {
        // skip
      }
    }
    return all
  }

  function resetForm() {
    setForm(initialForm)
    setEditingId(null)
    setShowForm(false)
  }

  function startEdit(a: Assignment) {
    setForm({
      title: a.title,
      description: a.description || "",
      instructions: a.instructions || "",
      assignmentType: a.assignmentType || "ESSAY",
      classGroupId: a.classGroupId,
      subjectId: a.subjectId,
      // Wall-clock as stored (backend LocalDateTime = server-local time, UTC+3);
      // toISOString() would shift the edit field 3 hours back.
      dueDate: a.dueDate ? String(a.dueDate).replace(" ", "T").slice(0, 16) : "",
      totalMarks: a.totalMarks,
      status: a.status || "DRAFT",
      attachments: a.attachments || "",
    })
    setEditingId(a.id)
    setShowForm(true)
  }

  async function handleSubmit() {
    if (!form.title.trim() || !form.classGroupId || !form.subjectId) {
      setError(t("assignments.requiredError"))
      return
    }
    try {
      setSubmitting(true)
      setError(null)
      const payload = {
        title: form.title.trim(),
        description: form.description.trim(),
        instructions: form.instructions.trim(),
        assignmentType: form.assignmentType,
        classGroupId: form.classGroupId,
        subjectId: form.subjectId,
        // Wall-clock as picked — backend stores LocalDateTime in server-local time
        // (UTC+3); toISOString() would store the due date 3 hours too early.
        dueDate: form.dueDate ? (form.dueDate.length === 16 ? `${form.dueDate}:00` : form.dueDate.slice(0, 19)) : undefined,
        totalMarks: Number(form.totalMarks) || 100,
        status: form.status,
        attachments: form.attachments.trim(),
      }

      if (editingId) {
        await appFetch(`/v1/learning/assignments/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        })
        setSuccess(t("assignments.updatedSuccess"))
      } else {
        await appFetch("/v1/learning/assignments", {
          method: "POST",
          body: JSON.stringify(payload),
        })
        setSuccess(t("assignments.createdSuccess"))
      }
      resetForm()
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("assignments.saveError"))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(id: string) {
    if (!confirm(t("assignments.deleteConfirm"))) return
    try {
      setError(null)
      await appFetch(`/v1/learning/assignments/${id}`, { method: "DELETE" })
      setSuccess(t("assignments.deletedSuccess"))
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("assignments.deleteError"))
    }
  }

  async function viewSubmissions(assignmentId: string) {
    try {
      setLoadingSubmissions(true)
      setSelectedAssignment(assignmentId)
      const data = await appFetch<AssignmentSubmission[]>(`/v1/learning/assignments/${assignmentId}/submissions`)
      setSubmissions(data)
    } catch {
      setSubmissions([])
    } finally {
      setLoadingSubmissions(false)
    }
  }

  function startGrading(submission: AssignmentSubmission, assignmentTotalMarks: number) {
    setGradingSubmissionId(submission.id)
    setGradeValue(submission.obtainedMarks?.toString() ?? submission.grade?.toString() ?? "")
    setGradeFeedback(submission.feedback ?? "")
  }

  function cancelGrading() {
    setGradingSubmissionId(null)
    setGradeValue("")
    setGradeFeedback("")
  }

  async function submitGrade(submissionId: string, totalMarks: number) {
    const grade = Number(gradeValue)
    if (isNaN(grade) || grade < 0 || grade > totalMarks) {
      setError(t("assignments.gradeRangeError", { max: totalMarks }))
      return
    }
    try {
      setGradingLoading(true)
      setError(null)
      const params = new URLSearchParams({ grade: String(grade) })
      if (gradeFeedback.trim()) {
        params.set("feedback", gradeFeedback.trim())
      }
      await appFetch(`/v1/learning/submissions/${submissionId}/grade?${params.toString()}`, {
        method: "PUT",
      })
      setSuccess(t("assignments.gradedSuccess"))
      cancelGrading()
      if (selectedAssignment) {
        await viewSubmissions(selectedAssignment)
      }
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("assignments.gradeError"))
    } finally {
      setGradingLoading(false)
    }
  }

  function isOverdue(dueDate?: string) {
    if (!dueDate) return false
    return new Date(dueDate) < new Date()
  }

  const filtered = assignments.filter((a) => {
    if (selectedClassId && a.classGroupId !== selectedClassId) return false
    if (statusFilter && a.status !== statusFilter) return false
    return true
  })

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("assignments")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("assignments.subtitle")}</p>
        </div>
        <Button
          className="gap-2"
          onClick={() => {
            resetForm()
            setShowForm(!showForm)
          }}
        >
          {showForm ? <X className="size-4" /> : <Plus className="size-4" />}
          {showForm ? tc("cancel") : t("assignments.createAssignment")}
        </Button>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4 shrink-0" />
            {error}
          </div>
        </div>
      )}

      {success && (
        <div className="rounded-2xl border border-green-500/20 bg-green-500/5 p-4">
          <div className="flex items-center gap-2 text-sm text-green-600">
            <CheckCircle className="size-4 shrink-0" />
            {success}
          </div>
        </div>
      )}

      {showForm && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <h2 className="text-base font-semibold text-foreground">
            {editingId ? t("assignments.editTitle") : t("assignments.newTitle")}
          </h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("announcements.titleLabel")}</label>
              <input
                type="text"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder={t("assignments.titlePlaceholder")}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("lessons.descLabel")}</label>
              <textarea
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder={t("assignments.descPlaceholder")}
                rows={2}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
              />
            </div>
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("assignments.instructionsLabel")}</label>
              <textarea
                value={form.instructions}
                onChange={(e) => setForm({ ...form, instructions: e.target.value })}
                placeholder={t("assignments.instructionsPlaceholder")}
                rows={4}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("assessments.typeLabel")}</label>
              <div className="relative">
                <select
                  value={form.assignmentType}
                  onChange={(e) => setForm({ ...form, assignmentType: e.target.value })}
                  className="h-10 w-full appearance-none rounded-lg border border-border bg-background px-3 pr-10 text-sm outline-none focus:border-ring"
                >
                  {assignmentTypes.map((at) => (
                    <option key={at.value} value={at.value}>{at.label}</option>
                  ))}
                </select>
                <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              </div>
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("lessons.classLabel")}</label>
              <div className="relative">
                <select
                  value={form.classGroupId}
                  onChange={(e) => {
                    const cls = classes.find((c) => c.classGroupId === e.target.value)
                    setForm({
                      ...form,
                      classGroupId: e.target.value,
                      subjectId: cls?.subjectId || "",
                    })
                  }}
                  className="h-10 w-full appearance-none rounded-lg border border-border bg-background px-3 pr-10 text-sm outline-none focus:border-ring"
                >
                  <option value="">{t("lessons.selectClassOption")}</option>
                  {classes.map((c) => (
                    <option key={c.classGroupId} value={c.classGroupId}>
                      {c.className} - {c.subjectName}
                    </option>
                  ))}
                </select>
                <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              </div>
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("assignments.dueDateLabel")}</label>
              <input
                type="datetime-local"
                value={form.dueDate}
                onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("assessments.totalMarks")}</label>
              <input
                type="number"
                value={form.totalMarks}
                onChange={(e) => setForm({ ...form, totalMarks: Number(e.target.value) })}
                min={1}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("grading.colStatus")}</label>
              <div className="relative">
                <select
                  value={form.status}
                  onChange={(e) => setForm({ ...form, status: e.target.value })}
                  className="h-10 w-full appearance-none rounded-lg border border-border bg-background px-3 pr-10 text-sm outline-none focus:border-ring"
                >
                  <option value="DRAFT">{t("lessons.draft")}</option>
                  <option value="PUBLISHED">{t("lessons.published")}</option>
                </select>
                <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              </div>
            </div>
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("assignments.attachmentsLabel")}</label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={form.attachments}
                  onChange={(e) => setForm({ ...form, attachments: e.target.value })}
                  placeholder="e.g. assignment-file.pdf"
                  className="h-10 flex-1 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                />
                <Button type="button" variant="outline" size="icon" className="shrink-0" title={t("assignments.attachFile")}>
                  <Paperclip className="size-4" />
                </Button>
              </div>
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={resetForm}>{tc("cancel")}</Button>
            <Button
              onClick={handleSubmit}
              disabled={submitting || !form.title.trim() || !form.classGroupId || !form.subjectId}
            >
              {submitting ? tc("saving") : editingId ? t("announcements.update") : tc("create")}
            </Button>
          </div>
        </div>
      )}

      {selectedAssignment && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">{t("assignments.submissionsTitle")}</h2>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                setSelectedAssignment(null)
                setSubmissions([])
              }}
            >
              {tc("close")}
            </Button>
          </div>
          {loadingSubmissions ? (
            <div className="flex justify-center py-8">
              <Loader2 className="size-6 animate-spin text-muted-foreground" />
            </div>
          ) : submissions.length === 0 ? (
            <p className="py-4 text-center text-sm text-muted-foreground">{t("assignments.noSubmissions")}</p>
          ) : (
            <div className="mt-4 overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    <th className="px-3 py-2">{t("gradebook.colStudent")}</th>
                    <th className="px-3 py-2">{t("assignments.colSubmitted")}</th>
                    <th className="px-3 py-2">{t("assessments.marksLabel")}</th>
                    <th className="px-3 py-2">{t("grading.colStatus")}</th>
                    <th className="px-3 py-2">{t("assignments.colActions")}</th>
                  </tr>
                </thead>
                <tbody>
                  {submissions.map((s) => {
                    const isGraded = s.grade !== null && s.grade !== undefined
                    const isGrading = gradingSubmissionId === s.id
                    const currentAssignment = filtered.find((a) => a.id === selectedAssignment)
                    const totalMarks = currentAssignment?.totalMarks ?? 100
                    return (
                      <tr key={s.id} className="border-b border-border last:border-0">
                        <td className="px-3 py-2 font-medium text-foreground">
                          {s.studentName || s.studentId.slice(0, 8)}...
                        </td>
                        <td className="px-3 py-2 text-muted-foreground">
                          {new Date(s.submittedAt).toLocaleDateString()}
                        </td>
                        <td className="px-3 py-2 text-muted-foreground">
                          {s.obtainedMarks ?? s.grade ?? "—"}
                        </td>
                        <td className="px-3 py-2">
                          <span
                            className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                              isGraded
                                ? "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
                                : "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400"
                            }`}
                          >
                            {isGraded ? t("assignments.gradedBadge") : ts("pending")}
                          </span>
                        </td>
                        <td className="px-3 py-2">
                          {isGrading ? (
                            <div className="flex flex-col gap-2 min-w-[200px]">
                              <div className="flex items-center gap-2">
                                <label className="text-xs text-muted-foreground shrink-0">{t("assignments.gradeLabel", { max: totalMarks })}</label>
                                <input
                                  type="number"
                                  min={0}
                                  max={totalMarks}
                                  value={gradeValue}
                                  onChange={(e) => setGradeValue(e.target.value)}
                                  className="h-7 w-20 rounded-lg border border-border bg-background px-2 text-xs outline-none focus:border-ring"
                                  autoFocus
                                />
                              </div>
                              <textarea
                                value={gradeFeedback}
                                onChange={(e) => setGradeFeedback(e.target.value)}
                                placeholder={t("assignments.feedbackPlaceholder")}
                                rows={2}
                                className="w-full rounded-lg border border-border bg-background px-2 py-1 text-xs outline-none focus:border-ring resize-none"
                              />
                              <div className="flex gap-1">
                                <Button
                                  size="sm"
                                  onClick={() => submitGrade(s.id, totalMarks)}
                                  disabled={gradingLoading || gradeValue === ""}
                                  className="h-7 gap-1 text-xs"
                                >
                                  {gradingLoading ? (
                                    <Loader2 className="size-3 animate-spin" />
                                  ) : (
                                    <Save className="size-3" />
                                  )}
                                  {tc("save")}
                                </Button>
                                <Button
                                  size="sm"
                                  variant="ghost"
                                  onClick={cancelGrading}
                                  disabled={gradingLoading}
                                  className="h-7 text-xs"
                                >
                                  {tc("cancel")}
                                </Button>
                              </div>
                            </div>
                          ) : (
                            !isGraded && (
                              <Button
                                size="sm"
                                variant="outline"
                                onClick={() => startGrading(s, totalMarks)}
                                className="gap-1 h-7 text-xs"
                              >
                                <Award className="size-3" />
                                {t("assignments.gradeBtn")}
                              </Button>
                            )
                          )}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      <div className="flex flex-wrap gap-3">
        {classes.length > 0 && (
          <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
            <label className="text-sm font-medium text-foreground">{t("assignments.filterClass")}</label>
            <div className="relative mt-1">
              <select
                value={selectedClassId}
                onChange={(e) => {
                  setSelectedClassId(e.target.value)
                  setSelectedAssignment(null)
                  setSubmissions([])
                }}
                className="w-full appearance-none rounded-lg border border-border bg-background px-3 py-2.5 pr-10 text-sm text-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              >
                <option value="">{t("assignments.allClasses")}</option>
                {classes.map((c) => (
                  <option key={c.classGroupId} value={c.classGroupId}>
                    {c.className} - {c.subjectName}
                  </option>
                ))}
              </select>
              <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            </div>
          </div>
        )}
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <label className="text-sm font-medium text-foreground">{t("grading.colStatus")}</label>
          <div className="relative mt-1">
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value)
                setSelectedAssignment(null)
                setSubmissions([])
              }}
              className="w-full appearance-none rounded-lg border border-border bg-background px-3 py-2.5 pr-10 text-sm text-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            >
              {statusOptions.map((s) => (
                <option key={s.value} value={s.value}>
                  {s.label}
                </option>
              ))}
            </select>
            <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          </div>
        </div>
      </div>

      {filtered.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <FileText className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("assignments.emptyTitle")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">{t("assignments.emptyDesc")}</p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((a) => {
            const overdue = isOverdue(a.dueDate)
            const status = a.status ? statusConfig[a.status] : null
            return (
              <div key={a.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:shadow-sm">
                <div className="flex items-start gap-4">
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 shrink-0">
                    <FileText className="size-5 text-primary" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <h3 className="text-sm font-semibold text-foreground">{a.title}</h3>
                      {status && (
                        <span className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-semibold ${status.className}`}>
                          {status.label}
                        </span>
                      )}
                      {a.assignmentType && (
                        <span className="inline-flex rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                          {assignmentTypes.find((at) => at.value === a.assignmentType)?.label || a.assignmentType}
                        </span>
                      )}
                    </div>
                    {a.description && (
                      <p className="mt-1 text-xs text-muted-foreground line-clamp-1">{a.description}</p>
                    )}
                    {a.instructions && (
                      <p className="mt-0.5 text-xs text-muted-foreground/70 line-clamp-1 italic">
                        {t("assignments.instructionsPrefix", { text: a.instructions })}
                      </p>
                    )}
                    <div className="mt-2 flex items-center gap-3 text-xs text-muted-foreground flex-wrap">
                      {a.className && a.subjectName && (
                        <span className="flex items-center gap-1">
                          <Users className="size-3" />
                          {a.className} · {a.subjectName}
                        </span>
                      )}
                      <span className="flex items-center gap-1">
                        <Calendar className="size-3" />
                        {t("assignments.totalMarksLabel", { marks: a.totalMarks })}
                      </span>
                      {a.dueDate && (
                        <span className={`flex items-center gap-1 ${overdue ? "text-destructive" : ""}`}>
                          <Clock className="size-3" />
                          {t("classDetail.dueLabel", { date: new Date(a.dueDate).toLocaleDateString() })}
                        </span>
                      )}
                      {(a.submissionCount !== undefined || a.totalStudents !== undefined) && (
                        <span className="flex items-center gap-1">
                          <Users className="size-3" />
                          {t("assignments.submittedCount", { sub: a.submissionCount ?? 0, total: a.totalStudents ?? 0 })}
                        </span>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => viewSubmissions(a.id)}
                      className="gap-1"
                    >
                      <Eye className="size-3" /> {t("assignments.submissionsTitle")}
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => startEdit(a)}
                      title={tc("edit")}
                    >
                      <Pencil className="size-3.5" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => handleDelete(a.id)}
                      title={tc("delete")}
                      className="text-destructive hover:text-destructive"
                    >
                      <Trash2 className="size-3.5" />
                    </Button>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
