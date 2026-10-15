"use client"

import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import { useTranslations } from "next-intl"
import {
  Loader2,
  AlertCircle,
  CreditCard,
  RefreshCw,
  Download,
  ShieldCheck,
  CheckCircle,
  Clock,
  Smartphone,
} from "lucide-react"
import {
  parentApi,
  MOBILE_MONEY_NETWORKS,
  normaliseMsisdn,
  type PaymentItem,
  type ParentPayments,
  type ChildOverview,
  type EntitlementItem,
  type MobileMoneyNetwork,
  type PricingQuote,
} from "@/lib/parent-api"
import { useLocale } from "@/hooks/use-locale"
import { formatDate } from "@/lib/format-date"
import { formatMoney } from "@/lib/money"
import {
  presentPaymentStatus,
  isCancellable,
  statusToneClasses,
  describeExpiry,
} from "@/lib/payment-status"
import { toCsv, downloadCsv, exportFilename } from "@/lib/csv"

/**
 * Parent payments.
 *
 * Reads real records from GET /v1/my/payments. Two properties this page is built around:
 *
 *  - A pending payment is never shown as a paid one. The backend splits the response into
 *    recentPayments and pendingPayments and computes paidThisTerm from COMPLETED rows only;
 *    this page renders the split it was given rather than re-deriving it, and any status the
 *    classifier does not recognise is shown verbatim instead of being folded into either group.
 *
 *  - Cancelling never loses the reference. The backend rejects a cancel on a payment that has
 *    already settled, and its error arrives without a status code, so a failed cancel leaves the
 *    row on screen and asks the parent to refresh rather than optimistically hiding it.
 *
 * Paying never lets the browser state the price. The checkout below takes its amount from
 * GET /v1/my/pricing/quote and the backend refuses an initiate whose amount disagrees with the
 * ACTIVE pricing policy - which is exactly what makes a pay button safe to show at all. Before
 * that quote existed this page deliberately had no pay button, since initiate used to take the
 * amount from the caller.
 */

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
  const tp = useTranslations("parent.payments")
  const locale = useLocale()

  const [payments, setPayments] = useState<ParentPayments | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [cancellingId, setCancellingId] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  // Which pending payment the mobile-money checkout panel is open for. Nothing is collected
  // here - the panel drives the gateway and the backend is what reports a payment settled.
  const [checkout, setCheckout] = useState<PaymentItem | null>(null)

  // Child selection. The payer is always the signed-in parent; the selected child is the
  // recipient. Keeping those two roles distinct on screen is the point of the selector - a
  // family payment record is not the child's own account.
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [selectedChildId, setSelectedChildId] = useState<string>("")
  const [childPayments, setChildPayments] = useState<PaymentItem[] | null>(null)
  const [childAccess, setChildAccess] = useState<EntitlementItem[] | null>(null)
  const [childLoading, setChildLoading] = useState(false)
  const [childError, setChildError] = useState<string | null>(null)

  // Date range, applied to the rows on screen. Scoped to what has been fetched - see the note
  // above the control.
  const [fromDate, setFromDate] = useState("")
  const [toDate, setToDate] = useState("")

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setPayments(await parentApi.getPayments())
    } catch (e) {
      setPayments(null)
      setError(e instanceof Error ? e.message : tp("loadFailed"))
    } finally {
      setLoading(false)
    }
  }, [tp])

  useEffect(() => {
    void load()
  }, [load])

  // Children are loaded once. A failure here must not blank the payment history, which is the
  // part of the page that matters most, so it is reported separately.
  useEffect(() => {
    let cancelled = false
    parentApi
      .getChildren()
      .then((rows) => {
        if (cancelled) return
        const list = Array.isArray(rows) ? rows : []
        setChildren(list)
        if (list.length > 0) setSelectedChildId(list[0].studentId)
      })
      .catch(() => {
        if (!cancelled) setChildError(tp("childrenFailed"))
      })
    return () => {
      cancelled = true
    }
  }, [tp])

  const loadChild = useCallback(
    async (studentId: string) => {
      if (!studentId) return
      setChildLoading(true)
      setChildError(null)
      try {
        const [paymentsRes, accessRes] = await Promise.all([
          parentApi.getChildPayments(studentId).catch(() => null),
          // An access-grant failure must not hide the payment history, so it degrades to empty
          // rather than failing the whole view.
          parentApi.getChildEntitlements(studentId).catch(() => null),
        ])
        setChildPayments(Array.isArray(paymentsRes) ? paymentsRes : [])
        setChildAccess(Array.isArray(accessRes) ? accessRes : [])
      } catch (e) {
        setChildError(e instanceof Error ? e.message : tp("childFailed"))
      } finally {
        setChildLoading(false)
      }
    },
    [tp],
  )

  useEffect(() => {
    void loadChild(selectedChildId)
  }, [selectedChildId, loadChild])

  const handleCancel = async (payment: PaymentItem) => {
    if (!window.confirm(tp("cancelConfirm"))) return
    setCancellingId(payment.id)
    setActionError(null)
    try {
      await parentApi.cancelPayment(payment.id)
      await load()
    } catch (e) {
      // The row stays exactly where it was. Guessing at the outcome here would be the one
      // thing this page must never do.
      setActionError(e instanceof Error ? e.message : tp("cancelFailed"))
      await load()
    } finally {
      setCancellingId(null)
    }
  }

  const fmtDate = (value: string | null | undefined) =>
    value ? formatDate(value, locale, { day: "2-digit", month: "short", year: "numeric" }) : null

  /**
   * Date filtering over the rows already fetched.
   *
   * The endpoint returns this account's payments unpaged, so for this page the filtered set IS
   * the full set the server sent - the caveat about pagination applies to the Platform Admin
   * views, not here.
   */
  const inRange = useCallback(
    (value: string | null | undefined) => {
      if (!fromDate && !toDate) return true
      if (!value) return false
      const day = value.slice(0, 10)
      if (fromDate && day < fromDate) return false
      if (toDate && day > toDate) return false
      return true
    },
    [fromDate, toDate],
  )

  const visibleRecent = useMemo(
    () => (payments?.recentPayments ?? []).filter((p) => inRange(p.paidAt ?? p.createdAt)),
    [payments, inRange],
  )
  const visiblePending = useMemo(
    () => (payments?.pendingPayments ?? []).filter((p) => inRange(p.createdAt)),
    [payments, inRange],
  )
  const filterActive = Boolean(fromDate || toDate)

  const exportable = [...visiblePending, ...visibleRecent]

  const handleExport = () => {
    if (exportable.length === 0) return
    downloadCsv(
      exportFilename("elmkusoma-payments"),
      toCsv(exportable, [
        { header: tp("columns.reference"), value: (p) => p.id },
        { header: tp("columns.date"), value: (p) => (p.paidAt ?? p.createdAt).slice(0, 10) },
        { header: tp("columns.description"), value: (p) => p.description || p.serviceType },
        { header: tp("columns.serviceType"), value: (p) => p.serviceType },
        { header: tp("columns.amount"), value: (p) => p.amount },
        { header: tp("columns.currency"), value: (p) => p.currency },
        { header: tp("columns.status"), value: (p) => p.status },
        { header: tp("columns.providerReference"), value: (p) => p.providerReference },
      ]),
    )
  }

  if (loading && !payments) {
    return (
      <div className="flex items-center justify-center py-20" role="status" aria-live="polite">
        <Loader2 className="size-6 animate-spin text-muted-foreground" aria-hidden="true" />
        <span className="sr-only">{tp("loading")}</span>
      </div>
    )
  }

  const recent = visibleRecent
  const pending = visiblePending
  const hasAny = recent.length > 0 || pending.length > 0


  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("payments")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("payments.subtitle")}</p>
      </div>

      {error && (
        <div
          role="alert"
          className="flex flex-col gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 dark:border-red-900 dark:bg-red-950/30 sm:flex-row sm:items-center sm:justify-between"
        >
          <p className="flex items-start gap-2 text-sm text-red-700 dark:text-red-400">
            <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            {error}
          </p>
          <button
            type="button"
            onClick={() => void load()}
            className="inline-flex shrink-0 items-center gap-1.5 rounded-lg border border-red-300 px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-100 dark:border-red-800 dark:text-red-400 dark:hover:bg-red-950/50"
          >
            <RefreshCw className="size-3.5" aria-hidden="true" />
            {tp("retry")}
          </button>
        </div>
      )}

      {actionError && (
        <div
          role="alert"
          className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800 dark:border-amber-900 dark:bg-amber-950/30 dark:text-amber-300"
        >
          {actionError}
        </div>
      )}

      {/* Figures the backend computed. Both are BigDecimal sums of the rows it classified,
          not anything derived in this component. */}
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{tp("awaitingPayment")}</p>
          <p className="mt-1 text-2xl font-extrabold text-amber-600 dark:text-amber-400">
            {formatMoney(payments?.outstanding, "TZS", locale)}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">{tp("awaitingPaymentHint")}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{t("payments.paidThisTerm")}</p>
          <p className="mt-1 text-2xl font-extrabold text-green-600 dark:text-green-400">
            {formatMoney(payments?.paidThisTerm, "TZS", locale)}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">{tp("paidHint")}</p>
        </div>
      </div>

      {checkout && (
        <MobileMoneyCheckout
          payment={checkout}
          onClose={() => setCheckout(null)}
          onSettled={load}
        />
      )}

      <NewPaymentForm onInitiated={load} />

      {!hasAny && !error && !filterActive && (
        <div className="rounded-2xl border border-dashed border-border bg-card p-10 text-center">
          <CreditCard className="mx-auto size-8 text-muted-foreground" aria-hidden="true" />
          <p className="mt-3 text-sm font-medium text-foreground">{tp("emptyTitle")}</p>
          <p className="mx-auto mt-1 max-w-md text-sm text-muted-foreground">{tp("emptyBody")}</p>
        </div>
      )}

      {/* The toolbar stays mounted whenever data has loaded, including at zero matches.
          Gating it on `hasAny` would unmount the very control needed to undo a filter that
          excluded everything, stranding the user on an empty panel with no way back. */}
      {payments !== null && (
        <div className="flex flex-col gap-3 rounded-2xl border border-border bg-card p-4 sm:flex-row sm:flex-wrap sm:items-end sm:justify-between">
          <div className="flex flex-wrap items-end gap-3">
            <label htmlFor="payment-filter-from" className="flex flex-col gap-1 text-xs text-muted-foreground">
              <span>{tp("filterFrom")}</span>
              <input
                id="payment-filter-from"
                type="date"
                value={fromDate}
                max={toDate || undefined}
                onChange={(e) => setFromDate(e.target.value)}
                className="rounded-lg border border-input bg-background px-2.5 py-1.5 text-sm"
              />
            </label>
            <label htmlFor="payment-filter-to" className="flex flex-col gap-1 text-xs text-muted-foreground">
              <span>{tp("filterTo")}</span>
              <input
                id="payment-filter-to"
                type="date"
                value={toDate}
                min={fromDate || undefined}
                onChange={(e) => setToDate(e.target.value)}
                className="rounded-lg border border-input bg-background px-2.5 py-1.5 text-sm"
              />
            </label>
            {filterActive && (
              <button
                type="button"
                onClick={() => {
                  setFromDate("")
                  setToDate("")
                }}
                className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground hover:bg-muted"
              >
                {tp("filterClear")}
              </button>
            )}
          </div>
          <div className="flex items-center gap-3">
            <p className="text-xs text-muted-foreground">{tp("filterScopeNote")}</p>
            <button
              type="button"
              onClick={handleExport}
              disabled={exportable.length === 0}
              className="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground hover:bg-muted disabled:opacity-50"
            >
              <Download className="size-4" aria-hidden="true" />
              {tp("exportAction")}
            </button>
          </div>
        </div>
      )}

      {filterActive && !hasAny && (
        <div className="rounded-2xl border border-dashed border-border bg-card p-8 text-center">
          <p className="text-sm font-medium text-foreground">{tp("noMatchTitle")}</p>
          <p className="mx-auto mt-1 max-w-md text-sm text-muted-foreground">{tp("noMatchBody")}</p>
        </div>
      )}

      {pending.length > 0 && (
        <section className="rounded-2xl border border-amber-200 bg-amber-50 p-5 shadow-xs dark:border-amber-900 dark:bg-amber-950/30">
          <h2 className="text-base font-semibold text-foreground">{t("payments.pendingTitle")}</h2>
          <p className="mt-1 text-xs text-muted-foreground">{tp("pendingHint")}</p>
          <ul className="mt-3 space-y-3">
            {pending.map((payment) => (
              <PaymentRow
                key={payment.id}
                payment={payment}
                locale={locale}
                formattedDate={fmtDate(payment.createdAt)}
                action={
                  <>
                    {payment.status === "PENDING" ? (
                      <button
                        type="button"
                        onClick={() => setCheckout(payment)}
                        className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-card px-3 py-1.5 text-xs font-semibold text-foreground hover:bg-muted"
                      >
                        <Smartphone className="size-3.5" aria-hidden="true" />
                        {t("payments.payNow")}
                      </button>
                    ) : null}
                    {isCancellable(payment.status) ? (
                      <button
                        type="button"
                        onClick={() => void handleCancel(payment)}
                        disabled={cancellingId === payment.id}
                        className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-card px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted disabled:opacity-60"
                      >
                        {cancellingId === payment.id ? (
                          <Loader2 className="size-3.5 animate-spin" aria-hidden="true" />
                        ) : null}
                        {tp("cancelAction")}
                      </button>
                    ) : null}
                  </>
                }
              />
            ))}
          </ul>
        </section>
      )}

      {recent.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-base font-semibold text-foreground">{tp("historyTitle")}</h2>
          <ul className="space-y-3">
            {recent.map((payment) => (
              <PaymentRow
                key={payment.id}
                payment={payment}
                locale={locale}
                formattedDate={fmtDate(payment.paidAt ?? payment.createdAt)}
                action={null}
              />
            ))}
          </ul>
        </section>
      )}

      {children.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5">
          <h2 className="flex items-center gap-2 text-base font-semibold text-foreground">
            <ShieldCheck className="size-4 text-muted-foreground" aria-hidden="true" />
            {tp("accessTitle")}
          </h2>
          <p className="mt-1 text-xs text-muted-foreground">{tp("accessSubtitle")}</p>

          <label htmlFor="payment-child-select" className="mt-4 flex flex-col gap-1 text-xs text-muted-foreground">
            <span>{tp("selectChild")}</span>
            <select
              id="payment-child-select"
              value={selectedChildId}
              onChange={(e) => setSelectedChildId(e.target.value)}
              className="rounded-lg border border-input bg-background px-2.5 py-1.5 text-sm"
            >
              {children.map((child) => (
                <option key={child.studentId} value={child.studentId}>
                  {child.studentName}
                  {child.className ? ` (${child.className})` : ""}
                </option>
              ))}
            </select>
          </label>

          {childError && (
            <p role="alert" className="mt-3 text-sm text-amber-700 dark:text-amber-400">
              {childError}
            </p>
          )}

          {childLoading ? (
            <div className="mt-4 flex items-center gap-2 text-sm text-muted-foreground" role="status">
              <Loader2 className="size-4 animate-spin" aria-hidden="true" />
              {tp("accessLoading")}
            </div>
          ) : (
            <>
              <div className="mt-4">
                <h3 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  {tp("accessGrantsHeading")}
                </h3>
                {(childAccess ?? []).length === 0 ? (
                  <p className="mt-2 text-sm text-muted-foreground">{tp("accessEmpty")}</p>
                ) : (
                  <ul className="mt-2 space-y-2">
                    {(childAccess ?? []).map((grant) => (
                      <AccessGrantRow key={grant.id} grant={grant} fmtDate={fmtDate} />
                    ))}
                  </ul>
                )}
              </div>

              <div className="mt-5">
                <h3 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  {tp("childPaymentsHeading")}
                </h3>
                {(childPayments ?? []).length === 0 ? (
                  <p className="mt-2 text-sm text-muted-foreground">{tp("childPaymentsEmpty")}</p>
                ) : (
                  <ul className="mt-2 space-y-3">
                    {(childPayments ?? []).map((payment) => (
                      <PaymentRow
                        key={payment.id}
                        payment={payment}
                        locale={locale}
                        formattedDate={fmtDate(payment.paidAt ?? payment.createdAt)}
                        action={null}
                        showCancel={false}
                      />
                    ))}
                  </ul>
                )}
              </div>
            </>
          )}
        </section>
      )}
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
  locale,
  formattedDate,
  action,
  showCancel = true,
}: {
  payment: PaymentItem
  locale: string
  formattedDate: string | null
  action: React.ReactNode
  showCancel?: boolean
}) {
  const tp = useTranslations("parent.payments")
  const tpa = useTranslations("parent.payments.access")
  const status = presentPaymentStatus(payment.status)

  return (
    <li className="rounded-xl border border-border bg-card p-4 shadow-xs">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <p className="truncate text-sm font-medium text-foreground">
              {payment.description || payment.serviceType || tp("noDescription")}
            </p>
            <span
              className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${statusToneClasses(status.tone)}`}
            >
              {payment.status || tp("statusUnknown")}
            </span>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            {payment.serviceType}
            {formattedDate ? ` · ${formattedDate}` : ""}
          </p>
          {payment.providerReference && (
            <p className="mt-1 break-all text-xs text-muted-foreground">
              {tp("reference")}: <span className="font-mono">{payment.providerReference}</span>
            </p>
          )}
        </div>
        <div className="flex items-center gap-3">
          <p className="text-sm font-semibold tabular-nums text-foreground">
            {formatMoney(payment.amount, payment.currency, locale)}
          </p>
          {showCancel ? action : null}
        </div>
      </div>
    </li>
  )
}

/**
 * One access grant.
 *
 * The expiry wording is the point. "No end date" and "ended 3 days ago" are different facts, and
 * a null `expiresAt` must never be rendered as a countdown - a grant with no expiry has not run
 * out, and saying it has would tell a parent to pay for something they already hold.
 */
function AccessGrantRow({
  grant,
  fmtDate,
}: {
  grant: EntitlementItem
  fmtDate: (value: string | null | undefined) => string | null
}) {
  const tpa = useTranslations("parent.payments.access")
  const { expiring, expired, daysRemaining } = describeExpiry(grant.expiresAt)
  const tone = statusToneClasses(presentPaymentStatus(grant.status).tone)

  let expiryText: string
  if (!expiring) {
    expiryText = tpa("noExpiry")
  } else if (expired) {
    expiryText = tpa("expiredOn", { date: fmtDate(grant.expiresAt) ?? "" })
  } else if (daysRemaining !== null && daysRemaining <= 7) {
    expiryText = tpa("expiresSoonDays", { days: daysRemaining })
  } else {
    expiryText = tpa("expiresOn", { date: fmtDate(grant.expiresAt) ?? "" })
  }

  return (
    <li className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-border bg-background p-3">
      <div>
        <p className="text-sm font-medium text-foreground">{grant.serviceType}</p>
        <p className="text-xs text-muted-foreground">
          {tpa("starts")}: {fmtDate(grant.startsAt) ?? "—"}
        </p>
      </div>
      <div className="text-right">
        <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${tone}`}>
          {grant.status || tpa("statusUnknown")}
        </span>
        <p
          className={`mt-1 text-xs ${
            expired ? "text-red-600 dark:text-red-400" : "text-muted-foreground"
          }`}
        >
          {expiryText}
        </p>
      </div>
    </li>
  )
}
