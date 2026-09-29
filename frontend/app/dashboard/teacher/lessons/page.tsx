"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"
import { BookOpen, Plus, Pencil, Trash2, Loader2, AlertCircle, ChevronDown, Eye, EyeOff, GripVertical, X, Video, Link2, CalendarClock } from "lucide-react"
import { teacherFetch } from "@/lib/teacher-api"

interface Lesson {
  id: string
  subjectId: string
  classGroupId: string
  title: string
  description: string | null
  contentText: string | null
  videoUrl: string | null
  fileAttachments: string | null
  sortOrder: number
  isPublished: boolean
  createdAt: string
}

interface LiveClassLite {
  id: string
  title: string
  status: string
  scheduledAt: string | null
  durationMinutes: number
  classGroupId: string | null
  lessonId: string | null
  lessonTitle: string | null
  recordingUrl: string | null
}

interface ClassOption {
  classGroupId: string
  className: string
  subjectName: string
  subjectId: string
}

const initialForm = {
  title: "",
  description: "",
  contentText: "",
  videoUrl: "",
  subjectId: "",
  classGroupId: "",
  sortOrder: 0,
  isPublished: false,
}

export default function TeacherLessonsPage() {
  const { user } = useAuth()
  const t = useTranslations("teacher")
  const tn = useTranslations("nav")
  const tc = useTranslations("common")
  const ts = useTranslations("status")
  const [lessons, setLessons] = useState<Lesson[]>([])
  const [classes, setClasses] = useState<ClassOption[]>([])
  const [selectedClassId, setSelectedClassId] = useState<string>("")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingLesson, setEditingLesson] = useState<Lesson | null>(null)
  const [form, setForm] = useState(initialForm)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState<string | null>(null)
  // Lesson ↔ Live Class
  const [liveClasses, setLiveClasses] = useState<LiveClassLite[]>([])
  const [liveBusy, setLiveBusy] = useState(false)
  const [showCreateLive, setShowCreateLive] = useState(false)
  const [showLinkPicker, setShowLinkPicker] = useState(false)
  const [linkTargetId, setLinkTargetId] = useState("")
  const [liveForm, setLiveForm] = useState({ title: "", scheduledAt: "", durationMinutes: 60 })

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const classesRes = await teacherFetch<{ classGroupId: string; className: string; subjectName: string; subjectId: string }[]>("/v1/teachers/me/classes").catch(() => [])
      const classOptions: ClassOption[] = classesRes.map((c) => ({
        classGroupId: c.classGroupId,
        className: c.className,
        subjectName: c.subjectName,
        subjectId: c.subjectId || "",
      }))
      const unique = classOptions.filter((c, i, arr) => arr.findIndex((x) => x.classGroupId === c.classGroupId) === i)
      setClasses(unique)
      teacherFetch<LiveClassLite[]>("/v1/teachers/me/live-classes")
        .then((data) => setLiveClasses(Array.isArray(data) ? data : []))
        .catch(() => setLiveClasses([]))
      await loadLessons()
    } catch {
      setError(tc("error.load"))
    } finally {
      setLoading(false)
    }
  }

  async function loadLessons() {
    try {
      if (selectedClassId) {
        const data = await teacherFetch<Lesson[]>(`/v1/learning/lessons/class/${selectedClassId}`)
        setLessons(data)
      }
    } catch {
      setLessons([])
    }
  }

  useEffect(() => {
    if (selectedClassId) loadLessons()
  }, [selectedClassId])

  async function handleSave() {
    if (!form.title || !form.classGroupId) return
    setSaving(true)
    try {
      const body = {
        title: form.title,
        description: form.description || undefined,
        contentText: form.contentText || undefined,
        videoUrl: form.videoUrl || undefined,
        subjectId: form.subjectId || classes.find((c) => c.classGroupId === form.classGroupId)?.subjectId || "",
        classGroupId: form.classGroupId,
        sortOrder: form.sortOrder,
        isPublished: form.isPublished,
      }
      if (editingLesson) {
        await teacherFetch(`/v1/learning/lessons/${editingLesson.id}`, { method: "PUT", body: JSON.stringify(body) })
      } else {
        await teacherFetch("/v1/learning/lessons", { method: "POST", body: JSON.stringify(body) })
      }
      setShowForm(false)
      setEditingLesson(null)
      setForm(initialForm)
      await loadLessons()
    } catch {
      setError(t("lessons.saveError"))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: string) {
    if (!confirm(t("lessons.deleteConfirm"))) return
    setDeleting(id)
    try {
      await teacherFetch(`/v1/learning/lessons/${id}`, { method: "DELETE" })
      await loadLessons()
    } catch {
      setError(t("lessons.deleteError"))
    } finally {
      setDeleting(null)
    }
  }

  async function togglePublish(lesson: Lesson) {
    try {
      await teacherFetch(`/v1/learning/lessons/${lesson.id}/${lesson.isPublished ? "unpublish" : "publish"}`, {
        method: "POST",
      })
      await loadLessons()
    } catch {
      setError(t("lessons.updateError"))
    }
  }

  /** The live class linked to a lesson: active link preferred over terminal ones. */
  function liveForLesson(lessonId: string): LiveClassLite | null {
    const matches = liveClasses.filter((lc) => lc.lessonId === lessonId)
    if (matches.length === 0) return null
    const active = matches.find((lc) => !["CANCELLED", "COMPLETED", "ENDED"].includes(lc.status))
    return active || matches[0]
  }

  function liveStatusMeta(status: string): { label: string; className: string } {
    switch (status) {
      case "SCHEDULED": return { label: ts("scheduled"), className: "bg-blue-100 text-blue-700" }
      case "STARTING": return { label: t("liveClassDetail.statusStarting"), className: "bg-amber-100 text-amber-700" }
      case "IN_PROGRESS":
      case "LIVE": return { label: t("liveClassDetail.statusLive"), className: "bg-green-100 text-green-700" }
      case "ENDING": return { label: t("liveClassDetail.statusEnding"), className: "bg-amber-100 text-amber-700" }
      case "COMPLETED": return { label: ts("completed"), className: "bg-gray-100 text-gray-700" }
      case "ENDED": return { label: t("liveClassDetail.statusEnded"), className: "bg-gray-100 text-gray-700" }
      case "CANCELLED": return { label: ts("cancelled"), className: "bg-red-100 text-red-700" }
      default: return { label: status, className: "bg-gray-100 text-gray-700" }
    }
  }

  function formatLiveDate(iso: string | null) {
    if (!iso) return ""
    return new Date(String(iso).replace(" ", "T")).toLocaleString(undefined, {
      weekday: "short", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit",
    })
  }

  async function refreshLiveClasses() {
    try {
      const data = await teacherFetch<LiveClassLite[]>("/v1/teachers/me/live-classes")
      setLiveClasses(Array.isArray(data) ? data : [])
    } catch {
      /* keep the previous list — non-fatal */
    }
  }

  async function handleCreateLiveForLesson() {
    if (!editingLesson || !liveForm.title || !liveForm.scheduledAt) return
    setLiveBusy(true)
    try {
      const scheduledAt = liveForm.scheduledAt.length === 16 ? `${liveForm.scheduledAt}:00` : liveForm.scheduledAt
      await teacherFetch("/v1/teachers/me/live-classes", {
        method: "POST",
        body: JSON.stringify({
          title: liveForm.title.trim(),
          scheduledAt,
          durationMinutes: Number(liveForm.durationMinutes) || 60,
          classGroupId: editingLesson.classGroupId || undefined,
          subjectId: editingLesson.subjectId || undefined,
          lessonId: editingLesson.id,
        }),
      })
      setShowCreateLive(false)
      setLiveForm({ title: "", scheduledAt: "", durationMinutes: 60 })
      await refreshLiveClasses()
    } catch (err) {
      setError(err instanceof Error ? err.message : t("lessons.liveCreateError"))
    } finally {
      setLiveBusy(false)
    }
  }

  async function handleLinkLive() {
    if (!editingLesson || !linkTargetId) return
    setLiveBusy(true)
    try {
      const previous = liveForLesson(editingLesson.id)
      if (previous && previous.id !== linkTargetId) {
        await teacherFetch(`/v1/teachers/me/live-classes/${previous.id}/lesson`, { method: "DELETE" })
      }
      await teacherFetch(`/v1/teachers/me/live-classes/${linkTargetId}/lesson`, {
        method: "PUT",
        body: JSON.stringify({ lessonId: editingLesson.id }),
      })
      setShowLinkPicker(false)
      setLinkTargetId("")
      await refreshLiveClasses()
    } catch (err) {
      setError(err instanceof Error ? err.message : t("lessons.liveLinkError"))
    } finally {
      setLiveBusy(false)
    }
  }

  async function handleUnlinkLive(liveId: string) {
    if (!confirm(t("lessons.liveUnlinkConfirm"))) return
    setLiveBusy(true)
    try {
      await teacherFetch(`/v1/teachers/me/live-classes/${liveId}/lesson`, { method: "DELETE" })
      await refreshLiveClasses()
    } catch (err) {
      setError(err instanceof Error ? err.message : t("lessons.liveUnlinkError"))
    } finally {
      setLiveBusy(false)
    }
  }

  async function handleStartLiveFromLesson(liveId: string) {
    setLiveBusy(true)
    try {
      await teacherFetch(`/v1/teachers/me/live-classes/${liveId}/start`, { method: "POST" })
      await refreshLiveClasses()
    } catch (err) {
      setError(err instanceof Error ? err.message : t("lessons.liveStartError"))
    } finally {
      setLiveBusy(false)
    }
  }

  function openEdit(lesson: Lesson) {
    setEditingLesson(lesson)
    setShowCreateLive(false)
    setShowLinkPicker(false)
    setLinkTargetId("")
    setForm({
      title: lesson.title,
      description: lesson.description || "",
      contentText: lesson.contentText || "",
      videoUrl: lesson.videoUrl || "",
      subjectId: lesson.subjectId,
      classGroupId: lesson.classGroupId,
      sortOrder: lesson.sortOrder,
      isPublished: lesson.isPublished,
    })
    setShowForm(true)
  }

  function openCreate() {
    setEditingLesson(null)
    setShowCreateLive(false)
    setShowLinkPicker(false)
    setLinkTargetId("")
    setForm({ ...initialForm, classGroupId: selectedClassId, subjectId: classes.find((c) => c.classGroupId === selectedClassId)?.subjectId || "" })
    setShowForm(true)
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  const linkedLive = editingLesson ? liveForLesson(editingLesson.id) : null
  const linkCandidates = editingLesson
    ? liveClasses.filter(
        (lc) =>
          lc.status !== "CANCELLED" &&
          lc.id !== linkedLive?.id &&
          (!lc.lessonId || lc.lessonId === editingLesson.id) &&
          (!lc.classGroupId || lc.classGroupId === editingLesson.classGroupId)
      )
    : []

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("lessons")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("lessons.subtitle")}</p>
        </div>
        {selectedClassId && (
          <Button onClick={openCreate} className="gap-2">
            <Plus className="size-4" /> {t("lessons.createLesson")}
          </Button>
        )}
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          <AlertCircle className="size-4 shrink-0" />{error}
          <button onClick={() => setError(null)} className="ml-auto"><X className="size-4" /></button>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
        <label className="text-sm font-medium text-foreground">{t("lessons.selectClass")}</label>
        <div className="relative mt-1">
          <select
            value={selectedClassId}
            onChange={(e) => { setSelectedClassId(e.target.value); setLessons([]) }}
            className="w-full appearance-none rounded-lg border border-border bg-background px-3 py-2.5 pr-10 text-sm text-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          >
            <option value="">{t("lessons.chooseClass")}</option>
            {classes.map((c) => (
              <option key={c.classGroupId} value={c.classGroupId}>{c.className}</option>
            ))}
          </select>
          <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        </div>
      </div>

      {showForm && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-foreground">{editingLesson ? t("lessons.editLesson") : t("lessons.createLesson")}</h3>
            <button onClick={() => { setShowForm(false); setEditingLesson(null); setShowCreateLive(false); setShowLinkPicker(false); setLinkTargetId("") }} className="text-muted-foreground hover:text-foreground"><X className="size-4" /></button>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <label className="text-xs font-medium text-muted-foreground">{t("lessons.titleLabel")}</label>
              <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" placeholder={t("lessons.titlePlaceholder")} />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">{t("lessons.classLabel")}</label>
              <select value={form.classGroupId} onChange={(e) => setForm({ ...form, classGroupId: e.target.value })} className="mt-1 w-full appearance-none rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none">
                <option value="">{t("lessons.selectClassOption")}</option>
                {classes.map((c) => <option key={c.classGroupId} value={c.classGroupId}>{c.className}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">{t("lessons.sortOrder")}</label>
              <input type="number" value={form.sortOrder} onChange={(e) => setForm({ ...form, sortOrder: parseInt(e.target.value) || 0 })} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" />
            </div>
            <div className="sm:col-span-2">
              <label className="text-xs font-medium text-muted-foreground">{t("lessons.descLabel")}</label>
              <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} rows={2} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" placeholder={t("lessons.descPlaceholder")} />
            </div>
            <div className="sm:col-span-2">
              <label className="text-xs font-medium text-muted-foreground">{t("lessons.contentLabel")}</label>
              <textarea value={form.contentText} onChange={(e) => setForm({ ...form, contentText: e.target.value })} rows={6} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" placeholder={t("lessons.contentPlaceholder")} />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">Video URL</label>
              <input value={form.videoUrl} onChange={(e) => setForm({ ...form, videoUrl: e.target.value })} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" placeholder="https://..." />
            </div>
            <div className="flex items-end">
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={form.isPublished} onChange={(e) => setForm({ ...form, isPublished: e.target.checked })} className="size-4 rounded border-border" />
                <span className="text-sm text-foreground">{t("lessons.published")}</span>
              </label>
            </div>
          </div>

          {/* LIVE LEARNING — Lesson ↔ Live Class */}
          <div className="space-y-3 rounded-xl border border-border bg-background/60 p-4">
            <div className="flex items-center gap-2">
              <Video className="size-4 text-teal-600" />
              <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{t("lessons.liveSectionTitle")}</span>
            </div>
            {!editingLesson ? (
              <p className="text-xs text-muted-foreground">{t("lessons.liveSaveFirst")}</p>
            ) : linkedLive ? (
              <div className="space-y-3">
                <div className="flex flex-wrap items-center gap-2">
                  <span className={`inline-flex shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold ${liveStatusMeta(linkedLive.status).className}`}>
                    {liveStatusMeta(linkedLive.status).label}
                  </span>
                  <span className="text-sm font-medium text-foreground">{linkedLive.title}</span>
                  {linkedLive.scheduledAt && (
                    <span className="flex items-center gap-1 text-xs text-muted-foreground">
                      <CalendarClock className="size-3" />
                      {formatLiveDate(linkedLive.scheduledAt)}
                    </span>
                  )}
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <Link
                    href={`/dashboard/teacher/live-classes/${linkedLive.id}`}
                    className="inline-flex items-center gap-1 rounded-lg border border-border bg-card px-2.5 py-1 text-xs font-medium text-foreground hover:bg-muted"
                  >
                    {t("lessons.liveView")}
                  </Link>
                  {linkedLive.status === "SCHEDULED" && (
                    <>
                      <Link
                        href={`/dashboard/teacher/live-classes/${linkedLive.id}/prepare`}
                        className="inline-flex items-center gap-1 rounded-lg border border-border bg-card px-2.5 py-1 text-xs font-medium text-foreground hover:bg-muted"
                      >
                        {t("lessons.livePrepare")}
                      </Link>
                      <button
                        type="button"
                        onClick={() => handleStartLiveFromLesson(linkedLive.id)}
                        disabled={liveBusy}
                        className="inline-flex items-center gap-1 rounded-lg bg-green-600 px-2.5 py-1 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50"
                      >
                        {liveBusy ? <Loader2 className="size-3 animate-spin" /> : <Video className="size-3" />}
                        {t("lessons.liveStart")}
                      </button>
                    </>
                  )}
                  {(linkedLive.status === "IN_PROGRESS" || linkedLive.status === "LIVE") && (
                    <Link
                      href={`/live-classes/${linkedLive.id}`}
                      className="inline-flex items-center gap-1 rounded-lg bg-green-600 px-2.5 py-1 text-xs font-medium text-white hover:bg-green-700"
                    >
                      {t("lessons.liveEnter")}
                    </Link>
                  )}
                  <button
                    type="button"
                    onClick={() => setShowLinkPicker((v) => !v)}
                    className="inline-flex items-center gap-1 rounded-lg border border-border bg-card px-2.5 py-1 text-xs font-medium text-foreground hover:bg-muted"
                  >
                    <Link2 className="size-3" />
                    {t("lessons.liveRelink")}
                  </button>
                  <button
                    type="button"
                    onClick={() => handleUnlinkLive(linkedLive.id)}
                    disabled={liveBusy}
                    className="inline-flex items-center gap-1 rounded-lg border border-destructive/30 px-2.5 py-1 text-xs font-medium text-destructive hover:bg-destructive/10 disabled:opacity-50"
                  >
                    {t("lessons.liveUnlink")}
                  </button>
                </div>
              </div>
            ) : (
              <div className="space-y-3">
                {!showCreateLive && !showLinkPicker && (
                  <div className="flex flex-wrap items-center gap-2">
                    <Button type="button" size="sm" variant="outline" className="gap-1" onClick={() => { setShowCreateLive(true); setLiveForm({ ...liveForm, title: editingLesson.title }) }}>
                      <Video className="size-3" /> {t("lessons.liveCreate")}
                    </Button>
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      className="gap-1"
                      onClick={() => setShowLinkPicker(true)}
                      disabled={linkCandidates.length === 0}
                    >
                      <Link2 className="size-3" /> {t("lessons.liveLinkExisting")}
                    </Button>
                    {linkCandidates.length === 0 && (
                      <span className="text-xs text-muted-foreground">{t("lessons.liveNoCandidates")}</span>
                    )}
                  </div>
                )}

                {showCreateLive && (
                  <div className="space-y-2 rounded-lg border border-border bg-card p-3">
                    <div className="grid gap-2 sm:grid-cols-3">
                      <div className="sm:col-span-3">
                        <label className="text-xs font-medium text-muted-foreground">{t("lessons.liveTitleLabel")}</label>
                        <input
                          value={liveForm.title}
                          onChange={(e) => setLiveForm({ ...liveForm, title: e.target.value })}
                          placeholder={editingLesson.title}
                          className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
                        />
                      </div>
                      <div>
                        <label className="text-xs font-medium text-muted-foreground">{t("lessons.liveDateLabel")}</label>
                        <input
                          type="datetime-local"
                          value={liveForm.scheduledAt}
                          onChange={(e) => setLiveForm({ ...liveForm, scheduledAt: e.target.value })}
                          className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
                        />
                      </div>
                      <div>
                        <label className="text-xs font-medium text-muted-foreground">{t("lessons.liveDurationLabel")}</label>
                        <input
                          type="number"
                          min={1}
                          max={480}
                          value={liveForm.durationMinutes}
                          onChange={(e) => setLiveForm({ ...liveForm, durationMinutes: parseInt(e.target.value) || 60 })}
                          className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
                        />
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button
                        type="button"
                        size="sm"
                        onClick={handleCreateLiveForLesson}
                        disabled={liveBusy || !liveForm.title || !liveForm.scheduledAt}
                      >
                        {liveBusy ? <Loader2 className="size-3 animate-spin" /> : <Video className="size-3" />}
                        {t("lessons.liveSchedule")}
                      </Button>
                      <Button type="button" size="sm" variant="ghost" onClick={() => setShowCreateLive(false)}>{tc("cancel")}</Button>
                    </div>
                  </div>
                )}

                {showLinkPicker && (
                  <div className="space-y-2 rounded-lg border border-border bg-card p-3">
                    <label className="text-xs font-medium text-muted-foreground">{t("lessons.livePickExisting")}</label>
                    <select
                      value={linkTargetId}
                      onChange={(e) => setLinkTargetId(e.target.value)}
                      className="mt-1 w-full appearance-none rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
                    >
                      <option value="">{t("lessons.livePickPlaceholder")}</option>
                      {linkCandidates.map((lc) => (
                        <option key={lc.id} value={lc.id}>
                          {lc.title} — {formatLiveDate(lc.scheduledAt)}
                        </option>
                      ))}
                    </select>
                    <div className="flex gap-2">
                      <Button type="button" size="sm" onClick={handleLinkLive} disabled={!linkTargetId || liveBusy}>
                        {liveBusy ? <Loader2 className="size-3 animate-spin" /> : null}
                        {t("lessons.liveLinkConfirm")}
                      </Button>
                      <Button type="button" size="sm" variant="ghost" onClick={() => { setShowLinkPicker(false); setLinkTargetId("") }}>
                        {tc("cancel")}
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={() => { setShowForm(false); setEditingLesson(null); setShowCreateLive(false); setShowLinkPicker(false); setLinkTargetId("") }}>{tc("cancel")}</Button>
            <Button onClick={handleSave} disabled={saving || !form.title || !form.classGroupId}>
              {saving ? <Loader2 className="size-4 animate-spin" /> : null}
              {editingLesson ? t("announcements.update") : tc("create")}
            </Button>
          </div>
        </div>
      )}

      {!selectedClassId ? (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <BookOpen className="mx-auto mb-3 size-8 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">{t("lessons.selectClassEmpty")}</p>
          <p className="mt-1 text-xs text-muted-foreground">{t("lessons.selectClassEmptyDesc")}</p>
        </div>
      ) : lessons.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <BookOpen className="mx-auto mb-3 size-8 text-muted-foreground" />
          <h3 className="text-sm font-semibold text-foreground">{t("lessons.emptyTitle")}</h3>
          <p className="mt-1 text-xs text-muted-foreground">{t("lessons.emptyDesc")}</p>
          <Button onClick={openCreate} className="mt-4 gap-2" size="sm"><Plus className="size-3" /> {t("lessons.createLesson")}</Button>
        </div>
      ) : (
        <div className="space-y-3">
          {lessons.sort((a, b) => a.sortOrder - b.sortOrder).map((lesson) => {
            const lessonLive = liveForLesson(lesson.id)
            const lessonLiveMeta = lessonLive ? liveStatusMeta(lessonLive.status) : null
            return (
            <div key={lesson.id} className="rounded-2xl border border-border bg-card p-4 shadow-xs">
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-start gap-3 min-w-0">
                  <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-xs font-bold text-primary">{lesson.sortOrder}</div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <h4 className="text-sm font-semibold text-foreground truncate">{lesson.title}</h4>
                      <span className={`inline-flex shrink-0 rounded-full px-2 py-0.5 text-[10px] font-medium ${
                        lesson.isPublished ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-600"
                      }`}>{lesson.isPublished ? t("lessons.published") : t("lessons.draft")}</span>
                      {lessonLive && lessonLiveMeta && (
                        <Link
                          href={`/dashboard/teacher/live-classes/${lessonLive.id}`}
                          className={`inline-flex shrink-0 items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-medium ${lessonLiveMeta.className}`}
                          title={t("lessons.liveOpenBadge")}
                        >
                          <Video className="size-3" />
                          {lessonLiveMeta.label}
                        </Link>
                      )}
                    </div>
                    {lesson.description && <p className="mt-0.5 text-xs text-muted-foreground line-clamp-1">{lesson.description}</p>}
                    <div className="mt-1 flex items-center gap-3 text-xs text-muted-foreground">
                      {lesson.contentText && <span className="flex items-center gap-1"><BookOpen className="size-3" /> {t("lessons.hasContent")}</span>}
                      {lesson.videoUrl && <span className="flex items-center gap-1">🎥 {t("lessons.hasVideo")}</span>}
                    </div>
                  </div>
                </div>
                <div className="flex shrink-0 items-center gap-1">
                  <button onClick={() => togglePublish(lesson)} className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground" title={lesson.isPublished ? t("lessons.unpublish") : t("lessons.publish")}>
                    {lesson.isPublished ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                  </button>
                  <button onClick={() => openEdit(lesson)} className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground" title={tc("edit")}><Pencil className="size-4" /></button>
                  <button onClick={() => handleDelete(lesson.id)} disabled={deleting === lesson.id} className="rounded-lg p-1.5 text-muted-foreground hover:bg-destructive/10 hover:text-destructive" title={tc("delete")}>
                    {deleting === lesson.id ? <Loader2 className="size-4 animate-spin" /> : <Trash2 className="size-4" />}
                  </button>
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
