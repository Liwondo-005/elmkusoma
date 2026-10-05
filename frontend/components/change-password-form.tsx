"use client"

import { useState } from "react"
import { useTranslations } from "next-intl"
import { AlertCircle, CheckCircle, Loader2, Lock, Eye, EyeOff } from "lucide-react"
import { announce } from "@/lib/announce"

/**
 * Single shared Change Password card (R3 consolidation).
 *
 * Every settings surface renders this component instead of maintaining its own
 * copy of the form: state, client validation (match + min length), the
 * /v1/auth/reset-password call with bearer auth, and the accessible show/hide
 * toggles all live here.
 */
export function ChangePasswordForm() {
  const t = useTranslations("profile")
  const tc = useTranslations("common")

  const [currentPassword, setCurrentPassword] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [showCurrentPassword, setShowCurrentPassword] = useState(false)
  const [showNewPassword, setShowNewPassword] = useState(false)
  const [changingPassword, setChangingPassword] = useState(false)
  const [passwordSuccess, setPasswordSuccess] = useState<string | null>(null)
  const [passwordError, setPasswordError] = useState<string | null>(null)

  async function handleChangePassword() {
    if (!newPassword || !confirmPassword) {
      setPasswordError(t("passwordFillAll"))
      return
    }
    if (newPassword !== confirmPassword) {
      setPasswordError(t("passwordNoMatch"))
      return
    }
    if (newPassword.length < 8) {
      setPasswordError(t("passwordMinLength"))
      return
    }
    try {
      setChangingPassword(true)
      setPasswordError(null)
      setPasswordSuccess(null)
      const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || ""}/v1/auth/change-password`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ currentPassword, newPassword }),
      })
      if (!res.ok) {
        const body = await res.json().catch(() => ({}))
        throw new Error(body.error || body.message || t("passwordChangeError"))
      }
      setPasswordSuccess(t("passwordChangeSuccess"))
      announce(t("passwordChangeSuccess"))
      setCurrentPassword("")
      setNewPassword("")
      setConfirmPassword("")
    } catch (err: any) {
      setPasswordError(err.message || t("passwordChangeError"))
      announce(err.message || t("passwordChangeError"))
    } finally {
      setChangingPassword(false)
    }
  }

  return (
    <div className="rounded-2xl border border-border bg-card p-6 shadow-xs" data-testid="change-password-form">
      <div className="flex items-center gap-2 mb-4">
        <Lock className="size-4 text-muted-foreground" />
        <h2 className="font-semibold text-foreground">{t("changePassword")}</h2>
      </div>

      {passwordError && (
        <div className="mb-4 rounded-lg border border-destructive/20 bg-destructive/5 p-3">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {passwordError}
          </div>
        </div>
      )}

      {passwordSuccess && (
        <div className="mb-4 rounded-lg border border-green-500/20 bg-green-500/5 p-3">
          <div className="flex items-center gap-2 text-sm text-green-600">
            <CheckCircle className="size-4" />
            {passwordSuccess}
          </div>
        </div>
      )}

      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-foreground mb-2" htmlFor="change-password-current">
            {t("currentPassword")}
          </label>
          <div className="relative">
            <input
              id="change-password-current"
              type={showCurrentPassword ? "text" : "password"}
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              placeholder={t("currentPasswordPlaceholder")}
              className="h-10 w-full rounded-lg border border-border bg-background px-3 pr-10 text-sm outline-none focus:border-ring"
            />
            <button
              type="button"
              aria-label={showCurrentPassword ? "Hide current password" : "Show current password"}
              onClick={() => setShowCurrentPassword(!showCurrentPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
            >
              {showCurrentPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
            </button>
          </div>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-sm font-medium text-foreground mb-2" htmlFor="change-password-new">
              {t("newPassword")}
            </label>
            <div className="relative">
              <input
                id="change-password-new"
                type={showNewPassword ? "text" : "password"}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder={t("newPasswordPlaceholder")}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 pr-10 text-sm outline-none focus:border-ring"
              />
              <button
                type="button"
                aria-label={showNewPassword ? "Hide new password" : "Show new password"}
                onClick={() => setShowNewPassword(!showNewPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              >
                {showNewPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
              </button>
            </div>
            <p className="mt-1 text-xs text-muted-foreground">{t("passwordMinHelp")}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-2" htmlFor="change-password-confirm">
              {t("confirmPassword")}
            </label>
            <input
              id="change-password-confirm"
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder={t("confirmPasswordPlaceholder")}
              className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
            />
          </div>
        </div>

        <div className="flex justify-end">
          <button
            onClick={handleChangePassword}
            disabled={changingPassword || !currentPassword || !newPassword || !confirmPassword}
            className="inline-flex items-center gap-2 rounded-lg border border-border bg-background px-6 py-2.5 text-sm font-medium text-foreground hover:bg-muted disabled:opacity-50"
          >
            {changingPassword ? (
              <>
                <Loader2 className="size-4 animate-spin" />
                {tc("saving")}
              </>
            ) : (
              <>
                <Lock className="size-4" />
                {t("changePassword")}
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  )
}
