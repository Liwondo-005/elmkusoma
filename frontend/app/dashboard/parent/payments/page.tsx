"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { useTranslations } from "next-intl"
import { Loader2, AlertCircle, CreditCard, RefreshCw, Download, ShieldCheck } from "lucide-react"
import {
  parentApi,
  type PaymentItem,
  type ParentPayments,
  type ChildOverview,
  type EntitlementItem,
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
 * There is no "pay" button here on purpose. POST /v1/my/payments/initiate takes the amount from
 * the caller, so presenting it as a checkout would mean letting the browser state the price.
 * That endpoint is raised to Developer 01 as a contract defect rather than surfaced.
 */
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
                  isCancellable(payment.status) ? (
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
                  ) : null
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
