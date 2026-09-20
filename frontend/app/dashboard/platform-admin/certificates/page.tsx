"use client"

import { useEffect, useState, useCallback } from "react"
import { Award, Loader2, Hash } from "lucide-react"
import { platformAdminApi, type CertificateSummary, type PageResponse } from "@/lib/platform-admin-api"

const PAGE_SIZE = 20

export default function CertificatesPage() {
  const [data, setData] = useState<PageResponse<CertificateSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await platformAdminApi.listCertificates(page, PAGE_SIZE)
      setData(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load certificates")
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    loadData()
  }, [loadData])

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
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Certificates</h1>
        <p className="mt-1 text-sm text-muted-foreground">View and manage all issued certificates.</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive">{error}</div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-8 animate-spin text-muted-foreground" />
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <Award className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">No certificates found</p>
          <p className="mt-1 text-sm text-muted-foreground">No certificates have been issued yet.</p>
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Title</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Serial Number</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Issue Date</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Status</th>
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
            Prev
          </button>
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {data.totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
            disabled={page >= data.totalPages - 1}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}
