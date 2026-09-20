"use client"

import { useEffect, useState } from "react"
import { Globe, Loader2, Search, ArrowRight } from "lucide-react"
import Link from "next/link"
import { platformAdminApi, type UserSummary } from "@/lib/platform-admin-api"

export default function ProvidersPage() {
  const [providers, setProviders] = useState<UserSummary[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    platformAdminApi.listUsers(0, 100, "PROVIDER_ADMIN")
      .then((res) => setProviders(res.content))
      .catch(() => setProviders([]))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return (
    <div className="flex items-center justify-center py-32">
      <Loader2 className="size-8 animate-spin text-primary" />
    </div>
  )

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Provider Ecosystem</h1>
        <p className="mt-1 text-sm text-muted-foreground">Organizations, content providers, and service providers</p>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="text-base font-semibold text-foreground">About Providers</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Providers are organizations beyond traditional schools that use ELMKUSOMA to deliver services.
          This includes banks, companies, NGOs, government institutions, training organizations,
          content creators, event organizers, and skills providers.
        </p>
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          {["Banks & Financial", "Companies & Corporate", "NGOs & Non-Profit", "Government Institutions", "Training Organizations", "Content Creators"].map((type) => (
            <div key={type} className="flex items-center gap-2 rounded-lg border border-border px-3 py-2">
              <Globe className="size-3.5 text-muted-foreground" />
              <span className="text-sm text-foreground">{type}</span>
            </div>
          ))}
        </div>
      </div>

      <div>
        <h2 className="text-base font-semibold text-foreground mb-4">Registered Provider Admins</h2>
        {providers.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-border p-12 text-center">
            <Globe className="mx-auto size-10 text-muted-foreground" />
            <p className="mt-4 text-sm font-medium text-foreground">No providers registered yet</p>
            <p className="mt-1 text-xs text-muted-foreground">Provider accounts will appear here once onboarded</p>
          </div>
        ) : (
          <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Name</th>
                  <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Email</th>
                  <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {providers.map((p) => (
                  <tr key={p.id} className="hover:bg-muted/30 transition-colors">
                    <td className="px-5 py-3.5 text-sm font-medium text-foreground">{p.firstName} {p.lastName}</td>
                    <td className="px-5 py-3.5 text-sm text-muted-foreground">{p.email}</td>
                    <td className="px-5 py-3.5">
                      <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                        p.isActive ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"
                      }`}>{p.isActive ? "Active" : "Inactive"}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
