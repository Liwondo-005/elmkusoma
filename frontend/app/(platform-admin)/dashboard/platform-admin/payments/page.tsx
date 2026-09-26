"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { CreditCard, Loader2, DollarSign } from "lucide-react"
import { platformAdminApi, type PaymentSummary, type PageResponse } from "@/lib/platform-admin-api"

const STATUS_OPTIONS = ["", "PENDING", "COMPLETED", "FAILED", "REFUNDED"]
const PAGE_SIZE = 20

export default function PaymentsPage() {
  const t = useTranslations("platformAdmin");
  const tc = useTranslations("common");
  const [data, setData] = useState<PageResponse<PaymentSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState("")

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await platformAdminApi.listPayments(page, PAGE_SIZE, status || undefined)
      setData(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("payments.failedToLoadPayments"))
    } finally {
      setLoading(false)
    }
  }, [page, status])

  useEffect(() => {
    loadData()
  }, [loadData])

  const getStatusBadge = (s: string) => {
    switch (s) {
      case "COMPLETED":
        return <span className="inline-block rounded-full bg-green-500/10 px-2.5 py-0.5 text-xs font-medium text-green-600">COMPLETED</span>
      case "PENDING":
        return <span className="inline-block rounded-full bg-amber-500/10 px-2.5 py-0.5 text-xs font-medium text-amber-600">PENDING</span>
      case "FAILED":
        return <span className="inline-block rounded-full bg-red-500/10 px-2.5 py-0.5 text-xs font-medium text-red-600">FAILED</span>
      case "REFUNDED":
        return <span className="inline-block rounded-full bg-blue-500/10 px-2.5 py-0.5 text-xs font-medium text-blue-600">REFUNDED</span>
      default:
        return <span className="inline-block rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground">{s}</span>
    }
  }

  const formatDate = (d: string) => new Date(d).toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" })

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("payments.payments")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("payments.viewAllPaymentTransactions")}</p>
      </div>

      <div className="flex items-center gap-3">
        <select
          value={status}
          onChange={(e) => { setStatus(e.target.value); setPage(0) }}
          className="rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
        >
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>{s || t("payments.allStatuses")}</option>
          ))}
        </select>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive">{error}</div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-8 animate-spin text-muted-foreground" />
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <CreditCard className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">{t("payments.noPaymentsFound")}</p>
          <p className="mt-1 text-sm text-muted-foreground">
            {status ? t("payments.noPaymentsMatchThe") : t("payments.noPaymentTransactionsAvailable")}
          </p>
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("payments.amount")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("payments.status")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("payments.serviceType")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("payments.date")}</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((payment) => (
                  <tr key={payment.id} className="border-b border-border last:border-0 hover:bg-muted/30 transition-colors">
                    <td className="px-5 py-3.5">
                      <span className="inline-flex items-center gap-1 font-medium text-foreground">
                        <DollarSign className="size-3.5 text-muted-foreground" />
                        {payment.amount.toLocaleString()} {payment.currency}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">{getStatusBadge(payment.status)}</td>
                    <td className="px-5 py-3.5 text-muted-foreground">{payment.serviceType}</td>
                    <td className="px-5 py-3.5 text-muted-foreground">{formatDate(payment.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {data && data.totalElements > PAGE_SIZE && (
        <div className="flex items-center justify-center gap-2">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
          >
            {t("payments.prev")}</button>
          <span className="text-sm text-muted-foreground">
            {t("payments.pageOf", { p0: page + 1, p1: data.totalPages })}</span>
          <button
            onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
            disabled={page >= data.totalPages - 1}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
          >
            {tc("next")}</button>
        </div>
      )}
    </div>
  )
}
