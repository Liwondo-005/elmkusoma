"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { CertificateCard } from "@/components/certificates/certificate-card"
import { certificateApi, type CertificateVerificationResponse } from "@/lib/api"
import { AlertTriangle, BadgeCheck, XCircle } from "lucide-react"

export function CertificateVerifier() {
  const [query, setQuery] = useState("")
  const [result, setResult] = useState<CertificateVerificationResponse | null | undefined>(undefined)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleVerify(e: React.FormEvent) {
    e.preventDefault()
    const code = query.trim()
    if (!code) return

    setLoading(true)
    setError(null)
    setResult(undefined)

    try {
      const response = await certificateApi.verify(code)
      setResult(response)
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Verification failed"
      setError(message)
      setResult(null)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
      <form onSubmit={handleVerify} className="flex gap-3">
        <div className="relative flex-1">
          <input
            type="text"
            placeholder="Enter certificate verification code"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value)
              setResult(undefined)
              setError(null)
            }}
            className="h-11 w-full rounded-xl border border-border bg-background px-4 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:ring-2 focus:ring-ring/20"
          />
        </div>
        <Button type="submit" size="lg" className="px-6" disabled={loading || !query.trim()}>
          {loading ? "Verifying..." : "Verify"}
        </Button>
      </form>

      <div className="mt-8">
        {result === undefined && !error && (
          <div className="rounded-2xl border border-border border-dashed bg-muted/30 px-6 py-16 text-center">
            <p className="text-sm text-muted-foreground">
              Enter a verification code above to check certificate authenticity.
            </p>
          </div>
        )}

        {error && (
          <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-16 text-center">
            <XCircle className="mx-auto size-8 text-destructive" />
            <p className="mt-3 text-sm font-medium text-destructive">Verification Failed</p>
            <p className="mt-1 text-xs text-muted-foreground">{error}</p>
          </div>
        )}

        {result && !result.valid && (
          <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-16 text-center">
            <XCircle className="mx-auto size-8 text-destructive" />
            <p className="mt-3 text-sm font-medium text-destructive">Certificate Not Valid</p>
            <p className="mt-1 text-xs text-muted-foreground">
              {result.message || "No certificate matches this verification code. Please check and try again."}
            </p>
          </div>
        )}

        {result && result.valid && (
          <div className="space-y-4">
            <div className="flex items-center gap-2 rounded-xl border border-teal/20 bg-teal/5 px-4 py-3">
              <BadgeCheck className="size-5 text-teal" />
              <span className="text-sm font-semibold text-teal">Certificate Verified</span>
            </div>
            <CertificateCard
              certificate={{
                id: result.serialNumber || result.id || "",
                studentName: result.studentName || "",
                courseTitle: result.courseTitle || result.title || "",
                instructor: result.instructorName || "",
                completionDate: result.completionDate || "",
                grade: result.grade || "",
                skills: result.skills || [],
              }}
            />
            {result.institutionName && (
              <p className="text-center text-xs text-muted-foreground">
                Issued by: <span className="font-medium text-foreground">{result.institutionName}</span>
              </p>
            )}
            {result.status && result.status !== "ISSUED" && (
              <div className="flex items-center gap-2 rounded-xl border border-amber-500/20 bg-amber-500/5 px-4 py-3">
                <AlertTriangle className="size-4 text-amber-600" />
                <span className="text-xs font-medium text-amber-600">Status: {result.status}</span>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
