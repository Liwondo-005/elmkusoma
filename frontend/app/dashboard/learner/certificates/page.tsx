"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { learnerApi, type Certificate } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { Award, ExternalLink, AlertCircle, Download, ShieldCheck, Calendar, Hash, ArrowRight } from "lucide-react"
import Link from "next/link"

export default function LearnerCertificatesPage() {
  const { user, loading: authLoading } = useAuth()
  const t = useTranslations("learner")
  const tc = useTranslations("common")
  const [certificates, setCertificates] = useState<Certificate[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user || (user.role !== "Other Learner" && user.role !== "Student")) return
    loadCertificates()
  }, [user])

  async function loadCertificates() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getCertificates()
      setCertificates(data)
    } catch {
      setError(t("certs.loadError"))
    } finally {
      setLoading(false)
    }
  }

  function getStatusBadge(status: string) {
    const styles: Record<string, string> = {
      ACTIVE: "bg-green-500/10 text-green-600",
      ISSUED: "bg-green-500/10 text-green-600",
      REVOKED: "bg-red-500/10 text-red-500",
      EXPIRED: "bg-orange/10 text-orange",
      DRAFT: "bg-yellow-500/10 text-yellow-600",
    }
    return styles[status] || "bg-muted text-muted-foreground"
  }

  function handleDownload(cert: Certificate) {
    const verifyUrl = `${window.location.origin}/certificates/verify/${cert.verificationCode}`
    const content = [
      t("certs.fileHeader"),
      "",
      t("certs.fileTitle", { title: cert.title }),
      cert.description ? t("certs.fileDesc", { text: cert.description }) : "",
      t("certs.fileStudent", { name: cert.studentName }),
      cert.courseOrProgramme ? t("certs.fileCourse", { name: cert.courseOrProgramme }) : "",
      t("certs.fileCompleted", { date: new Date(cert.completionDate).toLocaleDateString() }),
      t("certs.fileIssued", { date: new Date(cert.issueDate).toLocaleDateString() }),
      t("certs.fileCode", { code: cert.verificationCode }),
      t("certs.file{t("certs.verify")}", { url: verifyUrl }),
    ].filter(Boolean).join("\n")

    const blob = new Blob([content], { type: "text/plain" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = `certificate-${cert.verificationCode}.txt`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }

  if (authLoading || (user?.role !== "Other Learner" && user?.role !== "Student")) {
    return <LoadingState />
  }

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("certs.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("certs.subtitle")}
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      <div aria-live="polite" aria-busy={loading}>
      {loading ? (
        <div aria-busy="true"><LoadingState /></div>
      ) : certificates.length === 0 ? (
        <div role="status">
        <EmptyState
          icon={<Award className="size-8" />}
          title={t("certs.emptyTitle")}
          description={t("certs.emptyDesc")}
        />
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {certificates.map((cert) => (
            <div key={cert.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-start justify-between">
                <div className="flex size-10 items-center justify-center rounded-xl bg-yellow-500/10">
                  <Award className="size-5 text-yellow-600" />
                </div>
                <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${getStatusBadge(cert.status)}`}>
                  {cert.status}
                </span>
              </div>
              <h3 className="mt-3 text-sm font-semibold text-foreground">{cert.title}</h3>
              {cert.description && (
                <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{cert.description}</p>
              )}
              {cert.courseOrProgramme && (
                <p className="mt-2 text-xs font-medium text-foreground">{cert.courseOrProgramme}</p>
              )}
              <div className="mt-3 space-y-1.5">
                <div className="flex items-center gap-2 text-xs">
                  <Calendar className="size-3 text-muted-foreground" />
                  <span className="text-muted-foreground">{t("certs.completedLabel")}</span>
                  <span className="text-foreground">{new Date(cert.completionDate).toLocaleDateString()}</span>
                </div>
                <div className="flex items-center gap-2 text-xs">
                  <Calendar className="size-3 text-muted-foreground" />
                  <span className="text-muted-foreground">{t("certs.issuedLabel")}</span>
                  <span className="text-foreground">{new Date(cert.issueDate).toLocaleDateString()}</span>
                </div>
                <div className="flex items-center gap-2 text-xs">
                  <Hash className="size-3 text-muted-foreground" />
                  <span className="text-muted-foreground">{t("certs.codeLabel")}</span>
                  <span className="font-mono text-foreground">{cert.verificationCode}</span>
                </div>
              </div>
              <div className="mt-4 flex gap-2">
                <a
                  href={`/certificates/verify?code=${cert.verificationCode}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label={t("certs.verifyLabel", { title: cert.title })}
                  className="inline-flex flex-1 items-center justify-center gap-1.5 rounded-lg border border-border bg-background px-3 py-2 text-xs font-medium text-foreground hover:bg-muted"
                >
                  <ShieldCheck className="size-3" />
                  {t("certs.verify")}
                </a>
                <button
                  onClick={() => handleDownload(cert)}
                  aria-label={t("certs.downloadLabel", { title: cert.title })}
                  className="inline-flex items-center justify-center gap-1.5 rounded-lg border border-border bg-background px-3 py-2 text-xs font-medium text-foreground hover:bg-muted"
                >
                  <Download className="size-3" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
      </div>

      <div className="rounded-xl border border-border bg-card p-6 text-center">
        <p className="text-sm text-muted-foreground">{t("certs.browseHint")}
        <Link href="/dashboard/learner/courses" className="mt-2 inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline">
          {t("exploreCourses")} <ArrowRight className="size-3" />
        </Link>
      </div>
    </div>
  )
}
