"use client"

import { useEffect, useState, useCallback, useRef } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { assessmentApi, type Assessment, type Question, type Attempt, type AssessmentResult } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { Button } from "@/components/ui/button"
import { ArrowLeft, Clock, CheckCircle, XCircle, Trophy, ArrowRight, Compass, Star, Play } from "lucide-react"

type QuizState = "loading" | "ready" | "in_progress" | "submitting" | "results"

const optionLetters = ["A", "B", "C", "D"]
const optionColors = [
  "from-blue-400 to-blue-600",
  "from-emerald-400 to-emerald-600",
  "from-amber-400 to-amber-600",
  "from-rose-400 to-rose-600",
]

function PrimaryQuizView({
  state,
  assessment,
  questions,
  currentQ,
  answers,
  timeLeft,
  attempt,
  onSelectOption,
  onPrev,
  onNext,
  onSubmit,
  onStart,
  formatTime,
}: {
  state: QuizState
  assessment: Assessment | null
  questions: Question[]
  currentQ: number
  answers: Record<string, { selectedOptionId?: string; textAnswer?: string }>
  timeLeft: number
  attempt: Attempt | null
  onSelectOption: (qId: string, optId: string) => void
  onPrev: () => void
  onNext: () => void
  onSubmit: () => void
  onStart: () => void
  formatTime: (s: number) => string
}) {
  if (state === "ready" && assessment) {
    return (
      <div className="mx-auto max-w-lg space-y-6">
        <Link
          href="/dashboard/assessments"
          className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
        >
          <ArrowLeft className="size-4" /> Back to Quizzes
        </Link>

        <div className="rounded-2xl border border-border bg-gradient-to-br from-primary/10 via-primary/5 to-transparent p-8 text-center shadow-xs">
          <div className="mx-auto flex size-16 items-center justify-center rounded-2xl bg-primary/15">
            <Compass className="size-8 text-primary" />
          </div>
          <h1 className="mt-4 text-3xl font-bold tracking-tight text-foreground">{assessment.title}</h1>
          {assessment.description && (
            <p className="mt-3 text-lg text-muted-foreground">{assessment.description}</p>
          )}

          <div className="mt-6 grid grid-cols-2 gap-3">
            <div className="rounded-xl bg-muted/50 p-4">
              <p className="text-sm text-muted-foreground">Questions</p>
              <p className="mt-1 text-2xl font-bold text-foreground">{questions.length}</p>
            </div>
            <div className="rounded-xl bg-muted/50 p-4">
              <p className="text-sm text-muted-foreground">Total Marks</p>
              <p className="mt-1 text-2xl font-bold text-foreground">{assessment.totalMarks}</p>
            </div>
            {assessment.timeLimitMinutes && (
              <div className="col-span-2 rounded-xl bg-muted/50 p-4">
                <p className="text-sm text-muted-foreground">Time Limit</p>
                <p className="mt-1 text-2xl font-bold text-foreground">{assessment.timeLimitMinutes} minutes</p>
              </div>
            )}
          </div>

          <button
            onClick={onStart}
            className="mt-8 inline-flex items-center gap-2 rounded-2xl bg-primary px-8 py-4 text-lg font-bold text-primary-foreground shadow-lg transition-all hover:bg-primary/90 hover:shadow-xl active:scale-95"
          >
            <Play className="size-6" /> Start Quiz
          </button>
        </div>
      </div>
    )
  }

  if (state === "results" && attempt?.result) {
    const result = attempt.result
    const percentage = assessment ? Math.round((result.totalScore / assessment.totalMarks) * 100) : 0
    const stars = percentage >= 90 ? 3 : percentage >= 60 ? 2 : percentage >= 30 ? 1 : 0

    return (
      <div className="mx-auto max-w-lg space-y-6">
        <Link
          href="/dashboard/assessments"
          className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
        >
          <ArrowLeft className="size-4" /> Back to Quizzes
        </Link>

        <div className="rounded-2xl border-2 border-border bg-gradient-to-br from-amber-50 via-card to-orange-50 p-8 text-center shadow-xs">
          <div className={`mx-auto flex size-20 items-center justify-center rounded-full ${result.isPassed ? "bg-amber-400/15" : "bg-muted"}`}>
            {result.isPassed ? (
              <Trophy className="size-10 text-amber-600" />
            ) : (
              <XCircle className="size-10 text-muted-foreground" />
            )}
          </div>

          <h1 className="mt-4 text-3xl font-bold text-foreground">
            {result.isPassed ? "Great effort!" : "Keep trying!"}
          </h1>
          <p className="mt-2 text-lg text-muted-foreground">
            {result.isPassed
              ? "You did an amazing job! Keep it up!"
              : "Practice makes perfect. You will get better!"}
          </p>

          <div className="mt-6 flex justify-center gap-2">
            {[1, 2, 3].map((i) => (
              <Star
                key={i}
                className={`size-10 ${i <= stars ? "fill-amber-400 text-amber-400" : "fill-muted text-muted"}`}
              />
            ))}
          </div>

          <div className="mt-6 space-y-3">
            <div className="flex items-center justify-between rounded-xl bg-white/60 px-5 py-3">
              <span className="text-base text-muted-foreground">Your Score</span>
              <span className="text-xl font-bold text-foreground">{result.totalScore} / {assessment?.totalMarks}</span>
            </div>
            <div className="flex items-center justify-between rounded-xl bg-white/60 px-5 py-3">
              <span className="text-base text-muted-foreground">Percentage</span>
              <span className="text-xl font-bold text-foreground">{percentage}%</span>
            </div>
            <div className="flex items-center justify-between rounded-xl bg-white/60 px-5 py-3">
              <span className="text-base text-muted-foreground">Status</span>
              <span className={`text-xl font-bold ${result.isPassed ? "text-emerald-600" : "text-rose-600"}`}>
                {result.isPassed ? "PASSED" : "NOT PASSED"}
              </span>
            </div>
          </div>

          {result.feedback && (
            <div className="mt-4 rounded-xl bg-white/60 p-4 text-left">
              <p className="text-sm font-semibold text-foreground">Teacher says:</p>
              <p className="mt-1 text-base text-muted-foreground">{result.feedback}</p>
            </div>
          )}

          <Link
            href="/dashboard/assessments"
            className="mt-6 inline-flex items-center gap-2 rounded-2xl bg-primary px-8 py-3.5 text-base font-bold text-primary-foreground transition-all hover:bg-primary/90"
          >
            Back to Quizzes
          </Link>
        </div>
      </div>
    )
  }

  if (state === "in_progress" && questions.length > 0) {
    const question = questions[currentQ]
    if (!question) return null

    return (
      <div className="mx-auto max-w-2xl space-y-6">
        <div className="flex items-center justify-between">
          <Link
            href="/dashboard/assessments"
            className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft className="size-4" /> Exit
          </Link>
          {assessment?.timeLimitMinutes && (
            <div className={`flex items-center gap-2 rounded-2xl px-5 py-2.5 text-lg font-mono font-bold ${timeLeft < 60 ? "bg-rose-100 text-rose-700" : "bg-primary/10 text-primary"}`}>
              <Clock className="size-5" />
              {formatTime(timeLeft)}
            </div>
          )}
        </div>

        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-base font-semibold text-muted-foreground">
              Question {currentQ + 1} of {questions.length}
            </span>
            <span className="text-sm text-muted-foreground">{question.marks} mark{question.marks !== 1 ? "s" : ""}</span>
          </div>
          <div className="mt-3 h-2.5 w-full rounded-full bg-muted overflow-hidden">
            <div
              className="h-full rounded-full bg-gradient-to-r from-primary to-primary/70 transition-all duration-500"
              style={{ width: `${((currentQ + 1) / questions.length) * 100}%` }}
            />
          </div>

          <h2 className="mt-5 text-2xl font-bold text-foreground leading-relaxed">{question.questionText}</h2>

          {(question.questionType === "MCQ" || question.questionType === "TRUE_FALSE") && question.options.length > 0 ? (
            <div className="mt-6 grid gap-3 sm:grid-cols-2">
              {question.options.map((opt, i) => {
                const isSelected = answers[question.id]?.selectedOptionId === opt.id
                return (
                  <button
                    key={opt.id}
                    onClick={() => onSelectOption(question.id, opt.id)}
                    className={`relative flex items-center gap-4 rounded-2xl border-2 p-5 text-left transition-all ${
                      isSelected
                        ? "border-primary bg-primary/5 shadow-md scale-[1.02]"
                        : "border-border hover:border-primary/30 hover:bg-muted/30"
                    }`}
                  >
                    <div className={`flex size-12 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br text-lg font-bold text-white ${optionColors[i % 4]}`}>
                      {optionLetters[i]}
                    </div>
                    <span className="text-lg font-medium text-foreground">{opt.optionText}</span>
                    {isSelected && (
                      <div className="absolute right-3 top-3">
                        <CheckCircle className="size-5 text-primary" />
                      </div>
                    )}
                  </button>
                )
              })}
            </div>
          ) : (
            <div className="mt-6">
              <textarea
                rows={4}
                placeholder="Write your answer here..."
                value={answers[question.id]?.textAnswer || ""}
                onChange={(e) => {}}
                className="w-full rounded-2xl border-2 border-border bg-muted/40 px-5 py-4 text-lg text-foreground outline-none focus:border-primary focus:bg-background resize-none transition-colors"
              />
            </div>
          )}
        </div>

        <div className="flex items-center justify-between">
          <button
            onClick={onPrev}
            disabled={currentQ === 0}
            className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-5 py-3 text-base font-medium text-foreground transition-colors hover:bg-muted disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <ArrowLeft className="size-4" /> Previous
          </button>
          {currentQ < questions.length - 1 ? (
            <button
              onClick={onNext}
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-6 py-3 text-base font-bold text-primary-foreground transition-colors hover:bg-primary/90"
            >
              Next <ArrowRight className="size-4" />
            </button>
          ) : (
            <button
              onClick={onSubmit}
              className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-6 py-3 text-base font-bold text-white transition-colors hover:bg-emerald-700"
            >
              <CheckCircle className="size-4" /> Submit Quiz
            </button>
          )}
        </div>
      </div>
    )
  }

  return null
}

export default function AssessmentDetailPage() {
  const params = useParams()
  const router = useRouter()
  const { user } = useRequireAuth()
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  const [state, setState] = useState<QuizState>("loading")
  const [assessment, setAssessment] = useState<Assessment | null>(null)
  const [questions, setQuestions] = useState<Question[]>([])
  const [attempt, setAttempt] = useState<Attempt | null>(null)
  const [answers, setAnswers] = useState<Record<string, { selectedOptionId?: string; textAnswer?: string }>>({})
  const [currentQ, setCurrentQ] = useState(0)
  const [timeLeft, setTimeLeft] = useState(0)
  const timerRef = useRef<NodeJS.Timeout | null>(null)

  useEffect(() => {
    if (!user || !params.id) return
    loadAssessment()
    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [user, params.id])

  async function loadAssessment() {
    try {
      setState("loading")
      const assessments = await assessmentApi.getByClass(user!.classGroupId || "")
      const found = assessments.find((a) => a.id === params.id)
      if (!found) {
        setState("ready")
        return
      }
      setAssessment(found)
      const qs = await assessmentApi.getQuestions(params.id as string)
      setQuestions(qs)
      setState("ready")
    } catch {
      setState("ready")
    }
  }

  const startQuiz = useCallback(async () => {
    if (!assessment || !user) return
    try {
      const att = await assessmentApi.startAttempt(assessment.id)
      setAttempt(att)
      if (assessment.timeLimitMinutes) {
        setTimeLeft(assessment.timeLimitMinutes * 60)
        timerRef.current = setInterval(() => {
          setTimeLeft((prev) => {
            if (prev <= 1) {
              if (timerRef.current) clearInterval(timerRef.current)
              return 0
            }
            return prev - 1
          })
        }, 1000)
      }
      setState("in_progress")
    } catch {
      // ignore
    }
  }, [assessment, user])

  const submitQuiz = useCallback(async () => {
    if (!attempt || !user) return
    if (timerRef.current) clearInterval(timerRef.current)
    setState("submitting")
    try {
      const answerList = Object.entries(answers).map(([questionId, a]) => ({
        questionId,
        selectedOptionId: a.selectedOptionId,
        textAnswer: a.textAnswer,
      }))
      const result = await assessmentApi.submitAttempt(attempt.id, answerList)
      setAttempt(result)
      setState("results")
    } catch {
      setState("in_progress")
    }
  }, [attempt, answers, user])

  const autoSubmit = useCallback(() => {
    submitQuiz()
  }, [submitQuiz])

  useEffect(() => {
    if (state === "in_progress" && timeLeft === 0 && assessment?.timeLimitMinutes) {
      autoSubmit()
    }
  }, [state, timeLeft, assessment, autoSubmit])

  function selectOption(questionId: string, optionId: string) {
    setAnswers((prev) => ({
      ...prev,
      [questionId]: { ...prev[questionId], selectedOptionId: optionId },
    }))
  }

  function setTextAnswer(questionId: string, text: string) {
    setAnswers((prev) => ({
      ...prev,
      [questionId]: { ...prev[questionId], textAnswer: text },
    }))
  }

  function formatTime(seconds: number) {
    const m = Math.floor(seconds / 60)
    const s = seconds % 60
    return `${m}:${s.toString().padStart(2, "0")}`
  }

  if (state === "loading") {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  if (!assessment) {
    return (
      <div className="space-y-4">
        <Button variant="ghost" size="sm" onClick={() => router.back()}>
          <ArrowLeft className="mr-2 size-4" /> Back
        </Button>
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <XCircle className="mx-auto size-12 text-destructive/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">Assessment Not Found</h3>
        </div>
      </div>
    )
  }

  if (isPrimary) {
    return (
      <PrimaryQuizView
        state={state}
        assessment={assessment}
        questions={questions}
        currentQ={currentQ}
        answers={answers}
        timeLeft={timeLeft}
        attempt={attempt}
        onSelectOption={selectOption}
        onPrev={() => setCurrentQ((c) => c - 1)}
        onNext={() => setCurrentQ((c) => c + 1)}
        onSubmit={submitQuiz}
        onStart={startQuiz}
        formatTime={formatTime}
      />
    )
  }

  if (state === "ready") {
    return (
      <div className="space-y-6">
        <Button variant="ghost" size="sm" onClick={() => router.back()} className="w-fit">
          <ArrowLeft className="mr-2 size-4" /> Back
        </Button>
        <div className="mx-auto max-w-lg rounded-2xl border border-border bg-card p-8 text-center">
          <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-primary/10">
            <Clock className="size-7 text-primary" />
          </div>
          <h1 className="mt-4 text-2xl font-bold text-foreground">{assessment.title}</h1>
          {assessment.description && (
            <p className="mt-2 text-sm text-muted-foreground">{assessment.description}</p>
          )}
          <div className="mt-6 space-y-2 text-sm text-muted-foreground">
            <p>Questions: {questions.length}</p>
            <p>Total Marks: {assessment.totalMarks}</p>
            <p>Pass Marks: {assessment.passMarks}</p>
            {assessment.timeLimitMinutes && <p>Time Limit: {assessment.timeLimitMinutes} minutes</p>}
          </div>
          <Button onClick={startQuiz} className="mt-8 gap-2">
            Start Quiz <ArrowRight className="size-4" />
          </Button>
        </div>
      </div>
    )
  }

  if (state === "results" && attempt?.result) {
    const result = attempt.result
    return (
      <div className="space-y-6">
        <Button variant="ghost" size="sm" onClick={() => router.push("/dashboard/assessments")} className="w-fit">
          <ArrowLeft className="mr-2 size-4" /> Back to Assessments
        </Button>
        <div className="mx-auto max-w-lg rounded-2xl border border-border bg-card p-8 text-center">
          <div className={`mx-auto flex size-14 items-center justify-center rounded-full ${result.isPassed ? "bg-primary/10" : "bg-destructive/10"}`}>
            {result.isPassed ? (
              <Trophy className="size-7 text-primary" />
            ) : (
              <XCircle className="size-7 text-destructive" />
            )}
          </div>
          <h1 className="mt-4 text-2xl font-bold text-foreground">
            {result.isPassed ? "Congratulations!" : "Keep Practicing"}
          </h1>
          <p className="mt-2 text-sm text-muted-foreground">
            {result.isPassed
              ? "You passed this assessment."
              : "You didn't pass this time. Review the material and try again."}
          </p>
          <div className="mt-6 space-y-2">
            <div className="flex items-center justify-between rounded-xl bg-muted/50 px-4 py-2 text-sm">
              <span className="text-muted-foreground">Score</span>
              <span className="font-bold text-foreground">{result.totalScore} / {assessment.totalMarks}</span>
            </div>
            <div className="flex items-center justify-between rounded-xl bg-muted/50 px-4 py-2 text-sm">
              <span className="text-muted-foreground">Pass Mark</span>
              <span className="font-medium text-foreground">{assessment.passMarks}</span>
            </div>
            <div className="flex items-center justify-between rounded-xl bg-muted/50 px-4 py-2 text-sm">
              <span className="text-muted-foreground">Status</span>
              <span className={`font-bold ${result.isPassed ? "text-primary" : "text-destructive"}`}>
                {result.isPassed ? "PASSED" : "FAILED"}
              </span>
            </div>
          </div>
          {result.feedback && (
            <div className="mt-4 rounded-xl bg-muted/30 p-4 text-left text-sm text-muted-foreground">
              {result.feedback}
            </div>
          )}
          <Button onClick={() => router.push("/dashboard/assessments")} className="mt-6">
            Back to Assessments
          </Button>
        </div>
      </div>
    )
  }

  const question = questions[currentQ]
  if (!question) return null

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <Button variant="ghost" size="sm" onClick={() => router.back()}>
          <ArrowLeft className="mr-2 size-4" /> Exit Quiz
        </Button>
        {assessment.timeLimitMinutes && (
          <div className={`flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-mono font-bold ${timeLeft < 60 ? "bg-destructive/10 text-destructive" : "bg-muted text-foreground"}`}>
            <Clock className="size-4" />
            {formatTime(timeLeft)}
          </div>
        )}
      </div>

      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>Question {currentQ + 1} of {questions.length}</span>
          <span>{question.marks} mark{question.marks !== 1 ? "s" : ""}</span>
        </div>
        <div className="mt-2 h-1.5 w-full rounded-full bg-muted overflow-hidden">
          <div
            className="h-full rounded-full bg-primary transition-all"
            style={{ width: `${((currentQ + 1) / questions.length) * 100}%` }}
          />
        </div>
        <h2 className="mt-4 text-lg font-semibold text-foreground">{question.questionText}</h2>
        <p className="mt-1 text-xs text-muted-foreground capitalize">{question.questionType.replace("_", " ")}</p>

        <div className="mt-6 space-y-3">
          {question.questionType === "MCQ" || question.questionType === "TRUE_FALSE" ? (
            question.options.map((opt) => (
              <label
                key={opt.id}
                className={`flex cursor-pointer items-center gap-3 rounded-xl border p-4 transition-all ${
                  answers[question.id]?.selectedOptionId === opt.id
                    ? "border-primary bg-primary/5 ring-1 ring-primary"
                    : "border-border hover:border-primary/30 hover:bg-muted/30"
                }`}
              >
                <input
                  type="radio"
                  name={question.id}
                  checked={answers[question.id]?.selectedOptionId === opt.id}
                  onChange={() => selectOption(question.id, opt.id)}
                  className="size-4 text-primary"
                />
                <span className="text-sm text-foreground">{opt.optionText}</span>
              </label>
            ))
          ) : (
            <textarea
              rows={4}
              placeholder="Type your answer here..."
              value={answers[question.id]?.textAnswer || ""}
              onChange={(e) => setTextAnswer(question.id, e.target.value)}
              className="w-full rounded-xl border border-border bg-muted/60 px-4 py-3 text-sm text-foreground outline-none focus:border-ring focus:bg-background resize-none"
            />
          )}
        </div>
      </div>

      <div className="flex items-center justify-between">
        <Button
          variant="outline"
          disabled={currentQ === 0}
          onClick={() => setCurrentQ((c) => c - 1)}
        >
          Previous
        </Button>
        <div className="flex gap-2">
          {currentQ < questions.length - 1 ? (
            <Button onClick={() => setCurrentQ((c) => c + 1)}>
              Next <ArrowRight className="ml-2 size-4" />
            </Button>
          ) : (
            <Button onClick={submitQuiz} disabled={state === "submitting"} className="gap-2">
              {state === "submitting" ? (
                <div className="size-4 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent" />
              ) : (
                <CheckCircle className="size-4" />
              )}
              Submit Quiz
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
