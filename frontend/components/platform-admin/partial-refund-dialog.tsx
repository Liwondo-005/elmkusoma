"use client"

import { useCallback, useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { Loader2, X } from "lucide-react"
import { platformAdminApi } from "@/lib/platform-admin-api"

/**
 * BR-02: request a partial refund against one payment.
 *
 * <p>The wording here carries more weight than usual. Creating this request does not move money -
 * it records an intent that enters the ordinary refund lifecycle and is executed by a separate
 * approval path. A dialog whose success message says "refunded" would put money in an operator's
 * head that the learner has not received, so every state below distinguishes REQUESTED from paid,
 * and the outstanding balance is re-read from the server rather than trusted from the list row,
 * because the row is a snapshot and the balance is what the server will accept now.
 *
 * <p>The amount cannot exceed the eligible balance. That is enforced again by the server; this
 * exists so an operator is not offered a figure that will be refused, not to replace the check.
 */
export default function PartialRefundDialog({
  paymentId,
  currency,
  onClose,
  onRequested,
}: {
  paymentId: string
  currency: string
  onClose: () => void
  onRequested: () => void | Promise<void>
}) {
  const t = useTranslations("platformAdmin.payments.refunds")

  const [remaining, setRemaining] = useState<number | null>(null)
  const [loadingBalance, setLoadingBalance] = useState(true)
  const [balanceError, setBalanceError] = useState<string | null>(null)

  const [amount, setAmount] = useState("")
  const [reason, setReason] = useState("")
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [requested, setRequested] = useState<string | null>(null)

  const loadBalance = useCallback(async () => {
    setLoadingBalance(true)
    setBalanceError(null)
    try {
      const res = await platformAdminApi.getRefundableAmount(paymentId)
      const value = res?.amount ?? null
      setRemaining(value)
      // Prefill with the whole eligible balance. An operator opening this dialog almost always
      // wants the full amount, and typing a figure that must match the balance exactly is a good
      // way to produce a typo the server refuses.
      if (value != null && Number(value) > 0) setAmount(String(value))
    } catch (e) {
      setBalanceError(e instanceof Error ? e.message : t("balanceFailed"))
    } finally {
      setLoadingBalance(false)
    }
  }, [paymentId, t])

  useEffect(() => {
    void loadBalance()
  }, [loadBalance])

  const parsed = Number(amount)
  const amountIsValid =
    Number.isFinite(parsed) &&
    parsed > 0 &&
    remaining != null &&
    parsed <= Number(remaining)

  async function submit() {
    if (!amountIsValid || submitting) return
    setSubmitting(true)
    setError(null)
    try {
      const created = await platformAdminApi.requestPartialRefund({
        paymentId,
        amount: parsed,
        reason: reason.trim() || undefined,
      })
      // Record the request id so the operator has something to search for. It is a request, not a
      // refund, and the message below says so in as many words.
      setRequested(created?.id ?? null)
      await onRequested()
    } catch (e) {
      setError(e instanceof Error ? e.message : t("requestFailed"))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="partial-refund-title"
    >
      <div className="w-full max-w-lg rounded-2xl border border-border bg-card p-6 shadow-lg">
        <div className="flex items-start justify-between gap-4">
          <h2 id="partial-refund-title" className="text-base font-semibold text-foreground">
            {t("title")}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label={t("close")}
            className="rounded-lg p-1 text-muted-foreground hover:bg-muted"
          >
            <X className="size-4" />
          </button>
        </div>

        <p className="mt-2 text-xs text-muted-foreground">
          {t("reference", { id: paymentId })}
        </p>

        {requested ? (
          <div className="mt-4 rounded-xl border border-border p-4">
            <p className="text-sm font-medium text-foreground">{t("requestedTitle")}</p>
            <p className="mt-1 text-sm text-muted-foreground">
              {t("requestedBody", { id: requested })}
            </p>
            <p className="mt-2 text-xs text-muted-foreground">{t("notYetPaid")}</p>
            <button
              type="button"
              onClick={onClose}
              className="mt-4 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground hover:bg-muted"
            >
              {t("done")}
            </button>
          </div>
        ) : (
          <>
            <p className="mt-3 rounded-lg bg-muted/60 px-3 py-2 text-xs text-muted-foreground">
              {t("requestOnlyNotice")}
            </p>

            <div className="mt-4">
              <label className="block text-sm font-medium text-foreground" htmlFor="refund-amount">
                {t("amountLabel")}
              </label>
              {loadingBalance ? (
                <div className="mt-2 flex items-center gap-2 text-sm text-muted-foreground">
                  <Loader2 className="size-4 animate-spin" />
                  {t("loadingBalance")}
                </div>
              ) : (
                <>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {t("remaining", {
                      amount: (remaining ?? 0).toLocaleString(),
                      currency,
                    })}
                  </p>
                  <input
                    id="refund-amount"
                    type="number"
                    inputMode="decimal"
                    min={0}
                    step="0.01"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    aria-describedby="refund-amount-help"
                    className="mt-2 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm"
                  />
                  <p id="refund-amount-help" className="mt-1 text-xs text-muted-foreground">
                    {t("amountHelp")}
                  </p>
                </>
              )}
            </div>

            <div className="mt-4">
              <label className="block text-sm font-medium text-foreground" htmlFor="refund-reason">
                {t("reasonLabel")}
              </label>
              <textarea
                id="refund-reason"
                rows={3}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder={t("reasonPlaceholder")}
                className="mt-2 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm"
              />
              <p className="mt-1 text-xs text-muted-foreground">{t("reasonHelp")}</p>
            </div>

            {balanceError && (
              <p role="alert" className="mt-3 text-sm text-destructive">
                {balanceError}
              </p>
            )}
            {error && (
              <p role="alert" className="mt-3 text-sm text-destructive">
                {error}
              </p>
            )}

            <div className="mt-5 flex gap-2">
              <button
                type="button"
                disabled={!amountIsValid || submitting || loadingBalance}
                onClick={() => void submit()}
                className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground disabled:opacity-50 hover:bg-muted"
              >
                {submitting ? t("submitting") : t("submit")}
              </button>
              <button
                type="button"
                onClick={onClose}
                className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-muted-foreground hover:bg-muted"
              >
                {t("cancel")}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}