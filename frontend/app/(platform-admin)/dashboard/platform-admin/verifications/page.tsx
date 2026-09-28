"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { CheckCircle, XCircle, Loader2, ShieldCheck, Plus, MessageSquareWarning } from "lucide-react"
import { platformAdminApi, type VerificationSummary } from "@/lib/platform-admin-api"

export default function VerificationsPage() {
  const t = useTranslations("platformAdmin");
  const [items, setItems] = useState<VerificationSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [flash, setFlash] = useState<string | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const [showSubmit, setShowSubmit] = useState(false)
  const [entityType, setEntityType] = useState("INSTITUTION")
  const [entityId, setEntityId] = useState("")
  const [verificationType, setVerificationType] = useState("PROVIDER_LICENSE")
  const [documents, setDocuments] = useState("")
  const [submitNotes, setSubmitNotes] = useState("")
  const [reviewNotes, setReviewNotes] = useState("")

  const load = useCallback(() => {
    setLoading(true); setError(null)
    platformAdminApi.listPendingVerifications()
      .then(setItems)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  async function review(v: VerificationSummary, status: string) {
    if (status !== "APPROVED" && !reviewNotes.trim()) {
      setError("Notes are required when rejecting or requesting changes"); return
    }
    setBusy(v.id); setError(null); setFlash(null)
    try {
      // INSTITUTION/PROVIDER reviews go through the delegation-enforced endpoint;
      // SERVICE reviews use the generic endpoint (no delegation scope applies).
      if (v.entityType === "INSTITUTION" || v.entityType === "PROVIDER") {
        await platformAdminApi.reviewProviderVerification(v.id, status, reviewNotes || undefined)
      } else {
        await platformAdminApi.reviewVerification(v.id, status, reviewNotes || undefined)
      }
      setFlash(`Verification ${status.toLowerCase()}`)
      setReviewNotes("")
      setItems(prev => prev.filter(x => x.id !== v.id))
    } catch (e: any) {
      setError(e.message)
    } finally { setBusy(null) }
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    if (!entityId.trim()) { setError("Entity ID is required"); return }
    setBusy("submit"); setError(null); setFlash(null)
    try {
      await platformAdminApi.submitVerification({
        entityType, entityId: entityId.trim(), verificationType,
        documents: documents.trim() || undefined, notes: submitNotes.trim() || undefined,
      })
      setFlash("Verification request submitted")
      setEntityId(""); setDocuments(""); setSubmitNotes(""); setShowSubmit(false)
      load()
    } catch (err: any) {
      setError(err.message)
    } finally { setBusy(null) }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("verifications.pendingVerifications")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("verifications.reviewAndApprovePending")}</p>
        </div>
        <button onClick={() => setShowSubmit((s) => !s)} className="inline-flex items-center gap-2 self-start rounded-xl bg-primary px-3 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 sm:self-center">
          <Plus className="size-4" /> Submit request
        </button>
      </div>

      {flash && <div className="rounded-2xl border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">{flash}</div>}
      {error && <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      {showSubmit && (
        <form onSubmit={submit} className="space-y-3 rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-sm font-semibold text-foreground">Submit verification request</h2>
          <div className="grid gap-3 sm:grid-cols-3">
            <select value={entityType} onChange={(e) => setEntityType(e.target.value)} className="rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring">
              <option value="INSTITUTION">INSTITUTION</option>
              <option value="PROVIDER">PROVIDER</option>
              <option value="SERVICE">SERVICE</option>
            </select>
            <input value={entityId} onChange={(e) => setEntityId(e.target.value)} placeholder="Entity UUID" className="rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
            <input value={verificationType} onChange={(e) => setVerificationType(e.target.value)} placeholder="Verification type" className="rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
          </div>
          <input value={documents} onChange={(e) => setDocuments(e.target.value)} placeholder="Evidence / documents reference (optional)" className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
          <input value={submitNotes} onChange={(e) => setSubmitNotes(e.target.value)} placeholder="Notes (optional)" className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => setShowSubmit(false)} className="rounded-lg border border-border px-4 py-2 text-sm font-medium hover:bg-muted">Cancel</button>
            <button type="submit" disabled={busy === "submit"} className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
              {busy === "submit" && <Loader2 className="size-4 animate-spin" />} Submit
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <div className="flex justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : items.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-10 text-center">
          <ShieldCheck className="mx-auto size-10 text-green-500/50" />
          <p className="mt-3 text-sm text-muted-foreground">{t("verifications.allCaughtUpNo")}</p>
        </div>
      ) : (
        <div className="space-y-3">
          <input value={reviewNotes} onChange={(e) => setReviewNotes(e.target.value)} placeholder="Reviewer notes (required to reject or request changes)…" className="w-full rounded-xl border border-border bg-card px-4 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/20" />
          {items.map(v => (
            <div key={v.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-semibold text-foreground">{v.verificationType} — {v.entityType}</p>
                    <span className="rounded-full bg-yellow-50 px-2 py-0.5 text-[10px] font-semibold text-yellow-700">{v.status}</span>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {t("verifications.entity")}{v.entityId?.slice(0, 8)}{t("verifications.submitted")}{new Date(v.submittedAt).toLocaleString("en-GB")}
                  </p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <button onClick={() => review(v, "APPROVED")} disabled={busy !== null} className="inline-flex items-center gap-1 rounded-xl bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50">
                    {busy === v.id ? <Loader2 className="size-3 animate-spin" /> : <CheckCircle className="size-3" />} {t("verifications.approve")}</button>
                  <button onClick={() => review(v, "CHANGES_REQUIRED")} disabled={busy !== null} className="inline-flex items-center gap-1 rounded-xl border border-orange-300 px-3 py-1.5 text-xs font-medium text-orange-700 hover:bg-orange-50 disabled:opacity-50">
                    <MessageSquareWarning className="size-3" /> Request changes</button>
                  <button onClick={() => review(v, "REJECTED")} disabled={busy !== null} className="inline-flex items-center gap-1 rounded-xl border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50">
                    <XCircle className="size-3" /> {t("verifications.reject")}</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
