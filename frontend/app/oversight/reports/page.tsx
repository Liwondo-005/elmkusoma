"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import {
  Users,
  GraduationCap,
  TrendingUp,
  BarChart3,
  FileBarChart,
  FileText,
  Download,
  ClipboardList,
  Award,
  BookOpen,
  Video,
  AlertTriangle,
  Loader2,
} from "lucide-react"
import { oversightApi, type OversightReport } from "@/lib/api"

export default function OversightReportsPage() {
  const t = useTranslations("oversight");
  const { user, loading: authLoading } = useRequireAuth()
  const [reportTypes, setReportTypes] = useState<OversightReport[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (authLoading || !user) return
    oversightApi
      .reports()
      .then(setReportTypes)
      .catch(() => setError(t("reports.loadFailed")))
      .finally(() => setLoading(false))
  }, [authLoading, user, t])

  if (authLoading || loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-muted-foreground">{t("reports.loadingReports")}</div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
            <FileBarChart className="size-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">{t("reports.reportsCenter")}</h1>
            <p className="text-sm text-muted-foreground">
              {t("reports.generateAndDownloadJurisdiction")}</p>
          </div>
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive">
          <AlertTriangle className="size-4 shrink-0" />
          {error}
        </div>
      )}

      {!error && reportTypes.length === 0 && (
        <div className="rounded-2xl border border-dashed border-border p-8 text-center text-muted-foreground">
          {t("reports.empty")}
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {reportTypes.map((report) => (
          <ReportCard key={report.id} report={report} />
        ))}
      </div>
    </div>
  )
}

function ReportCard({ report }: { report: OversightReport }) {
  const t = useTranslations("oversight")
  const [downloading, setDownloading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)

  const icons: Record<string, React.ReactNode> = {
    Award: <Award className="size-5" />,
    ClipboardList: <ClipboardList className="size-5" />,
    Users: <Users className="size-5" />,
    GraduationCap: <GraduationCap className="size-5" />,
    FileText: <FileText className="size-5" />,
    BookOpen: <BookOpen className="size-5" />,
    Video: <Video className="size-5" />,
    BarChart3: <BarChart3 className="size-5" />,
  }

  const Icon = icons[report.icon] || <FileBarChart className="size-5" />

  async function download() {
    setDownloading(true)
    setError(null)
    setDone(false)
    try {
      const { filename, blob } = await oversightApi.exportReport(report.id)
      const url = URL.createObjectURL(blob)
      const link = document.createElement("a")
      link.href = url
      link.download = filename
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
      setDone(true)
    } catch {
      setError(t("reports.exportFailed"))
    } finally {
      setDownloading(false)
    }
  }

  return (
    <div className="rounded-2xl border border-border bg-card p-5 transition-colors hover:bg-muted/50">
      <div className="flex items-start gap-3">
        <div className={`flex size-10 items-center justify-center rounded-xl ${report.color}`}>
          {Icon}
        </div>
        <div className="flex-1">
          <h3 className="font-medium text-foreground">{report.title}</h3>
          <p className="mt-1 text-sm text-muted-foreground">{report.description}</p>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <span className="inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-700">
              {report.type}
            </span>
            {report.available && (
              <button
                type="button"
                onClick={download}
                disabled={downloading}
                className="flex items-center gap-1 text-sm font-medium text-primary hover:underline disabled:opacity-60"
              >
                {downloading ? (
                  <Loader2 className="size-3 animate-spin" />
                ) : (
                  <Download className="size-3" />
                )}
                {downloading ? t("reports.downloading") : done ? t("reports.downloaded") : t("reports.generate")}
              </button>
            )}
          </div>
          {error && <p className="mt-2 text-xs text-destructive">{error}</p>}
        </div>
      </div>
    </div>
  )
}
