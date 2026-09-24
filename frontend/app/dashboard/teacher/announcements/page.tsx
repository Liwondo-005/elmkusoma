"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"
import { Bell, Plus, Pencil, Trash2, Loader2, AlertCircle, ChevronDown } from "lucide-react"
import { teacherFetch } from "@/lib/teacher-api"

interface Announcement {
  id: string
  title: string
  content: string
  priority: string
  createdAt: string
}

const initialForm = {
  title: "",
  content: "",
  priority: "NORMAL",
}

export default function TeacherAnnouncementsPage() {
  const { user } = useAuth()
  const t = useTranslations("teacher")
  const tn = useTranslations("nav")
  const tc = useTranslations("common")
  const [announcements, setAnnouncements] = useState<Announcement[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState(initialForm)
  const [submitting, setSubmitting] = useState(false)

  const priorityConfig: Record<string, { label: string; className: string }> = {
    LOW: { label: t("announcements.priorityLow"), className: "bg-gray-100 text-gray-700 dark:bg-gray-800/30 dark:text-gray-400" },
    NORMAL: { label: t("announcements.priorityNormal"), className: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400" },
    HIGH: { label: t("announcements.priorityHigh"), className: "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400" },
    URGENT: { label: t("announcements.priorityUrgent"), className: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400" },
  }

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const data = await teacherFetch<Announcement[]>("/v1/teachers/me/announcements")
      setAnnouncements(data)
    } catch {
      setError(t("announcements.loadError"))
    } finally {
      setLoading(false)
    }
  }

  function resetForm() {
    setForm(initialForm)
    setEditingId(null)
    setShowForm(false)
  }

  function startEdit(ann: Announcement) {
    setForm({
      title: ann.title,
      content: ann.content,
      priority: ann.priority,
    })
    setEditingId(ann.id)
    setShowForm(true)
  }

  async function handleSubmit() {
    if (!form.title.trim() || !form.content.trim()) {
      setError(t("announcements.requiredError"))
      return
    }
    try {
      setSubmitting(true)
      setError(null)
      const payload = {
        title: form.title.trim(),
        content: form.content.trim(),
        priority: form.priority,
      }

      if (editingId) {
        await teacherFetch(`/v1/teachers/me/announcements/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        })
        setSuccess(t("announcements.updatedSuccess"))
      } else {
        await teacherFetch("/v1/teachers/me/announcements", {
          method: "POST",
          body: JSON.stringify(payload),
        })
        setSuccess(t("announcements.createdSuccess"))
      }
      resetForm()
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("announcements.saveError"))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(id: string) {
    if (!confirm(t("announcements.deleteConfirm"))) return
    try {
      setError(null)
      await teacherFetch(`/v1/teachers/me/announcements/${id}`, { method: "DELETE" })
      setSuccess(t("announcements.deletedSuccess"))
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("announcements.deleteError"))
    }
  }

  function formatDate(iso: string) {
    if (!iso) return "—"
    return new Date(iso).toLocaleString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    })
  }

  const sorted = [...announcements].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())

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
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("announcements")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("announcements.subtitle")}</p>
        </div>
        <Button
          className="gap-2"
          onClick={() => {
            resetForm()
            setShowForm(!showForm)
          }}
        >
          {showForm ? <Trash2 className="size-4" /> : <Plus className="size-4" />}
          {showForm ? tc("cancel") : t("announcements.newAnnouncement")}
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
            <Bell className="size-4 shrink-0" />
            {success}
          </div>
        </div>
      )}

      {showForm && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <h2 className="text-base font-semibold text-foreground">
            {editingId ? t("announcements.editTitle") : t("announcements.newAnnouncement")}
          </h2>
          <div className="grid gap-4">
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("announcements.titleLabel")}</label>
              <input
                type="text"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder={t("announcements.titlePlaceholder")}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("announcements.contentLabel")}</label>
              <textarea
                value={form.content}
                onChange={(e) => setForm({ ...form, content: e.target.value })}
                placeholder={t("announcements.contentPlaceholder")}
                rows={5}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("announcements.priorityLabel")}</label>
              <div className="relative">
                <select
                  value={form.priority}
                  onChange={(e) => setForm({ ...form, priority: e.target.value })}
                  className="h-10 w-full appearance-none rounded-lg border border-border bg-background px-3 pr-10 text-sm outline-none focus:border-ring"
                >
                  <option value="LOW">{t("announcements.priorityLow")}</option>
                  <option value="NORMAL">{t("announcements.priorityNormal")}</option>
                  <option value="HIGH">{t("announcements.priorityHigh")}</option>
                  <option value="URGENT">{t("announcements.priorityUrgent")}</option>
                </select>
                <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              </div>
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={resetForm}>{tc("cancel")}</Button>
            <Button onClick={handleSubmit} disabled={submitting || !form.title.trim() || !form.content.trim()}>
              {submitting ? tc("saving") : editingId ? t("announcements.update") : t("announcements.publish")}
            </Button>
          </div>
        </div>
      )}

      {sorted.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border bg-card p-12 text-center">
          <Bell className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("announcements.emptyTitle")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            {t("announcements.emptyDesc")}
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {sorted.map((ann) => {
            const priority = priorityConfig[ann.priority] || priorityConfig.NORMAL
            return (
              <div key={ann.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:shadow-sm">
                <div className="flex items-start gap-4">
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 shrink-0">
                    <Bell className="size-5 text-primary" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <h3 className="text-sm font-semibold text-foreground">{ann.title}</h3>
                      <span className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-semibold ${priority.className}`}>
                        {priority.label}
                      </span>
                    </div>
                    <p className="mt-1.5 text-xs text-muted-foreground line-clamp-3">{ann.content}</p>
                    <p className="mt-2 text-xs text-muted-foreground">{formatDate(ann.createdAt)}</p>
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => startEdit(ann)}
                      title={tc("edit")}
                    >
                      <Pencil className="size-3.5" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => handleDelete(ann.id)}
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
