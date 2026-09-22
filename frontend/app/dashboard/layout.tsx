"use client"

import type { ReactNode } from "react"
import { useState, useEffect } from "react"
import { NextIntlClientProvider } from "next-intl"
import { DashboardSidebar } from "@/components/dashboard/dashboard-sidebar"
import { DashboardTopbar } from "@/components/dashboard/dashboard-topbar"
import { AuthGuard } from "@/components/auth/auth-guard"
import { LowBandwidthProvider } from "@/components/primary/low-bandwidth-provider"

import en from "../../messages/en.json"
import sw from "../../messages/sw.json"

const messages = { en, sw }

export default function DashboardLayout({ children }: { children: ReactNode }) {
  return (
    <NextIntlClientProvider locale="en" messages={messages.en}>
      <AuthGuard>
        <LowBandwidthProvider>
          <div className="flex min-h-dvh bg-muted/40">
            <aside className="fixed inset-y-0 left-0 hidden z-40 w-64 border-r border-border bg-card lg:block">
              <DashboardSidebar />
            </aside>
            <div className="flex min-w-0 flex-1 flex-col lg:pl-64">
              <DashboardTopbar />
              <main className="flex-1 p-4 sm:p-6">{children}</main>
            </div>
          </div>
        </LowBandwidthProvider>
      </AuthGuard>
    </NextIntlClientProvider>
  )
}
