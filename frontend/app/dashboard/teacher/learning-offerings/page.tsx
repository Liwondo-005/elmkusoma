"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"
import { Compass, Plus, Pencil, Trash2, Loader2, AlertCircle, ChevronDown } from "lucide-react"
import { teacherFetch, teacherApi, type LearningOffering, type LearningOfferingInput } from "@/lib/teacher-api"

interface SubjectOption {
  id: string
  name: string
}

interface CourseOption {
  id: string
  title: string
}

const EDUCATION_LEVELS = ["NURSERY", "PRIMARY", "SECONDARY", "COLLEGE", "VETA", "UNIVERSITY"]

const initialForm = {
  title: "",
  description: "",
  educationLevel: "",
  subjectId: "",
  courseId: "",
  visibility: "INSTITUTION",
  status: "DRAFT",
  independent: false,
}

export default function TeacherLearningOfferingsPage() {
  const { user } = useAuth()
  const t = useTranslations("teacher")
  const tn = useTranslations("nav")
  const tc = useTranslations("common")
  const [offerings, setOfferings] = useState<LearningOffering[]>([])
  const [subjects, setSubjects] = useState<SubjectOption[]>([])
  const [courses, setCourses] = useState<CourseOption[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState(initialForm)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  useEffect(() => {
    if (!user || !showForm) return
    let cancelled = false
    Promise.all([
      teacherFetch<SubjectOption[]>("/v1/academic/subjects").catch(() => []),
      teacherFetch<CourseOption[]>("/v1/courses").catch(() => []),
    ])
      .then(([subjectData, courseData]) => {
        if (cancelled) return
        setSubjects(Array.isArray(subjectData) ? subjectData : [])
        setCourses(Array.isArray(courseData) ? courseData : [])
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [user, showForm])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const data = await teacherApi.getOfferings()
      setOfferings(data)
    } catch {
      setError(t("offerings.loadError"))
    } finally {
      setLoading(false)
    }
  }

  function resetForm() {
    setForm(initialForm)
    setEditingId(null)
    setShowForm(false)
  }

  function startEdit(offering: LearningOffering) {
    setForm({
      title: offering.title,
      description: offering.description ?? "",
      educationLevel: offering.educationLevel ?? "",
      subjectId: offering.subjectId ?? "",
      courseId: offering.courseId ?? "",
      visibility: offering.visibility,
      status: offering.status,
      independent: offering.institutionId == null,
    })
    setEditingId(offering.id)
    setShowForm(true)
  }

  async function handleSubmit() {
    if (!form.title.trim()) {
      setError(t("offerings.requiredError"))
      return
    }
    try {
      setSubmitting(true)
      setError(null)
      const payload: LearningOfferingInput = {
        title: form.title.trim(),
        description: form.description.trim() || undefined,
        educationLevel: form.educationLevel || null,
        subjectId: form.subjectId || null,
        courseId: form.courseId || null,
        visibility: form.visibility,
        status: form.status,
        independent: form.independent,
      }
      if (editingId) {
        await teacherApi.updateOffering(editingId, payload)
        setSuccess(t("offerings.updatedSuccess"))
      } else {
        await teacherApi.createOffering(payload)
        setSuccess(t("offerings.createdSuccess"))
      }
      resetForm()
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("offerings.saveError"))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(id: string) {
    if (!confirm(t("offerings.deleteConfirm"))) return
    try {
      setError(null)
      await teacherApi.deleteOffering(id)
      setSuccess(t("offerings.deletedSuccess"))
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("offerings.deleteError"))
    }
  }

  function formatDate(iso: string) {
    if (!iso) return "—"
    return new Date(iso).toLocaleString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
    })
  }

  function visibilityLabel(visibility: string) {
    if (visibility === "PUBLIC") return t("offerings.visibilityPublic")
    if (visibility === "PRIVATE") return t("offerings.visibilityPrivate")
    return t("offerings.visibilityInstitution")
  }

  const sorted = [...offerings].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())

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
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("learningOfferings")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("offerings.subtitle")}</p>
        </div>
        <Button
          className="gap-2"
          onClick={() => {
            resetForm()
            setShowForm(!showForm)
          }}
        >
          {showForm ? <Trash2 className="size-4" /> : <Plus className="size-4" />}
          {showForm ? tc("cancel") : t("offerings.newOffering")}
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
            <Compass className="size-4 shrink-0" />
            {success}
          </div>
        </div>
      )}

      {showForm && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <h2 className="text-base font-semibold text-foreground">
            {editingId ? t("offerings.editTitle") : t("offerings.newOffering")}
          </h2>
          <div className="grid gap-4">
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("offerings.titleLabel")}</label>
              <input
                type="text"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder={t("offerings.titlePlaceholder")}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("offerings.descriptionLabel")}</label>
              <textarea
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder={t("offerings.descriptionPlaceholder")}
                rows={4}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("offerings.levelLabel")}</label>
                <div className="relative">
                  <select
                    value={form.educationLevel}
                    onChange={(e) => setForm({ ...form, educationLevel: e.target.value })}
                    className="h-10 w-full appearance-none rounded-lg border border-border bg-background px-3 pr-10 text-sm outline-none focus:border-ring"
                  >
                    <option value="">{t("offerings.generalLevel")}</option>
                    {EDUCATION_LEVELS.map((level) => (
                      <option key={level} value={level}>{level}</option>
                    ))}
                  </select>
                  <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                </div>
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("offerings.subjectLabel")}</label>
                <div className="relative">
                  <select
                    value={form.subjectId}
                    onChange={(e) => setForm({ ...form, subjectId: e.target.value })}
                    className="h-10 w-full appearance-none rounded-lg border border-border bg-background px-3 pr-10 text-sm outline-none focus:border-ring"
                  >
                    <option value="">
                      {subjects.length === 0 ? t("offerings.noSubjects") : "—"}
                    </option>
                    {subjects.map((subject) => (
                      <option key={subject.id} value={subject.id}>{subject.name}</option>
                    ))}
                  </select>
                  <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                </div>
              </div>
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("offerings.courseLabel")}</label>
              <div className="relative">
                <select
                  value={form.courseId}
                  onChange={(e) => setForm({ ...form, courseId: e.target.value })}
                  className="h-10 w-full appearance-none rounded-lg border border-border bg-background px-3 pr-10 text-sm outline-none focus:border-ring"
                >
                  <option value="">
                    {courses.length === 0 ? t("offerings.noCourses") : "—"}
                  </option>
                  {courses.map((course) => (
                    <option key={course.id} value={course.id}>{course.title}</option>
                  ))}
                </select>
                <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              </div>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("offerings.visibilityLabel")}</label>
                <div className="relative">
                  <select
                    value={form.visibility}
                    onChange={(e) => setForm({ ...form, visibility: e.target.value })}
                    className="h-10 w-full appearance-none rounded-lg border border-border bg-background px-3 pr-10 text-sm outline-none focus:border-ring"
                  >
                    <option value="INSTITUTION">{t("offerings.visibilityInstitution")}</option>
                    <option value="PUBLIC">{t("offerings.visibilityPublic")}</option>
                    <option value="PRIVATE">{t("offerings.visibilityPrivate")}</option>
                  </select>
                  <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                </div>
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("offerings.statusLabel")}</label>
                <div className="relative">
                  <select
                    value={form.status}
                    onChange={(e) => setForm({ ...form, status: e.target.value })}
                    className="h-10 w-full appearance-none rounded-lg border border-border bg-background px-3 pr-10 text-sm outline-none focus:border-ring"
                  >
                    <option value="DRAFT">{t("offerings.statusDraft")}</option>
                    <option value="PUBLISHED">{t("offerings.statusPublished")}</option>
                  </select>
                  <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                </div>
              </div>
            </div>
            <label className="flex items-start gap-2 text-sm text-foreground">
              <input
                type="checkbox"
                checked={form.independent}
                onChange={(e) => setForm({ ...form, independent: e.target.checked })}
                className="mt-0.5 size-4 rounded border-border"
              />
              <span>
                {t("offerings.independentLabel")}
                <span className="mt-0.5 block text-xs text-muted-foreground">{t("offerings.independentHint")}</span>
              </span>
            </label>
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={resetForm}>{tc("cancel")}</Button>
            <Button onClick={handleSubmit} disabled={submitting || !form.title.trim()}>
              {submitting ? tc("saving") : editingId ? t("offerings.update") : t("offerings.create")}
            </Button>
          </div>
        </div>
      )}

      {sorted.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border bg-card p-12 text-center">
          <Compass className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("offerings.emptyTitle")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">{t("offerings.emptyDesc")}</p>
        </div>
      ) : (
        <div className="space-y-3">
          {sorted.map((offering) => {
            const published = offering.status === "PUBLISHED"
            return (
              <div key={offering.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:shadow-sm">
                <div className="flex items-start gap-4">
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 shrink-0">
                    <Compass className="size-5 text-primary" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <h3 className="text-sm font-semibold text-foreground">{offering.title}</h3>
                      <span
                        className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                          published
                            ? "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
                            : "bg-gray-100 text-gray-700 dark:bg-gray-800/30 dark:text-gray-400"
                        }`}
                      >
                        {published ? t("offerings.badgePublished") : t("offerings.badgeDraft")}
                      </span>
                      <span className="inline-flex rounded-full bg-blue-100 px-2 py-0.5 text-[10px] font-semibold text-blue-700 dark:bg-blue-900/30 dark:text-blue-400">
                        {visibilityLabel(offering.visibility)}
                      </span>
                      {offering.educationLevel && (
                        <span className="inline-flex rounded-full bg-purple-100 px-2 py-0.5 text-[10px] font-semibold text-purple-700 dark:bg-purple-900/30 dark:text-purple-400">
                          {offering.educationLevel}
                        </span>
                      )}
                      {offering.subjectName && (
                        <span className="inline-flex rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-semibold text-amber-700 dark:bg-amber-900/30 dark:text-amber-400">
                          {offering.subjectName}
                        </span>
                      )}
                    </div>
                    {offering.description && (
                      <p className="mt-1.5 text-xs text-muted-foreground line-clamp-3">{offering.description}</p>
                    )}
                    <p className="mt-2 text-xs text-muted-foreground">{formatDate(offering.createdAt)}</p>
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => startEdit(offering)}
                      title={tc("edit")}
                    >
                      <Pencil className="size-3.5" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => handleDelete(offering.id)}
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
