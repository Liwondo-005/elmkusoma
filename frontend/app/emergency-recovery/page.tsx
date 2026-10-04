"use client"

import { useState } from "react"
import Link from "next/link"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"
import { AlertCircle, CheckCircle, LifeBuoy } from "lucide-react"
import { authApi } from "@/lib/api"
import { useTranslations } from "next-intl"

export default function EmergencyRecoveryPage() {
  const t = useTranslations("auth")
  const [done, setDone] = useState(false)
  const [serverError, setServerError] = useState("")

  const emergencySchema = z
    .object({
      email: z.string().min(1, t("emailRequired")).email(t("invalidEmail")),
      recoveryCode: z.string().min(1, t("fieldRequired")),
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

  type EmergencyValues = z.infer<typeof emergencySchema>

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<EmergencyValues>({
    resolver: zodResolver(emergencySchema),
  })

  async function onSubmit(values: EmergencyValues) {
    setServerError("")
    try {
      // Uniform outcome by design: success and failure look alike except here,
      // where only a genuinely consumed code yields done=true.
      await authApi.emergencyRecover({
        email: values.email,
        recoveryCode: values.recoveryCode,
        newPassword: values.password,
      })
      setDone(true)
    } catch {
      // Deliberately generic: must not reveal whether the account or code exists.
      setServerError(t("emergencyFailed"))
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
                  {t("emergencySuccessTitle")}
                </h1>
                <p className="mt-2 text-sm text-muted-foreground">
                  {t("emergencySuccessMessage")}
                </p>
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
                    <LifeBuoy className="size-5" />
                  </div>
                  <h1 className="mt-4 text-2xl font-bold tracking-tight text-foreground">
                    {t("emergencyTitle")}
                  </h1>
                  <p className="mt-2 text-sm text-muted-foreground">{t("emergencySubtitle")}</p>
                </div>

                <form className="mt-8 space-y-5" onSubmit={handleSubmit(onSubmit)}>
                  <div>
                    <label htmlFor="email" className="block text-sm font-medium text-foreground">
                      {t("emailAddress")}
                    </label>
                    <input
                      id="email"
                      type="email"
                      placeholder="you@example.com"
                      {...register("email")}
                      className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                    />
                    {errors.email && (
                      <p className="mt-1.5 text-xs text-destructive">{errors.email.message}</p>
                    )}
                  </div>

                  <div>
                    <label
                      htmlFor="recoveryCode"
                      className="block text-sm font-medium text-foreground"
                    >
                      {t("emergencyCodeLabel")}
                    </label>
                    <input
                      id="recoveryCode"
                      type="text"
                      autoComplete="off"
                      placeholder={t("emergencyCodePlaceholder")}
                      {...register("recoveryCode")}
                      className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm uppercase tracking-widest text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                    />
                    {errors.recoveryCode && (
                      <p className="mt-1.5 text-xs text-destructive">
                        {errors.recoveryCode.message}
                      </p>
                    )}
                  </div>

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
                    {isSubmitting ? t("emergencyRecovering") : t("emergencyTitle")}
                  </Button>
                </form>

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
