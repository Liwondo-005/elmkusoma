"use client"

import { useState, useRef, useEffect } from "react"
import { useRequireAuth } from "@/lib/auth"
import { primaryApi } from "@/lib/api"
import { Brain, Send, Loader2, Sparkles, ArrowLeft, BookOpen, HelpCircle, Lightbulb, RefreshCw } from "lucide-react"
import Link from "next/link"
import { cn } from "@/lib/utils"

interface ChatMessage {
  role: "user" | "assistant"
  content: string
  timestamp: Date
}

const SUGGESTIONS = [
  "Help me understand fractions",
  "What should I study next?",
  "Explain the water cycle",
  "Give me a practice problem",
  "How do I improve my writing?",
  "What did I learn today?",
]

function getLocalResponse(message: string): string {
  const lower = message.toLowerCase()
  if (lower.includes("fraction")) return "Fractions represent parts of a whole! For example, 1/2 means one part out of two equal parts. Think of a pizza cut into 2 slices — if you eat 1, you ate 1/2 of the pizza. Would you like me to give you a practice problem?"
  if (lower.includes("water cycle")) return "The water cycle is how water moves around our planet! It goes: Evaporation (sun heats water) → Condensation (forms clouds) → Precipitation (rain/snow) → Collection (rivers, lakes). It never stops!"
  if (lower.includes("what should i study") || (lower.includes("next") && lower.includes("study"))) return "Based on your learning journey, I'd suggest reviewing your recent lessons and trying a practice quiz. Check your Progress page to see where you can improve!"
  if (lower.includes("practice problem") || lower.includes("quiz me")) return "Here's a fun one: If you have 3/4 of a pizza and give away 1/4, how much do you have left? Think about it and type your answer!"
  if (lower.includes("writing") || lower.includes("essay")) return "Great writing tips: 1) Start with a clear main idea. 2) Use describing words. 3) Read your work out loud. 4) Check your spelling. Practice makes perfect!"
  if (lower.includes("today") || lower.includes("learn")) return "Check your Learning Evidence page to see a summary of everything you've done today! You can also look at your Learning Passport for your progress stamps."
  if (lower.includes("math")) return "Math is all about patterns! Start with what you know, take it step by step, and don't be afraid to make mistakes — that's how we learn!"
  if (lower.includes("science")) return "Science is about asking questions and finding answers through experiments! What topic are you curious about?"
  if (lower.includes("hello") || lower.includes("hi") || lower.includes("hey")) return `Hello! I'm your AI Learning Guide. I can help you understand subjects, give practice problems, and suggest what to study. What would you like to learn about?`
  return "That's a great question! I'm here to help you learn. Try asking me about a specific subject like Math, Science, or English, or ask me to explain a topic or give you a practice problem."
}

export default function AIGuidePage() {
  const { user } = useRequireAuth()
  const firstName = user?.name?.split(" ")[0] || "Student"
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      role: "assistant",
      content: `Hello ${firstName}! I'm your AI Learning Guide. Ask me anything about your schoolwork, and I'll help you understand it better. What would you like to learn about today?`,
      timestamp: new Date(),
    },
  ])
  const [input, setInput] = useState("")
  const [loading, setLoading] = useState(false)
  const chatEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  const sendMessage = async (text?: string) => {
    const content = text || input.trim()
    if (!content || loading) return

    const userMsg: ChatMessage = { role: "user", content, timestamp: new Date() }
    setMessages((prev) => [...prev, userMsg])
    setInput("")
    setLoading(true)

    try {
      const response = await primaryApi.askAI(content)
      const assistantMsg: ChatMessage = {
        role: "assistant",
        content: response.answer || getLocalResponse(content),
        timestamp: new Date(),
      }
      setMessages((prev) => [...prev, assistantMsg])
    } catch {
      const assistantMsg: ChatMessage = {
        role: "assistant",
        content: getLocalResponse(content),
        timestamp: new Date(),
      }
      setMessages((prev) => [...prev, assistantMsg])
    } finally {
      setLoading(false)
    }
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
              <h1 className="text-lg font-bold tracking-tight text-foreground">AI Learning Guide</h1>
              <p className="text-xs text-muted-foreground">Ask me anything about your schoolwork</p>
            </div>
          </div>
          <Link href="/dashboard" className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground">
            <ArrowLeft className="size-3" /> Dashboard
          </Link>
        </div>
      </div>

      {messages.length === 1 && (
        <div className="mb-4 grid grid-cols-2 gap-2 sm:grid-cols-3">
          {SUGGESTIONS.map((s) => (
            <button
              key={s}
              onClick={() => sendMessage(s)}
              className="rounded-xl border border-border bg-card p-3 text-left text-xs text-muted-foreground transition-colors hover:border-primary/50 hover:bg-primary/5 hover:text-foreground"
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
                    <span className="text-[10px] font-semibold text-purple-600">AI Guide</span>
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
                Thinking...
              </div>
            </div>
          )}
          <div ref={chatEndRef} />
        </div>
      </div>

      <div className="mt-3 flex items-center gap-2">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && sendMessage()}
          placeholder="Ask a question about your schoolwork..."
          className="flex-1 rounded-xl border border-border bg-background px-4 py-3 text-sm outline-none focus:border-primary"
          disabled={loading}
        />
        <button
          onClick={() => sendMessage()}
          disabled={!input.trim() || loading}
          className="flex size-11 items-center justify-center rounded-xl bg-primary text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
        >
          <Send className="size-5" />
        </button>
      </div>
    </div>
  )
}
