"use client"

import type { ReactNode } from "react"
import { AdminContextHeader } from "@/components/dashboard/admin-context-header"

export default function AdminWorkspaceLayout({ children }: { children: ReactNode }) {
  return (
    <div className="space-y-6">
      <AdminContextHeader />
      {children}
    </div>
  )
}
