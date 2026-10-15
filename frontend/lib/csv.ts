/**
 * CSV export for the payment surfaces.
 *
 * Scope, stated plainly because it is the thing most likely to be misread: this exports the rows
 * currently held by the page, not the whole ledger. The backend list endpoints are paged and
 * expose no bulk-export route, so a "full export" produced here would silently omit every row
 * that did not fit on the first page - which for a financial report is worse than no export at
 * all. Callers label the file accordingly.
 *
 * Everything written comes from a record already rendered on screen. No value is derived, summed
 * or recomputed here; in particular no total row is emitted, because a total over one page would
 * read as a platform-wide figure.
 */

export interface CsvColumn<T> {
  header: string
  value: (row: T) => string | number | null | undefined
}

/** Escapes a single CSV field per RFC 4180. */
function escapeField(value: string | number | null | undefined): string {
  if (value === null || value === undefined) return ""
  const text = String(value)
  // A leading =, +, - or @ is executed by spreadsheet software when the file is opened.
  // Prefixing with a quote neutralises that, so an attacker-controlled service type or
  // description cannot turn an export into a formula injection.
  const needsGuard = /^[=+\-@\t\r]/.test(text)
  const guarded = needsGuard ? `'${text}` : text
  return /[",\r\n]/.test(guarded) ? `"${guarded.replace(/"/g, '""')}"` : guarded
}

/** Serialises rows to CSV text. Always ends with a line break so the file ends cleanly. */
export function toCsv<T>(rows: T[], columns: CsvColumn<T>[]): string {
  const header = columns.map((c) => escapeField(c.header)).join(",")
  const body = rows.map((row) => columns.map((c) => escapeField(c.value(row))).join(","))
  return [header, ...body].join("\r\n") + "\r\n"
}

/**
 * Triggers a browser download of the CSV.
 *
 * The object URL is revoked on the next tick; leaving it alive holds the whole file in memory for
 * the lifetime of the document, which matters on the low-bandwidth devices this is used on.
 */
export function downloadCsv(filename: string, csv: string): void {
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" })
  const url = URL.createObjectURL(blob)
  const link = document.createElement("a")
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/** A dated filename, so an export is identifiable after the fact. */
export function exportFilename(prefix: string, now: Date = new Date()): string {
  const stamp = [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, "0"),
    String(now.getDate()).padStart(2, "0"),
  ].join("-")
  return `${prefix}-${stamp}.csv`
}
