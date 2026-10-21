"use client"

import { useEffect, useMemo, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"
import { AlertCircle, BadgeCheck, ClipboardCheck, FileText, Loader2, Plus, ShieldCheck } from "lucide-react"
import {
  teacherApi,
  type TeacherQualification,
  type TeacherVerificationStatus,
} from "@/lib/teacher-api"

/** Service categories (PricingService BAND_GOVERNED taxonomy). Documents required per category come from the server matrix. */
const SERVICE_TYPES = [
  "LIVE_CLASS",
  "LIVE_SESSION",
  "LIVE_EVENT",
  "VOCATIONAL_MODULE",
  "ACADEMIC_MODULE",
  "COURSE",
  "RESOURCE",
  "REPLAY",
] as const

const initialQual = {
  qualificationName: "",
  institutionName: "",
  fieldOfStudy: "",
  yearObtained: "",
  certificateUrl: "",
}

export default function TeacherVerificationPage() {
  const { user } = useAuth()
  const t = useTranslations("teacher")
  const tn = useTranslations("nav")
  const [status, setStatus] = useState<TeacherVerificationStatus | null>(null)
  const [qualifications, setQualifications] = useState<TeacherQualification[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  // Submission form
  const [showForm, setShowForm] = useState(false)
  const [selectedTypes, setSelectedTypes] = useState<string[]>([])
  const [docRows, setDocRows] = useState<Record<string, { url: string; notes: string }>>({})
  const [appNotes, setAppNotes] = useState("")
  const [submitting, setSubmitting] = useState(false)

  // Qualification form
  const [showQualForm, setShowQualForm] = useState(false)
  const [qualForm, setQualForm] = useState(initialQual)
  const [savingQual, setSavingQual] = useState(false)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const [verification, quals] = await Promise.all([
        teacherApi.getVerification(),
        teacherApi.getMyQualifications().catch(() => [] as TeacherQualification[]),
      ])
      setStatus(verification)
      setQualifications(Array.isArray(quals) ? quals : [])
    } catch {
      setError(t("verification.loadError"))
    } finally {
      setLoading(false)
    }
  }

  /** Union of the required document types across the selected service categories (server matrix). */
  const requiredDocTypes = useMemo(() => {
    const matrix = status?.requiredDocumentsByServiceType ?? {}
    const union: string[] = []
    for (const serviceType of selectedTypes) {
      for (const docType of matrix[serviceType] ?? []) {
        if (!union.includes(docType)) union.push(docType)
      }
    }
    return union
  }, [selectedTypes, status])

  function toggleServiceType(serviceType: string) {
    setSelectedTypes((prev) =>
      prev.includes(serviceType) ? prev.filter((s) => s !== serviceType) : [...prev, serviceType],
    )
  }

  async function handleSubmit() {
    if (selectedTypes.length === 0) {
      setError(t("verification.serviceTypesRequired"))
      return
    }
    try {
      setSubmitting(true)
      setError(null)
      const documents = requiredDocTypes.map((type) => ({
        type,
        url: (docRows[type]?.url ?? "").trim() || undefined,
        notes: (docRows[type]?.notes ?? "").trim() || undefined,
      }))
      await teacherApi.submitVerification({
        serviceTypes: selectedTypes,
        documents,
        notes: appNotes.trim() || undefined,
      })
      setSuccess(t("verification.submittedSuccess"))
      setShowForm(false)
      setSelectedTypes([])
      setDocRows({})
      setAppNotes("")
      loadData()
      setTimeout(() => setSuccess(null), 4000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("verification.submitError"))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleAddQualification() {
    if (!qualForm.qualificationName.trim() || !qualForm.institutionName.trim()) {
      setError(t("verification.qualsRequired"))
      return
    }
    try {
      setSavingQual(true)
      setError(null)
      await teacherApi.addMyQualification({
        qualificationName: qualForm.qualificationName.trim(),
        institutionName: qualForm.institutionName.trim(),
        fieldOfStudy: qualForm.fieldOfStudy.trim() || undefined,
        yearObtained: qualForm.yearObtained ? Number(qualForm.yearObtained) : undefined,
        certificateUrl: qualForm.certificateUrl.trim() || undefined,
      })
      setSuccess(t("verification.qualsAdded"))
      setQualForm(initialQual)
      setShowQualForm(false)
      loadData()
      setTimeout(() => setSuccess(null), 4000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("verification.qualsError"))
    } finally {
      setSavingQual(false)
    }
  }

  function formatDate(iso: string | null) {
    if (!iso) return "—"
    return new Date(iso).toLocaleString(undefined, { year: "numeric", month: "short", day: "numeric" })
  }

  function statusBadgeClasses(effectiveStatus: string) {
    switch (effectiveStatus) {
      case "APPROVED":
        return "border-green-500/30 bg-green-500/10 text-green-600"
      case "SUSPENDED":
      case "REJECTED":
        return "border-destructive/30 bg-destructive/10 text-destructive"
      case "PENDING":
      case "CHANGES_REQUIRED":
        return "border-amber-500/30 bg-amber-500/10 text-amber-600"
      default:
        return "border-border bg-muted text-muted-foreground"
    }
  }

  function statusLabel(effectiveStatus: string) {
    switch (effectiveStatus) {
      case "APPROVED":
        return t("verification.statusApproved")
      case "SUSPENDED":
        return t("verification.statusSuspended")
      case "PENDING":
        return t("verification.statusPending")
      case "CHANGES_REQUIRED":
        return t("verification.statusChangesRequired")
      case "REJECTED":
        return t("verification.statusRejected")
      default:
        return t("verification.statusProvisional")
    }
  }

  function statusExplanation(effectiveStatus: string) {
    switch (effectiveStatus) {
      case "APPROVED":
        return t("verification.approvedDesc")
      case "SUSPENDED":
        return t("verification.suspendedDesc")
      case "PENDING":
        return t("verification.pendingDesc")
      case "CHANGES_REQUIRED":
        return t("verification.changesRequiredDesc")
      case "REJECTED":
        return t("verification.rejectedDesc")
      default:
        return t("verification.provisionalDesc")
    }
  }

  /** Latest reviewer note (from the newest reviewed record). */
  const latestDecisionNote = useMemo(() => {
    if (!status) return null
    const reviewed = status.records.filter((r) => r.reviewedAt).slice(-1)
    return reviewed.length > 0 ? reviewed[0].notes : null
  }, [status])

  const canSubmit =
    status != null && status.effectiveStatus !== "PENDING" && status.effectiveStatus !== "SUSPENDED"

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("verification")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("verification.subtitle")}</p>
        </div>
        {canSubmit && (
          <Button className="gap-2" onClick={() => setShowForm(!showForm)}>
            {showForm ? <AlertCircle className="size-4" /> : <ClipboardCheck className="size-4" />}
            {showForm ? t("verification.closeForm") : t("verification.startApplication")}
          </Button>
        )}
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4 shrink-0" />
            {error}
          </div>
        </div>
      )}

      {success && (
        <div className="rounded-2xl border border-green-500/20 bg-green-500/5 p-4">
          <div className="flex items-center gap-2 text-sm text-green-600">
            <BadgeCheck className="size-4 shrink-0" />
            {success}
          </div>
        </div>
      )}

      {/* Status card */}
      {status && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-3">
          <div className="flex items-center gap-3">
            <ShieldCheck className="size-5 text-muted-foreground" />
            <span
              className={`inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold ${statusBadgeClasses(status.effectiveStatus)}`}
            >
              {statusLabel(status.effectiveStatus)}
            </span>
            {status.formallyVerified && (
              <span className="text-xs text-green-600">{t("verification.formallyVerifiedBadge")}</span>
            )}
          </div>
          <p className="text-sm text-muted-foreground">{statusExplanation(status.effectiveStatus)}</p>
          {latestDecisionNote && (
            <p className="rounded-xl border border-border bg-muted/50 p-3 text-sm text-foreground">
              {t("verification.reviewerNote")}: {latestDecisionNote}
            </p>
          )}
          {status.effectiveStatus === "SUSPENDED" && (
            <p className="text-sm text-destructive">{t("verification.suspendedBlocked")}</p>
          )}
          {status.effectiveStatus === "PROVISIONAL" && (
            <p className="text-sm text-amber-600">{t("verification.provisionalBlocked")}</p>
          )}
        </div>
      )}

      {/* Application form */}
      {showForm && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <h2 className="text-base font-semibold text-foreground">{t("verification.formTitle")}</h2>

          <div>
            <label className="mb-2 block text-xs font-medium text-muted-foreground">
              {t("verification.serviceTypesLabel")}
            </label>
            <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
              {SERVICE_TYPES.map((serviceType) => (
                <label
                  key={serviceType}
                  className="flex cursor-pointer items-center gap-2 rounded-lg border border-border bg-background px-3 py-2 text-sm"
                >
                  <input
                    type="checkbox"
                    checked={selectedTypes.includes(serviceType)}
                    onChange={() => toggleServiceType(serviceType)}
                    className="size-4"
                  />
                  {serviceType}
                </label>
              ))}
            </div>
            <p className="mt-1 text-xs text-muted-foreground">{t("verification.serviceTypesHint")}</p>
          </div>

          {requiredDocTypes.length > 0 && (
            <div className="space-y-3">
              <label className="block text-xs font-medium text-muted-foreground">
                {t("verification.requiredDocuments")}
              </label>
              {requiredDocTypes.map((docType) => (
                <div key={docType} className="rounded-xl border border-border bg-background p-3 space-y-2">
                  <p className="text-sm font-medium text-foreground">{docType}</p>
                  <input
                    type="url"
                    value={docRows[docType]?.url ?? ""}
                    onChange={(e) =>
                      setDocRows({ ...docRows, [docType]: { ...docRows[docType], url: e.target.value } })
                    }
                    placeholder={t("verification.docUrlPlaceholder")}
                    className="h-9 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                  />
                  <input
                    type="text"
                    value={docRows[docType]?.notes ?? ""}
                    onChange={(e) =>
                      setDocRows({ ...docRows, [docType]: { ...docRows[docType], notes: e.target.value } })
                    }
                    placeholder={t("verification.docNotesPlaceholder")}
                    className="h-9 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                  />
                </div>
              ))}
            </div>
          )}

          <div>
            <label className="mb-1 block text-xs font-medium text-muted-foreground">
              {t("verification.notesLabel")}
            </label>
            <textarea
              value={appNotes}
              onChange={(e) => setAppNotes(e.target.value)}
              rows={3}
              placeholder={t("verification.notesPlaceholder")}
              className="w-full resize-none rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring"
            />
          </div>

          <Button className="gap-2" onClick={handleSubmit} disabled={submitting}>
            {submitting ? <Loader2 className="size-4 animate-spin" /> : <FileText className="size-4" />}
            {t("verification.submit")}
          </Button>
        </div>
      )}

      {/* Requirements matrix */}
      {status && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-3">
          <h2 className="text-base font-semibold text-foreground">{t("verification.requirementsTitle")}</h2>
          <p className="text-sm text-muted-foreground">{t("verification.requirementsDesc")}</p>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs text-muted-foreground">
                  <th className="py-2 pr-4">{t("verification.colCategory")}</th>
                  <th className="py-2">{t("verification.colDocuments")}</th>
                </tr>
              </thead>
              <tbody>
                {Object.entries(status.requiredDocumentsByServiceType).map(([serviceType, docTypes]) => (
                  <tr key={serviceType} className="border-b border-border/50">
                    <td className="py-2 pr-4 font-medium text-foreground">{serviceType}</td>
                    <td className="py-2 text-muted-foreground">{docTypes.join(", ")}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Qualifications */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-base font-semibold text-foreground">{t("verification.qualsTitle")}</h2>
            <p className="text-sm text-muted-foreground">{t("verification.qualsDesc")}</p>
          </div>
          <Button variant="outline" className="gap-2" onClick={() => setShowQualForm(!showQualForm)}>
            <Plus className="size-4" />
            {t("verification.qualsAdd")}
          </Button>
        </div>

        {qualifications.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t("verification.qualsEmpty")}</p>
        ) : (
          <ul className="divide-y divide-border/50">
            {qualifications.map((qual) => (
              <li key={qual.id} className="py-3 text-sm">
                <p className="font-medium text-foreground">{qual.qualificationName}</p>
                <p className="text-muted-foreground">
                  {qual.institutionName ?? qual.institution ?? "—"}
                  {qual.yearObtained ? ` · ${qual.yearObtained}` : ""}
                </p>
              </li>
            ))}
          </ul>
        )}

        {showQualForm && (
          <div className="grid gap-3 rounded-xl border border-border bg-background p-4 sm:grid-cols-2">
            <input
              type="text"
              value={qualForm.qualificationName}
              onChange={(e) => setQualForm({ ...qualForm, qualificationName: e.target.value })}
              placeholder={t("verification.qualNamePlaceholder")}
              className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
            />
            <input
              type="text"
              value={qualForm.institutionName}
              onChange={(e) => setQualForm({ ...qualForm, institutionName: e.target.value })}
              placeholder={t("verification.qualInstitutionPlaceholder")}
              className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
            />
            <input
              type="text"
              value={qualForm.fieldOfStudy}
              onChange={(e) => setQualForm({ ...qualForm, fieldOfStudy: e.target.value })}
              placeholder={t("verification.qualFieldPlaceholder")}
              className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
            />
            <input
              type="number"
              value={qualForm.yearObtained}
              onChange={(e) => setQualForm({ ...qualForm, yearObtained: e.target.value })}
              placeholder={t("verification.qualYearPlaceholder")}
              className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
            />
            <input
              type="url"
              value={qualForm.certificateUrl}
              onChange={(e) => setQualForm({ ...qualForm, certificateUrl: e.target.value })}
              placeholder={t("verification.qualCertPlaceholder")}
              className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring sm:col-span-2"
            />
            <div className="sm:col-span-2">
              <Button onClick={handleAddQualification} disabled={savingQual} className="gap-2">
                {savingQual ? <Loader2 className="size-4 animate-spin" /> : <Plus className="size-4" />}
                {t("verification.qualsSave")}
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Review history */}
      {status && status.records.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-3">
          <h2 className="text-base font-semibold text-foreground">{t("verification.historyTitle")}</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs text-muted-foreground">
                  <th className="py-2 pr-4">{t("verification.colStatus")}</th>
                  <th className="py-2 pr-4">{t("verification.colSubmitted")}</th>
                  <th className="py-2 pr-4">{t("verification.colReviewed")}</th>
                  <th className="py-2">{t("verification.colNotes")}</th>
                </tr>
              </thead>
              <tbody>
                {[...status.records].reverse().map((record) => (
                  <tr key={record.id} className="border-b border-border/50">
                    <td className="py-2 pr-4">
                      <span
                        className={`inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-medium ${statusBadgeClasses(record.status)}`}
                      >
                        {record.status}
                      </span>
                    </td>
                    <td className="py-2 pr-4 text-muted-foreground">{formatDate(record.submittedAt)}</td>
                    <td className="py-2 pr-4 text-muted-foreground">{formatDate(record.reviewedAt)}</td>
                    <td className="py-2 text-muted-foreground">{record.notes ?? "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
