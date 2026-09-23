"use client"

import { useState, useMemo } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { Eye, EyeOff, CheckCircle, ShieldCheck, RefreshCw } from "lucide-react"

const roles = [
  "Student",
  "Teacher",
  "Parent",
  "Other Learner",
]

function generateCaptchaCode(): string {
  return Math.floor(10000 + Math.random() * 90000).toString()
}

const CAPTCHA_STYLES: React.CSSProperties[] = [
  { transform: "rotate(-3deg) skewX(-2deg)" },
  { transform: "rotate(2deg) skewX(1deg)" },
  { transform: "rotate(-1deg) skewX(-1deg)" },
  { transform: "rotate(3deg) skewX(2deg)" },
  { transform: "rotate(-2deg) skewX(1deg)" },
]

export default function RegisterPage() {
  const t = useTranslations("auth")
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [serverError, setServerError] = useState("")
  const [registered, setRegistered] = useState(false)
  const { register: registerUser } = useAuth()
  const router = useRouter()

  const [captchaKey, setCaptchaKey] = useState(0)
  const captchaCode = useMemo(() => generateCaptchaCode(), [captchaKey])
  const [userCaptchaInput, setUserCaptchaInput] = useState("")
  const [captchaError, setCaptchaError] = useState("")

  const registerSchema = z
    .object({
      firstName: z.string().min(1, t("firstNameRequired")).min(2, t("firstNameTooShort")),
      middleName: z.string().optional(),
      lastName: z.string().min(1, t("lastNameRequired")).min(2, t("lastNameTooShort")),
      email: z.string().min(1, t("emailRequired")).email(t("invalidEmail")),
      phone: z.string().min(1, t("phoneRequired")).min(10, t("phoneTooShort")),
      role: z.string().min(1, t("roleRequired")),
      learningLevel: z.string().optional(),
      password: z
        .string()
        .min(1, t("passwordRequired"))
        .min(8, t("passwordMin8"))
        .regex(/[A-Z]/, t("passwordUppercase"))
        .regex(/[0-9]/, t("passwordNumber")),
      confirmPassword: z.string().min(1, t("confirmPasswordRequired")),
      agreeToTerms: z.boolean().refine((val) => val === true, {
        message: t("agreeToTermsRequired"),
      }),
    })
    .refine((data) => data.password === data.confirmPassword, {
      message: t("passwordsDoNotMatch"),
      path: ["confirmPassword"],
    })

  type RegisterValues = z.infer<typeof registerSchema>

  const learningLevels = [
    { value: "NURSERY", label: t("levelNursery") },
    { value: "PRIMARY", label: t("levelPrimary") },
    { value: "SECONDARY", label: t("levelSecondary") },
    { value: "COLLEGE", label: t("levelCollege") },
    { value: "VETA", label: t("levelVeta") },
    { value: "UNIVERSITY", label: t("levelUniversity") },
  ]

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
  })

  const selectedRole = watch("role")

  function refreshCaptcha() {
    setCaptchaKey((k) => k + 1)
    setUserCaptchaInput("")
    setCaptchaError("")
  }

  async function onSubmit(values: RegisterValues) {
    setServerError("")
    if (userCaptchaInput !== captchaCode) {
      setCaptchaError(t("captchaIncorrect"))
      refreshCaptcha()
      return
    }
    const result = await registerUser({
      firstName: values.firstName,
      middleName: values.middleName || undefined,
      lastName: values.lastName,
      email: values.email,
      phone: values.phone,
      password: values.password,
      role: values.role,
      learningLevel: values.role === "Student" ? values.learningLevel : undefined,
    })
    if (result.error) {
      setServerError(result.error)
      return
    }
    setRegistered(true)
  }

  if (registered) {
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
            style={{ backgroundImage: "url('/images/register-bg.jpg')" }}
          />
          <div className="absolute inset-0 bg-foreground/60" />
          <div className="relative z-10 w-full max-w-md">
            <div className="rounded-2xl border border-border bg-card/95 backdrop-blur-sm p-8 shadow-lg text-center">
              <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-teal/10">
                <CheckCircle className="size-7 text-teal" />
              </div>
              <h1 className="mt-4 text-2xl font-bold tracking-tight text-foreground">
                {t("accountCreated")}!
              </h1>
              <p className="mt-2 text-sm text-muted-foreground">
                {t("accountCreatedMessage")}
              </p>
              <Button onClick={() => router.push("/dashboard")} className="mt-6 w-full">
                {t("goToDashboard")}
              </Button>
            </div>
          </div>
        </main>
      </div>
    )
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
          style={{ backgroundImage: "url('/images/register-bg.jpg')" }}
        />
        <div className="absolute inset-0 bg-foreground/60" />
        <div className="relative z-10 w-full max-w-md">
          <div className="rounded-2xl border border-border bg-card/95 backdrop-blur-sm p-8 shadow-lg">
            <div className="text-center">
              <h1 className="text-2xl font-bold tracking-tight text-foreground">
                {t("createAccount")}
              </h1>
              <p className="mt-2 text-sm text-muted-foreground">
                {t("registerSubtitle")}
              </p>
            </div>

            {serverError && (
              <div className="mt-6 rounded-lg border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">
                {serverError}
              </div>
            )}

            <form className="mt-8 space-y-5" onSubmit={handleSubmit(onSubmit)}>
              <div className="space-y-4">
                <div className="grid gap-4 sm:grid-cols-2">
                  <div>
                    <label htmlFor="firstName" className="block text-sm font-medium text-foreground">
                      {t("firstName")}
                    </label>
                    <input
                      id="firstName"
                      type="text"
                      placeholder={t("firstNamePlaceholder")}
                      {...register("firstName")}
                      className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                    />
                    {errors.firstName && (
                      <p className="mt-1.5 text-xs text-destructive">{errors.firstName.message}</p>
                    )}
                  </div>

                  <div>
                    <label htmlFor="middleName" className="block text-sm font-medium text-foreground">
                      {t("middleName")} <span className="text-muted-foreground">{t("optional")}</span>
                    </label>
                    <input
                      id="middleName"
                      type="text"
                      placeholder={t("middleNamePlaceholder")}
                      {...register("middleName")}
                      className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="lastName" className="block text-sm font-medium text-foreground">
                    {t("lastName")}
                  </label>
                  <input
                    id="lastName"
                    type="text"
                    placeholder={t("lastNamePlaceholder")}
                    {...register("lastName")}
                    className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                  />
                  {errors.lastName && (
                    <p className="mt-1.5 text-xs text-destructive">{errors.lastName.message}</p>
                  )}
                </div>

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
                  <label htmlFor="phone" className="block text-sm font-medium text-foreground">
                    {t("phoneNumber")}
                  </label>
                  <input
                    id="phone"
                    type="tel"
                    placeholder="+255 700 000 000"
                    {...register("phone")}
                    className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                  />
                  {errors.phone && (
                    <p className="mt-1.5 text-xs text-destructive">{errors.phone.message}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="role" className="block text-sm font-medium text-foreground">
                    {t("registerAs")}
                  </label>
                  <select
                    id="role"
                    {...register("role")}
                    className="mt-1.5 h-11 w-full appearance-none rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors focus:border-ring focus:bg-background"
                  >
                    <option value="">{t("selectRole")}</option>
                    {roles.map((role) => (
                      <option key={role} value={role}>
                        {role}
                      </option>
                    ))}
                  </select>
                  {errors.role && (
                    <p className="mt-1.5 text-xs text-destructive">{errors.role.message}</p>
                  )}
                </div>

                {selectedRole === "Student" && (
                  <div className="animate-in fade-in slide-in-from-top-2 duration-200">
                    <label htmlFor="learningLevel" className="block text-sm font-medium text-foreground">
                      {t("learningLevel")}
                    </label>
                    <select
                      id="learningLevel"
                      {...register("learningLevel")}
                      className="mt-1.5 h-11 w-full appearance-none rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors focus:border-ring focus:bg-background"
                    >
                      <option value="">{t("selectLearningLevel")}</option>
                      {learningLevels.map((level) => (
                        <option key={level.value} value={level.value}>
                          {level.label}
                        </option>
                      ))}
                    </select>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {t("learningLevelHint")}
                    </p>
                  </div>
                )}

                <div>
                  <label htmlFor="password" className="block text-sm font-medium text-foreground">
                    {t("password")}
                  </label>
                  <div className="relative mt-1.5">
                    <input
                      id="password"
                      type={showPassword ? "text" : "password"}
                      placeholder={t("createPasswordPlaceholder")}
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

                <div>
                  <label htmlFor="confirmPassword" className="block text-sm font-medium text-foreground">
                    {t("confirmPassword")}
                  </label>
                  <div className="relative mt-1.5">
                    <input
                      id="confirmPassword"
                      type={showConfirm ? "text" : "password"}
                      placeholder={t("confirmPasswordPlaceholder")}
                      {...register("confirmPassword")}
                      className="h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 pr-11 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirm((v) => !v)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-muted-foreground hover:text-foreground"
                      tabIndex={-1}
                      aria-label={t("togglePasswordVisibility")}
                    >
                      {showConfirm ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                    </button>
                  </div>
                  {errors.confirmPassword && (
                    <p className="mt-1.5 text-xs text-destructive">{errors.confirmPassword.message}</p>
                  )}
                </div>
              </div>

              {/* Human Verification */}
              <div className="rounded-xl border border-border bg-muted/40 p-4">
                <div className="flex items-center gap-2 mb-3">
                  <ShieldCheck className="size-5 text-primary" />
                  <span className="text-sm font-medium text-foreground">{t("captchaTitle")}*</span>
                </div>

                <div className="flex flex-col items-center gap-3">
                  <div
                    className="relative flex items-center justify-center rounded-lg border border-border bg-background px-6 py-3 select-none"
                    style={{ minWidth: 180 }}
                  >
                    <div className="flex items-center gap-1">
                      {captchaCode.split("").map((digit, i) => (
                        <span
                          key={`${captchaKey}-${i}`}
                          className="inline-block text-2xl font-bold tracking-wider text-foreground/80"
                          style={{
                            ...CAPTCHA_STYLES[i % CAPTCHA_STYLES.length],
                            fontFamily: "monospace",
                            textShadow: "1px 1px 0 rgba(0,0,0,0.08), -1px -1px 0 rgba(255,255,255,0.3)",
                          }}
                        >
                          {digit}
                        </span>
                      ))}
                    </div>
                    <div className="pointer-events-none absolute inset-0 rounded-lg opacity-[0.04]"
                      style={{
                        backgroundImage: "repeating-linear-gradient(0deg, transparent, transparent 3px, currentColor 3px, currentColor 4px)",
                      }}
                    />
                  </div>

                  <p className="text-xs text-muted-foreground">{t("captchaHint")}</p>

                  <input
                    type="text"
                    inputMode="numeric"
                    maxLength={5}
                    value={userCaptchaInput}
                    onChange={(e) => {
                      setUserCaptchaInput(e.target.value.replace(/\D/g, "").slice(0, 5))
                      setCaptchaError("")
                    }}
                    placeholder="_ _ _ _ _"
                    className="h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground text-center tracking-[0.5em] font-mono outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                  />

                  <button
                    type="button"
                    onClick={refreshCaptcha}
                    className="inline-flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors"
                  >
                    <RefreshCw className="size-3" />
                    {t("refreshCaptcha")}
                  </button>
                </div>

                {captchaError && (
                  <p className="mt-2 text-xs text-destructive text-center">{captchaError}</p>
                )}
              </div>

              <div className="space-y-4">
                <label className="flex items-start gap-3">
                  <input
                    type="checkbox"
                    {...register("agreeToTerms")}
                    className="mt-0.5 size-4 rounded border-border accent-primary"
                  />
                  <span className="text-sm text-muted-foreground">
                    {t("agreePrefix")}{" "}
                    <Link href="/terms" className="font-medium text-primary hover:underline">
                      {t("termsOfService")}
                    </Link>{" "}
                    {t("and")}{" "}
                    <Link href="/privacy" className="font-medium text-primary hover:underline">
                      {t("privacyPolicy")}
                    </Link>
                  </span>
                </label>
                {errors.agreeToTerms && (
                  <p className="text-xs text-destructive">{errors.agreeToTerms.message}</p>
                )}
              </div>

              <Button type="submit" className="h-11 w-full text-sm" disabled={isSubmitting}>
                {isSubmitting ? t("creatingAccount") : t("createAccount")}
              </Button>
            </form>

            <p className="mt-6 text-center text-sm text-muted-foreground">
              {t("alreadyHaveAccount")}{" "}
              <Link href="/login" className="font-medium text-primary hover:underline">
                {t("login")}
              </Link>
            </p>
          </div>
        </div>
      </main>
    </div>
  )
}
