"use client"

import { useRef, useState } from "react"
import { useTranslations } from "next-intl"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { Button } from "@/components/ui/button"
import {
  AlertCircle,
  Mail,
  Phone,
  MessageSquare,
  CheckCircle,
  ChevronDown,
  ChevronUp,
} from "lucide-react"
import { publicSiteApi, type ContactReceipt } from "@/lib/public-site-api"
import { useSiteSettings, telHref, whatsappClickToChat } from "@/hooks/use-site-settings"

export default function SupportPage() {
  const t = useTranslations("public")
  const tSite = useTranslations("siteContact")
  const { value } = useSiteSettings()
  const [receipt, setReceipt] = useState<ContactReceipt | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [sending, setSending] = useState(false)
  const [openFaq, setOpenFaq] = useState<number | null>(null)
  const formStartedAt = useRef<number>(Date.now())

  const supportEmail = value("public.contact.supportEmail") ?? value("public.contact.email")
  const supportPhone = value("public.contact.phone")
  const phoneHref = telHref(supportPhone)
  const whatsappHref = whatsappClickToChat(value("public.contact.whatsapp"))

  const faqs = [
    { q: t("support.faq1q"), a: t("support.faq1a") },
    { q: t("support.faq2q"), a: t("support.faq2a") },
    { q: t("support.faq3q"), a: t("support.faq3a") },
    { q: t("support.faq4q"), a: t("support.faq4a") },
    { q: t("support.faq5q"), a: t("support.faq5a") },
    { q: t("support.faq6q"), a: t("support.faq6a") },
  ]

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setSubmitError(null)
    const form = e.currentTarget
    const data = new FormData(form)
    setSending(true)
    try {
      const result = await publicSiteApi.submitContact({
        name: String(data.get("name") ?? ""),
        email: String(data.get("email") ?? ""),
        category: "SUPPORT",
        subject: String(data.get("subject") ?? ""),
        message: String(data.get("message") ?? ""),
        formStartedAt: formStartedAt.current,
      })
      setReceipt(result)
      form.reset()
      formStartedAt.current = Date.now()
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : tSite("submitFailed"))
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <section className="mx-auto max-w-5xl px-4 py-16 sm:px-6 lg:px-8">
          <h1 className="text-3xl font-bold tracking-tight text-foreground">{t("support.title")}</h1>
          <p className="mt-2 text-muted-foreground">
            {t("support.subtitle")}
          </p>

          <div className="mt-12 grid gap-10 lg:grid-cols-2">
            {/* Contact Options */}
            <div>
              <h2 className="text-lg font-semibold text-foreground">{t("support.contactTitle")}</h2>
              <div className="mt-4 space-y-4">
                {/* Every option below renders only when an administrator has configured it.
                    A placeholder phone number or an invented account is worse than no row. */}
                {supportEmail && (
                  <a
                    href={`mailto:${supportEmail}`}
                    className="flex items-center gap-4 rounded-xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
                  >
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                      <Mail className="size-5" />
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-foreground">{t("support.emailUs")}</p>
                      <p className="text-sm text-muted-foreground">{supportEmail}</p>
                    </div>
                  </a>
                )}

                {supportPhone && phoneHref && (
                  <a
                    href={phoneHref}
                    className="flex items-center gap-4 rounded-xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
                  >
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-teal/10 text-teal">
                      <Phone className="size-5" />
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-foreground">{t("support.callUs")}</p>
                      <p className="text-sm text-muted-foreground">{supportPhone}</p>
                    </div>
                  </a>
                )}

                {whatsappHref && (
                  <a
                    href={whatsappHref}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-4 rounded-xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
                  >
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-600">
                      <MessageSquare className="size-5" />
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-foreground">{t("support.whatsapp")}</p>
                      {/* Says what a click-to-chat link actually does, rather than implying a
                          background channel to the support team exists. */}
                      <p className="text-sm text-muted-foreground">{tSite("whatsappOpensChat")}</p>
                    </div>
                  </a>
                )}
              </div>

              {/* Contact Form */}
              <div className="mt-8">
                <h2 className="text-lg font-semibold text-foreground">{t("support.formTitle")}</h2>
                {receipt ? (
                  <div className="mt-4 rounded-xl border border-border bg-card p-5">
                    <div className="flex items-center gap-3 text-sm text-foreground">
                      <CheckCircle className="size-5 shrink-0 text-teal" />
                      <span className="font-medium">{tSite("receivedTitle")}</span>
                    </div>
                    <p className="mt-2 text-sm text-muted-foreground">
                      {tSite("storedReference", { reference: receipt.reference })}
                    </p>
                    {/* Previously this page set local state and showed "Message sent! We will
                        get back to you within 24 hours." Nothing was sent at all. The state is
                        now reported as the server reports it. */}
                    {receipt.notificationStatus !== "SENT" && (
                      <p
                        className="mt-3 flex items-start gap-2 rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-xs text-amber-700 dark:text-amber-300"
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
                    <Button
                      type="button"
                      variant="outline"
                      className="mt-4"
                      onClick={() => setReceipt(null)}
                    >
                      {tSite("sendAnother")}
                    </Button>
                  </div>
                ) : (
                  <form className="mt-4 space-y-4" onSubmit={handleSubmit}>
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
                      <label htmlFor="support-website">Website</label>
                      <input
                        id="support-website"
                        type="text"
                        name="website"
                        tabIndex={-1}
                        autoComplete="off"
                      />
                    </div>

                    <div>
                      <label htmlFor="name" className="block text-sm font-medium text-foreground">{t("support.nameLabel")}</label>
                      <input
                        id="name"
                        name="name"
                        type="text"
                        required
                        minLength={2}
                        maxLength={120}
                        placeholder={t("support.namePlaceholder")}
                        className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                      />
                    </div>
                    <div>
                      <label htmlFor="email" className="block text-sm font-medium text-foreground">{t("support.emailLabel")}</label>
                      <input
                        id="email"
                        name="email"
                        type="email"
                        required
                        maxLength={254}
                        placeholder="you@example.com"
                        className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                      />
                    </div>
                    <div>
                      <label htmlFor="subject" className="block text-sm font-medium text-foreground">{t("support.subjectLabel")}</label>
                      <input
                        id="subject"
                        name="subject"
                        type="text"
                        required
                        minLength={4}
                        maxLength={160}
                        placeholder={t("support.subjectPlaceholder")}
                        className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                      />
                    </div>
                    <div>
                      <label htmlFor="message" className="block text-sm font-medium text-foreground">{t("support.messageLabel")}</label>
                      <textarea
                        id="message"
                        name="message"
                        required
                        minLength={20}
                        maxLength={4000}
                        rows={4}
                        placeholder={t("support.messagePlaceholder")}
                        className="mt-1.5 w-full rounded-lg border border-border bg-muted/60 px-3.5 py-3 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                      />
                    </div>
                    <Button type="submit" className="h-11 w-full text-sm" disabled={sending}>
                      {sending ? tSite("sending") : t("support.sendMessage")}
                    </Button>
                  </form>
                )}
              </div>
            </div>

            {/* FAQ */}
            <div>
              <h2 className="text-lg font-semibold text-foreground">{t("support.faqTitle")}</h2>
              <div className="mt-4 space-y-3">
                {faqs.map((faq, i) => (
                  <div
                    key={i}
                    className="rounded-2xl border border-border bg-card shadow-xs"
                  >
                    <button
                      onClick={() => setOpenFaq(openFaq === i ? null : i)}
                      className="flex w-full items-center justify-between p-5 text-left text-sm font-medium text-foreground"
                    >
                      {faq.q}
                      <span className="ml-2 shrink-0 text-muted-foreground">
                        {openFaq === i ? <ChevronUp className="size-4" /> : <ChevronDown className="size-4" />}
                      </span>
                    </button>
                    {openFaq === i && (
                      <div className="px-5 pb-5 text-sm leading-relaxed text-muted-foreground">
                        {faq.a}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  )
}
