"use client"

import { useEffect, useState } from "react"
import { ArrowLeft, Globe, Loader2, Shield, Users, Mail, Phone, MapPin } from "lucide-react"
import Link from "next/link"
import { useParams } from "next/navigation"
import { platformAdminApi, type UserSummary, type PageResponse } from "@/lib/platform-admin-api"

export default function ProviderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [provider, setProvider] = useState<UserSummary | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!id) return
    platformAdminApi.getUser(id).then(setProvider).catch(() => setProvider(null)).finally(() => setLoading(false))
  }, [id])

  if (loading) return (
    <div className="flex items-center justify-center py-32"><Loader2 className="size-8 animate-spin text-primary" /></div>
  )

  if (!provider) return (
    <div className="mx-auto max-w-4xl py-10 text-center">
      <Globe className="mx-auto size-10 text-muted-foreground" />
      <p className="mt-4 text-sm text-muted-foreground">Provider not found</p>
      <Link href="/dashboard/platform-admin/providers" className="mt-4 inline-flex items-center gap-2 text-sm text-primary hover:underline">
        <ArrowLeft className="size-4" /> Back to Providers
      </Link>
    </div>
  )

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <Link href="/dashboard/platform-admin/providers" className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> Back to Providers
      </Link>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-start gap-4">
          <div className="flex size-14 items-center justify-center rounded-2xl bg-primary/10">
            <Globe className="size-7 text-primary" />
          </div>
          <div className="flex-1">
            <h1 className="text-xl font-bold tracking-tight text-foreground">{provider.firstName} {provider.lastName}</h1>
            <p className="text-sm text-muted-foreground">{provider.email}</p>
            <div className="mt-3 flex items-center gap-3">
              <span className={`rounded-full px-3 py-1 text-xs font-semibold ${provider.isActive ? "bg-green-50 text-green-700" : "bg-red-50 text-red-700"}`}>
                {provider.isActive ? "Active" : "Inactive"}
              </span>
              <span className="rounded-full bg-muted px-3 py-1 text-xs font-medium">{provider.role}</span>
            </div>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => platformAdminApi.updateUserStatus(provider.id, !provider.isActive).then(setProvider)}
              className={`rounded-xl border px-4 py-2 text-xs font-medium ${provider.isActive ? "border-red-200 text-red-600 hover:bg-red-50" : "border-green-200 text-green-600 hover:bg-green-50"}`}
            >
              {provider.isActive ? "Suspend" : "Activate"}
            </button>
          </div>
        </div>
      </div>

      <div className="grid gap-6 sm:grid-cols-2">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground mb-4">Provider Details</h2>
          <div className="space-y-3">
            <div className="flex items-center gap-3 text-sm"><Mail className="size-4 text-muted-foreground" /><span className="text-foreground">{provider.email}</span></div>
            <div className="flex items-center gap-3 text-sm"><Users className="size-4 text-muted-foreground" /><span className="text-muted-foreground">Role: {provider.role}</span></div>
            <div className="flex items-center gap-3 text-sm"><Shield className="size-4 text-muted-foreground" /><span className="text-muted-foreground">ID: {provider.id?.slice(0, 8)}...</span></div>
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground mb-4">Account Info</h2>
          <div className="space-y-3">
            <div className="flex justify-between text-sm"><span className="text-muted-foreground">Status</span><span className={`font-medium ${provider.isActive ? "text-green-600" : "text-red-600"}`}>{provider.isActive ? "Active" : "Inactive"}</span></div>
            <div className="flex justify-between text-sm"><span className="text-muted-foreground">Created</span><span className="text-foreground">{provider.createdAt ? new Date(provider.createdAt).toLocaleDateString("en-GB") : "N/A"}</span></div>
          </div>
        </div>
      </div>
    </div>
  )
}
