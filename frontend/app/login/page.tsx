"use client"

import { Suspense, useState } from "react"
import Link from "next/link"
import { useSearchParams, useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { Eye, EyeOff } from "lucide-react"

function LoginForm() {
  const t = useTranslations("auth")
  const [showPassword, setShowPassword] = useState(false)
  const [serverError, setServerError] = useState("")
  const [mfaToken, setMfaToken] = useState<string | null>(null)
  const [mfaCode, setMfaCode] = useState("")
  const [mfaBusy, setMfaBusy] = useState(false)
  const { login, completeMfaLogin } = useAuth()
  const router = useRouter()
  const searchParams = useSearchParams()
  const redirect = searchParams.get("redirect") || "/dashboard"

  const loginSchema = z.object({
    email: z.string().min(1, t("emailRequired")).email(t("invalidEmail")),
    password: z.string().min(1, t("passwordRequired")).min(6, t("passwordTooShort")),
  })

  type LoginValues = z.infer<typeof loginSchema>

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
  })

  async function onSubmit(values: LoginValues) {
    setServerError("")
    const result = await login(values.email, values.password)
    if (result.error) {
      setServerError(result.error)
      return
    }
    if (result.mfaRequired) {
      // Password accepted, but the account holds a verified authenticator:
      // only a short-lived challenge was issued — complete step two below.
      setMfaToken(result.mfaToken || "")
      return
    }
    router.push(redirect)
  }

  async function onMfaSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!mfaToken) return
    setMfaBusy(true)
    setServerError("")
    const result = await completeMfaLogin(mfaToken, mfaCode.trim())
    setMfaBusy(false)
    if (result.error) {
      setServerError(result.error)
      return
    }
    router.push(redirect)
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="relative z-10 border-b border-border bg-background/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center px-4 sm:px-6 lg:px-8">
          <Logo />
        </div>
      </header>

      <main className="relative flex-1 flex items-center justify-center px-4 py-12">
        <div
          className="absolute inset-0 bg-cover bg-center bg-no-repeat"
          style={{ backgroundImage: "url('/images/login-bg.jpg')" }}
        />
        <div className="absolute inset-0 bg-foreground/60" />
        <div className="relative z-10 w-full max-w-md">
          <div className="rounded-2xl border border-border bg-card/95 backdrop-blur-sm p-8 shadow-lg">
            <div className="text-center">
              <h1 className="text-2xl font-bold tracking-tight text-foreground">
                {t("welcomeBack")}
              </h1>
              <p className="mt-2 text-sm text-muted-foreground">
                {t("loginToContinue")}
              </p>
            </div>

            {serverError && (
              <div className="mt-6 rounded-lg border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">
                {serverError}
              </div>
            )}

            {mfaToken ? (
              <form className="mt-8 space-y-5" onSubmit={onMfaSubmit}>
                <div>
                  <label htmlFor="mfaCode" className="block text-sm font-medium text-foreground">
                    {t("mfaCodeLabel")}
                  </label>
                  <input
                    id="mfaCode"
                    type="text"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    maxLength={6}
                    placeholder="000000"
                    value={mfaCode}
                    onChange={(e) => setMfaCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
                    className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-center text-lg tracking-[0.5em] text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                  />
                  <p className="mt-1.5 text-xs text-muted-foreground">{t("mfaCodeHint")}</p>
                </div>

                <Button type="submit" className="h-11 w-full text-sm" disabled={mfaBusy || mfaCode.length !== 6}>
                  {mfaBusy ? t("verifying") : t("verifyAndSignIn")}
                </Button>

                <button
                  type="button"
                  onClick={() => {
                    setMfaToken(null)
                    setMfaCode("")
                    setServerError("")
                  }}
                  className="w-full text-center text-sm text-muted-foreground hover:text-foreground"
                >
                  {t("backToLogin")}
                </button>
              </form>
            ) : (
            <form className="mt-8 space-y-5" onSubmit={handleSubmit(onSubmit)}>
              <div className="space-y-4">
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
                  <div className="flex items-center justify-between">
                    <label htmlFor="password" className="text-sm font-medium text-foreground">
                      {t("password")}
                    </label>
                    <Link
                      href="/forgot-password"
                      className="text-xs font-medium text-primary hover:underline"
                    >
                      {t("forgotPassword")}?
                    </Link>
                  </div>
                  <div className="relative mt-1.5">
                    <input
                      id="password"
                      type={showPassword ? "text" : "password"}
                      placeholder={t("passwordPlaceholder")}
                      {...register("password")}
                      className="h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 pr-11 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((v) => !v)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-muted-foreground hover:text-foreground"
                      tabIndex={-1}
                      aria-label={t("togglePasswordVisibility")}
                    >
                      {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                    </button>
                  </div>
                  {errors.password && (
                    <p className="mt-1.5 text-xs text-destructive">{errors.password.message}</p>
                  )}
                </div>
              </div>

              <Button type="submit" className="h-11 w-full text-sm" disabled={isSubmitting}>
                {isSubmitting ? t("signingIn") : t("signIn")}
              </Button>
            </form>
            )}

            <p className="mt-6 text-center text-sm text-muted-foreground">
              {t("dontHaveAccount")}{" "}
              <Link href="/register" className="font-medium text-primary hover:underline">
                {t("register")}
              </Link>
            </p>
          </div>
        </div>
      </main>
    </div>
  )
}

export default function LoginPage() {
  return (
    <Suspense>
      <LoginForm />
    </Suspense>
  )
}
