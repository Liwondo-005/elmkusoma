"use client"

import { useEffect, useState } from "react"
import { FileText, Loader2, CheckCircle } from "lucide-react"
import { useTranslations } from "next-intl"
import { certificateApi, type TranscriptResponse } from "@/lib/api"

export default function TranscriptsPage() {
  const t = useTranslations("learner")
  const tc = useTranslations("common")
  const [transcripts, setTranscripts] = useState<TranscriptResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [studentId, setStudentId] = useState("")
  const [searching, setSearching] = useState(false)

  async function loadTranscripts(sid: string) {
    if (!sid.trim()) return
    setSearching(true)
    setLoading(true)
    setError(null)
    try {
      const data = await certificateApi.listTranscripts(sid.trim())
      setTranscripts(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("transcriptsPage.loadFailed"))
    } finally {
      setLoading(false)
      setSearching(false)
    }
  }

  function handleSearch() {
    loadTranscripts(studentId)
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("transcriptsPage.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("transcriptsPage.subtitle")}</p>
      </div>

      <div className="flex gap-2">
        <input
          type="text"
          placeholder={t("certGenerate.studentIdPlaceholder")}
          value={studentId}
          onChange={(e) => setStudentId(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleSearch()}
          className="h-10 flex-1 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
        />
        <button
          onClick={handleSearch}
          disabled={searching || !studentId.trim()}
          className="flex h-10 items-center gap-2 rounded-lg bg-primary px-4 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
        >
          {searching ? <Loader2 className="size-4 animate-spin" /> : <FileText className="size-4" />}
          {tc("search")}
        </button>
      </div>

      {loading && searching && (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center">
          <p className="text-sm font-medium text-destructive">{error}</p>
        </div>
      )}

      {!loading && transcripts.length === 0 && studentId && !error && (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <FileText className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">{t("transcriptsPage.emptyTitle")}</p>
          <p className="mt-1 text-sm text-muted-foreground">{t("transcriptsPage.emptyDescription")}</p>
        </div>
      )}

      {!loading && transcripts.length > 0 && (
        <div className="space-y-3">
          {transcripts.map((tr) => (
            <div key={tr.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-foreground">{t("transcriptsPage.transcriptTitle", { serial: tr.serialNumber })}</span>
                    <span className={`rounded px-2 py-0.5 text-[10px] font-semibold ${
                      tr.status === "ISSUED" ? "bg-teal/10 text-teal" : "bg-muted text-muted-foreground"
                    }`}>{tr.status}</span>
                  </div>
                  <p className="mt-0.5 text-xs text-muted-foreground">{t("certificatesPage.serial", { serial: tr.serialNumber })}</p>
                  <p className="text-xs text-muted-foreground">{t("transcriptsPage.period", { year: tr.academicYear ?? "N/A", term: tr.term ?? "N/A" })}</p>
                  {tr.averageScore != null && (
                    <p className="text-xs text-muted-foreground">{t("transcriptsPage.averageScore", { score: tr.averageScore })}</p>
                  )}
                  {tr.classRank != null && (
                    <p className="text-xs text-muted-foreground">{t("transcriptsPage.classRank", { rank: tr.classRank })}</p>
                  )}
                  {tr.entries && tr.entries.length > 0 && (
                    <div className="mt-2 space-y-1">
                      {tr.entries.map((e, i) => (
                        <div key={i} className="flex items-center gap-3 text-xs text-muted-foreground">
                          <span className="font-medium text-foreground">{e.subjectName}</span>
                          {e.subjectCode && <span className="text-muted-foreground/70">({e.subjectCode})</span>}
                          <span>{t("transcriptsPage.grade", { grade: e.grade ?? "N/A" })}</span>
                          {e.score != null && <span>{t("transcriptsPage.score", { score: e.score })}</span>}
                          {e.remarks && <span className="italic">— {e.remarks}</span>}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
                <span className="shrink-0 text-xs text-muted-foreground">
                  {new Date(tr.createdAt).toLocaleDateString()}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
