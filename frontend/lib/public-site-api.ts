import { appFetch } from "@/lib/fetch"

/**
 * Public site API — the anonymous surface backing the landing page, footer, contact form,
 * support page and legal pages.
 *
 * Contract: docs/API-CONTRACT-PUBLIC-SITE.md
 */

/** Keys the backend is willing to expose. Anything absent was never configured. */
export interface SiteSettings {
  "public.contact.email"?: string
  "public.contact.supportEmail"?: string
  "public.contact.phone"?: string
  /** Bare digits with country code, e.g. "255700000000". No "+" and no spaces. */
  "public.contact.whatsapp"?: string
  "public.contact.address"?: string
  "public.contact.workingHours"?: string
  "public.contact.responseTime"?: string
  "public.social.facebook"?: string
  "public.social.instagram"?: string
  "public.social.youtube"?: string
  "public.social.linkedin"?: string
  "public.social.tiktok"?: string
  "public.social.x"?: string
}

export interface ContactSubmission {
  name: string
  email: string
  category: string
  subject: string
  message: string
  /** Honeypot. Leave empty. */
  website?: string
  /** Honeypot: epoch millis the form was rendered. */
  formStartedAt?: number
}

/**
 * What the server actually recorded about notification.
 *
 * NOT_CONFIGURED means the enquiry is stored but no support address is set, so nobody was
 * emailed. QUEUED means the message was handed to the async mail worker — which is not proof
 * that it reached anybody, and is deliberately not reported as "sent". The form must not claim
 * delivery it cannot see; that distinction is the whole reason this endpoint returns the field.
 */
export type NotificationStatus =
  | "NOT_CONFIGURED"
  | "PENDING"
  | "QUEUED"
  | "SENT"
  | "FAILED"

export interface ContactReceipt {
  reference: string
  status: string
  receivedAt?: string
  notificationStatus?: NotificationStatus
}

export interface LegalDocumentView {
  type: string
  title: string
  version: number
  /** Plain text. Render as text nodes, never as HTML. */
  content: string
  effectiveDate?: string
  publishedAt?: string
}

const CONTACT_CATEGORIES = ["GENERAL", "SUPPORT", "PARTNERSHIP", "FEEDBACK", "COMPLAINT"] as const

export const publicSiteApi = {
  /** Approved public settings. Resolves to `{}` rather than throwing when unavailable. */
  getSettings: async (): Promise<SiteSettings> => {
    try {
      return await appFetch<SiteSettings>("/v1/public/site-settings")
    } catch {
      // A visitor should still get a working page if the settings call fails; the components
      // simply render no contact details rather than inventing any.
      return {}
    }
  },

  submitContact: (submission: ContactSubmission) =>
    appFetch<ContactReceipt>("/v1/public/contact", {
      method: "POST",
      body: JSON.stringify(submission),
    }),

  /** Published version only. Throws (404) when nothing has been published yet. */
  getLegalDocument: (type: "TERMS" | "PRIVACY" | "COOKIE" | "SUPPORT_POLICY") =>
    appFetch<LegalDocumentView>(`/v1/public/legal/${type}`),
}

export { CONTACT_CATEGORIES }