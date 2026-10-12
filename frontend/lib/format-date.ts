const EN_GB = "en-GB"

function resolve(locale: string | undefined): string {
  return locale === "sw" ? "sw-TZ" : EN_GB
}

export function formatDate(
  value: string | number | Date | null | undefined,
  locale?: string,
  options?: Intl.DateTimeFormatOptions
): string {
  if (value === null || value === undefined || value === "") return ""
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ""
  return date.toLocaleDateString(resolve(locale), options)
}

export function formatDateTime(
  value: string | number | Date | null | undefined,
  locale?: string,
  options?: Intl.DateTimeFormatOptions
): string {
  if (value === null || value === undefined || value === "") return ""
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ""
  return date.toLocaleString(resolve(locale), options)
}

export function formatTime(
  value: string | number | Date | null | undefined,
  locale?: string,
  options?: Intl.DateTimeFormatOptions
): string {
  if (value === null || value === undefined || value === "") return ""
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ""
  return date.toLocaleTimeString(resolve(locale), options)
}
