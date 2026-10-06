"use client"

import { Suspense, useEffect, useRef, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { ArrowLeft, Check, Loader2, ShieldCheck } from "lucide-react"
import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"
import { OtpInput } from "@/components/auth/otp-input"
import { ApiRequestError, authApi } from "@/lib/api"

/**
 * Verify OTP — the UI layer of the EXISTING backend flow
 * (`POST /v1/auth/send-code` → `POST /v1/auth/verify-code`).
 *
 * Security contract:
 *  - the frontend validates FORMAT ONLY (5 digits) and never decides validity;
 *  - the code lives in component state only — never in storage, URLs or logs;
 *  - countdowns run only after the backend confirms a send;
 *  - on success the existing authentication flow decides where to continue.
 */

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
/** Mirrors `VerifyCodeRequest` (`^\d{5}$`) — format gate only, correctness is the backend's. */
const CODE_LENGTH = 5
/** Mirrors the 60s cooldown enforced by `AuthServiceImpl.sendVerificationCode`. */
const COOLDOWN_SECONDS = 60

type Phase = "email" | "sending" | "ready" | "verifying" | "verified"
type Status = { kind: "success" | "error" | "info"; text: string }

function maskEmail(email: string): string {
  const at = email.indexOf("@")
  if (at <= 0) return email
  const local = email.slice(0, at)
  const domain = email.slice(at + 1)
  const stars = Math.max(Math.min(local.length - 1, 3), 1)
  return `${local[0]}${"*".repeat(stars)}@${domain}`
}

function formatCountdown(totalSeconds: number): string {
  const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, "0")
  const seconds = String(totalSeconds % 60).padStart(2, "0")
  return `${minutes}:${seconds}`
}

function safeRedirectPath(raw: string | null): string {
  if (raw && raw.startsWith("/") && !raw.startsWith("//")) return raw
  return "/login"
}

function VerifyOtpForm() {
  const t = useTranslations("otp")
  const ta = useTranslations("auth")
  const router = useRouter()
  const params = useSearchParams()

  const emailParam = (params.get("email") ?? "").trim()
  const context = params.get("context") ?? "default"
  const redirect = safeRedirectPath(params.get("redirect"))

  const [email, setEmail] = useState(emailParam)
  const [draftEmail, setDraftEmail] = useState(emailParam)
  const [digits, setDigits] = useState("")
  const [sent, setSent] = useState(false)
  const [phase, setPhase] = useState<Phase>(emailParam ? "sending" : "email")
  const [cooldown, setCooldown] = useState(0)
  const [status, setStatus] = useState<Status | null>(null)
  const [focusToken, setFocusToken] = useState(0)
  const sentAtRef = useRef(0)
  const autoSentRef = useRef(false)

  // One interval, only while a cooldown is live (no competing timers).
  const ticking = cooldown > 0
  useEffect(() => {
    if (!ticking) return
    const id = setInterval(() => {
      const remaining = Math.max(
        0,
        Math.ceil((sentAtRef.current + COOLDOWN_SECONDS * 1000 - Date.now()) / 1000),
      )
      setCooldown(remaining)
      if (remaining === 0) clearInterval(id)
    }, 1000)
    return () => clearInterval(id)
  }, [ticking])

  function markSent() {
    sentAtRef.current = Date.now()
    setSent(true)
    setCooldown(COOLDOWN_SECONDS)
    setDigits("")
    setPhase("ready")
    setFocusToken((token) => token + 1)
  }

  async function sendCode(target: string): Promise<boolean> {
    setPhase("sending")
    setStatus(null)
    try {
      await authApi.sendVerificationCode({ email: target })
      setEmail(target)
      setDraftEmail(target)
      markSent()
      setStatus({ kind: "success", text: t("codeSentSuccess") })
      return true
    } catch (error) {
      if (error instanceof ApiRequestError && error.status === 400) {
        if (/wait\s*60|before requesting a new code/i.test(error.message)) {
          // The backend confirms a code exists within the cooldown window —
          // resume the waiting state without claiming a *new* code was sent.
          setEmail(target)
          setDraftEmail(target)
          markSent()
          setStatus({ kind: "info", text: t("recentlySent") })
          return true
        }
        setPhase(sent ? "ready" : "email")
        setStatus({ kind: "error", text: error.message })
        return false
      }
      setPhase(sent ? "ready" : "email")
      setStatus({
        kind: "error",
        text: error instanceof ApiRequestError && error.status === 429
          ? t("rateLimited")
          : t("sendFailed"),
      })
      return false
    }
  }

  // Deep link (`/verify-otp?email=...`) requests a real code exactly once.
  useEffect(() => {
    if (!emailParam || autoSentRef.current) return
    autoSentRef.current = true
    setDraftEmail(emailParam)
    if (!EMAIL_RE.test(emailParam)) {
      setPhase("email")
      setStatus({ kind: "error", text: t("invalidEmail") })
      return
    }
    void sendCode(emailParam)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function verifyCode() {
    if (digits.length !== CODE_LENGTH || phase !== "ready") return
    setPhase("verifying")
    setStatus(null)
    try {
      await authApi.verifyCode({ email, code: digits })
      setPhase("verified")
      setStatus({ kind: "success", text: t("verified") })
      const reduceMotion =
        typeof window !== "undefined" &&
        window.matchMedia("(prefers-reduced-motion: reduce)").matches
      window.setTimeout(() => router.push(redirect), reduceMotion ? 0 : 750)
    } catch (error) {
      setPhase("ready")
      setFocusToken((token) => token + 1)
      let text = t("invalidCode")
      if (error instanceof ApiRequestError) {
        if (/Too many failed attempts/i.test(error.message)) text = t("tooManyAttempts")
        else if (/Invalid or expired/i.test(error.message)) text = t("invalidCode")
        else if (error.status === 0 || /non-JSON/i.test(error.message)) text = t("networkError")
        else text = error.message
      } else {
        text = t("networkError")
      }
      setStatus({ kind: "error", text })
    }
  }

  const busy = phase === "sending" || phase === "verifying" || phase === "verified"
  const sending = phase === "sending"
  const verifyEnabled = sent && digits.length === CODE_LENGTH && phase === "ready"
  const subtitle = context === "recovery" ? t("subtitleRecovery") : t("subtitle")

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="relative z-10 border-b border-border bg-background/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center px-4 sm:px-6 lg:px-8">
          <Logo />
        </div>
      </header>

      <main className="relative flex flex-1 items-center justify-center px-4 py-10 sm:py-12">
        <div
          aria-hidden
          className="absolute inset-0 bg-cover bg-center"
          style={{ backgroundImage: "url('/images/forgot-bg.jpg')" }}
        />
        <div aria-hidden className="absolute inset-0 bg-black/70" />

        <div className="relative z-10 w-full max-w-md">
          <div className="rounded-2xl border border-white/12 bg-card/70 p-7 shadow-2xl shadow-black/40 backdrop-blur-xl sm:p-8">
            <div className="mx-auto flex size-16 items-center justify-center rounded-2xl border border-teal/30 bg-teal/10 shadow-lg shadow-teal/10">
              <ShieldCheck className="size-8 text-teal" strokeWidth={1.75} aria-hidden />
            </div>

            <h1 className="mt-5 text-center text-2xl font-bold tracking-tight text-foreground">
              {t("title")}{" "}
              <span className="text-teal">{t("titleAccent")}</span>
            </h1>
            <p className="mt-2 text-center text-sm text-muted-foreground">
              {phase === "email" && !emailParam ? t("emailSubtitle") : subtitle}
            </p>

            {phase === "email" && !emailParam ? (
              <form
                className="mt-7 space-y-4"
                onSubmit={(event) => {
                  event.preventDefault()
                  const target = draftEmail.trim().toLowerCase()
                  if (!EMAIL_RE.test(target)) {
                    setStatus({ kind: "error", text: t("invalidEmail") })
                    return
                  }
                  void sendCode(target)
                }}
              >
                <div>
                  <label
                    htmlFor="otp-email"
                    className="block text-sm font-medium text-foreground"
                  >
                    {t("emailLabel")}
                  </label>
                  <input
                    id="otp-email"
                    type="email"
                    inputMode="email"
                    autoComplete="email"
                    autoFocus
                    placeholder={t("emailPlaceholder")}
                    value={draftEmail}
                    onChange={(event) => setDraftEmail(event.target.value)}
                    className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                  />
                </div>

                <div
                  id="otp-status"
                  role="status"
                  aria-live="polite"
                  className="min-h-5 text-sm"
                >
                  {status && (
                    <p
                      className={
                        status.kind === "error"
                          ? "text-destructive"
                          : status.kind === "success"
                            ? "text-teal"
                            : "text-muted-foreground"
                      }
                    >
                      {status.text}
                    </p>
                  )}
                </div>

                <Button type="submit" className="h-11 w-full" disabled={sending}>
                  {sending ? (
                    <>
                      <Loader2 className="animate-spin" aria-hidden />
                      {t("sendingCode")}
                    </>
                  ) : (
                    t("sendCode")
                  )}
                </Button>
              </form>
            ) : (
              <div className="mt-7 space-y-5">
                <p className="text-center text-sm text-muted-foreground">
                  {sent ? (
                    <>
                      {t("sentTo", { email: maskEmail(email) })}
                      <span className="sr-only"> ({email})</span>
                    </>
                  ) : (
                    t("sendingTo", { email: maskEmail(email) })
                  )}
                </p>

                <OtpInput
                  length={CODE_LENGTH}
                  value={digits}
                  onChange={(next) => {
                    if (phase === "ready" || phase === "verifying") setDigits(next)
                  }}
                  disabled={!sent || busy}
                  invalid={status?.kind === "error"}
                  success={phase === "verified"}
                  groupLabel={t("groupLabel")}
                  digitLabel={(index) => t("digitLabel", { n: index + 1 })}
                  describedBy="otp-status"
                  focusToken={focusToken}
                />

                <div
                  id="otp-status"
                  role="status"
                  aria-live="polite"
                  className="min-h-5 text-center text-sm"
                >
                  {status && (
                    <p
                      className={
                        status.kind === "error"
                          ? "text-destructive"
                          : status.kind === "success"
                            ? "text-teal"
                            : "text-muted-foreground"
                      }
                    >
                      {status.text}
                    </p>
                  )}
                </div>

                <div className="flex flex-col items-center gap-2 text-center text-sm text-muted-foreground">
                  <span>{t("didntReceive")}</span>
                  {cooldown > 0 ? (
                    <span className="font-medium tabular-nums text-foreground/80">
                      {t("resendIn", { time: formatCountdown(cooldown) })}
                    </span>
                  ) : sent && phase !== "verified" ? (
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      className="h-9"
                      disabled={sending}
                      onClick={() => void sendCode(email)}
                    >
                      {sending ? (
                        <>
                          <Loader2 className="animate-spin" aria-hidden />
                          {t("sendingCode")}
                        </>
                      ) : (
                        t("resendOtp")
                      )}
                    </Button>
                  ) : (
                    <span className="text-xs">{t("sendingCode")}</span>
                  )}
                </div>

                <Button
                  type="button"
                  className="h-11 w-full"
                  disabled={!verifyEnabled}
                  onClick={() => void verifyCode()}
                >
                  {phase === "verifying" ? (
                    <>
                      <Loader2 className="animate-spin" aria-hidden />
                      {t("verifying")}
                    </>
                  ) : phase === "verified" ? (
                    <>
                      <Check aria-hidden />
                      {t("verified")}
                    </>
                  ) : (
                    t("verifyCode")
                  )}
                </Button>

                <div className="flex items-center justify-between text-xs">
                  <Link
                    href="/login"
                    className="inline-flex items-center gap-1 text-muted-foreground transition-colors hover:text-foreground"
                  >
                    <ArrowLeft className="size-3.5" aria-hidden />
                    {ta("backToLogin")}
                  </Link>
                  <button
                    type="button"
                    className="text-muted-foreground transition-colors hover:text-foreground disabled:opacity-50"
                    disabled={sending}
                    onClick={() => {
                      setPhase("email")
                      setSent(false)
                      setCooldown(0)
                      setDigits("")
                      setStatus(null)
                    }}
                  >
                    {t("changeEmail")}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  )
}

export default function VerifyOtpPage() {
  return (
    <Suspense>
      <VerifyOtpForm />
    </Suspense>
  )
}
