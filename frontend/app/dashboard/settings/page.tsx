"use client"

import { useState, useEffect } from "react"
import { Settings } from "lucide-react"
import { useTranslations } from "next-intl"

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
  const t = useTranslations("primary")
  const ts = useTranslations("status")
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
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("settingsPage.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("settingsPage.subtitle")}
      </div>

      <form className="space-y-6" onSubmit={handleSubmit}>
        {/* Notifications */}
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">{t("settingsPage.notifications")}
          <div className="mt-4 space-y-4">
            <label className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-foreground">{t("settingsPage.emailLabel")}</p>
                <p className="text-xs text-muted-foreground">{t("settingsPage.emailDesc")}</p>
              </div>
              <input type="checkbox" checked={settings.emailNotifications ?? true} onChange={() => handleToggle("emailNotifications")} className="size-4 rounded border-border accent-primary" />
            </label>
            <label className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-foreground">{t("settingsPage.liveLabel")}</p>
                <p className="text-xs text-muted-foreground">{t("settingsPage.liveDesc")}</p>
              </div>
              <input type="checkbox" checked={settings.liveClassReminders ?? true} onChange={() => handleToggle("liveClassReminders")} className="size-4 rounded border-border accent-primary" />
            </label>
            <label className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-foreground">{t("settingsPage.assignLabel")}</p>
                <p className="text-xs text-muted-foreground">{t("settingsPage.assignDesc")}</p>
              </div>
              <input type="checkbox" checked={settings.assignmentDeadlines ?? true} onChange={() => handleToggle("assignmentDeadlines")} className="size-4 rounded border-border accent-primary" />
            </label>
            <label className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-foreground">{t("settingsPage.marketingLabel")}</p>
                <p className="text-xs text-muted-foreground">{t("settingsPage.marketingDesc")}</p>
              </div>
              <input type="checkbox" checked={settings.marketingEmails ?? false} onChange={() => handleToggle("marketingEmails")} className="size-4 rounded border-border accent-primary" />
            </label>
          </div>
        </div>

        {/* Privacy */}
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">{t("settingsPage.privacy")}
          <div className="mt-4 space-y-4">
            <label className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-foreground">{t("settingsPage.showProfile")}
                <p className="text-xs text-muted-foreground">{t("settingsPage.showProfileDesc")}</p>
              </div>
              <input type="checkbox" checked={settings.showProfile ?? true} onChange={() => handleToggle("showProfile")} className="size-4 rounded border-border accent-primary" />
            </label>
            <label className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-foreground">{t("settingsPage.showActivity")}
                <p className="text-xs text-muted-foreground">{t("settingsPage.showActivityDesc")}</p>
              </div>
              <input type="checkbox" checked={settings.showLearningActivity ?? false} onChange={() => handleToggle("showLearningActivity")} className="size-4 rounded border-border accent-primary" />
            </label>
          </div>
        </div>

        {/* Danger Zone */}
        <div className="rounded-2xl border border-destructive/30 bg-card p-6 shadow-xs">
          <h2 className="text-base font-semibold text-destructive">{t("settingsPage.danger")}
          <div className="mt-4 flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-foreground">{t("settingsPage.deleteTitle")}
              <p className="text-xs text-muted-foreground">{t("settingsPage.deleteDesc")}
            </div>
            <button type="button" className="h-9 rounded-lg border border-destructive/30 px-4 text-xs font-medium text-destructive hover:bg-destructive/5">
              {t("settingsPage.deleteButton")}
            </button>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button type="submit" className="h-11 rounded-lg bg-primary px-6 text-sm font-medium text-primary-foreground hover:bg-primary/90">
            {t("settingsPage.save")}
          </button>
          {saved && <span className="text-sm text-teal font-medium">{t("settingsPage.saved")}}
        </div>
      </form>
    </div>
  )
}
