"use client"

import { useCallback, useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { listBundles, getBundle, quoteBundle, type Bundle, type BundleItem, type TopUpQuote } from "@/lib/bundle-api"
import { parentApi, MOBILE_MONEY_NETWORKS, type MobileMoneyNetwork } from "@/lib/parent-api"
import { formatMoney } from "@/lib/money"

/**
 * Browsing and buying bundles.
 *
 * Everything shown is decided by the backend. The price comes from the pricing quote endpoint,
 * never from anything on this page, and access is only ever reflected after the payment is
 * verified server-side - a successful mobile-money prompt is a *pending* payment, not a purchase,
 * so the page never claims the buyer has access on the strength of a client-side state.
 *
 * A bundle is bought through the ordinary payment lifecycle (quote -> initiate -> mobile money ->
 * poll). There is deliberately no bundle-specific checkout: a second checkout would be a second
 * set of rules, and the whole point of reusing the existing one is that there is not.
 */


type Checkout = {
  bundle: Bundle
  items: BundleItem[]
  quote: TopUpQuote | null
  studentId: string
  msisdn: string
  network: MobileMoneyNetwork
  paymentId: string | null
  status: "idle" | "initiating" | "awaiting" | "failed"
  message: string | null
}



export default function BundlesPage() {
  const t = useTranslations("bundles")
  const [bundles, setBundles] = useState<Bundle[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [checkout, setCheckout] = useState<Checkout | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setBundles(await listBundles())
    } catch (e) {
      setBundles(null)
      setError(e instanceof Error ? e.message : t("loadFailed"))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    void load()
  }, [load])

  async function open(bundle: Bundle) {
    setError(null)
    try {
      const detail = await getBundle(bundle.id)
      // No price is fetched yet, deliberately. The honest figure depends on which child is being
      // bought for and what that child already holds, so it is requested once a child is chosen.
      // Quoting the bundle's headline price here and charging it later is exactly how a learner
      // who already owns part of a bundle gets billed twice for it.
      setCheckout({
        bundle: detail.bundle,
        items: detail.items,
        quote: null,
        studentId: "",
        msisdn: "",
        network: MOBILE_MONEY_NETWORKS[0].value,
        paymentId: null,
        status: "idle",
        message: null,
      })
    } catch (e) {
      setError(e instanceof Error ? e.message : t("loadFailed"))
    }
  }

  /** Re-prices for the chosen child. Only the server knows what that child already holds. */
  async function requote(current: Checkout, studentId: string): Promise<Checkout> {
    if (!studentId) return { ...current, quote: null }
    try {
      return { ...current, studentId, quote: await quoteBundle(current.bundle.id, studentId) }
    } catch (e) {
      setError(e instanceof Error ? e.message : t("loadFailed"))
      return { ...current, studentId, quote: null }
    }
  }

  async function submit(current: Checkout) {
    // One submission at a time. A second click while a purchase is in flight would create a
    // second payment for the same bundle; the idempotency key makes a retry safe, but the guard
    // means the duplicate is never created in the first place.
    if (current.status === "initiating" || current.status === "awaiting") return
    // Refuse to send a charge without a server quote. Sending the headline bundle price because a
    // quote was missing would double-charge a learner who already holds part of the bundle.
    if (!current.quote) {
      setCheckout({ ...current, status: "failed", message: t("priceUnavailable") })
      return
    }
    setError(null)
    try {
      setCheckout({ ...current, status: "initiating", message: null })

      const initiated = await parentApi.initiatePayment({
        studentId: current.studentId,
        amount: current.quote!.total,
        serviceType: current.bundle.serviceType,
        serviceId: current.bundle.id,
        idempotencyKey: `bundle-${current.bundle.id}-${current.studentId}`,
        mobileNumber: current.msisdn,
        network: current.network,
      })
      const paymentId = initiated.paymentId

      const paid = await parentApi.payWithMobileMoney(paymentId, {
        mobileNumber: current.msisdn,
        network: current.network,
      })

      // A prompt being accepted is not a completed payment. The page reflects the status the
      // server reports and stops there.
      setCheckout({
        ...current,
        paymentId,
        status: paid.status === "COMPLETED" ? "awaiting" : "awaiting",
        message: t("awaitingConfirmation"),
      })
    } catch (e) {
      setCheckout({
        ...current,
        status: "failed",
        message: e instanceof Error ? e.message : t("purchaseFailed"),
      })
    }
  }

  if (loading) {
    return (
      <div className="p-6">
        <p className="text-sm text-muted-foreground">{t("loading")}</p>
      </div>
    )
  }

  return (
    <div className="space-y-6 p-6">
      <header>
        <h1 className="text-xl font-semibold text-foreground">{t("title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("subtitle")}</p>
      </header>

      {error && (
        <section className="rounded-2xl border border-border bg-card p-5">
          <p className="text-sm text-foreground">{error}</p>
          <button
            type="button"
            onClick={() => void load()}
            className="mt-3 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground hover:bg-muted"
          >
            {t("retry")}
          </button>
        </section>
      )}

      {!error && (bundles?.length ?? 0) === 0 && (
        <section className="rounded-2xl border border-dashed border-border bg-card p-5">
          <h2 className="text-base font-semibold text-foreground">{t("emptyTitle")}</h2>
          <p className="mt-1 text-sm text-muted-foreground">{t("emptyBody")}</p>
        </section>
      )}

      {!error && (bundles?.length ?? 0) > 0 && (
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {bundles!.map((bundle) => (
            <li key={bundle.id} className="rounded-2xl border border-border bg-card p-5">
              <h2 className="text-base font-semibold text-foreground">{bundle.name}</h2>
              {bundle.description && (
                <p className="mt-1 text-sm text-muted-foreground">{bundle.description}</p>
              )}
              <button
                type="button"
                onClick={() => void open(bundle)}
                className="mt-4 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground hover:bg-muted"
              >
                {t("view")}
              </button>
            </li>
          ))}
        </ul>
      )}

      {checkout && (
        <section className="rounded-2xl border border-border bg-card p-5">
          <div className="flex items-start justify-between gap-4">
            <h2 className="text-base font-semibold text-foreground">{checkout.bundle.name}</h2>
            <button
              type="button"
              onClick={() => setCheckout(null)}
              className="text-sm text-muted-foreground hover:text-foreground"
            >
              {t("close")}
            </button>
          </div>

          <h3 className="mt-4 text-sm font-medium text-foreground">{t("includes")}</h3>
          <ul className="mt-2 space-y-1">
            {checkout.items.map((item) => (
              <li key={item.id} className="text-sm text-muted-foreground">
                {item.serviceType}
              </li>
            ))}
          </ul>

          <div className="mt-4 grid gap-3 sm:grid-cols-3">
            <label className="text-sm">
              <span className="block text-muted-foreground">{t("child")}</span>
              <input
                value={checkout.studentId}
                onChange={(e) =>
                  setCheckout({ ...checkout, studentId: e.target.value })
                }
                className="mt-1 w-full rounded-lg border border-input bg-background px-2.5 py-1.5 text-sm"
              />
            </label>
            <label className="text-sm">
              <span className="block text-muted-foreground">{t("msisdn")}</span>
              <input
                value={checkout.msisdn}
                onChange={(e) => setCheckout({ ...checkout, msisdn: e.target.value })}
                className="mt-1 w-full rounded-lg border border-input bg-background px-2.5 py-1.5 text-sm"
              />
            </label>
            <label className="text-sm">
              <span className="block text-muted-foreground">{t("network")}</span>
              <select
                value={checkout.network}
                onChange={(e) =>
                  setCheckout({ ...checkout, network: e.target.value as MobileMoneyNetwork })
                }
                className="mt-1 w-full rounded-lg border border-input bg-background px-2.5 py-1.5 text-sm"
              >
                {MOBILE_MONEY_NETWORKS.map((n) => (
                  <option key={n.value} value={n.value}>
                    {n.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-sm text-muted-foreground">
              {checkout.quote ? t("price") : t("selectChildFirst")}
            </span>
            <span className="text-lg font-semibold text-foreground">
              {checkout.quote
                ? formatMoney(checkout.quote.total, checkout.quote.currency ?? "TZS")
                : ""}
            </span>
          </div>
          {checkout.quote && checkout.quote.coveredCount > 0 && (
            <p className="mt-1 text-xs text-muted-foreground">{t("alreadyCovered", { count: checkout.quote.coveredCount })}</p>
          )}
          {checkout.quote?.fullyCovered && (
            <p className="mt-2 text-sm text-muted-foreground">{t("fullyCovered")}</p>
          )}

          {checkout.message && (
            <p className="mt-3 text-sm text-muted-foreground">{checkout.message}</p>
          )}

          <button
            type="button"
            disabled={
              checkout.status === "initiating" ||
              checkout.status === "awaiting" ||
              !checkout.studentId ||
              !checkout.msisdn
            }
            onClick={() => void submit(checkout)}
            className="mt-4 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground disabled:opacity-50 hover:bg-muted"
          >
            {checkout.status === "initiating"
              ? t("submitting")
              : checkout.status === "awaiting"
                ? t("awaitingConfirmation")
                : t("pay")}
          </button>
        </section>
      )}
    </div>
  )
}