"use client"

import { useRef, useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { useTranslations } from "next-intl"
import { Button } from "@/components/ui/button"
import { AlertCircle, CheckCircle, Send } from "lucide-react"
import { publicSiteApi, type ContactReceipt } from "@/lib/public-site-api"

type ContactValues = {
  name: string
  email: string
  subject: string
  message: string
}

export function ContactSection() {
  const t = useTranslations("home")
  const tc = useTranslations("common")
  const tSite = useTranslations("siteContact")
  const [receipt, setReceipt] = useState<ContactReceipt | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  // Honeypot values. A person never sees these; a bot fills every field it finds.
  const formStartedAt = useRef<number>(Date.now())

  const contactSchema = z.object({
    name: z.string().min(1, t("contact.errNameRequired")).min(2, t("contact.errNameShort")),
    email: z.string().min(1, t("contact.errEmailRequired")).email(t("contact.errEmailInvalid")),
    subject: z.string().min(1, t("contact.errSubjectRequired")).min(3, t("contact.errSubjectShort")),
    // Matches the server's minimum so the visitor is not told "too short" only after a
    // round trip.
    message: z.string().min(1, t("contact.errMessageRequired")).min(20, tSite("messageTooShort")),
  })

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ContactValues>({
    resolver: zodResolver(contactSchema),
  })

  async function onSubmit(values: ContactValues) {
    setSubmitError(null)
    try {
      const result = await publicSiteApi.submitContact({
        name: values.name,
        email: values.email,
        // The landing form has no category control, so it posts as a general enquiry rather
        // than inventing one the visitor never chose.
        category: "GENERAL",
        subject: values.subject,
        message: values.message,
        formStartedAt: formStartedAt.current,
      })
      setReceipt(result)
      reset()
      formStartedAt.current = Date.now()
    } catch (err) {
      // Server-side messages are safe to show: the contact endpoints return plain validation
      // text, never stack traces or SQL.
      setSubmitError(err instanceof Error ? err.message : tSite("submitFailed"))
    }
  }

  return (
    <section className="bg-muted/50 py-16 lg:py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-balance text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
            {tc("sendMessage")}
          </h2>
          <p className="mt-3 text-pretty text-muted-foreground">
            {t("contact.subtitle")}
          </p>
        </div>

        <div className="mx-auto mt-12 max-w-2xl">
          {receipt ? (
            <div className="rounded-2xl border border-border bg-card p-8 text-center shadow-xs">
              <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-teal/10">
                <CheckCircle className="size-7 text-teal" />
              </div>
              <h3 className="mt-4 text-xl font-bold text-foreground">{t("contact.successTitle")}</h3>
              <p className="mt-2 mx-auto max-w-sm text-sm text-muted-foreground">
                {tSite("storedReference", { reference: receipt.reference })}
              </p>
              {/* Delivery is reported as it actually is. When no support address is
                  configured the enquiry is safely stored but nobody was emailed, and saying
                  "we'll be in touch shortly" there would be a promise the platform cannot keep. */}
              {(receipt.notificationStatus === "QUEUED" || receipt.notificationStatus === "SENT") ? (
                <p className="mt-2 mx-auto max-w-sm text-sm text-muted-foreground">
                  {tSite("notified")}
                </p>
              ) : (
                <p
                  className="mt-3 mx-auto flex max-w-md items-start gap-2 rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-left text-xs text-amber-700 dark:text-amber-300"
                  role="status"
                >
                  <AlertCircle className="mt-0.5 size-3.5 shrink-0" aria-hidden="true" />
                  <span>
                    {receipt.notificationStatus === "FAILED"
                      ? tSite("storedNotNotifiedFailed")
                      : tSite("storedNotNotified")}
                  </span>
                </p>
              )}
              <Button onClick={() => setReceipt(null)} variant="outline" className="mt-6">
                {t("contact.backToForm")}
              </Button>
            </div>
          ) : (
            <div className="rounded-2xl border border-border bg-card p-6 shadow-xs sm:p-8">
              {submitError && (
                <div
                  role="alert"
                  className="mb-5 flex items-start gap-2 rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-sm text-destructive"
                >
                  <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
                  <span>{submitError}</span>
                </div>
              )}
              <form className="space-y-5" onSubmit={handleSubmit(onSubmit)}>
                {/* Honeypot. Hidden from people, and removed from the tab order so a keyboard
                    user cannot land on it. */}
                <div className="hidden" aria-hidden="true">
                  <label htmlFor="home-website">Website</label>
                  <input id="home-website" type="text" tabIndex={-1} autoComplete="off" name="website" />
                </div>
                <div className="grid gap-5 sm:grid-cols-2">
                  <div>
                    <label htmlFor="home-name" className="block text-sm font-medium text-foreground">
                      {t("contact.nameLabel")}
                    </label>
                    <input
                      id="home-name"
                      type="text"
                      placeholder={t("contact.namePlaceholder")}
                      {...register("name")}
                      className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                    />
                    {errors.name && (
                      <p className="mt-1.5 text-xs text-destructive">{errors.name.message}</p>
                    )}
                  </div>

                  <div>
                    <label htmlFor="home-email" className="block text-sm font-medium text-foreground">
                      {t("contact.emailLabel")}
                    </label>
                    <input
                      id="home-email"
                      type="email"
                      placeholder="you@example.com"
                      {...register("email")}
                      className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                    />
                    {errors.email && (
                      <p className="mt-1.5 text-xs text-destructive">{errors.email.message}</p>
                    )}
                  </div>
                </div>

                <div>
                    <label htmlFor="home-subject" className="block text-sm font-medium text-foreground">
                      {t("contact.subjectLabel")}
                    </label>
                    <input
                      id="home-subject"
                      type="text"
                      placeholder={t("contact.subjectPlaceholder")}
                      {...register("subject")}
                    className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                  />
                  {errors.subject && (
                    <p className="mt-1.5 text-xs text-destructive">{errors.subject.message}</p>
                  )}
                </div>

                <div>
                    <label htmlFor="home-message" className="block text-sm font-medium text-foreground">
                      {t("contact.messageLabel")}
                    </label>
                    <textarea
                      id="home-message"
                      rows={4}
                      placeholder={t("contact.messagePlaceholder")}
                    {...register("message")}
                    className="mt-1.5 w-full rounded-lg border border-border bg-muted/60 px-3.5 py-3 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background resize-none"
                  />
                  {errors.message && (
                    <p className="mt-1.5 text-xs text-destructive">{errors.message.message}</p>
                  )}
                </div>

                <Button type="submit" className="h-11 w-full text-sm" disabled={isSubmitting}>
                  {isSubmitting ? t("contact.sending") : <><Send className="mr-2 size-4" /> {t("contact.send")}</>}
                </Button>
              </form>
            </div>
          )}


        </div>
      </div>
    </section>
  )
}
