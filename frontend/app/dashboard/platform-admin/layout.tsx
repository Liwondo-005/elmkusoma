"use client"

import type { ReactNode } from "react"
import { PlatformAdminSidebar } from "@/components/dashboard/platform-admin-sidebar"
import { DashboardTopbar } from "@/components/dashboard/dashboard-topbar"
import { AuthGuard } from "@/components/auth/auth-guard"
import { useAuth } from "@/lib/auth"
import { useRouter } from "next/navigation"
import { useEffect } from "react"

export default function PlatformAdminLayout({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const router = useRouter()

  useEffect(() => {
    if (user && user.role !== "Admin") {
      router.replace("/dashboard")
    }
  }, [user, router])

  if (user && user.role !== "Admin") return null

  return (
    <AuthGuard>
      <div className="flex min-h-dvh bg-muted/40">
        <aside className="fixed inset-y-0 left-0 hidden w-64 border-r border-border lg:block">
          <PlatformAdminSidebar />
        </aside>
        <div className="flex min-w-0 flex-1 flex-col lg:pl-64">
          <DashboardTopbar />
          <main className="flex-1 p-4 sm:p-6">{children}</main>
        </div>
      </div>
    </AuthGuard>
  )
}
