"use client"

import { useEffect, useState, useCallback } from "react"
import { FileText, AlertCircle, RefreshCw, Archive, ArchiveRestore, Loader2 } from "lucide-react"
import { platformAdminApi, type PageResponse } from "@/lib/platform-admin-api"

interface ResourceRow {
  id: string
  title: string
  resourceType?: string
  institutionId?: string
  createdAt?: string
}

function Skeleton() {
  return <div className="animate-pulse space-y-3"><div className="h-14 rounded-xl bg-muted" /><div className="h-14 rounded-xl bg-muted" /><div className="h-14 rounded-xl bg-muted" /></div>
}

export default function PlatformResourcesPage() {
  const [page, setPage] = useState<PageResponse<ResourceRow> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [pageIndex, setPageIndex] = useState(0)
  const [acting, setActing] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const res = await platformAdminApi.listPlatformResources(pageIndex, 20)
      setPage(res)
    } catch (e: any) {
      setError(e.message || "Failed to load resources")
    } finally { setLoading(false) }
  }, [pageIndex])

  useEffect(() => { load() }, [load])

  const archiveAction = async (id: string, action: "ARCHIVE" | "RESTORE") => {
    setActing(id); setError(null)
    try {
      await platformAdminApi.bulkContentAction("RESOURCE", action, [id])
      await load()
    } catch (e: any) {
      setError(e.message || `Failed to ${action.toLowerCase()} resource`)
    } finally { setActing(null) }
  }

  const items = page?.content ?? []

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-amber-500 text-white"><FileText className="size-4" /></span> Platform Resources</h1>
            <p className="mt-1 text-sm text-muted-foreground">Documents, PDFs, and learning resources governance — access, retention, and compliance.</p>
          </div>
          <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> Refresh</button>
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
            <FileText className="size-10 text-muted-foreground/50" />
            <p className="mt-3 text-sm font-semibold text-foreground">No resources yet</p>
            <p className="mt-1 text-xs text-muted-foreground">No resources exist yet for platform governance.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead><tr className="border-b border-border bg-muted/50"><th className="px-4 py-3 text-left font-medium text-muted-foreground">Title</th><th className="px-4 py-3 text-left font-medium text-muted-foreground">Type</th><th className="px-4 py-3 text-left font-medium text-muted-foreground">Institution</th><th className="px-4 py-3 text-left font-medium text-muted-foreground">Created</th><th className="px-4 py-3 text-right font-medium text-muted-foreground">Actions</th></tr></thead>
              <tbody>
                {items.map((r) => (
                  <tr key={r.id} className="border-b border-border last:border-0 hover:bg-muted/30">
                    <td className="px-4 py-3 font-medium text-foreground">{r.title}</td>
                    <td className="px-4 py-3 text-muted-foreground">{r.resourceType ?? "—"}</td>
                    <td className="px-4 py-3 text-muted-foreground text-xs">{r.institutionId ?? "—"}</td>
                    <td className="px-4 py-3 text-muted-foreground text-xs">{r.createdAt ? new Date(r.createdAt).toLocaleDateString() : "—"}</td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => archiveAction(r.id, "ARCHIVE")}
                        disabled={acting === r.id}
                        className="inline-flex items-center gap-1 rounded-lg border border-border bg-background px-2.5 py-1 text-xs font-semibold hover:bg-muted disabled:opacity-50"
                        title="Archive resource"
                      >
                        {acting === r.id ? <Loader2 className="size-3 animate-spin" /> : <Archive className="size-3" />} Archive
                      </button>
                    </td>
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
