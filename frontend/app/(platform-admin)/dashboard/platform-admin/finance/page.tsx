"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { useTranslations } from "next-intl"
import { Loader2, AlertCircle, RefreshCw, ShieldAlert, CircleSlash, Search, Download } from "lucide-react"
import {
  platformAdminApi,
  type PaymentSummary,
  type EntitlementSummary,
  type ServiceSummary,
  type WebhookEvent,
  type PageResponse,
  type FinancialTransaction,
} from "@/lib/platform-admin-api"
import { useLocale } from "@/hooks/use-locale"
import { formatDate } from "@/lib/format-date"
import { formatMoney } from "@/lib/money"
import { presentPaymentStatus, statusToneClasses } from "@/lib/payment-status"
import { toCsv, downloadCsv, exportFilename } from "@/lib/csv"

/**
 * Platform Admin financial command center.
 *
 * Every figure on this page comes from a real, already-authorised admin endpoint:
 *
 *   GET /v1/platform-admin/payments              paged, status filter
 *   GET /v1/platform-admin/financial/transactions  paged, status/service/date/reference filter
 *   GET /v1/platform-admin/entitlements          paged, status filter
 *   GET /v1/platform-admin/webhook-events        source=PAYMENT, failedOnly
 *   GET /v1/platform-admin/services              the platform service catalogue
 *
 * The transactions tab is the one place an operator can look up an individual payment - by
 * receipt number, by gateway reference, or by id - and take the same filtered set away as CSV.
 * Screen and export run off one backend filter set, so a report and its file cannot disagree.
 *
 * What this page still deliberately does NOT show, and why:
 *
 *  - Earnings, provider share and profit. The backend stores a commission amount per payment, but
 *    has no provider/teacher/host earnings endpoint, no settlement or fee allocation, and no
 *    approved accounting methodology. Those tiles stay an explicit "not implemented" state rather
 *    than as zero, because a zero here would be a fabricated financial statement.
 *
 *  - A summed total over the current page. The endpoints are paged, so summing the twenty rows
 *    on screen and labelling the result a total would understate a larger ledger by a factor of
 *    twenty. The counts shown are the endpoints' own `totalElements`, which is a real figure.
 *
 * A commission is rendered as "no approved rate" when the backend returns null. The backend only
 * leaves it null when no approved rate existed at confirmation time, which is not the same claim
 * as "the commission was zero"; the two are kept apart everywhere in this file.
 *
 * The reconciliation panel lists failed payment webhooks. That is genuinely what the backend can
 * detect today: an external event that arrived and did not process cleanly. It is not a full
 * settlement reconciliation and is labelled as such.
 */

type Tab = "payments" | "transactions" | "entitlements" | "exceptions" | "catalogue"

const PAGE_SIZE = 20
const PAYMENT_STATUSES = ["", "PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED", "REFUNDED"]
const ENTITLEMENT_STATUSES = ["", "ACTIVE", "REVOKED"]

export default function FinancePage() {
  const tf = useTranslations("platformAdmin.finance")
  const locale = useLocale()

  const [tab, setTab] = useState<Tab>("payments")
  const [payments, setPayments] = useState<PageResponse<PaymentSummary> | null>(null)
  const [entitlements, setEntitlements] = useState<PageResponse<EntitlementSummary> | null>(null)
  const [exceptions, setExceptions] = useState<PageResponse<WebhookEvent> | null>(null)
  const [services, setServices] = useState<PageResponse<ServiceSummary> | null>(null)

  const [paymentStatus, setPaymentStatus] = useState("")
  const [entitlementStatus, setEntitlementStatus] = useState("")
  const [paymentPage, setPaymentPage] = useState(0)
  const [entitlementPage, setEntitlementPage] = useState(0)

  /**
   * Reference / service search, applied to the rows on screen.
   *
   * The endpoint paginates and has no server-side search parameter, so this narrows the current
   * page only. The control says so, and the empty state repeats it, because a filter that quietly
   * only searched 20 of 5,000 rows would be a misleading tool on a financial screen.
   */
  const [search, setSearch] = useState("")
  const [fromDate, setFromDate] = useState("")
  const [toDate, setToDate] = useState("")

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  const fmtDate = useCallback(
    (value: string | null | undefined) =>
      value ? formatDate(value, locale, { day: "2-digit", month: "short", year: "numeric" }) : "—",
    [locale],
  )

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    // All four are independent reads, so they are started together rather than in sequence.
    Promise.all([
      platformAdminApi.listPayments(paymentPage, PAGE_SIZE, paymentStatus || undefined),
      platformAdminApi.listEntitlements(entitlementPage, PAGE_SIZE, entitlementStatus || undefined),
      platformAdminApi.listWebhookEvents("PAYMENT", true, 0, PAGE_SIZE),
      platformAdminApi.listServices(0, 50),
    ])
      .then(([p, e, x, s]) => {
        if (cancelled) return
        setPayments(p)
        setEntitlements(e)
        setExceptions(x)
        setServices(s)
      })
      .catch((err: unknown) => {
        if (cancelled) return
        // One failed panel blanks the page rather than rendering partial financial figures
        // alongside empty panels, which would read as "no data" instead of "could not load".
        setError(err instanceof Error ? err.message : tf("loadFailed"))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [paymentPage, paymentStatus, entitlementPage, entitlementStatus, reloadKey, tf])

  const hasPayments = (payments?.totalElements ?? 0) > 0
  const hasExceptions = (exceptions?.totalElements ?? 0) > 0

  const matchesFilters = (row: { id: string; serviceType: string; createdAt: string }) => {
    if (search.trim()) {
      const needle = search.trim().toLowerCase()
      const haystack = `${row.id} ${row.serviceType}`.toLowerCase()
      if (!haystack.includes(needle)) return false
    }
    const day = row.createdAt.slice(0, 10)
    if (fromDate && day < fromDate) return false
    if (toDate && day > toDate) return false
    return true
  }

  const visiblePayments = useMemo(
    () => (payments?.content ?? []).filter(matchesFilters),
    [payments, search, fromDate, toDate],
  )
  const filterActive = Boolean(search.trim() || fromDate || toDate)

  const handleExport = () => {
    if (visiblePayments.length === 0) return
    downloadCsv(
      exportFilename("elmkusoma-payment-ledger-page"),
      toCsv(visiblePayments, [
        { header: tf("columns.reference"), value: (p) => p.id },
        { header: tf("columns.service"), value: (p) => p.serviceType },
        { header: tf("columns.status"), value: (p) => p.status },
        { header: tf("columns.amount"), value: (p) => p.amount },
        { header: tf("columns.currency"), value: (p) => p.currency },
        { header: tf("columns.created"), value: (p) => p.createdAt.slice(0, 10) },
      ]),
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{tf("title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{tf("subtitle")}</p>
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
            onClick={() => setReloadKey((k) => k + 1)}
            className="inline-flex shrink-0 items-center gap-1.5 rounded-lg border border-red-300 px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-100 dark:border-red-800 dark:text-red-400 dark:hover:bg-red-950/50"
          >
            <RefreshCw className="size-3.5" aria-hidden="true" />
            {tf("retry")}
          </button>
        </div>
      )}

      {/* Record counts, taken from each endpoint's own totalElements. */}
      <div className="grid gap-4 sm:grid-cols-3">
        <CountTile
          label={tf("counts.payments")}
          value={payments?.totalElements ?? null}
          tone={hasPayments ? "neutral" : "empty"}
          locale={locale}
        />
        <CountTile
          label={tf("counts.entitlements")}
          value={entitlements?.totalElements ?? null}
          tone={(entitlements?.totalElements ?? 0) > 0 ? "neutral" : "empty"}
          locale={locale}
        />
        <CountTile
          label={tf("counts.exceptions")}
          value={exceptions?.totalElements ?? null}
          tone={hasExceptions ? "problem" : "empty"}
          locale={locale}
        />
      </div>

      {/* Financial metrics the backend cannot currently produce. Shown as absent, not as zero. */}
      <section className="rounded-2xl border border-dashed border-border bg-card p-5">
        <h2 className="flex items-center gap-2 text-sm font-semibold text-foreground">
          <CircleSlash className="size-4 text-muted-foreground" aria-hidden="true" />
          {tf("unavailable.title")}
        </h2>
        <p className="mt-1 text-xs text-muted-foreground">{tf("unavailable.body")}</p>
        <ul className="mt-3 flex flex-wrap gap-2">
          {[
            tf("unavailable.items.commission"),
            tf("unavailable.items.providerShare"),
            tf("unavailable.items.earnings"),
            tf("unavailable.items.netCollections"),
            tf("unavailable.items.profit"),
            tf("unavailable.items.discounts"),
            tf("unavailable.items.subscriptions"),
          ].map((item) => (
            <li
              key={item}
              className="rounded-full bg-muted px-3 py-1 text-xs text-muted-foreground"
            >
              {item}
            </li>
          ))}
        </ul>
      </section>

      {/* Tabs */}
      <div className="flex flex-wrap gap-1 border-b border-border" role="tablist">
        {(
          [
["payments", tf("tabs.payments")],
    ["transactions", tf("tabs.transactions")],
    ["entitlements", tf("tabs.entitlements")],
            ["exceptions", tf("tabs.exceptions")],
            ["catalogue", tf("tabs.catalogue")],
          ] as Array<[Tab, string]>
        ).map(([key, label]) => (
          <button
            key={key}
            type="button"
            role="tab"
            aria-selected={tab === key}
            onClick={() => setTab(key)}
            className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium transition-colors ${
              tab === key
                ? "border-primary text-primary"
                : "border-transparent text-muted-foreground hover:text-foreground"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {loading && (
        <div className="flex items-center justify-center py-12" role="status" aria-live="polite">
          <Loader2 className="size-6 animate-spin text-muted-foreground" aria-hidden="true" />
          <span className="sr-only">{tf("loading")}</span>
        </div>
      )}

      {!loading && !error && tab === "payments" && (
        <PaymentsPanel
          data={visiblePayments}
          totalPages={payments?.totalPages ?? 0}
          page={payments?.page ?? 0}
          totalElements={payments?.totalElements ?? 0}
          status={paymentStatus}
          onStatus={setPaymentStatus}
          onPage={setPaymentPage}
          locale={locale}
          fmtDate={fmtDate}
          search={search}
          onSearch={setSearch}
          fromDate={fromDate}
          onFromDate={setFromDate}
          toDate={toDate}
          onToDate={setToDate}
          filterActive={filterActive}
          onExport={handleExport}
        />
      )}

      {!loading && !error && tab === "transactions" && (
        <TransactionsPanel
          locale={locale}
          fmtDate={fmtDate}
          search={search}
          onSearch={setSearch}
        />
      )}

      {!loading && !error && tab === "entitlements" && (
        <EntitlementsPanel
          data={entitlements}
          status={entitlementStatus}
          onStatus={setEntitlementStatus}
          onPage={setEntitlementPage}
          fmtDate={fmtDate}
        />
      )}

      {!loading && !error && tab === "exceptions" && <ExceptionsPanel data={exceptions} fmtDate={fmtDate} />}

      {!loading && !error && tab === "catalogue" && (
        <CataloguePanel data={services} locale={locale} />
      )}
    </div>
  )
}

/* ── panels ──────────────────────────────────────────────────────────────── */

function PaymentsPanel({
  data,
  totalPages,
  page,
  totalElements,
  status,
  onStatus,
  onPage,
  locale,
  fmtDate,
  search,
  onSearch,
  fromDate,
  onFromDate,
  toDate,
  onToDate,
  filterActive,
  onExport,
}: {
  data: PaymentSummary[]
  totalPages: number
  page: number
  totalElements: number
  status: string
  onStatus: (value: string) => void
  onPage: (value: number) => void
  locale: string
  fmtDate: (value: string | null | undefined) => string
  search: string
  onSearch: (value: string) => void
  fromDate: string
  onFromDate: (value: string) => void
  toDate: string
  onToDate: (value: string) => void
  filterActive: boolean
  onExport: () => void
}) {
  const tf = useTranslations("platformAdmin.finance")
  const rows = data

  return (
    <section className="space-y-3">
      <div className="flex flex-col gap-3 rounded-2xl border border-border bg-card p-4">
        <div className="flex flex-wrap items-end gap-3">
          <label
            htmlFor="finance-search"
            className="flex min-w-[12rem] flex-1 flex-col gap-1 text-xs text-muted-foreground"
          >
            <span>{tf("filters.search")}</span>
            <span className="relative">
              <Search
                className="pointer-events-none absolute left-2.5 top-2.5 size-4 text-muted-foreground"
                aria-hidden="true"
              />
              <input
                id="finance-search"
                type="search"
                value={search}
                onChange={(e) => onSearch(e.target.value)}
                placeholder={tf("filters.searchPlaceholder")}
                className="w-full rounded-lg border border-input bg-background py-1.5 pl-8 pr-2.5 text-sm"
              />
            </span>
          </label>
          <label
            htmlFor="finance-status"
            className="flex flex-col gap-1 text-xs text-muted-foreground"
          >
            <span>{tf("filters.status")}</span>
            <select
              id="finance-status"
              value={status}
              onChange={(e) => {
                onStatus(e.target.value)
                onPage(0)
              }}
              className="rounded-lg border border-input bg-background px-2.5 py-1.5 text-sm"
            >
              {PAYMENT_STATUSES.map((value) => (
                <option key={value || "all"} value={value}>
                  {value || tf("filters.allStatuses")}
                </option>
              ))}
            </select>
          </label>
          <label
            htmlFor="finance-from"
            className="flex flex-col gap-1 text-xs text-muted-foreground"
          >
            <span>{tf("filters.from")}</span>
            <input
              id="finance-from"
              type="date"
              value={fromDate}
              max={toDate || undefined}
              onChange={(e) => onFromDate(e.target.value)}
              className="rounded-lg border border-input bg-background px-2.5 py-1.5 text-sm"
            />
          </label>
          <label
            htmlFor="finance-to"
            className="flex flex-col gap-1 text-xs text-muted-foreground"
          >
            <span>{tf("filters.to")}</span>
            <input
              id="finance-to"
              type="date"
              value={toDate}
              min={fromDate || undefined}
              onChange={(e) => onToDate(e.target.value)}
              className="rounded-lg border border-input bg-background px-2.5 py-1.5 text-sm"
            />
          </label>
          {filterActive && (
            <button
              type="button"
              onClick={() => {
                onSearch("")
                onFromDate("")
                onToDate("")
              }}
              className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground hover:bg-muted"
            >
              {tf("filters.clear")}
            </button>
          )}
        </div>
        <div className="flex flex-wrap items-center justify-between gap-2 border-t border-border pt-3">
          <p className="text-xs text-muted-foreground">{tf("filters.pageScope")}</p>
          <button
            type="button"
            onClick={onExport}
            disabled={rows.length === 0}
            className="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground hover:bg-muted disabled:opacity-50"
          >
            <Download className="size-4" aria-hidden="true" />
            {tf("filters.exportAction")}
          </button>
        </div>
      </div>

      {rows.length === 0 ? (
        filterActive ? (
          <EmptyPanel message={tf("empty.paymentsNoMatch")} />
        ) : (
          <EmptyPanel message={tf("empty.payments")} />
        )
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-border">
          <table className="w-full text-sm">
            <thead className="bg-muted/50 text-left text-xs uppercase tracking-wide text-muted-foreground">
              <tr>
                <th scope="col" className="px-4 py-3 font-medium">{tf("columns.reference")}</th>
                <th scope="col" className="px-4 py-3 font-medium">{tf("columns.service")}</th>
                <th scope="col" className="px-4 py-3 font-medium">{tf("columns.status")}</th>
                <th scope="col" className="px-4 py-3 text-right font-medium">{tf("columns.amount")}</th>
                <th scope="col" className="px-4 py-3 font-medium">{tf("columns.created")}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {rows.map((row) => {
                const presentation = presentPaymentStatus(row.status)
                return (
                  <tr key={row.id}>
                    <td className="px-4 py-3 font-mono text-xs text-muted-foreground">{row.id}</td>
                    <td className="px-4 py-3">{row.serviceType}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${statusToneClasses(presentation.tone)}`}
                      >
                        {row.status || "—"}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums">
                      {formatMoney(row.amount, row.currency, locale)}
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">{fmtDate(row.createdAt)}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      <Pager page={page} totalPages={totalPages} totalElements={totalElements} onPage={onPage} />
    </section>
  )
}

function EntitlementsPanel({
  data,
  status,
  onStatus,
  onPage,
  fmtDate,
}: {
  data: PageResponse<EntitlementSummary> | null
  status: string
  onStatus: (value: string) => void
  onPage: (value: number) => void
  fmtDate: (value: string | null | undefined) => string
}) {
  const tf = useTranslations("platformAdmin.finance")
  const rows = data?.content ?? []

  return (
    <section className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <label
          htmlFor="entitlement-status"
          className="flex items-center gap-2 text-sm"
        >
          <span className="text-muted-foreground">{tf("filters.status")}</span>
          <select
            id="entitlement-status"
            value={status}
            onChange={(e) => {
              onStatus(e.target.value)
              onPage(0)
            }}
            className="rounded-lg border border-input bg-background px-2.5 py-1.5 text-sm"
          >
            {ENTITLEMENT_STATUSES.map((value) => (
              <option key={value || "all"} value={value}>
                {value || tf("filters.allStatuses")}
              </option>
            ))}
          </select>
        </label>
        <Pager
        page={data?.page ?? 0}
        totalPages={data?.totalPages ?? 0}
        totalElements={data?.totalElements ?? 0}
        onPage={onPage}
      />
      </div>

      {rows.length === 0 ? (
        <EmptyPanel message={tf("empty.entitlements")} />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-border">
          <table className="w-full text-sm">
            <thead className="bg-muted/50 text-left text-xs uppercase tracking-wide text-muted-foreground">
              <tr>
                <th scope="col" className="px-4 py-3 font-medium">{tf("columns.user")}</th>
                <th scope="col" className="px-4 py-3 font-medium">{tf("columns.service")}</th>
                <th scope="col" className="px-4 py-3 font-medium">{tf("columns.status")}</th>
                <th scope="col" className="px-4 py-3 font-medium">{tf("columns.starts")}</th>
                <th scope="col" className="px-4 py-3 font-medium">{tf("columns.expires")}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {rows.map((row) => (
                <tr key={row.id}>
                  <td className="px-4 py-3 font-mono text-xs text-muted-foreground">{row.userId}</td>
                  <td className="px-4 py-3">
                    {row.serviceType}
                    <span className="ml-1 text-xs text-muted-foreground">{row.serviceId}</span>
                  </td>
                  <td className="px-4 py-3">{row.status}</td>
                  <td className="px-4 py-3 text-muted-foreground">{fmtDate(row.startsAt)}</td>
                  <td className="px-4 py-3 text-muted-foreground">{fmtDate(row.expiresAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function ExceptionsPanel({
  data,
  fmtDate,
}: {
  data: PageResponse<WebhookEvent> | null
  fmtDate: (value: string | null | undefined) => string
}) {
  const tf = useTranslations("platformAdmin.finance")
  const rows = data?.content ?? []

  return (
    <section className="space-y-3">
      <p className="text-xs text-muted-foreground">{tf("exceptions.scope")}</p>
      {rows.length === 0 ? (
        <EmptyPanel message={tf("empty.exceptions")} />
      ) : (
        <ul className="space-y-3">
          {rows.map((row) => (
            <li key={row.id} className="rounded-2xl border border-border bg-card p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="text-sm font-medium text-foreground">
                  {row.eventType || row.source}
                </p>
                <span className="inline-block rounded-full bg-red-500/10 px-2.5 py-0.5 text-xs font-medium text-red-700 dark:text-red-400">
                  {row.processingResult || row.verificationStatus}
                </span>
              </div>
              <p className="mt-1 text-xs text-muted-foreground">
                {tf("exceptions.received")}: {fmtDate(row.receivedAt)}
                {` · ${tf("exceptions.retries")}: ${row.retryCount}`}
              </p>
              {row.errorDetails && (
                <p className="mt-2 break-words rounded-lg bg-muted p-2 text-xs text-muted-foreground">
                  {row.errorDetails}
                </p>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

/**
 * The platform service catalogue - the only genuinely priced records in the system today.
 *
 * Prices here are whatever Platform Admin has stored on the service. Nothing is defaulted or
 * filled in: a service with no price reads as unpriced rather than as free, because free is a
 * pricing decision and absence is not.
 */
function CataloguePanel({
  data,
  locale,
}: {
  data: PageResponse<ServiceSummary> | null
  locale: string
}) {
  const tf = useTranslations("platformAdmin.finance")
  const rows = data?.content ?? []

  if (rows.length === 0) {
    return <EmptyPanel message={tf("empty.catalogue")} />
  }

  return (
    <div className="overflow-x-auto rounded-2xl border border-border">
      <table className="w-full text-sm">
        <thead className="bg-muted/50 text-left text-xs uppercase tracking-wide text-muted-foreground">
          <tr>
            <th scope="col" className="px-4 py-3 font-medium">{tf("columns.service")}</th>
            <th scope="col" className="px-4 py-3 font-medium">{tf("columns.category")}</th>
            <th scope="col" className="px-4 py-3 font-medium">{tf("columns.status")}</th>
            <th scope="col" className="px-4 py-3 font-medium">{tf("columns.seats")}</th>
            <th scope="col" className="px-4 py-3 text-right font-medium">{tf("columns.price")}</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {rows.map((row) => (
            <tr key={row.id}>
              <td className="px-4 py-3">
                <span className="font-medium text-foreground">{row.name}</span>
                <span className="ml-2 font-mono text-xs text-muted-foreground">{row.code}</span>
              </td>
              <td className="px-4 py-3 text-muted-foreground">{row.category}</td>
              <td className="px-4 py-3">
                <span
                  className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${
                    row.isActive
                      ? "bg-green-500/10 text-green-700 dark:text-green-400"
                      : "bg-muted text-muted-foreground"
                  }`}
                >
                  {row.isActive ? tf("catalogue.active") : tf("catalogue.inactive")}
                </span>
              </td>
              <td className="px-4 py-3 text-muted-foreground">
                {row.maxSeats ?? tf("catalogue.unlimited")}
              </td>
              <td className="px-4 py-3 text-right tabular-nums">
                {row.monthlyPrice === null || row.monthlyPrice === undefined ? (
                  <span className="text-muted-foreground">{tf("catalogue.unpriced")}</span>
                ) : (
                  formatMoney(row.monthlyPrice, row.currency, locale)
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

/* ── shared bits ─────────────────────────────────────────────────────────── */

function CountTile({
  label,
  value,
  tone,
  locale,
}: {
  label: string
  value: number | null
  tone: "neutral" | "empty" | "problem"
  locale: string
}) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <p className="text-xs font-medium text-muted-foreground">{label}</p>
      {value === null ? (
        <p className="mt-1 text-sm text-muted-foreground">—</p>
      ) : (
        <p
          className={`mt-1 text-2xl font-extrabold tabular-nums ${
            tone === "problem"
              ? "text-red-600 dark:text-red-400"
              : tone === "empty"
                ? "text-muted-foreground"
                : "text-foreground"
          }`}
        >
          {value.toLocaleString(locale ?? undefined)}
        </p>
      )}
    </div>
  )
}

function EmptyPanel({ message }: { message: string }) {
  return (
    <div className="rounded-2xl border border-dashed border-border bg-card p-10 text-center">
      <p className="text-sm text-muted-foreground">{message}</p>
    </div>
  )
}

function Pager({
  page,
  totalPages,
  totalElements,
  onPage,
}: {
  page: number
  totalPages: number
  totalElements: number
  onPage: (value: number) => void
}) {
  const tf = useTranslations("platformAdmin.finance")
  const locale = useLocale()
  if (totalPages <= 1) return null

  return (
    <div className="flex items-center gap-2 text-sm">
      <button
        type="button"
        onClick={() => onPage(Math.max(0, page - 1))}
        disabled={page === 0}
        className="rounded-lg border border-border px-3 py-1.5 font-medium text-foreground hover:bg-muted disabled:opacity-40"
      >
        {tf("pager.previous")}
      </button>
      <span className="tabular-nums text-muted-foreground">
        {tf("pager.page", { current: page + 1, total: totalPages })}
      </span>
      <button
        type="button"
        onClick={() => onPage(Math.min(totalPages - 1, page + 1))}
        disabled={page >= totalPages - 1}
        className="rounded-lg border border-border px-3 py-1.5 font-medium text-foreground hover:bg-muted disabled:opacity-40"
      >
        {tf("pager.next")}
      </button>
      <span className="text-xs text-muted-foreground">
        {tf("pager.total", { count: totalElements.toLocaleString(locale ?? undefined) })}
      </span>
    </div>
  )
}

/**
 * Transaction search, from the financial command centre endpoint.
 *
 * <p>Fetches from the backend on every settled filter change rather than filtering the rows
 * already on screen. The payments tab can filter locally because it is showing one paged slice
 * it already holds; here the operator is searching by receipt number or gateway reference, which
 * is a question about the whole ledger, and answering it from twenty cached rows would return a
 * confident "not found" for a payment that exists on page nine.</p>
 *
 * <p>A null commission renders as an explicit "no approved rate" rather than as 0. The backend
 * nulls that field when no approved rate existed at confirmation, which is a different statement
 * from "the commission was zero", and a report that renders the two identically misstates
 * revenue.</p>
 */
function TransactionsPanel({
  locale,
  fmtDate,
  search,
  onSearch,
}: {
  locale: string | undefined
  fmtDate: (value: string | null | undefined) => string
  search: string
  onSearch: (value: string) => void
}) {
  const tf = useTranslations("platformAdmin.finance")

  const [status, setStatus] = useState("")
  const [page, setPage] = useState(0)
  const [data, setData] = useState<PageResponse<FinancialTransaction> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // The free-text box is debounced so typing a receipt number does not fire a request per
  // keystroke, while a filter or page change applies immediately.
  const [debouncedSearch, setDebouncedSearch] = useState(search)
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search), 350)
    return () => clearTimeout(timer)
  }, [search])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    platformAdminApi
      .searchFinancialTransactions({
        query: debouncedSearch || undefined,
        status: status || undefined,
        page,
        size: PAGE_SIZE,
      })
      .then((result) => {
        if (!cancelled) setData(result)
      })
      .catch(() => {
        if (!cancelled) {
          setError(tf("transactions.loadFailed"))
          setData(null)
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [debouncedSearch, status, page, tf])

  const filters = { query: debouncedSearch || undefined, status: status || undefined }
  const exportUrl = platformAdminApi.financialTransactionsExportUrl(filters)

  return (
    <section className="space-y-3">
      <div className="flex flex-col gap-3 rounded-2xl border border-border bg-card p-4">
        <div className="flex flex-wrap items-end gap-3">
          <label
            htmlFor="transactions-search"
            className="flex min-w-[14rem] flex-1 flex-col gap-1 text-xs text-muted-foreground"
          >
            <span>{tf("transactions.reference")}</span>
            <span className="relative">
              <Search
                className="pointer-events-none absolute left-2.5 top-2.5 size-4 text-muted-foreground"
                aria-hidden="true"
              />
              <input
                id="transactions-search"
                type="search"
                value={search}
                onChange={(e) => {
                  onSearch(e.target.value)
                  setPage(0)
                }}
                placeholder={tf("transactions.searchPlaceholder")}
                className="w-full rounded-lg border border-input bg-background py-1.5 pl-8 pr-2.5 text-sm"
              />
            </span>
          </label>

          <label
            htmlFor="transactions-status"
            className="flex flex-col gap-1 text-xs text-muted-foreground"
          >
            <span>{tf("filters.status")}</span>
            <select
              id="transactions-status"
              value={status}
              onChange={(e) => {
                setStatus(e.target.value)
                setPage(0)
              }}
              className="rounded-lg border border-input bg-background px-2.5 py-1.5 text-sm"
            >
              {["", "PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED", "PARTIALLY_REFUNDED", "REFUNDED"].map(
                (value) => (
                  <option key={value} value={value}>
                    {value === "" ? tf("filters.allStatuses") : value}
                  </option>
                ),
              )}
            </select>
          </label>

          {/* A plain link, not a fetch: the browser should download the file itself. */}
          <a
            href={exportUrl}
            className="inline-flex items-center gap-2 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground hover:bg-muted"
          >
            <Download className="size-4" aria-hidden="true" />
            {tf("transactions.export")}
          </a>
        </div>
      </div>

      {loading && (
        <div className="flex items-center justify-center py-12" role="status" aria-live="polite">
          <Loader2 className="size-6 animate-spin text-muted-foreground" aria-hidden="true" />
          <span className="sr-only">{tf("loading")}</span>
        </div>
      )}

      {!loading && error && (
        <div
          className="flex items-center gap-2 rounded-2xl border border-destructive/40 bg-destructive/5 p-4 text-sm text-destructive"
          role="alert"
        >
          <AlertCircle className="size-4" aria-hidden="true" />
          {error}
        </div>
      )}

      {!loading && !error && data && data.content.length === 0 && (
        <p className="rounded-2xl border border-border bg-card p-8 text-center text-sm text-muted-foreground">
          {tf("transactions.empty")}
        </p>
      )}

      {!loading && !error && data && data.content.length > 0 && (
        <div className="overflow-x-auto rounded-2xl border border-border bg-card">
          <table className="w-full text-left text-sm">
            <caption className="sr-only">{tf("transactions.caption")}</caption>
            <thead className="border-b border-border text-xs uppercase text-muted-foreground">
              <tr>
                <th scope="col" className="px-4 py-3">{tf("transactions.receipt")}</th>
                <th scope="col" className="px-4 py-3">{tf("transactions.service")}</th>
                <th scope="col" className="px-4 py-3">{tf("transactions.status")}</th>
                <th scope="col" className="px-4 py-3 text-right">{tf("transactions.amount")}</th>
                <th scope="col" className="px-4 py-3 text-right">{tf("transactions.refunded")}</th>
                <th scope="col" className="px-4 py-3 text-right">{tf("transactions.commission")}</th>
                <th scope="col" className="px-4 py-3">{tf("transactions.paidAt")}</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((row) => (
                <tr key={row.paymentId} className="border-b border-border/60 last:border-0">
                  <td className="px-4 py-3 font-mono text-xs">
                    {row.receiptNumber ?? row.paymentId.slice(0, 8)}
                  </td>
                  <td className="px-4 py-3">{row.serviceType}</td>
<td className="px-4 py-3">
                    {(() => {
                      const presentation = presentPaymentStatus(row.status)
                      return (
                        <span
                          className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${statusToneClasses(
                            presentation.tone,
                          )}`}
                        >
                          {row.status}
                        </span>
                      )
                    })()}
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums">
                    {formatMoney(row.amount, row.currency, locale)}
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums">
                    {Number(row.refundedAmount) > 0 ? (
                      formatMoney(row.refundedAmount, row.currency, locale)
                    ) : (
                      <span className="text-muted-foreground">-</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums">
                    {row.commissionAmount === null ? (
                      // Not zero: no approved rate existed when this payment was confirmed.
                      <span className="text-xs text-muted-foreground">
                        {tf("transactions.noApprovedRate")}
                      </span>
                    ) : (
                      formatMoney(row.commissionAmount, row.currency, locale)
                    )}
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">{fmtDate(row.paidAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {!loading && !error && data && data.totalPages > 1 && (
        <div className="flex items-center justify-between gap-3 rounded-2xl border border-border bg-card px-4 py-3">
          <button
            type="button"
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
            className="rounded-lg border border-border px-3 py-1.5 font-medium text-foreground hover:bg-muted disabled:opacity-40"
          >
            {tf("pager.previous")}
          </button>
          <span className="text-xs text-muted-foreground">
            {tf("pager.total", { count: data.totalElements.toLocaleString(locale ?? undefined) })}
          </span>
          <button
            type="button"
            onClick={() => setPage(Math.min(data.totalPages - 1, page + 1))}
            disabled={page >= data.totalPages - 1}
            className="rounded-lg border border-border px-3 py-1.5 font-medium text-foreground hover:bg-muted disabled:opacity-40"
          >
            {tf("pager.next")}
          </button>
        </div>
      )}
    </section>
  )
}
