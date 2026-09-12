"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { learnerApi, type Certificate } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { Award, ExternalLink, AlertCircle } from "lucide-react"

export default function LearnerCertificatesPage() {
  const { user, loading: authLoading } = useAuth()
  const [certificates, setCertificates] = useState<Certificate[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user || user.role !== "Other Learner") return
    loadCertificates()
  }, [user])

  async function loadCertificates() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getCertificates()
      setCertificates(data)
    } catch {
      setError("Failed to load certificates")
    } finally {
      setLoading(false)
    }
  }

  function getStatusBadge(status: string) {
    const styles: Record<string, string> = {
      ACTIVE: "bg-green-500/10 text-green-600",
      REVOKED: "bg-red-500/10 text-red-500",
      PENDING: "bg-yellow-500/10 text-yellow-600",
    }
    return styles[status] || "bg-muted text-muted-foreground"
  }

  if (authLoading || user?.role !== "Other Learner") {
    return <LoadingState />
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Certificates</h1>
        <p className="mt-1 text-sm text-muted-foreground">Your earned certificates and achievements.</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      {loading ? (
        <LoadingState />
      ) : certificates.length === 0 ? (
        <EmptyState
          icon={<Award className="size-8" />}
          title="No certificates yet"
          description="Complete courses to earn certificates."
        />
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
                <p className="mt-2 text-xs text-muted-foreground">{cert.courseOrProgramme}</p>
              )}
              <div className="mt-3 space-y-1">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-muted-foreground">Issued</span>
                  <span className="text-foreground">{new Date(cert.issueDate).toLocaleDateString()}</span>
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="text-muted-foreground">Completion</span>
                  <span className="text-foreground">{new Date(cert.completionDate).toLocaleDateString()}</span>
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="text-muted-foreground">Verification</span>
                  <span className="font-mono text-foreground">{cert.verificationCode}</span>
                </div>
              </div>
              <a
                href={`/certificates/verify/${cert.verificationCode}`}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-lg border border-border bg-background px-4 py-2 text-xs font-medium text-foreground hover:bg-muted"
              >
                <ExternalLink className="size-3" />
                Verify Certificate
              </a>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
