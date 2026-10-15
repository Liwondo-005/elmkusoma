"use client"

import { useTranslations } from "next-intl"
import { CircleSlash, ReceiptText } from "lucide-react"

/**
 * Subscription plans.
 *
 * This page previously rendered three hard-coded plans - "Free TZS 0", "Premium TZS 25,000" and
 * "Institution Custom" - with Upgrade buttons that had no click handler at all. It looked like a
 * pricing catalogue while being a static mockup, and the 25,000 figure matched no approved policy.
 *
 * There is no subscription entity, plan, price list or purchase endpoint anywhere in the backend:
 * no `subscriptions` table, no `/v1/subscription/**` route, no price on any content entity. The
 * only priced records in the system are platform service rows, which are administrator-facing.
 *
 * So rather than re-render different invented numbers, the page states the situation and points
 * at what does exist. Filling this in properly needs a Developer 01 contract for plans and prices;
 * the requirement is recorded in the delivery report.
 */
export default function DashboardSubscriptionPage() {
  const t = useTranslations("subscription")

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("subtitle")}</p>
      </div>

      <div className="rounded-2xl border border-dashed border-border bg-card p-8 text-center">
        <CircleSlash className="mx-auto size-8 text-muted-foreground" aria-hidden="true" />
        <h2 className="mt-3 text-base font-semibold text-foreground">{t("unavailableTitle")}</h2>
        <p className="mx-auto mt-2 max-w-xl text-sm text-muted-foreground">{t("unavailableBody")}</p>
      </div>

      <section className="rounded-2xl border border-border bg-card p-5">
        <h3 className="flex items-center gap-2 text-sm font-semibold text-foreground">
          <ReceiptText className="size-4 text-muted-foreground" aria-hidden="true" />
          {t("whatWorksTodayTitle")}
        </h3>
        <ul className="mt-3 space-y-2 text-sm text-muted-foreground">
          <li className="flex items-start gap-2">
            <span aria-hidden="true" className="mt-1.5 size-1.5 shrink-0 rounded-full bg-muted-foreground" />
            {t("whatWorksToday.paymentHistory")}
          </li>
          <li className="flex items-start gap-2">
            <span aria-hidden="true" className="mt-1.5 size-1.5 shrink-0 rounded-full bg-muted-foreground" />
            {t("whatWorksToday.pendingStatus")}
          </li>
          <li className="flex items-start gap-2">
            <span aria-hidden="true" className="mt-1.5 size-1.5 shrink-0 rounded-full bg-muted-foreground" />
            {t("whatWorksToday.accessGrants")}
          </li>
        </ul>
        <p className="mt-4 text-sm">
          <a
            href="/dashboard/parent/payments"
            className="font-medium text-primary underline-offset-4 hover:underline"
          >
            {t("viewPayments")}
          </a>
        </p>
      </section>
    </div>
  )
}
