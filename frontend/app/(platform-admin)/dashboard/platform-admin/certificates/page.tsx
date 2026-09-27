"use client"

import { useTranslations } from "next-intl"
import { useEffect, useState, useCallback } from "react"
import {
  Award, Loader2, Hash, Ban, AlertCircle, Search, LayoutTemplate, Users,
  BarChart3, Eye, X, Filter,
} from "lucide-react"
import {
  platformAdminApi,
  type CertificateSummary,
  type PageResponse,
} from "@/lib/platform-admin-api"
import { CertificateDetailDrawer } from "@/components/platform-admin/certificate-detail-drawer"
import { CertificateOverviewSection } from "@/components/platform-admin/certificate-overview-section"
import { CertificateTemplatesSection } from "@/components/platform-admin/certificate-templates-section"
import { CertificateSignatoriesSection } from "@/components/platform-admin/certificate-signatories-section"

const PAGE_SIZE = 20

type TabKey = "overview" | "certificates" | "templates" | "signatories"

interface CertificateFilters {
  search: string
  status: string
  type: string
  institutionId: string
  from: string
  to: string
}

const EMPTY_FILTERS: CertificateFilters = {
  search: "",
  status: "",
  type: "",
  institutionId: "",
  from: "",
  to: "",
}

interface InstitutionOption {
  id: string
  name: string
}

const CERT_TYPES = ["COMPLETION", "ACHIEVEMENT", "PARTICIPATION", "TRANSCRIPT"]

export default function CertificatesPage() {
  const t = useTranslations("platformAdmin")
  const tc = useTranslations("common")
  const [tab, setTab] = useState<TabKey>("certificates")
  const [data, setData] = useState<PageResponse<CertificateSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [revoking, setRevoking] = useState<string | null>(null)
  const [revokePrompt, setRevokePrompt] = useState<{ id: string; reason: string } | null>(null)
  const [filters, setFilters] = useState<CertificateFilters>(EMPTY_FILTERS)
  const [draftFilters, setDraftFilters] = useState<CertificateFilters>(EMPTY_FILTERS)
  const [institutions, setInstitutions] = useState<InstitutionOption[]>([])
  const [detailId, setDetailId] = useState<string | null>(null)

  const hasFilters = Object.values(filters).some((v) => v !== "")

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await platformAdminApi.listCertificates(page, PAGE_SIZE, {
        search: filters.search.trim() || undefined,
        status: filters.status || undefined,
        type: filters.type || undefined,
        institutionId: filters.institutionId || undefined,
        from: filters.from || undefined,
        to: filters.to || undefined,
      })
      setData(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("certificates.failedToLoadCertificates"))
    } finally {
      setLoading(false)
    }
  }, [page, filters])

  useEffect(() => {
    loadData()
  }, [loadData])

  useEffect(() => {
    platformAdminApi
      .listInstitutions(0, 100)
      .then((res) => setInstitutions(res.content.map((i) => ({ id: i.id, name: i.name }))))
      .catch(() => setInstitutions([]))
  }, [])

  const applyFilters = () => {
    setPage(0)
    setFilters({ ...draftFilters, search: draftFilters.search.trim() })
  }

  const clearFilters = () => {
    setDraftFilters(EMPTY_FILTERS)
    setPage(0)
    setFilters(EMPTY_FILTERS)
  }

  const revoke = async () => {
    if (!revokePrompt) return
    setRevoking(revokePrompt.id); setError(null)
    try {
      await platformAdminApi.revokeCertificatePlatform(revokePrompt.id, revokePrompt.reason.trim() || undefined)
      setRevokePrompt(null)
      await loadData()
    } catch (e) {
      setError(e instanceof Error ? e.message : t("certificates.failedToRevokeCertificate"))
    } finally { setRevoking(null) }
  }

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "ISSUED":
        return <span className="inline-block rounded-full bg-green-500/10 px-2.5 py-0.5 text-xs font-medium text-green-600">ISSUED</span>
      case "DRAFT":
        return <span className="inline-block rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground">DRAFT</span>
      case "REVOKED":
        return <span className="inline-block rounded-full bg-red-500/10 px-2.5 py-0.5 text-xs font-medium text-red-600">REVOKED</span>
      default:
        return <span className="inline-block rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground">{status}</span>
    }
  }

  const formatDate = (d: string) => new Date(d).toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" })

  const tabs: { key: TabKey; label: string; icon: typeof Award }[] = [
    { key: "overview", label: t("certificates.tabOverview"), icon: BarChart3 },
    { key: "certificates", label: t("certificates.certificates"), icon: Award },
    { key: "templates", label: t("certificates.tabTemplates"), icon: LayoutTemplate },
    { key: "signatories", label: t("certificates.tabSignatories"), icon: Users },
  ]

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("certificates.certificates")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {t("certificates.subtitle")}
        </p>
      </div>

      {/* Tabs */}
      <div className="flex flex-wrap gap-1 rounded-2xl border border-border bg-muted/50 p-1">
        {tabs.map((tabItem) => (
          <button
            key={tabItem.key}
            onClick={() => setTab(tabItem.key)}
            className={`inline-flex flex-1 items-center justify-center gap-2 rounded-xl px-4 py-2 text-sm font-medium transition-colors ${
              tab === tabItem.key
                ? "bg-card text-foreground shadow-xs"
                : "text-muted-foreground hover:text-foreground"
            }`}
          >
            <tabItem.icon className="size-4" />
            {tabItem.label}
          </button>
        ))}
      </div>

      {tab === "overview" && <CertificateOverviewSection />}

      {tab === "templates" && <CertificateTemplatesSection />}

      {tab === "signatories" && <CertificateSignatoriesSection />}

      {tab === "certificates" && (
        <>
          {error && (
            <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive flex items-center gap-2"><AlertCircle className="size-4" />{error}</div>
          )}

          {revokePrompt && (
            <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4 space-y-3">
              <p className="text-sm font-semibold text-destructive">{t("certificates.revokeCertificatePlatformAction")}</p>
              <p className="text-sm text-muted-foreground">
                {t("certificates.revokeDescription")}
              </p>
              <input
                autoFocus
                value={revokePrompt.reason}
                onChange={(e) => setRevokePrompt({ ...revokePrompt, reason: e.target.value })}
                placeholder={t("certificates.reasonRequiredForAudit")}
                className="w-full rounded-xl border border-destructive/30 bg-background px-4 py-2 text-sm outline-none focus:ring-2 focus:ring-destructive/30"
              />
              <div className="flex gap-2">
                <button onClick={revoke} disabled={revoking === revokePrompt.id || !revokePrompt.reason.trim()}
                  className="inline-flex items-center gap-2 rounded-xl bg-destructive px-4 py-2 text-sm font-semibold text-destructive-foreground hover:opacity-90 disabled:opacity-50">
                  {revoking === revokePrompt.id ? <Loader2 className="size-4 animate-spin" /> : <Ban className="size-4" />} {t("certificates.confirmRevoke")}
                </button>
                <button onClick={() => setRevokePrompt(null)} className="rounded-xl border border-border px-4 py-2 text-sm text-muted-foreground">{tc("cancel")}</button>
              </div>
            </div>
          )}

          {/* Filters — only fields backed by real backend query parameters */}
          <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <input
                  value={draftFilters.search}
                  onChange={(e) => setDraftFilters({ ...draftFilters, search: e.target.value })}
                  onKeyDown={(e) => e.key === "Enter" && applyFilters()}
                  placeholder={t("certificates.searchPlaceholder")}
                  className="w-full rounded-xl border border-border bg-background pl-9 pr-4 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/30"
                />
              </div>
              <select
                value={draftFilters.status}
                onChange={(e) => setDraftFilters({ ...draftFilters, status: e.target.value })}
                className="rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none"
              >
                <option value="">{t("certificates.allStatuses")}</option>
                <option value="ISSUED">{t("certificates.filterIssued")}</option>
                <option value="DRAFT">{t("certificates.filterDraft")}</option>
                <option value="REVOKED">{t("certificates.filterRevoked")}</option>
              </select>
              <select
                value={draftFilters.type}
                onChange={(e) => setDraftFilters({ ...draftFilters, type: e.target.value })}
                className="rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none"
              >
                <option value="">{tc("allTypes")}</option>
                {CERT_TYPES.map((certType) => <option key={certType} value={certType}>{certType}</option>)}
              </select>
              <select
                value={draftFilters.institutionId}
                onChange={(e) => setDraftFilters({ ...draftFilters, institutionId: e.target.value })}
                className="rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none"
              >
                <option value="">{t("certificates.allInstitutions")}</option>
                {institutions.map((i) => <option key={i.id} value={i.id}>{i.name}</option>)}
              </select>
              <label className="flex items-center gap-2 text-xs text-muted-foreground">
                {t("certificates.from")}
                <input
                  type="date"
                  value={draftFilters.from}
                  onChange={(e) => setDraftFilters({ ...draftFilters, from: e.target.value })}
                  className="flex-1 rounded-xl border border-border bg-background px-2 py-2 text-sm text-foreground outline-none"
                />
              </label>
              <label className="flex items-center gap-2 text-xs text-muted-foreground">
                {t("certificates.to")}
                <input
                  type="date"
                  value={draftFilters.to}
                  onChange={(e) => setDraftFilters({ ...draftFilters, to: e.target.value })}
                  className="flex-1 rounded-xl border border-border bg-background px-2 py-2 text-sm text-foreground outline-none"
                />
              </label>
            </div>
            <div className="mt-3 flex items-center gap-2">
              <button
                onClick={applyFilters}
                className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90"
              >
                <Filter className="size-4" /> {t("certificates.applyFilters")}
              </button>
              {hasFilters && (
                <button
                  onClick={clearFilters}
                  className="inline-flex items-center gap-1 rounded-xl border border-border px-3 py-2 text-sm text-muted-foreground hover:bg-muted"
                >
                  <X className="size-3.5" /> {t("certificates.clear")}
                </button>
              )}
            </div>
          </div>

          {loading ? (
            <div className="flex items-center justify-center py-20">
              <Loader2 className="size-8 animate-spin text-muted-foreground" />
            </div>
          ) : !data || data.content.length === 0 ? (
            <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
              <Award className="size-10 text-muted-foreground/50" />
              <p className="mt-4 text-sm font-medium text-foreground">{t("certificates.noCertificatesFound")}</p>
              <p className="mt-1 text-sm text-muted-foreground">
                {hasFilters
                  ? t("certificates.noCertificatesMatchFilters")
                  : t("certificates.noCertificatesHaveBeen")}
              </p>
            </div>
          ) : (
            <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border bg-muted/50">
                      <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("certificates.title")}</th>
                      <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("certificates.recipient")}</th>
                      <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("certificates.type")}</th>
                      <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("certificates.serialNumber")}</th>
                      <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("certificates.institution")}</th>
                      <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("certificates.issueDate")}</th>
                      <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("certificates.status")}</th>
                      <th className="px-5 py-3" />
                    </tr>
                  </thead>
                  <tbody>
                    {data.content.map((cert) => (
                      <tr key={cert.id} className="border-b border-border last:border-0 hover:bg-muted/30 transition-colors">
                        <td className="px-5 py-3.5 font-medium text-foreground">{cert.title}</td>
                        <td className="px-5 py-3.5 text-muted-foreground">{cert.studentName || "—"}</td>
                        <td className="px-5 py-3.5 text-muted-foreground">{cert.certificateType || "—"}</td>
                        <td className="px-5 py-3.5">
                          <span className="inline-flex items-center gap-1 text-muted-foreground">
                            <Hash className="size-3.5" />
                            {cert.serialNumber}
                          </span>
                        </td>
                        <td className="px-5 py-3.5 text-muted-foreground">{cert.institutionName || "—"}</td>
                        <td className="px-5 py-3.5 text-muted-foreground">{formatDate(cert.issueDate)}</td>
                        <td className="px-5 py-3.5">{getStatusBadge(cert.status)}</td>
                        <td className="px-5 py-3.5 text-right">
                          <div className="inline-flex gap-1.5">
                            <button
                              onClick={() => setDetailId(cert.id)}
                              className="inline-flex items-center gap-1 rounded-lg border border-border px-2.5 py-1 text-xs font-semibold text-foreground hover:bg-muted"
                            >
                              <Eye className="size-3" /> {t("certificates.details")}
                            </button>
                            {cert.status !== "REVOKED" && (
                              <button
                                onClick={() => setRevokePrompt({ id: cert.id, reason: "" })}
                                className="inline-flex items-center gap-1 rounded-lg border border-red-200 px-2.5 py-1 text-xs font-semibold text-red-600 hover:bg-red-50"
                              >
                                <Ban className="size-3" /> {t("certificates.revoke")}
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {data && data.totalElements > PAGE_SIZE && (
            <div className="flex items-center justify-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
              >
                {t("certificates.prev")}
              </button>
              <span className="text-sm text-muted-foreground">
                {t("certificates.pageOf", { p0: page + 1, p1: data.totalPages })}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
                disabled={page >= data.totalPages - 1}
                className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
              >
                {tc("next")}
              </button>
            </div>
          )}
        </>
      )}

      {detailId && <CertificateDetailDrawer certificateId={detailId} onClose={() => setDetailId(null)} />}
    </div>
  )
}
