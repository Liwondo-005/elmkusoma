"use client"

import { useCallback, useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { api } from "@/lib/api-client"
import type { PageResponse } from "@/lib/types/api"
import { useLocale } from "@/hooks/use-locale"
import { formatDate } from "@/lib/format-date"
import { formatMoney } from "@/lib/money"
import { presentPaymentStatus, statusToneClasses } from "@/lib/payment-status"

/**
 * Provider transactions - the sales recorded against this institution's services.
 *
 * Everything shown here comes from `GET /v1/provider/financial/transactions`, which takes no
 * institution parameter: the server resolves the caller's own institution from the authenticated
 * principal and returns only that institution's rows. Nothing on this page is derived in the
 * browser, and there is no way to ask for another provider's sales by changing a URL or a header.
 *
 * Two honesty rules carried over from the finance command centre. A commission (or provider
 * share) that the backend returns as null is rendered as "no approved rate" or an em dash, never
 * as zero - "no approved rate existed at confirmation" and "the rate was zero" are different
 * facts. And no summed total is shown: the endpoint is paged, so summing the rows on screen and
 * labelling it a total would understate the ledger.
 */

/** Mirrors TransactionSearchService.TransactionRow on the backend. */
type ProviderTransaction = {
  paymentId: string
  receiptNumber: string | null
  providerReference: string | null
  provider: string | null
  serviceType: string
  description: string | null
  status: string
  amount: number
  currency: string
  refundedAmount: number
  refundableAmount: number
  commissionAmount: number | null
  providerShareAmount: number | null
  commissionStatus: string | null
  commissionPolicyKey: string | null
  commissionPolicyVersion: number | null
  paidAt: string | null
  createdAt: string | null
  parentId: string | null
  studentId: string | null
  institutionId: string | null
}

const PAGE_SIZE = 20

export default function ProviderPaymentsPage() {
  const tn = useTranslations("nav")
  const te = useTranslations("earnings")
  const tc = useTranslations("common")
  const tf = useTranslations("platformAdmin.finance")
  const locale = useLocale()

  const [page, setPage] = useState(0)
  const [data, setData] = useState<PageResponse<ProviderTransaction> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await api.get<PageResponse<ProviderTransaction>>(
        "/v1/provider/financial/transactions",
        { page, size: PAGE_SIZE },
      )
      // A 200 with no payload is not a ledger. Treating it as an empty page would render a
      // confident "no transactions" where the truth is "the server sent nothing".
      if (!response.data) {
        throw new Error(te("loadFailed"))
      }
      setData(response.data)
    } catch (e) {
      setData(null)
      setError(e instanceof Error ? e.message : te("loadFailed"))
    } finally {
      setLoading(false)
    }
  }, [page, te])

  useEffect(() => {
    void load()
  }, [load])

  if (loading) {
    return (
      <div className="p-6">
        <p className="text-sm text-muted-foreground">{te("loading")}</p>
      </div>
    )
  }

  // An error must not render an empty table. "We could not reach the server" and "you have no
  // sales" look identical on a list, and only one of them is true.
  if (error) {
    return (
      <div className="p-6">
        <section className="rounded-2xl border border-border bg-card p-5">
          <h2 className="text-base font-semibold text-foreground">{te("errorTitle")}</h2>
          <p className="mt-1 text-sm text-muted-foreground">{error}</p>
          <button
            type="button"
            onClick={() => void load()}
            className="mt-4 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground hover:bg-muted"
          >
            {te("retry")}
          </button>
        </section>
      </div>
    )
  }

  const rows = data?.content ?? []
  const isEmpty = rows.length === 0

  return (
    <div className="space-y-6 p-6">
      <header>
        <h1 className="text-xl font-semibold text-foreground">{tn("payments")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{te("subtitle")}</p>
      </header>

      {isEmpty ? (
        <section className="rounded-2xl border border-dashed border-border bg-card p-5">
          <h2 className="text-base font-semibold text-foreground">{te("emptyTitle")}</h2>
          <p className="mt-1 text-sm text-muted-foreground">{te("emptyBody")}</p>
        </section>
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-border bg-card">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-border text-xs uppercase text-muted-foreground">
              <tr>
                <th scope="col" className="px-4 py-3">{tf("transactions.receipt")}</th>
                <th scope="col" className="px-4 py-3">{tf("transactions.service")}</th>
                <th scope="col" className="px-4 py-3">{tf("transactions.status")}</th>
                <th scope="col" className="px-4 py-3 text-right">{tf("transactions.amount")}</th>
                <th scope="col" className="px-4 py-3">{tf("transactions.paidAt")}</th>
                <th scope="col" className="px-4 py-3 text-right">{tf("transactions.refunded")}</th>
                <th scope="col" className="px-4 py-3 text-right">{tf("transactions.commission")}</th>
                <th scope="col" className="px-4 py-3 text-right">{te("providerShare")}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.paymentId} className="border-b border-border/60 last:border-0">
                  <td className="px-4 py-3 font-mono text-xs">
                    {row.receiptNumber ?? row.paymentId.slice(0, 8)}
                  </td>
                  <td className="px-4 py-3">
                    {row.serviceType}
                    {row.description && (
                      <span className="block text-xs text-muted-foreground">{row.description}</span>
                    )}
                  </td>
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
                  <td className="px-4 py-3 text-muted-foreground">
                    {formatDate(row.paidAt ?? row.createdAt, locale)}
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
                  <td className="px-4 py-3 text-right tabular-nums">
                    {row.providerShareAmount === null ? (
                      <span className="text-muted-foreground">-</span>
                    ) : (
                      formatMoney(row.providerShareAmount, row.currency, locale)
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {!isEmpty && data && data.totalPages > 1 && (
        <div className="flex items-center justify-between gap-3 rounded-2xl border border-border bg-card px-4 py-3">
          <button
            type="button"
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
            className="rounded-lg border border-border px-3 py-1.5 font-medium text-foreground hover:bg-muted disabled:opacity-40"
          >
            {tc("previous")}
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
            {tc("next")}
          </button>
        </div>
      )}
    </div>
  )
}
