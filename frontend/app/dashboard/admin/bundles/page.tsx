"use client"

import { useCallback, useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import {
  listInstitutionBundles,
  publishBundle,
  suspendBundle,
  type Bundle,
} from "@/lib/bundle-api"

/**
 * Authoring bundles for this institution.
 *
 * <p>Everything the server enforces is reflected here so the UI does not pretend to be the rule:
 * a bundle arrives as a DRAFT, only a draft can be published, and publication is refused without
 * a duration or without items. The buttons are shaped by that, but the authority is the server -
 * hiding a button is presentation, and {@code BundleAdminController} refuses the call regardless.
 *
 * <p>No price field exists anywhere on this page. A bundle's price comes from the approved
 * pricing policy at checkout, so accepting one here would create a second, ungoverned way to
 * change what a learner pays.
 */
export default function AdminBundlesPage() {
  const t = useTranslations("bundlesAdmin")
  const [bundles, setBundles] = useState<Bundle[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setBundles(await listInstitutionBundles())
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

  async function act(bundle: Bundle, action: "publish" | "suspend") {
    setError(null)
    setNotice(null)
    setBusyId(bundle.id)
    try {
      if (action === "publish") {
        await publishBundle(bundle.id)
        setNotice(t("published", { name: bundle.name }))
      } else {
        await suspendBundle(bundle.id)
        setNotice(t("suspended", { name: bundle.name }))
      }
      await load()
    } catch (e) {
      // The server's refusal is the message worth showing - "a bundle needs an access
      // duration" is more useful than anything invented here.
      setError(e instanceof Error ? e.message : t("actionFailed"))
    } finally {
      setBusyId(null)
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

      {notice && !error && (
        <p className="text-sm text-muted-foreground">{notice}</p>
      )}

      {!error && (bundles?.length ?? 0) === 0 && (
        <section className="rounded-2xl border border-dashed border-border bg-card p-5">
          <h2 className="text-base font-semibold text-foreground">{t("emptyTitle")}</h2>
          <p className="mt-1 text-sm text-muted-foreground">{t("emptyBody")}</p>
        </section>
      )}

      {!error && (bundles?.length ?? 0) > 0 && (
        <ul className="space-y-3">
          {bundles!.map((bundle) => (
            <li key={bundle.id} className="rounded-2xl border border-border bg-card p-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h2 className="text-base font-semibold text-foreground">{bundle.name}</h2>
                  {bundle.description && (
                    <p className="mt-1 text-sm text-muted-foreground">{bundle.description}</p>
                  )}
                  <p className="mt-1 text-xs text-muted-foreground">
                    {t("serviceType")}: {bundle.serviceType} &middot;{" "}
                    {t("duration")}:{" "}
                    {bundle.accessDays
                      ? t("days", { count: bundle.accessDays })
                      : t("durationUnset")}
                  </p>
                </div>
                <span className="rounded-full bg-muted px-3 py-1 text-xs text-muted-foreground">
                  {t(`status.${bundle.status}`)}
                </span>
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                {bundle.status === "DRAFT" && (
                  <button
                    type="button"
                    disabled={busyId === bundle.id}
                    onClick={() => void act(bundle, "publish")}
                    className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground disabled:opacity-50 hover:bg-muted"
                  >
                    {busyId === bundle.id ? t("working") : t("publish")}
                  </button>
                )}
                {bundle.status === "PUBLISHED" && (
                  <button
                    type="button"
                    disabled={busyId === bundle.id}
                    onClick={() => void act(bundle, "suspend")}
                    className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground disabled:opacity-50 hover:bg-muted"
                  >
                    {busyId === bundle.id ? t("working") : t("suspend")}
                  </button>
                )}
                {/* No delete. A bundle that has been bought cannot be unbought, and removing the
                    row would orphan entitlements learners already hold. Suspension is the way to
                    stop it being sold. */}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}