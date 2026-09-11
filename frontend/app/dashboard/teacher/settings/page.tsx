"use client"

import { useState } from "react"
import { Settings, Save, Loader2, AlertCircle } from "lucide-react"
import { useAuth } from "@/lib/auth"

export default function TeacherSettingsPage() {
  const { user } = useAuth()
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState("")
  const [firstName, setFirstName] = useState(user?.firstName || "")
  const [lastName, setLastName] = useState(user?.lastName || "")
  const [phone, setPhone] = useState("")
  const [notifAttendance, setNotifAttendance] = useState(true)
  const [notifAssignments, setNotifAssignments] = useState(true)
  const [notifMessages, setNotifMessages] = useState(false)
  const [notifResults, setNotifResults] = useState(true)

  async function handleSave() {
    setSaving(true)
    setError("")
    try {
      const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
      const res = await fetch(`/v1/teachers/me/profile`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ firstName, lastName }),
      })
      if (res.ok) {
        setSaved(true)
        setTimeout(() => setSaved(false), 3000)
      } else {
        const body = await res.json().catch(() => ({}))
        setError(body.error || body.message || "Failed to save. Profile updates may require admin assistance.")
      }
    } catch {
      setError("Network error. Please try again.")
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Settings</h1>
          <p className="mt-1 text-sm text-muted-foreground">Manage your profile and preferences.</p>
        </div>
        <button
          onClick={handleSave}
          disabled={saving}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-xs hover:bg-primary/90 disabled:opacity-50"
        >
          {saving ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />}
          {saved ? "Saved!" : "Save Changes"}
        </button>
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          <AlertCircle className="size-4 shrink-0" />
          {error}
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
        <h3 className="text-sm font-semibold text-foreground">Profile</h3>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="text-xs font-medium text-muted-foreground">First Name</label>
            <input value={firstName} onChange={(e) => setFirstName(e.target.value)} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" />
          </div>
          <div>
            <label className="text-xs font-medium text-muted-foreground">Last Name</label>
            <input value={lastName} onChange={(e) => setLastName(e.target.value)} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" />
          </div>
          <div>
            <label className="text-xs font-medium text-muted-foreground">Email</label>
            <input value={user?.email || ""} disabled className="mt-1 w-full rounded-lg border border-border bg-muted px-3 py-2 text-sm text-muted-foreground" />
          </div>
          <div>
            <label className="text-xs font-medium text-muted-foreground">Phone</label>
            <input value={phone} onChange={(e) => setPhone(e.target.value)} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" placeholder="+255..." />
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
        <h3 className="text-sm font-semibold text-foreground">Notification Preferences</h3>
        <div className="space-y-3">
          {[
            { label: "Attendance alerts", value: notifAttendance, onChange: setNotifAttendance },
            { label: "Assignment deadlines", value: notifAssignments, onChange: setNotifAssignments },
            { label: "Messages from parents/students", value: notifMessages, onChange: setNotifMessages },
            { label: "Grade publishing", value: notifResults, onChange: setNotifResults },
          ].map((item) => (
            <label key={item.label} className="flex items-center justify-between">
              <span className="text-sm text-foreground">{item.label}</span>
              <button
                type="button"
                onClick={() => item.onChange(!item.value)}
                className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors ${
                  item.value ? "bg-primary" : "bg-muted"
                }`}
              >
                <span className={`pointer-events-none inline-block size-5 rounded-full bg-white shadow-xs transition-transform ${
                  item.value ? "translate-x-5" : "translate-x-0"
                }`} />
              </button>
            </label>
          ))}
        </div>
      </div>
    </div>
  )
}
