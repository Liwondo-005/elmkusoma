"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { Globe, Loader2, AlertCircle, Search, RefreshCw, Eye, TriangleAlert } from "lucide-react"
import Link from "next/link"
import { platformAdminApi, type ProviderRegistryItem, type ProviderAttentionItem } from "@/lib/platform-admin-api"

const PROVIDER_TYPES = [
  "TRAINING_PROVIDER", "PROFESSIONAL_BODY", "COMPANY", "NGO",
  "GOVERNMENT", "CONTENT_PROVIDER", "EVENT_PROVIDER",
]

function verificationStyle(v: string | null): string {
  switch ((v ?? "").toUpperCase()) {
    case "VERIFIED": case "APPROVED": return "bg-green-50 text-green-700"
    case "PENDING": return "bg-amber-50 text-amber-700"
    case "CHANGES_REQUIRED": return "bg-orange-50 text-orange-700"
    case "REJECTED": return "bg-red-50 text-red-700"
    default: return "bg-muted text-muted-foreground"
  }
}

export default function ProvidersPage() {
  const t = useTranslations("platformAdmin");
  const ts = useTranslations("status");
  const [providers, setProviders] = useState<ProviderRegistryItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [typeFilter, setTypeFilter] = useState("")
  const [statusFilter, setStatusFilter] = useState("")
  const [verificationFilter, setVerificationFilter] = useState("")
  const [attention, setAttention] = useState<ProviderAttentionItem[]>([])
  const [metrics, setMetrics] = useState({ total: 0, verified: 0, pending: 0, suspended: 0 })

  const load = useCallback((p = 0) => {
    setLoading(true); setError(null)
    platformAdminApi.listProviderRegistry({
      search: search || undefined,
      type: typeFilter || undefined,
      status: statusFilter || undefined,
      verification: verificationFilter || undefined,
      page: p, size: 20,
    })
      .then((res) => { setProviders(res.content ?? []); setTotal(res.totalElements ?? 0); setPage(p) })
      .catch((e) => setError(e.message || t("providers.failedToLoadProviders")))
      .finally(() => setLoading(false))
  }, [search, typeFilter, statusFilter, verificationFilter])

  useEffect(() => { load(0) }, [load])
  useEffect(() => {
    platformAdminApi.getProviderAttention().then(setAttention).catch(() => setAttention([]))
    platformAdminApi.listProviderRegistry({ page: 0, size: 1000 })
      .then((res) => {
        const all = res.content ?? []
        setMetrics({
          total: res.totalElements ?? all.length,
          verified: all.filter((p) => ["VERIFIED", "APPROVED"].includes((p.verificationStatus ?? "").toUpperCase())).length,
          pending: all.filter((p) => (p.verificationStatus ?? "").toUpperCase() === "PENDING").length,
          suspended: all.filter((p) => !p.isActive || (p.status ?? "").toUpperCase() === "SUSPENDED").length,
        })
      })
      .catch(() => {})
  }, [])

  const counts = metrics

  const totalPages = Math.max(1, Math.ceil(total / 20))

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("providers.providerEcosystem")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("providers.organizationsContentProvidersAnd")}</p>
        </div>
        <button onClick={() => load(page)} className="inline-flex items-center gap-2 self-start rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted sm:self-center">
          <RefreshCw className="size-4" /> Refresh</button>
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" /> {error}
        </div>
      )}

      <div className="grid gap-3 sm:grid-cols-4">
        {[["Total providers", counts.total, "text-foreground"], ["Verified", counts.verified, "text-green-700"], ["Pending verification", counts.pending, "text-amber-700"], ["Suspended", counts.suspended, "text-red-600"]].map(([label, n, cls]) => (
          <div key={label as string} className="rounded-2xl border border-border bg-card p-4 shadow-xs">
            <p className="text-xs text-muted-foreground">{label}</p>
            <p className={`mt-1 text-2xl font-bold ${cls}`}>{n}</p>
          </div>
        ))}
      </div>

      {attention.length > 0 && (
        <div className="rounded-2xl border border-amber-200 bg-amber-50/60 p-4">
          <h2 className="flex items-center gap-2 text-sm font-semibold text-amber-900"><TriangleAlert className="size-4" /> Requires attention ({attention.length})</h2>
          <ul className="mt-2 space-y-1.5">
            {attention.slice(0, 8).map((a) => (
              <li key={`${a.providerId}-${a.category}`} className="text-xs text-amber-900">
                <Link href={a.actionUrl} className="font-semibold hover:underline">{a.providerName}</Link>
                <span className="text-amber-700"> — {a.title}: {a.description}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="flex flex-col gap-3 lg:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input type="text" placeholder="Search providers by name, code, or city…" value={search} onChange={(e) => setSearch(e.target.value)}
            className="w-full rounded-xl border border-border bg-card py-2 pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-primary/20" />
        </div>
        <div className="flex flex-wrap gap-2">
          <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)} className="rounded-xl border border-border bg-card px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/20">
            <option value="">All types</option>
            {PROVIDER_TYPES.map((ty) => <option key={ty} value={ty}>{ty}</option>)}
          </select>
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="rounded-xl border border-border bg-card px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/20">
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="SUSPENDED">Suspended</option>
            <option value="DEACTIVATED">Deactivated</option>
            <option value="ARCHIVED">Archived</option>
          </select>
          <select value={verificationFilter} onChange={(e) => setVerificationFilter(e.target.value)} className="rounded-xl border border-border bg-card px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/20">
            <option value="">All verifications</option>
            <option value="VERIFIED">Verified</option>
            <option value="PENDING">Pending</option>
            <option value="CHANGES_REQUIRED">Changes required</option>
            <option value="REJECTED">Rejected</option>
            <option value="NONE">None</option>
          </select>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-32"><Loader2 className="size-8 animate-spin text-primary" /></div>
      ) : providers.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border p-12 text-center">
          <Globe className="mx-auto size-10 text-muted-foreground" />
          <p className="mt-4 text-sm font-medium text-foreground">{t("providers.noProvidersRegisteredYet")}</p>
          <p className="mt-1 text-xs text-muted-foreground">{t("providers.providerAccountsWillAppear")}</p>
        </div>
      ) : (
        <>
          <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[760px]">
                <thead>
                  <tr className="border-b border-border bg-muted/50">
                    <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Provider</th>
                    <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Type</th>
                    <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Status</th>
                    <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Verification</th>
                    <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Admins</th>
                    <th className="px-5 py-3 text-right text-xs font-semibold text-muted-foreground uppercase tracking-wider">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {providers.map((p) => (
                    <tr key={p.id} className="hover:bg-muted/30 transition-colors">
                      <td className="px-5 py-3.5">
                        <p className="text-sm font-medium text-foreground">{p.name}</p>
                        <p className="text-xs text-muted-foreground">{p.code}{p.city ? ` · ${p.city}` : ""}</p>
                      </td>
                      <td className="px-5 py-3.5 text-xs text-muted-foreground">{p.type ?? "—"}</td>
                      <td className="px-5 py-3.5">
                        <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${p.isActive ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"}`}>
                          {p.isActive ? ts("active") : ts("inactive")}</span>
                      </td>
                      <td className="px-5 py-3.5">
                        <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${verificationStyle(p.verificationStatus)}`}>
                          {p.verificationStatus ?? "NONE"}</span>
                      </td>
                      <td className="px-5 py-3.5 text-sm text-muted-foreground">{p.adminCount ?? 0}</td>
                      <td className="px-5 py-3.5 text-right">
                        <Link href={`/dashboard/platform-admin/providers/${p.id}`}
                          className="inline-flex items-center gap-1 rounded-lg border border-border px-2.5 py-1.5 text-xs font-medium hover:bg-muted">
                          <Eye className="size-3.5" /> Govern
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          <div className="flex items-center justify-between text-sm text-muted-foreground">
            <span>Page {page + 1} of {totalPages} · {total} providers</span>
            <div className="flex gap-2">
              <button disabled={page <= 0} onClick={() => load(page - 1)} className="rounded-lg border border-border px-3 py-1.5 text-xs font-medium hover:bg-muted disabled:opacity-50">Prev</button>
              <button disabled={page + 1 >= totalPages} onClick={() => load(page + 1)} className="rounded-lg border border-border px-3 py-1.5 text-xs font-medium hover:bg-muted disabled:opacity-50">Next</button>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
