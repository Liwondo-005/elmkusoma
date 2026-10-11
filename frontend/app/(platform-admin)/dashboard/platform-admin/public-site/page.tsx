"use client"

import { useCallback, useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import {
  AlertCircle,
  CheckCircle,
  ExternalLink,
  FileText,
  Globe,
  Inbox,
  Loader2,
  Save,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  platformAdminApi,
  type ContactMessageRecord,
  type LegalDocumentRecord,
  type LegalVersionRecord,
  type PlatformConfigItem,
} from "@/lib/platform-admin-api"

type Tab = "settings" | "legal" | "inbox"

/** Groups the public settings by what an administrator is actually configuring. */
const SETTING_GROUPS: { id: string; keys: string[] }[] = [
  {
    id: "PUBLIC_CONTACT",
    keys: [
      "public.contact.email",
      "public.contact.supportEmail",
      "public.contact.phone",
      "public.contact.whatsapp",
      "public.contact.address",
      "public.contact.workingHours",
      "public.contact.responseTime",
    ],
  },
  {
    id: "PUBLIC_SOCIAL",
    keys: [
      "public.social.facebook",
      "public.social.instagram",
      "public.social.youtube",
      "public.social.linkedin",
      "public.social.tiktok",
      "public.social.x",
    ],
  },
  {
    id: "SUPPORT_INTERNAL",
    keys: ["support.notify.email", "support.notify.adminEmail", "support.whatsapp.enabled"],
  },
]

/** Client-side mirror of the server's URL rule, to fail fast without inventing a warning. */
function looksUnsafe(value: string, key: string): boolean {
  const trimmed = value.trim()
  if (!trimmed) return false
  if (key.startsWith("public.social.")) return !/^https?:\/\/\S+$/.test(trimmed)
  if (key === "public.contact.whatsapp") return !/^\d{8,15}$/.test(trimmed)
  if (key === "public.contact.phone") return !/^\+[\d ()-]{6,24}$/.test(trimmed)
  if (key.endsWith("email")) return !/^[^@\s]+@[^@\s]+\.[A-Za-z]{2,}$/.test(trimmed)
  return false
}

export default function PublicSiteAdminPage() {
  const t = useTranslations("platformAdmin.publicSite")
  const [tab, setTab] = useState<Tab>("settings")

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("subtitle")}</p>
      </div>

      <div role="tablist" aria-label={t("title")} className="flex flex-wrap gap-2">
        {([
          ["settings", Globe, t("tabSettings")],
          ["legal", FileText, t("tabLegal")],
          ["inbox", Inbox, t("tabInbox")],
        ] as const).map(([id, Icon, label]) => (
          <button
            key={id}
            role="tab"
            type="button"
            aria-selected={tab === id}
            onClick={() => setTab(id)}
            className={`inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition-colors ${
              tab === id
                ? "border-primary bg-primary text-primary-foreground"
                : "border-border bg-card text-foreground hover:bg-muted"
            }`}
          >
            <Icon className="size-4" aria-hidden="true" />
            {label}
          </button>
        ))}
      </div>

      {tab === "settings" && <SettingsTab />}
      {tab === "legal" && <LegalTab />}
      {tab === "inbox" && <InboxTab />}
    </div>
  )
}

// ── public + internal settings ────────────────────────────────────────────────────────

function SettingsTab() {
  const t = useTranslations("platformAdmin.publicSite")
  const [configs, setConfigs] = useState<PlatformConfigItem[]>([])
  const [edits, setEdits] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState<string | null>(null)
  const [savedKey, setSavedKey] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(() => {
    setLoading(true)
    platformAdminApi
      .listConfig()
      .then((items) => setConfigs(items.filter((c) => SETTING_GROUPS.some((g) => g.keys.includes(c.configKey)))))
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  useEffect(load, [load])

  async function save(key: string) {
    const value = edits[key]
    if (value === undefined) return
    setSaving(key)
    setError(null)
    try {
      const updated = await platformAdminApi.updateConfig(key, value)
      setConfigs((prev) =>
        prev.map((c) => (c.configKey === key ? { ...c, configValue: updated.configValue } : c)),
      )
      setEdits((prev) => {
        const next = { ...prev }
        delete next[key]
        return next
      })
      setSavedKey(key)
      setTimeout(() => setSavedKey(null), 2500)
    } catch (e) {
      setError(e instanceof Error ? e.message : t("saveFailed"))
    } finally {
      setSaving(null)
    }
  }

  if (loading) {
    return (
      <p className="flex items-center gap-2 py-10 text-sm text-muted-foreground">
        <Loader2 className="size-4 animate-spin" aria-hidden="true" /> {t("loading")}
      </p>
    )
  }

  const byKey = new Map(configs.map((c) => [c.configKey, c]))

  return (
    <div className="space-y-6">
      {error && (
        <p role="alert" className="flex items-start gap-2 rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-sm text-destructive">
          <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" /> {error}
        </p>
      )}

      <p className="flex items-start gap-2 rounded-lg border border-border bg-muted/40 px-3 py-2 text-xs text-muted-foreground">
        <AlertCircle className="mt-0.5 size-3.5 shrink-0" aria-hidden="true" />
        {t("unsetNotice")}
      </p>

      {SETTING_GROUPS.map((group) => (
        <section key={group.id} className="rounded-2xl border border-border bg-card p-5">
          <h2 className="text-sm font-semibold text-foreground">{t(`group.${group.id}`)}</h2>
          <div className="mt-4 space-y-4">
            {group.keys.map((key) => {
              const entry = byKey.get(key)
              const current = edits[key] ?? entry?.configValue ?? ""
              const unsafe = looksUnsafe(current, key)
              const dirty = edits[key] !== undefined
              return (
                <div key={key}>
                  <label htmlFor={key} className="block text-sm font-medium text-foreground">
                    {key}
                  </label>
                  <p className="mt-0.5 text-xs text-muted-foreground">{entry?.description}</p>
                  <div className="mt-1.5 flex flex-col gap-2 sm:flex-row">
                    <input
                      id={key}
                      type="text"
                      value={current}
                      onChange={(e) => setEdits((prev) => ({ ...prev, [key]: e.target.value }))}
                      aria-invalid={unsafe}
                      className={`h-10 flex-1 rounded-lg border bg-background px-3 text-sm text-foreground outline-none focus:ring-2 focus:ring-ring ${
                        unsafe ? "border-destructive" : "border-border"
                      }`}
                    />
                    <Button
                      type="button"
                      size="sm"
                      disabled={!dirty || saving === key || unsafe}
                      onClick={() => save(key)}
                      className="h-10 gap-1"
                    >
                      {saving === key ? (
                        <Loader2 className="size-4 animate-spin" aria-hidden="true" />
                      ) : savedKey === key ? (
                        <CheckCircle className="size-4" aria-hidden="true" />
                      ) : (
                        <Save className="size-4" aria-hidden="true" />
                      )}
                      {t("save")}
                    </Button>
                  </div>
                  {unsafe && (
                    <p role="alert" className="mt-1 text-xs text-destructive">
                      {t("invalidValue")}
                    </p>
                  )}
                </div>
              )
            })}
          </div>
        </section>
      ))}
    </div>
  )
}

// ── legal content ───────────────────────────────────────────────────────────────────────

const LEGAL_TYPES = ["TERMS", "PRIVACY", "COOKIE", "SUPPORT_POLICY"] as const

function LegalTab() {
  const t = useTranslations("platformAdmin.publicSite")
  const [documents, setDocuments] = useState<LegalDocumentRecord[]>([])
  const [selected, setSelected] = useState<string | null>(null)
  const [draft, setDraft] = useState({ title: "", content: "", effectiveDate: "" })
  const [versions, setVersions] = useState<LegalVersionRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    platformAdminApi
      .listLegalDocuments()
      .then(setDocuments)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  useEffect(load, [load])

  const open = async (doc: LegalDocumentRecord) => {
    setSelected(doc.id)
    setError(null)
    setNotice(null)
    setDraft({
      title: doc.title,
      content: doc.content,
      effectiveDate: doc.effectiveDate ?? "",
    })
    try {
      setVersions(await platformAdminApi.legalDocumentVersions(doc.id))
    } catch (e) {
      setError(e instanceof Error ? e.message : t("loadFailed"))
    }
  }

  const create = async (type: string) => {
    setBusy(true)
    setError(null)
    try {
      const created = await platformAdminApi.createLegalDocument({
        type: type as "TERMS",
        title: t(`legalType.${type}`),
        content: t("newDocumentBody"),
      })
      await load()
      await open(created)
    } catch (e) {
      setError(e instanceof Error ? e.message : t("loadFailed"))
    } finally {
      setBusy(false)
    }
  }

  const act = async (run: () => Promise<unknown>, successKey: string) => {
    setBusy(true)
    setError(null)
    setNotice(null)
    try {
      await run()
      setNotice(t(successKey))
      await load()
      const refreshed = documents.find((d) => d.id === selected)
      if (refreshed) await open(refreshed)
    } catch (e) {
      setError(e instanceof Error ? e.message : t("loadFailed"))
    } finally {
      setBusy(false)
    }
  }

  if (loading) {
    return (
      <p className="flex items-center gap-2 py-10 text-sm text-muted-foreground">
        <Loader2 className="size-4 animate-spin" aria-hidden="true" /> {t("loading")}
      </p>
    )
  }

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <section className="space-y-3">
        <h2 className="text-sm font-semibold text-foreground">{t("documents")}</h2>
        {LEGAL_TYPES.map((type) => {
          const doc = documents.find((d) => d.type === type)
          return (
            <div key={type} className="rounded-2xl border border-border bg-card p-4">
              <p className="text-sm font-medium text-foreground">{t(`legalType.${type}`)}</p>
              {doc ? (
                <>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {t("draft")} v{doc.draftVersion} · {t("published")}{" "}
                    {doc.publishedVersion ? `v${doc.publishedVersion}` : t("notPublished")}
                  </p>
                  <div className="mt-3 flex gap-2">
                    <Button type="button" size="xs" variant="outline" onClick={() => open(doc)}>
                      {t("editDraft")}
                    </Button>
                  </div>
                </>
              ) : (
                <Button
                  type="button"
                  size="xs"
                  variant="outline"
                  disabled={busy}
                  onClick={() => create(type)}
                  className="mt-3"
                >
                  {t("createDraft")}
                </Button>
              )}
            </div>
          )
        })}
      </section>

      <section className="space-y-3 lg:col-span-2">
        {error && (
          <p role="alert" className="flex items-start gap-2 rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-sm text-destructive">
            <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" /> {error}
          </p>
        )}
        {notice && (
          <p role="status" className="flex items-start gap-2 rounded-lg border border-primary/30 bg-primary/5 px-3 py-2 text-sm text-foreground">
            <CheckCircle className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden="true" /> {notice}
          </p>
        )}

        {!selected ? (
          <p className="rounded-2xl border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
            {t("selectDocument")}
          </p>
        ) : (
          <div className="rounded-2xl border border-border bg-card p-5">
            <h2 className="text-sm font-semibold text-foreground">{t("draftHeading")}</h2>
            <p className="mt-1 text-xs text-muted-foreground">{t("draftExplainer")}</p>

            <label htmlFor="legal-title" className="mt-4 block text-sm font-medium text-foreground">
              {t("titleLabel")}
            </label>
            <input
              id="legal-title"
              type="text"
              value={draft.title}
              maxLength={200}
              onChange={(e) => setDraft((d) => ({ ...d, title: e.target.value }))}
              className="mt-1.5 h-10 w-full rounded-lg border border-border bg-background px-3 text-sm text-foreground outline-none focus:ring-2 focus:ring-ring"
            />

            <label htmlFor="legal-date" className="mt-4 block text-sm font-medium text-foreground">
              {t("effectiveDateLabel")}
            </label>
            <input
              id="legal-date"
              type="date"
              value={draft.effectiveDate}
              onChange={(e) => setDraft((d) => ({ ...d, effectiveDate: e.target.value }))}
              className="mt-1.5 h-10 rounded-lg border border-border bg-background px-3 text-sm text-foreground outline-none focus:ring-2 focus:ring-ring"
            />

            <label htmlFor="legal-content" className="mt-4 block text-sm font-medium text-foreground">
              {t("contentLabel")}
            </label>
            <p className="mt-0.5 text-xs text-muted-foreground">{t("contentFormatNote")}</p>
            <textarea
              id="legal-content"
              rows={14}
              value={draft.content}
              onChange={(e) => setDraft((d) => ({ ...d, content: e.target.value }))}
              className="mt-1.5 w-full rounded-lg border border-border bg-background px-3 py-2 font-mono text-xs text-foreground outline-none focus:ring-2 focus:ring-ring"
            />

            <div className="mt-4 flex flex-wrap gap-2">
              <Button
                type="button"
                size="sm"
                disabled={busy || !draft.title.trim() || !draft.content.trim()}
                onClick={() =>
                  act(
                    () =>
                      platformAdminApi.updateLegalDocument(selected, {
                        title: draft.title.trim(),
                        content: draft.content,
                        effectiveDate: draft.effectiveDate || undefined,
                      }),
                    "draftSaved",
                  )
                }
                className="gap-1"
              >
                <Save className="size-4" aria-hidden="true" /> {t("saveDraft")}
              </Button>
              <Button
                type="button"
                size="sm"
                variant="outline"
                disabled={busy || !draft.title.trim() || !draft.content.trim()}
                onClick={() =>
                  act(() => platformAdminApi.publishLegalDocument(selected), "publishSuccess")
                }
                className="gap-1"
              >
                <ExternalLink className="size-4" aria-hidden="true" /> {t("publish")}
              </Button>
            </div>

            <h3 className="mt-8 text-sm font-semibold text-foreground">{t("history")}</h3>
            <p className="mt-1 text-xs text-muted-foreground">{t("historyExplainer")}</p>
            <ul className="mt-3 space-y-2">
              {versions.length === 0 && (
                <li className="text-sm text-muted-foreground">{t("noVersions")}</li>
              )}
              {versions.map((version) => (
                <li
                  key={version.id}
                  className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-border px-3 py-2"
                >
                  <div>
                    <p className="text-sm font-medium text-foreground">v{version.version}</p>
                    <p className="text-xs text-muted-foreground">
                      {version.publishedAt} · {version.publishedBy}
                    </p>
                  </div>
                  <Button
                    type="button"
                    size="xs"
                    variant="ghost"
                    disabled={busy}
                    onClick={() =>
                      act(
                        () => platformAdminApi.revertLegalDocument(selected, version.version),
                        "reverted",
                      )
                    }
                  >
                    {t("restoreIntoDraft")}
                  </Button>
                </li>
              ))}
            </ul>
          </div>
        )}
      </section>
    </div>
  )
}

// ── contact inbox ───────────────────────────────────────────────────────────────────────

const STATUSES = ["NEW", "IN_PROGRESS", "AWAITING_RESPONSE", "RESOLVED", "CLOSED"] as const

function InboxTab() {
  const t = useTranslations("platformAdmin.publicSite")
  const [messages, setMessages] = useState<ContactMessageRecord[]>([])
  const [status, setStatus] = useState<string>("")
  const [selected, setSelected] = useState<ContactMessageRecord | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    platformAdminApi
      .listContactMessages({ status: status || undefined, size: 50 })
      .then((page) => setMessages(page.content ?? []))
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [status])

  useEffect(load, [load])

  const refreshSelected = async (id: string) => {
    try {
      setSelected(await platformAdminApi.getContactMessage(id))
    } catch {
      /* the list refresh below is enough if the detail fetch fails */
    }
  }

  const run = async (action: () => Promise<unknown>, id: string) => {
    setBusy(true)
    setError(null)
    try {
      await action()
      await load()
      await refreshSelected(id)
    } catch (e) {
      setError(e instanceof Error ? e.message : t("loadFailed"))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <section className="space-y-3">
        <label htmlFor="inbox-status" className="block text-sm font-medium text-foreground">
          {t("filterStatus")}
        </label>
        <select
          id="inbox-status"
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          className="h-10 rounded-lg border border-border bg-background px-3 text-sm text-foreground outline-none focus:ring-2 focus:ring-ring"
        >
          <option value="">{t("allStatuses")}</option>
          {STATUSES.map((option) => (
            <option key={option} value={option}>
              {t(`status.${option}`)}
            </option>
          ))}
        </select>

        {error && (
          <p role="alert" className="flex items-start gap-2 rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-sm text-destructive">
            <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" /> {error}
          </p>
        )}

        {loading ? (
          <p className="flex items-center gap-2 py-6 text-sm text-muted-foreground">
            <Loader2 className="size-4 animate-spin" aria-hidden="true" /> {t("loading")}
          </p>
        ) : messages.length === 0 ? (
          <p className="rounded-2xl border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
            {t("inboxEmpty")}
          </p>
        ) : (
          <ul className="space-y-2">
            {messages.map((message) => (
              <li key={message.id}>
                <button
                  type="button"
                  onClick={() => setSelected(message)}
                  aria-current={selected?.id === message.id ? "true" : undefined}
                  className={`w-full rounded-xl border px-3 py-3 text-left transition-colors ${
                    selected?.id === message.id
                      ? "border-primary bg-primary/5"
                      : "border-border bg-card hover:bg-muted/50"
                  }`}
                >
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <span className="text-sm font-medium text-foreground">{message.subject}</span>
                    <span className="text-xs text-muted-foreground">{message.reference}</span>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {message.name} · {message.email} · {message.category}
                  </p>
                  <div className="mt-2 flex flex-wrap gap-2 text-xs">
                    <span className="rounded-full bg-muted px-2 py-0.5">
                      {t(`status.${message.status}`)}
                    </span>
                    <span
                      className={`rounded-full px-2 py-0.5 ${
                        message.notificationStatus === "SENT"
                          ? "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300"
                          : "bg-amber-500/10 text-amber-700 dark:text-amber-300"
                      }`}
                    >
                      {t(`notification.${message.notificationStatus}`)}
                    </span>
                  </div>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section>
        {!selected ? (
          <p className="rounded-2xl border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
            {t("selectMessage")}
          </p>
        ) : (
          <div className="rounded-2xl border border-border bg-card p-5">
            <h2 className="text-base font-semibold text-foreground">{selected.subject}</h2>
            <p className="mt-1 text-xs text-muted-foreground">
              {selected.reference} · {selected.createdAt} · {selected.name} &lt;{selected.email}&gt;
            </p>

            <p className="mt-4 whitespace-pre-wrap rounded-lg border border-border bg-muted/40 p-3 text-sm text-foreground">
              {selected.message}
            </p>

            {/* Delivery truth, so support can see when an enquiry quietly failed to notify. */}
            <p className="mt-3 text-xs text-muted-foreground">
              {t(`notification.${selected.notificationStatus}`)}
              {selected.notificationError ? ` — ${selected.notificationError}` : ""}
            </p>

            <div className="mt-4 flex flex-wrap items-end gap-2">
              <div>
                <label htmlFor="msg-status" className="block text-xs font-medium text-foreground">
                  {t("filterStatus")}
                </label>
                <select
                  id="msg-status"
                  value={selected.status}
                  disabled={busy}
                  onChange={(e) =>
                    run(
                      () =>
                        platformAdminApi.updateContactMessageStatus(
                          selected.id,
                          e.target.value as "NEW",
                        ),
                      selected.id,
                    )
                  }
                  className="mt-1 h-9 rounded-lg border border-border bg-background px-2 text-sm text-foreground outline-none focus:ring-2 focus:ring-ring"
                >
                  {STATUSES.map((option) => (
                    <option key={option} value={option}>
                      {t(`status.${option}`)}
                    </option>
                  ))}
                </select>
              </div>
              <Button
                type="button"
                size="sm"
                variant="outline"
                disabled={busy}
                onClick={() =>
                  run(() => platformAdminApi.renotifyContactMessage(selected.id), selected.id)
                }
              >
                {t("retryNotification")}
              </Button>
            </div>
          </div>
        )}
      </section>
    </div>
  )
}