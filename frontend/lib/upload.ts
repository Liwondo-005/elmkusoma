/**
 * Shared XHR upload transport with REAL network progress and cancellation.
 *
 * Progress comes from XMLHttpRequest `upload.onprogress` (actual bytes sent) —
 * never simulated. Cancellation uses AbortSignal → xhr.abort() (real request abort).
 * Error messages mirror the JSON envelopes produced by the existing backend
 * (ApiRequestError-compatible wording); safe to surface to users.
 */

export interface UploadOptions {
  /** Receives actual bytes-sent percentage (0-100) while the request is in flight. */
  onProgress?: (percent: number) => void
  /** Aborts the underlying request when triggered. */
  signal?: AbortSignal
}

export interface UploadRequest extends UploadOptions {
  url: string
  file: File
  method?: "POST" | "PUT"
  /** Form field name for the file bytes. Defaults to "file" (matches all existing endpoints). */
  fieldName?: string
  headers?: Record<string, string>
  /** Extra plain form fields sent alongside the file. */
  extraFields?: Record<string, string>
}

export class UploadError extends Error {
  readonly status: number
  readonly body: unknown

  constructor(message: string, status: number, body: unknown = null) {
    super(message)
    this.name = "UploadError"
    this.status = status
    this.body = body
  }
}

export function isAbortError(err: unknown): boolean {
  return (
    (err instanceof DOMException && err.name === "AbortError") ||
    (err instanceof Error && err.name === "AbortError")
  )
}

export function uploadFile({
  url,
  file,
  method = "POST",
  fieldName = "file",
  headers = {},
  extraFields,
  onProgress,
  signal,
}: UploadRequest): Promise<unknown> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(new DOMException("Upload aborted", "AbortError"))
      return
    }

    const xhr = new XMLHttpRequest()
    const abort = () => xhr.abort()

    const cleanup = () => {
      signal?.removeEventListener("abort", abort)
    }

    signal?.addEventListener("abort", abort)

    xhr.upload.addEventListener("progress", (event) => {
      if (event.lengthComputable && event.total > 0 && onProgress) {
        const pct = Math.round((event.loaded / event.total) * 100)
        onProgress(Math.min(pct, 100))
      }
    })

    xhr.addEventListener("abort", () => {
      cleanup()
      reject(new DOMException("Upload aborted", "AbortError"))
    })

    xhr.addEventListener("error", () => {
      cleanup()
      reject(new UploadError("Network error during upload", 0, null))
    })

    xhr.addEventListener("timeout", () => {
      cleanup()
      reject(new UploadError("Upload timed out", 0, null))
    })

    xhr.addEventListener("load", () => {
      cleanup()
      const status = xhr.status
      let body: unknown = null
      try {
        body = xhr.responseText ? JSON.parse(xhr.responseText) : null
      } catch {
        body = null
      }
      const okJson = body !== null && typeof body === "object"
      if (status >= 200 && status < 300) {
        if (!okJson) {
          reject(new UploadError(`Server returned non-JSON response (${status})`, status, null))
          return
        }
        const envelope = body as { success?: boolean; error?: string; message?: string }
        if (envelope.success === false) {
          reject(
            new UploadError(
              String(envelope.error || envelope.message || `Upload failed (${status})`),
              status,
              body,
            ),
          )
          return
        }
        resolve(body)
        return
      }
      // HTTP error status: surface the backend's safe message when present.
      const envelope = (okJson ? (body as { error?: string; message?: string }) : {}) || {}
      reject(
        new UploadError(
          String(envelope.error || envelope.message || `Upload failed (${status})`),
          status,
          body,
        ),
      )
    })

    const form = new FormData()
    if (extraFields) {
      for (const [key, value] of Object.entries(extraFields)) {
        form.append(key, value)
      }
    }
    form.append(fieldName, file, file.name)

    xhr.open(method, url)
    for (const [key, value] of Object.entries(headers)) {
      if (value) xhr.setRequestHeader(key, value)
    }
    xhr.send(form)
  })
}
