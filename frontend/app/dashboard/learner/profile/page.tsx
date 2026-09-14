"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { learnerApi, type LearnerProfile, type ProfileUpdate } from "@/lib/learner-api"
import { LoadingState } from "@/components/learner/shared"
import { User, Save, AlertCircle, CheckCircle, Loader2 } from "lucide-react"

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

  useEffect(() => {
    if (!user || user.role !== "Other Learner") return
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
      setError("Failed to load profile")
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
      setSuccess("Profile updated successfully!")
    } catch (err: any) {
      setError(err.message || "Failed to update profile")
    } finally {
      setSaving(false)
    }
  }

  if (authLoading || loading || user?.role !== "Other Learner") {
    return <LoadingState />
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Profile</h1>
        <p className="mt-1 text-sm text-muted-foreground">Manage your profile and learning preferences.</p>
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

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-center gap-4 mb-6">
          <div className="flex size-16 items-center justify-center rounded-full bg-primary/10">
            {avatarUrl ? (
              <img src={avatarUrl} alt="Avatar" className="size-16 rounded-full object-cover" />
            ) : (
              <User className="size-8 text-primary/40" />
            )}
          </div>
          <div>
            <h2 className="text-lg font-semibold text-foreground">
              {user?.firstName} {user?.lastName}
            </h2>
            <p className="text-sm text-muted-foreground">{user?.email}</p>
            <p className="text-xs text-muted-foreground mt-1">
              Learning Level: {user?.learningLevel || "Not set"}
            </p>
          </div>
        </div>

        <div className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-foreground mb-2">Bio</label>
            <textarea
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              placeholder="Tell us about yourself..."
              rows={4}
              className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-2">Interests</label>
            <input
              type="text"
              value={interests}
              onChange={(e) => setInterests(e.target.value)}
              placeholder="e.g., Mathematics, Science, Programming"
              className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
            />
            <p className="mt-1 text-xs text-muted-foreground">Comma-separated list of your interests</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-2">Learning Goal</label>
            <textarea
              value={learningGoal}
              onChange={(e) => setLearningGoal(e.target.value)}
              placeholder="What do you want to achieve?"
              rows={3}
              className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-2">Avatar URL</label>
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
                  Saving...
                </>
              ) : (
                <>
                  <Save className="size-4" />
                  Save Changes
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
