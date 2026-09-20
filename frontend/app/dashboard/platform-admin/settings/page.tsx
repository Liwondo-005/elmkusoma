"use client"

import { Settings, Database, Server, Shield, Globe, Zap } from "lucide-react"

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
  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Platform Configuration</h1>
        <p className="mt-1 text-sm text-muted-foreground">Platform-wide settings and system information</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <InfoCard icon={Globe} label="Platform" value="ELMKUSOMA" color="bg-blue-100 text-blue-700" />
        <InfoCard icon={Server} label="Environment" value="Development" color="bg-amber-100 text-amber-700" />
        <InfoCard icon={Database} label="Database" value="PostgreSQL 18" color="bg-emerald-100 text-emerald-700" />
        <InfoCard icon={Shield} label="Auth" value="JWT + RBAC" color="bg-purple-100 text-purple-700" />
        <InfoCard icon={Zap} label="Realtime" value="WebSocket" color="bg-red-100 text-red-700" />
        <InfoCard icon={Server} label="Architecture" value="Microservices" color="bg-cyan-100 text-cyan-700" />
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-base font-semibold text-foreground mb-4">System Information</h2>
        <div className="space-y-2">
          <SettingRow label="Platform Name" value="ELMKUSOMA" />
          <SettingRow label="Version" value="1.0.0" />
          <SettingRow label="Backend" value="Spring Boot 3 / Java 21" />
          <SettingRow label="Frontend" value="Next.js 16 / React" />
          <SettingRow label="Database" value="PostgreSQL 18" />
          <SettingRow label="Cache" value="Redis" />
          <SettingRow label="Message Queue" value="RabbitMQ" />
          <SettingRow label="Video" value="LiveKit" />
          <SettingRow label="Storage" value="MinIO" />
          <SettingRow label="Migration" value="Flyway" />
          <SettingRow label="ORM" value="Hibernate / JPA" />
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-base font-semibold text-foreground mb-4">Security Configuration</h2>
        <div className="space-y-2">
          <SettingRow label="Authentication" value="JWT with refresh tokens" />
          <SettingRow label="Password Hashing" value="BCrypt" />
          <SettingRow label="Authorization" value="Role-Based Access Control" />
          <SettingRow label="API Protection" value="@PreAuthorize on all endpoints" />
          <SettingRow label="Multi-Tenancy" value="Institution-scoped data isolation" />
          <SettingRow label="Soft Delete" value="Enabled (is_deleted flag)" />
          <SettingRow label="Audit Logging" value="Enabled for all critical actions" />
        </div>
      </div>
    </div>
  )
}
