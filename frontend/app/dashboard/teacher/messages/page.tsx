"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"
import { MessageSquare, Send, Pencil, Trash2, Loader2, AlertCircle, ChevronDown, Search, Users } from "lucide-react"
import { teacherFetch } from "@/lib/teacher-api"

interface Message {
  id: string
  title: string
  content: string
  priority: string
  createdAt: string
}

interface StudentInfo {
  id: string
  firstName: string
  lastName: string
  email: string | null
  admissionNumber: string
}

const initialForm = {
  title: "",
  content: "",
  priority: "NORMAL",
}

export default function TeacherMessagesPage() {
  const { user } = useAuth()
  const t = useTranslations("teacher")
  const tn = useTranslations("nav")
  const tc = useTranslations("common")
  const [messages, setMessages] = useState<Message[]>([])
  const [students, setStudents] = useState<StudentInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState(initialForm)
  const [submitting, setSubmitting] = useState(false)
  const [search, setSearch] = useState("")
  const [expandedId, setExpandedId] = useState<string | null>(null)

  const priorityConfig: Record<string, { label: string; className: string; dotClassName: string }> = {
    LOW: { label: t("announcements.priorityLow"), className: "bg-gray-100 text-gray-700 dark:bg-gray-800/30 dark:text-gray-400", dotClassName: "bg-gray-400" },
    NORMAL: { label: t("announcements.priorityNormal"), className: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400", dotClassName: "bg-blue-400" },
    HIGH: { label: t("announcements.priorityHigh"), className: "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400", dotClassName: "bg-orange-400" },
    URGENT: { label: t("announcements.priorityUrgent"), className: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400", dotClassName: "bg-red-400" },
  }

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const [messagesResult, studentsResult] = await Promise.allSettled([
        teacherFetch<Message[]>("/v1/teachers/me/announcements"),
        teacherFetch<StudentInfo[]>("/v1/teachers/me/students"),
      ])
      setMessages(messagesResult.status === "fulfilled" ? messagesResult.value : [])
      setStudents(studentsResult.status === "fulfilled" ? studentsResult.value : [])
    } catch {
      setError(t("messages.loadError"))
    } finally {
      setLoading(false)
    }
  }

  function resetForm() {
    setForm(initialForm)
    setEditingId(null)
    setShowForm(false)
  }

  function startEdit(msg: Message) {
    setForm({
      title: msg.title,
      content: msg.content,
      priority: msg.priority,
    })
    setEditingId(msg.id)
    setShowForm(true)
    window.scrollTo({ top: 0, behavior: "smooth" })
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
        setSuccess(t("messages.updatedSuccess"))
      } else {
        await teacherFetch("/v1/teachers/me/announcements", {
          method: "POST",
          body: JSON.stringify(payload),
        })
        setSuccess(t("messages.sentSuccess"))
      }
      resetForm()
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("messages.saveError"))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(id: string) {
    if (!confirm(t("messages.deleteConfirm"))) return
    try {
      setError(null)
      await teacherFetch(`/v1/teachers/me/announcements/${id}`, { method: "DELETE" })
      setSuccess(t("messages.deletedSuccess"))
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("messages.deleteError"))
    }
  }

  function formatDate(iso: string) {
    if (!iso) return "—"
    const date = new Date(iso)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffMins = Math.floor(diffMs / 60000)
    const diffHours = Math.floor(diffMins / 60)
    const diffDays = Math.floor(diffHours / 24)

    if (diffMins < 1) return t("notifications.timeJustNow")
    if (diffMins < 60) return t("messages.timeMinutes", { count: diffMins })
    if (diffHours < 24) return t("messages.timeHours", { count: diffHours })
    if (diffDays < 7) return t("messages.timeDays", { count: diffDays })

    return date.toLocaleDateString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
    })
  }

  function formatFullDate(iso: string) {
    if (!iso) return "—"
    return new Date(iso).toLocaleString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    })
  }

  function truncateText(text: string, maxLength: number) {
    if (text.length <= maxLength) return text
    return text.slice(0, maxLength).trimEnd() + "..."
  }

  const sorted = [...messages].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())

  const filtered = sorted.filter((msg) =>
    msg.title.toLowerCase().includes(search.toLowerCase()) ||
    msg.content.toLowerCase().includes(search.toLowerCase())
  )

  const totalStudents = students.length

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("messages")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {t("messages.subtitle")} {totalStudents > 0 && t("messages.recipientsNote", { count: totalStudents })}
          </p>
        </div>
        <Button
          className="gap-2 self-start"
          onClick={() => {
            resetForm()
            setShowForm(!showForm)
          }}
        >
          {showForm ? <Trash2 className="size-4" /> : <Send className="size-4" />}
          {showForm ? tc("cancel") : t("messages.newMessage")}
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
            <Send className="size-4 shrink-0" />
            {success}
          </div>
        </div>
      )}

      {showForm && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
              <Send className="size-5 text-primary" />
            </div>
            <div>
              <h2 className="text-base font-semibold text-foreground">
                {editingId ? t("messages.editTitle") : t("messages.composeTitle")}
              </h2>
              <p className="text-xs text-muted-foreground">
                {editingId ? t("messages.editDesc") : t("messages.composeDesc")}
              </p>
            </div>
          </div>
          <div className="grid gap-4">
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("announcements.titleLabel")}</label>
              <input
                type="text"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder={t("messages.titlePlaceholder")}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("announcements.contentLabel")}</label>
              <textarea
                value={form.content}
                onChange={(e) => setForm({ ...form, content: e.target.value })}
                placeholder={t("messages.contentPlaceholder")}
                rows={6}
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
          <div className="flex items-center justify-between rounded-xl border border-border bg-muted/40 px-4 py-3">
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Users className="size-4" />
              <span>
                {t("messages.recipientsLabel", { count: totalStudents })}
              </span>
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={resetForm}>{tc("cancel")}</Button>
            <Button onClick={handleSubmit} disabled={submitting || !form.title.trim() || !form.content.trim()} className="gap-2">
              {submitting ? (
                <>
                  <Loader2 className="size-4 animate-spin" />
                  {t("messages.sending")}
                </>
              ) : editingId ? (
                t("announcements.update")
              ) : (
                <>
                  <Send className="size-4" />
                  {t("messages.sendMessage")}
                </>
              )}
            </Button>
          </div>
        </div>
      )}

      {!showForm && messages.length > 0 && (
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder={t("messages.searchPlaceholder")}
            className="h-10 w-full rounded-lg border border-border bg-background pl-9 pr-3 text-sm outline-none focus:border-ring"
          />
        </div>
      )}

      {filtered.length === 0 && !showForm ? (
        messages.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-border bg-card p-12 text-center">
            <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-muted">
              <MessageSquare className="size-7 text-muted-foreground" />
            </div>
            <h3 className="mt-4 text-lg font-semibold text-foreground">{t("messages.emptyTitle")}</h3>
            <p className="mt-2 max-w-md mx-auto text-sm text-muted-foreground">
              {t("messages.emptyDesc")}
            </p>
            <Button className="mt-6 gap-2" onClick={() => setShowForm(true)}>
              <Send className="size-4" />
              {t("messages.composeFirst")}
            </Button>
          </div>
        ) : (
          <div className="rounded-2xl border border-dashed border-border bg-card p-8 text-center">
            <Search className="mx-auto size-8 text-muted-foreground/50" />
            <h3 className="mt-3 text-sm font-semibold text-foreground">{t("messages.noMatchTitle")}</h3>
            <p className="mt-1 text-xs text-muted-foreground">
              {t("messages.noMatchDesc")}
            </p>
          </div>
        )
      ) : (
        <div className="space-y-3">
          {filtered.map((msg) => {
            const priority = priorityConfig[msg.priority] || priorityConfig.NORMAL
            const isExpanded = expandedId === msg.id
            return (
              <div key={msg.id} className="rounded-2xl border border-border bg-card shadow-xs transition-all hover:shadow-sm">
                <div className="p-5">
                  <div className="flex items-start gap-4">
                    <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 shrink-0">
                      <MessageSquare className="size-5 text-primary" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <h3 className="text-sm font-semibold text-foreground">{msg.title}</h3>
                        <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold ${priority.className}`}>
                          <span className={`size-1.5 rounded-full ${priority.dotClassName}`} />
                          {priority.label}
                        </span>
                      </div>
                      <p className="mt-1.5 text-xs text-muted-foreground line-clamp-2">
                        {truncateText(msg.content, 150)}
                      </p>
                      <div className="mt-2.5 flex items-center gap-3 text-xs text-muted-foreground">
                        <span title={formatFullDate(msg.createdAt)}>{formatDate(msg.createdAt)}</span>
                        <span className="text-border">·</span>
                        <span className="flex items-center gap-1">
                          <Users className="size-3" />
                          {t("messages.recipientsLabel", { count: totalStudents })}
                        </span>
                      </div>
                    </div>
                    <div className="flex items-center gap-1 shrink-0">
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => setExpandedId(isExpanded ? null : msg.id)}
                        title={isExpanded ? tc("collapse") : tc("expand")}
                      >
                        <ChevronDown className={`size-3.5 transition-transform ${isExpanded ? "rotate-180" : ""}`} />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => startEdit(msg)}
                        title={tc("edit")}
                      >
                        <Pencil className="size-3.5" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => handleDelete(msg.id)}
                        title={tc("delete")}
                        className="text-destructive hover:text-destructive"
                      >
                        <Trash2 className="size-3.5" />
                      </Button>
                    </div>
                  </div>
                </div>

                {isExpanded && (
                  <div className="border-t border-border px-5 py-4">
                    <p className="text-sm text-foreground whitespace-pre-wrap leading-relaxed">{msg.content}</p>
                    <div className="mt-4 flex items-center gap-4 text-xs text-muted-foreground">
                      <span>{t("messages.sentLabel", { date: formatFullDate(msg.createdAt) })}</span>
                      {students.length > 0 && (
                        <span className="flex items-center gap-1">
                          <Users className="size-3" />
                          {t("messages.toLabel", { names: students.map((s) => `${s.firstName} ${s.lastName}`).slice(0, 3).join(", ") })}
                          {students.length > 3 && t("messages.andMore", { count: students.length - 3 })}
                        </span>
                      )}
                    </div>
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
