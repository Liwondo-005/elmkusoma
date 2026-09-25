"use client"

import { useEffect, useState } from "react"
import QRCode from "qrcode"

/**
 * Professional certificate document (screen preview + print-to-PDF).
 *
 * Renders ONLY real data: certificate fields, institution name and the template's
 * authorised signatories. No fabricated names, dates or signatures. The QR code
 * encodes the real public verification URL (`/certificates/verify/{code}`).
 */

export interface DocumentSignatory {
  fullName: string
  positionTitle?: string | null
  organization?: string | null
  signatureImage?: string | null
}

export interface DocumentCertificate {
  serialNumber: string
  certificateType: string
  title: string
  courseTitle?: string | null
  courseOrProgramme?: string | null
  description?: string | null
  studentName: string
  studentIdNumber?: string | null
  instructorName?: string | null
  grade?: string | null
  skills?: string[]
  completionDate?: string | null
  issueDate?: string | null
  expiryDate?: string | null
  status?: string | null
  verificationCode: string
  verificationUrl?: string | null
  revokedReason?: string | null
  revokedAt?: string | null
}

/** The dynamic-field tokens supported in template statement text (only real data sources). */
export const CERTIFICATE_TOKENS: { token: string; label: string }[] = [
  { token: "{{recipient.fullName}}", label: "Recipient full name" },
  { token: "{{recipient.studentId}}", label: "Recipient student ID" },
  { token: "{{certificate.serialNumber}}", label: "Serial number" },
  { token: "{{certificate.number}}", label: "Certificate number" },
  { token: "{{certificate.title}}", label: "Certificate title" },
  { token: "{{certificate.type}}", label: "Certificate type" },
  { token: "{{certificate.status}}", label: "Certificate status" },
  { token: "{{certificate.grade}}", label: "Grade / result" },
  { token: "{{certificate.issueDate}}", label: "Issue date" },
  { token: "{{certificate.completionDate}}", label: "Completion date" },
  { token: "{{certificate.expiryDate}}", label: "Expiry date" },
  { token: "{{certificate.description}}", label: "Certificate description" },
  { token: "{{course.title}}", label: "Course / programme title" },
  { token: "{{institution.name}}", label: "Issuing institution name" },
  { token: "{{institution.logo}}", label: "Institution logo URL" },
  { token: "{{instructor.name}}", label: "Instructor name" },
  { token: "{{skills}}", label: "Skills list (comma separated)" },
]

function formatDate(value?: string | null): string {
  if (!value) return "—"
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleDateString("en-GB", { day: "2-digit", month: "long", year: "numeric" })
}

function buildContext(
  certificate: DocumentCertificate,
  extras: { institutionName?: string | null; logoUrl?: string | null } = {}
): Record<string, string> {
  const course = certificate.courseOrProgramme || certificate.courseTitle || ""
  const values: Record<string, string> = {
    "recipient.fullName": certificate.studentName || "",
    "recipient.studentId": certificate.studentIdNumber || "",
    "certificate.serialNumber": certificate.serialNumber || "",
    "certificate.number": certificate.serialNumber || "",
    "certificate.title": certificate.title || "",
    "certificate.type": certificate.certificateType || "",
    "certificate.status": certificate.status || "",
    "certificate.grade": certificate.grade || "",
    "certificate.issueDate": certificate.issueDate ? formatDate(certificate.issueDate) : "",
    "certificate.completionDate": certificate.completionDate ? formatDate(certificate.completionDate) : "",
    "certificate.expiryDate": certificate.expiryDate ? formatDate(certificate.expiryDate) : "",
    "certificate.description": certificate.description || "",
    "course.title": course,
    "institution.name": extras.institutionName || "",
    "institution.logo": extras.logoUrl || "",
    "instructor.name": certificate.instructorName || "",
    skills: (certificate.skills || []).join(", "),
  }
  return values
}

/** Expands {{token}} placeholders against real certificate data. Unknown tokens are left as-is. */
export function expandCertificateTokens(text: string, context: Record<string, string>): string {
  return text.replace(/\{\{\s*([\w.]+)\s*\}\}/g, (match, key: string) =>
    Object.prototype.hasOwnProperty.call(context, key) ? context[key] : match
  )
}

const TYPE_HEADINGS: Record<string, string> = {
  COMPLETION: "CERTIFICATE OF COMPLETION",
  ACHIEVEMENT: "CERTIFICATE OF ACHIEVEMENT",
  PARTICIPATION: "CERTIFICATE OF PARTICIPATION",
  TRANSCRIPT: "ACADEMIC TRANSCRIPT",
}

const TYPE_STATEMENTS: Record<string, string> = {
  COMPLETION: "has successfully completed the programme outlined below, meeting all requirements set forth by the issuing institution.",
  ACHIEVEMENT: "has demonstrated outstanding achievement in the programme outlined below, in accordance with the standards of the issuing institution.",
  PARTICIPATION: "has participated in the programme outlined below, fulfilling the attendance and engagement requirements of the issuing institution.",
  TRANSCRIPT: "has satisfied the academic requirements recorded in this transcript, issued under the authority of the issuing institution.",
}

interface CertificateDocumentProps {
  certificate: DocumentCertificate
  institutionName?: string | null
  logoUrl?: string | null
  signatories?: DocumentSignatory[]
  /** Template preview mode — renders clearly marked sample content. */
  sample?: boolean
  /** Extra class for the printable root. */
  className?: string
}

export function CertificateDocument({
  certificate,
  institutionName,
  logoUrl,
  signatories = [],
  sample = false,
  className = "",
}: CertificateDocumentProps) {
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null)
  const [origin, setOrigin] = useState<string>("")

  const verifyPath = `/certificates/verify/${certificate.verificationCode}`
  const verifyUrl = `${origin || ""}${verifyPath}`

  useEffect(() => {
    if (typeof window === "undefined") return
    setOrigin(window.location.origin)
    let cancelled = false
    QRCode.toDataURL(`${window.location.origin}${verifyPath}`, {
      width: 128,
      margin: 1,
      errorCorrectionLevel: "M",
      color: { dark: "#111827", light: "#ffffff" },
    })
      .then((url) => {
        if (!cancelled) setQrDataUrl(url)
      })
      .catch((err) => {
        console.error("QR generation failed", err)
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [certificate.verificationCode])

  const context = buildContext(certificate, { institutionName, logoUrl })
  const heading = TYPE_HEADINGS[certificate.certificateType] || "CERTIFICATE"
  const statement = certificate.description
    ? expandCertificateTokens(certificate.description, context)
    : TYPE_STATEMENTS[certificate.certificateType] ||
      "has fulfilled the requirements of the programme outlined below, as certified by the issuing institution."
  const course = certificate.courseOrProgramme || certificate.courseTitle || certificate.title
  const isRevoked = certificate.status === "REVOKED"
  const isDraft = certificate.status === "DRAFT"

  return (
    <div className={`print-cert-root ${className}`}>
      <style>{`
        @media print {
          @page { size: A4 landscape; margin: 10mm; }
          html, body { background: #ffffff !important; }
          body * { visibility: hidden !important; }
          .print-cert-root, .print-cert-root * { visibility: visible !important; }
          .print-cert-root {
            position: absolute !important;
            left: 0 !important;
            top: 0 !important;
            width: 100% !important;
            margin: 0 !important;
            padding: 0 !important;
          }
        }
      `}</style>

      <div className="relative overflow-hidden rounded-lg border-[3px] border-foreground/80 bg-white p-6 text-neutral-900 shadow-lg sm:p-10 print:border-neutral-900 print:shadow-none">
        {/* Inner decorative frame */}
        <div className="pointer-events-none absolute inset-2 rounded border border-foreground/25" />
        <div className="pointer-events-none absolute inset-3 rounded border border-foreground/10" />

        {(isRevoked || isDraft) && (
          <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center">
            <span
              className={`rotate-[-24deg] text-6xl font-black tracking-widest sm:text-8xl ${
                isRevoked ? "text-red-600/25" : "text-neutral-500/25"
              }`}
            >
              {isRevoked ? "REVOKED" : "DRAFT"}
            </span>
          </div>
        )}
        {sample && (
          <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center">
            <span className="rotate-[-24deg] text-6xl font-black tracking-widest text-sky-700/20 sm:text-8xl">
              SAMPLE
            </span>
          </div>
        )}

        <div className="relative z-0 flex min-h-[420px] flex-col">
          {/* Header */}
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-center gap-3">
              {logoUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={logoUrl} alt="Institution logo" className="h-12 w-auto max-w-[160px] object-contain" />
              ) : null}
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-[0.25em] text-neutral-500">
                  {institutionName || "Issuing Institution"}
                </p>
                <p className="text-[10px] uppercase tracking-widest text-neutral-400">Official Certificate</p>
              </div>
            </div>
            <div className="text-right">
              <p className="text-[10px] uppercase tracking-widest text-neutral-400">Certificate No.</p>
              <p className="font-mono text-xs font-semibold text-neutral-700">{certificate.serialNumber}</p>
              <p className="mt-1 text-[10px] uppercase tracking-widest text-neutral-400">Issued</p>
              <p className="text-xs font-medium text-neutral-700">{formatDate(certificate.issueDate || certificate.completionDate)}</p>
            </div>
          </div>

          {/* Body */}
          <div className="flex flex-1 flex-col items-center justify-center py-6 text-center">
            <p className="text-[10px] font-semibold uppercase tracking-[0.4em] text-neutral-500">This is to certify that</p>
            <h1 className="mt-3 font-serif text-3xl font-bold tracking-tight text-neutral-900 sm:text-5xl">
              {certificate.studentName}
            </h1>
            {certificate.studentIdNumber ? (
              <p className="mt-1 text-xs text-neutral-500">Student ID: {certificate.studentIdNumber}</p>
            ) : null}
            <p className="mt-4 max-w-2xl text-sm leading-relaxed text-neutral-600 sm:text-base">{statement}</p>
            <p className="mt-4 font-serif text-xl font-semibold text-neutral-800 sm:text-2xl">{course}</p>
            <div className="mt-3 flex flex-wrap items-center justify-center gap-x-6 gap-y-1 text-xs text-neutral-500">
              {certificate.grade ? <span>Grade: <strong className="text-neutral-700">{certificate.grade}</strong></span> : null}
              {certificate.instructorName ? <span>Instructor: <strong className="text-neutral-700">{certificate.instructorName}</strong></span> : null}
              <span>Completed: <strong className="text-neutral-700">{formatDate(certificate.completionDate)}</strong></span>
              {certificate.expiryDate ? <span>Valid until: <strong className="text-neutral-700">{formatDate(certificate.expiryDate)}</strong></span> : null}
            </div>
            {certificate.skills && certificate.skills.length > 0 ? (
              <p className="mt-2 max-w-2xl text-xs text-neutral-500">
                Skills: {certificate.skills.join(" · ")}
              </p>
            ) : null}
          </div>

          {/* Footer: signatures + QR */}
          <div className="flex flex-col gap-6 border-t border-neutral-300 pt-5 sm:flex-row sm:items-end sm:justify-between">
            <div className="flex flex-1 flex-wrap items-end justify-center gap-8 sm:justify-start">
              {signatories.length > 0 ? (
                signatories.slice(0, 3).map((signatory, index) => (
                  <div key={`${signatory.fullName}-${index}`} className="w-44 text-center">
                    {signatory.signatureImage ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img
                        src={signatory.signatureImage}
                        alt={`Signature of ${signatory.fullName}`}
                        className="mx-auto h-12 object-contain"
                      />
                    ) : (
                      <div className="mx-auto h-12" />
                    )}
                    <div className="mx-auto w-40 border-t border-neutral-400" />
                    <p className="mt-1.5 text-xs font-semibold text-neutral-800">{signatory.fullName}</p>
                    {signatory.positionTitle ? (
                      <p className="text-[11px] text-neutral-600">{signatory.positionTitle}</p>
                    ) : null}
                    {signatory.organization ? (
                      <p className="text-[11px] text-neutral-500">{signatory.organization}</p>
                    ) : null}
                  </div>
                ))
              ) : (
                <div className="text-center sm:text-left">
                  <div className="mx-auto h-12 w-44 sm:mx-0" />
                  <div className="w-44 border-t border-neutral-400" />
                  <p className="mt-1.5 text-xs font-semibold text-neutral-800">
                    {institutionName || "Issuing Institution"}
                  </p>
                  <p className="text-[11px] text-neutral-500">Authorised issuer</p>
                </div>
              )}
            </div>

            <div className="flex items-end gap-3">
              <div className="text-right text-[10px] leading-relaxed text-neutral-500">
                <p className="font-semibold uppercase tracking-widest text-neutral-600">Scan to verify</p>
                <p className="font-mono">{verifyPath}</p>
                <p className="mt-1 uppercase tracking-widest">Serial</p>
                <p className="font-mono text-neutral-700">{certificate.serialNumber}</p>
              </div>
              <div className="flex h-24 w-24 items-center justify-center rounded border border-neutral-300 bg-white p-1">
                {qrDataUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={qrDataUrl} alt="Verification QR code" className="h-full w-full" />
                ) : (
                  <span className="text-[9px] text-neutral-400">Generating QR…</span>
                )}
              </div>
            </div>
          </div>

          {isRevoked && certificate.revokedReason ? (
            <p className="mt-3 rounded border border-red-200 bg-red-50 px-3 py-1.5 text-center text-xs font-semibold text-red-700">
              This certificate was revoked on {formatDate(certificate.revokedAt)}: {certificate.revokedReason}
            </p>
          ) : null}
        </div>
      </div>
    </div>
  )
}
