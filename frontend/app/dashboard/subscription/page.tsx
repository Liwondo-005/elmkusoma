"use client"

import { useTranslations } from "next-intl"
import { CheckCircle } from "lucide-react"

export default function DashboardSubscriptionPage() {
  const t = useTranslations("subscription")
  const plans = [
    {
      id: "free",
      name: t("plans.free.name"),
      price: "0",
      period: t("plans.free.period"),
      features: [t("plans.free.f1"), t("plans.free.f2"), t("plans.free.f3"), t("plans.free.f4")],
      current: true,
    },
    {
      id: "premium",
      name: t("plans.premium.name"),
      price: "25,000",
      period: t("plans.premium.period"),
      features: [t("plans.premium.f1"), t("plans.premium.f2"), t("plans.premium.f3"), t("plans.premium.f4"), t("plans.premium.f5"), t("plans.premium.f6")],
      current: false,
      highlighted: true,
    },
    {
      id: "institution",
      name: t("plans.institution.name"),
      price: "Custom",
      period: "",
      features: [t("plans.institution.f1"), t("plans.institution.f2"), t("plans.institution.f3"), t("plans.institution.f4"), t("plans.institution.f5"), t("plans.institution.f6")],
      current: false,
    },
  ]
  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("subtitle")}</p>
      </div>

      <div className="grid gap-6 sm:grid-cols-3">
        {plans.map((plan) => (
          <div
            key={plan.id}
            className={`rounded-2xl border bg-card p-6 shadow-xs ${plan.highlighted ? "border-primary border-2" : "border-border"}`}
          >
            {plan.highlighted && (
              <span className="inline-flex rounded-full bg-primary/10 px-2.5 py-0.5 text-[10px] font-bold text-primary">
                {t("recommendedBadge")}
              </span>
            )}
            <h3 className="mt-3 text-lg font-bold text-foreground">{plan.name}</h3>
            <div className="mt-2 flex items-baseline gap-1">
              <span className="text-3xl font-extrabold text-foreground">{plan.price === "Custom" ? t("customPrice") : `TZS ${plan.price}`}</span>
              {plan.period && <span className="text-sm text-muted-foreground">{plan.period}</span>}
            </div>
            <ul className="mt-5 space-y-3">
              {plan.features.map((f) => (
                <li key={f} className="flex items-start gap-2 text-sm text-muted-foreground">
                  <CheckCircle className="mt-0.5 size-4 shrink-0 text-teal" />
                  {f}
                </li>
              ))}
            </ul>
            <button
              className={`mt-6 h-10 w-full rounded-lg text-sm font-medium transition-colors ${
                plan.current
                  ? "border border-border bg-muted text-muted-foreground"
                  : plan.highlighted
                  ? "bg-primary text-primary-foreground hover:bg-primary/90"
                  : "border border-border bg-card text-foreground hover:bg-muted"
              }`}
              disabled={plan.current}
            >
              {plan.current ? t("currentPlanButton") : t("upgradeButton")}
            </button>
          </div>
        ))}
      </div>
    </div>
  )
}
