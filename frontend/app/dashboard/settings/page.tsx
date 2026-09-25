"use client"

import { useState, useEffect } from "react"
import { Settings } from "lucide-react"

const STORAGE_KEY = "elmkusoma_settings"

function loadSettings(): Record<string, boolean> {
  if (typeof window === "undefined") return {}
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function saveSettings(settings: Record<string, boolean>) {
  if (typeof window === "undefined") return
  localStorage.setItem(STORAGE_KEY, JSON.stringify(settings))
}

export default function DashboardSettingsPage() {
  const [saved, setSaved] = useState(false)
  const [settings, setSettings] = useState<Record<string, boolean>>({})

  useEffect(() => {
    setSettings(loadSettings())
  }, [])

  function handleToggle(key: string) {
    setSettings((prev) => ({ ...prev, [key]: !prev[key] }))
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    saveSettings(settings)
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Settings</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Preferences below are stored on this device (browser only) — they are not synced to your account.
        </p>
      </div>

      <form className="space-y-6" onSubmit={handleSubmit}>
        {/* Notifications */}
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">Notifications</h2>
          <div className="mt-4 space-y-4">
            <label className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-foreground">Email notifications</p>
                <p className="text-xs text-muted-foreground">Receive updates about courses and classes</p>
              </div>
              <input type="checkbox" checked={settings.emailNotifications ?? true} onChange={() => handleToggle("emailNotifications")} className="size-4 rounded border-border accent-primary" />
            </label>
            <label className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-foreground">Live class reminders</p>
                <p className="text-xs text-muted-foreground">Get notified before live classes start</p>
              </div>
              <input type="checkbox" checked={settings.liveClassReminders ?? true} onChange={() => handleToggle("liveClassReminders")} className="size-4 rounded border-border accent-primary" />
            </label>
            <label className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-foreground">Assignment deadlines</p>
                <p className="text-xs text-muted-foreground">Reminders before assignment due dates</p>
              </div>
              <input type="checkbox" checked={settings.assignmentDeadlines ?? true} onChange={() => handleToggle("assignmentDeadlines")} className="size-4 rounded border-border accent-primary" />
            </label>
            <label className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-foreground">Marketing emails</p>
                <p className="text-xs text-muted-foreground">News about new features and promotions</p>
              </div>
              <input type="checkbox" checked={settings.marketingEmails ?? false} onChange={() => handleToggle("marketingEmails")} className="size-4 rounded border-border accent-primary" />
            </label>
          </div>
        </div>

        {/* Privacy */}
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">Privacy</h2>
          <div className="mt-4 space-y-4">
            <label className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-foreground">Show profile to others</p>
                <p className="text-xs text-muted-foreground">Allow other learners to see your profile</p>
              </div>
              <input type="checkbox" checked={settings.showProfile ?? true} onChange={() => handleToggle("showProfile")} className="size-4 rounded border-border accent-primary" />
            </label>
            <label className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-foreground">Show learning activity</p>
                <p className="text-xs text-muted-foreground">Display your progress on your profile</p>
              </div>
              <input type="checkbox" checked={settings.showLearningActivity ?? false} onChange={() => handleToggle("showLearningActivity")} className="size-4 rounded border-border accent-primary" />
            </label>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button type="submit" className="h-11 rounded-lg bg-primary px-6 text-sm font-medium text-primary-foreground hover:bg-primary/90">
            Save Settings
          </button>
          {saved && <span className="text-sm text-teal font-medium">Saved on this device.</span>}
        </div>
      </form>
    </div>
  )
}
