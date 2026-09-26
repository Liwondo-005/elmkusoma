"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useRef } from "react"
import { Upload, Loader2, CheckCircle, Clock, AlertTriangle, FileText } from "lucide-react"
import { adminApi, getInstitutionId, type ImportJobResponse } from "@/lib/api"

export default function AdminImportPage() {
  const t = useTranslations("admin");
  const [jobs, setJobs] = useState<ImportJobResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [importing, setImporting] = useState(false)
  const fileRef = useRef<HTMLInputElement>(null)

  const institutionId = getInstitutionId()

  useEffect(() => {
    if (!institutionId) { setError(t("import.noInstitutionContextFound")); setLoading(false); return }
    adminApi
      .listImportJobs(institutionId)
      .then(setJobs)
      .catch((err) => setError(err instanceof Error ? err.message : t("import.failedToLoadImport")))
      .finally(() => setLoading(false))
  }, [institutionId])

  async function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file || !institutionId) return

    setImporting(true)
    setError(null)
    try {
      const importType = file.name.includes("student") ? "STUDENT" : "TEACHER"
      const job = await adminApi.triggerImport(institutionId, importType, file.name)
      setJobs((prev) => [job, ...prev])
    } catch (err) {
      setError(err instanceof Error ? err.message : t("import.failedToStartImport"))
    } finally {
      setImporting(false)
      if (fileRef.current) fileRef.current.value = ""
    }
  }

  function statusIcon(status: string) {
    switch (status) {
      case "COMPLETED": return <CheckCircle className="size-4 text-teal" />
      case "PROCESSING": return <Clock className="size-4 text-primary animate-pulse" />
      case "FAILED": return <AlertTriangle className="size-4 text-destructive" />
      default: return <Clock className="size-4 text-muted-foreground" />
    }
  }

  function statusColor(status: string) {
    switch (status) {
      case "COMPLETED": return "bg-teal/10 text-teal"
      case "PROCESSING": return "bg-primary/10 text-primary"
      case "FAILED": return "bg-destructive/10 text-destructive"
      default: return "bg-muted text-muted-foreground"
    }
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("import.dataImport")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("import.importStudentsAndTeachers")}</p>
        </div>
        <label
          className={`flex h-9 cursor-pointer items-center gap-2 rounded-lg bg-primary px-4 text-xs font-medium text-primary-foreground hover:bg-primary/90 ${importing ? "pointer-events-none opacity-50" : ""}`}
        >
          <Upload className="size-3.5" /> {importing ? t("import.uploading") : t("import.uploadCsv")}
          <input
            ref={fileRef}
            type="file"
            accept=".csv"
            onChange={handleFileSelect}
            className="hidden"
            disabled={importing}
          />
        </label>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center">
          <p className="text-sm font-medium text-destructive">{error}</p>
        </div>
      )}

      {loading && (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {!loading && jobs.length === 0 && (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <FileText className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">{t("import.noImportJobsYet")}</p>
          <p className="mt-1 text-sm text-muted-foreground">{t("import.uploadACsvFile")}</p>
        </div>
      )}

      {!loading && jobs.length > 0 && (
        <div className="space-y-3">
          {jobs.map((job) => (
            <div key={job.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    {statusIcon(job.status)}
                    <span className="text-sm font-semibold text-foreground">{job.fileName}</span>
                    <span className={`rounded px-2 py-0.5 text-[10px] font-semibold ${statusColor(job.status)}`}>
                      {job.status}
                    </span>
                    <span className="rounded bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                      {job.importType}
                    </span>
                  </div>
                  <div className="mt-1 flex items-center gap-4 text-xs text-muted-foreground">
                    {job.totalRows != null && <span>{t("import.total", { p0: job.totalRows })}</span>}
                    {job.processedRows != null && <span>{t("import.processed", { p0: job.processedRows })}</span>}
                    {job.failedRows != null && job.failedRows > 0 && (
                      <span className="text-destructive">{t("import.failed", { p0: job.failedRows })}</span>
                    )}
                  </div>
                  {job.errorLog && (
                    <p className="mt-1 text-xs text-destructive">{t("import.importErrorsOccurredCheck")}</p>
                  )}
                </div>
                <span className="shrink-0 text-xs text-muted-foreground">
                  {new Date(job.createdAt).toLocaleString()}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
