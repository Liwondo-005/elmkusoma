"use client"

import { useEffect, useState } from "react"
import { Loader2, X } from "lucide-react"
import { platformAdminApi, type AdminAccount } from "@/lib/platform-admin-api"

interface AdminFormModalProps {
  open: boolean
  admin?: AdminAccount | null
  onClose: () => void
  onSaved: (message: string) => void
}

export function AdminFormModal({ open, admin, onClose, onSaved }: AdminFormModalProps) {
  const isEdit = Boolean(admin)
  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    password: "",
    role: "ADMIN",
    isActive: true,
    institutionId: undefined as string | undefined,
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [institutions, setInstitutions] = useState<{ id: string; name: string; code: string }[]>([])

  useEffect(() => {
    if (!open) return
    setError(null)
    if (admin) {
      setForm({
        firstName: admin.fullName?.split(" ")[0] ?? "",
        lastName: admin.fullName?.split(" ").slice(1).join(" ") ?? "",
        email: admin.email ?? "",
        phone: "",
        password: "",
        role: admin.role ?? "ADMIN",
        isActive: admin.isActive ?? true,
        institutionId: admin.institutionId ?? undefined,
      })
    } else {
      setForm({
        firstName: "",
        lastName: "",
        email: "",
        phone: "",
        password: "",
        role: "ADMIN",
        isActive: true,
        institutionId: undefined,
      })
    }
  }, [open, admin])

  useEffect(() => {
    platformAdminApi.listInstitutions().then((resp) => setInstitutions(resp.content || []))
  }, [])

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
      setError("Password is required for new admins")
      return
    }
    if (form.password && form.password.length < 8) {
      setError("Password must be at least 8 characters")
      return
    }
    setSaving(true)
    setError(null)
    try {
      if (isEdit && admin) {
        const updatePayload: any = {
          firstName,
          lastName,
          phone: form.phone?.trim() || undefined,
          role: form.role,
          isActive: form.isActive,
          institutionId: form.institutionId,
        }
        if (form.password) {
          updatePayload.password = form.password
        }
        await platformAdminApi.updateAdmin(admin.userId, updatePayload)
        onSaved("Admin updated successfully")
      } else {
        await platformAdminApi.createAdmin({
          firstName,
          lastName,
          email,
          phone: form.phone?.trim() || undefined,
          password: form.password,
          role: form.role,
          institutionId: form.institutionId,
        })
        onSaved("Admin created successfully")
      }
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save admin")
    } finally {
      setSaving(false)
    }
  }

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50" onClick={saving ? undefined : onClose} />
      <div className="relative max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-border bg-card p-6 shadow-lg">
        <div className="mb-5 flex items-start justify-between">
          <div>
            <h2 className="text-lg font-bold text-foreground">{isEdit ? "Edit Admin" : "Add Admin"}</h2>
            <p className="mt-0.5 text-xs text-muted-foreground">
              {isEdit ? "Update the admin details below." : "Create a new admin account."}
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
              <label className="block text-xs font-medium text-muted-foreground mb-1.5">First Name *</label>
              <input type="text" value={form.firstName} onChange={(e) => setForm((p) => ({ ...p, firstName: e.target.value }))} required minLength={1} maxLength={100} placeholder="John" className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
            </div>
            <div>
              <label className="block text-xs font-medium text-muted-foreground mb-1.5">Last Name *</label>
              <input type="text" value={form.lastName} onChange={(e) => setForm((p) => ({ ...p, lastName: e.target.value }))} required minLength={1} maxLength={100} placeholder="Doe" className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-muted-foreground mb-1.5">Email *</label>
            <input type="email" value={form.email} onChange={(e) => setForm((p) => ({ ...p, email: e.target.value }))} required maxLength={255} placeholder="admin@example.com" className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
          </div>

          <div>
            <label className="block text-xs font-medium text-muted-foreground mb-1.5">Phone</label>
            <input type="text" value={form.phone} onChange={(e) => setForm((p) => ({ ...p, phone: e.target.value }))} maxLength={50} placeholder="+255 ..." className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-xs font-medium text-muted-foreground mb-1.5">Role *</label>
              <select value={form.role} onChange={(e) => setForm((p) => ({ ...p, role: e.target.value }))} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring">
                {["ADMIN", "INSTITUTION_ADMIN", "NATIONAL_ADMIN", "REGIONAL_ADMIN", "DISTRICT_ADMIN"].map((r) => (
                  <option key={r} value={r}>{r}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-muted-foreground mb-1.5">Institution *</label>
              <select value={form.institutionId ?? ""} onChange={(e) => setForm((p) => ({ ...p, institutionId: e.target.value === "" ? undefined : e.target.value }))} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring">
                <option value="">-- Select Institution --</option>
                {institutions.map((inst) => (
                  <option key={inst.id} value={inst.id}>{inst.name} ({inst.code})</option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-xs font-medium text-muted-foreground mb-1.5">Status</label>
              <select value={form.isActive ? "true" : "false"} onChange={(e) => setForm((p) => ({ ...p, isActive: e.target.value === "true" }))} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring">
                <option value="true">Active</option>
                <option value="false">Inactive</option>
              </select>
            </div>
          </div>

          {!isEdit && (
            <div>
              <label className="block text-xs font-medium text-muted-foreground mb-1.5">Password *</label>
              <input type="password" value={form.password} onChange={(e) => setForm((p) => ({ ...p, password: e.target.value }))} required minLength={8} placeholder="Min 8 characters" className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
            </div>
          )}

          {isEdit && (
            <div>
              <label className="block text-xs font-medium text-muted-foreground mb-1.5">New Password (optional)</label>
              <input type="password" value={form.password} onChange={(e) => setForm((p) => ({ ...p, password: e.target.value }))} minLength={8} placeholder="Leave blank to keep current password" className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
            </div>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} disabled={saving} className="rounded-lg border border-border px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-50">
              Cancel
            </button>
            <button type="submit" disabled={saving} className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
              {saving && <Loader2 className="size-4 animate-spin" />}
              {isEdit ? "Save Changes" : "Create Admin"}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
