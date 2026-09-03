"use client"

import Link from "next/link"
import { ArrowRight, Mail } from "lucide-react"
import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"

export default function ForgotPasswordPage() {
  return (
    <div className="flex min-h-dvh flex-col bg-muted/40">
      <header className="border-b border-border bg-background/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center px-4 sm:px-6 lg:px-8">
          <Logo />
        </div>
      </header>

      <main className="flex flex-1 items-center justify-center px-4 py-12">
        <div className="w-full max-w-md">
          <div className="rounded-2xl border border-border bg-card p-8 shadow-xs">
            <div className="text-center">
              <div className="mx-auto flex size-12 items-center justify-center rounded-xl bg-accent text-primary">
                <Mail className="size-6" />
              </div>
              <h1 className="mt-4 text-2xl font-bold tracking-tight text-foreground">
                Reset your password
              </h1>
              <p className="mt-2 text-sm text-muted-foreground">
                Enter your email address and we&apos;ll send you a link to reset your password.
              </p>
            </div>

            <form className="mt-8 space-y-5" onSubmit={(e) => e.preventDefault()}>
              <div>
                <label htmlFor="email" className="block text-sm font-medium text-foreground">
                  Email Address
                </label>
                <input
                  id="email"
                  type="email"
                  required
                  placeholder="you@example.com"
                  className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                />
              </div>

              <Button type="submit" className="h-11 w-full gap-2 text-sm">
                Send Reset Link
                <ArrowRight className="size-4" />
              </Button>
            </form>

            <p className="mt-6 text-center text-sm text-muted-foreground">
              Remember your password?{" "}
              <Link href="/login" className="font-medium text-primary hover:underline">
                Back to Login
              </Link>
            </p>
          </div>
        </div>
      </main>
    </div>
  )
}
