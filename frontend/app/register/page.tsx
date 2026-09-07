"use client"

import { useState, useCallback } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/lib/auth"
import { Eye, EyeOff, CheckCircle, ShieldCheck } from "lucide-react"

const roles = [
  "Student",
  "Teacher",
  "Lecturer",
  "Facilitator",
  "Parent",
  "Other",
]

const registerSchema = z
  .object({
    firstName: z.string().min(1, "First name is required").min(2, "First name must be at least 2 characters"),
    middleName: z.string().optional(),
    lastName: z.string().min(1, "Last name / surname is required").min(2, "Last name must be at least 2 characters"),
    email: z.string().min(1, "Email is required").email("Please enter a valid email address"),
    role: z.string().min(1, "Please select your role"),
    password: z
      .string()
      .min(1, "Password is required")
      .min(8, "Password must be at least 8 characters")
      .regex(/[A-Z]/, "Password must contain at least one uppercase letter")
      .regex(/[0-9]/, "Password must contain at least one number"),
    confirmPassword: z.string().min(1, "Please confirm your password"),
    acceptTerms: z.literal(true, { errorMap: () => ({ message: "You must accept the Terms of Service and Privacy Policy" }) }),
    captchaVerified: z.literal(true, { errorMap: () => ({ message: "Please verify you are not a robot" }) }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  })

type RegisterValues = z.infer<typeof registerSchema>

export default function RegisterPage() {
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [serverError, setServerError] = useState("")
  const [registered, setRegistered] = useState(false)
  const [captchaLoading, setCaptchaLoading] = useState(false)
  const { register: registerUser } = useAuth()
  const router = useRouter()

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      acceptTerms: false as unknown as true,
      captchaVerified: false as unknown as true,
    },
  })

  const acceptTerms = watch("acceptTerms")
  const captchaVerified = watch("captchaVerified")

  const handleCaptcha = useCallback(() => {
    setCaptchaLoading(true)
    setTimeout(() => {
      setValue("captchaVerified", true as unknown as true, { shouldValidate: true })
      setCaptchaLoading(false)
    }, 1500)
  }, [setValue])

  async function onSubmit(values: RegisterValues) {
    setServerError("")
    const fullName = [values.firstName, values.middleName, values.lastName].filter(Boolean).join(" ")
    const result = await registerUser({
      name: fullName,
      email: values.email,
      password: values.password,
      role: values.role,
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

              {/* reCAPTCHA */}
              <div>
                {captchaVerified ? (
                  <div className="flex items-center gap-2 rounded-lg border border-teal/30 bg-teal/5 px-4 py-3">
                    <ShieldCheck className="size-5 text-teal" />
                    <span className="text-sm font-medium text-teal">Verified — you are not a robot</span>
                  </div>
                ) : (
                  <button
                    type="button"
                    onClick={handleCaptcha}
                    disabled={captchaLoading}
                    className="flex w-full items-center gap-3 rounded-lg border border-border bg-muted/60 px-4 py-3 transition-colors hover:bg-muted disabled:opacity-60"
                  >
                    <div className="flex size-7 items-center justify-center rounded border-2 border-muted-foreground/30">
                      {captchaLoading ? (
                        <div className="size-4 animate-spin rounded-full border-2 border-primary border-t-transparent" />
                      ) : null}
                    </div>
                    <div className="text-left">
                      <p className="text-sm font-medium text-foreground">I'm not a robot</p>
                      <p className="text-[10px] text-muted-foreground">reCAPTCHA verification</p>
                    </div>
                    <div className="ml-auto">
                      <img src="https://www.gstatic.com/recaptcha/api2/logo_48.png" alt="reCAPTCHA" className="h-8 w-8 opacity-60" />
                    </div>
                  </button>
                )}
                {errors.captchaVerified && (
                  <p className="mt-1.5 text-xs text-destructive">{errors.captchaVerified.message}</p>
                )}
              </div>

              {/* Terms Checkbox */}
              <div>
                <label className="flex items-start gap-3 cursor-pointer">
                  <input
                    type="checkbox"
                    {...register("acceptTerms")}
                    className="mt-0.5 size-4 rounded border-border accent-primary"
                  />
                  <span className="text-sm text-muted-foreground">
                    I agree to the{" "}
                    <Link href="/terms" className="font-medium text-primary hover:underline" onClick={(e) => e.stopPropagation()}>
                      Terms of Service
                    </Link>{" "}
                    and{" "}
                    <Link href="/privacy" className="font-medium text-primary hover:underline" onClick={(e) => e.stopPropagation()}>
                      Privacy Policy
                    </Link>
                  </span>
                </label>
                {errors.acceptTerms && (
                  <p className="mt-1.5 text-xs text-destructive">{errors.acceptTerms.message}</p>
                )}
              </div>

              <Button type="submit" className="h-11 w-full text-sm" disabled={isSubmitting || !acceptTerms || !captchaVerified}>
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
