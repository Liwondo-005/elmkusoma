"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { Award, Loader2, Hash, Ban, AlertCircle } from "lucide-react"
import { platformAdminApi, type CertificateSummary, type PageResponse } from "@/lib/platform-admin-api"

const PAGE_SIZE = 20

export default function CertificatesPage() {
  const t = useTranslations("platformAdmin");
  const tc = useTranslations("common");
  const [data, setData] = useState<PageResponse<CertificateSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [revoking, setRevoking] = useState<string | null>(null)
  const [revokePrompt, setRevokePrompt] = useState<{ id: string; reason: string } | null>(null)

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await platformAdminApi.listCertificates(page, PAGE_SIZE)
      setData(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("certificates.failedToLoadCertificates"))
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    loadData()
  }, [loadData])

  const revoke = async () => {
    if (!revokePrompt) return
    setRevoking(revokePrompt.id); setError(null)
    try {
      await platformAdminApi.revokeCertificatePlatform(revokePrompt.id, revokePrompt.reason.trim() || undefined)
      setRevokePrompt(null)
      await loadData()
    } catch (e: any) {
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

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("certificates.certificates")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("certificates.viewAndManageAll")}</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive flex items-center gap-2"><AlertCircle className="size-4" />{error}</div>
      )}

      {revokePrompt && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4 space-y-3">
          <p className="text-sm font-semibold text-destructive">{t("certificates.revokeCertificatePlatformAction")}</p>
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
              {revoking === revokePrompt.id ? <Loader2 className="size-4 animate-spin" /> : <Ban className="size-4" />} {t("certificates.confirmRevoke")}</button>
            <button onClick={() => setRevokePrompt(null)} className="rounded-xl border border-border px-4 py-2 text-sm text-muted-foreground">{tc("cancel")}</button>
          </div>
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-8 animate-spin text-muted-foreground" />
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <Award className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">{t("certificates.noCertificatesFound")}</p>
          <p className="mt-1 text-sm text-muted-foreground">{t("certificates.noCertificatesHaveBeen")}</p>
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("certificates.title")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("certificates.serialNumber")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("certificates.issueDate")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("certificates.status")}</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody>
                {data.content.map((cert) => (
                  <tr key={cert.id} className="border-b border-border last:border-0 hover:bg-muted/30 transition-colors">
                    <td className="px-5 py-3.5 font-medium text-foreground">{cert.title}</td>
                    <td className="px-5 py-3.5">
                      <span className="inline-flex items-center gap-1 text-muted-foreground">
                        <Hash className="size-3.5" />
                        {cert.serialNumber}
                      </span>
                    </td>
                    <td className="px-5 py-3.5 text-muted-foreground">{formatDate(cert.issueDate)}</td>
                    <td className="px-5 py-3.5">{getStatusBadge(cert.status)}</td>
                    <td className="px-5 py-3.5 text-right">
                      {cert.status !== "REVOKED" && (
                        <button
                          onClick={() => setRevokePrompt({ id: cert.id, reason: "" })}
                          className="inline-flex items-center gap-1 rounded-lg border border-red-200 px-2.5 py-1 text-xs font-semibold text-red-600 hover:bg-red-50"
                        >
                          <Ban className="size-3" /> {t("certificates.revoke")}</button>
                      )}
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
            {t("certificates.prev")}</button>
          <span className="text-sm text-muted-foreground">
            {t("certificates.pageOf", { p0: page + 1, p1: data.totalPages })}</span>
          <button
            onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
            disabled={page >= data.totalPages - 1}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
          >
            {tc("next")}</button>
        </div>
      )}
    </div>
  )
}
