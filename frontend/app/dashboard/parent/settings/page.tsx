"use client"

import { useState, useEffect } from "react"
import { User, Bell, Shield, Save, Loader2 } from "lucide-react"
import { useAuth } from "@/lib/auth"
import { parentApi, type NotificationPreferenceData } from "@/lib/parent-api"

export default function ParentSettingsPage() {
  const { user } = useAuth()
  const [activeTab, setActiveTab] = useState<"profile" | "notifications" | "privacy">("profile")
  const [saved, setSaved] = useState(false)
  const [saving, setSaving] = useState(false)
  const [firstName, setFirstName] = useState(user?.name?.split(" ")[0] || "")
  const [lastName, setLastName] = useState(user?.name?.split(" ").slice(1).join(" ") || "")
  const [phone, setPhone] = useState("")
  const [prefs, setPrefs] = useState<NotificationPreferenceData | null>(null)
  const [prefsLoading, setPrefsLoading] = useState(true)

  useEffect(() => {
    parentApi.getNotificationPreferences().then(setPrefs).catch(() => setPrefs(null)).finally(() => setPrefsLoading(false))
  }, [])

  async function handleProfileSave() {
    setSaving(true)
    try {
      await parentApi.updateProfile({ firstName, lastName, phone: phone || undefined })
      setSaved(true)
      setTimeout(() => setSaved(false), 2000)
    } catch {
    } finally {
      setSaving(false)
    }
  }

  async function handlePrefsSave() {
    if (!prefs) return
    setSaving(true)
    try {
      const updated = await parentApi.updateNotificationPreferences({
        attendanceAlerts: prefs.attendanceAlerts,
        gradeAlerts: prefs.gradeAlerts,
        feeAlerts: prefs.feeAlerts,
        generalAnnouncements: prefs.generalAnnouncements,
        smsEnabled: prefs.smsEnabled,
        emailEnabled: prefs.emailEnabled,
        pushEnabled: prefs.pushEnabled,
      })
      setPrefs(updated)
      setSaved(true)
      setTimeout(() => setSaved(false), 2000)
    } catch {
    } finally {
      setSaving(false)
    }
  }

  function togglePref(key: keyof NotificationPreferenceData) {
    if (!prefs) return
    setPrefs({ ...prefs, [key]: !prefs[key] })
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h1 className="text-xl font-bold text-foreground">Settings</h1>

      <div className="flex gap-1 rounded-lg border border-border bg-muted p-1">
        {([
          { key: "profile" as const, label: "Profile", icon: User },
          { key: "notifications" as const, label: "Notifications", icon: Bell },
          { key: "privacy" as const, label: "Privacy", icon: Shield },
        ]).map((tab) => (
          <button key={tab.key} onClick={() => setActiveTab(tab.key)}
            className={`flex flex-1 items-center justify-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors ${
              activeTab === tab.key ? "bg-card text-foreground shadow-xs" : "text-muted-foreground hover:text-foreground"
            }`}>
            <tab.icon className="size-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === "profile" && (
        <section className="rounded-2xl border border-border bg-card p-6 shadow-xs space-y-4">
          <h2 className="text-base font-semibold text-foreground">Profile Information</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="text-xs font-medium text-muted-foreground">First Name</label>
              <input type="text" value={firstName} onChange={(e) => setFirstName(e.target.value)}
                className="mt-1 w-full rounded-xl border border-border bg-muted/50 px-4 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20" />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">Last Name</label>
              <input type="text" value={lastName} onChange={(e) => setLastName(e.target.value)}
                className="mt-1 w-full rounded-xl border border-border bg-muted/50 px-4 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20" />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">Email</label>
              <input type="email" defaultValue={user?.email || ""} disabled
                className="mt-1 w-full rounded-xl border border-border bg-muted/50 px-4 py-2.5 text-sm text-muted-foreground" />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">Phone</label>
              <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="+255 XXX XXX XXX"
                className="mt-1 w-full rounded-xl border border-border bg-muted/50 px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/20" />
            </div>
          </div>
          <button onClick={handleProfileSave} disabled={saving}
            className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
            {saving ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />}
            {saved ? "Saved!" : saving ? "Saving..." : "Save Changes"}
          </button>
        </section>
      )}

      {activeTab === "notifications" && (
        <section className="rounded-2xl border border-border bg-card p-6 shadow-xs space-y-4">
          <h2 className="text-base font-semibold text-foreground">Notification Preferences</h2>
          {prefsLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="size-5 animate-spin text-muted-foreground" />
            </div>
          ) : prefs ? (
            <>
              {([
                { key: "attendanceAlerts" as const, label: "Attendance Alerts", desc: "Get notified when your child is absent or late" },
                { key: "gradeAlerts" as const, label: "Grade Published", desc: "Notification when report cards or grades are published" },
                { key: "feeAlerts" as const, label: "Fee Reminders", desc: "Payment due date reminders" },
                { key: "generalAnnouncements" as const, label: "School Events", desc: "Notifications about school events and meetings" },
                { key: "pushEnabled" as const, label: "Live Class Reminders", desc: "Reminders before live classes start" },
              ]).map((pref) => (
                <label key={pref.key} className="flex items-center justify-between rounded-xl border border-border p-4">
                  <div>
                    <p className="text-sm font-medium text-foreground">{pref.label}</p>
                    <p className="text-xs text-muted-foreground">{pref.desc}</p>
                  </div>
                  <input
                    type="checkbox"
                    checked={!!prefs[pref.key]}
                    onChange={() => togglePref(pref.key)}
                    className="size-4 rounded border-border accent-primary"
                  />
                </label>
              ))}
              <button onClick={handlePrefsSave} disabled={saving}
                className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
                {saving ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />}
                {saved ? "Saved!" : saving ? "Saving..." : "Save Preferences"}
              </button>
            </>
          ) : (
            <p className="text-sm text-muted-foreground py-4">Unable to load notification preferences.</p>
          )}
        </section>
      )}

      {activeTab === "privacy" && (
        <section className="rounded-2xl border border-border bg-card p-6 shadow-xs space-y-4">
          <h2 className="text-base font-semibold text-foreground">Privacy & Security</h2>
          <div className="space-y-3">
            <div className="rounded-xl border border-border p-4">
              <p className="text-sm font-medium text-foreground">Password</p>
              <p className="text-xs text-muted-foreground">Last changed: Never</p>
              <button className="mt-2 text-xs font-medium text-primary hover:underline">Change Password</button>
            </div>
            <div className="rounded-xl border border-border p-4">
              <p className="text-sm font-medium text-foreground">Two-Factor Authentication</p>
              <p className="text-xs text-muted-foreground">Add an extra layer of security to your account</p>
              <button className="mt-2 text-xs font-medium text-primary hover:underline">Enable 2FA</button>
            </div>
            <div className="rounded-xl border border-border p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-foreground">Share Progress with Teachers</p>
                  <p className="text-xs text-muted-foreground">Allow teachers to see parent engagement data</p>
                </div>
                <input type="checkbox" defaultChecked className="size-4 rounded border-border accent-primary" />
              </div>
            </div>
          </div>
        </section>
      )}
    </div>
  )
}
