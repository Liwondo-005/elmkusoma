"use client"

import { useEffect, useState, useCallback } from "react"
import { Image as ImageIcon, AlertCircle, RefreshCw, HardDrive, Shield, Clock } from "lucide-react"
import { platformAdminApi, type PageResponse } from "@/lib/platform-admin-api"

interface MediaRow {
  id: string
  title: string
  mediaType?: string
  status?: string
  institutionId?: string
  createdAt?: string
}

function Skeleton() {
  return <div className="animate-pulse space-y-3"><div className="h-12 rounded-xl bg-muted" /><div className="h-12 rounded-xl bg-muted" /><div className="h-12 rounded-xl bg-muted" /></div>
}

export default function PlatformMediaPage() {
  const [page, setPage] = useState<PageResponse<MediaRow> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [pageIndex, setPageIndex] = useState(0)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const res = await platformAdminApi.listPlatformMedia(pageIndex, 20)
      setPage(res)
    } catch (e: any) {
      setError(e.message || "Failed to load media")
    } finally { setLoading(false) }
  }, [pageIndex])

  useEffect(() => { load() }, [load])

  const items = page?.content ?? []

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-emerald-500 text-white"><ImageIcon className="size-4" /></span> Platform Media</h1>
            <p className="mt-1 text-sm text-muted-foreground">Governance: ownership, status, processing, and storage across institutions.</p>
          </div>
          <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> Refresh</button>
        </div>
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          <div className="rounded-xl border border-border bg-muted/20 p-3"><p className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-muted-foreground"><Shield className="size-3.5" /> Ownership</p><p className="mt-1 text-xs text-muted-foreground">Institution owns media; platform governs retention & access.</p></div>
          <div className="rounded-xl border border-border bg-muted/20 p-3"><p className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-muted-foreground"><Clock className="size-3.5" /> Processing</p><p className="mt-1 text-xs text-muted-foreground">Upload → Scan → Transcode → Ready. Status shown per asset.</p></div>
          <div className="rounded-xl border border-border bg-muted/20 p-3"><p className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-muted-foreground"><HardDrive className="size-3.5" /> Storage</p><p className="mt-1 text-xs text-muted-foreground">Provider-managed S3 / local. Quotas and retention pending policy.</p></div>
        </div>
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{error}</span>
          <button onClick={load} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">Retry</button>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        {loading ? <Skeleton /> : items.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-12 text-center">
            <ImageIcon className="size-10 text-muted-foreground/50" />
            <p className="mt-3 text-sm font-semibold text-foreground">No media assets yet</p>
            <p className="mt-1 text-xs text-muted-foreground">No media exists yet for platform governance.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead><tr className="border-b border-border bg-muted/50"><th className="px-4 py-3 text-left font-medium text-muted-foreground">Title</th><th className="px-4 py-3 text-left font-medium text-muted-foreground">Type</th><th className="px-4 py-3 text-left font-medium text-muted-foreground">Status</th><th className="px-4 py-3 text-left font-medium text-muted-foreground">Institution</th><th className="px-4 py-3 text-left font-medium text-muted-foreground">Created</th></tr></thead>
              <tbody>
                {items.map((m) => (
                  <tr key={m.id} className="border-b border-border last:border-0 hover:bg-muted/30">
                    <td className="px-4 py-3 font-medium text-foreground">{m.title}</td>
                    <td className="px-4 py-3 text-muted-foreground">{m.mediaType ?? "—"}</td>
                    <td className="px-4 py-3"><span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium">{m.status ?? "Unknown"}</span></td>
                    <td className="px-4 py-3 text-muted-foreground text-xs">{m.institutionId ?? "—"}</td>
                    <td className="px-4 py-3 text-muted-foreground text-xs">{m.createdAt ? new Date(m.createdAt).toLocaleDateString() : "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
              <span>Page {pageIndex + 1} of {Math.max(page?.totalPages ?? 1, 1)}</span>
              <div className="flex gap-2">
                <button disabled={pageIndex === 0} onClick={() => setPageIndex(pageIndex - 1)} className="rounded-lg border border-border px-3 py-1.5 font-medium disabled:opacity-50">Prev</button>
                <button disabled={(page?.totalPages ?? 1) <= pageIndex + 1} onClick={() => setPageIndex(pageIndex + 1)} className="rounded-lg border border-border px-3 py-1.5 font-medium disabled:opacity-50">Next</button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
