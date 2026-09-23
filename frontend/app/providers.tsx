"use client"

import { AuthProvider } from "@/lib/auth"
import { ToastProvider } from "@/components/toast"
import { ErrorBoundary } from "@/components/error-boundary"
import { LocaleProvider, LocaleMessages } from "@/components/locale-provider"

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <ErrorBoundary>
      <LocaleProvider>
        <AuthProvider>
          <ToastProvider>
            <LocaleMessages>{children}</LocaleMessages>
          </ToastProvider>
        </AuthProvider>
      </LocaleProvider>
    </ErrorBoundary>
  )
}
