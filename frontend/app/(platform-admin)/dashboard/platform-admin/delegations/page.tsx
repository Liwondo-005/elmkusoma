"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { Users, Loader2, ShieldOff, Plus, Search, RefreshCw, AlertCircle, Eye, CheckCircle2, XCircle, CalendarClock } from "lucide-react"
import { platformAdminApi, type DelegationSummary, type DelegationDetail, type AuditLogEntry } from "@/lib/platform-admin-api"
import { DelegationFormModal } from "@/components/platform-admin/delegation-form-modal"
import { formatDateTime, formatDate } from "@/lib/format-date"
import { useLocale } from "@/hooks/use-locale"

function statusStyle(status: string): string {
  switch (status) {
    case "ACTIVE": return "bg-green-50 text-green-700"
    case "PENDING_APPROVAL": return "bg-amber-50 text-amber-700"
    case "EXPIRED": return "bg-muted text-muted-foreground"
    case "REVOKED": case "REJECTED": return "bg-red-50 text-red-700"
    default: return "bg-muted text-muted-foreground"
  }
}

function DelegationDetailPanel({ id, onChanged, onClose }: { id: string; onChanged: () => void; onClose: () => void }) {
  const t = useTranslations("platformAdmin");
  const tc = useTranslations("common");
  const locale = useLocale()
  const [detail, setDetail] = useState<DelegationDetail | null>(null)
  const [audit, setAudit] = useState<AuditLogEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const [reason, setReason] = useState("")
  const [newExpiry, setNewExpiry] = useState("")

  const load = useCallback(() => {
    setLoading(true); setError(null)
    Promise.all([
      platformAdminApi.getDelegation(id),
      platformAdminApi.getAuditLogs(0, 20, undefined, "DELEGATION", id)
        .then((r) => r.content)
        .catch(() => [] as AuditLogEntry[]),
    ])
      .then(([d, a]) => { setDetail(d); setAudit(a) })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => { load() }, [load])

  const run = async (action: string, fn: () => Promise<unknown>) => {
    setBusy(action); setError(null)
    try { await fn(); onChanged(); load() }
    catch (e: any) { setError(e.message) }
    finally { setBusy(null) }
  }

  const approve = () => run("approve", () => platformAdminApi.approveDelegation(id, reason || undefined))
  const reject = () => {
    if (!reason.trim()) { setError(t("shared.delegations.errRejectionReasonRequired")); return }
    return run("reject", () => platformAdminApi.rejectDelegation(id, reason))
  }
  const revoke = () => {
    if (!reason.trim()) { setError(t("shared.delegations.errRevocationReasonRequired")); return }
    return run("revoke", () => platformAdminApi.revokeDelegation(id, reason))
  }
  const extend = () => {
    if (!newExpiry) { setError(t("shared.delegations.errSelectNewExpiry")); return }
    return run("extend", () => platformAdminApi.extendDelegation(id, new Date(newExpiry).toISOString(), reason || undefined))
  }

  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-base font-semibold text-foreground">{t("shared.delegations.detail")}</h2>
        <button onClick={onClose} className="rounded-lg border border-border px-3 py-1.5 text-xs font-medium hover:bg-muted">{tc("close")}</button>
      </div>
      {loading ? (
        <div className="flex justify-center py-10"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : error && !detail ? (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
      ) : detail ? (
        <div className="space-y-4">
          {error && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}
          <div className="flex flex-wrap items-center gap-2">
            <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${statusStyle(detail.status)}`}>{detail.status}</span>
            <span className="rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium">{detail.authority}</span>
            <span className="rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium">{detail.scope}</span>
            {detail.currentlyEffective && <span className="rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-semibold text-green-700">{t("shared.delegations.effectiveNow")}</span>}
          </div>
          <dl className="grid gap-2 text-sm sm:grid-cols-2">
            <div><dt className="text-xs text-muted-foreground">{t("shared.delegations.labelDelegator")}</dt><dd className="font-medium text-foreground">{detail.delegatorName ?? detail.delegatorId} <span className="text-xs text-muted-foreground">{detail.delegatorEmail}</span></dd></div>
            <div><dt className="text-xs text-muted-foreground">{t("shared.delegations.labelDelegate")}</dt><dd className="font-medium text-foreground">{detail.delegateName ?? detail.delegateId} <span className="text-xs text-muted-foreground">{detail.delegateEmail}</span></dd></div>
            <div><dt className="text-xs text-muted-foreground">{t("shared.delegations.labelPermissions")}</dt><dd className="font-mono text-xs text-foreground">{detail.permissions}</dd></div>
            <div><dt className="text-xs text-muted-foreground">{t("shared.delegations.labelValid")}</dt><dd className="text-foreground">{detail.startsAt ? formatDateTime(detail.startsAt, locale) : "—"} → {detail.expiresAt ? formatDateTime(detail.expiresAt, locale) : t("delegationForm.validNoExpiry")}</dd></div>
            {detail.resourceNames?.length > 0 && <div className="sm:col-span-2"><dt className="text-xs text-muted-foreground">{t("shared.delegations.labelScopedResources")}</dt><dd className="text-foreground">{detail.resourceNames.join(", ")}</dd></div>}
            {detail.reason && <div className="sm:col-span-2"><dt className="text-xs text-muted-foreground">{t("shared.delegations.labelReason")}</dt><dd className="text-foreground">{detail.reason}</dd></div>}
            {detail.notes && <div className="sm:col-span-2"><dt className="text-xs text-muted-foreground">{t("shared.delegations.labelNotes")}</dt><dd className="whitespace-pre-wrap text-foreground">{detail.notes}</dd></div>}
            {detail.approvedBy && <div><dt className="text-xs text-muted-foreground">{t("shared.delegations.labelApprovedBy")}</dt><dd className="text-foreground">{detail.approvedByName ?? detail.approvedBy} · {detail.approvedAt ? formatDateTime(detail.approvedAt, locale) : ""}</dd></div>}
            {detail.rejectionReason && <div><dt className="text-xs text-muted-foreground">{t("shared.delegations.labelRejection")}</dt><dd className="text-foreground">{detail.rejectionReason}</dd></div>}
            {detail.revokedBy && <div><dt className="text-xs text-muted-foreground">{t("shared.delegations.labelRevokedBy")}</dt><dd className="text-foreground">{detail.revokedByName ?? detail.revokedBy} · {detail.revokedAt ? formatDateTime(detail.revokedAt, locale) : ""}{detail.revocationReason ? ` — ${detail.revocationReason}` : ""}</dd></div>}
          </dl>

          {(detail.status === "PENDING_APPROVAL" || detail.status === "ACTIVE") && (
            <div className="space-y-2 rounded-xl border border-border p-3">
              <input value={reason} onChange={(e) => setReason(e.target.value)} placeholder={t("shared.delegations.phReason")} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
              <div className="flex flex-wrap gap-2">
                {detail.status === "PENDING_APPROVAL" && (
                  <>
                    <button onClick={approve} disabled={busy !== null} className="inline-flex items-center gap-1 rounded-lg bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50">
                      {busy === "approve" ? <Loader2 className="size-3 animate-spin" /> : <CheckCircle2 className="size-3" />} {t("shared.delegations.approve")}</button>
                    <button onClick={reject} disabled={busy !== null} className="inline-flex items-center gap-1 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50">
                      {busy === "reject" ? <Loader2 className="size-3 animate-spin" /> : <XCircle className="size-3" />} {t("shared.delegations.reject")}</button>
                  </>
                )}
                {detail.status === "ACTIVE" && (
                  <button onClick={revoke} disabled={busy !== null} className="inline-flex items-center gap-1 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50">
                    {busy === "revoke" ? <Loader2 className="size-3 animate-spin" /> : <ShieldOff className="size-3" />} {t("delegations.revoke")}</button>
                )}
              </div>
              {detail.status === "ACTIVE" && (
                <div className="flex flex-wrap items-center gap-2 pt-1">
                  <input type="datetime-local" value={newExpiry} onChange={(e) => setNewExpiry(e.target.value)} className="rounded-lg border border-border bg-background px-3 py-1.5 text-xs outline-none focus:ring-2 focus:ring-ring" />
                  <button onClick={extend} disabled={busy !== null} className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-medium hover:bg-muted disabled:opacity-50">
                    {busy === "extend" ? <Loader2 className="size-3 animate-spin" /> : <CalendarClock className="size-3" />} {t("shared.delegations.extend")}</button>
                </div>
              )}
            </div>
          )}

          <div>
            <h3 className="text-sm font-semibold text-foreground">{t("shared.delegations.auditTrail")}</h3>
            {audit.length === 0 ? (
              <p className="mt-1 text-xs text-muted-foreground">{t("shared.delegations.noAuditRecords")}</p>
            ) : (
              <ul className="mt-2 space-y-1.5">
                {audit.map((a) => (
                  <li key={a.id} className="rounded-lg bg-muted/40 px-3 py-2 text-xs">
                    <span className="font-semibold text-foreground">{a.action}</span>
                    <span className="text-muted-foreground"> · {a.performedBy ?? a.userId} · {a.createdAt ? formatDateTime(a.createdAt, locale) : ""}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      ) : null}
    </div>
  )
}

export default function DelegationsPage() {
  const t = useTranslations("platformAdmin");
  const locale = useLocale()
  const [delegations, setDelegations] = useState<DelegationSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [flash, setFlash] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [statusFilter, setStatusFilter] = useState("")
  const [modalOpen, setModalOpen] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true); setError(null)
    platformAdminApi.listDelegations()
      .then(setDelegations)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  const filtered = delegations.filter((d) => {
    if (statusFilter && d.status !== statusFilter) return false
    if (search) {
      const q = search.toLowerCase()
      const hay = `${d.delegatorName ?? ""} ${d.delegateName ?? ""} ${d.authority ?? ""} ${d.scope} ${d.permissions}`.toLowerCase()
      if (!hay.includes(q)) return false
    }
    return true
  })

  const counts = {
    active: delegations.filter((d) => d.status === "ACTIVE").length,
    pending: delegations.filter((d) => d.status === "PENDING_APPROVAL").length,
    expired: delegations.filter((d) => d.status === "EXPIRED").length,
    revoked: delegations.filter((d) => d.status === "REVOKED" || d.status === "REJECTED").length,
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("delegations.adminDelegations")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("delegations.manageDelegatedAdministrativeAccess")}</p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> {t("shared.common.refresh")}</button>
          <button onClick={() => setModalOpen(true)} className="inline-flex items-center gap-2 rounded-xl bg-primary px-3 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
            <Plus className="size-4" /> {t("delegationForm.title")}
          </button>
        </div>
      </div>

      {flash && (
        <div className="rounded-2xl border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">{flash}</div>
      )}
      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center gap-2">
          <AlertCircle className="size-4" />{error}
        </div>
      )}

      <div className="grid gap-3 sm:grid-cols-4">
        {[[t("shared.common.active"), counts.active, "text-green-700"], [t("shared.delegations.pendingApproval"), counts.pending, "text-amber-700"], [t("shared.delegations.expired"), counts.expired, "text-muted-foreground"], [t("shared.delegations.revokedRejected"), counts.revoked, "text-red-600"]].map(([label, n, cls]) => (
          <div key={label as string} className="rounded-2xl border border-border bg-card p-4 shadow-xs">
            <p className="text-xs text-muted-foreground">{label}</p>
            <p className={`mt-1 text-2xl font-bold ${cls}`}>{n}</p>
          </div>
        ))}
      </div>

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input type="text" placeholder={t("shared.delegations.searchByPersonAuthorityScope")} value={search} onChange={(e) => setSearch(e.target.value)}
            className="w-full rounded-xl border border-border bg-card py-2 pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-primary/20" />
        </div>
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="rounded-xl border border-border bg-card px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/20">
          <option value="">{t("shared.common.allStatuses")}</option>
          <option value="ACTIVE">{t("shared.common.active")}</option>
          <option value="PENDING_APPROVAL">{t("shared.delegations.pendingApproval")}</option>
          <option value="EXPIRED">{t("shared.delegations.expired")}</option>
          <option value="REVOKED">{t("certificates.filterRevoked")}</option>
          <option value="REJECTED">{t("shared.common.rejected")}</option>
        </select>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : filtered.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-10 text-center">
          <Users className="mx-auto size-10 text-muted-foreground/50" />
          <p className="mt-3 text-sm font-medium text-foreground">{delegations.length === 0 ? t("delegations.noDelegationsConfigured") : t("shared.delegations.noMatchFilters")}</p>
          <p className="mt-1 text-xs text-muted-foreground">{t("shared.delegations.noAuthorityDelegatedYet")}</p>
          {delegations.length === 0 && (
            <button onClick={() => setModalOpen(true)} className="mt-4 inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
              <Plus className="size-4" /> {t("delegationForm.title")}
            </button>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((d) => (
            <div key={d.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="text-sm font-semibold text-foreground">{d.delegateName ?? d.delegateId?.slice(0, 8)}</p>
                    <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${statusStyle(d.status)}`}>{d.status}</span>
                    {d.authority && <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium">{d.authority}</span>}
                    <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium">{d.scope}</span>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">{t("shared.delegations.fromExpires", { p0: d.delegatorName ?? d.delegatorId?.slice(0, 8), p1: d.expiresAt ? formatDate(d.expiresAt, locale) : t("delegations.never") })}</p>
                </div>
                <button onClick={() => setSelectedId(d.id)} className="inline-flex items-center gap-1 self-start rounded-xl border border-border px-3 py-1.5 text-xs font-medium hover:bg-muted sm:self-center">
                  <Eye className="size-3" /> {t("shared.delegations.viewAndManage")}</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {selectedId && (
        <DelegationDetailPanel id={selectedId} onChanged={load} onClose={() => setSelectedId(null)} />
      )}

      <DelegationFormModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSaved={(msg) => { setFlash(msg); load() }}
      />
    </div>
  )
}
