"use client"

import { useState, useRef, useEffect } from "react"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { aiApi, isAiUnavailable } from "@/lib/ai-api"
import { Brain, Send, Loader2, Sparkles, ArrowLeft, BookOpen, HelpCircle, Lightbulb, RefreshCw, AlertCircle } from "lucide-react"
import Link from "next/link"
import { cn } from "@/lib/utils"

interface ChatMessage {
  role: "user" | "assistant"
  content: string
  timestamp: Date
}


export default function AIGuidePage() {
  const { user, loading: authLoading } = useRequireAuth()
  const t = useTranslations("primary")
  const SUGGESTIONS = [t("aiGuide.suggestFractions"), t("aiGuide.suggestNext"), t("aiGuide.suggestWater"), t("aiGuide.suggestPractice"), t("aiGuide.suggestWriting"), t("aiGuide.suggestToday")]
  const ts = useTranslations("status")
  const tc = useTranslations("common")
  const ta = useTranslations("ai")
  const firstName = user?.name?.split(" ")[0] || "Student"
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      role: "assistant",
      content: t("aiGuide.greeting", { name: firstName }),
      timestamp: new Date(),
    },
  ])
  const [input, setInput] = useState("")
  const [loading, setLoading] = useState(false)
  const [aiUnavailable, setAiUnavailable] = useState(false)
  const [aiError, setAiError] = useState<string | null>(null)
  const chatEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  useEffect(() => {
    let active = true
    aiApi
      .getAiHealth()
      .then((health) => {
        if (active && !health.configured) setAiUnavailable(true)
      })
      .catch(() => {})
    return () => {
      active = false
    }
  }, [])

  const sendMessage = async (text?: string) => {
    const content = text || input.trim()
    if (!content || loading || aiUnavailable) return

    const userMsg: ChatMessage = { role: "user", content, timestamp: new Date() }
    setMessages((prev) => [...prev, userMsg])
    setInput("")
    setLoading(true)
    setAiError(null)

    try {
      const response = await aiApi.askAi(content)
      const assistantMsg: ChatMessage = {
        role: "assistant",
        content: response.answer,
        timestamp: new Date(),
      }
      setMessages((prev) => [...prev, assistantMsg])
    } catch (err) {
      if (isAiUnavailable(err)) {
        setAiUnavailable(true)
      } else {
        setAiError(tc("error.generic"))
      }
    } finally {
      setLoading(false)
    }
  }

  if (authLoading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <Loader2 className="size-8 animate-spin text-primary" />
      </div>
    )
  }

  return (
    <div className="mx-auto flex h-[calc(100dvh-8rem)] max-w-3xl flex-col">
      <div className="mb-4 rounded-2xl border border-border bg-gradient-to-br from-purple-50/50 via-card to-primary/5 p-4 shadow-xs">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-purple-500/10">
              <Brain className="size-5 text-purple-600" />
            </div>
            <div>
              <h1 className="text-lg font-bold tracking-tight text-foreground">{t("aiGuide.title")}</h1>
              <p className="text-xs text-muted-foreground">{t("aiGuide.subtitle")}</p>
            </div>
          </div>
          <Link href="/dashboard" className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground">
            <ArrowLeft className="size-3" /> {t("aiGuide.dashboard")}
          </Link>
        </div>
      </div>

      {aiUnavailable && (
        <div
          role="status"
          className="mb-4 flex items-start gap-3 rounded-2xl border border-amber-300 bg-amber-50 p-4 text-amber-900 dark:border-amber-500/40 dark:bg-amber-500/10 dark:text-amber-200"
        >
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <div>
            <p className="text-sm font-semibold">{ta("notConfiguredTitle")}</p>
            <p className="text-xs">{ta("notConfigured")}</p>
          </div>
        </div>
      )}

      {messages.length === 1 && (
        <div className="mb-4 grid grid-cols-2 gap-2 sm:grid-cols-3">
          {SUGGESTIONS.map((s) => (
            <button
              key={s}
              onClick={() => sendMessage(s)}
              disabled={aiUnavailable}
              className="rounded-xl border border-border bg-card p-3 text-left text-xs text-muted-foreground transition-colors hover:border-primary/50 hover:bg-primary/5 hover:text-foreground disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Lightbulb className="mb-1 size-3.5 text-amber-500" />
              {s}
            </button>
          ))}
        </div>
      )}

      <div className="flex-1 overflow-y-auto rounded-2xl border border-border bg-card p-4 shadow-xs">
        <div className="space-y-4">
          {messages.map((msg, i) => (
            <div key={i} className={cn("flex", msg.role === "user" ? "justify-end" : "justify-start")}>
              <div
                className={cn(
                  "max-w-[80%] rounded-2xl px-4 py-2.5 text-sm",
                  msg.role === "user"
                    ? "bg-primary text-primary-foreground"
                    : "bg-muted text-foreground"
                )}
              >
                {msg.role === "assistant" && (
                  <div className="mb-1 flex items-center gap-1.5">
                    <Sparkles className="size-3 text-purple-500" />
                    <span className="text-[10px] font-semibold text-purple-600">{t("aiGuide.aiLabel")}</span>
                  </div>
                )}
                <p className="whitespace-pre-wrap">{msg.content}</p>
              </div>
            </div>
          ))}
          {loading && (
            <div className="flex justify-start">
              <div className="flex items-center gap-2 rounded-2xl bg-muted px-4 py-2.5 text-sm text-muted-foreground">
                <Loader2 className="size-4 animate-spin" />
                {t("aiGuide.thinking")}
              </div>
            </div>
          )}
          <div ref={chatEndRef} />
        </div>
      </div>

      {aiError && (
        <div
          role="alert"
          className="mt-3 flex items-center gap-2 rounded-2xl border border-red-300 bg-red-50 p-3 text-sm text-red-700 dark:border-red-500/40 dark:bg-red-500/10 dark:text-red-300"
        >
          <AlertCircle className="size-4 shrink-0" />
          <span>{aiError}</span>
        </div>
      )}

      <div className="mt-3 flex items-center gap-2">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && sendMessage()}
          placeholder={aiUnavailable ? ta("notConfiguredTitle") : t("aiGuide.inputPlaceholder")}
          disabled={loading || aiUnavailable}
          className="flex-1 rounded-xl border border-border bg-background px-4 py-3 text-sm outline-none focus:border-primary disabled:cursor-not-allowed disabled:opacity-60"
        />
        <button
          onClick={() => sendMessage()}
          disabled={!input.trim() || loading || aiUnavailable}
          className="flex size-11 items-center justify-center rounded-xl bg-primary text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
        >
          <Send className="size-5" />
        </button>
      </div>
    </div>
  )
}
