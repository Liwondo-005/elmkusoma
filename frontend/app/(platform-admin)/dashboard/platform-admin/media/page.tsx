"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { Image as ImageIcon, AlertCircle, RefreshCw, HardDrive, Shield, Clock, Archive, Loader2 } from "lucide-react"
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
  const t = useTranslations("platformAdmin");
  const tc = useTranslations("common");
  const [page, setPage] = useState<PageResponse<MediaRow> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [pageIndex, setPageIndex] = useState(0)
  const [acting, setActing] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const res = await platformAdminApi.listPlatformMedia(pageIndex, 20)
      setPage(res)
    } catch (e: any) {
      setError(e.message || t("media.failedToLoadMedia"))
    } finally { setLoading(false) }
  }, [pageIndex])

  useEffect(() => { load() }, [load])

  const archiveMedia = async (id: string) => {
    setActing(id); setError(null)
    try {
      await platformAdminApi.bulkContentAction("MEDIA", "ARCHIVE", [id])
      await load()
    } catch (e: any) {
      setError(e.message || t("media.failedToArchiveMedia"))
    } finally { setActing(null) }
  }

  const items = page?.content ?? []

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-emerald-500 text-white"><ImageIcon className="size-4" /></span> {t("media.platformMedia")}</h1>
            <p className="mt-1 text-sm text-muted-foreground">{t("media.governanceOwnershipStatusProcessing")}</p>
          </div>
          <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> {t("media.refresh")}</button>
        </div>
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          <div className="rounded-xl border border-border bg-muted/20 p-3"><p className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-muted-foreground"><Shield className="size-3.5" /> {t("media.ownership")}</p><p className="mt-1 text-xs text-muted-foreground">{t("media.institutionOwnsMediaPlatform")}</p></div>
          <div className="rounded-xl border border-border bg-muted/20 p-3"><p className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-muted-foreground"><Clock className="size-3.5" /> {t("media.processing")}</p><p className="mt-1 text-xs text-muted-foreground">{t("media.uploadScanTranscodeReady")}</p></div>
          <div className="rounded-xl border border-border bg-muted/20 p-3"><p className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-muted-foreground"><HardDrive className="size-3.5" /> {t("media.storage")}</p><p className="mt-1 text-xs text-muted-foreground">{t("media.providerManagedS3Local")}</p></div>
        </div>
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{error}</span>
          <button onClick={load} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">{t("media.retry")}</button>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        {loading ? <Skeleton /> : items.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-12 text-center">
            <ImageIcon className="size-10 text-muted-foreground/50" />
            <p className="mt-3 text-sm font-semibold text-foreground">{t("media.noMediaAssetsYet")}</p>
            <p className="mt-1 text-xs text-muted-foreground">{t("media.noMediaExistsYet")}</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead><tr className="border-b border-border bg-muted/50"><th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("media.title")}</th><th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("media.type")}</th><th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("media.status")}</th><th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("media.institution")}</th><th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("media.created")}</th><th className="px-4 py-3 text-right font-medium text-muted-foreground">{t("media.actions")}</th></tr></thead>
              <tbody>
                {items.map((m) => (
                  <tr key={m.id} className="border-b border-border last:border-0 hover:bg-muted/30">
                    <td className="px-4 py-3 font-medium text-foreground">{m.title}</td>
                    <td className="px-4 py-3 text-muted-foreground">{m.mediaType ?? "—"}</td>
                    <td className="px-4 py-3"><span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium">{m.status ?? "Unknown"}</span></td>
                    <td className="px-4 py-3 text-muted-foreground text-xs">{m.institutionId ?? "—"}</td>
                    <td className="px-4 py-3 text-muted-foreground text-xs">{m.createdAt ? new Date(m.createdAt).toLocaleDateString() : "—"}</td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => archiveMedia(m.id)}
                        disabled={acting === m.id}
                        className="inline-flex items-center gap-1 rounded-lg border border-border bg-background px-2.5 py-1 text-xs font-semibold hover:bg-muted disabled:opacity-50"
                        title="Archive media asset"
                      >
                        {acting === m.id ? <Loader2 className="size-3 animate-spin" /> : <Archive className="size-3" />} {t("media.archive")}</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
              <span>{t("media.pageOf", { p0: pageIndex + 1, p1: Math.max(page?.totalPages ?? 1, 1) })}</span>
              <div className="flex gap-2">
                <button disabled={pageIndex === 0} onClick={() => setPageIndex(pageIndex - 1)} className="rounded-lg border border-border px-3 py-1.5 font-medium disabled:opacity-50">{t("media.prev")}</button>
                <button disabled={(page?.totalPages ?? 1) <= pageIndex + 1} onClick={() => setPageIndex(pageIndex + 1)} className="rounded-lg border border-border px-3 py-1.5 font-medium disabled:opacity-50">{tc("next")}</button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
