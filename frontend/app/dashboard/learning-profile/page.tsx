"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { primaryApi, type LearningProfile } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { User, Edit3, X, Star, BookOpen, Target, Award, CheckCircle } from "lucide-react"

const learningStyles = [
  { value: "VISUAL", label: "Visual", description: "You learn best by seeing — charts, diagrams, videos", color: "bg-blue-100 text-blue-700", icon: "eyes" },
  { value: "AUDITORY", label: "Auditory", description: "You learn best by hearing — discussions, lectures, music", color: "bg-purple-100 text-purple-700", icon: "ear" },
  { value: "KINESTHETIC", label: "Kinesthetic", description: "You learn best by doing — hands-on activities, movement", color: "bg-green-100 text-green-700", icon: "hand" },
  { value: "READING_WRITING", label: "Reading/Writing", description: "You learn best by reading and writing", color: "bg-amber-100 text-amber-700", icon: "book" },
]

const styleColorMap: Record<string, string> = {
  VISUAL: "bg-blue-100 text-blue-700",
  AUDITORY: "bg-purple-100 text-purple-700",
  KINESTHETIC: "bg-green-100 text-green-700",
  READING_WRITING: "bg-amber-100 text-amber-700",
}

export default function LearningProfilePage() {
  const { user } = useRequireAuth()
  const [profile, setProfile] = useState<LearningProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ learningStyle: "VISUAL", strengths: "", interests: "", goals: "" })
  const [strengthInput, setStrengthInput] = useState("")
  const [interestInput, setInterestInput] = useState("")
  const [strengthsList, setStrengthsList] = useState<string[]>([])
  const [interestsList, setInterestsList] = useState<string[]>([])
  const [submitting, setSubmitting] = useState(false)
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  useEffect(() => {
    if (!user) return
    loadProfile()
  }, [user])

  async function loadProfile() {
    try {
      setLoading(true)
      const data = await primaryApi.getLearningProfile().catch(() => null)
      setProfile(data)
      if (data) {
        setForm({
          learningStyle: data.learningStyle || "VISUAL",
          strengths: data.strengths || "",
          interests: data.interests || "",
          goals: data.goals || "",
        })
        setStrengthsList(data.strengths ? data.strengths.split(",").map((s) => s.trim()).filter(Boolean) : [])
        setInterestsList(data.interests ? data.interests.split(",").map((s) => s.trim()).filter(Boolean) : [])
      }
    } catch {
      setProfile(null)
    } finally {
      setLoading(false)
    }
  }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault()
    try {
      setSubmitting(true)
      const updated = await primaryApi.updateLearningProfile({
        learningStyle: form.learningStyle,
        strengths: strengthsList.join(", "),
        interests: interestsList.join(", "),
        goals: form.goals,
      })
      setProfile(updated)
      setShowForm(false)
    } catch {
      // handle silently
    } finally {
      setSubmitting(false)
    }
  }

  function addStrength() {
    if (strengthInput.trim() && !strengthsList.includes(strengthInput.trim())) {
      setStrengthsList((prev) => [...prev, strengthInput.trim()])
      setStrengthInput("")
    }
  }

  function removeStrength(s: string) {
    setStrengthsList((prev) => prev.filter((x) => x !== s))
  }

  function addInterest() {
    if (interestInput.trim() && !interestsList.includes(interestInput.trim())) {
      setInterestsList((prev) => [...prev, interestInput.trim()])
      setInterestInput("")
    }
  }

  function removeInterest(s: string) {
    setInterestsList((prev) => prev.filter((x) => x !== s))
  }

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  if (!isPrimary) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-2xl bg-purple-500/10">
            <User className="size-5 text-purple-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">My Learning Profile</h1>
            <p className="text-sm text-muted-foreground">Your personalized learning style</p>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <User className="size-12 text-muted-foreground/30" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">Learning Profile is for Primary learners</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Switch to a primary learner account to set up your profile.
          </p>
        </div>
      </div>
    )
  }

  if (!profile && !showForm) {
    return (
      <div className="mx-auto max-w-4xl space-y-6">
        <div className="rounded-2xl border border-border bg-gradient-to-br from-purple-500/5 via-card to-blue-500/5 p-6 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-purple-500/10">
              <User className="size-6 text-purple-500" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-foreground">My Learning Profile</h1>
              <p className="text-sm text-muted-foreground">Your personalized learning style</p>
            </div>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10">
            <User className="size-8 text-primary" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">Tell us about yourself!</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Set up your learning profile to personalize your experience.
          </p>
          <button
            onClick={() => setShowForm(true)}
            className="mt-6 inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            <Edit3 className="size-4" /> Create Profile
          </button>
        </div>
      </div>
    )
  }

  if (showForm) {
    return (
      <div className="mx-auto max-w-4xl space-y-6">
        <div className="rounded-2xl border border-border bg-gradient-to-br from-purple-500/5 via-card to-blue-500/5 p-6 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-purple-500/10">
              <User className="size-6 text-purple-500" />
            </div>
            <div className="flex-1">
              <h1 className="text-2xl font-bold tracking-tight text-foreground">
                {profile ? "Edit Learning Profile" : "Create Learning Profile"}
              </h1>
              <p className="text-sm text-muted-foreground">Tell us how you learn best</p>
            </div>
            {profile && (
              <button onClick={() => setShowForm(false)} className="text-muted-foreground hover:text-foreground">
                <X className="size-5" />
              </button>
            )}
          </div>
        </div>

        <form onSubmit={handleSave} className="rounded-2xl border border-border bg-card p-6 shadow-xs space-y-6">
          <div>
            <label className="text-sm font-bold text-foreground">Learning Style</label>
            <p className="text-xs text-muted-foreground mt-1">Choose the way you learn best</p>
            <div className="mt-3 grid gap-3 sm:grid-cols-2">
              {learningStyles.map((style) => (
                <button
                  key={style.value}
                  type="button"
                  onClick={() => setForm((prev) => ({ ...prev, learningStyle: style.value }))}
                  className={`rounded-xl border p-4 text-left transition-all ${
                    form.learningStyle === style.value
                      ? `${styleColorMap[style.value]} border-current ring-2 ring-primary/20`
                      : "border-border bg-background hover:border-primary/30"
                  }`}
                >
                  <p className="text-sm font-bold">{style.label}</p>
                  <p className="mt-1 text-xs opacity-80">{style.description}</p>
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="text-sm font-bold text-foreground">Strengths</label>
            <p className="text-xs text-muted-foreground mt-1">What are you good at?</p>
            <div className="mt-2 flex gap-2">
              <input
                type="text"
                value={strengthInput}
                onChange={(e) => setStrengthInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), addStrength())}
                placeholder="Add a strength..."
                className="flex-1 rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring"
              />
              <button type="button" onClick={addStrength} className="rounded-lg bg-muted px-3 py-2 text-sm font-medium hover:bg-muted/80 transition-colors">
                Add
              </button>
            </div>
            {strengthsList.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-2">
                {strengthsList.map((s) => (
                  <span key={s} className="inline-flex items-center gap-1 rounded-full bg-green-100 px-3 py-1 text-xs font-medium text-green-700">
                    {s}
                    <button type="button" onClick={() => removeStrength(s)} className="hover:text-green-900">
                      <X className="size-3" />
                    </button>
                  </span>
                ))}
              </div>
            )}
          </div>

          <div>
            <label className="text-sm font-bold text-foreground">Interests</label>
            <p className="text-xs text-muted-foreground mt-1">What do you enjoy?</p>
            <div className="mt-2 flex gap-2">
              <input
                type="text"
                value={interestInput}
                onChange={(e) => setInterestInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), addInterest())}
                placeholder="Add an interest..."
                className="flex-1 rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring"
              />
              <button type="button" onClick={addInterest} className="rounded-lg bg-muted px-3 py-2 text-sm font-medium hover:bg-muted/80 transition-colors">
                Add
              </button>
            </div>
            {interestsList.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-2">
                {interestsList.map((s) => (
                  <span key={s} className="inline-flex items-center gap-1 rounded-full bg-blue-100 px-3 py-1 text-xs font-medium text-blue-700">
                    {s}
                    <button type="button" onClick={() => removeInterest(s)} className="hover:text-blue-900">
                      <X className="size-3" />
                    </button>
                  </span>
                ))}
              </div>
            )}
          </div>

          <div>
            <label className="text-sm font-bold text-foreground">Goals</label>
            <p className="text-xs text-muted-foreground mt-1">What do you want to learn?</p>
            <textarea
              value={form.goals}
              onChange={(e) => setForm((prev) => ({ ...prev, goals: e.target.value }))}
              placeholder="Write your learning goals..."
              rows={4}
              className="mt-2 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
          >
            {submitting ? (
              <div className="size-4 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent" />
            ) : (
              "Save Profile"
            )}
          </button>
        </form>
      </div>
    )
  }

  const styleInfo = learningStyles.find((s) => s.value === profile?.learningStyle)
  const strengthsArr = profile?.strengths ? profile.strengths.split(",").map((s) => s.trim()).filter(Boolean) : []
  const interestsArr = profile?.interests ? profile.interests.split(",").map((s) => s.trim()).filter(Boolean) : []

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-purple-500/5 via-card to-blue-500/5 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-purple-500/10">
            <User className="size-6 text-purple-500" />
          </div>
          <div className="flex-1">
            <h1 className="text-2xl font-bold tracking-tight text-foreground">My Learning Profile</h1>
            <p className="text-sm text-muted-foreground">Your personalized learning style</p>
          </div>
          <button
            onClick={() => setShowForm(true)}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            <Edit3 className="size-4" /> Edit Profile
          </button>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-purple-50">
              <Award className="size-5 text-purple-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{profile?.level || 1}</p>
              <p className="text-xs text-muted-foreground">Current Level</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-amber-50">
              <Star className="size-5 text-amber-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{profile?.totalPoints || 0}</p>
              <p className="text-xs text-muted-foreground">Total Points</p>
            </div>
          </div>
        </div>
      </div>

      {styleInfo && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">Learning Style</h2>
          <div className={`inline-flex items-center gap-2 rounded-xl px-4 py-3 ${styleColorMap[profile?.learningStyle || "VISUAL"]}`}>
            <BookOpen className="size-5" />
            <div>
              <p className="text-sm font-bold">{styleInfo.label}</p>
              <p className="text-xs opacity-80">{styleInfo.description}</p>
            </div>
          </div>
        </div>
      )}

      {strengthsArr.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">Strengths</h2>
          <div className="flex flex-wrap gap-2">
            {strengthsArr.map((s) => (
              <span key={s} className="inline-flex items-center gap-1 rounded-full bg-green-100 px-3 py-1.5 text-sm font-medium text-green-700">
                <CheckCircle className="size-3" /> {s}
              </span>
            ))}
          </div>
        </div>
      )}

      {interestsArr.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">Interests</h2>
          <div className="flex flex-wrap gap-2">
            {interestsArr.map((s) => (
              <span key={s} className="inline-flex items-center gap-1 rounded-full bg-blue-100 px-3 py-1.5 text-sm font-medium text-blue-700">
                {s}
              </span>
            ))}
          </div>
        </div>
      )}

      {profile?.goals && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">Goals</h2>
          <div className="rounded-xl bg-muted/30 p-4">
            <p className="text-sm text-foreground whitespace-pre-wrap">{profile.goals}</p>
          </div>
        </div>
      )}

      {!styleInfo && strengthsArr.length === 0 && interestsArr.length === 0 && !profile?.goals && (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-12 text-center">
          <Target className="size-10 text-muted-foreground/30" />
          <h3 className="mt-3 text-sm font-semibold text-foreground">No profile data yet</h3>
          <p className="mt-1 text-xs text-muted-foreground">Edit your profile to add your learning preferences.</p>
        </div>
      )}
    </div>
  )
}
