"use client"

import { useEffect, useState } from "react"
import { useAuth, translateRole } from "@/lib/auth"
import { learnerApi, type LearnerProfile, type ProfileUpdate } from "@/lib/learner-api"
import { announce } from "@/lib/announce"
import { LoadingState } from "@/components/learner/shared"
import { ChangePasswordForm } from "@/components/change-password-form"
import { useTranslations } from "next-intl"
import { User, Save, AlertCircle, CheckCircle, Loader2, Shield, BookOpen } from "lucide-react"

export default function LearnerProfilePage() {
  const { user, loading: authLoading } = useAuth()
  const [profile, setProfile] = useState<LearnerProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const [bio, setBio] = useState("")
  const [interests, setInterests] = useState("")
  const [learningGoal, setLearningGoal] = useState("")
  const [avatarUrl, setAvatarUrl] = useState("")

  const t = useTranslations("profile")
  const tc = useTranslations("common")
  const tr = useTranslations("roles")

  useEffect(() => {
    if (!user || (user.role !== "Other Learner" && user.role !== "Student")) return
    loadProfile()
  }, [user])

  async function loadProfile() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getProfile()
      setProfile(data)
      setBio(data.bio || "")
      setInterests(data.interests || "")
      setLearningGoal(data.learningGoal || "")
      setAvatarUrl(data.avatarUrl || "")
    } catch {
      setError(t("loadError"))
    } finally {
      setLoading(false)
    }
  }

  async function handleSave() {
    try {
      setSaving(true)
      setError(null)
      setSuccess(null)
      const data: ProfileUpdate = {
        bio: bio || undefined,
        interests: interests || undefined,
        learningGoal: learningGoal || undefined,
        avatarUrl: avatarUrl || undefined,
      }
      const updated = await learnerApi.updateProfile(data)
      setProfile(updated)
      setSuccess(t("updateSuccess"))
      announce(t("updateSuccess"))
    } catch (err: any) {
      setError(err.message || t("updateError"))
      announce(err.message || t("updateError"))
    } finally {
      setSaving(false)
    }
  }

  if (authLoading || loading || (user?.role !== "Other Learner" && user?.role !== "Student")) {
    return <LoadingState />
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("description")}</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      {success && (
        <div className="rounded-2xl border border-green-500/20 bg-green-500/5 p-4">
          <div className="flex items-center gap-2 text-sm text-green-600">
            <CheckCircle className="size-4" />
            {success}
          </div>
        </div>
      )}

      {/* Account Information */}
      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-center gap-2 mb-4">
          <Shield className="size-4 text-muted-foreground" />
          <h2 className="font-semibold text-foreground">{t("accountInfo")}</h2>
        </div>
        <div className="flex items-center gap-4 mb-6">
          <div className="flex size-16 items-center justify-center rounded-full bg-primary/10">
            {avatarUrl ? (
              <img src={avatarUrl} alt="Avatar" className="size-16 rounded-full object-cover" />
            ) : (
              <User className="size-8 text-primary/40" />
            )}
          </div>
          <div>
            <h3 className="text-lg font-semibold text-foreground">
              {user?.firstName} {user?.lastName}
            </h3>
            <p className="text-sm text-muted-foreground">{user?.email}</p>
            <div className="mt-1 flex items-center gap-2">
              <span className="inline-flex items-center rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold text-primary">
                {tc("generalLearner")}
              </span>
              {user?.learningLevel && (
                <span className="inline-flex items-center rounded-full bg-muted px-2 py-0.5 text-[10px] font-semibold text-muted-foreground">
                  {user.learningLevel}
                </span>
              )}
            </div>
          </div>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-xs font-medium text-muted-foreground mb-1">{t("fullName")}</label>
            <p className="text-sm text-foreground">{user?.firstName} {user?.lastName || ""}</p>
          </div>
          <div>
            <label className="block text-xs font-medium text-muted-foreground mb-1">{t("email")}</label>
            <p className="text-sm text-foreground">{user?.email}</p>
          </div>
          <div>
            <label className="block text-xs font-medium text-muted-foreground mb-1">{t("role")}</label>
            <p className="text-sm text-foreground">{translateRole(user?.role, tr)}</p>
          </div>
          <div>
            <label className="block text-xs font-medium text-muted-foreground mb-1">{t("learningLevel")}</label>
            <p className="text-sm text-foreground">{user?.learningLevel || "Not set"}</p>
          </div>
          {user?.phone && (
            <div>
              <label className="block text-xs font-medium text-muted-foreground mb-1">{t("phone")}</label>
              <p className="text-sm text-foreground">{user.phone}</p>
            </div>
          )}
        </div>
      </div>

      {/* Learning Preferences */}
      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-center gap-2 mb-4">
          <BookOpen className="size-4 text-muted-foreground" />
          <h2 className="font-semibold text-foreground">{t("learningPreferences")}</h2>
        </div>

        <div className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-foreground mb-2">{t("bio")}</label>
            <textarea
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              placeholder={t("bioPlaceholder")}
              rows={3}
              className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-2">{t("interests")}</label>
            <input
              type="text"
              value={interests}
              onChange={(e) => setInterests(e.target.value)}
              placeholder={t("interestsPlaceholder")}
              className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
            />
            <p className="mt-1 text-xs text-muted-foreground">{t("interestsHelp")}</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-2">{t("learningGoal")}</label>
            <textarea
              value={learningGoal}
              onChange={(e) => setLearningGoal(e.target.value)}
              placeholder={t("learningGoalPlaceholder")}
              rows={3}
              className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-2">{t("avatarUrl")}</label>
            <input
              type="url"
              value={avatarUrl}
              onChange={(e) => setAvatarUrl(e.target.value)}
              placeholder="https://example.com/avatar.jpg"
              className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
            />
          </div>

          <div className="flex justify-end">
            <button
              onClick={handleSave}
              disabled={saving}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              {saving ? (
                <>
                  <Loader2 className="size-4 animate-spin" />
                  {tc("saving")}
                </>
              ) : (
                <>
                  <Save className="size-4" />
                  {t("saveChanges")}
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Change Password — shared component (R3) */}
      <ChangePasswordForm />
    </div>
  )
}
