"use client"

import { useEffect, useRef, type ChangeEvent, type ClipboardEvent, type KeyboardEvent } from "react"
import { cn } from "@/lib/utils"

export interface OtpInputProps {
  /** Matches the backend's configured code length (`VerifyCodeRequest`: exactly 5 digits). */
  length?: number
  value: string
  onChange: (value: string) => void
  disabled?: boolean
  /** Format-level / backend-reported rejection — red tile treatment. */
  invalid?: boolean
  /** Backend-confirmed success — teal treatment. */
  success?: boolean
  groupLabel: string
  digitLabel: (index: number) => string
  describedBy?: string
  /** Bump to steal focus onto the first empty tile (e.g. after a send). */
  focusToken?: number
}

const DIGITS_RE = /\D/g

function onlyDigits(raw: string): string {
  return raw.replace(DIGITS_RE, "")
}

/**
 * Real multi-tile OTP control: typing, auto-advance, backspace navigation,
 * arrow keys, full-code paste and `autocomplete="one-time-code"` on the first
 * tile so mobile browsers can offer SMS/email autofill. Format-only by design —
 * correctness always belongs to the backend.
 */
export function OtpInput({
  length = 5,
  value,
  onChange,
  disabled = false,
  invalid = false,
  success = false,
  groupLabel,
  digitLabel,
  describedBy,
  focusToken,
}: OtpInputProps) {
  const refs = useRef<Array<HTMLInputElement | null>>([])

  const digits = Array.from({ length }, (_, i) => value[i] ?? "")

  useEffect(() => {
    if (focusToken === undefined) return
    const target = Math.min(value.length, length - 1)
    refs.current[target]?.focus()
    refs.current[target]?.select()
    // value intentionally omitted: focus should follow the token, not typing.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [focusToken])

  const writeFrom = (start: number, incoming: string) => {
    const next = digits.slice()
    let index = start
    for (const char of incoming) {
      if (index >= length) break
      next[index] = char
      index += 1
    }
    onChange(next.join(""))
    return index // first index after the last written digit
  }

  const focusAt = (index: number) => {
    const clamped = Math.max(0, Math.min(index, length - 1))
    refs.current[clamped]?.focus()
    refs.current[clamped]?.select()
  }

  const handleChange = (index: number) => (event: ChangeEvent<HTMLInputElement>) => {
    const raw = onlyDigits(event.target.value)
    if (!raw) {
      const next = digits.slice()
      next[index] = ""
      onChange(next.join(""))
      return
    }
    if (raw.length === 1) {
      writeFrom(index, raw)
      focusAt(index + 1)
      return
    }
    // Multi-digit value in one tile: autofill or paste landing here — fill on.
    const after = writeFrom(index, raw.slice(0, length - index))
    focusAt(after)
  }

  const handlePaste = (event: ClipboardEvent<HTMLInputElement>) => {
    event.preventDefault()
    if (disabled) return
    const raw = onlyDigits(event.clipboardData.getData("text")).slice(0, length)
    if (!raw) return
    const after = writeFrom(0, raw)
    focusAt(after < length ? after : length - 1)
  }

  const handleKeyDown = (index: number) => (event: KeyboardEvent<HTMLInputElement>) => {
    if (disabled) return
    if (event.key === "Backspace") {
      if (digits[index]) {
        // Let the change handler clear this tile, then stay put.
        return
      }
      event.preventDefault()
      if (index > 0) {
        const next = digits.slice()
        next[index - 1] = ""
        onChange(next.join(""))
        focusAt(index - 1)
      }
      return
    }
    if (event.key === "ArrowLeft") {
      event.preventDefault()
      focusAt(index - 1)
    } else if (event.key === "ArrowRight") {
      event.preventDefault()
      focusAt(index + 1)
    } else if (event.key === "Home") {
      event.preventDefault()
      focusAt(0)
    } else if (event.key === "End") {
      event.preventDefault()
      focusAt(length - 1)
    }
  }

  return (
    <div
      role="group"
      aria-label={groupLabel}
      aria-describedby={describedBy}
      className="relative flex items-center justify-center gap-2 sm:gap-3"
    >
      {/* Decorative connection line — purely visual, never interactive. */}
      <span
        aria-hidden
        className={cn(
          "pointer-events-none absolute inset-x-8 top-1/2 h-px -translate-y-1/2",
          "bg-[repeating-linear-gradient(90deg,currentColor_0_3px,transparent_3px_8px)] text-teal/50",
          "motion-reduce:hidden",
        )}
      />
      {digits.map((digit, index) => (
        <input
          key={index}
          ref={(el) => {
            refs.current[index] = el
          }}
          type="text"
          inputMode="numeric"
          pattern="[0-9]*"
          maxLength={1}
          autoComplete={index === 0 ? "one-time-code" : "off"}
          aria-label={digitLabel(index)}
          value={digit}
          disabled={disabled}
          onChange={handleChange(index)}
          onPaste={handlePaste}
          onKeyDown={handleKeyDown(index)}
          onFocus={(event) => event.target.select()}
          className={cn(
            "relative z-10 size-12 rounded-xl border bg-card/60 text-center text-xl font-semibold tabular-nums",
            "text-foreground caret-teal shadow-sm outline-none backdrop-blur transition-all duration-200 sm:size-14 sm:text-2xl",
            "focus-visible:ring-3 focus-visible:outline-none",
            "disabled:cursor-not-allowed disabled:opacity-60",
            invalid && !success
              ? "border-destructive/70 bg-destructive/5 text-destructive focus-visible:border-destructive focus-visible:ring-destructive/30"
              : success
                ? "border-teal bg-teal/10 text-teal motion-safe:scale-105"
                : digit
                  ? "border-teal/50 bg-card/80 focus-visible:border-teal focus-visible:ring-teal/40"
                  : "border-border focus-visible:border-teal focus-visible:ring-teal/40 motion-safe:hover:border-teal/40",
          )}
        />
      ))}
    </div>
  )
}
