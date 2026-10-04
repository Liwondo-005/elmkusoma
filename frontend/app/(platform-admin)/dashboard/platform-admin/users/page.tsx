"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { useRouter } from "next/navigation"
import { Users, Loader2, Search, UserCheck, UserX, Plus, Pencil, Trash2, Key, RotateCcw, AlertCircle, RefreshCw, Mail, Shield, ShieldAlert } from "lucide-react"
import { platformAdminApi, type UserSummary, type PageResponse } from "@/lib/platform-admin-api"
import { UserFormModal } from "@/components/platform-admin/user-form-modal"

const ROLES = ["", "STUDENT", "TEACHER", "PARENT", "OTHER_LEARNER", "ADMIN", "INSTITUTION_ADMIN", "PROVIDER_ADMIN"]
const PAGE_SIZE = 20

export default function PlatformUsersPage() {
  const t = useTranslations("platformAdmin");
  const tc = useTranslations("common");
  const ts = useTranslations("status");
  const router = useRouter()
  const [data, setData] = useState<PageResponse<UserSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [role, setRole] = useState("")
  const [search, setSearch] = useState("")
  const [searchInput, setSearchInput] = useState("")
  const [toggling, setToggling] = useState<string | null>(null)
  const [confirmId, setConfirmId] = useState<string | null>(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<UserSummary | null>(null)
  const [resetTarget, setResetTarget] = useState<UserSummary | null>(null)
  const [resetPassword, setResetPassword] = useState("")
  const [resetConfirm, setResetConfirm] = useState("")
  const [resetting, setResetting] = useState(false)
  const [resetError, setResetError] = useState<string | null>(null)
const editingForModal = editing ? { 
  ...editing, 
  phone: editing.phone ?? "", 
  isEmailVerified: editing.isEmailVerified ?? false,
  isPhoneVerified: editing.isPhoneVerified ?? false,
} : null

  const flash = (msg: string) => {
    setSuccess(msg)
    setTimeout(() => setSuccess(null), 5000)
  }

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await platformAdminApi.listUsers(page, PAGE_SIZE, role || undefined, search || undefined)
      setData(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("users.failedToLoadUsers"))
    } finally {
      setLoading(false)
    }
  }, [page, role, search])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    setSearch(searchInput)
  }

  const handleToggleStatus = async (user: UserSummary) => {
    try {
      setToggling(user.id)
      await platformAdminApi.updateUserStatus(user.id, !user.isActive)
      setData((prev) => {
        if (!prev) return prev
        return {
          ...prev,
          content: prev.content.map((u) =>
            u.id === user.id ? { ...u, isActive: !u.isActive } : u
          ),
        }
      })
      flash(user.isActive ? t("users.userSuspended") : t("users.userActivated"))
    } catch (err) {
      setError(err instanceof Error ? err.message : t("users.failedToUpdateUser"))
    } finally {
      setToggling(null)
    }
  }

  const handleSaved = async (message: string) => {
    flash(message)
    await loadData()
  }

  const handleDelete = async (user: UserSummary) => {
    if (!window.confirm(`${t("users.confirmDelete")} "${user.firstName} ${user.lastName}"?`)) return
    setConfirmId(user.id)
    setError(null)
    try {
      await platformAdminApi.deleteUser(user.id)
      flash(`${t("users.userDeleted")} ${user.firstName} ${user.lastName}`)
      await loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : t("users.failedToDeleteUser"))
    } finally {
      setConfirmId(null)
    }
  }

  const openResetDialog = (user: UserSummary) => {
    setResetTarget(user)
    setResetPassword("")
    setResetConfirm("")
    setResetError(null)
  }

  const handleResetPassword = async () => {
    if (!resetTarget) return
    if (resetPassword.length < 8) {
      setResetError(t("users.passwordMinLength"))
      return
    }
    if (resetPassword !== resetConfirm) {
      setResetError(t("users.passwordsNoMatch"))
      return
    }
    setResetting(true)
    setResetError(null)
    try {
      await platformAdminApi.resetUserPassword(resetTarget.id, resetPassword)
      flash(`${t("users.passwordResetFor")} ${resetTarget.email}`)
      setResetTarget(null)
    } catch (err) {
      setResetError(err instanceof Error ? err.message : t("users.failedToResetPassword"))
    } finally {
      setResetting(false)
    }
  }

  const handleSendResetLink = async (user: UserSummary) => {
    try {
      await platformAdminApi.sendPasswordResetLink(user.id)
      flash(`${t("users.resetLinkSentTo")} ${user.email}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("users.failedToSendResetLink"))
    }
  }

  const formatDate = (d: string) => new Date(d).toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" })

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("users.users")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("users.managePlatformUsersAcross")}</p>
        </div>
        <button
          onClick={() => { setEditing(null); setModalOpen(true) }}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-4" /> {t("users.addUser")}
        </button>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <form onSubmit={handleSearch} className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder={t("users.searchByNameOr")}
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="w-full rounded-lg border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
          />
        </form>
        <select
          value={role}
          onChange={(e) => { setRole(e.target.value); setPage(0) }}
          className="rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
        >
          {ROLES.map((r) => (
            <option key={r} value={r}>{r || t("users.allRoles")}</option>
          ))}
        </select>
      </div>

      {success && (
        <div className="flex items-center gap-2 rounded-2xl border border-green-500/20 bg-green-500/5 px-6 py-4 text-sm text-green-600">
          <UserCheck className="size-4 shrink-0" /> {success}
        </div>
      )}

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive">{error}</div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-8 animate-spin text-muted-foreground" />
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <Users className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">{t("users.noUsersFound")}</p>
          <p className="mt-1 text-sm text-muted-foreground">
            {search ? t("users.tryADifferentSearch") : t("users.noUsersAvailable")}
          </p>
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("users.name")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("users.email")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("users.role")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("users.status")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("users.created")}</th>
                  <th className="px-5 py-3 text-right font-medium text-muted-foreground">{t("users.actions")}</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((user) => (
                  <tr key={user.id} className="border-b border-border last:border-0 hover:bg-muted/30 transition-colors">
                    <td className="px-5 py-3.5 font-medium text-foreground">
                      <button onClick={() => router.push(`/dashboard/platform-admin/users/${user.id}`)} className="hover:underline text-left">
                        {user.firstName} {user.lastName}
                      </button>
                    </td>
                    <td className="px-5 py-3.5 text-muted-foreground">{user.email}</td>
                    <td className="px-5 py-3.5">
                      <span className="inline-block rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
                        {user.role}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${user.isActive ? "bg-green-500/10 text-green-600" : "bg-red-500/10 text-red-600"}`}>
                        {user.isActive ? ts("active") : t("users.suspended")}
                      </span>
                    </td>
                    <td className="px-5 py-3.5 text-muted-foreground">{formatDate(user.createdAt)}</td>
                    <td className="px-5 py-3.5 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => { setEditing(user); setModalOpen(true) }}
                          className="inline-flex items-center gap-1 rounded-lg border border-border bg-background px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted"
                        >
                          <Pencil className="size-3" /> Edit
                        </button>
                        <button
                          onClick={() => openResetDialog(user)}
                          className="inline-flex items-center gap-1 rounded-lg border border-border bg-background px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted"
                          title={t("users.resetPassword")}
                        >
                          <Key className="size-3" /> {t("users.resetPassword")}
                        </button>
                        <button
                          onClick={() => handleSendResetLink(user)}
                          className="inline-flex items-center gap-1 rounded-lg border border-border bg-background px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted"
                          title={t("users.sendResetLink")}
                        >
                          <RotateCcw className="size-3" /> {t("users.sendResetLink")}
                        </button>
                        {confirmId === user.id ? (
                          <div className="inline-flex items-center gap-1.5">
                            <span className="text-xs text-muted-foreground">
                              {tc("confirm")} {user.isActive ? t("users.suspend") : t("users.activate")}?
                            </span>
                            <button
                              onClick={() => handleToggleStatus(user)}
                              disabled={toggling === user.id}
                              className="rounded-lg bg-red-600 px-2.5 py-1 text-xs font-medium text-white hover:bg-red-700 disabled:opacity-50"
                            >
                              {toggling === user.id ? <Loader2 className="size-3 animate-spin" /> : t("users.confirmYes")}
                            </button>
                            <button
                              onClick={() => setConfirmId(null)}
                              className="rounded-lg border border-border px-2.5 py-1 text-xs font-medium hover:bg-muted"
                            >
                              {tc("cancel")}</button>
                          </div>
                        ) : (
                          <button
                            onClick={() => setConfirmId(user.id)}
                            disabled={toggling === user.id}
                            className={`inline-flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-xs font-medium transition-colors disabled:opacity-50 ${
                              user.isActive
                                ? "border-red-200 text-red-600 hover:bg-red-50 dark:border-red-800"
                                : "border-green-200 text-green-600 hover:bg-green-50 dark:border-green-800"
                            }`}
                          >
                            {toggling === user.id ? (
                              <Loader2 className="size-3.5 animate-spin" />
                            ) : user.isActive ? (
                              <UserX className="size-3.5" />
                            ) : (
                              <UserCheck className="size-3.5" />
                            )}
                            {user.isActive ? t("users.suspend2") : t("users.activate2")}
                          </button>
                        )}
                        <button
                          onClick={() => handleDelete(user)}
                          className="inline-flex items-center gap-1 rounded-lg border border-red-200 bg-red-50 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-100 dark:border-red-800"
                        >
                          <Trash2 className="size-3" /> {t("users.delete")}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {data && data.totalElements > PAGE_SIZE && (
        <div className="flex items-center justify-center gap-2">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
          >
            {t("users.prev")}</button>
          <span className="text-sm text-muted-foreground">
            {t("users.pageOf", { p0: page + 1, p1: data.totalPages })}</span>
          <button
            onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
            disabled={page >= data.totalPages - 1}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
          >
            {tc("next")}</button>
        </div>
      )}

      <UserFormModal
        open={modalOpen}
        user={editingForModal}
        onClose={() => setModalOpen(false)}
        onSaved={handleSaved}
      />

      {resetTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-foreground/40 p-4" role="dialog" aria-modal="true" aria-label={t("users.resetDialogTitle")}>
          <div className="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-lg">
            <h2 className="text-lg font-semibold text-foreground">{t("users.resetDialogTitle")}</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              {t("users.resetDialogFor", { email: resetTarget.email })}
            </p>
            <div className="mt-4 space-y-4">
              <div>
                <label htmlFor="reset-new-password" className="block text-sm font-medium text-foreground">
                  {t("users.newPasswordLabel")}
                </label>
                <input
                  id="reset-new-password"
                  type="password"
                  autoComplete="new-password"
                  value={resetPassword}
                  onChange={(e) => setResetPassword(e.target.value)}
                  className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none focus:border-ring focus:bg-background"
                />
              </div>
              <div>
                <label htmlFor="reset-confirm-password" className="block text-sm font-medium text-foreground">
                  {t("users.confirmNewPasswordLabel")}
                </label>
                <input
                  id="reset-confirm-password"
                  type="password"
                  autoComplete="new-password"
                  value={resetConfirm}
                  onChange={(e) => setResetConfirm(e.target.value)}
                  className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none focus:border-ring focus:bg-background"
                />
              </div>
              {resetError && (
                <p className="text-sm text-destructive">{resetError}</p>
              )}
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setResetTarget(null)}
                  className="rounded-lg border border-border px-4 py-2.5 text-sm font-medium hover:bg-muted"
                >
                  {tc("cancel")}
                </button>
                <button
                  type="button"
                  onClick={handleResetPassword}
                  disabled={resetting}
                  className="rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                >
                  {resetting ? t("users.resetting") : t("users.confirmReset")}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}