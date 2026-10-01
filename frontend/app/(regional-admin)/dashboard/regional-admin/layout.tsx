"use client"

import type { ReactNode } from "react"
import { RegionalAdminSidebar } from "@/components/dashboard/regional-admin-sidebar"
import { DashboardTopbar } from "@/components/dashboard/dashboard-topbar"
import { AuthGuard } from "@/components/auth/auth-guard"
import { LowBandwidthProvider } from "@/components/primary/low-bandwidth-provider"
import { useAuth } from "@/lib/auth"
import { useRouter } from "next/navigation"
import { useEffect } from "react"

// Regional Administration workspace shell. Lives in the (regional-admin) route
// group so no other dashboard shell can wrap these pages. URL is
// /dashboard/regional-admin — proxy.ts gates the prefix for role "Regional Admin"
// (startsWith "/dashboard/regional"), while the backend @PreAuthorize on
// /v1/regional-admin remains the authoritative jurisdiction check.
export default function RegionalAdminLayout({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const router = useRouter()
  const allowed = user?.role === "Regional Admin" || user?.role === "District Admin"

  useEffect(() => {
    if (user && !allowed) router.replace("/dashboard")
  }, [user, allowed, router])

  if (user && !allowed) return null

  return (
    <AuthGuard>
      <LowBandwidthProvider>
        <div className="flex min-h-dvh bg-muted/40">
          <aside className="fixed inset-y-0 left-0 hidden w-64 border-r border-border lg:block">
            <RegionalAdminSidebar />
          </aside>
          <div className="flex min-w-0 flex-1 flex-col lg:pl-64">
            <DashboardTopbar
              renderSidebar={(onNavigate) => <RegionalAdminSidebar onNavigate={onNavigate} />}
            />
            <main className="flex-1 p-4 sm:p-6">{children}</main>
          </div>
        </div>
      </LowBandwidthProvider>
    </AuthGuard>
  )
}
