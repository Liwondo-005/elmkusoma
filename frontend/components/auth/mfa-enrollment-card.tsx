"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import QRCode from "qrcode"
import { ShieldCheck, Loader2, AlertCircle, CheckCircle, Copy, RefreshCw } from "lucide-react"
import { authApi } from "@/lib/api"

/**
 * Self-service two-step verification: enroll an authenticator app (TOTP via
 * QR code), confirm it, and manage single-use recovery codes.
 */
export function MfaEnrollmentCard() {
  const t = useTranslations("auth")
  const [status, setStatus] = useState<{ enrolled: boolean; remainingCodes: number } | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [enrollment, setEnrollment] = useState<{
    factorId: string
    otpauthUri: string
    base32Secret: string
    recoveryCodes: string[]
  } | null>(null)
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null)
  const [code, setCode] = useState("")
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  async function loadStatus() {
    try {
      setLoading(true)
      setError(null)
      const s = await authApi.mfaStatus()
      setStatus({ enrolled: s.enrolled, remainingCodes: s.remainingCodes })
    } catch (err) {
      setError(err instanceof Error ? err.message : t("mfaStatusError"))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadStatus()
  }, [])

  async function startEnroll() {
    setBusy(true)
    setError(null)
    setNotice(null)
    try {
      const res = await authApi.mfaEnroll()
      setEnrollment({
        factorId: res.factorId,
        otpauthUri: res.otpauthUri,
        base32Secret: res.base32Secret,
        recoveryCodes: res.recoveryCodes,
      })
      setQrDataUrl(await QRCode.toDataURL(res.otpauthUri, { width: 220, margin: 1 }))
      setCode("")
    } catch (err) {
      setError(err instanceof Error ? err.message : t("mfaEnrollError"))
    } finally {
      setBusy(false)
    }
  }

  async function confirmEnroll() {
    if (!enrollment) return
    setBusy(true)
    setError(null)
    try {
      await authApi.mfaConfirm({ factorId: enrollment.factorId, code: code.trim() })
      setEnrollment(null)
      setQrDataUrl(null)
      setCode("")
      setNotice(t("mfaEnabledNotice"))
      await loadStatus()
    } catch (err) {
      setError(err instanceof Error ? err.message : t("mfaConfirmError"))
    } finally {
      setBusy(false)
    }
  }

  async function regenerate() {
    setBusy(true)
    setError(null)
    try {
      const res = await authApi.regenerateRecoveryCodes()
      setEnrollment({
        factorId: "",
        otpauthUri: "",
        base32Secret: "",
        recoveryCodes: res.recoveryCodes || [],
      })
      setNotice(t("mfaCodesRegenerated"))
      await loadStatus()
    } catch (err) {
      setError(err instanceof Error ? err.message : t("mfaCodesError"))
    } finally {
      setBusy(false)
    }
  }

  function copyCodes(codes: string[]) {
    const text = codes.join("\n")
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text).catch(() => {})
    }
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
      <div className="flex items-center gap-3">
        <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10">
          <ShieldCheck className="size-5 text-primary" />
        </div>
        <div>
          <h2 className="text-lg font-semibold text-foreground">{t("mfaTitle")}</h2>
          <p className="text-sm text-muted-foreground">{t("mfaDescription")}</p>
        </div>
      </div>

      {loading ? (
        <p className="mt-4 flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 className="size-4 animate-spin" /> {t("loading")}
        </p>
      ) : (
        <div className="mt-4 space-y-4">
          <p className="text-sm">
            {status?.enrolled ? (
              <span className="inline-flex rounded-full bg-teal/10 px-2.5 py-0.5 text-xs font-semibold text-teal">
                {t("mfaEnabledBadge")}
              </span>
            ) : (
              <span className="inline-flex rounded-full bg-muted px-2.5 py-0.5 text-xs font-semibold text-muted-foreground">
                {t("mfaDisabledBadge")}
              </span>
            )}
            <span className="ml-2 text-muted-foreground">
              {t("mfaCodesRemaining", { count: status?.remainingCodes ?? 0 })}
            </span>
          </p>

          {error && (
            <p className="flex items-start gap-2 text-sm text-destructive">
              <AlertCircle className="mt-0.5 size-4 shrink-0" /> {error}
            </p>
          )}
          {notice && (
            <p className="flex items-start gap-2 text-sm text-teal">
              <CheckCircle className="mt-0.5 size-4 shrink-0" /> {notice}
            </p>
          )}

          {!status?.enrolled && !enrollment && (
            <button
              type="button"
              onClick={startEnroll}
              disabled={busy}
              className="h-11 rounded-lg bg-primary px-6 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              {busy ? t("mfaStarting") : t("mfaEnrollButton")}
            </button>
          )}

          {enrollment && enrollment.factorId && (
            <div className="rounded-xl border border-border p-4">
              <p className="text-sm font-medium text-foreground">{t("mfaScanTitle")}</p>
              <p className="mt-1 text-xs text-muted-foreground">{t("mfaScanHint")}</p>
              {qrDataUrl && (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={qrDataUrl} alt={t("mfaQrAlt")} width={220} height={220} className="mt-3 rounded-lg border border-border" />
              )}
              <p className="mt-3 text-xs text-muted-foreground">{t("mfaManualEntry")}</p>
              <code className="mt-1 block break-all rounded bg-muted px-2 py-1 font-mono text-xs">
                {enrollment.base32Secret}
              </code>
              <div className="mt-3 flex gap-2">
                <input
                  type="text"
                  inputMode="numeric"
                  maxLength={6}
                  placeholder="000000"
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
                  className="h-11 w-40 rounded-lg border border-border bg-muted/60 px-3.5 text-center text-lg tracking-[0.5em] outline-none focus:border-ring focus:bg-background"
                  aria-label={t("mfaCodeLabel")}
                />
                <button
                  type="button"
                  onClick={confirmEnroll}
                  disabled={busy || code.length !== 6}
                  className="h-11 rounded-lg bg-primary px-6 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                >
                  {busy ? t("verifying") : t("mfaConfirmButton")}
                </button>
              </div>
            </div>
          )}

          {(enrollment?.recoveryCodes?.length || (status?.enrolled && false)) && (
            <div className="rounded-xl border border-amber-500/30 bg-amber-500/5 p-4">
              <p className="text-sm font-medium text-foreground">{t("mfaCodesTitle")}</p>
              <p className="mt-1 text-xs text-muted-foreground">{t("mfaCodesWarning")}</p>
              <ul className="mt-2 grid grid-cols-2 gap-1 font-mono text-sm">
                {(enrollment?.recoveryCodes || []).map((c) => (
                  <li key={c} className="rounded bg-muted px-2 py-1">
                    {c}
                  </li>
                ))}
              </ul>
              <button
                type="button"
                onClick={() => copyCodes(enrollment?.recoveryCodes || [])}
                className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
              >
                <Copy className="size-3" /> {copied ? t("copied") : t("copyCodes")}
              </button>
            </div>
          )}

          {status?.enrolled && (
            <button
              type="button"
              onClick={regenerate}
              disabled={busy}
              className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:underline disabled:opacity-50"
            >
              <RefreshCw className="size-3.5" /> {t("mfaRegenerateCodes")}
            </button>
          )}
        </div>
      )}
    </div>
  )
}
