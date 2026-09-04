"use client"

import { useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Button } from "@/components/ui/button"
import { CheckCircle, Send } from "lucide-react"

const contactSchema = z.object({
  name: z.string().min(1, "Name is required").min(2, "Name must be at least 2 characters"),
  email: z.string().min(1, "Email is required").email("Please enter a valid email address"),
  subject: z.string().min(1, "Subject is required").min(3, "Subject must be at least 3 characters"),
  message: z.string().min(1, "Message is required").min(10, "Message must be at least 10 characters"),
})

type ContactValues = z.infer<typeof contactSchema>

export function ContactSection() {
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
    <section className="bg-muted/50 py-16 lg:py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-balance text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
            Send us a message
          </h2>
          <p className="mt-3 text-pretty text-muted-foreground">
            Questions, feedback or partnership ideas? We&apos;d love to hear from you.
          </p>
        </div>

        <div className="mx-auto mt-12 max-w-2xl">
          {submitted ? (
            <div className="rounded-2xl border border-border bg-card p-8 text-center shadow-xs">
              <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-teal/10">
                <CheckCircle className="size-7 text-teal" />
              </div>
              <h3 className="mt-4 text-xl font-bold text-foreground">Message Sent!</h3>
              <p className="mt-2 max-w-sm mx-auto text-sm text-muted-foreground">
                Thank you for reaching out. We&apos;ll get back to you within 24 hours.
              </p>
              <Button onClick={() => setSubmitted(false)} variant="outline" className="mt-6">
                Send Another Message
              </Button>
            </div>
          ) : (
            <div className="rounded-2xl border border-border bg-card p-6 shadow-xs sm:p-8">
              <form className="space-y-5" onSubmit={handleSubmit(onSubmit)}>
                <div className="grid gap-5 sm:grid-cols-2">
                  <div>
                    <label htmlFor="home-name" className="block text-sm font-medium text-foreground">
                      Your Name
                    </label>
                    <input
                      id="home-name"
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
                    <label htmlFor="home-email" className="block text-sm font-medium text-foreground">
                      Email Address
                    </label>
                    <input
                      id="home-email"
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
                  <label htmlFor="home-subject" className="block text-sm font-medium text-foreground">
                    Subject
                  </label>
                  <input
                    id="home-subject"
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
                  <label htmlFor="home-message" className="block text-sm font-medium text-foreground">
                    Message
                  </label>
                  <textarea
                    id="home-message"
                    rows={4}
                    placeholder="Tell us more about your inquiry..."
                    {...register("message")}
                    className="mt-1.5 w-full rounded-lg border border-border bg-muted/60 px-3.5 py-3 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background resize-none"
                  />
                  {errors.message && (
                    <p className="mt-1.5 text-xs text-red-600">{errors.message.message}</p>
                  )}
                </div>

                <Button type="submit" className="h-11 w-full text-sm" disabled={isSubmitting}>
                  {isSubmitting ? "Sending..." : <><Send className="mr-2 size-4" /> Send Message</>}
                </Button>
              </form>
            </div>
          )}


        </div>
      </div>
    </section>
  )
}
