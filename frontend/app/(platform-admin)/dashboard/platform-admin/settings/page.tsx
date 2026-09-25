"use client"

import { useEffect, useState, useCallback } from "react"
import { Settings, Database, Server, Shield, Globe, Zap, Loader2, Save, Flag, Rocket, AlertCircle, RefreshCw } from "lucide-react"
import { platformAdminApi, type PlatformConfigItem, type FeatureStatus, type PolicyFlag } from "@/lib/platform-admin-api"

const FEATURE_STATUSES = ["PLANNED", "DEVELOPMENT", "TESTING", "ROLLOUT", "ACTIVE", "DEPRECATED", "RETIRED"] as const

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
  const [features, setFeatures] = useState<FeatureStatus[]>([])
  const [policies, setPolicies] = useState<PolicyFlag[]>([])
  const [govError, setGovError] = useState<string | null>(null)
  const [busyFeature, setBusyFeature] = useState<string | null>(null)

  const loadGov = useCallback(async () => {
    try {
      const [f, p] = await Promise.all([
        platformAdminApi.listFeatures(),
        platformAdminApi.listPolicies(),
      ])
      setFeatures(f); setPolicies(p)
    } catch (e: any) {
      setGovError(e.message || "Features/policies unavailable")
    }
  }, [])

  useEffect(() => {
    platformAdminApi.listConfig().then(setConfigs).finally(() => setLoading(false))
    loadGov()
  }, [loadGov])

  async function handleSave(key: string) {
    const value = edits[key]
    if (value === undefined) return
    setSaving(key)
    try {
      const updated = await platformAdminApi.updateConfig(key, value)
      setConfigs(prev => prev.map(c => c.configKey === key ? { ...c, configValue: updated.configValue } : c))
      setEdits(prev => { const n = { ...prev }; delete n[key]; return n })
      if (key === "platform.maintenance.enabled") loadGov()
    } finally { setSaving(null) }
  }

  const transitionFeature = async (key: string, status: string) => {
    setBusyFeature(key); setGovError(null)
    try {
      const updated = await platformAdminApi.updateFeatureStatus(key, status)
      setFeatures(prev => prev.map(f => (f.key === key ? updated : f)))
    } catch (e: any) {
      setGovError(e.message || "Feature transition rejected")
    } finally { setBusyFeature(null) }
  }

  const togglePolicy = async (key: string, current: string) => {
    setGovError(null)
    try {
      const next = current !== "true"
      const updated = await platformAdminApi.updatePolicy(key, next)
      setPolicies(prev => prev.map(p => (p.key === key ? updated : p)))
    } catch (e: any) {
      setGovError(e.message || "Policy update failed")
    }
  }

  const categories = [...new Set(configs.map(c => c.category).filter(Boolean))]

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Platform Settings</h1>
        <p className="mt-1 text-sm text-muted-foreground">Configuration, central policies, feature lifecycle, maintenance mode</p>
      </div>

      {govError && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{govError}</span>
          <button onClick={loadGov} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">Retry</button>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <InfoCard icon={Globe} label="Platform" value="ELMKUSOMA" color="bg-blue-100 text-blue-700" />
        <InfoCard icon={Server} label="Environment" value={process.env.NODE_ENV === "production" ? "Production" : "Development"} color="bg-amber-100 text-amber-700" />
        <InfoCard icon={Database} label="Database" value="PostgreSQL 18" color="bg-emerald-100 text-emerald-700" />
        <InfoCard icon={Shield} label="Auth" value="JWT + RBAC" color="bg-purple-100 text-purple-700" />
        <InfoCard icon={Zap} label="Realtime" value="WebSocket" color="bg-red-100 text-red-700" />
        <InfoCard icon={Server} label="Architecture" value="Modular Monolith" color="bg-cyan-100 text-cyan-700" />
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between">
          <h2 className="flex items-center gap-2 text-base font-semibold text-foreground"><Flag className="size-4" /> Central Policies (§62)</h2>
          <button onClick={loadGov} className="text-muted-foreground hover:text-foreground" aria-label="Refresh policies"><RefreshCw className="size-3.5" /></button>
        </div>
        <p className="mt-1 text-xs text-muted-foreground">Defaults are permissive (true). Disabling a policy blocks the matching backend operation.</p>
        <div className="mt-3 grid gap-2 sm:grid-cols-2">
          {policies.length === 0 ? (
            <p className="text-sm text-muted-foreground sm:col-span-2">Data unavailable — policy endpoint did not return flags.</p>
          ) : policies.map(p => (
            <button key={p.key} onClick={() => togglePolicy(p.key, p.value)}
              className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3 text-left hover:bg-muted/40">
              <span>
                <span className="block text-sm font-medium text-foreground">{p.key.replace("policy.", "").replace(".enabled", "").replace(".required", "")}</span>
                <span className="block text-xs text-muted-foreground">{p.description}</span>
              </span>
              <span className={`rounded-full border px-2.5 py-0.5 text-xs font-bold ${p.value === "true" ? "border-emerald-200 bg-emerald-50 text-emerald-700" : "border-red-200 bg-red-50 text-red-700"}`}>
                {p.value === "true" ? "ON" : "OFF"}
              </span>
            </button>
          ))}
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between">
          <h2 className="flex items-center gap-2 text-base font-semibold text-foreground"><Rocket className="size-4" /> Feature Lifecycle (§63)</h2>
          <button onClick={loadGov} className="text-muted-foreground hover:text-foreground" aria-label="Refresh features"><RefreshCw className="size-3.5" /></button>
        </div>
        <div className="mt-3 space-y-2">
          {features.length === 0 ? (
            <p className="text-sm text-muted-foreground">Data unavailable — features not loaded.</p>
          ) : features.map(f => (
            <div key={f.key} className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-border bg-card px-4 py-3">
              <div className="min-w-0">
                <p className="text-sm font-medium text-foreground">{f.name}</p>
                <p className="text-xs text-muted-foreground">{f.description}</p>
              </div>
              <div className="flex items-center gap-2">
                <span className={`rounded-full border px-2.5 py-0.5 text-xs font-bold ${f.status === "ACTIVE" ? "border-emerald-200 bg-emerald-50 text-emerald-700" : f.status === "ROLLOUT" ? "border-amber-200 bg-amber-50 text-amber-700" : "border-slate-200 bg-slate-50 text-slate-600"}`}>
                  {f.status}
                </span>
                <select
                  value=""
                  disabled={busyFeature === f.key}
                  onChange={(e) => { if (e.target.value) transitionFeature(f.key, e.target.value) }}
                  className="rounded-lg border border-border bg-background px-2 py-1 text-xs outline-none"
                >
                  <option value="">Change…</option>
                  {FEATURE_STATUSES.filter(s => s !== f.status).map(s => <option key={s} value={s}>{s}</option>)}
                </select>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-base font-semibold text-foreground mb-4">System Information</h2>
        <div className="space-y-2">
          <SettingRow label="Platform Name" value="ELMKUSOMA" />
          <SettingRow label="Backend" value="Spring Boot 3.4 / Java 17" />
          <SettingRow label="Frontend" value="Next.js 16 / React 19" />
          <SettingRow label="Database" value="PostgreSQL 18" />
          <SettingRow label="Cache" value="Redis" />
          <SettingRow label="Message Queue" value="RabbitMQ" />
          <SettingRow label="Video" value="LiveKit" />
          <SettingRow label="Storage" value="MinIO" />
          <SettingRow label="Migration" value="Flyway (87 migrations)" />
          <SettingRow label="ORM" value="Hibernate / JPA" />
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-base font-semibold text-foreground mb-4">Security Configuration</h2>
        <div className="space-y-2">
          <SettingRow label="Authentication" value="JWT with refresh tokens (1h access / 7d refresh)" />
          <SettingRow label="Password Hashing" value="BCrypt" />
          <SettingRow label="Authorization" value="Role-Based Access Control (13 roles)" />
          <SettingRow label="API Protection" value="Role-based @PreAuthorize on admin APIs" />
          <SettingRow label="Multi-Tenancy" value="Institution-scoped data isolation" />
          <SettingRow label="Soft Delete" value="Enabled (is_deleted flag)" />
          <SettingRow label="Audit Logging" value="Enabled on admin, config & security writes" />
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
