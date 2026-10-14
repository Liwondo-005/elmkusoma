"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import { useTranslations } from "next-intl"
import { CreditCard, Loader2, CheckCircle, Clock, AlertCircle, Smartphone, RefreshCw } from "lucide-react"
import {
  parentApi,
  MOBILE_MONEY_NETWORKS,
  normaliseMsisdn,
  type ChildOverview,
  type MobileMoneyNetwork,
  type PaymentItem,
  type ParentPayments,
  type PricingQuote,
} from "@/lib/parent-api"
import { useLocale } from "@/hooks/use-locale"
import { formatDate } from "@/lib/format-date"

/**
 * Priced services a payer can buy. The figures are never shown from this list:
 * the amount displayed (and paid) always comes from the server's pricing
 * quote, because an ACTIVE pricing policy is the only authority on what a
 * service costs.
 */
const PAYABLE_SERVICES = [
  "DAILY_ACCESS",
  "WEEKLY_ACCESS",
  "MONTHLY_ACCESS",
  "BASIC_ACCESS",
  "PLUS_ACCESS",
  "PREMIUM_ACCESS",
] as const

/** How often the backend re-checks the transaction, and for how long. */
const POLL_INTERVAL_MS = 5000
const POLL_MAX_ATTEMPTS = 24

export default function ParentPaymentsPage() {
  const t = useTranslations("parent")
  const tn = useTranslations("nav")
  const [payments, setPayments] = useState<ParentPayments | null>(null)
  const [loading, setLoading] = useState(true)
  const [checkout, setCheckout] = useState<PaymentItem | null>(null)

  const loadPayments = useCallback(() => {
    return parentApi
      .getPayments()
      .then(setPayments)
      .catch(() => setPayments(null))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    loadPayments()
  }, [loadPayments])

  if (loading)
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("payments")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("payments.subtitle")}</p>
      </div>

      {/* Summary */}
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{t("payments.outstanding")}</p>
          <p className="mt-1 text-2xl font-extrabold text-red-600">
            TSh {(payments?.outstanding || 0).toLocaleString()}
          </p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{t("payments.paidThisTerm")}</p>
          <p className="mt-1 text-2xl font-extrabold text-green-600">
            TSh {(payments?.paidThisTerm || 0).toLocaleString()}
          </p>
        </div>
      </div>

      {checkout && (
        <MobileMoneyCheckout
          payment={checkout}
          onClose={() => setCheckout(null)}
          onSettled={loadPayments}
        />
      )}

      <NewPaymentForm onInitiated={loadPayments} />

      {/* Pending */}
      {payments && payments.pendingPayments.length > 0 && (
        <section className="rounded-2xl border border-orange-200 bg-orange-50 p-5 shadow-xs dark:border-orange-900 dark:bg-orange-950/30">
          <h2 className="text-base font-semibold text-foreground">{t("payments.pendingTitle")}</h2>
          <div className="mt-3 space-y-2">
            {payments.pendingPayments.map((p) => (
              <PaymentRow
                key={p.id}
                payment={p}
                action={
                  p.status === "PENDING" ? (
                    <button
                      type="button"
                      onClick={() => setCheckout(p)}
                      className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-card px-3 py-1.5 text-xs font-semibold text-foreground hover:bg-muted"
                    >
                      <Smartphone className="size-3.5" />
                      {t("payments.payNow")}
                    </button>
                  ) : undefined
                }
              />
            ))}
          </div>
        </section>
      )}

      {/* History */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-base font-semibold text-foreground">{t("payments.historyTitle")}</h2>
        {!payments || payments.recentPayments.length === 0 ? (
          <div className="mt-6 py-8 text-center">
            <CreditCard className="mx-auto mb-3 size-8 text-muted-foreground" />
            <p className="text-sm text-muted-foreground">{t("payments.noHistory")}</p>
          </div>
        ) : (
          <div className="mt-3 space-y-2">
            {payments.recentPayments.map((p) => (
              <PaymentRow key={p.id} payment={p} />
            ))}
          </div>
        )}
      </section>
    </div>
  )
}

/**
 * Mobile-money checkout for one pending payment.
 *
 * The whole flow is server-authoritative: the amount comes from the stored
 * payment, the network/phone are validated client-side only for a faster
 * error, and success is whatever the backend reports after it re-checks the
 * transaction with the gateway. Nothing here can mark a payment paid.
 */
function MobileMoneyCheckout({
  payment,
  onClose,
  onSettled,
}: {
  payment: PaymentItem
  onClose: () => void
  onSettled: () => void | Promise<void>
}) {
  const t = useTranslations("parent")
  const [network, setNetwork] = useState<MobileMoneyNetwork>("Mpesa")
  const [phone, setPhone] = useState("")
  const [phase, setPhase] = useState<"form" | "polling" | "success" | "failed" | "timeout">("form")
  const [error, setError] = useState<string | null>(null)
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    return () => {
      if (timer.current) clearTimeout(timer.current)
    }
  }, [])

  const poll = useCallback(
    async (attempt: number) => {
      try {
        const result = await parentApi.getPaymentStatus(payment.id)
        if (result.status === "COMPLETED") {
          setPhase("success")
          onSettled()
          return
        }
        if (result.status === "FAILED" || result.status === "CANCELLED") {
          setPhase("failed")
          onSettled()
          return
        }
        if (attempt >= POLL_MAX_ATTEMPTS) {
          setPhase("timeout")
          onSettled()
          return
        }
        timer.current = setTimeout(() => poll(attempt + 1), POLL_INTERVAL_MS)
      } catch (err) {
        // A temporary gateway or network problem must not be shown as a
        // failure: the payment may still complete. Keep polling until the
        // attempt budget runs out.
        if (attempt >= POLL_MAX_ATTEMPTS) {
          setPhase("timeout")
          onSettled()
          return
        }
        timer.current = setTimeout(() => poll(attempt + 1), POLL_INTERVAL_MS)
      }
    },
    [payment.id, onSettled],
  )

  async function submit() {
    setError(null)
    const msisdn = normaliseMsisdn(phone)
    if (!msisdn) {
      setError(t("payments.invalidPhone"))
      return
    }
    try {
      await parentApi.payWithMobileMoney(payment.id, { mobileNumber: msisdn, network })
      setPhase("polling")
      poll(1)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("payments.payFailed"))
    }
  }

  return (
    <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold text-foreground">{t("payments.checkoutTitle")}</h2>
          <p className="mt-0.5 text-sm text-muted-foreground">
            {payment.description || payment.serviceType}
          </p>
        </div>
        <p className="text-lg font-extrabold text-foreground">
          TSh {payment.amount.toLocaleString()}
        </p>
      </div>

      {phase === "form" && (
        <div className="mt-4 space-y-4">
          <div>
            <label className="mb-1.5 block text-xs font-medium text-muted-foreground">
              {t("payments.network")}
            </label>
            <div className="grid grid-cols-2 gap-2">
              {MOBILE_MONEY_NETWORKS.map((n) => (
                <button
                  key={n.value}
                  type="button"
                  onClick={() => setNetwork(n.value)}
                  className={`rounded-lg border px-3 py-2 text-xs font-semibold transition-colors ${
                    network === n.value
                      ? "border-primary bg-primary/10 text-foreground"
                      : "border-border bg-card text-muted-foreground hover:bg-muted"
                  }`}
                >
                  {n.label}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="mb-1.5 block text-xs font-medium text-muted-foreground" htmlFor="mm-phone">
              {t("payments.phone")}
            </label>
            <input
              id="mm-phone"
              type="tel"
              inputMode="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="0742 000 331"
              className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-primary"
            />
            <p className="mt-1 text-xs text-muted-foreground">{t("payments.phoneHint")}</p>
          </div>

          {error && (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700 dark:bg-red-950/40 dark:text-red-300">
              {error}
            </p>
          )}

          <div className="flex gap-2">
            <button
              type="button"
              onClick={submit}
              className="inline-flex flex-1 items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90"
            >
              <Smartphone className="size-4" />
              {t("payments.sendPrompt")}
            </button>
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-border px-4 py-2.5 text-sm font-medium text-muted-foreground hover:bg-muted"
            >
              {t("payments.cancel")}
            </button>
          </div>
        </div>
      )}

      {phase === "polling" && (
        <div className="mt-4 flex items-start gap-3 rounded-lg bg-amber-50 p-4 dark:bg-amber-950/30">
          <Loader2 className="mt-0.5 size-4 shrink-0 animate-spin text-amber-600" />
          <div>
            <p className="text-sm font-semibold text-foreground">{t("payments.awaitingApproval")}</p>
            <p className="mt-1 text-xs text-muted-foreground">{t("payments.awaitingHint")}</p>
          </div>
        </div>
      )}

      {phase === "success" && (
        <div className="mt-4 flex items-start gap-3 rounded-lg bg-green-50 p-4 dark:bg-green-950/30">
          <CheckCircle className="mt-0.5 size-4 shrink-0 text-green-600" />
          <div>
            <p className="text-sm font-semibold text-foreground">{t("payments.paymentComplete")}</p>
            <p className="mt-1 text-xs text-muted-foreground">{t("payments.receiptHint")}</p>
          </div>
        </div>
      )}

      {phase === "failed" && (
        <div className="mt-4 flex items-start gap-3 rounded-lg bg-red-50 p-4 dark:bg-red-950/30">
          <AlertCircle className="mt-0.5 size-4 shrink-0 text-red-600" />
          <div>
            <p className="text-sm font-semibold text-foreground">{t("payments.paymentFailed")}</p>
            <p className="mt-1 text-xs text-muted-foreground">{t("payments.retryHint")}</p>
            <button
              type="button"
              onClick={() => setPhase("form")}
              className="mt-2 inline-flex items-center gap-1.5 rounded-lg border border-border bg-card px-3 py-1.5 text-xs font-semibold text-foreground hover:bg-muted"
            >
              <RefreshCw className="size-3.5" />
              {t("payments.tryAgain")}
            </button>
          </div>
        </div>
      )}

      {phase === "timeout" && (
        <div className="mt-4 flex items-start gap-3 rounded-lg bg-amber-50 p-4 dark:bg-amber-950/30">
          <Clock className="mt-0.5 size-4 shrink-0 text-amber-600" />
          <div>
            <p className="text-sm font-semibold text-foreground">{t("payments.stillPending")}</p>
            <p className="mt-1 text-xs text-muted-foreground">{t("payments.stillPendingHint")}</p>
          </div>
        </div>
      )}
    </section>
  )
}

/**
 * Creates a new payment for a priced service and immediately starts a mobile
 * money collection for it. This is the entry point for both access passes and
 * subscription tiers - they are the same priced purchase end to end, so this
 * deliberately does not build a second checkout.
 */
function NewPaymentForm({ onInitiated }: { onInitiated: () => void | Promise<void> }) {
  const t = useTranslations("parent")
  const [open, setOpen] = useState(false)
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [childId, setChildId] = useState("")
  const [serviceType, setServiceType] = useState<string>(PAYABLE_SERVICES[2])
  const [quote, setQuote] = useState<PricingQuote | null>(null)
  const [quoteError, setQuoteError] = useState<string | null>(null)
  const [network, setNetwork] = useState<MobileMoneyNetwork>("Mpesa")
  const [phone, setPhone] = useState("")
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<string | null>(null)
  const polling = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    if (!open) return
    parentApi.getChildren().then(setChildren).catch(() => setChildren([]))
  }, [open])

  useEffect(() => {
    return () => {
      if (polling.current) clearTimeout(polling.current)
    }
  }, [])

  useEffect(() => {
    if (!open) return
    setQuote(null)
    setQuoteError(null)
    parentApi
      .getPricingQuote(serviceType)
      .then(setQuote)
      .catch((err) => setQuoteError(err instanceof Error ? err.message : t("payments.noPricing")))
  }, [open, serviceType, t])

  async function submit() {
    setError(null)
    setResult(null)
    const msisdn = normaliseMsisdn(phone)
    if (!msisdn) {
      setError(t("payments.invalidPhone"))
      return
    }
    if (!childId) {
      setError(t("payments.selectChild"))
      return
    }
    if (!quote) {
      setError(t("payments.noPricing"))
      return
    }
    setBusy(true)
    try {
      const initiated = await parentApi.initiatePayment({
        studentId: childId,
        amount: quote.amount,
        serviceType,
        idempotencyKey: typeof crypto !== "undefined" && "randomUUID" in crypto
          ? crypto.randomUUID()
          : `${serviceType}-${childId}-${Date.now()}`,
        mobileNumber: msisdn,
        network,
      })
      setResult(t("payments.awaitingApproval"))
      // The prompt is queued; completion is confirmed by the backend only.
      const check = async (attempt: number) => {
        try {
          const status = await parentApi.getPaymentStatus(initiated.paymentId)
          if (status.status === "COMPLETED") {
            setResult(t("payments.paymentComplete"))
            onInitiated()
            return
          }
          if (status.status === "FAILED" || status.status === "CANCELLED") {
            setResult(t("payments.paymentFailed"))
            onInitiated()
            return
          }
        } catch {
          // keep polling - a transient error is not a failure
        }
        if (attempt >= POLL_MAX_ATTEMPTS) {
          setResult(t("payments.stillPending"))
          onInitiated()
          return
        }
        polling.current = setTimeout(() => check(attempt + 1), POLL_INTERVAL_MS)
      }
      check(1)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("payments.payFailed"))
    } finally {
      setBusy(false)
    }
  }

  if (!open) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="inline-flex items-center gap-2 rounded-2xl border border-border bg-card px-4 py-2.5 text-sm font-semibold text-foreground shadow-xs hover:bg-muted"
      >
        <Smartphone className="size-4" />
        {t("payments.newPayment")}
      </button>
    )
  }

  return (
    <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className="flex items-start justify-between gap-3">
        <h2 className="text-base font-semibold text-foreground">{t("payments.newPayment")}</h2>
        <button
          type="button"
          onClick={() => setOpen(false)}
          className="text-xs font-medium text-muted-foreground hover:text-foreground"
        >
          {t("payments.close")}
        </button>
      </div>

      <div className="mt-4 space-y-4">
        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <label className="mb-1.5 block text-xs font-medium text-muted-foreground" htmlFor="np-child">
              {t("payments.child")}
            </label>
            <select
              id="np-child"
              value={childId}
              onChange={(e) => setChildId(e.target.value)}
              className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-primary"
            >
              <option value="">{t("payments.selectChild")}</option>
              {children.map((c) => (
                <option key={c.studentId} value={c.studentId}>
                  {c.studentName}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1.5 block text-xs font-medium text-muted-foreground" htmlFor="np-service">
              {t("payments.service")}
            </label>
            <select
              id="np-service"
              value={serviceType}
              onChange={(e) => setServiceType(e.target.value)}
              className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-primary"
            >
              {PAYABLE_SERVICES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </div>
        </div>

        {quote ? (
          <div className="flex items-center justify-between rounded-lg bg-muted/40 px-4 py-3">
            <span className="text-sm text-muted-foreground">{t("payments.payableAmount")}</span>
            <span className="text-lg font-extrabold text-foreground">
              TSh {quote.netAmount.toLocaleString()}
              {quote.discountAmount > 0 && (
                <span className="ml-2 text-xs font-medium text-green-600 line-through">
                  TSh {quote.amount.toLocaleString()}
                </span>
              )}
            </span>
          </div>
        ) : (
          <p className="rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-800 dark:bg-amber-950/30 dark:text-amber-300">
            {quoteError || t("payments.loadingQuote")}
          </p>
        )}

        <div>
          <label className="mb-1.5 block text-xs font-medium text-muted-foreground">
            {t("payments.network")}
          </label>
          <div className="grid grid-cols-2 gap-2">
            {MOBILE_MONEY_NETWORKS.map((n) => (
              <button
                key={n.value}
                type="button"
                onClick={() => setNetwork(n.value)}
                className={`rounded-lg border px-3 py-2 text-xs font-semibold transition-colors ${
                  network === n.value
                    ? "border-primary bg-primary/10 text-foreground"
                    : "border-border bg-card text-muted-foreground hover:bg-muted"
                }`}
              >
                {n.label}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label className="mb-1.5 block text-xs font-medium text-muted-foreground" htmlFor="np-phone">
            {t("payments.phone")}
          </label>
          <input
            id="np-phone"
            type="tel"
            inputMode="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="0742 000 331"
            className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-primary"
          />
        </div>

        {error && (
          <p className="rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700 dark:bg-red-950/40 dark:text-red-300">
            {error}
          </p>
        )}
        {result && (
          <p className="rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-800 dark:bg-amber-950/30 dark:text-amber-300">
            {result}
          </p>
        )}

        <button
          type="button"
          onClick={submit}
          disabled={busy || !quote}
          className="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50"
        >
          {busy ? <Loader2 className="size-4 animate-spin" /> : <Smartphone className="size-4" />}
          {t("payments.sendPrompt")}
        </button>
      </div>
    </section>
  )
}

function PaymentRow({
  payment,
  action,
}: {
  payment: PaymentItem
  action?: React.ReactNode
}) {
  const t = useTranslations("parent")
  const ts = useTranslations("status")
  const locale = useLocale()
  const statusConfig: Record<string, { icon: typeof CheckCircle; color: string; label: string }> = {
    COMPLETED: { icon: CheckCircle, color: "text-green-600", label: t("payments.statusPaid") },
    PENDING: { icon: Clock, color: "text-orange", label: ts("pending") },
    PROCESSING: { icon: Clock, color: "text-blue-500", label: t("payments.statusProcessing") },
    FAILED: { icon: AlertCircle, color: "text-red-500", label: t("payments.statusFailed") },
    CANCELLED: { icon: AlertCircle, color: "text-muted-foreground", label: ts("cancelled") },
  }
  const cfg = statusConfig[payment.status] || statusConfig.PENDING
  const StatusIcon = cfg.icon

  return (
    <div className="flex items-center gap-3 rounded-xl border border-border bg-muted/30 p-3">
      <StatusIcon className={`size-4 shrink-0 ${cfg.color}`} />
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-foreground">{payment.description || payment.serviceType}</p>
        <p className="text-xs text-muted-foreground">
          {payment.paidAt ? formatDate(payment.paidAt, locale) : formatDate(payment.createdAt, locale)}
          {payment.providerReference && ` · ${payment.providerReference}`}
        </p>
      </div>
      <div className="text-right">
        <p className="text-sm font-bold text-foreground">TSh {payment.amount.toLocaleString()}</p>
        <p className={`text-xs ${cfg.color}`}>{cfg.label}</p>
      </div>
      {action}
    </div>
  )
}
