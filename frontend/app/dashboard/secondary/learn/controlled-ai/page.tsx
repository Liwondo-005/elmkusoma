"use client"

import { ArrowLeft, Brain, Sparkles, BookOpen, CheckCircle } from "lucide-react"
import Link from "next/link"
import { useState } from "react"
import { useTranslations } from "next-intl"

interface ChatMessage {
  id: string
  role: "user" | "ai"
  content: string
  timestamp: string
}

const AI_TOPICS = [
  { id: "math", label: "Mathematics", desc: "Get help with equations, formulas, and problem-solving strategies", icon: "📐" },
  { id: "science", label: "Science", desc: "Understand concepts in Physics, Chemistry, and Biology", icon: "🔬" },
  { id: "english", label: "English", desc: "Grammar, writing, and language comprehension help", icon: "📝" },
  { id: "history", label: "History & Civics", desc: "Historical events, governance, and citizenship", icon: "🏛️" },
]

const SAMPLE_RESPONSES: Record<string, string> = {
  math: "I can help you with mathematics! Ask me about algebra, geometry, trigonometry, calculus, or any math topic. I'll guide you through step by step — remember, understanding the process matters more than just the answer.",
  science: "Let's explore science together! Whether it's physics formulas, chemical reactions, or biological processes, I'll help you understand the concepts. What topic would you like to learn about?",
  english: "I'm here to help with English! From grammar rules to essay writing, vocabulary to comprehension — ask me anything. Great communication starts with strong language skills.",
  history: "History helps us understand the present! Ask me about world history, African history, Tanzanian history, or civics. I can help you connect events across time periods.",
}

export default function ControlledAIPage() {
  const t = useTranslations("secondary")
  const tc = useTranslations("common")
  const [selectedTopic, setSelectedTopic] = useState<string | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState("")

  function sendMessage() {
    if (!input.trim() || !selectedTopic) return
    const userMsg: ChatMessage = { id: Date.now().toString(), role: "user", content: input.trim(), timestamp: new Date().toISOString() }
    setMessages(prev => [...prev, userMsg])
    setInput("")

    setTimeout(() => {
      const aiMsg: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: "ai",
        content: `Great question! Based on your query about "${input.trim()}", here's what I can help with:\n\nAs an AI learning assistant, I'm designed to guide you toward understanding rather than just giving answers. Let me suggest you:\n\n1. Review the relevant topic in your textbook\n2. Try solving a similar problem first\n3. Come back and I'll help clarify any confusion\n\nRemember: The goal is for YOU to learn, not for me to do the work! 🎓`,
        timestamp: new Date().toISOString(),
      }
      setMessages(prev => [...prev, aiMsg])
    }, 800)
  }

  function selectTopic(id: string) {
    setSelectedTopic(id)
    const welcomeMsg: ChatMessage = {
      id: Date.now().toString(),
      role: "ai",
      content: SAMPLE_RESPONSES[id] || "How can I help you today?",
      timestamp: new Date().toISOString(),
    }
    setMessages([welcomeMsg])
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary/learn" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToLearn")}><ArrowLeft className="size-5 text-gray-600" /></Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{t("controlledAITutor")}</h1>
          <p className="text-sm text-gray-500">{t("aiGuidedLearningWithGuardrails")}</p>
        </div>
      </div>

      <div className="rounded-2xl bg-gradient-to-r from-violet-600 to-purple-500 p-5 text-white">
        <div className="flex items-center gap-3"><Brain className="size-8" /><div><h2 className="text-lg font-bold">{t("smartLearningAssistant")}</h2><p className="text-sm text-white/70">{t("askQuestionsGetGuidedExplanations")}</p></div></div>
      </div>

      <div className="rounded-2xl border border-violet-200 bg-violet-50 p-4">
        <h3 className="flex items-center gap-2 text-sm font-semibold text-violet-800"><Sparkles className="size-4" /> {t("howItWorks")}</h3>
        <ul className="mt-2 space-y-1 text-xs text-violet-700">
          <li>• {t("aiGuidesThroughProblems")}</li>
          <li>• {t("encouragesCriticalThinking")}</li>
          <li>• {t("tracksUnderstandingAndAdapts")}</li>
          <li>• {t("alwaysAlignedWithCurriculum")}</li>
        </ul>
      </div>

      {!selectedTopic ? (
        <div className="space-y-3">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-gray-500">{t("chooseASubject")}</h2>
          {AI_TOPICS.map(topic => (
            <button key={topic.id} onClick={() => selectTopic(topic.id)} className="flex w-full items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 text-left transition-all hover:border-violet-200 hover:shadow-sm" aria-label={`${topic.label} - ${topic.desc}`}>
              <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-violet-50 text-2xl">{topic.icon}</div>
              <div className="flex-1 min-w-0">
                <p className="font-semibold text-gray-900">{topic.label}</p>
                <p className="text-xs text-gray-400">{topic.desc}</p>
              </div>
            </button>
          ))}
        </div>
      ) : (
        <div className="space-y-4">
          <button onClick={() => { setSelectedTopic(null); setMessages([]) }} className="text-sm font-semibold text-violet-600 hover:text-violet-700" aria-label={t("changeSubject")}>← {t("changeSubject")}</button>
          <div className="space-y-3 max-h-[400px] overflow-y-auto rounded-2xl border border-gray-100 bg-white p-4" role="log" aria-label={t("chatMessages")}>
            {messages.map(msg => (
              <div key={msg.id} className={`flex ${msg.role === "user" ? "justify-end" : "justify-start"}`}>
                <div className={`max-w-[80%] rounded-2xl px-4 py-3 text-sm ${msg.role === "user" ? "bg-indigo-600 text-white" : "bg-gray-100 text-gray-800"}`}>
                  {msg.content.split("\n").map((line, i) => <p key={i} className={i > 0 ? "mt-1" : ""}>{line}</p>)}
                </div>
              </div>
            ))}
          </div>
          <div className="flex gap-2">
            <input type="text" value={input} onChange={e => setInput(e.target.value)} onKeyDown={e => e.key === "Enter" && sendMessage()} placeholder={t("askAQuestion")} className="flex-1 rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm focus:border-violet-300 focus:outline-none" aria-label={t("askAQuestion")} />
            <button onClick={sendMessage} disabled={!input.trim()} className="rounded-xl bg-violet-600 px-5 py-3 text-sm font-medium text-white hover:bg-violet-700 disabled:opacity-50" aria-label={tc("send")}>{tc("send")}</button>
          </div>
        </div>
      )}
    </div>
  )
}
