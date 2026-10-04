"use client"

import { Suspense, useEffect, useState } from "react"
import Link from "next/link"
import { useSearchParams } from "next/navigation"
import { Logo } from "@/components/logo"
import { CheckCircle, AlertCircle, Loader2 } from "lucide-react"
import { authApi } from "@/lib/api"
import { useTranslations } from "next-intl"

function VerifyEmailForm() {
  const t = useTranslations("auth")
  const searchParams = useSearchParams()
  const token = searchParams.get("token") || ""
  const [state, setState] = useState<"working" | "done" | "failed" | "missing">(
    token ? "working" : "missing",
  )

  useEffect(() => {
    if (!token) return
    let cancelled = false
    authApi
      .verifyEmail(token)
      .then(() => {
        if (!cancelled) setState("done")
      })
      .catch(() => {
        if (!cancelled) setState("failed")
      })
    return () => {
      cancelled = true
    }
  }, [token])

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="relative z-10 border-b border-border bg-background/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center px-4 sm:px-6 lg:px-8">
          <Logo />
        </div>
      </header>

      <main className="relative flex flex-1 items-center justify-center px-4 py-12">
        <div
          className="absolute inset-0 bg-cover bg-center bg-no-repeat"
          style={{ backgroundImage: "url('/images/forgot-bg.jpg')" }}
        />
        <div className="absolute inset-0 bg-black/60" />
        <div className="relative z-10 w-full max-w-md">
          <div className="rounded-2xl border border-white/10 bg-card/95 p-8 text-center shadow-lg backdrop-blur-sm">
            {state === "working" && (
              <>
                <Loader2 className="mx-auto size-10 animate-spin text-primary" />
                <p className="mt-4 text-sm text-muted-foreground">{t("verifyingEmail")}</p>
              </>
            )}
            {state === "missing" && (
              <>
                <AlertCircle className="mx-auto size-10 text-destructive" />
                <h1 className="mt-4 text-2xl font-bold tracking-tight text-foreground">
                  {t("verifyEmailTitle")}
                </h1>
                <p className="mt-2 text-sm text-muted-foreground">{t("verifyEmailMissing")}</p>
              </>
            )}
            {state === "done" && (
              <>
                <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-teal/10">
                  <CheckCircle className="size-7 text-teal" />
                </div>
                <h1 className="mt-4 text-2xl font-bold tracking-tight text-foreground">
                  {t("verifyEmailSuccessTitle")}
                </h1>
                <p className="mt-2 text-sm text-muted-foreground">
                  {t("verifyEmailSuccessMessage")}
                </p>
              </>
            )}
            {state === "failed" && (
              <>
                <AlertCircle className="mx-auto size-10 text-destructive" />
                <h1 className="mt-4 text-2xl font-bold tracking-tight text-foreground">
                  {t("verifyEmailTitle")}
                </h1>
                <p className="mt-2 text-sm text-muted-foreground">{t("verifyEmailFailed")}</p>
              </>
            )}
            {state !== "working" && (
              <Link
                href="/login"
                className="mt-6 inline-flex h-11 items-center justify-center rounded-lg bg-primary px-6 text-sm font-medium text-primary-foreground hover:bg-primary/90"
              >
                {t("backToLogin")}
              </Link>
            )}
          </div>
        </div>
      </main>
    </div>
  )
}

export default function VerifyEmailPage() {
  return (
    <Suspense>
      <VerifyEmailForm />
    </Suspense>
  )
}
