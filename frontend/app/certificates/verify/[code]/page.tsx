"use client"

import { useEffect, useState } from "react"
import { useParams } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { certificateApi, type CertificateVerificationResponse } from "@/lib/api"
import { LoadingState } from "@/components/learner/shared"
import { Award, CheckCircle, XCircle, AlertCircle, ArrowLeft, ExternalLink } from "lucide-react"

export default function CertificateVerifyPage() {
  const tc = useTranslations("common")
  const params = useParams()
  const code = params.code as string

  const [result, setResult] = useState<CertificateVerificationResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!code) return
    verifyCertificate()
  }, [code])

  async function verifyCertificate() {
    try {
      setLoading(true)
      setError(null)
      const data = await certificateApi.verify(code)
      setResult(data)
    } catch {
      setError(tc("verifyLoadError"))
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-muted/30">
        <LoadingState />
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-muted/30">
      <div className="mx-auto max-w-2xl space-y-6 px-4 py-12">
        <Link href="/" className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:underline">
          <ArrowLeft className="size-4" /> {tc("verifyBackHome")}
        </Link>

        <div className="rounded-2xl border border-border bg-card p-8 shadow-xs text-center">
          <div className="mx-auto mb-4 flex size-16 items-center justify-center rounded-full bg-primary/10">
            <Award className="size-8 text-primary" />
          </div>
          <h1 className="text-xl font-bold text-foreground">{tc("verifyPageTitle")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {tc("verifyCodeLabel", { code })}
          </p>
        </div>

        {error && (
          <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
            <div className="flex items-center gap-2 text-sm text-destructive">
              <AlertCircle className="size-4" />
              {error}
            </div>
          </div>
        )}

        {result && (
          <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
            <div className="mb-6 flex items-center gap-3">
              {result.valid ? (
                <>
                  <div className="flex size-10 items-center justify-center rounded-full bg-teal/10">
                    <CheckCircle className="size-6 text-teal" />
                  </div>
                  <div>
                    <h2 className="text-lg font-bold text-teal">{tc("verifyValidTitle")}</h2>
                    <p className="text-xs text-muted-foreground">{tc("verifyValidDesc")}</p>
                  </div>
                </>
              ) : (
                <>
                  <div className="flex size-10 items-center justify-center rounded-full bg-red-500/10">
                    <XCircle className="size-6 text-red-500" />
                  </div>
                  <div>
                    <h2 className="text-lg font-bold text-red-500">{tc("verifyInvalidTitle")}</h2>
                    <p className="text-xs text-muted-foreground">{result.message || tc("verifyInvalidDesc")}</p>
                  </div>
                </>
              )}
            </div>

            {result.id && (
              <div className="space-y-4">
                {result.title && (
                  <div className="rounded-xl border border-border p-4">
                    <p className="text-xs font-medium text-muted-foreground">{tc("verifyFieldTitle")}</p>
                    <p className="mt-0.5 text-sm font-semibold text-foreground">{result.title}</p>
                  </div>
                )}

                <div className="grid grid-cols-2 gap-3">
                  {result.studentName && (
                    <div className="rounded-xl border border-border p-4">
                      <p className="text-xs font-medium text-muted-foreground">{tc("verifyFieldRecipient")}</p>
                      <p className="mt-0.5 text-sm font-medium text-foreground">{result.studentName}</p>
                    </div>
                  )}
                  {result.certificateType && (
                    <div className="rounded-xl border border-border p-4">
                      <p className="text-xs font-medium text-muted-foreground">{tc("verifyFieldType")}</p>
                      <p className="mt-0.5 text-sm font-medium text-foreground capitalize">{result.certificateType.toLowerCase()}</p>
                    </div>
                  )}
                  {result.courseTitle && (
                    <div className="rounded-xl border border-border p-4">
                      <p className="text-xs font-medium text-muted-foreground">{tc("verifyFieldCourse")}</p>
                      <p className="mt-0.5 text-sm font-medium text-foreground">{result.courseTitle}</p>
                    </div>
                  )}
                  {result.instructorName && (
                    <div className="rounded-xl border border-border p-4">
                      <p className="text-xs font-medium text-muted-foreground">{tc("verifyFieldInstructor")}</p>
                      <p className="mt-0.5 text-sm font-medium text-foreground">{result.instructorName}</p>
                    </div>
                  )}
                  {result.grade && (
                    <div className="rounded-xl border border-border p-4">
                      <p className="text-xs font-medium text-muted-foreground">{tc("verifyFieldGrade")}</p>
                      <p className="mt-0.5 text-sm font-medium text-foreground">{result.grade}</p>
                    </div>
                  )}
                  {result.completionDate && (
                    <div className="rounded-xl border border-border p-4">
                      <p className="text-xs font-medium text-muted-foreground">{tc("verifyFieldCompletionDate")}</p>
                      <p className="mt-0.5 text-sm font-medium text-foreground">
                        {new Date(result.completionDate).toLocaleDateString()}
                      </p>
                    </div>
                  )}
                  {result.issueDate && (
                    <div className="rounded-xl border border-border p-4">
                      <p className="text-xs font-medium text-muted-foreground">{tc("verifyFieldIssueDate")}</p>
                      <p className="mt-0.5 text-sm font-medium text-foreground">
                        {new Date(result.issueDate).toLocaleDateString()}
                      </p>
                    </div>
                  )}
                  {result.serialNumber && (
                    <div className="rounded-xl border border-border p-4">
                      <p className="text-xs font-medium text-muted-foreground">{tc("verifyFieldSerial")}</p>
                      <p className="mt-0.5 text-sm font-mono font-medium text-foreground">{result.serialNumber}</p>
                    </div>
                  )}
                  {result.institutionName && (
                    <div className="rounded-xl border border-border p-4">
                      <p className="text-xs font-medium text-muted-foreground">{tc("verifyFieldInstitution")}</p>
                      <p className="mt-0.5 text-sm font-medium text-foreground">{result.institutionName}</p>
                    </div>
                  )}
                </div>

                {result.skills && result.skills.length > 0 && (
                  <div className="rounded-xl border border-border p-4">
                    <p className="text-xs font-medium text-muted-foreground">{tc("verifyFieldSkills")}</p>
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      {result.skills.map((skill, i) => (
                        <span key={i} className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
                          {skill}
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                {result.status && (
                  <div className="rounded-xl border border-border p-4">
                    <p className="text-xs font-medium text-muted-foreground">{tc("verifyFieldStatus")}</p>
                    <p className={`mt-0.5 text-sm font-semibold ${
                      result.status === "ISSUED" ? "text-teal" :
                      result.status === "REVOKED" ? "text-red-500" : "text-muted-foreground"
                    }`}>
                      {result.status}
                    </p>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
