/**
 * Money display for the payment surfaces.
 *
 * Every amount on screen is a value the backend sent. Nothing here derives, converts or sums a
 * price: the authoritative amount is decided server-side, and a client-side total would be a
 * guess wearing the costume of a receipt.
 *
 * Two things this deliberately does not do:
 *  - It does not hard-code "TZS". The backend stores currency as a string and will accept any
 *    value, so the symbol follows the row being displayed rather than the page being rendered.
 *  - It does not round away decimals. Payments are DECIMAL(12,2); showing "TSh 500" for a
 *    499.50 charge would misstate what someone owes.
 */

/** Codes Intl can format. Anything else falls back to a plain "CODE 1,234.50". */
const ISO_CODE = /^[A-Za-z]{3}$/;

/**
 * Formats an amount for display.
 *
 * @param amount   the value from the API. Accepts a JSON number or a numeric string, because
 *                 BigDecimal is serialized as a number but a decimal string is what a future
 *                 precision-safe API would send.
 * @param currency the currency code from the same record, if the API supplied one.
 * @param locale   BCP-47 tag; falls back to the runtime default.
 *
 * @returns a display string, or an em dash when there is no amount to show. Never "NaN",
 *          never "TSh 0" standing in for an absent value, because an absent amount and a
 *          zero amount are different facts.
 */
export function formatMoney(
  amount: number | string | null | undefined,
  currency?: string | null,
  locale?: string,
): string {
  const numeric = toNumber(amount);
  if (numeric === null) return "—";

  const code = currency?.trim().toUpperCase();

  if (code && ISO_CODE.test(code)) {
    try {
      return new Intl.NumberFormat(locale, {
        style: "currency",
        currency: code,
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
      }).format(numeric);
    } catch {
      // An unknown-but-well-formed code still deserves a readable rendering.
      return `${code} ${formatPlain(numeric, locale)}`;
    }
  }
  if (code) return `${code} ${formatPlain(numeric, locale)}`;
  return formatPlain(numeric, locale);
}

/**
 * Parses an amount without letting a malformed value become a misleading number.
 * Returns null rather than NaN so callers must decide what "absent" means in their context.
 */
function toNumber(amount: number | string | null | undefined): number | null {
  if (amount === null || amount === undefined) return null;
  if (typeof amount === "number") return Number.isFinite(amount) ? amount : null;

  const trimmed = amount.trim();
  if (trimmed === "") return null;
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? parsed : null;
}

function formatPlain(numeric: number, locale?: string): string {
  try {
    return new Intl.NumberFormat(locale, {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2,
    }).format(numeric);
  } catch {
    return numeric.toFixed(2);
  }
}

/**
 * Sums amounts already fetched from the API.
 *
 * Exists so that a subtotal shown on screen is auditable back to the rows the user can see.
 * Callers must only pass a complete result set - summing one page of a paged response and
 * labelling it a total would misstate the figure, which is why there is no page-aware helper
 * here to reach for by mistake.
 */
export function sumAmounts(amounts: Array<number | string | null | undefined>): number {
  return amounts.reduce<number>((total, amount) => {
    const numeric = toNumber(amount);
    return numeric === null ? total : total + numeric;
  }, 0);
}
