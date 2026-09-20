"use client"

import { useEffect, useState } from "react"
import { Settings, Database, Server, Shield, Globe, Zap, Loader2, Save } from "lucide-react"
import { platformAdminApi, type PlatformConfigItem } from "@/lib/platform-admin-api"

function InfoCard({ icon: Icon, label, value, color }: { icon: any; label: string; value: string; color: string }) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className="flex items-center gap-3">
        <div className={`flex size-10 items-center justify-center rounded-xl ${color}`}>
          <Icon className="size-5" />
        </div>
        <div>
          <p className="text-sm text-muted-foreground">{label}</p>
          <p className="text-lg font-bold text-foreground">{value}</p>
        </div>
      </div>
    </div>
  )
}

function SettingRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between rounded-lg border border-border px-4 py-3">
      <span className="text-sm font-medium text-foreground">{label}</span>
      <span className="text-sm text-muted-foreground">{value}</span>
    </div>
  )
}

export default function PlatformSettingsPage() {
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
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Platform Settings</h1>
        <p className="mt-1 text-sm text-muted-foreground">System information and platform configuration</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <InfoCard icon={Globe} label="Platform" value="ELMKUSOMA" color="bg-blue-100 text-blue-700" />
        <InfoCard icon={Server} label="Environment" value="Development" color="bg-amber-100 text-amber-700" />
        <InfoCard icon={Database} label="Database" value="PostgreSQL 18" color="bg-emerald-100 text-emerald-700" />
        <InfoCard icon={Shield} label="Auth" value="JWT + RBAC" color="bg-purple-100 text-purple-700" />
        <InfoCard icon={Zap} label="Realtime" value="WebSocket" color="bg-red-100 text-red-700" />
        <InfoCard icon={Server} label="Architecture" value="Modular Monolith" color="bg-cyan-100 text-cyan-700" />
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-base font-semibold text-foreground mb-4">System Information</h2>
        <div className="space-y-2">
          <SettingRow label="Platform Name" value="ELMKUSOMA" />
          <SettingRow label="Backend" value="Spring Boot 3 / Java 21" />
          <SettingRow label="Frontend" value="Next.js 16 / React" />
          <SettingRow label="Database" value="PostgreSQL 18" />
          <SettingRow label="Cache" value="Redis" />
          <SettingRow label="Message Queue" value="RabbitMQ" />
          <SettingRow label="Video" value="LiveKit" />
          <SettingRow label="Storage" value="MinIO" />
          <SettingRow label="Migration" value="Flyway (63 migrations)" />
          <SettingRow label="ORM" value="Hibernate / JPA" />
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-base font-semibold text-foreground mb-4">Security Configuration</h2>
        <div className="space-y-2">
          <SettingRow label="Authentication" value="JWT with refresh tokens (1h access / 7d refresh)" />
          <SettingRow label="Password Hashing" value="BCrypt" />
          <SettingRow label="Authorization" value="Role-Based Access Control (12 roles)" />
          <SettingRow label="API Protection" value="@PreAuthorize on all admin endpoints" />
          <SettingRow label="Multi-Tenancy" value="Institution-scoped data isolation" />
          <SettingRow label="Soft Delete" value="Enabled (is_deleted flag)" />
          <SettingRow label="Audit Logging" value="Enabled for all critical actions" />
          <SettingRow label="IDOR Prevention" value="InstitutionScopeService validation" />
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-8"><Loader2 className="size-5 animate-spin text-muted-foreground" /></div>
      ) : configs.length > 0 && (
        <div className="space-y-4">
          <h2 className="text-base font-semibold text-foreground">Live Configuration</h2>
          {categories.map(cat => (
            <div key={cat} className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
              <div className="border-b border-border bg-muted/30 px-5 py-3">
                <h3 className="text-sm font-semibold text-foreground">{cat}</h3>
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
