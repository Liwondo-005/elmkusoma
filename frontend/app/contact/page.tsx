"use client"

import { useState } from "react"
import Link from "next/link"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { Button } from "@/components/ui/button"
import { Mail, Clock, MapPin, CheckCircle } from "lucide-react"

const contactSchema = z.object({
  name: z.string().min(1, "Name is required").min(2, "Name must be at least 2 characters"),
  email: z.string().min(1, "Email is required").email("Please enter a valid email address"),
  subject: z.string().min(1, "Subject is required").min(3, "Subject must be at least 3 characters"),
  message: z.string().min(1, "Message is required").min(10, "Message must be at least 10 characters"),
})

type ContactValues = z.infer<typeof contactSchema>

export default function ContactPage() {
  const [submitted, setSubmitted] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
  } = useForm<ContactValues>({
    resolver: zodResolver(contactSchema),
  })

  async function onSubmit(_values: ContactValues) {
    await new Promise((r) => setTimeout(r, 1500))
    setSubmitted(true)
    reset()
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
                Get in touch
              </h1>
              <p className="mt-4 text-pretty text-base leading-relaxed text-muted-foreground">
                Have a question, suggestion or partnership inquiry? We&apos;d love to hear from you.
                Our team typically responds within 24 hours.
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
                  Contact Information
                </h2>
                <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
                  Reach out to us through any of the channels below, or fill out the form and we&apos;ll get back to you.
                </p>

                <div className="mt-8 space-y-6">
                  <div className="rounded-2xl border border-border bg-card p-5">
                    <div className="flex items-center gap-2">
                      <Mail className="size-4 text-muted-foreground" />
                      <h3 className="text-sm font-semibold text-foreground">Email</h3>
                    </div>
                    <a
                      href="mailto:info@elmkusoma.co.tz"
                      className="mt-1.5 block text-sm text-primary hover:underline"
                    >
                      info@elmkusoma.co.tz
                    </a>
                    <p className="mt-1 text-xs text-muted-foreground">
                      For general inquiries, support and partnerships
                    </p>
                  </div>

                  <div className="rounded-2xl border border-border bg-card p-5">
                    <div className="flex items-center gap-2">
                      <Clock className="size-4 text-muted-foreground" />
                      <h3 className="text-sm font-semibold text-foreground">Response Time</h3>
                    </div>
                    <p className="mt-1.5 text-sm text-muted-foreground">
                      We aim to respond within 24 hours on business days.
                    </p>
                  </div>

                  <div className="rounded-2xl border border-border bg-card p-5">
                    <div className="flex items-center gap-2">
                      <MapPin className="size-4 text-muted-foreground" />
                      <h3 className="text-sm font-semibold text-foreground">Location</h3>
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
                    <h2 className="mt-4 text-xl font-bold text-foreground">Message Sent!</h2>
                    <p className="mt-2 max-w-sm text-sm text-muted-foreground">
                      Thank you for reaching out. We&apos;ll get back to you at the email address you provided.
                    </p>
                    <Button onClick={() => setSubmitted(false)} variant="outline" className="mt-6">
                      Send Another Message
                    </Button>
                  </div>
                ) : (
                  <>
                    <h2 className="text-xl font-bold tracking-tight text-foreground">
                      Send us a message
                    </h2>
                    <p className="mt-2 text-sm text-muted-foreground">
                      Fill out the form below and we&apos;ll respond as soon as possible.
                    </p>

                    <form className="mt-8 space-y-5" onSubmit={handleSubmit(onSubmit)}>
                      <div className="grid gap-5 sm:grid-cols-2">
                        <div>
                          <label htmlFor="name" className="block text-sm font-medium text-foreground">
                            Your Name
                          </label>
                          <input
                            id="name"
                            type="text"
                            placeholder="Enter your name"
                            {...register("name")}
                            className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                          />
                          {errors.name && (
                            <p className="mt-1.5 text-xs text-red-600">{errors.name.message}</p>
                          )}
                        </div>

                        <div>
                          <label htmlFor="email" className="block text-sm font-medium text-foreground">
                            Email Address
                          </label>
                          <input
                            id="email"
                            type="email"
                            placeholder="you@example.com"
                            {...register("email")}
                            className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                          />
                          {errors.email && (
                            <p className="mt-1.5 text-xs text-red-600">{errors.email.message}</p>
                          )}
                        </div>
                      </div>

                      <div>
                        <label htmlFor="subject" className="block text-sm font-medium text-foreground">
                          Subject
                        </label>
                        <input
                          id="subject"
                          type="text"
                          placeholder="How can we help?"
                          {...register("subject")}
                          className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
                        />
                        {errors.subject && (
                          <p className="mt-1.5 text-xs text-red-600">{errors.subject.message}</p>
                        )}
                      </div>

                      <div>
                        <label htmlFor="message" className="block text-sm font-medium text-foreground">
                          Message
                        </label>
                        <textarea
                          id="message"
                          rows={5}
                          placeholder="Tell us more about your inquiry..."
                          {...register("message")}
                          className="mt-1.5 w-full rounded-lg border border-border bg-muted/60 px-3.5 py-3 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background resize-none"
                        />
                        {errors.message && (
                          <p className="mt-1.5 text-xs text-red-600">{errors.message.message}</p>
                        )}
                      </div>

                      <Button type="submit" className="h-11 w-full text-sm" disabled={isSubmitting}>
                        {isSubmitting ? "Sending..." : "Send Message"}
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
