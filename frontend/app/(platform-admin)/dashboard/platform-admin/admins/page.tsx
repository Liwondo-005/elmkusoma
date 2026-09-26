"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { Shield, Loader2, Search, KeyRound, Save, AlertCircle, RefreshCw } from "lucide-react"
import { platformAdminApi, type AdminAccount } from "@/lib/platform-admin-api"

export default function AdminsPage() {
  const t = useTranslations("platformAdmin");
  const ts = useTranslations("status");
  const [admins, setAdmins] = useState<AdminAccount[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [selected, setSelected] = useState<AdminAccount | null>(null)
  const [permDraft, setPermDraft] = useState("")
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      setAdmins(await platformAdminApi.listAdmins())
    } catch (e: any) {
      setError(e.message || t("admins.failedToLoadAdmins"))
      setAdmins([])
    } finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const filtered = admins.filter(a =>
    `${a.fullName} ${a.email} ${a.role}`.toLowerCase().includes(search.toLowerCase())
  )

  const openMatrix = async (a: AdminAccount) => {
    setSelected(a)
    try {
      const perms = await platformAdminApi.getRolePermissions(a.userId)
      setPermDraft(perms.join("\n"))
    } catch {
      setPermDraft((a.permissions ?? []).join("\n"))
    }
  }

  const saveMatrix = async () => {
    if (!selected) return
    setSaving(true); setError(null)
    try {
      const permissions = permDraft.split("\n").map(s => s.trim()).filter(Boolean)
      const updated = await platformAdminApi.updateRolePermissions(selected.userId, permissions)
      setSelected({ ...selected, permissions: updated })
      setAdmins(prev => prev.map(a => (a.userId === selected.userId ? { ...a, permissions: updated } : a)))
    } catch (e: any) {
      setError(e.message || t("admins.failedToUpdatePermissions"))
    } finally { setSaving(false) }
  }

  if (loading) return (
    <div className="flex items-center justify-center py-32">
      <Loader2 className="size-8 animate-spin text-primary" />
    </div>
  )

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("admins.administratorAccounts")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("admins.adminRolesWithEditable")}</p>
        </div>
        <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> {t("admins.refresh")}</button>
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center gap-2">
          <AlertCircle className="size-4" />{error}
        </div>
      )}

      <div className="flex items-center gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder={t("admins.searchAdmins")}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full rounded-xl border border-border bg-card py-2 pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-primary/20"
          />
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2 rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
          {filtered.length === 0 ? (
            <div className="p-12 text-center">
              <Shield className="mx-auto size-10 text-muted-foreground" />
              <p className="mt-4 text-sm font-medium text-foreground">{t("admins.noAdministratorsFound")}</p>
            </div>
          ) : (
            <table className="w-full">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">{t("admins.name")}</th>
                  <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">{t("admins.email")}</th>
                  <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">{t("admins.role")}</th>
                  <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">{t("admins.perms")}</th>
                  <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">{t("admins.status")}</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {filtered.map((admin) => (
                  <tr key={admin.userId} className={`hover:bg-muted/30 transition-colors ${selected?.userId === admin.userId ? "bg-primary/5" : ""}`}>
                    <td className="px-5 py-3.5 text-sm font-medium text-foreground">{admin.fullName}</td>
                    <td className="px-5 py-3.5 text-sm text-muted-foreground">{admin.email}</td>
                    <td className="px-5 py-3.5">
                      <span className="inline-flex rounded-full bg-purple-100 px-2.5 py-0.5 text-xs font-semibold text-purple-700">{admin.role}</span>
                    </td>
                    <td className="px-5 py-3.5 text-xs tabular-nums text-muted-foreground">{admin.permissions?.length ?? 0}</td>
                    <td className="px-5 py-3.5">
                      <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${admin.isActive ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"}`}>
                        {admin.isActive ? ts("active") : ts("inactive")}
                      </span>
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <button onClick={() => openMatrix(admin)}
                        className="inline-flex items-center gap-1 rounded-lg border border-border bg-background px-2.5 py-1 text-xs font-semibold hover:bg-muted">
                        <KeyRound className="size-3" /> {t("admins.matrix")}</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <h2 className="flex items-center gap-2 text-sm font-bold text-foreground"><KeyRound className="size-4" /> {t("admins.permissionMatrix")}</h2>
          {!selected ? (
            <p className="mt-3 text-sm text-muted-foreground">{t("admins.selectAnAdminTo")}</p>
          ) : (
            <>
              <p className="mt-2 text-xs text-muted-foreground">{selected.fullName} · {selected.email}</p>
              <textarea
                value={permDraft}
                onChange={(e) => setPermDraft(e.target.value)}
                rows={14}
                spellCheck={false}
                className="mt-3 w-full rounded-xl border border-border bg-background p-3 font-mono text-xs outline-none focus:ring-2 focus:ring-primary/20"
              />
              <button onClick={saveMatrix} disabled={saving}
                className="mt-3 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
                {saving ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />} {t("admins.savePermissions")}</button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
