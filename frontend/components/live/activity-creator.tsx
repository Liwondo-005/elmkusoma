"use client"

import { useState } from "react"
import {
  Plus,
  Trash2,
  Send,
  BarChart3,
  Brain,
  CheckCircle,
  Zap,
  Palette,
  HelpCircle,
  Trophy,
  ChevronDown,
} from "lucide-react"
import { primaryApi } from "@/lib/api"
import { cn } from "@/lib/utils"

interface ActivityCreatorProps {
  liveClassId: string
  onActivityCreated?: () => void
}

const ACTIVITY_TYPES = [
  { value: "POLL", label: "Poll", icon: BarChart3, color: "bg-blue-500" },
  { value: "MCQ", label: "MCQ", icon: CheckCircle, color: "bg-indigo-500" },
  { value: "QUIZ", label: "Quick Quiz", icon: Brain, color: "bg-purple-500" },
  { value: "TRUE_FALSE", label: "True / False", icon: CheckCircle, color: "bg-green-500" },
  { value: "MATCHING", label: "Matching", icon: Zap, color: "bg-orange-500" },
  { value: "DRAWING", label: "Drawing", icon: Palette, color: "bg-pink-500" },
  { value: "PREDICTION", label: "Prediction", icon: HelpCircle, color: "bg-cyan-500" },
  { value: "QUESTION", label: "Open Question", icon: HelpCircle, color: "bg-teal-500" },
  { value: "CHALLENGE", label: "Challenge", icon: Trophy, color: "bg-red-500" },
]

export function ActivityCreator({ liveClassId, onActivityCreated }: ActivityCreatorProps) {
  const [activityType, setActivityType] = useState("POLL")
  const [title, setTitle] = useState("")
  const [question, setQuestion] = useState("")
  const [options, setOptions] = useState(["", ""])
  const [correctAnswer, setCorrectAnswer] = useState("")
  const [timerSeconds, setTimerSeconds] = useState(0)
  const [orderIndex, setOrderIndex] = useState(0)
  const [sending, setSending] = useState(false)
  const [showTypeDropdown, setShowTypeDropdown] = useState(false)
  const [error, setError] = useState("")

  const needsOptions = ["POLL", "MCQ", "QUIZ", "TRUE_FALSE", "MATCHING", "PREDICTION"].includes(activityType)
  const needsTimer = ["MCQ", "QUIZ", "CHALLENGE"].includes(activityType)

  const addOption = () => {
    if (options.length < 8) setOptions([...options, ""])
  }

  const removeOption = (index: number) => {
    if (options.length > 2) setOptions(options.filter((_, i) => i !== index))
  }

  const updateOption = (index: number, value: string) => {
    const updated = [...options]
    updated[index] = value
    setOptions(updated)
  }

  const handleSend = async () => {
    if (!title.trim() || !question.trim()) {
      setError("Title and question are required")
      return
    }
    if (needsOptions && options.some((o) => !o.trim())) {
      setError("All options must be filled")
      return
    }

    setSending(true)
    setError("")
    try {
      await primaryApi.createLiveClassActivity(liveClassId, {
        activityType,
        title: title.trim(),
        question: question.trim(),
        options: needsOptions ? options.map((o) => o.trim()) : undefined,
        correctAnswer: correctAnswer.trim() || undefined,
        orderIndex,
        timerSeconds: needsTimer && timerSeconds > 0 ? timerSeconds : undefined,
      })
      setTitle("")
      setQuestion("")
      setOptions(["", ""])
      setCorrectAnswer("")
      setTimerSeconds(0)
      setOrderIndex(0)
      onActivityCreated?.()
    } catch {
      setError("Failed to send activity")
    } finally {
      setSending(false)
    }
  }

  const selectedType = ACTIVITY_TYPES.find((t) => t.value === activityType)

  return (
    <div className="rounded-2xl border border-border bg-card p-5 space-y-4">
      <h4 className="text-sm font-semibold text-foreground">Create Activity</h4>

      <div className="relative">
        <button
          onClick={() => setShowTypeDropdown(!showTypeDropdown)}
          className="flex w-full items-center gap-3 rounded-xl border border-border bg-background px-4 py-3 text-left text-sm"
        >
          {selectedType && (
            <span className={cn("flex size-6 items-center justify-center rounded-lg text-white", selectedType.color)}>
              <selectedType.icon className="size-3.5" />
            </span>
          )}
          <span className="flex-1 font-medium text-foreground">{selectedType?.label}</span>
          <ChevronDown className="size-4 text-muted-foreground" />
        </button>
        {showTypeDropdown && (
          <div className="absolute top-full left-0 right-0 z-10 mt-1 rounded-xl border border-border bg-card shadow-lg">
            {ACTIVITY_TYPES.map((type) => (
              <button
                key={type.value}
                onClick={() => {
                  setActivityType(type.value)
                  setShowTypeDropdown(false)
                }}
                className={cn(
                  "flex w-full items-center gap-3 px-4 py-2.5 text-left text-sm transition-colors hover:bg-muted",
                  activityType === type.value && "bg-primary/5"
                )}
              >
                <span className={cn("flex size-5 items-center justify-center rounded-md text-white", type.color)}>
                  <type.icon className="size-3" />
                </span>
                <span className="font-medium text-foreground">{type.label}</span>
              </button>
            ))}
          </div>
        )}
      </div>

      <input
        type="text"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        placeholder="Activity title"
        className="w-full rounded-xl border border-border bg-background px-4 py-3 text-sm outline-none focus:border-primary"
      />

      <textarea
        value={question}
        onChange={(e) => setQuestion(e.target.value)}
        placeholder="Question or prompt..."
        className="w-full rounded-xl border border-border bg-background px-4 py-3 text-sm outline-none focus:border-primary min-h-[80px] resize-none"
      />

      {needsOptions && (
        <div className="space-y-2">
          <p className="text-xs font-semibold text-muted-foreground uppercase">Options</p>
          {options.map((opt, i) => (
            <div key={i} className="flex items-center gap-2">
              <span className="flex size-6 shrink-0 items-center justify-center rounded-md bg-muted text-[10px] font-bold text-muted-foreground">
                {String.fromCharCode(65 + i)}
              </span>
              <input
                type="text"
                value={opt}
                onChange={(e) => updateOption(i, e.target.value)}
                placeholder={`Option ${String.fromCharCode(65 + i)}`}
                className="flex-1 rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-primary"
              />
              {options.length > 2 && (
                <button
                  onClick={() => removeOption(i)}
                  className="flex size-6 items-center justify-center rounded-md text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                >
                  <Trash2 className="size-3.5" />
                </button>
              )}
            </div>
          ))}
          {options.length < 8 && (
            <button
              onClick={addOption}
              className="flex items-center gap-1.5 text-xs font-medium text-primary hover:underline"
            >
              <Plus className="size-3.5" />
              Add option
            </button>
          )}
        </div>
      )}

      {needsOptions && (
        <div>
          <p className="mb-1.5 text-xs font-semibold text-muted-foreground uppercase">Correct Answer</p>
          <select
            value={correctAnswer}
            onChange={(e) => setCorrectAnswer(e.target.value)}
            className="w-full rounded-xl border border-border bg-background px-4 py-3 text-sm outline-none focus:border-primary"
          >
            <option value="">Select correct answer</option>
            {options.filter((o) => o.trim()).map((opt, i) => (
              <option key={i} value={opt}>{String.fromCharCode(65 + i)}. {opt}</option>
            ))}
          </select>
        </div>
      )}

      {needsTimer && (
        <div>
          <p className="mb-1.5 text-xs font-semibold text-muted-foreground uppercase">Timer (seconds)</p>
          <input
            type="number"
            value={timerSeconds}
            onChange={(e) => setTimerSeconds(parseInt(e.target.value) || 0)}
            min={0}
            max={300}
            placeholder="0 = no timer"
            className="w-full rounded-xl border border-border bg-background px-4 py-3 text-sm outline-none focus:border-primary"
          />
        </div>
      )}

      <div>
        <p className="mb-1.5 text-xs font-semibold text-muted-foreground uppercase">Order Index</p>
        <input
          type="number"
          value={orderIndex}
          onChange={(e) => setOrderIndex(parseInt(e.target.value) || 0)}
          min={0}
          className="w-full rounded-xl border border-border bg-background px-4 py-3 text-sm outline-none focus:border-primary"
        />
      </div>

      {error && (
        <p className="text-xs text-destructive">{error}</p>
      )}

      <button
        onClick={handleSend}
        disabled={sending || !title.trim() || !question.trim()}
        className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-3 text-sm font-bold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
      >
        <Send className="size-4" />
        {sending ? "Sending..." : "Send to Class"}
      </button>
    </div>
  )
}
