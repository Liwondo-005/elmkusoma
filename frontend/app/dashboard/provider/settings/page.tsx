"use client"
import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Loader2 } from "lucide-react"
import { adminApi, ApiRequestError } from "@/lib/api"

/**
 * Provider organization profile settings. Read and write both go through the
 * institution-scoped /v1/admin/org/profile API — the backend resolves the
 * caller's own institution server-side (client-supplied institution ids are
 * never trusted), so a PROVIDER_ADMIN can only ever edit its own organization.
 */
export default function SettingsPage() {
  const t = useTranslations("provider")
  const tn = useTranslations("nav")
  const [providerName, setProviderName] = useState("")
  const [contactEmail, setContactEmail] = useState("")
  const [contactPhone, setContactPhone] = useState("")
  const [address, setAddress] = useState("")
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      try {
        const institutionId = localStorage.getItem("elmkusoma_institution_id") || ""
        const profile = await adminApi.getOrgProfile(institutionId)
        if (cancelled) return
        setProviderName(profile.name || "")
        setContactEmail(profile.email || "")
        setContactPhone(profile.phone || "")
        setAddress(profile.address || "")
      } catch (e) {
        if (cancelled) return
        setError(e instanceof ApiRequestError ? e.message : t("settings.loadError"))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const handleSave = async () => {
    setSaving(true)
    setError(null)
    setSaved(false)
    try {
      const institutionId = localStorage.getItem("elmkusoma_institution_id") || ""
      const profile = await adminApi.updateOrgProfile(institutionId, {
        name: providerName.trim() || undefined,
        email: contactEmail.trim() || undefined,
        phone: contactPhone.trim() || undefined,
        address: address.trim() || undefined,
      })
      setProviderName(profile.name || providerName)
      setContactEmail(profile.email || contactEmail)
      setContactPhone(profile.phone || contactPhone)
      setAddress(profile.address || address)
      setSaved(true)
    } catch (e) {
      setError(e instanceof ApiRequestError ? e.message : t("settings.saveError"))
    } finally {
      setSaving(false)
    }
  }

  if (loading) return (
    <div className="flex items-center justify-center py-20">
      <Loader2 className="size-8 animate-spin text-muted-foreground" />
    </div>
  )

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">{tn("settings")}</h1>
        <p className="text-muted-foreground">{t("settings.subtitle")}</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive">{error}</div>
      )}
      {saved && (
        <div className="rounded-2xl border border-green-500/20 bg-green-500/5 px-6 py-4 text-sm text-green-700 dark:text-green-400">{t("settings.saveSuccess")}</div>
      )}

      <Card>
        <CardHeader><CardTitle>{t("settings.providerInfoTitle")}</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="text-sm font-medium text-foreground">{t("settings.providerNameLabel")}</label>
            <input
              value={providerName}
              onChange={(e) => setProviderName(e.target.value)}
              placeholder={t("settings.providerNamePlaceholder")}
              className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div>
            <label className="text-sm font-medium text-foreground">{t("settings.contactEmailLabel")}</label>
            <input
              type="email"
              value={contactEmail}
              onChange={(e) => setContactEmail(e.target.value)}
              placeholder="email@example.com"
              className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div>
            <label className="text-sm font-medium text-foreground">{t("settings.contactPhoneLabel")}</label>
            <input
              value={contactPhone}
              onChange={(e) => setContactPhone(e.target.value)}
              placeholder="+255..."
              className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div>
            <label className="text-sm font-medium text-foreground">{t("settings.addressLabel")}</label>
            <textarea
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              placeholder={t("settings.addressPlaceholder")}
              rows={3}
              className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <button
            type="button"
            onClick={handleSave}
            disabled={saving}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-60"
          >
            {saving && <Loader2 className="size-4 animate-spin" />}
            {t("settings.saveChanges")}
          </button>
        </CardContent>
      </Card>
    </div>
  )
}
