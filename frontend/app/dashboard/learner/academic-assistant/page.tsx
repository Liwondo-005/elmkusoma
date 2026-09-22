"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { StudentCourseEnrollment } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { Bot, Send, BookOpen, HelpCircle, Lightbulb, AlertCircle } from "lucide-react"

interface ChatMessage {
  id: string
  role: "user" | "assistant"
  content: string
}

export default function AcademicAssistantPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [enrollments, setEnrollments] = useState<StudentCourseEnrollment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState("")

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const studentId = user?.id || ""
      const res = await collegeApi.getStudentEnrollments(studentId)
      setEnrollments((res.data as StudentCourseEnrollment[] | undefined) || [])
    } catch {
      setError(tc("error.load"))
    } finally {
      setLoading(false)
    }
  }

  function sendMessage(text?: string) {
    const content = text || input.trim()
    if (!content) return

    const userMsg: ChatMessage = { id: Date.now().toString(), role: "user", content }
    setMessages((prev) => [...prev, userMsg])
    setInput("")

    setTimeout(() => {
      const assistantMsg: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: "assistant",
        content: `Thank you for your question about "${content.slice(0, 50)}...". The Academic Assistant is designed to help with concept explanations, practice questions, and study guidance. In a full implementation, this would connect to an AI service for detailed responses. Your enrolled courses cover ${enrollments.length} subject area${enrollments.length !== 1 ? "s" : ""} — feel free to ask about any topic from your coursework.`,
      }
      setMessages((prev) => [...prev, assistantMsg])
    }, 800)
  }

  if (authLoading || loading) return <div role="main"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"

  const suggestedPrompts = [
    t("academicAssistant.prompt1"),
    t("academicAssistant.prompt2"),
    t("academicAssistant.prompt3"),
    t("academicAssistant.prompt4"),
  ]

  return (
    <div role="main" className="mx-auto max-w-4xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle={t("academicAssistant.subtitle")} />

      {error && (
        <div className="rounded-2xl border border-border bg-card p-4 text-sm text-red-600 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadData() }} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      )}

      {messages.length === 0 ? (
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-3">
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs text-center">
              <div className="mx-auto mb-2 flex size-10 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30">
                <BookOpen className="size-5 text-blue-600 dark:text-blue-400" />
              </div>
              <h3 className="text-sm font-semibold text-foreground">{t("academicAssistant.conceptExplanations")}</h3>
              <p className="mt-1 text-xs text-muted-foreground">{t("academicAssistant.conceptExplanationsDesc")}</p>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs text-center">
              <div className="mx-auto mb-2 flex size-10 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/30">
                <HelpCircle className="size-5 text-amber-600 dark:text-amber-400" />
              </div>
              <h3 className="text-sm font-semibold text-foreground">{t("academicAssistant.practiceQuestions")}</h3>
              <p className="mt-1 text-xs text-muted-foreground">{t("academicAssistant.practiceQuestionsDesc")}</p>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs text-center">
              <div className="mx-auto mb-2 flex size-10 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30">
                <Lightbulb className="size-5 text-emerald-600 dark:text-emerald-400" />
              </div>
              <h3 className="text-sm font-semibold text-foreground">{t("academicAssistant.studyGuidance")}</h3>
              <p className="mt-1 text-xs text-muted-foreground">{t("academicAssistant.studyGuidanceDesc")}</p>
            </div>
          </div>

          <EmptyState
            icon={<Bot className="size-8" />}
            title={t("academicAssistant.askAnything")}
            description={t("academicAssistant.askAnythingDesc")}
            action={
              <div className="flex flex-wrap justify-center gap-2">
                {suggestedPrompts.map((p) => (
                  <button
                    key={p}
                    onClick={() => sendMessage(p)}
                    aria-label={p}
                    className="rounded-xl border border-border bg-card px-4 py-2 text-xs text-muted-foreground hover:bg-muted transition"
                  >
                    {p.length > 40 ? p.slice(0, 40) + "..." : p}
                  </button>
                ))}
              </div>
            }
          />
        </div>
      ) : (
        <div className="space-y-3">
          {messages.map((msg) => (
            <div key={msg.id} className={`flex ${msg.role === "user" ? "justify-end" : "justify-start"}`}>
              <div
                className={`max-w-[80%] rounded-2xl px-4 py-3 text-sm ${
                  msg.role === "user"
                    ? "bg-primary text-primary-foreground"
                    : "border border-border bg-card text-foreground"
                }`}
              >
                {msg.content}
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="flex items-center gap-2 rounded-2xl border border-border bg-card p-3 shadow-xs">
        <input
          type="text"
          placeholder={t("academicAssistant.inputPlaceholder")}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && sendMessage()}
          aria-label={t("academicAssistant.inputPlaceholder")}
          className="flex-1 bg-transparent px-2 text-sm outline-none placeholder:text-muted-foreground"
        />
        <button
          onClick={() => sendMessage()}
          disabled={!input.trim()}
          aria-label={t("academicAssistant.send")}
          className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition"
        >
          <Send className="size-4" />
        </button>
      </div>
    </div>
  )
}
