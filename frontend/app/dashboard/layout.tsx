import type { ReactNode } from "react"
import { DashboardSidebar } from "@/components/dashboard/dashboard-sidebar"
import { DashboardTopbar } from "@/components/dashboard/dashboard-topbar"
import { AuthGuard } from "@/components/auth/auth-guard"

export default function DashboardLayout({ children }: { children: ReactNode }) {
  return (
    <AuthGuard>
      <div className="flex min-h-dvh bg-muted/40">
        <aside className="fixed inset-y-0 left-0 hidden w-64 border-r border-border lg:block">
          <DashboardSidebar />
        </aside>
        <div className="flex min-w-0 flex-1 flex-col lg:pl-64">
          <DashboardTopbar />
          <main className="flex-1 p-4 sm:p-6">{children}</main>
        </div>
      </div>
    </AuthGuard>
  )
}
