"use client"

import { ArrowLeft, Brain, CheckCircle, XCircle, AlertCircle } from "lucide-react"
import Link from "next/link"
import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type SecondaryProblem } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"

const BUILTIN_PROBLEMS = [
  { id: "builtin-1", problemTitle: "Solve: 2x + 5 = 15", problemDescription: "Find the value of x", problemType: "MCQ" as const, options: '["x = 5","x = 10","x = 7.5","x = 2.5"]', correctAnswer: "x = 5", solution: "Subtract 5: 2x = 10. Divide by 2: x = 5.", difficultyLevel: "BASIC" as const, category: "Algebra" },
  { id: "builtin-2", problemTitle: "What is the powerhouse of the cell?", problemDescription: "Identify the organelle responsible for energy production", problemType: "MCQ" as const, options: '["Nucleus","Mitochondria","Ribosome","Cell membrane"]', correctAnswer: "Mitochondria", solution: "Mitochondria produce ATP through cellular respiration.", difficultyLevel: "BASIC" as const, category: "Biology" },
  { id: "builtin-3", problemTitle: "Calculate the force: F = ma", problemDescription: "A 5 kg object accelerates at 3 m/s². What is the force?", problemType: "NUMERICAL" as const, options: null, correctAnswer: "15 N", solution: "F = 5 × 3 = 15 N", difficultyLevel: "BASIC" as const, category: "Physics" },
]

export default function ProblemSolvingPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("secondary")
  const tc = useTranslations("common")
  const [apiProblems, setApiProblems] = useState<SecondaryProblem[]>([])
  const [selected, setSelected] = useState<string | null>(null)
  const [answers, setAnswers] = useState<Record<string, number>>({})
  const [submitted, setSubmitted] = useState<Record<string, boolean>>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (user?.classGroupId) {
      secondaryApi.getProblemsByClass(user.classGroupId)
        .then(setApiProblems)
        .catch(() => setError(t("loadError")))
        .finally(() => setLoading(false))
    } else { setLoading(false) }
  }, [user, t])

  if (loading) return <LoadingState />

  if (error) {
    return (
      <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
        <div className="rounded-2xl border border-red-200 bg-red-50 p-8 text-center">
          <AlertCircle className="mx-auto size-12 text-red-400" />
          <h3 className="mt-3 text-lg font-bold text-red-800">{tc("error.generic")}</h3>
          <p className="mt-1 text-sm text-red-600">{error}</p>
        </div>
      </div>
    )
  }

  const allProblems = [
    ...BUILTIN_PROBLEMS,
    ...apiProblems.map(p => ({ id: p.id, problemTitle: p.problemTitle, problemDescription: p.problemDescription, problemType: p.problemType, options: p.options, correctAnswer: p.correctAnswer, solution: p.solution || "", difficultyLevel: p.difficultyLevel, category: "" })),
  ]

  function handleAnswer(problemId: string, optionIdx: number) { setAnswers(prev => ({ ...prev, [problemId]: optionIdx })) }
  function handleSubmit(problemId: string) { setSubmitted(prev => ({ ...prev, [problemId]: true })) }

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary/learn" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("secondary.backToLearn")}><ArrowLeft className="size-5 text-gray-600" /></Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{t("secondary.problemSolvingEngine")}</h1>
          <p className="text-sm text-gray-500">{t("secondary.practiceSolvingProblemsStepByStep")}</p>
        </div>
      </div>

      <div className="space-y-4">
        {allProblems.map(problem => {
          const options: string[] = problem.options ? JSON.parse(problem.options) : []
          const isSelected = answers[problem.id] !== undefined
          const isSubmitted = submitted[problem.id]
          const isCorrect = isSubmitted && options[answers[problem.id]] === problem.correctAnswer

          return (
            <div key={problem.id} className="rounded-2xl border border-gray-100 bg-white p-6">
              <div className="flex items-center gap-2"><Brain className="size-5 text-purple-600" /><span className="text-xs font-semibold uppercase tracking-wider text-purple-600">{problem.difficultyLevel}</span></div>
              <h3 className="mt-2 font-semibold text-gray-900">{problem.problemTitle}</h3>
              <p className="mt-1 text-sm text-gray-600">{problem.problemDescription}</p>

              {options.length > 0 && (
                <div className="mt-4 space-y-2">
                  {options.map((opt, i) => (
                    <button key={i} onClick={() => !isSubmitted && handleAnswer(problem.id, i)} disabled={isSubmitted}
                      aria-label={`${String.fromCharCode(65 + i)}: ${opt}`}
                      className={`flex w-full items-center gap-3 rounded-xl border p-3 text-left text-sm transition-all ${
                        isSubmitted && opt === problem.correctAnswer ? "border-green-200 bg-green-50" :
                        isSubmitted && answers[problem.id] === i && opt !== problem.correctAnswer ? "border-red-200 bg-red-50" :
                        answers[problem.id] === i ? "border-indigo-200 bg-indigo-50" : "border-gray-100 hover:border-gray-200"
                      }`}>
                      <span className="flex size-6 shrink-0 items-center justify-center rounded-full border text-xs font-bold">{String.fromCharCode(65 + i)}</span>
                      <span className="flex-1">{opt}</span>
                      {isSubmitted && opt === problem.correctAnswer && <CheckCircle className="size-4 shrink-0 text-green-600" />}
                      {isSubmitted && answers[problem.id] === i && opt !== problem.correctAnswer && <XCircle className="size-4 shrink-0 text-red-600" />}
                    </button>
                  ))}
                </div>
              )}

              {!isSubmitted ? (
                <button onClick={() => handleSubmit(problem.id)} disabled={!isSelected} className="mt-4 w-full rounded-xl bg-indigo-600 py-2.5 text-sm font-semibold text-white disabled:opacity-50" aria-label={t("secondary.checkAnswer")}>{t("secondary.checkAnswer")}</button>
              ) : (
                <div className="mt-4 rounded-xl bg-blue-50 p-4"><p className="text-sm font-medium text-blue-800">{t("secondary.solution")}</p><p className="mt-1 text-sm text-blue-700">{problem.solution}</p></div>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
