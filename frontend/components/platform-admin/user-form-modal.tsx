"use client"

import { useEffect, useState } from "react"
import { Loader2, X } from "lucide-react"
import { useTranslations } from "next-intl"
import { platformAdminApi, type UserSummary, type UserCreatePayload, type UserUpdatePayload } from "@/lib/platform-admin-api"

const ROLES = [
  "STUDENT",
  "TEACHER",
  "PARENT",
  "OTHER_LEARNER",
  "ADMIN",
  "INSTITUTION_ADMIN",
  "PROVIDER_ADMIN",
  "NATIONAL_ADMIN",
  "REGIONAL_ADMIN",
  "DISTRICT_ADMIN",
] as const

type UserFormValues = {
  firstName: string
  lastName: string
  email: string
  phone: string
  password: string
  role: string
  institutionId: string
  isActive: boolean
  isEmailVerified?: boolean
  isPhoneVerified?: boolean
}

/**
 * Audit B-02: these roles resolve their authority from an organization membership. Without an
 * institution the account logs in but every scoped API call fails, so the platform could not
 * provision a usable institution/provider administrator.
 */
const ORG_SCOPED_ROLES = new Set([
  "INSTITUTION_ADMIN",
  "PROVIDER_ADMIN",
  "PROVIDER_STAFF",
  "TEACHER",
  "INSTRUCTOR",
  "NATIONAL_ADMIN",
  "REGIONAL_ADMIN",
  "DISTRICT_ADMIN",
])

const EMPTY_FORM = {
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  password: "",
  role: "STUDENT",
  institutionId: "",
  isActive: true,
  isEmailVerified: false,
  isPhoneVerified: false,
}

interface UserFormModalProps {
  open: boolean
  user?: { id: string; firstName: string; lastName: string; email: string; phone: string; role: string; isActive: boolean; isEmailVerified: boolean; isPhoneVerified: boolean } | null
  onClose: () => void
  onSaved: (message: string) => void
}

export function UserFormModal({ open, user, onClose, onSaved }: UserFormModalProps) {
  const t = useTranslations("platformAdmin")
  const tc = useTranslations("common")
  const isEdit = Boolean(user)
  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    password: "",
    role: "STUDENT",
    institutionId: "",
    isActive: true,
    isEmailVerified: false,
    isPhoneVerified: false,
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // Audit B-02: the organization is now selectable at provisioning time.
  const [institutions, setInstitutions] = useState<{ id: string; name: string; type?: string }[]>([])
  const [institutionsError, setInstitutionsError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    let cancelled = false
    platformAdminApi
      .listInstitutions(0, 200)
      .then((page) => {
        if (cancelled) return
        setInstitutions(page?.content ?? [])
        setInstitutionsError(null)
      })
      .catch((e) => {
        if (cancelled) return
        setInstitutions([])
        setInstitutionsError(e?.message || t("userForm.institutionsLoadFailed"))
      })
    return () => {
      cancelled = true
    }
  }, [open, t])

  useEffect(() => {
    if (!open) return
    setError(null)
    if (user) {
      setForm({
        firstName: user.firstName ?? "",
        lastName: user.lastName ?? "",
        email: user.email ?? "",
        phone: user.phone ?? "",
        password: "",
        role: user.role ?? "STUDENT",
        institutionId: (user as any).institutionId ?? "",
        isActive: user.isActive ?? true,
        isEmailVerified: (user as any).isEmailVerified ?? false,
        isPhoneVerified: (user as any).isPhoneVerified ?? false,
      })
    } else {
      setForm({
        firstName: "",
        lastName: "",
        email: "",
        phone: "",
        password: "",
        role: "STUDENT",
        institutionId: "",
        isActive: true,
        isEmailVerified: false,
        isPhoneVerified: false,
      })
    }
  }, [open, user])

  if (!open) return null

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const firstName = form.firstName.trim()
    const lastName = form.lastName.trim()
    const email = form.email.trim()
    if (!firstName || !lastName || !email) {
      setError(t("userForm.errNamesEmail"))
      return
    }
    if (!isEdit && !form.password) {
      setError(t("userForm.errPasswordRequired"))
      return
    }
    if (form.password && form.password.length < 8) {
      setError(t("userForm.errPasswordMin"))
      return
    }
    // Audit B-02: block submission rather than creating an account that cannot resolve a scope.
    if (!isEdit && ORG_SCOPED_ROLES.has(form.role) && !form.institutionId) {
      setError(t("userForm.errInstitutionRequired"))
      return
    }
    setSaving(true)
    setError(null)
    try {
      const payload = {
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        phone: form.phone?.trim() || undefined,
        password: form.password || undefined,
        role: form.role,
        institutionId: form.institutionId || undefined,
        isActive: form.isActive,
        isEmailVerified: form.isEmailVerified,
        isPhoneVerified: false,
      }
      if (isEdit && user) {
        await platformAdminApi.updateUser(user.id, payload)
        onSaved(t("userForm.updatedOk"))
      } else {
        await platformAdminApi.createUser(payload)
        onSaved(t("userForm.createdOk"))
      }
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : t("userForm.saveFailed"))
    } finally {
      setSaving(false)
    }
  }

  const inputClass =
    "w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
  const labelClass = "block text-xs font-medium text-muted-foreground mb-1.5"
  const selectClass = "w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50" onClick={saving ? undefined : onClose} />
      <div className="relative max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-border bg-card p-6 shadow-lg">
        <div className="mb-5 flex items-start justify-between">
          <div>
            <h2 className="text-lg font-bold text-foreground">{isEdit ? t("userForm.titleEdit") : t("userForm.titleAdd")}</h2>
            <p className="mt-0.5 text-xs text-muted-foreground">
              {isEdit ? t("userForm.subEdit") : t("userForm.subAdd")}
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
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>{t("userForm.labelFirstName")}</label>
              <input type="text" value={form.firstName} onChange={(e) => setForm((p) => ({ ...p, firstName: e.target.value }))} required minLength={1} maxLength={100} placeholder="John" className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>{t("userForm.labelLastName")}</label>
              <input type="text" value={form.lastName} onChange={(e) => setForm((p) => ({ ...p, lastName: e.target.value }))} required minLength={1} maxLength={100} placeholder="Doe" className={inputClass} />
            </div>
          </div>

          <div>
            <label className={labelClass}>{t("userForm.labelEmail")}</label>
            <input type="email" value={form.email} onChange={(e) => setForm((p) => ({ ...p, email: e.target.value }))} required maxLength={255} placeholder="user@example.com" className={inputClass} />
          </div>

          <div>
            <label className={labelClass}>{t("userForm.labelPhone")}</label>
            <input type="text" value={form.phone} onChange={(e) => setForm((p) => ({ ...p, phone: e.target.value }))} maxLength={50} placeholder="+255 ..." className={inputClass} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>{t("userForm.labelRole")}</label>
              <select value={form.role} onChange={(e) => setForm((p) => ({ ...p, role: e.target.value }))} className={selectClass}>
                {ROLES.map((r) => (
                  <option key={r} value={r}>{r}</option>
                ))}
              </select>
            </div>
            <div>
              <label className={labelClass}>{t("userForm.labelStatus")}</label>
              <select value={form.isActive ? "active" : "inactive"} onChange={(e) => setForm((p) => ({ ...p, isActive: e.target.value === "active" }))} className={selectClass}>
                <option value="true">{t("userForm.optActive")}</option>
                <option value="false">{t("userForm.optInactive")}</option>
              </select>
            </div>
          </div>

          {/* Audit B-02: the organization an org-scoped account belongs to. */}
          <div>
            <label className={labelClass} htmlFor="user-form-institution">
              {t("userForm.labelInstitution")}
              {ORG_SCOPED_ROLES.has(form.role) && (
                <span className="ml-1 text-destructive">*</span>
              )}
            </label>
            {institutionsError ? (
              <p role="alert" className="rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-xs text-destructive">
                {institutionsError}
              </p>
            ) : (
              <select
                id="user-form-institution"
                value={form.institutionId}
                onChange={(e) => setForm((p) => ({ ...p, institutionId: e.target.value }))}
                required={ORG_SCOPED_ROLES.has(form.role)}
                disabled={institutions.length === 0}
                className={selectClass}
              >
                <option value="">
                  {institutions.length === 0
                    ? t("userForm.noInstitutions")
                    : t("userForm.selectInstitution")}
                </option>
                {institutions.map((i) => (
                  <option key={i.id} value={i.id}>
                    {i.name}{i.type ? ` — ${i.type}` : ""}
                  </option>
                ))}
              </select>
            )}
            <p className="mt-1 text-xs text-muted-foreground">{t("userForm.hintInstitution")}</p>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>{t("userForm.labelEmailVerified")}</label>
              <select value={form.isEmailVerified === true ? "true" : "false"} onChange={(e) => setForm((p) => ({ ...p, isEmailVerified: e.target.value === "true" }))} className={selectClass}>
                <option value="true">{t("userForm.optYes")}</option>
                <option value="false">{t("userForm.optNo")}</option>
              </select>
            </div>
            <div>
              <label className={labelClass}>{t("userForm.labelPhoneVerified")}</label>
              <select value={form.isPhoneVerified === true ? "true" : "false"} onChange={(e) => setForm((p) => ({ ...p, isPhoneVerified: e.target.value === "true" }))} className={selectClass}>
                <option value="true">{t("userForm.optYes")}</option>
                <option value="false">{t("userForm.optNo")}</option>
              </select>
            </div>
          </div>

          {!isEdit && (
            <div>
              <label className={labelClass}>{t("userForm.labelPassword")}</label>
              <input type="password" value={form.password} onChange={(e) => setForm((p) => ({ ...p, password: e.target.value }))} required minLength={8} placeholder={t("userForm.phPassword")} className={inputClass} />
            </div>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} disabled={saving} className="rounded-lg border border-border px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-50">
              {tc("cancel")}
            </button>
            <button type="submit" disabled={saving} className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
              {saving && <Loader2 className="size-4 animate-spin" />}
              {isEdit ? t("userForm.btnSave") : t("userForm.btnCreate")}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
