"use client"

import { useEffect, useState } from "react"
import { Loader2, X } from "lucide-react"
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
  isActive: boolean
  isEmailVerified?: boolean
  isPhoneVerified?: boolean
}

const EMPTY_FORM = {
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  password: "",
  role: "STUDENT",
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
  const isEdit = Boolean(user)
  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    password: "",
    role: "STUDENT",
    isActive: true,
    isEmailVerified: false,
    isPhoneVerified: false,
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

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
      setError("First name, last name, and email are required")
      return
    }
    if (!isEdit && !form.password) {
      setError("Password is required for new users")
      return
    }
    if (form.password && form.password.length < 8) {
      setError("Password must be at least 8 characters")
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
        isActive: form.isActive,
        isEmailVerified: form.isEmailVerified,
        isPhoneVerified: false,
      }
      if (isEdit && user) {
        await platformAdminApi.updateUser(user.id, payload)
        onSaved("User updated successfully")
      } else {
        await platformAdminApi.createUser(payload)
        onSaved("User created successfully")
      }
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save user")
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
            <h2 className="text-lg font-bold text-foreground">{isEdit ? "Edit User" : "Add User"}</h2>
            <p className="mt-0.5 text-xs text-muted-foreground">
              {isEdit ? "Update the user details below." : "Register a new user on the platform."}
            </p>
          </div>
          <button type="button" onClick={onClose} disabled={saving} className="rounded-lg p-1 text-muted-foreground hover:text-foreground disabled:opacity-50" aria-label="Close">
            <X className="size-5" />
          </button>
        </div>

        {error && (
          <div className="mb-4 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">{error}</div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>First Name *</label>
              <input type="text" value={form.firstName} onChange={(e) => setForm((p) => ({ ...p, firstName: e.target.value }))} required minLength={1} maxLength={100} placeholder="John" className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>Last Name *</label>
              <input type="text" value={form.lastName} onChange={(e) => setForm((p) => ({ ...p, lastName: e.target.value }))} required minLength={1} maxLength={100} placeholder="Doe" className={inputClass} />
            </div>
          </div>

          <div>
            <label className={labelClass}>Email *</label>
            <input type="email" value={form.email} onChange={(e) => setForm((p) => ({ ...p, email: e.target.value }))} required maxLength={255} placeholder="user@example.com" className={inputClass} />
          </div>

          <div>
            <label className={labelClass}>Phone</label>
            <input type="text" value={form.phone} onChange={(e) => setForm((p) => ({ ...p, phone: e.target.value }))} maxLength={50} placeholder="+255 ..." className={inputClass} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>Role *</label>
              <select value={form.role} onChange={(e) => setForm((p) => ({ ...p, role: e.target.value }))} className={selectClass}>
                {ROLES.map((r) => (
                  <option key={r} value={r}>{r}</option>
                ))}
              </select>
            </div>
            <div>
              <label className={labelClass}>Status</label>
              <select value={form.isActive ? "active" : "inactive"} onChange={(e) => setForm((p) => ({ ...p, isActive: e.target.value === "active" }))} className={selectClass}>
                <option value="true">Active</option>
                <option value="false">Inactive</option>
              </select>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>Email Verified</label>
              <select value={form.isEmailVerified === true ? "true" : "false"} onChange={(e) => setForm((p) => ({ ...p, isEmailVerified: e.target.value === "true" }))} className={selectClass}>
                <option value="true">Yes</option>
                <option value="false">No</option>
              </select>
            </div>
            <div>
              <label className={labelClass}>Phone Verified</label>
              <select value={form.isPhoneVerified === true ? "true" : "false"} onChange={(e) => setForm((p) => ({ ...p, isPhoneVerified: e.target.value === "true" }))} className={selectClass}>
                <option value="true">Yes</option>
                <option value="false">No</option>
              </select>
            </div>
          </div>

          {!isEdit && (
            <div>
              <label className={labelClass}>Password *</label>
              <input type="password" value={form.password} onChange={(e) => setForm((p) => ({ ...p, password: e.target.value }))} required minLength={8} placeholder="Min 8 characters" className={inputClass} />
            </div>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} disabled={saving} className="rounded-lg border border-border px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-50">
              Cancel
            </button>
            <button type="submit" disabled={saving} className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
              {saving && <Loader2 className="size-4 animate-spin" />}
              {isEdit ? "Save Changes" : "Create User"}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}