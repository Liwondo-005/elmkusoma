"use client"

import { Suspense, useState } from "react"
import Link from "next/link"
import { useSearchParams } from "next/navigation"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"
import { KeyRound, CheckCircle, AlertCircle } from "lucide-react"
import { authApi } from "@/lib/api"
import { useTranslations } from "next-intl"

function ResetPasswordForm() {
  const t = useTranslations("auth")
  const searchParams = useSearchParams()
  const token = searchParams.get("token") || ""
  const [done, setDone] = useState(false)
  const [serverError, setServerError] = useState("")

  const resetSchema = z
    .object({
      password: z
        .string()
        .min(1, t("passwordRequired"))
        .min(8, t("passwordMin8"))
        .regex(/[A-Z]/, t("passwordUppercase"))
        .regex(/[0-9]/, t("passwordNumber")),
      confirmPassword: z.string().min(1, t("confirmPasswordRequired")),
    })
    .refine((data) => data.password === data.confirmPassword, {
      message: t("passwordsDoNotMatch"),
      path: ["confirmPassword"],
    })

  type ResetValues = z.infer<typeof resetSchema>

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetValues>({
    resolver: zodResolver(resetSchema),
  })

  async function onSubmit(values: ResetValues) {
    setServerError("")
    try {
      await authApi.resetPassword({ token, newPassword: values.password })
      setDone(true)
    } catch (err) {
      const message = err instanceof Error ? err.message : t("resetFailed")
      // Never echo anything token-like; map known backend failures to safe copy.
      if (/invalid|used/i.test(message)) setServerError(t("resetTokenInvalid"))
      else if (/expir/i.test(message)) setServerError(t("resetTokenExpired"))
      else setServerError(t("resetFailed"))
    }
  }

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
          <div className="rounded-2xl border border-white/10 bg-card/95 p-8 shadow-lg backdrop-blur-sm">
            {done ? (
              <div className="text-center">
                <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-teal/10">
                  <CheckCircle className="size-7 text-teal" />
                </div>
                <h1 className="mt-4 text-2xl font-bold tracking-tight text-foreground">
                  {t("resetSuccessTitle")}
                </h1>
                <p className="mt-2 text-sm text-muted-foreground">{t("resetSuccessMessage")}</p>
                <Link
                  href="/login"
                  className="mt-6 inline-flex h-11 items-center justify-center rounded-lg bg-primary px-6 text-sm font-medium text-primary-foreground hover:bg-primary/90"
                >
                  {t("backToLogin")}
                </Link>
              </div>
            ) : (
              <>
                <div className="text-center">
                  <div className="mx-auto flex size-12 items-center justify-center rounded-xl bg-accent text-primary">
                    <KeyRound className="size-5" />
                  </div>
                  <h1 className="mt-4 text-2xl font-bold tracking-tight text-foreground">
                    {t("resetTitle")}
                  </h1>
                  <p className="mt-2 text-sm text-muted-foreground">{t("resetSubtitle")}</p>
                </div>

                {!token ? (
                  <div className="mt-8">
                    <p className="flex items-start gap-2 text-sm text-destructive">
                      <AlertCircle className="mt-0.5 size-4 shrink-0" />
                      {t("resetMissingToken")}
                    </p>
                    <Link
                      href="/forgot-password"
                      className="mt-6 inline-flex h-11 w-full items-center justify-center rounded-lg bg-primary px-6 text-sm font-medium text-primary-foreground hover:bg-primary/90"
                    >
                      {t("requestNewLink")}
                    </Link>
                  </div>
                ) : (
                  <form className="mt-8 space-y-5" onSubmit={handleSubmit(onSubmit)}>
                    <div>
                      <label htmlFor="password" className="block text-sm font-medium text-foreground">
                        {t("password")}
                      </label>
                      <input
                        id="password"
                        type="password"
                        autoComplete="new-password"
                        {...register("password")}
                        className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                      />
                      {errors.password && (
                        <p className="mt-1.5 text-xs text-destructive">{errors.password.message}</p>
                      )}
                    </div>

                    <div>
                      <label
                        htmlFor="confirmPassword"
                        className="block text-sm font-medium text-foreground"
                      >
                        {t("confirmPassword")}
                      </label>
                      <input
                        id="confirmPassword"
                        type="password"
                        autoComplete="new-password"
                        {...register("confirmPassword")}
                        className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                      />
                      {errors.confirmPassword && (
                        <p className="mt-1.5 text-xs text-destructive">
                          {errors.confirmPassword.message}
                        </p>
                      )}
                    </div>

                    {serverError && (
                      <p className="flex items-start gap-2 text-sm text-destructive">
                        <AlertCircle className="mt-0.5 size-4 shrink-0" />
                        {serverError}
                      </p>
                    )}

                    <Button type="submit" className="h-11 w-full text-sm" disabled={isSubmitting}>
                      {isSubmitting ? t("sending") : t("resetPassword")}
                    </Button>
                  </form>
                )}

                <p className="mt-6 text-center text-sm text-muted-foreground">
                  {t("rememberPassword")}{" "}
                  <Link href="/login" className="font-medium text-primary hover:underline">
                    {t("backToLogin")}
                  </Link>
                </p>
              </>
            )}
          </div>
        </div>
      </main>
    </div>
  )
}

export default function ResetPasswordPage() {
  return (
    <Suspense>
      <ResetPasswordForm />
    </Suspense>
  )
}
