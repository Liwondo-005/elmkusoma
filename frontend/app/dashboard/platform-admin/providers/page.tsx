"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { Globe, Loader2, ChevronDown, ChevronUp, Gauge, Ban, CheckCircle2, AlertCircle, Users } from "lucide-react"
import { platformAdminApi, type UserSummary, type ProviderQuota } from "@/lib/platform-admin-api"

function SponsorRow({ providerId, q, onDone }: { providerId: string; q: ProviderQuota; onDone: () => void }) {
  const [open, setOpen] = useState(false)
  const [userId, setUserId] = useState("")
  const [studentId, setStudentId] = useState("")
  const [busy, setBusy] = useState(false)
  const [msg, setMsg] = useState<string | null>(null)

  const grant = async () => {
    if (!userId || !studentId) { setMsg("User ID and Student ID are required"); return }
    setBusy(true); setMsg(null)
    try {
      const res = await platformAdminApi.grantSponsorSeats({ providerId, serviceId: q.serviceId, userId, studentId, seats: 1 })
      setMsg(t("providers.grantedSeatsUsed", { p0: res.seatsUsed, p1: res.maxSeats ?? "∞" }))
      setUserId(""); setStudentId("")
      onDone()
    } catch (e: any) {
      setMsg(e.message || t("providers.failedToGrantSeat"))
    } finally { setBusy(false) }
  }

  if (!open) {
    return (
      <button onClick={() => setOpen(true)} className="ml-4 inline-flex items-center gap-1 rounded-lg border border-border px-2 py-1 text-[11px] font-medium hover:bg-muted">
        <Users className="size-3" /> {t("providers.sponsorSeat")}</button>
    )
  }
  return (
    <span className="ml-4 inline-flex flex-wrap items-center gap-1.5 align-middle">
      <input value={userId} onChange={(e) => setUserId(e.target.value)} placeholder={t("providers.userId")} className="w-44 rounded-md border border-border bg-background px-2 py-1 text-[11px] outline-none focus:ring-1 focus:ring-ring" />
      <input value={studentId} onChange={(e) => setStudentId(e.target.value)} placeholder={t("providers.studentId")} className="w-44 rounded-md border border-border bg-background px-2 py-1 text-[11px] outline-none focus:ring-1 focus:ring-ring" />
      <button onClick={grant} disabled={busy} className="rounded-md bg-primary px-2 py-1 text-[11px] font-semibold text-primary-foreground disabled:opacity-50">{busy ? "…" : t("providers.grant")}</button>
      <button onClick={() => { setOpen(false); setMsg(null) }} className="rounded-md border border-border px-2 py-1 text-[11px]">{tc("cancel")}</button>
      {msg && <span className="text-[11px] text-muted-foreground">{msg}</span>}
    </span>
  )
}

function QuotaPanel({ providerId }: { providerId: string }) {
  const [quotas, setQuotas] = useState<ProviderQuota[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    platformAdminApi.getProviderQuotas(providerId)
      .then(setQuotas)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [providerId])

  useEffect(() => { load() }, [load])

  if (loading) return <div className="flex items-center gap-2 px-4 py-3 text-xs text-muted-foreground"><Loader2 className="size-3.5 animate-spin" /> {t("providers.loadingQuotas")}</div>
  if (error) return <div className="px-4 py-3 text-xs text-red-600">{error}</div>
  if (!quotas || quotas.length === 0) return <div className="px-4 py-3 text-xs text-muted-foreground">{t("providers.noServiceEntitlementsFor")}</div>

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-xs">
        <thead>
          <tr className="border-b border-border bg-muted/40">
            <th className="px-4 py-2 text-left font-semibold text-muted-foreground">{t("providers.service")}</th>
            <th className="px-4 py-2 text-left font-semibold text-muted-foreground">{t("providers.status")}</th>
            <th className="px-4 py-2 text-left font-semibold text-muted-foreground">{t("providers.seats")}</th>
            <th className="px-4 py-2 text-left font-semibold text-muted-foreground">{t("providers.expires")}</th>
          </tr>
        </thead>
        <tbody>
          {quotas.map((q) => {
            const pct = q.maxSeats ? Math.round(((q.seatsUsed ?? 0) / q.maxSeats) * 100) : null
            const near = pct !== null && pct >= 80
            return (
              <tr key={q.id} className="border-b border-border last:border-0">
                <td className="px-4 py-2 font-medium text-foreground">{q.serviceName ?? q.serviceCode ?? q.serviceId}</td>
                <td className="px-4 py-2"><span className={`rounded-full px-2 py-0.5 font-medium ${q.status === "ACTIVE" ? "bg-emerald-500/10 text-emerald-700" : "bg-amber-500/10 text-amber-700"}`}>{q.status}</span></td>
                <td className="px-4 py-2">
                  {q.maxSeats ? (
                    <span className={near ? "font-semibold text-amber-600" : "text-muted-foreground"}>{near ? t("providers.quotaNearLimit", { used: q.seatsUsed ?? 0, max: q.maxSeats }) : t("providers.quotaUsage", { used: q.seatsUsed ?? 0, max: q.maxSeats })}</span>
                  ) : (
                    <span className="text-muted-foreground">{t("providers.unlimited", { p0: q.seatsUsed ?? 0 })}</span>
                  )}
                  <SponsorRow providerId={providerId} q={q} onDone={load} />
                </td>
                <td className="px-4 py-2 text-muted-foreground">{q.expiresAt ? new Date(q.expiresAt).toLocaleDateString() : "—"}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

export default function ProvidersPage() {
  const t = useTranslations("platformAdmin");
  const tc = useTranslations("common");
  const ts = useTranslations("status");
  const [providers, setProviders] = useState<UserSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [expanded, setExpanded] = useState<string | null>(null)
  const [toggling, setToggling] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    platformAdminApi.listUsers(0, 100, "PROVIDER_ADMIN")
      .then((res) => setProviders(res.content))
      .catch((e) => setError(e.message || t("providers.failedToLoadProviders")))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  const toggleStatus = async (p: UserSummary) => {
    setToggling(p.id)
    try {
      const updated = await platformAdminApi.updateUserStatus(p.id, !p.isActive)
      setProviders((prev) => prev.map((x) => (x.id === p.id ? { ...x, isActive: updated.isActive } : x)))
    } catch (e: any) {
      setError(e.message || t("providers.failedToUpdateProvider"))
    } finally { setToggling(null) }
  }

  if (loading) return (
    <div className="flex items-center justify-center py-32">
      <Loader2 className="size-8 animate-spin text-primary" />
    </div>
  )

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("providers.providerEcosystem")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("providers.organizationsContentProvidersAnd")}</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" /> {error}
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="text-base font-semibold text-foreground">{t("providers.aboutProviders")}</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          {t("providers.providersAreOrganizationsBeyond")}</p>
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          {["Banks & Financial", "Companies & Corporate", "NGOs & Non-Profit", "Government Institutions", "Training Organizations", "Content Creators"].map((type) => (
            <div key={type} className="flex items-center gap-2 rounded-lg border border-border px-3 py-2">
              <Globe className="size-3.5 text-muted-foreground" />
              <span className="text-sm text-foreground">{type}</span>
            </div>
          ))}
        </div>
      </div>

      <div>
        <h2 className="text-base font-semibold text-foreground mb-4">{t("providers.registeredProviderAdmins")}</h2>
        {providers.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-border p-12 text-center">
            <Globe className="mx-auto size-10 text-muted-foreground" />
            <p className="mt-4 text-sm font-medium text-foreground">{t("providers.noProvidersRegisteredYet")}</p>
            <p className="mt-1 text-xs text-muted-foreground">{t("providers.providerAccountsWillAppear")}</p>
          </div>
        ) : (
          <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">{t("providers.name")}</th>
                  <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">{t("providers.email")}</th>
                  <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">{t("providers.status2")}</th>
                  <th className="px-5 py-3 text-right text-xs font-semibold text-muted-foreground uppercase tracking-wider">{t("providers.actions")}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {providers.map((p) => (
                  <ProviderRow
                    key={p.id}
                    provider={p}
                    expanded={expanded === p.id}
                    onToggleExpand={() => setExpanded(expanded === p.id ? null : p.id)}
                    onToggleStatus={() => toggleStatus(p)}
                    toggling={toggling === p.id}
                  />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

function ProviderRow({ provider, expanded, onToggleExpand, onToggleStatus, toggling }: {
  provider: UserSummary
  expanded: boolean
  onToggleExpand: () => void
  onToggleStatus: () => void
  toggling: boolean
}) {
  return (
    <>
      <tr className="hover:bg-muted/30 transition-colors">
        <td className="px-5 py-3.5 text-sm font-medium text-foreground">{provider.firstName} {provider.lastName}</td>
        <td className="px-5 py-3.5 text-sm text-muted-foreground">{provider.email}</td>
        <td className="px-5 py-3.5">
          <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${
            provider.isActive ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"
          }`}>{provider.isActive ? ts("active") : ts("inactive")}</span>
        </td>
        <td className="px-5 py-3.5 text-right">
          <div className="inline-flex gap-1.5">
            <button
              onClick={onToggleExpand}
              className="inline-flex items-center gap-1 rounded-lg border border-border px-2.5 py-1.5 text-xs font-medium hover:bg-muted"
            >
              <Gauge className="size-3.5" /> {t("providers.quotas")}{expanded ? <ChevronUp className="size-3" /> : <ChevronDown className="size-3" />}
            </button>
            <button
              onClick={onToggleStatus}
              disabled={toggling}
              className={`inline-flex items-center gap-1 rounded-lg border px-2.5 py-1.5 text-xs font-medium disabled:opacity-50 ${
                provider.isActive
                  ? "border-red-200 text-red-600 hover:bg-red-50 dark:border-red-800"
                  : "border-green-200 text-green-600 hover:bg-green-50 dark:border-green-800"
              }`}
            >
              {toggling ? <Loader2 className="size-3.5 animate-spin" /> : provider.isActive ? <Ban className="size-3.5" /> : <CheckCircle2 className="size-3.5" />}
              {provider.isActive ? t("providers.suspend") : t("providers.reactivate")}
            </button>
          </div>
        </td>
      </tr>
      {expanded && (
        <tr className="bg-muted/20">
          <td colSpan={4} className="px-0 py-0">
            <QuotaPanel providerId={provider.id} />
          </td>
        </tr>
      )}
    </>
  )
}
