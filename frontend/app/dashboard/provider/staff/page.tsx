"use client"
import { useCallback, useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Loader2, UserPlus, Trash2 } from "lucide-react"
import { nfeApi } from "@/lib/nfe-api"

/**
 * Provider staff management (authority level C -> D).
 *
 * Every action here is authorised on the server against the provider id resolved from the
 * signed-in account's own scope. This page never sends a role: `inviteStaff` has no role field,
 * so the only thing it can produce is a PROVIDER_STAFF invitation, and a modified client cannot
 * ask for anything more.
 *
 * Empty states are honest. If no provider is provisioned for this organisation the page says so
 * rather than rendering an empty table that reads like "you have no staff".
 */
type StaffMember = {
  userId: string
  email: string | null
  firstName: string | null
  lastName: string | null
  fullName: string | null
  globalRole: string | null
  membershipRole: string | null
  responsibility: string | null
  isActive: boolean | null
}

export default function ProviderStaffPage() {
  const t = useTranslations("provider.staff")
  const [staff, setStaff] = useState<StaffMember[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const [email, setEmail] = useState("")
  const [responsibility, setResponsibility] = useState("")
  const [inviting, setInviting] = useState(false)
  const [busyUserId, setBusyUserId] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      const rows = await nfeApi.listStaff()
      setStaff(Array.isArray(rows) ? rows : [])
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : t("loadError"))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    void load()
  }, [load])

  const handleInvite = async () => {
    if (!email.trim()) return
    setInviting(true)
    setError(null)
    setNotice(null)
    try {
      const result = await nfeApi.inviteStaff({
        email: email.trim(),
        responsibility: responsibility.trim() || null,
      })
      // The token is returned once, to the authorized inviter, for out-of-band delivery. It is
      // never re-listable, so it is surfaced here rather than silently discarded - an owner who
      // cannot see it cannot complete the workflow.
      setNotice(t("invited", { email, token: result?.token ?? "" }))
      setEmail("")
      setResponsibility("")
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : t("inviteError"))
    } finally {
      setInviting(false)
    }
  }

  const handleResponsibility = async (member: StaffMember) => {
    const next = window.prompt(t("responsibilityPrompt"), member.responsibility ?? "")
    if (next === null) return
    setBusyUserId(member.userId)
    setError(null)
    try {
      await nfeApi.setStaffResponsibility(member.userId, next.trim() || null)
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : t("responsibilityError"))
    } finally {
      setBusyUserId(null)
    }
  }

  const handleRemove = async (member: StaffMember) => {
    if (!window.confirm(t("removeConfirm", { email: member.email ?? member.userId }))) return
    setBusyUserId(member.userId)
    setError(null)
    try {
      await nfeApi.removeStaff(member.userId)
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : t("removeError"))
    } finally {
      setBusyUserId(null)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  const active = staff.filter((m) => m.isActive)
  const removed = staff.filter((m) => !m.isActive)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">{t("title")}</h1>
        <p className="text-sm text-muted-foreground">{t("subtitle")}</p>
      </div>

      {error && (
        <div role="alert" className="rounded-md border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {error}
        </div>
      )}
      {notice && (
        <div role="status" className="rounded-md border border-emerald-500/40 bg-emerald-500/10 px-4 py-3 text-sm">
          {notice}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <UserPlus className="h-4 w-4" /> {t("inviteTitle")}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <p className="text-sm text-muted-foreground">{t("inviteHelp")}</p>
          <div className="flex flex-col gap-2 sm:flex-row">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder={t("emailPlaceholder")}
              aria-label={t("emailPlaceholder")}
              className="flex-1 rounded-md border border-input bg-background px-3 py-2 text-sm"
            />
            <input
              value={responsibility}
              onChange={(e) => setResponsibility(e.target.value)}
              placeholder={t("responsibilityPlaceholder")}
              aria-label={t("responsibilityPlaceholder")}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm sm:w-56"
            />
            <Button onClick={handleInvite} disabled={inviting || !email.trim()}>
              {inviting ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
              {t("inviteAction")}
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">{t("rosterTitle", { count: active.length })}</CardTitle>
        </CardHeader>
        <CardContent>
          {active.length === 0 ? (
            <p className="text-sm text-muted-foreground">{t("empty")}</p>
          ) : (
            <ul className="divide-y divide-border">
              {active.map((member) => (
                <li key={member.userId} className="flex flex-col gap-2 py-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="text-sm font-medium">{member.fullName || member.email}</p>
                    <p className="text-xs text-muted-foreground">
                      {member.email}
                      {member.responsibility ? ` · ${member.responsibility}` : ""}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Button variant="outline" size="sm" onClick={() => handleResponsibility(member)} disabled={busyUserId === member.userId}>
                      {t("responsibilityAction")}
                    </Button>
                    <Button variant="destructive" size="sm" onClick={() => handleRemove(member)} disabled={busyUserId === member.userId}>
                      {busyUserId === member.userId ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
                      {t("removeAction")}
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          )}

          {removed.length > 0 && (
            <>
              <h3 className="mt-6 text-sm font-semibold">{t("removedTitle", { count: removed.length })}</h3>
              <p className="text-xs text-muted-foreground">{t("removedHelp")}</p>
              <ul className="mt-2 divide-y divide-border opacity-70">
                {removed.map((member) => (
                  <li key={member.userId} className="py-2 text-sm text-muted-foreground">
                    {member.fullName || member.email}
                    <span className="ml-2 text-xs">{t("removedBadge")}</span>
                  </li>
                ))}
              </ul>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
