"use client"

import { useState } from "react"
import { useTranslations } from "next-intl"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { Button } from "@/components/ui/button"
import { Mail, Phone, MessageSquare, CheckCircle, ChevronDown, ChevronUp } from "lucide-react"

export default function SupportPage() {
  const t = useTranslations("public")
  const [submitted, setSubmitted] = useState(false)
  const [openFaq, setOpenFaq] = useState<number | null>(null)

  const faqs = [
    { q: t("support.faq1q"), a: t("support.faq1a") },
    { q: t("support.faq2q"), a: t("support.faq2a") },
    { q: t("support.faq3q"), a: t("support.faq3a") },
    { q: t("support.faq4q"), a: t("support.faq4a") },
    { q: t("support.faq5q"), a: t("support.faq5a") },
    { q: t("support.faq6q"), a: t("support.faq6a") },
  ]

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSubmitted(true)
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
                <a
                  href="mailto:info@elmkusoma.co.tz"
                  className="flex items-center gap-4 rounded-xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
                >
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <Mail className="size-5" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-foreground">{t("support.emailUs")}</p>
                    <p className="text-sm text-muted-foreground">info@elmkusoma.co.tz</p>
                  </div>
                </a>

                <a
                  href="tel:+255700000000"
                  className="flex items-center gap-4 rounded-xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
                >
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-teal/10 text-teal">
                    <Phone className="size-5" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-foreground">{t("support.callUs")}</p>
                    <p className="text-sm text-muted-foreground">+255 700 000 000</p>
                  </div>
                </a>

                <a
                  href="https://wa.me/255700000000"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-4 rounded-xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
                >
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-600">
                    <MessageSquare className="size-5" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-foreground">{t("support.whatsapp")}</p>
                    <p className="text-sm text-muted-foreground">{t("support.whatsappDesc")}</p>
                  </div>
                </a>
              </div>

              {/* Contact Form */}
              <div className="mt-8">
                <h2 className="text-lg font-semibold text-foreground">{t("support.formTitle")}</h2>
                {submitted ? (
                  <div className="mt-4 flex items-center gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-5 text-sm text-emerald-700 dark:border-emerald-800 dark:bg-emerald-950 dark:text-emerald-400">
                    <CheckCircle className="size-5 shrink-0" />
                    {t("support.sentMessage")}
                  </div>
                ) : (
                  <form className="mt-4 space-y-4" onSubmit={handleSubmit}>
                    <div>
                      <label htmlFor="name" className="block text-sm font-medium text-foreground">{t("support.nameLabel")}</label>
                      <input
                        id="name"
                        type="text"
                        required
                        placeholder={t("support.namePlaceholder")}
                        className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                      />
                    </div>
                    <div>
                      <label htmlFor="email" className="block text-sm font-medium text-foreground">{t("support.emailLabel")}</label>
                      <input
                        id="email"
                        type="email"
                        required
                        placeholder="you@example.com"
                        className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                      />
                    </div>
                    <div>
                      <label htmlFor="subject" className="block text-sm font-medium text-foreground">{t("support.subjectLabel")}</label>
                      <input
                        id="subject"
                        type="text"
                        required
                        placeholder={t("support.subjectPlaceholder")}
                        className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                      />
                    </div>
                    <div>
                      <label htmlFor="message" className="block text-sm font-medium text-foreground">{t("support.messageLabel")}</label>
                      <textarea
                        id="message"
                        required
                        rows={4}
                        placeholder={t("support.messagePlaceholder")}
                        className="mt-1.5 w-full rounded-lg border border-border bg-muted/60 px-3.5 py-3 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                      />
                    </div>
                    <Button type="submit" className="h-11 w-full text-sm">
                      {t("support.sendMessage")}
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
