"use client"

import { useCallback, useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { parentApi, type ParentSubscription } from "@/lib/parent-api"

/**
 * Subscriptions this parent is paying for, and the one action available on them.
 *
 * <p>The only action is cancel-at-period-end, which is the whole of the approved BR-01 policy. The
 * wording matters more than usual here: cancelling does not cut access off now and does not refund.
 * A heading that just says "Cancel" would let a family believe they were refunded or cut off when
 * neither is true, so the button asks for a confirmation that states both, and the resulting state
 * shows the date access actually runs to.
 *
 * <p>An elapsed period has nothing left to cancel and the server says so. That refusal is shown
 * verbatim rather than softened, because "there is nothing left to cancel" is the truthful answer
 * and a vague error would send the reader looking for a problem that does not exist.
 */
export default function SubscriptionsSection({
  onChanged,
}: {
  onChanged?: () => void | Promise<void>
}) {
  const t = useTranslations("parent.subscriptions")
  const [items, setItems] = useState<ParentSubscription[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [confirmingId, setConfirmingId] = useState<string | null>(null)

  const load = useCallback(async () => {
    setError(null)
    try {
      setItems(await parentApi.getSubscriptions())
    } catch (e) {
      setItems(null)
      setError(e instanceof Error ? e.message : t("loadFailed"))
    }
  }, [t])

  useEffect(() => {
    void load()
  }, [load])

  async function cancel(subscription: ParentSubscription) {
    setError(null)
    setNotice(null)
    setBusyId(subscription.id)
    try {
      await parentApi.cancelSubscription(subscription.id)
      setConfirmingId(null)
      setNotice(
        t("cancelledNotice", {
          date: new Date(subscription.periodEnd).toLocaleDateString(),
        })
      )
      await load()
      await onChanged?.()
    } catch (e) {
      // The server's refusal is the useful part: an elapsed period answers "nothing left to
      // cancel", and paraphrasing that would hide the only fact the reader needs.
      setError(e instanceof Error ? e.message : t("cancelFailed"))
    } finally {
      setBusyId(null)
    }
  }

  const cancellable = (items ?? []).filter((s) => s.status === "ACTIVE")

  return (
    <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <h2 className="text-base font-semibold text-foreground">{t("title")}</h2>
      <p className="mt-1 text-sm text-muted-foreground">{t("subtitle")}</p>

      {error && (
        <p role="alert" className="mt-3 text-sm text-foreground">
          {error}
        </p>
      )}
      {notice && !error && <p className="mt-3 text-sm text-muted-foreground">{notice}</p>}

      {items === null && !error && (
        <p className="mt-3 text-sm text-muted-foreground">{t("loading")}</p>
      )}

      {items !== null && items.length === 0 && (
        <p className="mt-3 text-sm text-muted-foreground">{t("empty")}</p>
      )}

      {items !== null && items.length > 0 && (
        <ul className="mt-3 space-y-2">
          {items.map((subscription) => {
            const canCancel = subscription.status === "ACTIVE"
            return (
              <li
                key={subscription.id}
                className="rounded-xl border border-border p-4 text-sm"
              >
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <p className="font-medium text-foreground">
                      {subscription.serviceType}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {t("accessUntil", {
                        date: new Date(subscription.periodEnd).toLocaleDateString(),
                      })}
                    </p>
                    {subscription.cancelledAt && (
                      <p className="mt-1 text-xs text-muted-foreground">
                        {t("renewalStopped", {
                          date: new Date(subscription.cancelledAt).toLocaleDateString(),
                        })}
                      </p>
                    )}
                  </div>
                  <span className="rounded-full bg-muted px-3 py-1 text-xs text-muted-foreground">
                    {t(`status.${subscription.status}`)}
                  </span>
                </div>

                {canCancel && (
                  <div className="mt-3">
                    {confirmingId === subscription.id ? (
                      <div className="rounded-lg border border-border p-3">
                        <p className="text-sm text-foreground">{t("confirmTitle")}</p>
                        <p className="mt-1 text-xs text-muted-foreground">{t("confirmBody")}</p>
                        <p className="mt-1 text-xs text-muted-foreground">
                          {t("confirmRefund", {
                            date: new Date(subscription.periodEnd).toLocaleDateString(),
                          })}
                        </p>
                        <div className="mt-3 flex gap-2">
                          <button
                            type="button"
                            disabled={busyId === subscription.id}
                            onClick={() => void cancel(subscription)}
                            className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground disabled:opacity-50 hover:bg-muted"
                          >
                            {busyId === subscription.id ? t("working") : t("confirmAction")}
                          </button>
                          <button
                            type="button"
                            onClick={() => setConfirmingId(null)}
                            className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-muted-foreground hover:bg-muted"
                          >
                            {t("keepIt")}
                          </button>
                        </div>
                      </div>
                    ) : (
                      <button
                        type="button"
                        disabled={busyId !== null}
                        onClick={() => setConfirmingId(subscription.id)}
                        className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground disabled:opacity-50 hover:bg-muted"
                      >
                        {t("cancelAction")}
                      </button>
                    )}
                  </div>
                )}
              </li>
            )
          })}
        </ul>
      )}

      {cancellable.length === 0 && items !== null && items.length > 0 && (
        <p className="mt-3 text-xs text-muted-foreground">{t("nothingToCancel")}</p>
      )}
    </section>
  )
}