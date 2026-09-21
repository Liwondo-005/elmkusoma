"use client"

import { useState, useEffect, useCallback } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/lib/auth"
import { Eye, EyeOff, CheckCircle, ShieldCheck, Mail } from "lucide-react"
import { authApi } from "@/lib/api"

const roles = [
  "Student",
  "Teacher",
  "Parent",
  "Other Learner",
]

const learningLevels = [
  { value: "NURSERY", label: "Nursery" },
  { value: "PRIMARY", label: "Primary" },
  { value: "SECONDARY", label: "Secondary" },
  { value: "COLLEGE", label: "College" },
  { value: "UNIVERSITY", label: "University" },
]

const registerSchema = z
  .object({
    firstName: z.string().min(1, "First name is required").min(2, "First name must be at least 2 characters"),
    middleName: z.string().optional(),
    lastName: z.string().min(1, "Last name / surname is required").min(2, "Last name must be at least 2 characters"),
    email: z.string().min(1, "Email is required").email("Please enter a valid email address"),
    phone: z.string().min(1, "Phone number is required").min(10, "Phone number must be at least 10 digits"),
    role: z.string().min(1, "Please select your role"),
    learningLevel: z.string().optional(),
    password: z
      .string()
      .min(1, "Password is required")
      .min(8, "Password must be at least 8 characters")
      .regex(/[A-Z]/, "Password must contain at least one uppercase letter")
      .regex(/[0-9]/, "Password must contain at least one number"),
    confirmPassword: z.string().min(1, "Please confirm your password"),
    agreeToTerms: z.boolean().refine((val) => val === true, {
      message: "You must agree to the Terms of Service and Privacy Policy",
    }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  })

type RegisterValues = z.infer<typeof registerSchema>

type VerificationStep = "idle" | "sending" | "sent" | "verifying" | "verified"

export default function RegisterPage() {
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [serverError, setServerError] = useState("")
  const [registered, setRegistered] = useState(false)
  const { register: registerUser } = useAuth()
  const router = useRouter()

  const [verificationStep, setVerificationStep] = useState<VerificationStep>("idle")
  const [verificationCode, setVerificationCode] = useState("")
  const [verificationMessage, setVerificationMessage] = useState("")
  const [verificationError, setVerificationError] = useState("")
  const [cooldown, setCooldown] = useState(0)

  const {
    register,
    handleSubmit,
    watch,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
  })

  const selectedRole = watch("role")
  const emailValue = watch("email")

  useEffect(() => {
    if (cooldown <= 0) return
    const timer = setTimeout(() => setCooldown((c) => c - 1), 1000)
    return () => clearTimeout(timer)
  }, [cooldown])

  const handleSendCode = useCallback(async () => {
    const email = getValues("email")
    if (!email || errors.email) {
      setVerificationError("Please enter a valid email address first.")
      return
    }
    setVerificationError("")
    setVerificationMessage("")
    setVerificationStep("sending")
    try {
      await authApi.sendVerificationCode({ email })
      setVerificationStep("sent")
      setVerificationMessage("A 5-digit verification code has been sent to your email.")
      setCooldown(60)
      setVerificationCode("")
    } catch (err: any) {
      setVerificationStep("idle")
      setVerificationError(err?.message || "Failed to send verification code. Please try again.")
    }
  }, [getValues, errors.email])

  const handleVerifyCode = useCallback(async () => {
    if (!verificationCode || verificationCode.length !== 5) {
      setVerificationError("Please enter the 5-digit verification code.")
      return
    }
    setVerificationError("")
    setVerificationStep("verifying")
    try {
      const email = getValues("email")
      await authApi.verifyCode({ email, code: verificationCode })
      setVerificationStep("verified")
      setVerificationMessage("Verification successful!")
      setVerificationError("")
    } catch (err: any) {
      setVerificationStep("sent")
      setVerificationError(err?.message || "Invalid verification code. Please try again.")
    }
  }, [verificationCode, getValues])

  const handleResendCode = useCallback(async () => {
    if (cooldown > 0) return
    await handleSendCode()
  }, [cooldown, handleSendCode])

  async function onSubmit(values: RegisterValues) {
    setServerError("")
    if (verificationStep !== "verified") {
      setServerError("Please verify you are human before registering.")
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
                Account Created!
              </h1>
              <p className="mt-2 text-sm text-muted-foreground">
                Your account has been created successfully. Redirecting to dashboard...
              </p>
              <Button onClick={() => router.push("/dashboard")} className="mt-6 w-full">
                Go to Dashboard
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
                Create your account
              </h1>
              <p className="mt-2 text-sm text-muted-foreground">
                Join thousands of learners across Africa
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
                      First Name
                    </label>
                    <input
                      id="firstName"
                      type="text"
                      placeholder="First name"
                      {...register("firstName")}
                      className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                    />
                    {errors.firstName && (
                      <p className="mt-1.5 text-xs text-destructive">{errors.firstName.message}</p>
                    )}
                  </div>

                  <div>
                    <label htmlFor="middleName" className="block text-sm font-medium text-foreground">
                      Middle Name <span className="text-muted-foreground">(optional)</span>
                    </label>
                    <input
                      id="middleName"
                      type="text"
                      placeholder="Middle name"
                      {...register("middleName")}
                      className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="lastName" className="block text-sm font-medium text-foreground">
                    Last Name / Surname
                  </label>
                  <input
                    id="lastName"
                    type="text"
                    placeholder="Last name or surname"
                    {...register("lastName")}
                    className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                  />
                  {errors.lastName && (
                    <p className="mt-1.5 text-xs text-destructive">{errors.lastName.message}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="email" className="block text-sm font-medium text-foreground">
                    Email Address
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
                    Phone Number
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
                    Register as
                  </label>
                  <select
                    id="role"
                    {...register("role")}
                    className="mt-1.5 h-11 w-full appearance-none rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors focus:border-ring focus:bg-background"
                  >
                    <option value="">Select your role</option>
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
                      Learning Level
                    </label>
                    <select
                      id="learningLevel"
                      {...register("learningLevel")}
                      className="mt-1.5 h-11 w-full appearance-none rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors focus:border-ring focus:bg-background"
                    >
                      <option value="">Select your learning level</option>
                      {learningLevels.map((level) => (
                        <option key={level.value} value={level.value}>
                          {level.label}
                        </option>
                      ))}
                    </select>
                    <p className="mt-1 text-xs text-muted-foreground">
                      This helps us customize your learning experience.
                    </p>
                  </div>
                )}

                <div>
                  <label htmlFor="password" className="block text-sm font-medium text-foreground">
                    Password
                  </label>
                  <div className="relative mt-1.5">
                    <input
                      id="password"
                      type={showPassword ? "text" : "password"}
                      placeholder="Create a password"
                      {...register("password")}
                      className="h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 pr-11 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((v) => !v)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-muted-foreground hover:text-foreground"
                      tabIndex={-1}
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
                    Confirm Password
                  </label>
                  <div className="relative mt-1.5">
                    <input
                      id="confirmPassword"
                      type={showConfirm ? "text" : "password"}
                      placeholder="Confirm your password"
                      {...register("confirmPassword")}
                      className="h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 pr-11 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirm((v) => !v)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-muted-foreground hover:text-foreground"
                      tabIndex={-1}
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
                  <span className="text-sm font-medium text-foreground">Are you a human?</span>
                </div>

                {verificationStep === "idle" && (
                  <div>
                    <p className="text-xs text-muted-foreground mb-3">
                      Please verify you are human to continue with registration.
                    </p>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={handleSendCode}
                      className="w-full"
                    >
                      <Mail className="size-4 mr-2" />
                      Send verification code
                    </Button>
                  </div>
                )}

                {verificationStep === "sending" && (
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <div className="size-4 animate-spin rounded-full border-2 border-primary border-t-transparent" />
                    Sending code...
                  </div>
                )}

                {(verificationStep === "sent" || verificationStep === "verifying") && (
                  <div className="space-y-3">
                    {verificationMessage && (
                      <p className="text-xs text-emerald-600 dark:text-emerald-400">{verificationMessage}</p>
                    )}
                    <p className="text-xs text-muted-foreground">
                      Enter the 5-digit code sent to your email:
                    </p>
                    <input
                      type="text"
                      inputMode="numeric"
                      maxLength={5}
                      value={verificationCode}
                      onChange={(e) => {
                        const val = e.target.value.replace(/\D/g, "").slice(0, 5)
                        setVerificationCode(val)
                        setVerificationError("")
                      }}
                      placeholder="_ _ _ _ _"
                      className="h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground text-center tracking-[0.5em] font-mono outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                      disabled={verificationStep === "verifying"}
                    />
                    <div className="flex gap-2">
                      <Button
                        type="button"
                        size="sm"
                        onClick={handleVerifyCode}
                        disabled={verificationCode.length !== 5 || verificationStep === "verifying"}
                        className="flex-1"
                      >
                        {verificationStep === "verifying" ? "Verifying..." : "Verify code"}
                      </Button>
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        onClick={handleResendCode}
                        disabled={cooldown > 0}
                        className="shrink-0"
                      >
                        {cooldown > 0 ? `Resend (${cooldown}s)` : "Resend code"}
                      </Button>
                    </div>
                  </div>
                )}

                {verificationStep === "verified" && (
                  <div className="flex items-center gap-2 text-sm text-emerald-600 dark:text-emerald-400">
                    <CheckCircle className="size-5" />
                    Verification successful!
                  </div>
                )}

                {verificationError && (
                  <p className="mt-2 text-xs text-destructive">{verificationError}</p>
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
                    I agree to the{" "}
                    <Link href="/terms" className="font-medium text-primary hover:underline">
                      Terms of Service
                    </Link>{" "}
                    and{" "}
                    <Link href="/privacy" className="font-medium text-primary hover:underline">
                      Privacy Policy
                    </Link>
                  </span>
                </label>
                {errors.agreeToTerms && (
                  <p className="text-xs text-destructive">{errors.agreeToTerms.message}</p>
                )}
              </div>

              <Button type="submit" className="h-11 w-full text-sm" disabled={isSubmitting}>
                {isSubmitting ? "Creating account..." : "Create Account"}
              </Button>
            </form>

            <p className="mt-6 text-center text-sm text-muted-foreground">
              Already have an account?{" "}
              <Link href="/login" className="font-medium text-primary hover:underline">
                Login
              </Link>
            </p>
          </div>
        </div>
      </main>
    </div>
  )
}
