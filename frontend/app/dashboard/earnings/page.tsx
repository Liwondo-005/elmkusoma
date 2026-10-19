"use client"

import { useCallback, useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { api } from "@/lib/api-client"
import { formatMoney } from "@/lib/money"
import type { ApiResponse } from "@/lib/types/api"

/**
 * Earnings recorded against the signed-in person - a teacher, trainer, provider or event organiser.
 *
 * Everything shown here comes from `GET /v1/earnings/me`, which takes no beneficiary parameter:
 * the server decides who is asking and returns only what was recorded for them. Nothing on this
 * page is derived in the browser, and there is no way to ask for somebody else's figures by
 * changing a URL.
 *
 * Two things are deliberately absent. There is no profit figure, because profit needs fees, taxes,
 * expenses and a settlement date that the system does not record - gross minus provider share is
 * not an accounting result. And there is no "estimated" or "projected" value: an amount nobody has
 * been paid is not earnings, and showing one would put a number on screen that no transaction
 * supports.
 */

/** Mirrors EarningsService.Amount. `resolved` false means no approved policy produced this amount. */
type Amount = {
  value: number
  currency: string
  resolved: boolean
  note?: string | null
}

type EarningsSummary = {
  institutionId: string | null
  from: string | null
  to: string | null
  completedPaymentCount: number
  grossCollected: Amount
  refunded: Amount
  platformCommission: Amount
  providerShare: Amount
  unallocatedValue: Amount
  unallocatedCount: number
  profitNote?: string | null
}

export default function MyEarningsPage() {
  const t = useTranslations("earnings")
  const [summary, setSummary] = useState<EarningsSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await api.get<EarningsSummary>("/v1/earnings/me")
      // A 200 with no payload is not earnings. Treating it as an empty summary would render a
      // confident zero where the truth is "the server sent nothing".
      if (!response.data) {
        throw new Error(t("loadFailed"))
      }
      setSummary(response.data)
    } catch (e) {
      setSummary(null)
      setError(e instanceof Error ? e.message : t("loadFailed"))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    void load()
  }, [load])

  if (loading) {
    return (
      <div className="p-6">
        <p className="text-sm text-muted-foreground">{t("loading")}</p>
      </div>
    )
  }

  // An error must not render a zeroed summary. "We could not reach the server" and "you earned
  // nothing" look identical on a balance, and only one of them is true.
  if (error) {
    return (
      <div className="p-6">
        <section className="rounded-2xl border border-border bg-card p-5">
          <h2 className="text-base font-semibold text-foreground">{t("errorTitle")}</h2>
          <p className="mt-1 text-sm text-muted-foreground">{error}</p>
          <button
            type="button"
            onClick={() => void load()}
            className="mt-4 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground hover:bg-muted"
          >
            {t("retry")}
          </button>
        </section>
      </div>
    )
  }

  const hasPayments = (summary?.completedPaymentCount ?? 0) > 0

  return (
    <div className="space-y-6 p-6">
      <header>
        <h1 className="text-xl font-semibold text-foreground">{t("title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("subtitle")}</p>
      </header>

      {!hasPayments ? (
        <section className="rounded-2xl border border-dashed border-border bg-card p-5">
          <h2 className="text-base font-semibold text-foreground">{t("emptyTitle")}</h2>
          <p className="mt-1 text-sm text-muted-foreground">{t("emptyBody")}</p>
          <p className="mt-3 text-xs text-muted-foreground">{t("emptyHistoricalNote")}</p>
        </section>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-3">
            <Tile
              label={t("grossCollected")}
              amount={summary!.grossCollected}
            />
            <Tile
              label={t("providerShare")}
              amount={summary!.providerShare}
              hint={summary!.providerShare.resolved ? undefined : t("unresolvedHint")}
            />
            <Tile
              label={t("refunded")}
              amount={summary!.refunded}
            />
          </div>

          {summary!.unallocatedCount > 0 && (
            <section className="rounded-2xl border border-border bg-card p-5">
              <h2 className="text-base font-semibold text-foreground">
                {t("unallocatedTitle")}
              </h2>
              <p className="mt-1 text-sm text-muted-foreground">
                {summary!.unallocatedValue.note ?? t("unallocatedDefault")}
              </p>
              <p className="mt-3 text-sm font-medium text-foreground">
                {formatMoney(summary!.unallocatedValue.value, summary!.unallocatedValue.currency)}
              </p>
            </section>
          )}

          <section className="rounded-2xl border border-border bg-card p-5">
            <h2 className="text-base font-semibold text-foreground">{t("profitTitle")}</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              {summary!.profitNote ?? t("profitDefault")}
            </p>
          </section>
        </>
      )}
    </div>
  )
}

function Tile({
  label,
  amount,
  hint,
}: {
  label: string
  amount: Amount
  hint?: string
}) {
  // An unresolved amount is shown as the backend sent it, marked - never quietly as zero. "No
  // approved policy produced this" and "this is zero" are different facts and the display must not
  // collapse them.
  return (
    <section className="rounded-2xl border border-border bg-card p-5">
      <h2 className="text-sm text-muted-foreground">{label}</h2>
      <p className="mt-2 text-2xl font-semibold text-foreground">
        {formatMoney(amount.value, amount.currency)}
      </p>
      {!amount.resolved && (
        <p className="mt-1 text-xs text-muted-foreground">{hint ?? ""}</p>
      )}
    </section>
  )
}