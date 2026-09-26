"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import { ArrowLeft, Loader2, UserCheck, UserX, Mail, Calendar, Shield, Building2 } from "lucide-react"
import { platformAdminApi, type UserSummary } from "@/lib/platform-admin-api"

export default function UserDetailPage() {
  const t = useTranslations("platformAdmin");
  const tc = useTranslations("common");
  const ts = useTranslations("status");
  const params = useParams()
  const router = useRouter()
  const id = params.id as string
  const [user, setUser] = useState<UserSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [toggling, setToggling] = useState(false)
  const [confirmAction, setConfirmAction] = useState<"suspend" | "activate" | null>(null)

  useEffect(() => {
    if (!id) return
    setLoading(true)
    platformAdminApi.getUser(id)
      .then(setUser)
      .catch((err) => setError(err instanceof Error ? err.message : t("userDetail.failedToLoadUser")))
      .finally(() => setLoading(false))
  }, [id])

  const handleToggleStatus = async () => {
    if (!user) return
    try {
      setToggling(true)
      const updated = await platformAdminApi.updateUserStatus(user.id, !user.isActive)
      setUser(updated)
      setConfirmAction(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("userDetail.failedToUpdateUser"))
    } finally {
      setToggling(false)
    }
  }

  const formatDate = (d: string) => new Date(d).toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" })

  const getInitials = (u: UserSummary) =>
    `${u.firstName?.[0] || ""}${u.lastName?.[0] || ""}`.toUpperCase() || "U"

  const getRoleBadge = (role: string) => {
    const map: Record<string, string> = {
      ADMIN: "bg-purple-100 text-purple-700",
      INSTITUTION_ADMIN: "bg-blue-100 text-blue-700",
      TEACHER: "bg-emerald-100 text-emerald-700",
      STUDENT: "bg-amber-100 text-amber-700",
      PARENT: "bg-cyan-100 text-cyan-700",
      PROVIDER_ADMIN: "bg-rose-100 text-rose-700",
    }
    return map[role] || "bg-gray-100 text-gray-700"
  }

  if (loading) return (
    <div className="flex items-center justify-center py-32">
      <Loader2 className="size-8 animate-spin text-muted-foreground" />
    </div>
  )

  if (error) return (
    <div className="mx-auto max-w-4xl space-y-6 py-10">
      <button onClick={() => router.back()} className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> {tc("back")}</button>
      <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive">{error}</div>
    </div>
  )

  if (!user) return null

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <button onClick={() => router.back()} className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> {t("userDetail.backToUsers")}</button>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-4">
            <div className="flex size-16 items-center justify-center rounded-full bg-primary/10 text-xl font-bold text-primary">
              {getInitials(user)}
            </div>
            <div>
              <h1 className="text-xl font-bold text-foreground">{user.firstName} {user.lastName}</h1>
              <div className="mt-1 flex items-center gap-2 text-sm text-muted-foreground">
                <Mail className="size-3.5" /> {user.email}
              </div>
              <div className="mt-2 flex items-center gap-2">
                <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${getRoleBadge(user.role)}`}>
                  {user.role}
                </span>
                <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${user.isActive ? "bg-green-500/10 text-green-600" : "bg-red-500/10 text-red-600"}`}>
                  {user.isActive ? ts("active") : t("userDetail.suspended")}
                </span>
              </div>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {confirmAction === null ? (
              <button
                onClick={() => setConfirmAction(user.isActive ? "suspend" : "activate")}
                className={`inline-flex items-center gap-2 rounded-lg border px-4 py-2 text-sm font-medium transition-colors ${
                  user.isActive
                    ? "border-red-200 text-red-600 hover:bg-red-50 dark:border-red-800"
                    : "border-green-200 text-green-600 hover:bg-green-50 dark:border-green-800"
                }`}
              >
                {user.isActive ? <UserX className="size-4" /> : <UserCheck className="size-4" />}
                {user.isActive ? t("userDetail.suspend2") : t("userDetail.activate2")}
              </button>
            ) : (
              <div className="flex items-center gap-2 rounded-lg border border-border bg-card p-2">
                <span className="text-xs text-muted-foreground whitespace-nowrap">
                  {confirmAction === "suspend" ? t("userDetail.suspendThisUser") : t("userDetail.activateThisUser")}
                </span>
                <button
                  onClick={handleToggleStatus}
                  disabled={toggling}
                  className="rounded-md bg-red-600 px-3 py-1 text-xs font-medium text-white hover:bg-red-700 disabled:opacity-50"
                >
                  {toggling ? <Loader2 className="size-3 animate-spin" /> : tc("confirm")}
                </button>
                <button
                  onClick={() => setConfirmAction(null)}
                  className="rounded-md border border-border px-3 py-1 text-xs font-medium hover:bg-muted"
                >
                  {tc("cancel")}</button>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-purple-500/10 text-purple-600">
              <Shield className="size-5" />
            </div>
            <div>
              <p className="text-xs text-muted-foreground">{t("userDetail.role")}</p>
              <p className="font-bold text-foreground">{user.role}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-blue-500/10 text-blue-600">
              <Building2 className="size-5" />
            </div>
            <div>
              <p className="text-xs text-muted-foreground">{t("userDetail.institution")}</p>
              <p className="font-bold text-foreground">{user.institutionId ? user.institutionId.slice(0, 8) + "..." : t("userDetail.none")}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-600">
              <Calendar className="size-5" />
            </div>
            <div>
              <p className="text-xs text-muted-foreground">{t("userDetail.joined")}</p>
              <p className="font-bold text-foreground">{formatDate(user.createdAt)}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-base font-semibold text-foreground mb-4">{t("userDetail.accountActions")}</h2>
        <div className="space-y-2">
          <button
            onClick={() => router.push("/dashboard/platform-admin/audit")}
            className="flex w-full items-center gap-3 rounded-lg border border-border px-4 py-3 text-sm font-medium text-foreground hover:bg-muted transition-colors"
          >
            <Calendar className="size-4 text-muted-foreground" />
            {t("userDetail.viewAuditTrailFor")}</button>
        </div>
      </div>
    </div>
  )
}
