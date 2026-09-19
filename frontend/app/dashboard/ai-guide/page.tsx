"use client"

import { useRequireAuth } from "@/lib/auth"
import { Brain, Sparkles, BookOpen, HelpCircle, ArrowLeft } from "lucide-react"
import Link from "next/link"

export default function AIGuidePage() {
  const { user } = useRequireAuth()
  const firstName = user?.name?.split(" ")[0] || "Student"

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-purple-50/50 via-card to-primary/5 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-purple-500/10"><Brain className="size-6 text-purple-600" /></div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">AI Learning Guide</h1>
            <p className="text-sm text-muted-foreground">Your smart learning assistant</p>
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex flex-col items-center py-8 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-purple-500/10"><Sparkles className="size-8 text-purple-500" /></div>
          <h2 className="mt-4 text-lg font-semibold text-foreground">Coming Soon, {firstName}!</h2>
          <p className="mt-2 text-sm text-muted-foreground max-w-md">
            The AI Learning Guide will help you learn better by answering your questions, suggesting what to study next, and helping you understand difficult topics.
          </p>
          <div className="mt-6 space-y-3 w-full max-w-sm">
            <div className="flex items-center gap-3 rounded-xl bg-muted/50 p-3 text-left">
              <HelpCircle className="size-5 text-purple-500 shrink-0" />
              <div>
                <p className="text-sm font-medium text-foreground">Ask Questions</p>
                <p className="text-xs text-muted-foreground">Get answers to your learning questions</p>
              </div>
            </div>
            <div className="flex items-center gap-3 rounded-xl bg-muted/50 p-3 text-left">
              <BookOpen className="size-5 text-blue-500 shrink-0" />
              <div>
                <p className="text-sm font-medium text-foreground">Study Suggestions</p>
                <p className="text-xs text-muted-foreground">Personalized recommendations for what to study</p>
              </div>
            </div>
            <div className="flex items-center gap-3 rounded-xl bg-muted/50 p-3 text-left">
              <Brain className="size-5 text-green-500 shrink-0" />
              <div>
                <p className="text-sm font-medium text-foreground">Understand Better</p>
                <p className="text-xs text-muted-foreground">Explanations in simple language</p>
              </div>
            </div>
          </div>
          <Link href="/dashboard" className="mt-6 inline-flex items-center gap-2 text-sm text-primary hover:underline">
            <ArrowLeft className="size-4" /> Back to Dashboard
          </Link>
        </div>
      </div>
    </div>
  )
}
