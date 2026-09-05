"use client"

import { useState } from "react"
import Link from "next/link"
import { useSearchParams, useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/lib/auth"
import { Eye, EyeOff } from "lucide-react"

const loginSchema = z.object({
  email: z.string().min(1, "Email is required").email("Please enter a valid email address"),
  password: z.string().min(1, "Password is required").min(6, "Password must be at least 6 characters"),
})

type LoginValues = z.infer<typeof loginSchema>

export default function LoginPage() {
  const [showPassword, setShowPassword] = useState(false)
  const [serverError, setServerError] = useState("")
  const { login } = useAuth()
  const router = useRouter()
  const searchParams = useSearchParams()
  const redirect = searchParams.get("redirect") || "/dashboard"

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
        <div className="absolute inset-0 bg-black/60" />
        <div className="relative z-10 w-full max-w-md">
          <div className="rounded-2xl border border-white/10 bg-card/95 backdrop-blur-sm p-8 shadow-lg">
            <div className="text-center">
              <h1 className="text-2xl font-bold tracking-tight text-foreground">
                Welcome back
              </h1>
              <p className="mt-2 text-sm text-muted-foreground">
                Sign in to continue your learning journey
              </p>
            </div>

            {serverError && (
              <div className="mt-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-400">
                {serverError}
              </div>
            )}

            <form className="mt-8 space-y-5" onSubmit={handleSubmit(onSubmit)}>
              <div className="space-y-4">
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
                    <p className="mt-1.5 text-xs text-red-600">{errors.email.message}</p>
                  )}
                </div>

                <div>
                  <div className="flex items-center justify-between">
                    <label htmlFor="password" className="text-sm font-medium text-foreground">
                      Password
                    </label>
                    <Link
                      href="/forgot-password"
                      className="text-xs font-medium text-primary hover:underline"
                    >
                      Forgot password?
                    </Link>
                  </div>
                  <div className="relative mt-1.5">
                    <input
                      id="password"
                      type={showPassword ? "text" : "password"}
                      placeholder="Enter your password"
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
                    <p className="mt-1.5 text-xs text-red-600">{errors.password.message}</p>
                  )}
                </div>
              </div>

              <Button type="submit" className="h-11 w-full text-sm" disabled={isSubmitting}>
                {isSubmitting ? "Signing in..." : "Sign In"}
              </Button>
            </form>

            <p className="mt-6 text-center text-sm text-muted-foreground">
              Don&apos;t have an account?{" "}
              <Link href="/register" className="font-medium text-primary hover:underline">
                Register
              </Link>
            </p>
          </div>
        </div>
      </main>
    </div>
  )
}
