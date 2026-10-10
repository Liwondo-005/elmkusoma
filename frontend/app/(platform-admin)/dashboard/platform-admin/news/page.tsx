"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import {
  AlertCircle,
  Archive,
  CheckCircle2,
  Eye,
  Loader2,
  Plus,
  RefreshCw,
  Save,
  Send,
  Star,
  Trash2,
  Undo2,
} from "lucide-react"
import { useTranslations } from "next-intl"
import {
  platformAdminApi,
  type NewsArticlePayload,
  type NewsArticleRecord,
  type NewsPriority,
  type NewsStatus,
  type PageResponse,
} from "@/lib/platform-admin-api"

const PAGE_SIZE = 20

const EMPTY_FORM: NewsArticlePayload = {
  title: "",
  summary: "",
  body: "",
  slug: "",
  category: "",
  coverImageUrl: "",
  authorName: "",
  priority: "NORMAL",
  scheduledAt: null,
  expiresAt: null,
}

/**
 * Platform Admin news management.
 *
 * <p>Every control here calls a real endpoint and then re-reads the list, so what is on screen is
 * the persisted state rather than an optimistic guess. That matters most for the star and the
 * lifecycle buttons: a toggle that looks like it worked but did not would leave an administrator
 * believing a featured article is featured when it is not.</p>
 *
 * <p>Draft, publish, unpublish and archive are separate actions because they are separate
 * endpoints. There is deliberately no "status" dropdown on the form.</p>
 */
export default function NewsAdminPage() {
  const t = useTranslations("platformAdmin.news")

  const [page, setPage] = useState<PageResponse<NewsArticleRecord> | null>(null)
  const [pageIndex, setPageIndex] = useState(0)
  const [loading, setLoading] = useState(true)
  const [listError, setListError] = useState<string | null>(null)

  const [query, setQuery] = useState("")
  const [statusFilter, setStatusFilter] = useState<"" | NewsStatus>("")
  const [featuredFilter, setFeaturedFilter] = useState<"" | "true" | "false">("")

  const [editing, setEditing] = useState<NewsArticleRecord | null>(null)
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState<NewsArticlePayload>(EMPTY_FORM)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [confirmingDelete, setConfirmingDelete] = useState<string | null>(null)

  const load = useCallback(async (index: number) => {
    setLoading(true)
    setListError(null)
    try {
      const result = await platformAdminApi.listNews({
        page: index,
        size: PAGE_SIZE,
        status: statusFilter || undefined,
        featured: featuredFilter === "" ? undefined : featuredFilter === "true",
        q: query.trim() || undefined,
      })
      setPage(result)
    } catch (error) {
      setListError(error instanceof Error ? error.message : t("loadFailed"))
      setPage(null)
    } finally {
      setLoading(false)
    }
  }, [query, statusFilter, featuredFilter, t])

  useEffect(() => {
    load(pageIndex)
  }, [load, pageIndex])

  const filtersActive = query.trim() !== "" || statusFilter !== "" || featuredFilter !== ""

  function startCreate() {
    setCreating(true)
    setEditing(null)
    setForm(EMPTY_FORM)
    setFormError(null)
  }

  function startEdit(article: NewsArticleRecord) {
    setCreating(false)
    setEditing(article)
    setForm({
      title: article.title,
      summary: article.summary,
      body: article.body,
      slug: article.slug,
      category: article.category ?? "",
      coverImageUrl: article.coverImageUrl ?? "",
      authorName: article.authorName ?? "",
      priority: article.priority,
      scheduledAt: article.scheduledAt ? toLocalInput(article.scheduledAt) : null,
      expiresAt: article.expiresAt ? toLocalInput(article.expiresAt) : null,
    })
    setFormError(null)
  }

  function cancelEdit() {
    setCreating(false)
    setEditing(null)
    setForm(EMPTY_FORM)
    setFormError(null)
  }

  /** Payload as the API expects it: blank optional strings become absent, dates become ISO. */
  const payload: NewsArticlePayload = useMemo(
    () => ({
      ...form,
      slug: form.slug?.trim() || undefined,
      category: form.category?.trim() || undefined,
      coverImageUrl: form.coverImageUrl?.trim() || undefined,
      authorName: form.authorName?.trim() || undefined,
      scheduledAt: form.scheduledAt ? new Date(form.scheduledAt).toISOString() : null,
      expiresAt: form.expiresAt ? new Date(form.expiresAt).toISOString() : null,
    }),
    [form]
  )

  async function handleSave(event: React.FormEvent) {
    event.preventDefault()
    setSaving(true)
    setFormError(null)
    setNotice(null)
    try {
      if (editing) {
        await platformAdminApi.updateNewsArticle(editing.id, payload)
        setNotice(t("savedNotice"))
        setEditing(null)
      } else {
        await platformAdminApi.createNewsArticle(payload)
        setNotice(t("draftCreatedNotice"))
        setCreating(false)
      }
      setForm(EMPTY_FORM)
      await load(pageIndex)
    } catch (error) {
      setFormError(error instanceof Error ? error.message : t("saveFailed"))
    } finally {
      setSaving(false)
    }
  }

  /**
   * Runs a lifecycle transition and re-reads the list.
   *
   * <p>The star toggle is the reason this is shared code: it must flip back if the server rejects
   * it, which a pure local state flip could not do.</p>
   */
  async function runAction(
    article: NewsArticleRecord,
    action: () => Promise<unknown>,
    successMessage: string
  ) {
    setBusyId(article.id)
    setNotice(null)
    setListError(null)
    try {
      await action()
      setNotice(successMessage)
      await load(pageIndex)
    } catch (error) {
      setListError(error instanceof Error ? error.message : t("actionFailed"))
      // Re-read anyway: the failed call may still have changed something server-side.
      await load(pageIndex)
    } finally {
      setBusyId(null)
      setConfirmingDelete(null)
    }
  }

  const showForm = creating || editing !== null

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("title")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("subtitle")}</p>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => load(pageIndex)}
            className="inline-flex items-center gap-2 rounded-xl border border-border px-3 py-2 text-sm font-medium text-foreground hover:bg-muted"
          >
            <RefreshCw className="size-4" aria-hidden />
            {t("refresh")}
          </button>
          <button
            type="button"
            onClick={startCreate}
            className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            <Plus className="size-4" aria-hidden />
            {t("newArticle")}
          </button>
        </div>
      </div>

      {notice && (
        <p role="status" className="flex items-center gap-2 rounded-lg border border-teal/20 bg-teal/5 px-3 py-2 text-sm text-teal">
          <CheckCircle2 className="size-4 shrink-0" aria-hidden />
          {notice}
        </p>
      )}
      {listError && (
        <p role="alert" className="flex items-start gap-2 rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-sm text-destructive">
          <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden />
          <span>{listError}</span>
        </p>
      )}

      {showForm && (
        <form onSubmit={handleSave} className="space-y-4 rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">
            {editing ? t("editHeading") : t("createHeading")}
          </h2>

          <Field label={t("fieldTitle")} required>
            <input
              value={form.title ?? ""}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              className={inputClass}
              maxLength={200}
              required
            />
          </Field>

          <Field label={t("fieldSummary")} required hint={t("fieldSummaryHint")}>
            <textarea
              value={form.summary ?? ""}
              onChange={(e) => setForm({ ...form, summary: e.target.value })}
              className={`${inputClass} py-2 resize-none`}
              rows={2}
              maxLength={500}
              required
            />
          </Field>

          <Field label={t("fieldBody")} required hint={t("fieldBodyHint")}>
            <textarea
              value={form.body ?? ""}
              onChange={(e) => setForm({ ...form, body: e.target.value })}
              className={`${inputClass} py-2 resize-y`}
              rows={10}
              required
            />
          </Field>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label={t("fieldSlug")} hint={t("fieldSlugHint")}>
              <input
                value={form.slug ?? ""}
                onChange={(e) => setForm({ ...form, slug: e.target.value })}
                className={inputClass}
                maxLength={160}
                placeholder={t("fieldSlugPlaceholder")}
              />
            </Field>
            <Field label={t("fieldCategory")}>
              <input
                value={form.category ?? ""}
                onChange={(e) => setForm({ ...form, category: e.target.value })}
                className={inputClass}
                maxLength={40}
              />
            </Field>
            <Field label={t("fieldAuthor")}>
              <input
                value={form.authorName ?? ""}
                onChange={(e) => setForm({ ...form, authorName: e.target.value })}
                className={inputClass}
                maxLength={160}
              />
            </Field>
            <Field label={t("fieldPriority")}>
              <select
                value={form.priority ?? "NORMAL"}
                onChange={(e) => setForm({ ...form, priority: e.target.value as NewsPriority })}
                className={inputClass}
              >
                <option value="NORMAL">{t("priorityNormal")}</option>
                <option value="IMPORTANT">{t("priorityImportant")}</option>
                <option value="URGENT">{t("priorityUrgent")}</option>
              </select>
            </Field>
            <Field label={t("fieldScheduled")} hint={t("fieldScheduledHint")}>
              <input
                type="datetime-local"
                value={form.scheduledAt ?? ""}
                onChange={(e) => setForm({ ...form, scheduledAt: e.target.value || null })}
                className={inputClass}
              />
            </Field>
            <Field label={t("fieldExpires")} hint={t("fieldExpiresHint")}>
              <input
                type="datetime-local"
                value={form.expiresAt ?? ""}
                onChange={(e) => setForm({ ...form, expiresAt: e.target.value || null })}
                className={inputClass}
              />
            </Field>
          </div>

          <Field label={t("fieldCover")} hint={t("fieldCoverHint")}>
            <input
              value={form.coverImageUrl ?? ""}
              onChange={(e) => setForm({ ...form, coverImageUrl: e.target.value })}
              className={inputClass}
              maxLength={1000}
              placeholder={t("fieldCoverPlaceholder")}
            />
          </Field>

          {formError && (
            <p role="alert" className="flex items-start gap-2 rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-sm text-destructive">
              <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden />
              <span>{formError}</span>
            </p>
          )}

          <div className="flex items-center gap-2">
            <button
              type="submit"
              disabled={saving}
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              {saving ? <Loader2 className="size-4 animate-spin" aria-hidden /> : <Save className="size-4" aria-hidden />}
              {editing ? t("saveChanges") : t("saveDraft")}
            </button>
            <button
              type="button"
              onClick={cancelEdit}
              className="rounded-xl border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-muted"
            >
              {t("cancel")}
            </button>
          </div>
        </form>
      )}

      <div className="flex flex-wrap items-end gap-3">
        <Field label={t("searchLabel")}>
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className={inputClass}
            placeholder={t("searchPlaceholder")}
          />
        </Field>
        <Field label={t("statusLabel")}>
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value as "" | NewsStatus)
              setPageIndex(0)
            }}
            className={inputClass}
          >
            <option value="">{t("statusAll")}</option>
            <option value="DRAFT">{t("statusDraft")}</option>
            <option value="PUBLISHED">{t("statusPublished")}</option>
            <option value="ARCHIVED">{t("statusArchived")}</option>
          </select>
        </Field>
        <Field label={t("featuredLabel")}>
          <select
            value={featuredFilter}
            onChange={(e) => {
              setFeaturedFilter(e.target.value as "" | "true" | "false")
              setPageIndex(0)
            }}
            className={inputClass}
          >
            <option value="">{t("featuredAny")}</option>
            <option value="true">{t("featuredOnly")}</option>
            <option value="false">{t("unfeaturedOnly")}</option>
          </select>
        </Field>
      </div>

      {loading && (
        <p className="flex items-center gap-2 text-sm text-muted-foreground" role="status">
          <Loader2 className="size-4 animate-spin" aria-hidden />
          {t("loading")}
        </p>
      )}

      {!loading && page && page.content.length === 0 && (
        <p className="rounded-2xl border border-dashed border-border py-12 text-center text-sm text-muted-foreground">
          {filtersActive ? t("emptyFiltered") : t("empty")}
        </p>
      )}

      {!loading && page && page.content.length > 0 && (
        <ul className="space-y-3">
          {page.content.map((article) => (
            <li key={article.id} className="rounded-2xl border border-border bg-card p-4 shadow-xs">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusPill status={article.status} />
                    {article.featured && (
                      <span className="inline-flex items-center gap-1 rounded-md bg-orange/10 px-2 py-0.5 text-xs font-semibold text-orange">
                        <Star className="size-3 fill-current" aria-hidden />
                        {t("featuredBadge")}
                      </span>
                    )}
                    {article.priority !== "NORMAL" && (
                      <span className="rounded-md bg-secondary px-2 py-0.5 text-xs font-medium text-secondary-foreground">
                        {article.priority === "URGENT" ? t("priorityUrgent") : t("priorityImportant")}
                      </span>
                    )}
                  </div>
                  <h3 className="mt-2 font-semibold text-foreground">{article.title}</h3>
                  <p className="mt-1 text-sm text-muted-foreground">{article.summary}</p>
                  <p className="mt-2 text-xs text-muted-foreground">
                    <code className="rounded bg-muted px-1 py-0.5">/{article.slug}</code>
                    {article.publishedAt && <> · {t("publishedAt", { date: formatStamp(article.publishedAt) })}</>}
                    {article.expiresAt && <> · {t("expiresAt", { date: formatStamp(article.expiresAt) })}</>}
                    {article.lastModifiedBy && <> · {t("lastModifiedBy", { who: article.lastModifiedBy })}</>}
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-1.5">
                  <IconAction
                    label={article.featured ? t("unfeature") : t("feature")}
                    active={article.featured}
                    disabled={busyId === article.id}
                    onClick={() =>
                      runAction(
                        article,
                        () => platformAdminApi.setNewsFeatured(article.id, !article.featured),
                        article.featured ? t("unfeaturedNotice") : t("featuredNotice")
                      )
                    }
                  >
                    <Star className={`size-4 ${article.featured ? "fill-current" : ""}`} aria-hidden />
                  </IconAction>

                  <IconAction
                    label={t("edit")}
                    disabled={busyId === article.id}
                    onClick={() => startEdit(article)}
                  >
                    <Eye className="size-4" aria-hidden />
                  </IconAction>

                  {article.status === "PUBLISHED" ? (
                    <IconAction
                      label={t("unpublish")}
                      disabled={busyId === article.id}
                      onClick={() =>
                        runAction(article, () => platformAdminApi.unpublishNewsArticle(article.id), t("unpublishedNotice"))
                      }
                    >
                      <Undo2 className="size-4" aria-hidden />
                    </IconAction>
                  ) : (
                    <IconAction
                      label={t("publish")}
                      disabled={busyId === article.id}
                      onClick={() =>
                        runAction(article, () => platformAdminApi.publishNewsArticle(article.id), t("publishedNotice"))
                      }
                    >
                      <Send className="size-4" aria-hidden />
                    </IconAction>
                  )}

                  {article.status !== "ARCHIVED" && (
                    <IconAction
                      label={t("archive")}
                      disabled={busyId === article.id}
                      onClick={() =>
                        runAction(article, () => platformAdminApi.archiveNewsArticle(article.id), t("archivedNotice"))
                      }
                    >
                      <Archive className="size-4" aria-hidden />
                    </IconAction>
                  )}

                  {confirmingDelete === article.id ? (
                    <span className="flex items-center gap-1.5">
                      <button
                        type="button"
                        onClick={() =>
                          runAction(article, () => platformAdminApi.deleteNewsArticle(article.id), t("deletedNotice"))
                        }
                        className="rounded-lg bg-destructive px-2.5 py-1.5 text-xs font-semibold text-destructive-foreground"
                      >
                        {t("confirmDelete")}
                      </button>
                      <button
                        type="button"
                        onClick={() => setConfirmingDelete(null)}
                        className="rounded-lg border border-border px-2.5 py-1.5 text-xs font-medium"
                      >
                        {t("cancel")}
                      </button>
                    </span>
                  ) : (
                    <IconAction
                      label={t("delete")}
                      destructive
                      disabled={busyId === article.id}
                      onClick={() => setConfirmingDelete(article.id)}
                    >
                      <Trash2 className="size-4" aria-hidden />
                    </IconAction>
                  )}
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}

      {page && page.totalPages > 1 && (
        <nav className="flex items-center justify-center gap-3" aria-label={t("paginationLabel")}>
          <button
            type="button"
            disabled={pageIndex === 0 || loading}
            onClick={() => setPageIndex((p) => p - 1)}
            className="rounded-xl border border-border px-3 py-2 text-sm font-medium disabled:opacity-50"
          >
            {t("previous")}
          </button>
          <span className="text-sm text-muted-foreground">
            {t("pageOf", { page: pageIndex + 1, total: page.totalPages })}
          </span>
          <button
            type="button"
            disabled={pageIndex >= page.totalPages - 1 || loading}
            onClick={() => setPageIndex((p) => p + 1)}
            className="rounded-xl border border-border px-3 py-2 text-sm font-medium disabled:opacity-50"
          >
            {t("next")}
          </button>
        </nav>
      )}
    </div>
  )
}

const inputClass =
  "h-10 w-full rounded-lg border border-border bg-background px-3 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:ring-2 focus:ring-ring"

function Field({
  label,
  hint,
  required,
  children,
}: {
  label: string
  hint?: string
  required?: boolean
  children: React.ReactNode
}) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-foreground">
        {label}
        {required && <span className="ml-0.5 text-destructive">*</span>}
      </span>
      {children}
      {hint && <span className="mt-1 block text-xs text-muted-foreground">{hint}</span>}
    </label>
  )
}

function IconAction({
  label,
  onClick,
  disabled,
  destructive,
  active,
  children,
}: {
  label: string
  onClick: () => void
  disabled?: boolean
  destructive?: boolean
  active?: boolean
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      // The icon carries the meaning visually; the text label is what a screen reader reads and
      // what the tooltip shows. Without it these buttons would be unlabelled to assistive tech.
      title={label}
      aria-label={label}
      aria-pressed={active}
      className={`inline-flex size-9 items-center justify-center rounded-lg border disabled:opacity-50 ${
        destructive
          ? "border-destructive/30 text-destructive hover:bg-destructive/10"
          : active
            ? "border-orange/40 bg-orange/10 text-orange"
            : "border-border text-foreground hover:bg-muted"
      }`}
    >
      {children}
    </button>
  )
}

function StatusPill({ status }: { status: NewsStatus }) {
  const t = useTranslations("platformAdmin.news")
  const tone =
    status === "PUBLISHED"
      ? "bg-teal/10 text-teal"
      : status === "ARCHIVED"
        ? "bg-muted text-muted-foreground"
        : "bg-secondary text-secondary-foreground"
  const label = status === "PUBLISHED" ? t("statusPublished") : status === "ARCHIVED" ? t("statusArchived") : t("statusDraft")
  return <span className={`rounded-md px-2 py-0.5 text-xs font-semibold ${tone}`}>{label}</span>
}

/** ISO instant to the `datetime-local` format, which has no timezone. */
function toLocalInput(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ""
  const pad = (n: number) => String(n).padStart(2, "0")
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function formatStamp(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString()
}