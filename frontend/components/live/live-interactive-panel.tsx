"use client"

import { useState, useEffect, useRef, useCallback } from "react"
import {
  X,
  Sparkles,
  CheckCircle,
  XCircle,
  Send,
  Clock,
  Trophy,
  Brain,
  Palette,
  HelpCircle,
  Zap,
  BarChart3,
  Eye,
  EyeOff,
} from "lucide-react"
import { primaryApi, type LiveClassActivity, type LiveClassActivityStats } from "@/lib/api"
import { useAuth } from "@/lib/auth"
import { cn } from "@/lib/utils"

interface LiveInteractivePanelProps {
  liveClassId: string
  isTeacher: boolean
  isOpen: boolean
  onClose: () => void
}

const ACTIVITY_ICONS: Record<string, typeof Sparkles> = {
  POLL: BarChart3,
  MCQ: CheckCircle,
  QUIZ: Brain,
  TRUE_FALSE: CheckCircle,
  MATCHING: Zap,
  DRAWING: Palette,
  PREDICTION: HelpCircle,
  QUESTION: HelpCircle,
  CHALLENGE: Trophy,
}

const ACTIVITY_COLORS: Record<string, string> = {
  POLL: "bg-blue-500",
  MCQ: "bg-indigo-500",
  QUIZ: "bg-purple-500",
  TRUE_FALSE: "bg-green-500",
  MATCHING: "bg-orange-500",
  DRAWING: "bg-pink-500",
  PREDICTION: "bg-cyan-500",
  QUESTION: "bg-teal-500",
  CHALLENGE: "bg-red-500",
}

function PollActivity({
  activity,
  isTeacher,
  onSubmitted,
}: {
  activity: LiveClassActivity
  isTeacher: boolean
  onSubmitted?: () => void
}) {
  const [selected, setSelected] = useState<string | null>(null)
  const [submitted, setSubmitted] = useState(false)
  const [stats, setStats] = useState<LiveClassActivityStats | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const { token } = useAuth()

  const handleSubmit = async () => {
    if (!selected) return
    setSubmitting(true)
    try {
      await primaryApi.submitActivityAnswer(activity.id, { answer: selected })
      setSubmitted(true)
      if (isTeacher) {
        const s = await primaryApi.getActivityStats(activity.id)
        setStats(s)
      }
      onSubmitted?.()
    } catch {
    } finally {
      setSubmitting(false)
    }
  }

  useEffect(() => {
    if (isTeacher && activity.id) {
      primaryApi.getActivityStats(activity.id).then(setStats).catch(() => {})
    }
  }, [isTeacher, activity.id])

  const totalVotes = stats
    ? Object.values(stats.optionCounts).reduce((a, b) => a + b, 0)
    : 0

  return (
    <div className="space-y-4">
      <p className="text-base font-semibold text-foreground">{activity.question}</p>
      <div className="grid grid-cols-1 gap-3">
        {activity.options.map((opt, i) => {
          const letter = String.fromCharCode(65 + i)
          const isSelected = selected === opt
          const pct = stats && totalVotes > 0
            ? Math.round(((stats.optionCounts[opt] || 0) / totalVotes) * 100)
            : 0
          const isCorrect = activity.correctAnswer === opt

          return (
            <button
              key={i}
              onClick={() => !submitted && setSelected(opt)}
              disabled={submitted}
              className={cn(
                "relative flex items-center gap-3 rounded-xl border-2 p-4 text-left transition-all",
                submitted
                  ? isCorrect
                    ? "border-green-500 bg-green-50"
                    : isSelected
                      ? "border-red-500 bg-red-50"
                      : "border-border bg-muted/30"
                  : isSelected
                    ? "border-primary bg-primary/5"
                    : "border-border bg-card hover:border-primary/50"
              )}
            >
              <span
                className={cn(
                  "flex size-8 shrink-0 items-center justify-center rounded-lg text-sm font-bold",
                  isSelected ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground"
                )}
              >
                {letter}
              </span>
              <span className="flex-1 text-sm font-medium text-foreground">{opt}</span>
              {submitted && stats && (
                <span className="text-sm font-bold text-muted-foreground">{pct}%</span>
              )}
              {submitted && isCorrect && (
                <CheckCircle className="size-5 text-green-500" />
              )}
              {submitted && isSelected && !isCorrect && (
                <XCircle className="size-5 text-red-500" />
              )}
              {submitted && (
                <div className="absolute bottom-0 left-0 right-0 h-1 rounded-b-xl bg-muted overflow-hidden">
                  <div
                    className={cn(
                      "h-full transition-all",
                      isCorrect ? "bg-green-500" : "bg-primary/40"
                    )}
                    style={{ width: `${pct}%` }}
                  />
                </div>
              )}
            </button>
          )
        })}
      </div>
      {!submitted && !isTeacher && (
        <button
          onClick={handleSubmit}
          disabled={!selected || submitting}
          className="w-full rounded-xl bg-primary px-4 py-3 text-sm font-bold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
        >
          {submitting ? "Submitting..." : "Submit Answer"}
        </button>
      )}
      {isTeacher && stats && (
        <div className="rounded-xl bg-muted/50 p-4">
          <p className="text-xs font-semibold text-muted-foreground">
            {stats.totalResponses} response{stats.totalResponses !== 1 ? "s" : ""}
          </p>
          {stats.responses.slice(0, 5).map((r) => (
            <div key={r.userId} className="mt-1 flex items-center gap-2 text-xs">
              <span className="font-medium text-foreground">{r.userName}</span>
              <span className="text-muted-foreground">chose {r.answer}</span>
            </div>
          ))}
          {stats.responses.length > 5 && (
            <p className="mt-1 text-xs text-muted-foreground">
              +{stats.responses.length - 5} more
            </p>
          )}
        </div>
      )}
    </div>
  )
}

function QuizActivity({
  activity,
  isTeacher,
  onSubmitted,
}: {
  activity: LiveClassActivity
  isTeacher: boolean
  onSubmitted?: () => void
}) {
  const [selected, setSelected] = useState<string | null>(null)
  const [submitted, setSubmitted] = useState(false)
  const [isCorrect, setIsCorrect] = useState<boolean | null>(null)
  const [timer, setTimer] = useState(activity.timerSeconds || 0)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (timer <= 0 || submitted) return
    const t = setInterval(() => setTimer((p) => p - 1), 1000)
    return () => clearInterval(t)
  }, [timer, submitted])

  const handleSubmit = async () => {
    if (!selected) return
    setSubmitting(true)
    try {
      const res = await primaryApi.submitActivityAnswer(activity.id, { answer: selected })
      setSubmitted(true)
      setIsCorrect(res.isCorrect)
      onSubmitted?.()
    } catch {
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="space-y-4">
      {activity.timerSeconds && !submitted && (
        <div className={cn(
          "flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-bold",
          timer <= 10 ? "bg-red-100 text-red-700" : "bg-muted text-muted-foreground"
        )}>
          <Clock className="size-4" />
          {timer}s
        </div>
      )}
      <p className="text-base font-semibold text-foreground">{activity.question}</p>
      <div className="grid grid-cols-1 gap-3">
        {activity.options.map((opt, i) => {
          const letter = String.fromCharCode(65 + i)
          const isSelected = selected === opt
          const correct = activity.correctAnswer === opt

          return (
            <button
              key={i}
              onClick={() => !submitted && setSelected(opt)}
              disabled={submitted}
              className={cn(
                "flex items-center gap-3 rounded-xl border-2 p-4 text-left transition-all",
                submitted
                  ? correct
                    ? "border-green-500 bg-green-50"
                    : isSelected
                      ? "border-red-500 bg-red-50"
                      : "border-border bg-muted/30"
                  : isSelected
                    ? "border-primary bg-primary/5"
                    : "border-border bg-card hover:border-primary/50"
              )}
            >
              <span
                className={cn(
                  "flex size-8 shrink-0 items-center justify-center rounded-lg text-sm font-bold",
                  isSelected ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground"
                )}
              >
                {letter}
              </span>
              <span className="flex-1 text-sm font-medium text-foreground">{opt}</span>
              {submitted && correct && <CheckCircle className="size-5 text-green-500" />}
              {submitted && isSelected && !correct && <XCircle className="size-5 text-red-500" />}
            </button>
          )
        })}
      </div>
      {!submitted && !isTeacher && (
        <button
          onClick={handleSubmit}
          disabled={!selected || submitting || timer <= 0}
          className="w-full rounded-xl bg-primary px-4 py-3 text-sm font-bold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
        >
          {submitting ? "Submitting..." : "Submit Answer"}
        </button>
      )}
      {submitted && (
        <div className={cn(
          "rounded-xl p-4 text-sm font-medium",
          isCorrect ? "bg-green-50 text-green-700" : "bg-red-50 text-red-700"
        )}>
          {isCorrect ? "Correct! Well done!" : "Incorrect. Keep trying!"}
          {activity.correctAnswer && !isCorrect && (
            <p className="mt-1 text-xs opacity-70">Correct answer: {activity.correctAnswer}</p>
          )}
        </div>
      )}
    </div>
  )
}

function TrueFalseActivity({
  activity,
  isTeacher,
  onSubmitted,
}: {
  activity: LiveClassActivity
  isTeacher: boolean
  onSubmitted?: () => void
}) {
  const [selected, setSelected] = useState<string | null>(null)
  const [submitted, setSubmitted] = useState(false)
  const [isCorrect, setIsCorrect] = useState<boolean | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async () => {
    if (!selected) return
    setSubmitting(true)
    try {
      const res = await primaryApi.submitActivityAnswer(activity.id, { answer: selected })
      setSubmitted(true)
      setIsCorrect(res.isCorrect)
      onSubmitted?.()
    } catch {
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="space-y-4">
      <p className="text-base font-semibold text-foreground">{activity.question}</p>
      <div className="grid grid-cols-2 gap-3">
        {["True", "False"].map((opt) => {
          const isSelected = selected === opt
          const correct = activity.correctAnswer === opt
          return (
            <button
              key={opt}
              onClick={() => !submitted && setSelected(opt)}
              disabled={submitted}
              className={cn(
                "rounded-xl border-2 p-6 text-center text-sm font-bold transition-all",
                submitted
                  ? correct
                    ? "border-green-500 bg-green-50 text-green-700"
                    : isSelected
                      ? "border-red-500 bg-red-50 text-red-700"
                      : "border-border bg-muted/30"
                  : isSelected
                    ? "border-primary bg-primary/5 text-primary"
                    : "border-border bg-card hover:border-primary/50 text-foreground"
              )}
            >
              {opt}
            </button>
          )
        })}
      </div>
      {!submitted && !isTeacher && (
        <button
          onClick={handleSubmit}
          disabled={!selected || submitting}
          className="w-full rounded-xl bg-primary px-4 py-3 text-sm font-bold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
        >
          {submitting ? "Submitting..." : "Submit Answer"}
        </button>
      )}
      {submitted && (
        <div className={cn(
          "rounded-xl p-4 text-sm font-medium",
          isCorrect ? "bg-green-50 text-green-700" : "bg-red-50 text-red-700"
        )}>
          {isCorrect ? "Correct!" : "Incorrect."}
        </div>
      )}
    </div>
  )
}

function MatchingActivity({
  activity,
  isTeacher,
  onSubmitted,
}: {
  activity: LiveClassActivity
  isTeacher: boolean
  onSubmitted?: () => void
}) {
  const leftItems = activity.options.filter((_, i) => i % 2 === 0)
  const rightItems = activity.options.filter((_, i) => i % 2 === 1)
  const [matches, setMatches] = useState<Record<string, string>>({})
  const [selectedLeft, setSelectedLeft] = useState<string | null>(null)
  const [submitted, setSubmitted] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const handleLeftClick = (item: string) => {
    if (submitted) return
    setSelectedLeft(item)
  }

  const handleRightClick = (item: string) => {
    if (submitted || !selectedLeft) return
    setMatches((prev) => ({ ...prev, [selectedLeft]: item }))
    setSelectedLeft(null)
  }

  const handleSubmit = async () => {
    setSubmitting(true)
    try {
      const answer = JSON.stringify(matches)
      await primaryApi.submitActivityAnswer(activity.id, { answer })
      setSubmitted(true)
      onSubmitted?.()
    } catch {
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="space-y-4">
      <p className="text-base font-semibold text-foreground">{activity.question}</p>
      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <p className="text-xs font-semibold text-muted-foreground uppercase">Items</p>
          {leftItems.map((item) => (
            <button
              key={item}
              onClick={() => handleLeftClick(item)}
              className={cn(
                "w-full rounded-xl border-2 p-3 text-left text-sm transition-all",
                selectedLeft === item
                  ? "border-primary bg-primary/5"
                  : matches[item]
                    ? "border-green-500 bg-green-50"
                    : "border-border bg-card"
              )}
            >
              {item}
              {matches[item] && (
                <span className="ml-2 text-xs text-muted-foreground">= {matches[item]}</span>
              )}
            </button>
          ))}
        </div>
        <div className="space-y-2">
          <p className="text-xs font-semibold text-muted-foreground uppercase">Matches</p>
          {rightItems.map((item) => (
            <button
              key={item}
              onClick={() => handleRightClick(item)}
              disabled={submitted}
              className={cn(
                "w-full rounded-xl border-2 p-3 text-left text-sm transition-all",
                "border-border bg-card",
                !submitted && "hover:border-primary/50"
              )}
            >
              {item}
            </button>
          ))}
        </div>
      </div>
      {!submitted && !isTeacher && (
        <button
          onClick={handleSubmit}
          disabled={Object.keys(matches).length < leftItems.length || submitting}
          className="w-full rounded-xl bg-primary px-4 py-3 text-sm font-bold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
        >
          {submitting ? "Submitting..." : "Submit Matches"}
        </button>
      )}
    </div>
  )
}

function DrawingActivity({
  activity,
  isTeacher,
  onSubmitted,
}: {
  activity: LiveClassActivity
  isTeacher: boolean
  onSubmitted?: () => void
}) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [drawing, setDrawing] = useState(false)
  const [color, setColor] = useState("#000000")
  const [tool, setTool] = useState<"pencil" | "eraser">("pencil")
  const [submitted, setSubmitted] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const startDraw = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext("2d")
    if (!ctx) return
    const rect = canvas.getBoundingClientRect()
    ctx.beginPath()
    ctx.moveTo(e.clientX - rect.left, e.clientY - rect.top)
    setDrawing(true)
  }

  const draw = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!drawing) return
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext("2d")
    if (!ctx) return
    const rect = canvas.getBoundingClientRect()
    ctx.lineTo(e.clientX - rect.left, e.clientY - rect.top)
    ctx.strokeStyle = tool === "eraser" ? "#ffffff" : color
    ctx.lineWidth = tool === "eraser" ? 20 : 3
    ctx.lineCap = "round"
    ctx.stroke()
  }

  const stopDraw = () => setDrawing(false)

  const clearCanvas = () => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext("2d")
    if (!ctx) return
    ctx.clearRect(0, 0, canvas.width, canvas.height)
  }

  const handleSubmit = async () => {
    const canvas = canvasRef.current
    if (!canvas) return
    setSubmitting(true)
    try {
      const data = canvas.toDataURL("image/png")
      await primaryApi.submitActivityAnswer(activity.id, { answer: "drawing", drawingData: data })
      setSubmitted(true)
      onSubmitted?.()
    } catch {
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="space-y-4">
      <p className="text-base font-semibold text-foreground">{activity.question}</p>
      <div className="flex items-center gap-2">
        <button
          onClick={() => setTool("pencil")}
          className={cn(
            "rounded-lg px-3 py-1.5 text-xs font-bold",
            tool === "pencil" ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground"
          )}
        >
          Pencil
        </button>
        <button
          onClick={() => setTool("eraser")}
          className={cn(
            "rounded-lg px-3 py-1.5 text-xs font-bold",
            tool === "eraser" ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground"
          )}
        >
          Eraser
        </button>
        <input
          type="color"
          value={color}
          onChange={(e) => setColor(e.target.value)}
          className="size-8 cursor-pointer rounded-lg border-0"
        />
        <button
          onClick={clearCanvas}
          className="rounded-lg bg-muted px-3 py-1.5 text-xs font-bold text-muted-foreground"
        >
          Clear
        </button>
      </div>
      <canvas
        ref={canvasRef}
        width={400}
        height={300}
        className="w-full rounded-xl border border-border bg-white cursor-crosshair"
        onMouseDown={startDraw}
        onMouseMove={draw}
        onMouseUp={stopDraw}
        onMouseLeave={stopDraw}
      />
      {!submitted && !isTeacher && (
        <button
          onClick={handleSubmit}
          disabled={submitting}
          className="w-full rounded-xl bg-primary px-4 py-3 text-sm font-bold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
        >
          {submitting ? "Submitting..." : "Submit Drawing"}
        </button>
      )}
    </div>
  )
}

function PredictionActivity({
  activity,
  isTeacher,
  onSubmitted,
}: {
  activity: LiveClassActivity
  isTeacher: boolean
  onSubmitted?: () => void
}) {
  const [selected, setSelected] = useState<string | null>(null)
  const [submitted, setSubmitted] = useState(false)
  const [showAnswer, setShowAnswer] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async () => {
    if (!selected) return
    setSubmitting(true)
    try {
      await primaryApi.submitActivityAnswer(activity.id, { answer: selected })
      setSubmitted(true)
      onSubmitted?.()
    } catch {
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="space-y-4">
      <p className="text-base font-semibold text-foreground">{activity.question}</p>
      <div className="grid grid-cols-1 gap-3">
        {activity.options.map((opt, i) => {
          const isSelected = selected === opt
          return (
            <button
              key={i}
              onClick={() => !submitted && setSelected(opt)}
              disabled={submitted}
              className={cn(
                "rounded-xl border-2 p-4 text-left text-sm transition-all",
                submitted && showAnswer && activity.correctAnswer === opt
                  ? "border-green-500 bg-green-50"
                  : isSelected
                    ? "border-primary bg-primary/5"
                    : "border-border bg-card"
              )}
            >
              {opt}
            </button>
          )
        })}
      </div>
      {!submitted && !isTeacher && (
        <button
          onClick={handleSubmit}
          disabled={!selected || submitting}
          className="w-full rounded-xl bg-primary px-4 py-3 text-sm font-bold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
        >
          {submitting ? "Submitting..." : "Make Your Prediction"}
        </button>
      )}
      {isTeacher && submitted && (
        <button
          onClick={() => setShowAnswer(!showAnswer)}
          className="flex items-center gap-2 rounded-xl bg-muted px-4 py-2 text-sm font-bold text-muted-foreground"
        >
          {showAnswer ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          {showAnswer ? "Hide Answer" : "Reveal Answer"}
        </button>
      )}
      {showAnswer && activity.correctAnswer && (
        <div className="rounded-xl bg-green-50 p-4 text-sm font-medium text-green-700">
          Answer: {activity.correctAnswer}
        </div>
      )}
    </div>
  )
}

function QuestionActivity({
  activity,
  isTeacher,
  onSubmitted,
}: {
  activity: LiveClassActivity
  isTeacher: boolean
  onSubmitted?: () => void
}) {
  const [answer, setAnswer] = useState("")
  const [submitted, setSubmitted] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [stats, setStats] = useState<LiveClassActivityStats | null>(null)

  const handleSubmit = async () => {
    if (!answer.trim()) return
    setSubmitting(true)
    try {
      await primaryApi.submitActivityAnswer(activity.id, { answer: answer.trim() })
      setSubmitted(true)
      onSubmitted?.()
      if (isTeacher) {
        const s = await primaryApi.getActivityStats(activity.id)
        setStats(s)
      }
    } catch {
    } finally {
      setSubmitting(false)
    }
  }

  useEffect(() => {
    if (isTeacher && activity.id) {
      primaryApi.getActivityStats(activity.id).then(setStats).catch(() => {})
    }
  }, [isTeacher, activity.id])

  return (
    <div className="space-y-4">
      <p className="text-base font-semibold text-foreground">{activity.question}</p>
      {!submitted && !isTeacher && (
        <>
          <textarea
            value={answer}
            onChange={(e) => setAnswer(e.target.value)}
            placeholder="Type your answer..."
            className="w-full rounded-xl border border-border bg-background px-4 py-3 text-sm outline-none focus:border-primary min-h-[100px] resize-none"
          />
          <button
            onClick={handleSubmit}
            disabled={!answer.trim() || submitting}
            className="w-full rounded-xl bg-primary px-4 py-3 text-sm font-bold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
          >
            {submitting ? "Submitting..." : "Submit Answer"}
          </button>
        </>
      )}
      {submitted && (
        <div className="rounded-xl bg-green-50 p-4 text-sm font-medium text-green-700">
          Your answer has been submitted!
        </div>
      )}
      {isTeacher && stats && (
        <div className="rounded-xl bg-muted/50 p-4">
          <p className="text-xs font-semibold text-muted-foreground">
            {stats.totalResponses} response{stats.totalResponses !== 1 ? "s" : ""}
          </p>
          <div className="mt-2 space-y-2">
            {stats.responses.map((r) => (
              <div key={r.userId} className="rounded-lg bg-card p-3">
                <p className="text-xs font-semibold text-foreground">{r.userName}</p>
                <p className="mt-0.5 text-sm text-muted-foreground">{r.answer}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function ChallengeActivity({
  activity,
  isTeacher,
  onSubmitted,
}: {
  activity: LiveClassActivity
  isTeacher: boolean
  onSubmitted?: () => void
}) {
  const [answer, setAnswer] = useState("")
  const [submitted, setSubmitted] = useState(false)
  const [isCorrect, setIsCorrect] = useState<boolean | null>(null)
  const [score, setScore] = useState<number | null>(null)
  const [timer, setTimer] = useState(activity.timerSeconds || 30)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (timer <= 0 || submitted) return
    const t = setInterval(() => setTimer((p) => p - 1), 1000)
    return () => clearInterval(t)
  }, [timer, submitted])

  const handleSubmit = async () => {
    if (!answer.trim()) return
    setSubmitting(true)
    try {
      const res = await primaryApi.submitActivityAnswer(activity.id, { answer: answer.trim() })
      setSubmitted(true)
      setIsCorrect(res.isCorrect)
      setScore(res.score)
      onSubmitted?.()
    } catch {
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-base font-semibold text-foreground">{activity.question}</p>
        <span className={cn(
          "flex items-center gap-1.5 rounded-lg px-3 py-1 text-sm font-bold",
          timer <= 10 ? "bg-red-100 text-red-700" : "bg-muted text-muted-foreground"
        )}>
          <Clock className="size-3.5" />
          {timer}s
        </span>
      </div>
      <input
        type="text"
        value={answer}
        onChange={(e) => setAnswer(e.target.value)}
        onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
        placeholder="Quick! Type your answer..."
        disabled={submitted}
        className="w-full rounded-xl border border-border bg-background px-4 py-3 text-sm outline-none focus:border-primary"
      />
      {!submitted && !isTeacher && (
        <button
          onClick={handleSubmit}
          disabled={!answer.trim() || timer <= 0 || submitting}
          className="w-full rounded-xl bg-red-500 px-4 py-3 text-sm font-bold text-white transition-colors hover:bg-red-600 disabled:opacity-50"
        >
          {submitting ? "Submitting..." : "Challenge Submit!"}
        </button>
      )}
      {submitted && (
        <div className={cn(
          "rounded-xl p-4 text-sm font-medium",
          isCorrect ? "bg-green-50 text-green-700" : "bg-red-50 text-red-700"
        )}>
          {isCorrect ? "Correct!" : "Not quite!"}
          {score !== null && (
            <span className="ml-2">Score: {score}</span>
          )}
          {!isCorrect && activity.correctAnswer && (
            <p className="mt-1 text-xs opacity-70">Answer: {activity.correctAnswer}</p>
          )}
        </div>
      )}
    </div>
  )
}

export function LiveInteractivePanel({
  liveClassId,
  isTeacher,
  isOpen,
  onClose,
}: LiveInteractivePanelProps) {
  const [activities, setActivities] = useState<LiveClassActivity[]>([])
  const [currentIndex, setCurrentIndex] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [showResults, setShowResults] = useState(false)

  const fetchActivities = useCallback(async () => {
    try {
      setLoading(true)
      setError("")
      const data = await primaryApi.getLiveClassActivities(liveClassId)
      setActivities(data)
    } catch {
      setError("Could not load activities")
    } finally {
      setLoading(false)
    }
  }, [liveClassId])

  useEffect(() => {
    if (isOpen) fetchActivities()
  }, [isOpen, fetchActivities])

  const currentActivity = activities[currentIndex]

  const renderActivity = () => {
    if (!currentActivity) return null
    const props = { activity: currentActivity, isTeacher }

    switch (currentActivity.activityType) {
      case "POLL":
        return <PollActivity {...props} onSubmitted={fetchActivities} />
      case "MCQ":
        return <QuizActivity {...props} onSubmitted={fetchActivities} />
      case "QUIZ":
        return <QuizActivity {...props} onSubmitted={fetchActivities} />
      case "TRUE_FALSE":
        return <TrueFalseActivity {...props} onSubmitted={fetchActivities} />
      case "MATCHING":
        return <MatchingActivity {...props} onSubmitted={fetchActivities} />
      case "DRAWING":
        return <DrawingActivity {...props} onSubmitted={fetchActivities} />
      case "PREDICTION":
        return <PredictionActivity {...props} onSubmitted={fetchActivities} />
      case "QUESTION":
        return <QuestionActivity {...props} onSubmitted={fetchActivities} />
      case "CHALLENGE":
        return <ChallengeActivity {...props} onSubmitted={fetchActivities} />
      default:
        return <p className="text-sm text-muted-foreground">Unknown activity type</p>
    }
  }

  if (!isOpen) return null

  return (
    <div className="flex h-full flex-col border-l border-border bg-card">
      <div className="flex items-center justify-between border-b border-border px-4 py-3">
        <div className="flex items-center gap-2">
          <Sparkles className="size-4 text-primary" />
          <h3 className="text-sm font-semibold text-foreground">Activities</h3>
        </div>
        <button
          onClick={onClose}
          className="flex size-7 items-center justify-center rounded-lg text-muted-foreground hover:bg-muted"
        >
          <X className="size-4" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="size-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
          </div>
        ) : error ? (
          <div className="rounded-xl bg-destructive/10 p-4 text-center text-sm text-destructive">
            {error}
          </div>
        ) : activities.length === 0 ? (
          <div className="flex flex-col items-center py-12 text-center">
            <Sparkles className="size-10 text-muted-foreground/30" />
            <p className="mt-3 text-sm text-muted-foreground">
              No activities yet. Your teacher will send them soon!
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {currentActivity && (
              <div className="flex items-center gap-2">
                {(() => {
                  const Icon = ACTIVITY_ICONS[currentActivity.activityType] || Sparkles
                  return (
                    <span className={cn("flex size-6 items-center justify-center rounded-lg text-white", ACTIVITY_COLORS[currentActivity.activityType])}>
                      <Icon className="size-3.5" />
                    </span>
                  )
                })()}
                <span className="text-xs font-semibold text-muted-foreground uppercase">
                  {currentActivity.activityType.replace("_", " ")}
                </span>
                <span className="ml-auto text-xs text-muted-foreground">
                  {currentIndex + 1} / {activities.length}
                </span>
              </div>
            )}

            {currentActivity && (
              <div className="rounded-xl border border-border bg-background p-4">
                <h4 className="mb-3 text-xs font-semibold text-muted-foreground uppercase">
                  {currentActivity.title}
                </h4>
                {renderActivity()}
              </div>
            )}

            {activities.length > 1 && (
              <div className="flex items-center justify-between">
                <button
                  onClick={() => setCurrentIndex((p) => Math.max(0, p - 1))}
                  disabled={currentIndex === 0}
                  className="rounded-lg bg-muted px-3 py-1.5 text-xs font-bold text-muted-foreground disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  onClick={() => setCurrentIndex((p) => Math.min(activities.length - 1, p + 1))}
                  disabled={currentIndex === activities.length - 1}
                  className="rounded-lg bg-muted px-3 py-1.5 text-xs font-bold text-muted-foreground disabled:opacity-50"
                >
                  Next
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
