"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState } from "react"
import { Shield, Loader2, Plus, Trash2, X, Pencil, Check } from "lucide-react"
import { adminApi, getInstitutionId, type RoleResponse, type CreateRoleRequest } from "@/lib/api"

const PERMISSION_OPTIONS = [
  "STUDENTS_VIEW", "STUDENTS_MANAGE",
  "TEACHERS_VIEW", "TEACHERS_MANAGE",
  "PARENTS_VIEW", "PARENTS_MANAGE",
  "CERTIFICATES_VIEW", "CERTIFICATES_MANAGE",
  "GRADES_VIEW", "GRADES_MANAGE",
  "ATTENDANCE_VIEW", "ATTENDANCE_MANAGE",
  "ADMIN_SETTINGS", "ADMIN_ROLES", "ADMIN_IMPORT",
  "AUDIT_VIEW",
]

export default function AdminRolesPage() {
  const t = useTranslations("admin");
  const tc = useTranslations("common");
  const ts = useTranslations("status");
  const [roles, setRoles] = useState<RoleResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showNew, setShowNew] = useState(false)
  const [newName, setNewName] = useState("")
  const [newDisplayName, setNewDisplayName] = useState("")
  const [newDescription, setNewDescription] = useState("")
  const [newPermissions, setNewPermissions] = useState<string[]>([])
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState<string | null>(null)
  // B-57: editing an existing role. System roles stay read-only, matching the backend.
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editName, setEditName] = useState("")
  const [editDisplayName, setEditDisplayName] = useState("")
  const [editDescription, setEditDescription] = useState("")
  const [editPermissions, setEditPermissions] = useState<string[]>([])
  const [updating, setUpdating] = useState(false)

  const institutionId = getInstitutionId()

  useEffect(() => {
    if (!institutionId) {
      setError(t("roles.noInstitutionContextFound"))
      setLoading(false)
      return
    }
    adminApi
      .listRoles(institutionId)
      .then(setRoles)
      .catch((err) => setError(err instanceof Error ? err.message : t("roles.failedToLoadRoles")))
      .finally(() => setLoading(false))
  }, [institutionId])

  async function handleCreate() {
    if (!institutionId || !newName.trim() || !newDisplayName.trim()) return
    setSaving(true)
    try {
      const data: CreateRoleRequest = {
        name: newName.trim(),
        displayName: newDisplayName.trim(),
        description: newDescription || undefined,
        permissions: newPermissions.length > 0 ? newPermissions : undefined,
      }
      const created = await adminApi.createRole(institutionId, data)
      setRoles((prev) => [...prev, created])
      setShowNew(false)
      setNewName("")
      setNewDisplayName("")
      setNewDescription("")
      setNewPermissions([])
    } catch (err) {
      setError(err instanceof Error ? err.message : t("roles.failedToCreateRole"))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(roleId: string) {
    if (!institutionId) return
    setDeleting(roleId)
    try {
      await adminApi.deleteRole(institutionId, roleId)
      setRoles((prev) => prev.filter((r) => r.id !== roleId))
    } catch (err) {
      setError(err instanceof Error ? err.message : t("roles.failedToDeleteRole"))
    } finally {
      setDeleting(null)
    }
  }

  function startEdit(role: RoleResponse) {
    setEditingId((prev) => (prev === role.id ? null : role.id))
    setEditName(role.name)
    setEditDisplayName(role.displayName)
    setEditDescription(role.description ?? "")
    setEditPermissions(role.permissions)
    setError(null)
  }

  async function handleUpdate() {
    if (!institutionId || !editingId) return
    setUpdating(true)
    try {
      const updated = await adminApi.updateRole(institutionId, editingId, {
        name: editName.trim(),
        displayName: editDisplayName.trim(),
        description: editDescription.trim() || undefined,
        permissions: editPermissions,
      })
      setRoles((prev) => prev.map((r) => (r.id === updated.id ? updated : r)))
      setEditingId(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("roles.failedToUpdateRole"))
    } finally {
      setUpdating(false)
    }
  }

  function togglePermission(perm: string) {
    setNewPermissions((prev) => (prev.includes(perm) ? prev.filter((p) => p !== perm) : [...prev, perm]))
  }

  function toggleEditPermission(perm: string) {
    setEditPermissions((prev) => (prev.includes(perm) ? prev.filter((p) => p !== perm) : [...prev, perm]))
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("roles.roles")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("roles.manageCustomRolesAnd")}</p>
        </div>
        <button
          onClick={() => setShowNew(true)}
          className="flex h-9 items-center gap-2 rounded-lg bg-primary px-4 text-xs font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-3.5" /> {t("roles.newRole")}</button>
      </div>

      {loading && (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center">
          <p className="text-sm font-medium text-destructive">{error}</p>
        </div>
      )}

      {showNew && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h3 className="mb-4 text-sm font-semibold text-foreground">{t("roles.createRole")}</h3>
          <div className="space-y-3">
            <div className="grid gap-3 sm:grid-cols-2">
              <input
                type="text"
                placeholder={t("roles.nameEGClass")}
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
              <input
                type="text"
                placeholder={t("roles.displayNameEG")}
                value={newDisplayName}
                onChange={(e) => setNewDisplayName(e.target.value)}
                className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <input
              type="text"
              placeholder={t("roles.descriptionOptional")}
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
              className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
            />
            <div>
              <p className="mb-2 text-xs font-medium text-muted-foreground">{t("roles.permissions")}</p>
              <div className="flex flex-wrap gap-2">
                {PERMISSION_OPTIONS.map((perm) => (
                  <button
                    key={perm}
                    onClick={() => togglePermission(perm)}
                    className={`rounded-lg border px-3 py-1.5 text-xs font-medium transition-colors ${
                      newPermissions.includes(perm)
                        ? "border-primary bg-primary/10 text-primary"
                        : "border-border text-muted-foreground hover:bg-muted"
                    }`}
                  >
                    {perm}
                  </button>
                ))}
              </div>
            </div>
            <div className="flex gap-2">
              <button
                onClick={handleCreate}
                disabled={saving || !newName.trim() || !newDisplayName.trim()}
                className="flex h-9 items-center gap-2 rounded-lg bg-primary px-4 text-xs font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
              >
                {saving ? t("roles.creating") : t("roles.createRole")}
              </button>
              <button
                onClick={() => { setShowNew(false); setNewName(""); setNewDisplayName(""); setNewDescription(""); setNewPermissions([]) }}
                className="flex h-9 items-center gap-2 rounded-lg border border-border px-4 text-xs font-medium text-foreground hover:bg-muted"
              >
                <X className="size-3.5" /> {tc("cancel")}</button>
            </div>
          </div>
        </div>
      )}

      {!loading && !error && roles.length === 0 && (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <Shield className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">{t("roles.noCustomRoles")}</p>
          <p className="mt-1 text-sm text-muted-foreground">{t("roles.createRolesToManage")}</p>
        </div>
      )}

      {!loading && roles.length > 0 && (
        <div className="space-y-3">
          {roles.map((role) => (
            <div key={role.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              {editingId === role.id ? (
                <div className="space-y-3">
                  <div className="grid gap-3 sm:grid-cols-2">
                    <label className="block text-xs font-medium text-muted-foreground">
                      {t("roles.nameEGClass")}
                      <input
                        type="text"
                        value={editName}
                        onChange={(e) => setEditName(e.target.value)}
                        className="mt-1 h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:ring-ring"
                      />
                    </label>
                    <label className="block text-xs font-medium text-muted-foreground">
                      {t("roles.displayNameEG")}
                      <input
                        type="text"
                        value={editDisplayName}
                        onChange={(e) => setEditDisplayName(e.target.value)}
                        className="mt-1 h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:ring-ring"
                      />
                    </label>
                  </div>
                  <label className="block text-xs font-medium text-muted-foreground">
                    {t("roles.descriptionOptional")}
                    <input
                      type="text"
                      value={editDescription}
                      onChange={(e) => setEditDescription(e.target.value)}
                      className="mt-1 h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:ring-ring"
                    />
                  </label>
                  <div>
                    <p className="mb-2 text-xs font-medium text-muted-foreground">{t("roles.permissions")}</p>
                    <div className="flex flex-wrap gap-2">
                      {PERMISSION_OPTIONS.map((perm) => (
                        <button
                          key={perm}
                          type="button"
                          onClick={() => toggleEditPermission(perm)}
                          className={`rounded-lg border px-3 py-1.5 text-xs font-medium transition-colors ${
                            editPermissions.includes(perm)
                              ? "border-primary bg-primary/10 text-primary"
                              : "border-border text-muted-foreground hover:bg-muted"
                          }`}
                        >
                          {perm}
                        </button>
                      ))}
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={handleUpdate}
                      disabled={updating || !editName.trim() || !editDisplayName.trim()}
                      className="flex h-9 items-center gap-2 rounded-lg bg-primary px-4 text-xs font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                    >
                      {updating ? <Loader2 className="size-3.5 animate-spin" /> : <Check className="size-3.5" />}
                      {t("roles.saveChanges")}
                    </button>
                    <button
                      onClick={() => setEditingId(null)}
                      className="flex h-9 items-center gap-2 rounded-lg border border-border px-4 text-xs font-medium text-foreground hover:bg-muted"
                    >
                      <X className="size-3.5" /> {tc("cancel")}
                    </button>
                  </div>
                </div>
              ) : (
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-semibold text-foreground">{role.displayName}</span>
                      <span className="rounded bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                        {role.name}
                      </span>
                      {role.isSystemRole && (
                        <span className="rounded bg-primary/10 px-2 py-0.5 text-[10px] font-medium text-primary">
                          {t("roles.system")}</span>
                      )}
                      {!role.isActive && (
                        <span className="rounded bg-destructive/10 px-2 py-0.5 text-[10px] font-medium text-destructive">
                          {ts("inactive")}</span>
                      )}
                    </div>
                    {role.description && (
                      <p className="mt-1 text-xs text-muted-foreground">{role.description}</p>
                    )}
                    {role.permissions.length > 0 && (
                      <div className="mt-2 flex flex-wrap gap-1.5">
                        {role.permissions.map((perm) => (
                          <span key={perm} className="rounded bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                            {perm}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                  {!role.isSystemRole && (
                    <div className="flex shrink-0 gap-2">
                      <button
                        onClick={() => startEdit(role)}
                        disabled={deleting === role.id}
                        className="rounded-lg border border-border p-2 text-muted-foreground hover:bg-muted hover:text-foreground disabled:opacity-50"
                        aria-label={t("roles.editRole")}
                      >
                        <Pencil className="size-3.5" />
                      </button>
                      <button
                        onClick={() => handleDelete(role.id)}
                        disabled={deleting === role.id}
                        className="rounded-lg border border-border p-2 text-muted-foreground hover:bg-destructive/5 hover:text-destructive disabled:opacity-50"
                      >
                        <Trash2 className="size-3.5" />
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
