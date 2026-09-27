"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState } from "react"
import { Settings, Loader2, Save } from "lucide-react"
import { platformAdminApi, type PlatformConfigItem } from "@/lib/platform-admin-api"

export default function ConfigPage() {
  const t = useTranslations("platformAdmin");
  const [configs, setConfigs] = useState<PlatformConfigItem[]>([])
  const [loading, setLoading] = useState(true)
  const [edits, setEdits] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState<string | null>(null)

  useEffect(() => {
    platformAdminApi.listConfig().then(setConfigs).finally(() => setLoading(false))
  }, [])

  async function handleSave(key: string) {
    const value = edits[key]
    if (value === undefined) return
    setSaving(key)
    try {
      const updated = await platformAdminApi.updateConfig(key, value)
      setConfigs(prev => prev.map(c => c.configKey === key ? { ...c, configValue: updated.configValue } : c))
      setEdits(prev => { const n = { ...prev }; delete n[key]; return n })
    } finally { setSaving(null) }
  }

  const categories = [...new Set(configs.map(c => c.category).filter(Boolean))]

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("config.platformConfiguration")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("config.managePlatformWideSettings")}</p>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : configs.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-10 text-center">
          <Settings className="mx-auto size-10 text-muted-foreground/50" />
          <p className="mt-3 text-sm text-muted-foreground">{t("config.noConfigurationEntriesFound")}</p>
        </div>
      ) : (
        <div className="space-y-6">
          {categories.map(cat => (
            <div key={cat} className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
              <div className="border-b border-border bg-muted/30 px-5 py-3">
                <h2 className="text-sm font-semibold text-foreground">{cat || t("config.general")}</h2>
              </div>
              <div className="divide-y divide-border">
                {configs.filter(c => c.category === cat).map(c => (
                  <div key={c.id} className="flex items-center justify-between gap-4 px-5 py-3">
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium text-foreground">{c.configKey}</p>
                      {c.description && <p className="text-xs text-muted-foreground">{c.description}</p>}
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      {c.isSensitive ? (
                        <span className="text-xs text-muted-foreground font-mono">****</span>
                      ) : (
                        <input
                          value={edits[c.configKey] ?? c.configValue ?? ""}
                          onChange={e => setEdits({ ...edits, [c.configKey]: e.target.value })}
                          className="w-48 rounded-lg border border-border bg-background px-3 py-1.5 text-xs font-mono"
                        />
                      )}
                      {edits[c.configKey] !== undefined && (
                        <button onClick={() => handleSave(c.configKey)} disabled={saving === c.configKey} className="rounded-lg bg-primary px-2 py-1.5 text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
                          {saving === c.configKey ? <Loader2 className="size-3 animate-spin" /> : <Save className="size-3" />}
                        </button>
                      )}
                      <span className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${c.isPublic ? "bg-green-50 text-green-700" : "bg-muted text-muted-foreground"}`}>
                        {c.isPublic ? t("config.public") : t("config.private")}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
