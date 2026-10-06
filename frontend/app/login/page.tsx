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
import { Check, CheckCircle2, Eye, EyeOff, Lock, Mail, ShieldCheck, XCircle } from "lucide-react"

/**
 * Authentication lifecycle mirrored by the Secure Access panel.
 * Driven EXCLUSIVELY by the real request: idle → submitting → success/error.
 * No artificial timers — the backend response decides every transition.
 */
type AuthPhase = "idle" | "submitting" | "success" | "error"

function LoginForm() {
  const t = useTranslations("auth")
  const [showPassword, setShowPassword] = useState(false)
  const [serverError, setServerError] = useState("")
  const [mfaToken, setMfaToken] = useState<string | null>(null)
  const [mfaCode, setMfaCode] = useState("")
  const [mfaBusy, setMfaBusy] = useState(false)
  const [phase, setPhase] = useState<AuthPhase>("idle")
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
    watch,
    formState: { errors, isSubmitting, dirtyFields },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    mode: "onChange",
  })

  const emailValue = watch("email") ?? ""
  const passwordValue = watch("password") ?? ""
  const emailValid = !errors.email && dirtyFields.email && emailValue.trim().length > 0
  const passwordValid = !errors.password && dirtyFields.password && passwordValue.length > 0

  async function onSubmit(values: LoginValues) {
    setServerError("")
    // Real request starts immediately — the panel follows the actual promise.
    setPhase("submitting")
    const result = await login(values.email, values.password)
    if (result.error) {
      setServerError(result.error)
      setPhase("error")
      return
    }
    if (result.mfaRequired) {
      // Password accepted, but the account holds a verified authenticator:
      // only a short-lived challenge was issued — complete step two below.
      setMfaToken(result.mfaToken || "")
      setPhase("idle")
      return
    }
    // Success state flips, then redirect happens NOW (no animation wait).
    setPhase("success")
    router.push(redirect)
  }

  async function onMfaSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!mfaToken) return
    setMfaBusy(true)
    setServerError("")
    setPhase("submitting")
    const result = await completeMfaLogin(mfaToken, mfaCode.trim())
    setMfaBusy(false)
    if (result.error) {
      setServerError(result.error)
      setPhase("error")
      return
    }
    setPhase("success")
    router.push(redirect)
  }

  const steps: Array<{
    key: "stepCredentials" | "stepSecurityCheck" | "stepAuthentication"
    active?: boolean
    done?: boolean
    failed?: boolean
  }> = [
    { key: "stepCredentials", done: phase !== "idle" },
    { key: "stepSecurityCheck", active: phase === "submitting", done: phase === "success" },
    { key: "stepAuthentication", done: phase === "success", failed: phase === "error" },
  ]

  const statusText =
    phase === "submitting"
      ? t("statusVerifying")
      : phase === "success"
        ? t("statusSuccess")
        : phase === "error"
          ? t("statusError")
          : t("statusReady")

  const statusSub =
    phase === "success"
      ? t("statusSuccessSub")
      : phase === "error"
        ? t("statusErrorSub")
        : null

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="relative z-10 border-b border-border bg-background/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center px-4 sm:px-6 lg:px-8">
          <Logo />
        </div>
      </header>

      <main className="relative flex-1 flex items-center justify-center px-4 py-10 sm:py-12">
        <div
          className="absolute inset-0 bg-cover bg-center bg-no-repeat"
          style={{ backgroundImage: "url('/images/login-bg.jpg')" }}
          aria-hidden="true"
        />
        <div className="absolute inset-0 bg-foreground/60" aria-hidden="true" />

        <div className="relative z-10 w-full max-w-4xl overflow-hidden rounded-2xl border border-border/60 bg-card/92 shadow-2xl backdrop-blur-md">
          <div className="grid lg:grid-cols-2">
            {/* ────────────── LEFT: real ELMKUSOMA login ────────────── */}
            <section className="order-1 p-6 sm:p-8 lg:p-10" aria-labelledby="login-heading">
              <div>
                <h1
                  id="login-heading"
                  className="text-2xl font-bold tracking-tight text-foreground"
                >
                  {t("welcomeBack")}
                </h1>
                <p className="mt-2 text-sm text-muted-foreground">{t("loginToContinue")}</p>
              </div>

            {serverError && (
              <div
                role="alert"
                className="mt-6 rounded-lg border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive"
              >
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
              <>


              <form className="mt-7 space-y-5" onSubmit={handleSubmit(onSubmit)} noValidate>
                <div className="space-y-4">
                  <div>
                    <label htmlFor="email" className="block text-sm font-medium text-foreground">
                      {t("emailAddress")}
                    </label>
                    <div className="relative mt-1.5">
                      <Mail
                        className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                        aria-hidden="true"
                      />
                      <input
                        id="email"
                        type="email"
                        autoComplete="email"
                        inputMode="email"
                        placeholder="you@example.com"
                        aria-invalid={errors.email ? true : undefined}
                        aria-describedby={errors.email ? "email-error" : undefined}
                        {...register("email")}
                        className="h-11 w-full rounded-lg border border-border bg-muted/60 pl-9 pr-9 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                      />
                      {emailValid && (
                        <Check
                          className="absolute right-3 top-1/2 size-4 -translate-y-1/2 text-emerald-600"
                          aria-hidden="true"
                        />
                      )}
                    </div>
                    {errors.email && (
                      <p id="email-error" role="alert" className="mt-1.5 text-xs text-destructive">
                        {errors.email.message}
                      </p>
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
                      <Lock
                        className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                        aria-hidden="true"
                      />
                      <input
                        id="password"
                        type={showPassword ? "text" : "password"}
                        autoComplete="current-password"
                        placeholder={t("passwordPlaceholder")}
                        aria-invalid={errors.password ? true : undefined}
                        aria-describedby={errors.password ? "password-error" : undefined}
                        {...register("password")}
                        className="h-11 w-full rounded-lg border border-border bg-muted/60 pl-9 pr-11 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword((v) => !v)}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                        aria-label={t("togglePasswordVisibility")}
                        aria-pressed={showPassword}
                      >
                        {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                      </button>
                    </div>
                    {errors.password && (
                      <p id="password-error" role="alert" className="mt-1.5 text-xs text-destructive">
                        {errors.password.message}
                      </p>
                    )}
                  </div>
                </div>

                <Button
                  type="submit"
                  className="h-11 w-full text-sm"
                  disabled={isSubmitting}
                  aria-busy={isSubmitting || undefined}
                >
                  {isSubmitting ? t("signingIn") : t("signIn")}
                </Button>
              </form>

              <p className="mt-6 text-center text-sm text-muted-foreground">
                {t("dontHaveAccount")}{" "}
                <Link href="/register" className="font-medium text-primary hover:underline">
                  {t("register")}
                </Link>
              </p>
              </>
            )}
            </section>

            {/* ────────────── RIGHT: Secure Access (real auth state) ────────────── */}
            <aside
              className="order-2 border-t border-border/60 bg-gradient-to-br from-primary/12 via-card/70 to-card/85 p-6 sm:p-8 lg:border-t-0 lg:border-l lg:p-10"
              aria-labelledby="secure-access-heading"
            >
              <div className="flex h-full flex-col items-center justify-center text-center">
                <h2
                  id="secure-access-heading"
                  className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground"
                >
                  {t("secureAccess")}
                </h2>

                {/* Status ring — reflects the REAL request lifecycle */}
                <div className="relative mt-6 grid size-32 place-items-center sm:size-36">
                  <div
                    aria-hidden="true"
                    className={`absolute inset-0 rounded-full border-2 transition-colors duration-300 ${
                      phase === "success"
                        ? "border-emerald-500/60"
                        : phase === "error"
                          ? "border-destructive/60"
                          : phase === "submitting"
                            ? "border-primary/70 animate-pulse motion-reduce:animate-none"
                            : "border-primary/30"
                    }`}
                  />
                  <div
                    aria-hidden="true"
                    className={`absolute inset-3 rounded-full border border-dashed transition-colors duration-300 ${
                      phase === "success"
                        ? "border-emerald-500/40"
                        : phase === "error"
                          ? "border-destructive/40"
                          : "border-primary/20"
                    }`}
                  />
                  <div
                    className={`grid size-16 place-items-center rounded-full text-white shadow-lg transition-colors duration-300 sm:size-20 ${
                      phase === "success"
                        ? "bg-emerald-600"
                        : phase === "error"
                          ? "bg-destructive"
                          : "bg-primary"
                    }`}
                  >
                    {phase === "success" ? (
                      <CheckCircle2 className="size-8" aria-hidden="true" />
                    ) : phase === "error" ? (
                      <XCircle className="size-8" aria-hidden="true" />
                    ) : (
                      <ShieldCheck className="size-8" aria-hidden="true" />
                    )}
                  </div>
                </div>

                {/* Live status text (polite announcements) */}
                <div className="mt-5 min-h-[3rem]" aria-live="polite" aria-atomic="true">
                  <p className="text-sm font-semibold text-foreground">{statusText}</p>
                  {statusSub && <p className="mt-1 text-xs text-muted-foreground">{statusSub}</p>}
                </div>

                {/* Authentication status steps */}
                <ol className="mt-4 w-full max-w-[15rem] space-y-3 text-left" aria-label={t("authStatusRegion")}>
                  {steps.map((step) => {
                    const failed = step.failed === true && phase === "error"
                    const active = step.active === true && phase === "submitting"
                    const done = step.done && !failed
                    return (
                      <li key={step.key} className="flex items-center gap-3 text-sm">
                        <span
                          aria-hidden="true"
                          className={`grid size-5 shrink-0 place-items-center rounded-full border text-[10px] font-bold leading-none transition-colors duration-300 ${
                            failed
                              ? "border-destructive bg-destructive text-white"
                              : done
                                ? "border-emerald-600 bg-emerald-600 text-white"
                                : active
                                  ? "border-primary bg-primary text-primary-foreground"
                                  : "border-muted-foreground/40 text-muted-foreground"
                          }`}
                        >
                          {failed ? "✕" : done ? "✓" : active ? "●" : "○"}
                        </span>
                        <span
                          className={
                            failed ? "text-destructive" : done ? "text-foreground" : "text-muted-foreground"
                          }
                        >
                          {t(step.key)}
                        </span>
                      </li>
                    )
                  })}
                </ol>

                <div className="mt-10 border-t border-border/60 pt-5">
                  <p className="text-sm font-bold tracking-wide text-foreground">ELMKUSOMA</p>
                  <p className="mt-1 text-xs text-muted-foreground">{t("secureTagline")}</p>
                </div>
              </div>
            </aside>
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
