"use client"

import { useCallback, useEffect, useState } from "react"
import { AlertTriangle, History, Loader2 } from "lucide-react"
import { useTranslations } from "next-intl"
import {
  certificateApi,
  type CertificateApprovalResponse,
  type CertificateApprovalStage,
  type CertificateDecisionStatus,
} from "@/lib/api"

/**
 * The full decision record for one certificate.
 *
 * <p>Refused and withdrawn decisions are shown, not just the current state. That is the point of
 * keeping them: a reviewer looking at an award needs to see that it was already sent back once and
 * why, rather than an audit trail that has been quietly trimmed to look tidy.</p>
 */
export default function CertificateApprovalHistory({
  certificateId,
  onDecided,
}: {
  certificateId: string
  onDecided?: () => void
}) {
  const t = useTranslations("certificates.approval")

  const [history, setHistory] = useState<CertificateApprovalResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setHistory(await certificateApi.approvalHistory(certificateId))
    } catch (err) {
      setError(err instanceof Error ? err.message : t("actionFailed"))
    } finally {
      setLoading(false)
    }
  }, [certificateId, t])

  useEffect(() => {
    void load()
  }, [load])

  async function withdraw(item: CertificateApprovalResponse) {
    setBusy(item.id)
    setError(null)
    try {
      await certificateApi.withdrawApproval(
        certificateId,
        item.stage,
        "Withdrawn by reviewer",
      )
      await load()
      onDecided?.()
    } catch (err) {
      setError(err instanceof Error ? err.message : t("actionFailed"))
    } finally {
      setBusy(null)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Loader2 className="size-5 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <History className="size-4 text-muted-foreground" />
        <h3 className="text-sm font-semibold text-foreground">{t("history")}</h3>
      </div>

      {error && (
        <div
          role="alert"
          className="flex items-start gap-2 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3"
        >
          <AlertTriangle className="mt-0.5 size-4 shrink-0 text-destructive" />
          <p className="text-sm text-destructive">{error}</p>
        </div>
      )}

      {!error && history.length === 0 && (
        <p className="text-sm text-muted-foreground">{t("historyEmpty")}</p>
      )}

      {history.length > 0 && (
        <ol className="space-y-3">
          {history.map((item) => (
            <li key={item.id} className="rounded-xl border border-border bg-card p-4">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-sm font-medium text-foreground">
                      {stageLabel(item.stage)}
                    </span>
                    <span className={statusClass(item.decisionStatus)}>
                      {statusLabel(item.decisionStatus)}
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {item.decidedAt
                      ? `${t("decidedBy")}: ${item.actorEmail ?? "—"} · ${formatDate(item.decidedAt)}`
                      : t("notDecidedYet")}
                  </p>
                  {item.reason && (
                    <p className="mt-2 text-sm text-foreground">{item.reason}</p>
                  )}
                  {item.templateVersion != null && (
                    <p className="mt-1 text-xs text-muted-foreground">
                      {t("templateVersion")}: {item.templateVersion}
                    </p>
                  )}
                </div>

                {item.decisionStatus === "APPROVED" && (
                  <button
                    type="button"
                    onClick={() => void withdraw(item)}
                    disabled={busy === item.id}
                    title={t("withdrawTitle")}
                    className="shrink-0 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground transition-colors hover:bg-muted disabled:opacity-50"
                  >
                    {busy === item.id && <Loader2 className="mr-1 inline size-3 animate-spin" />}
                    {t("withdraw")}
                  </button>
                )}
              </div>
            </li>
          ))}
        </ol>
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

  function statusLabel(status: CertificateDecisionStatus) {
    switch (status) {
      case "APPROVED":
        return t("statusApproved")
      case "REJECTED":
        return t("statusRejected")
      case "WITHDRAWN":
        return t("statusWithdrawn")
      default:
        return t("statusPending")
    }
  }

  function statusClass(status: CertificateDecisionStatus) {
    switch (status) {
      case "APPROVED":
        return "rounded bg-teal/10 px-2 py-0.5 text-[10px] font-semibold text-teal"
      case "REJECTED":
        return "rounded bg-destructive/10 px-2 py-0.5 text-[10px] font-semibold text-destructive"
      case "WITHDRAWN":
        return "rounded bg-muted px-2 py-0.5 text-[10px] font-semibold text-muted-foreground"
      default:
        return "rounded bg-orange/10 px-2 py-0.5 text-[10px] font-semibold text-orange"
    }
  }

  function formatDate(value: string) {
    try {
      return new Date(value).toLocaleString()
    } catch {
      return value
    }
  }
}
