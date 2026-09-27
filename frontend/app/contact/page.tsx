"use client"

import { useState } from "react"
import Link from "next/link"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { useTranslations } from "next-intl"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { Button } from "@/components/ui/button"
import { Mail, Clock, MapPin, CheckCircle } from "lucide-react"

type ContactValues = {
  name: string
  email: string
  subject: string
  message: string
}

export default function ContactPage() {
  const t = useTranslations("public")
  const [submitted, setSubmitted] = useState(false)

  const contactSchema = z.object({
    name: z.string().min(1, t("contact.nameRequired")).min(2, t("contact.nameTooShort")),
    email: z.string().min(1, t("contact.emailRequired")).email(t("contact.emailInvalid")),
    subject: z.string().min(1, t("contact.subjectRequired")).min(3, t("contact.subjectTooShort")),
    message: z.string().min(1, t("contact.messageRequired")).min(10, t("contact.messageTooShort")),
  })

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ContactValues>({
    resolver: zodResolver(contactSchema),
  })

  function onSubmit(values: ContactValues) {
    // Honest handoff: no backend contact endpoint exists, so open the user's
    // mail app pre-filled to the published support address instead of faking a send.
    const subject = encodeURIComponent(values.subject)
    const body = encodeURIComponent(
      `${values.message}\n\n— ${values.name} (${values.email})`,
    )
    window.location.href = `mailto:info@elmkusoma.co.tz?subject=${subject}&body=${body}`
    setSubmitted(true)
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
                  <div className="rounded-2xl border border-border bg-card p-5">
                    <div className="flex items-center gap-2">
                      <Mail className="size-4 text-muted-foreground" />
                      <h3 className="text-sm font-semibold text-foreground">{t("contact.emailTitle")}</h3>
                    </div>
                    <a
                      href="mailto:info@elmkusoma.co.tz"
                      className="mt-1.5 block text-sm text-primary hover:underline"
                    >
                      info@elmkusoma.co.tz
                    </a>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {t("contact.emailDesc")}
                    </p>
                  </div>

                  <div className="rounded-2xl border border-border bg-card p-5">
                    <div className="flex items-center gap-2">
                      <Clock className="size-4 text-muted-foreground" />
                      <h3 className="text-sm font-semibold text-foreground">{t("contact.responseTitle")}</h3>
                    </div>
                    <p className="mt-1.5 text-sm text-muted-foreground">
                      {t("contact.responseDesc")}
                    </p>
                  </div>

                  <div className="rounded-2xl border border-border bg-card p-5">
                    <div className="flex items-center gap-2">
                      <MapPin className="size-4 text-muted-foreground" />
                      <h3 className="text-sm font-semibold text-foreground">{t("contact.locationTitle")}</h3>
                    </div>
                    <p className="mt-1.5 text-sm text-muted-foreground">
                      Dar es Salaam, Tanzania
                    </p>
                  </div>
                </div>
              </div>

              {/* Contact form */}
              <div className="rounded-2xl border border-border bg-card p-6 shadow-xs sm:p-8">
                {submitted ? (
                  <div className="flex flex-col items-center justify-center py-12 text-center">
                    <div className="flex size-14 items-center justify-center rounded-full bg-teal/10">
                      <CheckCircle className="size-7 text-teal" />
                    </div>
                    <h2 className="mt-4 text-xl font-bold text-foreground">{t("contact.mailtoTitle")}</h2>
                    <p className="mt-2 max-w-sm text-sm text-muted-foreground">
                      {t("contact.mailtoDescPrefix")}{" "}
                      <a href="mailto:info@elmkusoma.co.tz" className="font-medium text-primary hover:underline">
                        info@elmkusoma.co.tz
                      </a>
                      {t("contact.mailtoDescSuffix")}
                    </p>
                    <Button onClick={() => setSubmitted(false)} variant="outline" className="mt-6">
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
