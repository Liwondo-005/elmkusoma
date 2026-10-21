"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import { useTranslations } from "next-intl"
import { AlertCircle, Receipt, ShieldCheck, Clock, XCircle } from "lucide-react"
import { useAuth } from "@/lib/auth"
import { learnerApi, type MyPayment, type MyAccess } from "@/lib/learner-api"
import { EmptyState, LoadingState, LearnerHeader } from "@/components/learner/shared"
import { formatMoney } from "@/lib/money"
import { formatDate } from "@/lib/format-date"

/**
 * A learner's own purchases and access.
 *
 * <p>Everything on this page comes from two endpoints that resolve the caller from their own
 * session - there is no identifier in the URL to change, so there is nothing here that could show
 * a different learner's spending.</p>
 *
 * <p>Two rules the page follows deliberately:</p>
 *
 * <ul>
 *   <li>A pending payment is shown as pending. Nothing on this page can turn a queued prompt into
 *       a paid receipt; only the backend saying COMPLETED does that, and the page renders
 *       whatever status the server sends.</li>
 *   <li>Access is read from the access grant, not inferred from the payment. A learner who paid
 *       but whose entitlement has lapsed is shown as having expired access, not as a current
 *       subscriber, because the entitlement is what actually grants access.</li>
 * </ul>
 *
 * <p>No total is computed here. Summing a list the client holds would be a page-local figure that
 * disagrees with the backend the moment the list is filtered or paginated.</p>
 */
export default function LearnerPaymentsPage() {
  const { user } = useAuth()
  const t = useTranslations("learner.payments")
  const tc = useTranslations("common")

  const [payments, setPayments] = useState<MyPayment[]>([])
  const [access, setAccess] = useState<MyAccess[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  // Guards the state updates that follow a fetch which may already have been superseded, since
  // the loader itself is now reachable from a retry button and not only from the first mount.
  const activeRef = useRef(true)

  useEffect(() => {
    activeRef.current = true
    return () => {
      activeRef.current = false
    }
  }, [])

  const load = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      // Both are needed and they are independent: a learner may have access granted by a
      // parent with no payment of their own, and may have a payment with no grant yet.
      const [paymentRows, accessRows] = await Promise.all([
        learnerApi.getMyPayments(),
        learnerApi.getMyAccess(),
      ])
      if (!activeRef.current) return
      setPayments(paymentRows)
      setAccess(accessRows)
    } catch {
      if (activeRef.current) setError(t("loadFailed"))
    } finally {
      if (activeRef.current) setLoading(false)
    }
  }, [t])

  useEffect(() => {
    void load()
  }, [load])

  const firstName = user?.firstName || user?.name?.split(" ")[0] || t("defaultLearnerName")
  const activeAccess = access.filter((a) => a.currentlyValid)
  const expiredAccess = access.filter((a) => !a.currentlyValid)

  return (
    <div className="space-y-6">
      <LearnerHeader firstName={firstName} subtitle={t("subtitle")} />

      {loading && <LoadingState />}

      {!loading && error && (
        <div
          className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-destructive/40 bg-destructive/5 p-4 text-sm text-destructive"
          role="alert"
        >
          <span className="inline-flex items-center gap-2">
            <AlertCircle className="size-4 shrink-0" aria-hidden="true" />
            {error}
          </span>
          {/* A failed read here is almost always transient, and without this the only way back
              is a full page reload - which also discards the learner's place on the page. */}
          <button
            type="button"
            onClick={() => void load()}
            className="rounded-lg border border-destructive/40 px-3 py-1.5 text-xs font-medium text-destructive hover:bg-destructive/10"
          >
            {tc("retry")}
          </button>
        </div>
      )}

      {!loading && !error && (
        <>
          <section className="space-y-3">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
              {t("accessTitle")}
            </h2>

            {activeAccess.length === 0 ? (
              <EmptyState
                icon={<ShieldCheck className="size-6" aria-hidden="true" />}
                title={t("noAccess")}
                description={t("noAccessDescription")}
              />
            ) : (
              <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {activeAccess.map((a) => (
                  <li
                    key={a.entitlementId}
                    className="rounded-2xl border border-border bg-card p-4"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <span className="font-medium">{a.serviceType}</span>
                      <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-xs font-medium text-emerald-700 dark:text-emerald-400">
                        <ShieldCheck className="size-3" aria-hidden="true" />
                        {t("active")}
                      </span>
                    </div>
                    <p className="mt-2 text-xs text-muted-foreground">
                      {a.expiresAt ? (
                        <>
                          {t("expires")} {formatDate(a.expiresAt)}
                        </>
                      ) : (
                        t("noExpiry")
                      )}
                    </p>
                  </li>
                ))}
              </ul>
            )}

            {expiredAccess.length > 0 && (
              <details className="rounded-2xl border border-border bg-card p-4">
                <summary className="cursor-pointer text-sm font-medium text-muted-foreground">
                  {t("expiredTitle", { count: expiredAccess.length })}
                </summary>
                <ul className="mt-3 space-y-2">
                  {expiredAccess.map((a) => (
                    <li
                      key={a.entitlementId}
                      className="flex items-center justify-between gap-2 text-sm"
                    >
                      <span>{a.serviceType}</span>
                      <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                        <XCircle className="size-3" aria-hidden="true" />
                        {a.expiresAt ? `${t("expiredOn")} ${formatDate(a.expiresAt)}` : t("revoked")}
                      </span>
                    </li>
                  ))}
                </ul>
              </details>
            )}
          </section>

          <section className="space-y-3">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
              {t("historyTitle")}
            </h2>

            {payments.length === 0 ? (
              <EmptyState
                icon={<Receipt className="size-6" aria-hidden="true" />}
                title={t("noPayments")}
                description={t("noPaymentsDescription")}
              />
            ) : (
              <div className="overflow-x-auto rounded-2xl border border-border bg-card">
                <table className="w-full text-left text-sm">
                  <caption className="sr-only">{t("caption")}</caption>
                  <thead className="border-b border-border text-xs uppercase text-muted-foreground">
                    <tr>
                      <th scope="col" className="px-4 py-3">{t("service")}</th>
                      <th scope="col" className="px-4 py-3">{t("status")}</th>
                      <th scope="col" className="px-4 py-3 text-right">{t("amount")}</th>
                      <th scope="col" className="px-4 py-3">{t("date")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {payments.map((p) => (
                      <tr key={p.paymentId} className="border-b border-border/60 last:border-0">
                        <td className="px-4 py-3">
                          <span className="font-medium">{p.serviceType}</span>
                          {p.receiptNumber && (
                            <span className="block font-mono text-xs text-muted-foreground">
                              {p.receiptNumber}
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3">
                          <StatusPill status={p.status} />
                        </td>
                        <td className="px-4 py-3 text-right tabular-nums">
                          {formatMoney(p.amount, p.currency)}
                        </td>
                        <td className="px-4 py-3 text-muted-foreground">
                          {formatDate(p.paidAt ?? p.createdAt)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      )}
    </div>
  )
}

/**
 * Renders the backend's status verbatim in tone.
 *
 * <p>A pending or failed payment is never dressed as a success. The icon is decorative and the
 * text carries the meaning, so a screen reader hears the status rather than only the colour.</p>
 */
function StatusPill({ status }: { status: string }) {
  const t = useTranslations("learner.payments")

  const tone =
    status === "COMPLETED"
      ? "bg-emerald-500/10 text-emerald-700 dark:text-emerald-400"
      : status === "PENDING" || status === "PROCESSING"
        ? "bg-amber-500/10 text-amber-700 dark:text-amber-400"
        : status === "FAILED" || status === "CANCELLED"
          ? "bg-destructive/10 text-destructive"
          : "bg-muted text-muted-foreground"

  const Icon =
    status === "COMPLETED"
      ? ShieldCheck
      : status === "PENDING" || status === "PROCESSING"
        ? Clock
        : XCircle

  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium ${tone}`}>
      <Icon className="size-3" aria-hidden="true" />
      {status}
    </span>
  )
}