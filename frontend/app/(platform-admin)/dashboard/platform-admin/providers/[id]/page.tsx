"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { ArrowLeft, Globe, Loader2, Shield, Users, Mail, Phone, MapPin, AlertCircle, CheckCircle2, XCircle, MessageSquareWarning, Ban, RotateCcw } from "lucide-react"
import Link from "next/link"
import { useParams } from "next/navigation"
import { platformAdminApi, type ProviderGovernanceDetail } from "@/lib/platform-admin-api"

function verificationStyle(v: string | null): string {
  switch ((v ?? "").toUpperCase()) {
    case "VERIFIED": case "APPROVED": return "bg-green-50 text-green-700"
    case "PENDING": return "bg-amber-50 text-amber-700"
    case "CHANGES_REQUIRED": return "bg-orange-50 text-orange-700"
    case "REJECTED": return "bg-red-50 text-red-700"
    default: return "bg-muted text-muted-foreground"
  }
}

const COMPLIANCE_LABELS: Record<string, string> = {
  MISSING_CONTACT: "Missing contact information",
  SUSPENDED: "Suspended",
  VERIFICATION_PENDING: "Verification pending",
  NOT_VERIFIED: "Not verified",
  CHANGES_REQUESTED: "Changes requested by reviewer",
  NEVER_VERIFIED: "Never verified",
  NO_ADMINS: "No administrators assigned",
}

export default function ProviderDetailPage() {
  const t = useTranslations("platformAdmin");
  const ts = useTranslations("status");
  const { id } = useParams<{ id: string }>()
  const [provider, setProvider] = useState<ProviderGovernanceDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [flash, setFlash] = useState<string | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const [notes, setNotes] = useState("")

  const load = useCallback(() => {
    if (!id) return
    setLoading(true); setError(null)
    platformAdminApi.getProviderGovernance(id)
      .then(setProvider)
      .catch((e) => { setError(e.message); setProvider(null) })
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => { load() }, [load])

  const run = async (key: string, fn: () => Promise<unknown>, msg: string) => {
    setBusy(key); setError(null); setFlash(null)
    try { await fn(); setFlash(msg); load() }
    catch (e: any) { setError(e.message) }
    finally { setBusy(null) }
  }

  const review = (verificationId: string, status: string) => {
    if (status !== "APPROVED" && !notes.trim()) {
      setError("Notes are required when rejecting or requesting changes"); return
    }
    return run(`review-${verificationId}`,
      () => platformAdminApi.reviewProviderVerification(verificationId, status, notes || undefined),
      `Verification ${status.toLowerCase()}`)
  }

  const setLifecycle = (status: string) => {
    if (!window.confirm(`Change provider lifecycle to ${status}?`)) return
    return run("lifecycle", () => platformAdminApi.updateInstitutionLifecycle(id, status), `Provider ${status.toLowerCase()}`)
  }

  if (loading) return (
    <div className="flex items-center justify-center py-32"><Loader2 className="size-8 animate-spin text-primary" /></div>
  )

  if (error && !provider) return (
    <div className="mx-auto max-w-4xl py-10 text-center">
      <Globe className="mx-auto size-10 text-muted-foreground" />
      <p className="mt-4 text-sm text-muted-foreground">{t("providerDetail.providerNotFound")}</p>
      <p className="mt-1 text-xs text-red-600">{error}</p>
      <Link href="/dashboard/platform-admin/providers" className="mt-4 inline-flex items-center gap-2 text-sm text-primary hover:underline">
        <ArrowLeft className="size-4" /> {t("providerDetail.backToProviders2")}</Link>
    </div>
  )

  if (!provider) return null

  const pendingVerifications = provider.verificationHistory.filter((v) => ["PENDING", "CHANGES_REQUIRED"].includes((v.status ?? "").toUpperCase()))
  const services = (provider.enabledServices ?? "").split(/[,\s;]+/).map((s) => s.trim()).filter(Boolean)

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <Link href="/dashboard/platform-admin/providers" className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> {t("providerDetail.backToProviders2")}</Link>

      {flash && <div className="rounded-2xl border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">{flash}</div>}
      {error && <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center gap-2"><AlertCircle className="size-4 shrink-0" />{error}</div>}

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
          <div className="flex size-14 shrink-0 items-center justify-center rounded-2xl bg-primary/10">
            <Globe className="size-7 text-primary" />
          </div>
          <div className="flex-1">
            <h1 className="text-xl font-bold tracking-tight text-foreground">{provider.name}</h1>
            <p className="text-sm text-muted-foreground">{provider.code}{provider.city ? ` · ${provider.city}` : ""}{provider.region ? ` · ${provider.region}` : ""}</p>
            <div className="mt-3 flex flex-wrap items-center gap-2">
              <span className={`rounded-full px-3 py-1 text-xs font-semibold ${provider.isActive ? "bg-green-50 text-green-700" : "bg-red-50 text-red-700"}`}>
                {provider.isActive ? ts("active") : ts("inactive")}
              </span>
              <span className="rounded-full bg-muted px-3 py-1 text-xs font-medium">{provider.type}</span>
              <span className={`rounded-full px-3 py-1 text-xs font-semibold ${verificationStyle(provider.verificationStatus)}`}>
                {provider.verificationStatus ?? "NONE"}</span>
              {provider.status && <span className="rounded-full bg-muted px-3 py-1 text-xs font-medium">{provider.status}</span>}
            </div>
            {provider.complianceFlags.length > 0 && (
              <div className="mt-3 flex flex-wrap gap-1.5">
                {provider.complianceFlags.map((f) => (
                  <span key={f} className="rounded-full bg-amber-50 px-2.5 py-0.5 text-[11px] font-semibold text-amber-700">
                    {COMPLIANCE_LABELS[f] ?? f}</span>
                ))}
              </div>
            )}
          </div>
          <div className="flex flex-wrap gap-2">
            {provider.isActive ? (
              <button onClick={() => setLifecycle("SUSPENDED")} disabled={busy !== null} className="inline-flex items-center gap-1 rounded-xl border border-red-200 px-3 py-2 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50">
                {busy === "lifecycle" ? <Loader2 className="size-3.5 animate-spin" /> : <Ban className="size-3.5" />} Suspend</button>
            ) : (
              <button onClick={() => setLifecycle("ACTIVE")} disabled={busy !== null} className="inline-flex items-center gap-1 rounded-xl border border-green-200 px-3 py-2 text-xs font-medium text-green-600 hover:bg-green-50 disabled:opacity-50">
                {busy === "lifecycle" ? <Loader2 className="size-3.5 animate-spin" /> : <RotateCcw className="size-3.5" />} Reactivate</button>
            )}
          </div>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground mb-4">Identity & Registration</h2>
          <div className="space-y-3 text-sm">
            {provider.email && <div className="flex items-center gap-3"><Mail className="size-4 shrink-0 text-muted-foreground" /><span className="text-foreground">{provider.email}</span></div>}
            {provider.phone && <div className="flex items-center gap-3"><Phone className="size-4 shrink-0 text-muted-foreground" /><span className="text-foreground">{provider.phone}</span></div>}
            {(provider.address || provider.city || provider.region) && (
              <div className="flex items-center gap-3"><MapPin className="size-4 shrink-0 text-muted-foreground" /><span className="text-muted-foreground">{[provider.address, provider.city, provider.region, provider.country].filter(Boolean).join(", ")}</span></div>
            )}
            {provider.description && <p className="text-muted-foreground">{provider.description}</p>}
            <div className="flex justify-between border-t border-border pt-3"><span className="text-muted-foreground">Registered</span><span className="text-foreground">{provider.createdAt ? new Date(provider.createdAt).toLocaleDateString("en-GB") : "—"}</span></div>
            <div className="flex justify-between"><span className="text-muted-foreground">Verified</span><span className="text-foreground">{provider.approvedAt ? `${new Date(provider.approvedAt).toLocaleDateString("en-GB")} by ${provider.approvedBy ?? "—"}` : "—"}</span></div>
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground mb-4">Services</h2>
          {services.length === 0 && provider.serviceEntitlements.length === 0 ? (
            <p className="text-sm text-muted-foreground">No platform services configured for this provider.</p>
          ) : (
            <div className="space-y-2">
              {services.map((s) => (
                <div key={s} className="flex items-center justify-between rounded-lg border border-border px-3 py-2 text-sm">
                  <span className="font-medium text-foreground">{s}</span>
                  <span className="rounded-full bg-emerald-500/10 px-2 py-0.5 text-[11px] font-semibold text-emerald-700">Enabled</span>
                </div>
              ))}
              {provider.serviceEntitlements.map((q) => (
                <div key={q.id} className="flex items-center justify-between rounded-lg border border-border px-3 py-2 text-sm">
                  <span className="font-medium text-foreground">{q.serviceName ?? q.serviceCode ?? q.serviceId}</span>
                  <span className="text-xs text-muted-foreground">{q.status}{q.maxSeats ? ` · ${q.seatsUsed ?? 0}/${q.maxSeats} seats` : ""}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="flex items-center gap-2 text-base font-semibold text-foreground mb-4"><Users className="size-4" /> Provider Administrators ({provider.admins.length})</h2>
        {provider.admins.length === 0 ? (
          <p className="text-sm text-muted-foreground">No administrators assigned to this provider.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[520px] text-sm">
              <thead><tr className="border-b border-border text-left text-xs text-muted-foreground">
                <th className="py-2 pr-4 font-medium">Name</th><th className="py-2 pr-4 font-medium">Role</th><th className="py-2 pr-4 font-medium">Status</th>
              </tr></thead>
              <tbody className="divide-y divide-border">
                {provider.admins.map((a) => (
                  <tr key={a.userId}>
                    <td className="py-2.5 pr-4 font-medium text-foreground">{a.fullName ?? a.userId.slice(0, 8)}</td>
                    <td className="py-2.5 pr-4 text-muted-foreground">{a.membershipRole ?? a.userRole ?? "—"}</td>
                    <td className="py-2.5 pr-4"><span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${a.isActive ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"}`}>{a.isActive ? "Active" : "Inactive"}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="flex items-center gap-2 text-base font-semibold text-foreground mb-4"><Shield className="size-4" /> Verification</h2>
        {pendingVerifications.length > 0 && (
          <div className="mb-4 space-y-3">
            <input value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Reviewer notes (required to reject or request changes)…" className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
            {pendingVerifications.map((v) => (
              <div key={v.id} className="rounded-xl border border-amber-200 bg-amber-50/50 p-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="text-sm">
                    <span className="font-semibold text-foreground">{v.verificationType}</span>
                    <span className="ml-2 text-xs text-muted-foreground">submitted {v.submittedAt ? new Date(v.submittedAt).toLocaleString("en-GB") : "—"} by {v.submittedByName ?? v.submittedBy?.slice(0, 8) ?? "—"}</span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <button onClick={() => review(v.id, "APPROVED")} disabled={busy !== null} className="inline-flex items-center gap-1 rounded-lg bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50">
                      {busy === `review-${v.id}` ? <Loader2 className="size-3 animate-spin" /> : <CheckCircle2 className="size-3" />} Verify</button>
                    <button onClick={() => review(v.id, "CHANGES_REQUIRED")} disabled={busy !== null} className="inline-flex items-center gap-1 rounded-lg border border-orange-300 px-3 py-1.5 text-xs font-medium text-orange-700 hover:bg-orange-50 disabled:opacity-50">
                      <MessageSquareWarning className="size-3" /> Request changes</button>
                    <button onClick={() => review(v.id, "REJECTED")} disabled={busy !== null} className="inline-flex items-center gap-1 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50">
                      <XCircle className="size-3" /> Reject</button>
                  </div>
                </div>
                {v.notes && <p className="mt-2 text-xs text-muted-foreground">Notes: {v.notes}</p>}
                {v.documents && <p className="mt-1 font-mono text-[11px] text-muted-foreground">Evidence: {v.documents}</p>}
              </div>
            ))}
          </div>
        )}
        {provider.verificationHistory.length === 0 ? (
          <p className="text-sm text-muted-foreground">No verification records for this provider.</p>
        ) : (
          <ul className="space-y-2">
            {provider.verificationHistory.map((v) => (
              <li key={v.id} className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-muted/40 px-3 py-2 text-xs">
                <span className="font-semibold text-foreground">{v.verificationType}</span>
                <span className={`rounded-full px-2 py-0.5 font-semibold ${verificationStyle(v.status)}`}>{v.status}</span>
                <span className="text-muted-foreground">
                  {v.submittedAt ? new Date(v.submittedAt).toLocaleDateString("en-GB") : "—"}
                  {v.reviewedByName ? ` · reviewed by ${v.reviewedByName}` : ""}
                  {v.reviewedAt ? ` on ${new Date(v.reviewedAt).toLocaleDateString("en-GB")}` : ""}
                </span>
                {v.notes && <span className="w-full text-muted-foreground">“{v.notes}”</span>}
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-base font-semibold text-foreground mb-4">Recent audit</h2>
        {provider.recentAudit.length === 0 ? (
          <p className="text-sm text-muted-foreground">No audit records for this provider yet.</p>
        ) : (
          <ul className="space-y-1.5">
            {provider.recentAudit.map((a) => (
              <li key={a.id} className="rounded-lg bg-muted/40 px-3 py-2 text-xs">
                <span className="font-semibold text-foreground">{a.action}</span>
                <span className="text-muted-foreground"> · {a.entityType} · {a.performedBy ?? a.userId} · {a.createdAt ? new Date(a.createdAt).toLocaleString("en-GB") : ""}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
