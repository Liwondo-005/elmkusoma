"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { CertificateCard } from "@/components/certificates/certificate-card"
import type { Certificate } from "@/lib/data"

export function CertificateVerifier() {
  const [query, setQuery] = useState("")
  const [result, setResult] = useState<Certificate | null | undefined>(undefined)

  function handleVerify(e: React.FormEvent) {
    e.preventDefault()
    if (!query.trim()) return

    const mockCerts: Record<string, Certificate> = {
      "ELM-CERT-2026-001": {
        id: "ELM-CERT-2026-001",
        studentName: "Asha M.",
        courseTitle: "HTML & CSS Basics",
        instructor: "David O.",
        completionDate: "August 28, 2026",
        grade: "Distinction",
        skills: ["HTML5", "CSS3", "Responsive Design", "Flexbox", "Grid"],
      },
      "ELM-CERT-2026-002": {
        id: "ELM-CERT-2026-002",
        studentName: "Asha M.",
        courseTitle: "Data Science with Python",
        instructor: "Grace N.",
        completionDate: "August 15, 2026",
        grade: "Merit",
        skills: ["Python", "Pandas", "NumPy", "Data Visualization", "Statistics"],
      },
      "ELM-CERT-2026-003": {
        id: "ELM-CERT-2026-003",
        studentName: "John K.",
        courseTitle: "Digital Marketing Strategy",
        instructor: "Sarah K.",
        completionDate: "July 30, 2026",
        grade: "Distinction",
        skills: ["SEO", "Social Media Marketing", "Content Strategy", "Analytics"],
      },
    }

    const found = mockCerts[query.trim().toUpperCase()]
    setResult(found ?? null)
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
      <form onSubmit={handleVerify} className="flex gap-3">
        <div className="relative flex-1">
          <input
            type="text"
            placeholder="Enter certificate ID (e.g. ELM-CERT-2026-001)"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value)
              setResult(undefined)
            }}
            className="h-11 w-full rounded-xl border border-border bg-background px-4 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:ring-2 focus:ring-ring/20"
          />
        </div>
        <Button type="submit" size="lg" className="px-6">
          Verify
        </Button>
      </form>

      <div className="mt-8">
        {result === undefined && (
          <div className="rounded-2xl border border-border border-dashed bg-muted/30 px-6 py-16 text-center">
            <p className="text-sm text-muted-foreground">
              Enter a certificate ID above to verify its authenticity.
            </p>
            <p className="mt-1 text-xs text-muted-foreground/70">
              Try: ELM-CERT-2026-001, ELM-CERT-2026-002, or ELM-CERT-2026-003
            </p>
          </div>
        )}

        {result === null && (
          <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-16 text-center">
            <p className="mt-4 text-sm font-medium text-destructive">Certificate Not Found</p>
            <p className="mt-1 text-xs text-muted-foreground">
              No certificate matches ID &ldquo;{query}&rdquo;. Please check and try again.
            </p>
          </div>
        )}

        {result && <CertificateCard certificate={result} />}
      </div>
    </div>
  )
}
