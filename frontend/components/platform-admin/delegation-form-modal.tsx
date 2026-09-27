"use client"

import { useEffect, useState } from "react"
import { Loader2, X } from "lucide-react"
import { platformAdminApi, type UserSummary } from "@/lib/platform-admin-api"

const FALLBACK_AUTHORITIES = [
  "PROVIDER_VERIFICATION", "INSTITUTION_REVIEW", "COMPLIANCE_REVIEW",
  "INCIDENT_MANAGEMENT", "PLATFORM_SUPPORT", "CONTENT_GOVERNANCE",
  "SERVICE_GOVERNANCE", "GENERAL_ADMIN",
]

const FALLBACK_PERMISSIONS = [
  "VIEW", "REVIEW", "APPROVE", "VERIFY", "REJECT", "SUSPEND", "REACTIVATE", "MANAGE",
]

export interface DelegationDraft {
  delegatorId: string
  delegateId: string
  authority: string
  permissions: string[]
  scope: string
  resourceIds: string
  startsAt: string
  expiresAt: string
  reason: string
  notes: string
  requiresApproval: boolean
}

interface DelegationFormModalProps {
  open: boolean
  onClose: () => void
  onSaved: (message: string) => void
}

export function DelegationFormModal({ open, onClose, onSaved }: DelegationFormModalProps) {
  const [authorities, setAuthorities] = useState<string[]>(FALLBACK_AUTHORITIES)
  const [permissionTokens, setPermissionTokens] = useState<string[]>(FALLBACK_PERMISSIONS)
  const [users, setUsers] = useState<UserSummary[]>([])
  const [usersLoading, setUsersLoading] = useState(false)
  const [form, setForm] = useState<DelegationDraft>({
    delegatorId: "", delegateId: "", authority: "PROVIDER_VERIFICATION",
    permissions: ["VIEW", "REVIEW"], scope: "PLATFORM", resourceIds: "",
    startsAt: "", expiresAt: "", reason: "", notes: "", requiresApproval: false,
  })
  const [confirming, setConfirming] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    setError(null); setConfirming(false)
    platformAdminApi.delegationVocabulary()
      .then((v) => {
        if (v.authorities?.length) setAuthorities(v.authorities)
        if (v.permissions?.length) setPermissionTokens(v.permissions)
      })
      .catch(() => {})
    setUsersLoading(true)
    platformAdminApi.listUsers(0, 200)
      .then((res) => setUsers(res.content ?? []))
      .catch(() => setUsers([]))
      .finally(() => setUsersLoading(false))
  }, [open ])

  if (!open) return null

  const set = (key: keyof DelegationDraft, value: string | boolean | string[]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const togglePermission = (token: string) =>
    setForm((prev) => ({
      ...prev,
      permissions: prev.permissions.includes(token)
        ? prev.permissions.filter((p) => p !== token)
        : [...prev.permissions, token],
    }))

  const userLabel = (u: UserSummary) =>
    `${u.firstName ?? ""} ${u.lastName ?? ""}`.trim() + ` · ${u.email}` + (u.role ? ` · ${u.role}` : "")

  const validate = (): string | null => {
    if (!form.delegatorId) return "Delegator is required"
    if (!form.delegateId) return "Delegate is required"
    if (form.delegatorId === form.delegateId) return "Delegator and delegate must be different users"
    if (form.permissions.length === 0) return "Select at least one permission"
    if (!form.reason.trim()) return "A reason is required"
    if (form.expiresAt && form.startsAt && form.expiresAt <= form.startsAt) return "Expiry must be after the start"
    return null
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const problem = validate()
    if (problem) { setError(problem); return }
    setError(null)
    if (!confirming) { setConfirming(true); return }
    setSaving(true)
    try {
      const resourceIds = form.resourceIds.split(/[\s,;]+/).map((s) => s.trim()).filter(Boolean)
      await platformAdminApi.createDelegation({
        delegatorId: form.delegatorId,
        delegateId: form.delegateId,
        authority: form.authority,
        permissions: JSON.stringify(form.permissions),
        scope: form.scope.trim() || "PLATFORM",
        resourceIds: resourceIds.length ? resourceIds : undefined,
        startsAt: form.startsAt || undefined,
        expiresAt: form.expiresAt || undefined,
        reason: form.reason.trim(),
        notes: form.notes.trim() || undefined,
        requiresApproval: form.requiresApproval,
      })
      onSaved(form.requiresApproval ? "Delegation submitted for approval" : "Delegation created and active")
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create delegation")
    } finally {
      setSaving(false)
    }
  }

  const inputClass =
    "w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
  const labelClass = "block text-xs font-medium text-muted-foreground mb-1.5"

  const delegator = users.find((u) => u.id === form.delegatorId)
  const delegate = users.find((u) => u.id === form.delegateId)

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50" onClick={saving ? undefined : onClose} />
      <div className="relative max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl border border-border bg-card p-6 shadow-lg">
        <div className="mb-5 flex items-start justify-between">
          <div>
            <h2 className="text-lg font-bold text-foreground">Create Delegation</h2>
            <p className="mt-0.5 text-xs text-muted-foreground">Grant scoped administrative authority to a platform officer.</p>
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
              <label className={labelClass}>Delegator (grants authority) *</label>
              <select value={form.delegatorId} onChange={(e) => set("delegatorId", e.target.value)} required className={inputClass}>
                <option value="">{usersLoading ? "Loading users…" : "Select delegator…"}</option>
                {users.map((u) => <option key={u.id} value={u.id}>{userLabel(u)}</option>)}
              </select>
            </div>
            <div>
              <label className={labelClass}>Delegate (receives authority) *</label>
              <select value={form.delegateId} onChange={(e) => set("delegateId", e.target.value)} required className={inputClass}>
                <option value="">{usersLoading ? "Loading users…" : "Select delegate…"}</option>
                {users.map((u) => <option key={u.id} value={u.id}>{userLabel(u)}</option>)}
              </select>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>Authority *</label>
              <select value={form.authority} onChange={(e) => set("authority", e.target.value)} className={inputClass}>
                {authorities.map((a) => <option key={a} value={a}>{a}</option>)}
              </select>
            </div>
            <div>
              <label className={labelClass}>Scope *</label>
              <input value={form.scope} onChange={(e) => set("scope", e.target.value)} placeholder="PLATFORM, REGION:Dar es Salaam, INSTITUTION:<id>" className={inputClass} />
            </div>
          </div>

          <div>
            <label className={labelClass}>Permissions *</label>
            <div className="flex flex-wrap gap-2">
              {permissionTokens.map((token) => (
                <button key={token} type="button" onClick={() => togglePermission(token)}
                  className={`rounded-full px-3 py-1 text-xs font-semibold ${form.permissions.includes(token) ? "bg-primary text-primary-foreground" : "border border-border text-muted-foreground hover:bg-muted"}`}>
                  {token}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className={labelClass}>Scoped resources (optional UUIDs, comma separated)</label>
            <input value={form.resourceIds} onChange={(e) => set("resourceIds", e.target.value)} placeholder="institution / provider ids…" className={inputClass} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>Starts at</label>
              <input type="datetime-local" value={form.startsAt} onChange={(e) => set("startsAt", e.target.value)} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>Expires at</label>
              <input type="datetime-local" value={form.expiresAt} onChange={(e) => set("expiresAt", e.target.value)} className={inputClass} />
            </div>
          </div>

          <div>
            <label className={labelClass}>Reason *</label>
            <input value={form.reason} onChange={(e) => set("reason", e.target.value)} maxLength={500} placeholder="Why is this authority being delegated?" className={inputClass} />
          </div>

          <div>
            <label className={labelClass}>Notes</label>
            <input value={form.notes} onChange={(e) => set("notes", e.target.value)} maxLength={1000} placeholder="Optional conditions…" className={inputClass} />
          </div>

          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={form.requiresApproval} onChange={(e) => set("requiresApproval", e.target.checked)} className="size-4 accent-primary" />
            Require approval before activation
          </label>

          {confirming && (
            <div className="rounded-xl border border-border bg-muted/40 p-4 text-sm">
              <p className="font-semibold text-foreground">Confirmation summary</p>
              <dl className="mt-2 space-y-1 text-xs text-muted-foreground">
                <div className="flex gap-2"><dt className="w-24 shrink-0 font-medium">Delegator:</dt><dd>{delegator ? userLabel(delegator) : form.delegatorId}</dd></div>
                <div className="flex gap-2"><dt className="w-24 shrink-0 font-medium">Delegate:</dt><dd>{delegate ? userLabel(delegate) : form.delegateId}</dd></div>
                <div className="flex gap-2"><dt className="w-24 shrink-0 font-medium">Authority:</dt><dd>{form.authority}</dd></div>
                <div className="flex gap-2"><dt className="w-24 shrink-0 font-medium">Scope:</dt><dd>{form.scope || "PLATFORM"}</dd></div>
                <div className="flex gap-2"><dt className="w-24 shrink-0 font-medium">Permissions:</dt><dd>{form.permissions.join(", ")}</dd></div>
                <div className="flex gap-2"><dt className="w-24 shrink-0 font-medium">Valid:</dt><dd>{form.startsAt || "now"} → {form.expiresAt || "no expiry"}</dd></div>
                <div className="flex gap-2"><dt className="w-24 shrink-0 font-medium">Reason:</dt><dd>{form.reason}</dd></div>
              </dl>
            </div>
          )}

          <div className="flex justify-end gap-3 pt-2">
            {confirming && (
              <button type="button" onClick={() => setConfirming(false)} disabled={saving} className="rounded-lg border border-border px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-50">
                Back
              </button>
            )}
            <button type="button" onClick={onClose} disabled={saving} className="rounded-lg border border-border px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-50">
              Cancel
            </button>
            <button type="submit" disabled={saving} className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
              {saving && <Loader2 className="size-4 animate-spin" />}
              {confirming ? "Confirm & Create" : "Review"}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
