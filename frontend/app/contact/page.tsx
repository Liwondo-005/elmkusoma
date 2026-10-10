"use client"

import { useRef, useState } from "react"
import Link from "next/link"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { useTranslations } from "next-intl"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { Button } from "@/components/ui/button"
import { AlertCircle, Mail, Clock, MapPin, CheckCircle } from "lucide-react"
import { publicSiteApi, CONTACT_CATEGORIES, type ContactReceipt } from "@/lib/public-site-api"
import { useSiteSettings } from "@/hooks/use-site-settings"

type ContactValues = {
  name: string
  email: string
  category: string
  subject: string
  message: string
}

export default function ContactPage() {
  const t = useTranslations("public")
  const tSite = useTranslations("siteContact")
  const { value } = useSiteSettings()
  const [receipt, setReceipt] = useState<ContactReceipt | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const formStartedAt = useRef<number>(Date.now())

  const contactEmail = value("public.contact.email")
  const contactPhone = value("public.contact.phone")
  const contactAddress = value("public.contact.address")
  const responseTime = value("public.contact.responseTime")
  const workingHours = value("public.contact.workingHours")

  const contactSchema = z.object({
    name: z.string().min(1, t("contact.nameRequired")).min(2, t("contact.nameTooShort")),
    email: z.string().min(1, t("contact.emailRequired")).email(t("contact.emailInvalid")),
    category: z.string().min(1, tSite("categoryRequired")),
    subject: z.string().min(1, t("contact.subjectRequired")).min(4, t("contact.subjectTooShort")),
    // Server minimum is 20; matching it here avoids a round trip to be told "too short".
    message: z.string().min(1, t("contact.messageRequired")).min(20, tSite("messageTooShort")),
  })

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ContactValues>({
    resolver: zodResolver(contactSchema),
    defaultValues: { category: "GENERAL" },
  })

  async function onSubmit(values: ContactValues) {
    setSubmitError(null)
    try {
      const result = await publicSiteApi.submitContact({
        name: values.name,
        email: values.email,
        category: values.category,
        subject: values.subject,
        message: values.message,
        formStartedAt: formStartedAt.current,
      })
      setReceipt(result)
      reset({ category: values.category })
      formStartedAt.current = Date.now()
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : tSite("submitFailed"))
    }
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        {/* Hero */}
        <section className="bg-muted/50 py-16 lg:py-20">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="mx-auto max-w-2xl text-center">
              <h1 className="text-balance text-4xl font-extrabold tracking-tight text-foreground sm:text-5xl">
                {t("contact.title")}
              </h1>
              <p className="mt-4 text-pretty text-base leading-relaxed text-muted-foreground">
                {t("contact.subtitle")}
              </p>
            </div>
          </div>
        </section>

        <section className="py-16 lg:py-20">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="grid gap-12 lg:grid-cols-[1fr_1.2fr]">
              {/* Contact details */}
              <div>
                <h2 className="text-2xl font-bold tracking-tight text-foreground">
                  {t("contact.infoTitle")}
                </h2>
                <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
                  {t("contact.infoDesc")}
                </p>

                <div className="mt-8 space-y-6">
                  {contactEmail && (
                    <div className="rounded-2xl border border-border bg-card p-5">
                      <div className="flex items-center gap-2">
                        <Mail className="size-4 text-muted-foreground" />
                        <h3 className="text-sm font-semibold text-foreground">{t("contact.emailTitle")}</h3>
                      </div>
                      <a
                        href={`mailto:${contactEmail}`}
                        className="mt-1.5 block text-sm text-primary hover:underline"
                      >
                        {contactEmail}
                      </a>
                      <p className="mt-1 text-xs text-muted-foreground">
                        {t("contact.emailDesc")}
                      </p>
                    </div>
                  )}

                  {(responseTime || workingHours) && (
                    <div className="rounded-2xl border border-border bg-card p-5">
                      <div className="flex items-center gap-2">
                        <Clock className="size-4 text-muted-foreground" />
                        <h3 className="text-sm font-semibold text-foreground">{t("contact.responseTitle")}</h3>
                      </div>
                      {responseTime && (
                        <p className="mt-1.5 text-sm text-muted-foreground">{responseTime}</p>
                      )}
                      {workingHours && (
                        <p className="mt-1 text-sm text-muted-foreground">{workingHours}</p>
                      )}
                    </div>
                  )}

                  {contactAddress && (
                    <div className="rounded-2xl border border-border bg-card p-5">
                      <div className="flex items-center gap-2">
                        <MapPin className="size-4 text-muted-foreground" />
                        <h3 className="text-sm font-semibold text-foreground">{t("contact.locationTitle")}</h3>
                      </div>
                      <p className="mt-1.5 text-sm text-muted-foreground">{contactAddress}</p>
                    </div>
                  )}
                </div>
              </div>

              {/* Contact form */}
              <div className="rounded-2xl border border-border bg-card p-6 shadow-xs sm:p-8">
                {receipt ? (
                  <div className="flex flex-col items-center justify-center py-12 text-center">
                    <div className="flex size-14 items-center justify-center rounded-full bg-teal/10">
                      <CheckCircle className="size-7 text-teal" />
                    </div>
                    <h2 className="mt-4 text-xl font-bold text-foreground">{tSite("receivedTitle")}</h2>
                    <p className="mt-2 max-w-sm text-sm text-muted-foreground">
                      {tSite("storedReference", { reference: receipt.reference })}
                    </p>
                    {/* Truthful about delivery. Claiming "we'll reply within 24 hours" when no
                        support address is configured is a promise the platform cannot keep. */}
                    {receipt.notificationStatus === "SENT" ? (
                      <p className="mt-2 max-w-sm text-sm text-muted-foreground">{tSite("notified")}</p>
                    ) : (
                      <p
                        className="mt-3 flex max-w-md items-start gap-2 rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-left text-xs text-amber-700 dark:text-amber-300"
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
                  <>
                    <h2 className="text-xl font-bold tracking-tight text-foreground">
                      {t("contact.formTitle")}
                    </h2>
                    <p className="mt-2 text-sm text-muted-foreground">
                      {t("contact.formDesc")}
                    </p>

                    <form className="mt-8 space-y-5" onSubmit={handleSubmit(onSubmit)}>
                      {submitError && (
                        <div
                          role="alert"
                          className="flex items-start gap-2 rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-sm text-destructive"
                        >
                          <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
                          <span>{submitError}</span>
                        </div>
                      )}

                      {/* Honeypot: hidden from people, out of the tab order for keyboards. */}
                      <div className="hidden" aria-hidden="true">
                        <label htmlFor="website">Website</label>
                        <input
                          id="website"
                          type="text"
                          tabIndex={-1}
                          autoComplete="off"
                          name="website"
                        />
                      </div>

                      <div className="grid gap-5 sm:grid-cols-2">
                        <div>
                          <label htmlFor="name" className="block text-sm font-medium text-foreground">
                            {t("contact.nameLabel")}
                          </label>
                          <input
                            id="name"
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
                          <label htmlFor="email" className="block text-sm font-medium text-foreground">
                            {t("contact.emailLabel")}
                          </label>
                          <input
                            id="email"
                            type="email"
                            placeholder="you@example.com"
                            {...register("email")}
                            className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                          />
                          {errors.email && (
                            <p className="mt-1.5 text-xs text-destructive">{errors.email.message}</p>
                          )}
                        </div>
                      <div>
                          <label htmlFor="category" className="block text-sm font-medium text-foreground">
                            {tSite("categoryLabel")}
                          </label>
                          <select
                            id="category"
                            {...register("category")}
                            className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3 text-sm text-foreground outline-none transition-colors focus:border-ring focus:bg-background"
                          >
                            {CONTACT_CATEGORIES.map((option) => (
                              <option key={option} value={option}>
                                {tSite(`category.${option}`)}
                              </option>
                            ))}
                          </select>
                          {errors.category && (
                            <p className="mt-1.5 text-xs text-destructive">{errors.category.message}</p>
                          )}
                        </div>
                      </div>

                      <div>
                        <label htmlFor="subject" className="block text-sm font-medium text-foreground">
                          {t("contact.subjectLabel")}
                        </label>
                        <input
                          id="subject"
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
                        <label htmlFor="message" className="block text-sm font-medium text-foreground">
                          {t("contact.messageLabel")}
                        </label>
                        <textarea
                          id="message"
                          rows={5}
                          placeholder={t("contact.messagePlaceholder")}
                          {...register("message")}
                          className="mt-1.5 w-full rounded-lg border border-border bg-muted/60 px-3.5 py-3 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background resize-none"
                        />
                        {errors.message && (
                          <p className="mt-1.5 text-xs text-destructive">{errors.message.message}</p>
                        )}
                      </div>

                      <Button type="submit" className="h-11 w-full text-sm" disabled={isSubmitting}>
                        {isSubmitting ? t("contact.submitting") : t("contact.submit")}
                      </Button>
                    </form>
                  </>
                )}
              </div>
            </div>
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  )
}
