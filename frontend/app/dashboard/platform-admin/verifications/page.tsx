"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState } from "react"
import { CheckCircle, XCircle, Loader2, ShieldCheck } from "lucide-react"
import { platformAdminApi, type VerificationSummary } from "@/lib/platform-admin-api"

export default function VerificationsPage() {
  const t = useTranslations("platformAdmin");
  const [items, setItems] = useState<VerificationSummary[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    platformAdminApi.listPendingVerifications().then(setItems).finally(() => setLoading(false))
  }, [])

  async function review(id: string, status: string) {
    await platformAdminApi.reviewVerification(id, status)
    setItems(prev => prev.filter(v => v.id !== id))
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("verifications.pendingVerifications")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("verifications.reviewAndApprovePending")}</p>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : items.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-10 text-center">
          <ShieldCheck className="mx-auto size-10 text-green-500/50" />
          <p className="mt-3 text-sm text-muted-foreground">{t("verifications.allCaughtUpNo")}</p>
        </div>
      ) : (
        <div className="space-y-3">
          {items.map(v => (
            <div key={v.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-semibold text-foreground">{v.verificationType} — {v.entityType}</p>
                    <span className="rounded-full bg-yellow-50 px-2 py-0.5 text-[10px] font-semibold text-yellow-700">{v.status}</span>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {t("verifications.entity")}{v.entityId?.slice(0, 8)}{t("verifications.submitted")}{new Date(v.submittedAt).toLocaleString("en-GB")}
                  </p>
                </div>
                <div className="flex gap-2">
                  <button onClick={() => review(v.id, "APPROVED")} className="inline-flex items-center gap-1 rounded-xl bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700">
                    <CheckCircle className="size-3" /> {t("verifications.approve")}</button>
                  <button onClick={() => review(v.id, "REJECTED")} className="inline-flex items-center gap-1 rounded-xl border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50">
                    <XCircle className="size-3" /> {t("verifications.reject")}</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
