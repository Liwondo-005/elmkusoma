"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { Loader2, X } from "lucide-react"
import { platformAdminApi, INSTITUTION_TYPES, type InstitutionFormPayload, type InstitutionSummary } from "@/lib/platform-admin-api"

interface InstitutionFormModalProps {
  open: boolean
  institution?: InstitutionSummary | null
  onClose: () => void
  onSaved: (message: string) => void
}

const EMPTY_FORM: InstitutionFormPayload = {
  name: "",
  type: "SCHOOL",
  description: "",
  logoUrl: "",
  website: "",
  email: "",
  phone: "",
  address: "",
  city: "",
  country: "Tanzania",
}

export function InstitutionFormModal({ open, institution, onClose, onSaved }: InstitutionFormModalProps) {
  const t = useTranslations("platformAdmin")
  const tc = useTranslations("common")
  const isEdit = Boolean(institution)
  const [form, setForm] = useState<InstitutionFormPayload>(EMPTY_FORM)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    setError(null)
    if (institution) {
      setForm({
        name: institution.name ?? "",
        type: institution.type ?? "SCHOOL",
        description: (institution as { description?: string }).description ?? "",
        logoUrl: (institution as { logoUrl?: string }).logoUrl ?? "",
        website: (institution as { website?: string }).website ?? "",
        email: (institution as { email?: string }).email ?? "",
        phone: (institution as { phone?: string }).phone ?? "",
        address: (institution as { address?: string }).address ?? "",
        city: institution.city ?? "",
        country: (institution as { country?: string }).country ?? "Tanzania",
      })
    } else {
      setForm(EMPTY_FORM)
    }
  }, [open, institution])

  if (!open) return null

  const set = (key: keyof InstitutionFormPayload) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const name = form.name.trim()
    if (!name) {
      setError(t("institutionForm.nameRequired"))
      return
    }
    setSaving(true)
    setError(null)
    try {
      const payload: InstitutionFormPayload = {
        ...form,
        name,
        description: form.description?.trim() || undefined,
        logoUrl: form.logoUrl?.trim() || undefined,
        website: form.website?.trim() || undefined,
        email: form.email?.trim() || undefined,
        phone: form.phone?.trim() || undefined,
        address: form.address?.trim() || undefined,
        city: form.city?.trim() || undefined,
        country: form.country?.trim() || undefined,
      }
      if (isEdit && institution) {
        await platformAdminApi.updateInstitution(institution.id, payload)
        onSaved(t("institutionForm.updatedOk"))
      } else {
        await platformAdminApi.createInstitution(payload)
        onSaved(t("institutionForm.createdOk"))
      }
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : t("institutionForm.saveFailed"))
    } finally {
      setSaving(false)
    }
  }

  const inputClass =
    "w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
  const labelClass = "block text-xs font-medium text-muted-foreground mb-1.5"

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50" onClick={saving ? undefined : onClose} />
      <div className="relative max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-border bg-card p-6 shadow-lg">
        <div className="mb-5 flex items-start justify-between">
          <div>
            <h2 className="text-lg font-bold text-foreground">{isEdit ? t("institutionForm.titleEdit") : t("institutionForm.titleAdd")}</h2>
            <p className="mt-0.5 text-xs text-muted-foreground">
              {isEdit ? t("institutionForm.subtitleEdit") : t("institutionForm.subtitleAdd")}
            </p>
          </div>
          <button type="button" onClick={onClose} disabled={saving} className="rounded-lg p-1 text-muted-foreground hover:text-foreground disabled:opacity-50" aria-label={tc("close")}>
            <X className="size-5" />
          </button>
        </div>

        {error && (
          <div className="mb-4 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">{error}</div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className={labelClass}>{t("institutionForm.labelName")}</label>
            <input type="text" value={form.name} onChange={set("name")} required minLength={2} maxLength={255} placeholder={t("institutionForm.phName")} className={inputClass} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>{t("institutionForm.labelType")}</label>
              <select value={form.type} onChange={set("type")} className={inputClass}>
                {INSTITUTION_TYPES.map((it) => (
                  <option key={it} value={it}>{it.replace(/_/g, " ")}</option>
                ))}
              </select>
            </div>
            <div>
              <label className={labelClass}>{t("institutionForm.labelCity")}</label>
              <input type="text" value={form.city} onChange={set("city")} maxLength={100} placeholder={t("institutionForm.phCity")} className={inputClass} />
            </div>
          </div>

          <div>
            <label className={labelClass}>{t("institutionForm.labelDescription")}</label>
            <textarea value={form.description} onChange={set("description")} rows={3} maxLength={1000} placeholder={t("institutionForm.phDescription")} className={inputClass} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>{t("institutionForm.labelEmail")}</label>
              <input type="email" value={form.email} onChange={set("email")} maxLength={255} placeholder="info@school.tz" className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>{t("institutionForm.labelPhone")}</label>
              <input type="text" value={form.phone} onChange={set("phone")} maxLength={50} placeholder="+255 ..." className={inputClass} />
            </div>
          </div>

          <div>
            <label className={labelClass}>{t("institutionForm.labelWebsite")}</label>
            <input type="text" value={form.website} onChange={set("website")} maxLength={255} placeholder="https://..." className={inputClass} />
          </div>

          <div>
            <label className={labelClass}>{t("institutionForm.labelAddress")}</label>
            <input type="text" value={form.address} onChange={set("address")} maxLength={500} placeholder={t("institutionForm.phAddress")} className={inputClass} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>{t("institutionForm.labelCountry")}</label>
              <input type="text" value={form.country} onChange={set("country")} maxLength={100} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>{t("institutionForm.labelLogoUrl")}</label>
              <input type="text" value={form.logoUrl} onChange={set("logoUrl")} maxLength={500} placeholder="https://..." className={inputClass} />
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} disabled={saving} className="rounded-lg border border-border px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-50">
              {tc("cancel")}
            </button>
            <button type="submit" disabled={saving} className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
              {saving && <Loader2 className="size-4 animate-spin" />}
              {isEdit ? t("institutionForm.saveChanges") : t("institutionForm.createInstitution")}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
