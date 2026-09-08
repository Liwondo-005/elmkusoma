"use client"

import { useEffect, useState } from "react"
import { Settings, Loader2, Plus, Save, X } from "lucide-react"
import { adminApi, getInstitutionId, type SettingResponse, type SettingRequest } from "@/lib/api"

export default function AdminSettingsPage() {
  const [settings, setSettings] = useState<SettingResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editingKey, setEditingKey] = useState<string | null>(null)
  const [editValue, setEditValue] = useState("")
  const [showNew, setShowNew] = useState(false)
  const [newKey, setNewKey] = useState("")
  const [newValue, setNewValue] = useState("")
  const [newDescription, setNewDescription] = useState("")
  const [saving, setSaving] = useState(false)

  const institutionId = getInstitutionId()

  useEffect(() => {
    if (!institutionId) {
      setError("No institution context found.")
      setLoading(false)
      return
    }
    adminApi
      .listSettings(institutionId)
      .then(setSettings)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load settings"))
      .finally(() => setLoading(false))
  }, [institutionId])

  async function handleSave(key: string, value: string) {
    if (!institutionId) return
    setSaving(true)
    try {
      let parsed: Record<string, unknown>
      try {
        parsed = JSON.parse(value)
      } catch {
        parsed = { value }
      }
      const data: SettingRequest = { settingKey: key, settingValue: parsed }
      const updated = await adminApi.updateSetting(institutionId, data)
      setSettings((prev) => {
        const idx = prev.findIndex((s) => s.settingKey === key)
        if (idx >= 0) {
          const next = [...prev]
          next[idx] = updated
          return next
        }
        return [...prev, updated]
      })
      setEditingKey(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save setting")
    } finally {
      setSaving(false)
    }
  }

  async function handleCreate() {
    if (!institutionId || !newKey.trim()) return
    setSaving(true)
    try {
      let parsed: Record<string, unknown>
      try {
        parsed = JSON.parse(newValue)
      } catch {
        parsed = { value: newValue }
      }
      const data: SettingRequest = {
        settingKey: newKey.trim(),
        settingValue: parsed,
        description: newDescription || undefined,
      }
      const created = await adminApi.updateSetting(institutionId, data)
      setSettings((prev) => [...prev, created])
      setShowNew(false)
      setNewKey("")
      setNewValue("")
      setNewDescription("")
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create setting")
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Settings</h1>
          <p className="mt-1 text-sm text-muted-foreground">Manage institution settings.</p>
        </div>
        <button
          onClick={() => setShowNew(true)}
          className="flex h-9 items-center gap-2 rounded-lg bg-primary px-4 text-xs font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-3.5" /> Add Setting
        </button>
      </div>

      {loading && (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center">
          <p className="text-sm font-medium text-destructive">{error}</p>
        </div>
      )}

      {showNew && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h3 className="mb-4 text-sm font-semibold text-foreground">New Setting</h3>
          <div className="space-y-3">
            <input
              type="text"
              placeholder="Setting key (e.g. school_name)"
              value={newKey}
              onChange={(e) => setNewKey(e.target.value)}
              className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
            />
            <input
              type="text"
              placeholder="Description (optional)"
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
              className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
            />
            <textarea
              placeholder='Value (JSON or plain text, e.g. {"key": "value"} or "My School")'
              value={newValue}
              onChange={(e) => setNewValue(e.target.value)}
              rows={3}
              className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring"
            />
            <div className="flex gap-2">
              <button
                onClick={handleCreate}
                disabled={saving || !newKey.trim()}
                className="flex h-9 items-center gap-2 rounded-lg bg-primary px-4 text-xs font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
              >
                <Save className="size-3.5" /> {saving ? "Saving..." : "Save"}
              </button>
              <button
                onClick={() => { setShowNew(false); setNewKey(""); setNewValue(""); setNewDescription("") }}
                className="flex h-9 items-center gap-2 rounded-lg border border-border px-4 text-xs font-medium text-foreground hover:bg-muted"
              >
                <X className="size-3.5" /> Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {!loading && !error && settings.length === 0 && (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <Settings className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">No settings configured</p>
          <p className="mt-1 text-sm text-muted-foreground">Add settings to customize your institution.</p>
        </div>
      )}

      {!loading && settings.length > 0 && (
        <div className="space-y-3">
          {settings.map((setting) => (
            <div key={setting.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-foreground">{setting.settingKey}</span>
                    {setting.settingType && (
                      <span className="rounded bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                        {setting.settingType}
                      </span>
                    )}
                    {setting.isPublic && (
                      <span className="rounded bg-teal/10 px-2 py-0.5 text-[10px] font-medium text-teal">Public</span>
                    )}
                  </div>
                  {setting.description && (
                    <p className="mt-1 text-xs text-muted-foreground">{setting.description}</p>
                  )}
                  {editingKey === setting.settingKey ? (
                    <div className="mt-3 space-y-2">
                      <textarea
                        value={editValue}
                        onChange={(e) => setEditValue(e.target.value)}
                        rows={3}
                        className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring"
                      />
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleSave(setting.settingKey, editValue)}
                          disabled={saving}
                          className="flex h-8 items-center gap-1.5 rounded-lg bg-primary px-3 text-xs font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                        >
                          <Save className="size-3" /> Save
                        </button>
                        <button
                          onClick={() => setEditingKey(null)}
                          className="flex h-8 items-center gap-1.5 rounded-lg border border-border px-3 text-xs font-medium text-foreground hover:bg-muted"
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  ) : (
                    <pre className="mt-2 max-h-32 overflow-auto rounded-lg bg-muted/50 px-3 py-2 text-xs text-foreground">
                      {JSON.stringify(setting.settingValue, null, 2)}
                    </pre>
                  )}
                </div>
                {editingKey !== setting.settingKey && (
                  <button
                    onClick={() => {
                      setEditingKey(setting.settingKey)
                      setEditValue(JSON.stringify(setting.settingValue, null, 2))
                    }}
                    className="shrink-0 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted"
                  >
                    Edit
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
