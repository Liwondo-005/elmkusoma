"use client"

import { useCallback, useEffect, useState } from "react"
import {
  X, Loader2, AlertCircle, Printer, ExternalLink, ScrollText,
  BadgeCheck, ShieldAlert, User,
} from "lucide-react"
import {
  platformAdminApi,
  type CertificateDetail,
  type AuditLogEntry,
} from "@/lib/platform-admin-api"
import { CertificateDocument } from "@/components/certificates/certificate-document"

interface CertificateDetailDrawerProps {
  certificateId: string | null
  onClose: () => void
}

function Field({ label, value }: { label: string; value?: string | null }) {
  return (
    <div>
      <dt className="text-[11px] font-medium uppercase tracking-wide text-muted-foreground">{label}</dt>
      <dd className="mt-0.5 text-sm text-foreground">{value || <span className="text-muted-foreground">—</span>}</dd>
    </div>
  )
}

export function CertificateDetailDrawer({ certificateId, onClose }: CertificateDetailDrawerProps) {
  const [detail, setDetail] = useState<CertificateDetail | null>(null)
  const [audit, setAudit] = useState<AuditLogEntry[] | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [previewOpen, setPreviewOpen] = useState(false)

  const load = useCallback(async () => {
    if (!certificateId) return
    try {
      setLoading(true)
      setError(null)
      setDetail(null)
      setAudit(null)
      const res = await platformAdminApi.getCertificateDetail(certificateId)
      setDetail(res)
      try {
        setAudit(await platformAdminApi.getAuditLogs(0, 15, undefined, undefined, certificateId))
      } catch {
        setAudit(null) // honest "Data unavailable" state below
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load certificate detail")
    } finally {
      setLoading(false)
    }
  }, [certificateId])

  useEffect(() => {
    load()
  }, [load])

  // Close on Escape
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        if (previewOpen) setPreviewOpen(false)
        else onClose()
      }
    }
    window.addEventListener("keydown", onKey)
    return () => window.removeEventListener("keydown", onKey)
  }, [onClose, previewOpen])

  if (!certificateId) return null

  const cert = detail?.certificate
  const revoked = cert?.status === "REVOKED"

  return (
    <>
      <div className="fixed inset-0 z-40 bg-black/40" onClick={onClose} aria-hidden />
      <aside className="fixed inset-y-0 right-0 z-50 flex w-full max-w-xl flex-col border-l border-border bg-card shadow-2xl">
        <header className="flex items-center justify-between border-b border-border px-6 py-4">
          <div className="flex items-center gap-2">
            <ScrollText className="size-5 text-primary" />
            <div>
              <h2 className="text-base font-semibold text-foreground">Certificate detail</h2>
              <p className="text-xs text-muted-foreground">{certificateId}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg border border-border p-1.5 text-muted-foreground hover:bg-muted"
            aria-label="Close detail"
          >
            <X className="size-4" />
          </button>
        </header>

        <div className="flex-1 overflow-y-auto px-6 py-5 space-y-6">
          {loading ? (
            <div className="flex items-center justify-center py-16">
              <Loader2 className="size-6 animate-spin text-muted-foreground" />
            </div>
          ) : error ? (
            <div className="rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive flex items-center gap-2">
              <AlertCircle className="size-4 shrink-0" /> {error}
            </div>
          ) : cert ? (
            <>
              <div className="flex flex-wrap items-center gap-2">
                <span
                  className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${
                    cert.status === "ISSUED"
                      ? "bg-green-500/10 text-green-600"
                      : cert.status === "REVOKED"
                        ? "bg-red-500/10 text-red-600"
                        : "bg-muted text-muted-foreground"
                  }`}
                >
                  {cert.status}
                </span>
                <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground">
                  {cert.certificateType}
                </span>
                {detail?.templateName ? (
                  <span className="inline-block rounded-full bg-sky-500/10 px-2.5 py-0.5 text-xs font-medium text-sky-600">
                    {detail.templateName} v{detail.templateVersion ?? 1}
                  </span>
                ) : (
                  <span className="inline-block rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground">
                    No template
                  </span>
                )}
              </div>

              <div>
                <h3 className="text-lg font-semibold text-foreground">{cert.title}</h3>
                <p className="text-sm text-muted-foreground">{cert.courseOrProgramme || cert.courseTitle}</p>
              </div>

              {revoked && (
                <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                  <p className="flex items-center gap-2 font-semibold">
                    <ShieldAlert className="size-4" /> Revoked{cert.revokedAt ? ` — ${new Date(cert.revokedAt).toLocaleDateString("en-GB")}` : ""}
                  </p>
                  <p className="mt-1">{cert.revokedReason || "No reason recorded."}</p>
                </div>
              )}

              <div>
                <h4 className="mb-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Recipient</h4>
                <dl className="grid grid-cols-2 gap-x-4 gap-y-3">
                  <Field label="Full name" value={cert.studentName} />
                  <Field label="Student ID" value={cert.studentIdNumber} />
                  <Field label="Course / programme" value={cert.courseOrProgramme || cert.courseTitle} />
                  <Field label="Instructor" value={cert.instructorName} />
                  <Field label="Grade" value={cert.grade} />
                  <Field label="Skills" value={cert.skills?.length ? cert.skills.join(", ") : null} />
                </dl>
              </div>

              <div>
                <h4 className="mb-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Certificate</h4>
                <dl className="grid grid-cols-2 gap-x-4 gap-y-3">
                  <Field label="Serial number" value={cert.serialNumber} />
                  <Field label="Verification code" value={cert.verificationCode} />
                  <Field label="Issue date" value={cert.issueDate ? new Date(cert.issueDate).toLocaleDateString("en-GB") : null} />
                  <Field label="Completion date" value={cert.completionDate ? new Date(cert.completionDate).toLocaleDateString("en-GB") : null} />
                  <Field label="Expiry date" value={cert.expiryDate ? new Date(cert.expiryDate).toLocaleDateString("en-GB") : null} />
                  <Field label="Issued by" value={detail?.institutionName} />
                </dl>
              </div>

              <div>
                <h4 className="mb-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Authorised signatories</h4>
                {detail && detail.signatories.length > 0 ? (
                  <ul className="space-y-2">
                    {detail.signatories.map((s) => (
                      <li key={s.id} className="flex items-center gap-3 rounded-xl border border-border bg-muted/40 px-3 py-2">
                        {s.signatureImage ? (
                          // eslint-disable-next-line @next/next/no-img-element
                          <img src={s.signatureImage} alt="" className="h-8 w-16 object-contain" />
                        ) : (
                          <div className="flex h-8 w-10 items-center justify-center rounded bg-muted">
                            <User className="size-4 text-muted-foreground" />
                          </div>
                        )}
                        <div className="min-w-0">
                          <p className="truncate text-sm font-medium text-foreground">{s.fullName}</p>
                          <p className="truncate text-xs text-muted-foreground">
                            {[s.positionTitle, s.organization].filter(Boolean).join(" · ") || "—"}
                          </p>
                        </div>
                        <span className="ml-auto shrink-0 rounded-full bg-green-500/10 px-2 py-0.5 text-[10px] font-semibold text-green-600">
                          {s.status}
                        </span>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="rounded-xl border border-dashed border-border px-4 py-3 text-sm text-muted-foreground">
                    No signatories linked{detail?.templateName ? " to this template" : " (certificate has no template)"}.
                  </p>
                )}
              </div>

              <div>
                <h4 className="mb-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Verification</h4>
                <a
                  href={`/certificates/verify/${cert.verificationCode}`}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex items-center gap-2 rounded-xl border border-teal/30 bg-teal/5 px-4 py-2 text-sm font-medium text-teal hover:bg-teal/10"
                >
                  <ExternalLink className="size-4" /> Open public verification page
                </a>
              </div>

              <div>
                <h4 className="mb-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  Audit trail (this certificate)
                </h4>
                {audit === null ? (
                  <p className="rounded-xl border border-dashed border-border px-4 py-3 text-sm text-muted-foreground">
                    Data unavailable
                  </p>
                ) : audit.length === 0 ? (
                  <p className="rounded-xl border border-dashed border-border px-4 py-3 text-sm text-muted-foreground">
                    No audit entries recorded yet.
                  </p>
                ) : (
                  <ul className="space-y-1.5">
                    {audit.map((entry) => (
                      <li key={entry.id} className="flex items-center justify-between gap-3 text-xs">
                        <span className="flex items-center gap-2">
                          <span className="rounded bg-muted px-1.5 py-0.5 font-semibold text-muted-foreground">{entry.action}</span>
                          <span className="truncate text-foreground">{entry.performedBy || "system"}</span>
                        </span>
                        <span className="shrink-0 text-muted-foreground">
                          {new Date(entry.createdAt).toLocaleString("en-GB", { dateStyle: "short", timeStyle: "short" })}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </>
          ) : null}
        </div>

        <footer className="border-t border-border px-6 py-4 flex items-center gap-2">
          <button
            onClick={() => setPreviewOpen(true)}
            disabled={!cert}
            className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50"
          >
            <BadgeCheck className="size-4" /> Preview certificate
          </button>
          <button
            onClick={onClose}
            className="rounded-xl border border-border px-4 py-2 text-sm text-muted-foreground hover:bg-muted"
          >
            Close
          </button>
        </footer>
      </aside>

      {/* Print-ready preview */}
      {previewOpen && cert ? (
        <div className="fixed inset-0 z-[60] overflow-y-auto bg-black/70 p-4 sm:p-8 print:static print:bg-transparent print:p-0">
          <div className="mx-auto w-full max-w-5xl">
            <div className="mb-4 flex items-center justify-between print:hidden">
              <p className="text-sm font-medium text-white">Certificate preview — print or save as PDF</p>
              <div className="flex gap-2">
                <button
                  onClick={() => window.print()}
                  className="inline-flex items-center gap-2 rounded-xl bg-white px-4 py-2 text-sm font-semibold text-neutral-900 hover:bg-neutral-100"
                >
                  <Printer className="size-4" /> Print / Save as PDF
                </button>
                <button
                  onClick={() => setPreviewOpen(false)}
                  className="rounded-xl border border-white/30 px-4 py-2 text-sm text-white hover:bg-white/10"
                >
                  Close
                </button>
              </div>
            </div>
            <CertificateDocument
              certificate={{
                serialNumber: cert.serialNumber,
                certificateType: cert.certificateType,
                title: cert.title,
                courseTitle: cert.courseTitle,
                courseOrProgramme: cert.courseOrProgramme,
                description: cert.description,
                studentName: cert.studentName,
                studentIdNumber: cert.studentIdNumber,
                instructorName: cert.instructorName,
                grade: cert.grade,
                skills: cert.skills,
                completionDate: cert.completionDate,
                issueDate: cert.issueDate,
                expiryDate: cert.expiryDate,
                status: cert.status,
                verificationCode: cert.verificationCode,
                verificationUrl: cert.verificationUrl,
                revokedReason: cert.revokedReason,
                revokedAt: cert.revokedAt,
              }}
              institutionName={detail?.institutionName}
              signatories={detail?.signatories || []}
            />
          </div>
        </div>
      ) : null}
    </>
  )
}
