/**
 * Payment status presentation.
 *
 * `payments.status` is a free-text VARCHAR(20) on the backend, not an enum: nothing validates it,
 * so a value outside the set below can and does arrive. Two rules follow, and both matter more
 * than the colours:
 *
 *  1. An unrecognised status is shown verbatim and marked unknown. It is never folded into
 *     "pending" or "completed" to make the list look tidy - a status nobody can classify is a
 *     fact the operator needs to see, and quietly relabelling it is how a bad row goes unnoticed.
 *
 *  2. Only COMPLETED is ever presented as a settled payment. The list of values below is the
 *     backend's own observed vocabulary, not a frontend invention.
 */

export type PaymentStatusTone = "settled" | "awaiting" | "problem" | "neutral" | "unknown";

export interface PaymentStatusPresentation {
  /** True only for a payment the backend has confirmed as collected. */
  settled: boolean;
  tone: PaymentStatusTone;
  /**
   * Whether this status represents a live attempt, i.e. one that may still change. Used to decide
   * whether "cancel" is offered, and to keep pending work out of any "paid" total.
   */
  inFlight: boolean;
}

/** The status values the backend service layer actually writes. */
const PRESENTATION: Record<string, PaymentStatusPresentation> = {
  PENDING: { settled: false, tone: "awaiting", inFlight: true },
  PROCESSING: { settled: false, tone: "awaiting", inFlight: true },
  COMPLETED: { settled: true, tone: "settled", inFlight: false },
  FAILED: { settled: false, tone: "problem", inFlight: false },
  CANCELLED: { settled: false, tone: "neutral", inFlight: false },
  REFUNDED: { settled: false, tone: "problem", inFlight: false },
};

/**
 * Classifies a raw status string.
 *
 * Case-insensitive, because the value is stored as written and the webhook path writes it from
 * a payload. An unknown or missing value yields the `unknown` presentation, which is settled:
 * false and inFlight: false - so it is never counted as paid and never offered a cancel.
 */
export function presentPaymentStatus(status: string | null | undefined): PaymentStatusPresentation {
  const key = (status ?? "").trim().toUpperCase();
  return (
    PRESENTATION[key] ?? { settled: false, tone: "unknown", inFlight: false }
  );
}

/** True only when the backend has confirmed the payment as collected. */
export function isSettled(status: string | null | undefined): boolean {
  return presentPaymentStatus(status).settled;
}

/** True while a payment attempt may still change - the only states a cancel applies to. */
export function isCancellable(status: string | null | undefined): boolean {
  return presentPaymentStatus(status).inFlight;
}

/**
 * Masks a gateway reference for display (Payment.md §28: "Gateway reference, appropriately
 * masked").
 *
 * The reference is an operational token, not something the payer needs: printing it in full
 * on a screen or into an exported CSV is exactly how a gateway identifier ends up in a
 * screenshot, a support ticket or a shared spreadsheet. Only the last four characters
 * survive, which is enough for a support conversation to match a value without disclosing it.
 *
 * Three properties that matter to callers:
 *
 *  - `null`, `undefined` and blank all stay null. A payment that never received a reference
 *    has nothing to hide, and rendering a row of asterisks would read as a real value.
 *  - A value of four characters or fewer is masked entirely - keeping any of it would leave
 *    the whole token visible.
 *  - It is idempotent, so a reference the backend has already masked passes through
 *    unchanged instead of being masked down to `****` plus four asterisks.
 */
export function maskProviderReference(
  reference: string | null | undefined,
): string | null {
  if (reference == null) return null;
  const value = reference.trim();
  if (!value) return null;
  if (value.length <= 4) return "*".repeat(value.length);
  return `****${value.slice(-4)}`;
}

/** Tailwind classes per tone, matching the muted palette the existing dashboards use. */
export function statusToneClasses(tone: PaymentStatusTone): string {
  switch (tone) {
    case "settled":
      return "bg-green-500/10 text-green-700 dark:text-green-400"
    case "awaiting":
      return "bg-amber-500/10 text-amber-700 dark:text-amber-400"
    case "problem":
      return "bg-red-500/10 text-red-700 dark:text-red-400"
    case "neutral":
      return "bg-muted text-muted-foreground"
    case "unknown":
    default:
      // Deliberately visually distinct: an unrecognised status should look like it needs a look.
      return "bg-purple-500/10 text-purple-700 dark:text-purple-400"
  }
}

/**
 * Describes how an access grant ends, from its `expiresAt` alone.
 *
 * <p>Three outcomes, and the distinction matters: access that has <em>ended</em> is different from
 * access that was never time-limited. A null expiry is reported as "no end date", never as
 * "expired" and never as a number of days.</p>
 *
 * <p>`now` is injected so a list rendered once does not disagree with itself as the clock moves,
 * and so a test can pin the moment being described.</p>
 */
export function describeExpiry(
  expiresAt: string | null | undefined,
  now: number = Date.now(),
): { expiring: boolean; expired: boolean; daysRemaining: number | null } {
  if (!expiresAt) {
    return { expiring: false, expired: false, daysRemaining: null }
  }
  const parsed = new Date(expiresAt).getTime()
  if (Number.isNaN(parsed)) {
    // An unparseable date is not an expiry we can reason about, so it is not presented as one.
    return { expiring: false, expired: false, daysRemaining: null }
  }
  const msRemaining = parsed - now
  return {
    expiring: true,
    expired: msRemaining <= 0,
    daysRemaining: Math.ceil(msRemaining / 86_400_000),
  }
}
