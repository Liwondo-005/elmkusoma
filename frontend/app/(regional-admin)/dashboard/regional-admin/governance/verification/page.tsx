"use client"

import { useCallback, useEffect, useState } from "react"
import { CheckCircle2, ChevronDown, ChevronUp, FileText, ShieldCheck } from "lucide-react"
import {
  regionalAdminApi,
  type VerificationDetail,
  type VerificationSummary,
} from "@/lib/regional-admin-api"
import {
  Chip,
  ErrorState,
  LoadingState,
  PageHeader,
  PagedList,
  formatDateTime,
} from "@/components/dashboard/regional-admin/ui"

const STATUS_OPTIONS = ["", "PENDING", "APPROVED", "REJECTED", "CHANGES_REQUIRED"]

function statusTone(status: string): "success" | "danger" | "warning" {
  if (status === "APPROVED") return "success"
  if (status === "REJECTED") return "danger"
  return "warning"
}

const ACTION_CLASS =
  "inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-50"

function message(e: unknown, fallback: string): string {
  return e instanceof Error ? e.message : fallback
}

function VerificationCard({
  item, open, onToggle, onReviewed,
}: {
  item: VerificationSummary
  open: boolean
  onToggle: () => void
  onReviewed: (message: string) => void
}) {
  const [detail, setDetail] = useState<VerificationDetail | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [attempted, setAttempted] = useState(false)
  const [notes, setNotes] = useState("")
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setAttempted(true)
    setLoading(true)
    setError(null)
    try {
      setDetail(await regionalAdminApi.getVerification(item.id))
    } catch (e) {
      setError(message(e, "Unable to load this verification."))
    } finally {
      setLoading(false)
    }
  }, [item.id])

  useEffect(() => {
    if (open && !attempted) load()
  }, [open, attempted, load])

  const reviewable =
    detail !== null && (detail.status === "PENDING" || detail.status === "CHANGES_REQUIRED")

  async function submit(status: string) {
    setSubmitting(true)
    setSubmitError(null)
    try {
      const trimmed = notes.trim()
      const updated = await regionalAdminApi.reviewVerification(item.id, {
        status,
        ...(trimmed ? { reviewerNotes: trimmed } : {}),
      })
      setDetail(updated)
      setNotes("")
      onReviewed(
        `Review saved for ${item.entityName} — status is now ${updated.status.replace("_", " ").toLowerCase()}.`,
      )
    } catch (e) {
      setSubmitError(message(e, "Unable to save the review."))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <article className="rounded-2xl border border-border bg-card p-5 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="text-sm font-bold text-foreground">{item.entityName}</h3>
            <Chip tone="info">{item.entityType}</Chip>
            <Chip tone={statusTone(item.status)}>{item.status}</Chip>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            {item.verificationType} · Submitted by {item.submittedBy} · {formatDateTime(item.submittedAt)}
          </p>
        </div>
        <button
          type="button"
          onClick={onToggle}
          aria-expanded={open}
          className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-background px-3 py-1.5 text-xs font-semibold hover:bg-muted"
        >
          <ShieldCheck className="size-3.5" />
          {open ? "Hide" : "Review"}
          {open ? <ChevronUp className="size-3.5" /> : <ChevronDown className="size-3.5" />}
        </button>
      </div>

      {open && (
        <div className="mt-4 space-y-4 border-t border-border pt-4">
          {loading && <LoadingState label="Loading verification details…" />}

          {!loading && error && <ErrorState message={error} onRetry={load} />}

          {!loading && !error && detail && (
            <>
              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
                    Documents
                  </p>
                  {detail.documents.length === 0 ? (
                    <p className="mt-2 text-sm text-muted-foreground">No documents attached.</p>
                  ) : (
                    <ul className="mt-2 space-y-1.5">
                      {detail.documents.map((doc, i) => (
                        <li
                          key={`${doc}-${i}`}
                          className="flex items-center gap-2 rounded-lg border border-border bg-muted/30 px-3 py-2 text-sm text-foreground"
                        >
                          <FileText className="size-4 shrink-0 text-muted-foreground" />
                          <span className="min-w-0 break-words">{doc}</span>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>

                <div>
                  <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
                    Reviewer notes
                  </p>
                  <p className="mt-2 text-sm text-foreground">
                    {detail.reviewerNotes ? (
                      detail.reviewerNotes
                    ) : (
                      <span className="text-muted-foreground">—</span>
                    )}
                  </p>
                  {(detail.reviewedBy || detail.reviewedAt) && (
                    <p className="mt-2 text-xs text-muted-foreground">
                      Reviewed by {detail.reviewedBy || "—"}
                      {detail.reviewedAt && <> · {formatDateTime(detail.reviewedAt)}</>}
                    </p>
                  )}
                </div>
              </div>

              {reviewable && (
                <div className="space-y-3 rounded-xl border border-border bg-muted/20 p-4">
                  <label
                    htmlFor={`review-notes-${item.id}`}
                    className="block text-[10px] font-bold uppercase tracking-widest text-muted-foreground"
                  >
                    Reviewer notes
                  </label>
                  <textarea
                    id={`review-notes-${item.id}`}
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    rows={3}
                    placeholder="Notes shown to the submitter (optional)"
                    className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
                  />
                  {submitError && <p className="text-sm text-destructive">{submitError}</p>}
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={() => submit("APPROVED")}
                      disabled={submitting}
                      className={`${ACTION_CLASS} bg-emerald-600 text-white hover:bg-emerald-700`}
                    >
                      <CheckCircle2 className="size-3.5" /> Approve
                    </button>
                    <button
                      type="button"
                      onClick={() => submit("CHANGES_REQUIRED")}
                      disabled={submitting}
                      className={`${ACTION_CLASS} bg-amber-500 text-white hover:bg-amber-600`}
                    >
                      Request changes
                    </button>
                    <button
                      type="button"
                      onClick={() => submit("REJECTED")}
                      disabled={submitting}
                      className={`${ACTION_CLASS} bg-red-600 text-white hover:bg-red-700`}
                    >
                      Reject
                    </button>
                    {submitting && (
                      <span className="self-center text-xs text-muted-foreground">Saving…</span>
                    )}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      )}
    </article>
  )
}

export default function VerificationPage() {
  const [status, setStatus] = useState("")
  const [reloadKey, setReloadKey] = useState(0)
  const [openId, setOpenId] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  // reloadKey is part of the identity of this fetcher: bumping it makes
  // PagedList re-run the same request after a review is submitted.
  const fetcher = useCallback(
    (p: { page: number; size: number; search?: string }) =>
      regionalAdminApi.listVerifications({ ...p, status }),
    [status, reloadKey],
  )

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title="Verification"
        description="Review pending entity verifications inside your jurisdiction."
      />

      {notice && (
        <div className="flex items-center justify-between gap-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          <span className="flex items-center gap-2">
            <CheckCircle2 className="size-4 shrink-0" /> {notice}
          </span>
          <button
            type="button"
            onClick={() => setNotice(null)}
            className="text-xs font-semibold underline"
          >
            Dismiss
          </button>
        </div>
      )}

      <PagedList<VerificationSummary>
        key={status}
        fetcher={fetcher}
        searchPlaceholder="Search verifications…"
        emptyTitle="No verifications match this filter."
        emptyHint="Verifications submitted by entities inside your jurisdiction will appear here."
        toolbar={
          <div className="flex items-center gap-2">
            <label
              htmlFor="verification-status"
              className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground"
            >
              Status
            </label>
            <select
              id="verification-status"
              value={status}
              onChange={(e) => {
                setStatus(e.target.value)
                setOpenId(null)
                setNotice(null)
              }}
              className="rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option || "all"} value={option}>
                  {option || "All"}
                </option>
              ))}
            </select>
          </div>
        }
        renderItem={(item) => (
          <VerificationCard
            item={item}
            open={openId === item.id}
            onToggle={() => setOpenId((current) => (current === item.id ? null : item.id))}
            onReviewed={(msg) => {
              setNotice(msg)
              setReloadKey((k) => k + 1)
            }}
          />
        )}
      />
    </div>
  )
}
