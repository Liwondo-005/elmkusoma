"use client"

import { useCallback, useEffect, useState } from "react"
import { AlertTriangle, CheckCircle2, ClipboardCheck, Loader2, XCircle } from "lucide-react"
import { useTranslations } from "next-intl"
import {
  certificateApi,
  type CertificateApprovalResponse,
  type CertificateApprovalStage,
} from "@/lib/api"

/**
 * The reviewer's queue.
 *
 * <p>Issuance is gated on approvals, so without this screen the workflow could be started from the
 * certificate list but never completed: an operator could submit a certificate and then have no way
 * to see what was waiting or record a decision on it.</p>
 */
export default function CertificateApprovalQueuePage() {
  const t = useTranslations("certificates.approval")

  const [queue, setQueue] = useState<CertificateApprovalResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  // Rejection needs a reason, and the server enforces one. Collecting it in the same dialog that
  // performs the refusal means the operator is told why before submitting, not after.
  const [rejecting, setRejecting] = useState<CertificateApprovalResponse | null>(null)
  const [reason, setReason] = useState("")
  const [reasonError, setReasonError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setLoadError(null)
    try {
      setQueue(await certificateApi.approvalQueue())
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : t("actionFailed"))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    void load()
  }, [load])

  async function approve(item: CertificateApprovalResponse) {
    setBusyId(item.id)
    setActionError(null)
    try {
      await certificateApi.decideApproval(item.certificateId, item.stage, true)
      await load()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : t("actionFailed"))
    } finally {
      setBusyId(null)
    }
  }

  async function reject() {
    if (!rejecting) return
    if (!reason.trim()) {
      setReasonError(t("reasonRequired"))
      return
    }
    setBusyId(rejecting.id)
    setActionError(null)
    try {
      await certificateApi.decideApproval(rejecting.certificateId, rejecting.stage, false, reason.trim())
      setRejecting(null)
      setReason("")
      setReasonError(null)
      await load()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : t("actionFailed"))
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <header>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("queueTitle")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("subtitle")}</p>
      </header>

      {loading && (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {!loading && loadError && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center">
          <p className="text-sm font-medium text-destructive">{loadError}</p>
          <button
            type="button"
            onClick={() => void load()}
            className="mt-4 text-sm font-medium text-primary hover:underline"
          >
            Retry
          </button>
        </div>
      )}

      {!loading && !loadError && actionError && (
        <div
          role="alert"
          className="flex items-start gap-2 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3"
        >
          <AlertTriangle className="mt-0.5 size-4 shrink-0 text-destructive" />
          <p className="text-sm text-destructive">{actionError}</p>
        </div>
      )}

      {!loading && !loadError && queue.length === 0 && (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <ClipboardCheck className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">{t("queueEmpty")}</p>
          <p className="mt-1 text-sm text-muted-foreground">{t("queueEmptyHint")}</p>
        </div>
      )}

      {!loading && !loadError && queue.length > 0 && (
        <ul className="space-y-4">
          {queue.map((item) => (
            <li
              key={item.id}
              className="rounded-2xl border border-border bg-card p-5 shadow-xs"
            >
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded bg-orange/10 px-2 py-0.5 text-[10px] font-semibold text-orange">
                      {stageLabel(item.stage)}
                    </span>
                    <span className="text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
                      {item.certificateType ?? ""}
                    </span>
                  </div>

                  <h3 className="mt-2 text-base font-semibold text-foreground">
                    {item.certificateTitle ?? t("award")}
                  </h3>
                  <p className="mt-0.5 text-sm text-muted-foreground">
                    {item.studentName ?? "—"}
                  </p>
                  <p className="mt-1 font-mono text-xs text-muted-foreground">
                    {item.certificateSerialNumber ?? item.certificateId}
                  </p>

                  {item.templateVersion != null && (
                    <p className="mt-2 text-xs text-muted-foreground">
                      {t("templateVersion")}: {item.templateVersion}
                    </p>
                  )}
                </div>

                <div className="flex shrink-0 gap-2">
                  <button
                    type="button"
                    onClick={() => void approve(item)}
                    disabled={busyId === item.id}
                    className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-3 py-2 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:opacity-50"
                  >
                    {busyId === item.id ? (
                      <Loader2 className="size-4 animate-spin" />
                    ) : (
                      <CheckCircle2 className="size-4" />
                    )}
                    {t("approve")}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setRejecting(item)
                      setReason("")
                      setReasonError(null)
                    }}
                    disabled={busyId === item.id}
                    className="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-2 text-sm font-medium text-foreground transition-colors hover:bg-muted disabled:opacity-50"
                  >
                    <XCircle className="size-4" />
                    {t("reject")}
                  </button>
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}

      {rejecting && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="reject-title"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
        >
          <div className="w-full max-w-lg rounded-2xl border border-border bg-card p-6 shadow-lg">
            <h2 id="reject-title" className="text-lg font-semibold text-foreground">
              {t("reasonForRejection")}
            </h2>
            <p className="mt-1 text-sm text-muted-foreground">
              {rejecting.certificateTitle} — {rejecting.studentName}
            </p>

            <label className="mt-4 block text-sm font-medium text-foreground" htmlFor="reject-reason">
              {t("reason")}
            </label>
            <textarea
              id="reject-reason"
              value={reason}
              onChange={(e) => {
                setReason(e.target.value)
                if (reasonError) setReasonError(null)
              }}
              rows={4}
              placeholder={t("reasonPlaceholder")}
              aria-invalid={reasonError ? true : undefined}
              aria-describedby={reasonError ? "reject-reason-error" : undefined}
              className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-primary"
            />
            {reasonError && (
              <p id="reject-reason-error" role="alert" className="mt-1 text-xs text-destructive">
                {reasonError}
              </p>
            )}

            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => {
                  setRejecting(null)
                  setReason("")
                  setReasonError(null)
                }}
                className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-muted"
              >
                {t("cancel")}
              </button>
              <button
                type="button"
                onClick={() => void reject()}
                disabled={busyId === rejecting.id}
                className="inline-flex items-center gap-1.5 rounded-lg bg-destructive px-4 py-2 text-sm font-medium text-destructive-foreground transition-opacity hover:opacity-90 disabled:opacity-50"
              >
                {busyId === rejecting.id && <Loader2 className="size-4 animate-spin" />}
                {t("reject")}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )

  function stageLabel(stage: CertificateApprovalStage) {
    switch (stage) {
      case "PREPARATION":
        return t("stagePreparation")
      case "VALIDATION":
        return t("stageValidation")
      default:
        return t("stageApproval")
    }
  }
}
