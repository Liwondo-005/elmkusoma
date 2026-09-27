"use client"

import { useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { useTranslations } from "next-intl"
import { Button } from "@/components/ui/button"
import { CheckCircle, Send } from "lucide-react"

type ContactValues = {
  name: string
  email: string
  subject: string
  message: string
}

export function ContactSection() {
  const t = useTranslations("home")
  const tc = useTranslations("common")
  const [submitted, setSubmitted] = useState(false)

  const contactSchema = z.object({
    name: z.string().min(1, t("contact.errNameRequired")).min(2, t("contact.errNameShort")),
    email: z.string().min(1, t("contact.errEmailRequired")).email(t("contact.errEmailInvalid")),
    subject: z.string().min(1, t("contact.errSubjectRequired")).min(3, t("contact.errSubjectShort")),
    message: z.string().min(1, t("contact.errMessageRequired")).min(10, t("contact.errMessageShort")),
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
          {submitted ? (
            <div className="rounded-2xl border border-border bg-card p-8 text-center shadow-xs">
              <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-teal/10">
                <CheckCircle className="size-7 text-teal" />
              </div>
              <h3 className="mt-4 text-xl font-bold text-foreground">{t("contact.successTitle")}</h3>
              <p className="mt-2 max-w-sm mx-auto text-sm text-muted-foreground">
                {t.rich("contact.successDesc", {
                  email: (chunks) => (
                    <a href="mailto:info@elmkusoma.co.tz" className="font-medium text-primary hover:underline">
                      {chunks}
                    </a>
                  ),
                })}
              </p>
              <Button onClick={() => setSubmitted(false)} variant="outline" className="mt-6">
                {t("contact.backToForm")}
              </Button>
            </div>
          ) : (
            <div className="rounded-2xl border border-border bg-card p-6 shadow-xs sm:p-8">
              <form className="space-y-5" onSubmit={handleSubmit(onSubmit)}>
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
