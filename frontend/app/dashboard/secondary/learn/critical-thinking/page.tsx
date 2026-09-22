"use client"

import { useState } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { ArrowLeft, Brain, CheckCircle, XCircle, ChevronRight } from "lucide-react"

const challenges = [
  {
    id: "ct-1",
    title: "Identify the Logical Fallacy",
    description: "Read the argument below and identify which logical fallacy is being used.",
    scenario: `"All students who study hard get good grades. John got good grades, so John must have studied hard."`,
    options: [
      { id: "a", text: "Ad Hominem — attacking the person", correct: false },
      { id: "b", text: "Affirming the Consequent — reversing cause and effect", correct: true },
      { id: "c", text: "Straw Man — misrepresenting the argument", correct: false },
      { id: "d", text: "Appeal to Authority — using authority as evidence", correct: false },
    ],
    explanation: "This is Affirming the Consequent. The argument assumes that because studying leads to good grades, good grades must mean studying happened. But John could have gotten good grades for other reasons.",
    difficulty: "Medium",
  },
  {
    id: "ct-2",
    title: "Evaluate the Evidence",
    description: "A news headline says: 'Study shows chocolate cures cancer — 95% of patients improved!' What questions should you ask?",
    scenario: `A viral article claims chocolate cures cancer based on a study of 20 patients with no control group.`,
    options: [
      { id: "a", text: "Was the study peer-reviewed? Was there a control group? How large was the sample?", correct: true },
      { id: "b", text: "Chocolate is delicious, so it probably works", correct: false },
      { id: "c", text: "The article has many shares, so it must be true", correct: false },
      { id: "d", text: "If chocolate didn't work, someone would have debunked it", correct: false },
    ],
    explanation: "Good critical thinking means evaluating the quality of evidence: peer review, control groups, and sample size are essential for scientific validity. Viral sharing and personal opinion don't validate claims.",
    difficulty: "Easy",
  },
  {
    id: "ct-3",
    title: "Analyze the Argument",
    description: "Read this argument and identify its strongest and weakest points.",
    scenario: `"School uniforms should be mandatory because they reduce bullying based on clothing, create a sense of community, and save parents money on clothes. However, some argue they restrict personal expression."`,
    options: [
      { id: "a", text: "Strongest: community sense. Weakest: cost savings claim needs data.", correct: true },
      { id: "b", text: "Strongest: personal expression. Weakest: bullying reduction.", correct: false },
      { id: "c", text: "The argument is entirely weak because uniforms are uncomfortable.", correct: false },
      { id: "d", text: "There is no way to evaluate this argument.", correct: false },
    ],
    explanation: "The strongest point is the community-building aspect, which has direct observable evidence. The weakest point is the cost savings claim, which depends on uniform prices vs. regular clothing prices and needs supporting data.",
    difficulty: "Hard",
  },
]

export default function CriticalThinkingPage() {
  const t = useTranslations("secondary")
  const tc = useTranslations("common")
  const [selectedChallenge, setSelectedChallenge] = useState<string | null>(null)
  const [answers, setAnswers] = useState<Record<string, string>>({})
  const [submitted, setSubmitted] = useState<Record<string, boolean>>({})
  const activeChallenge = challenges.find(c => c.id === selectedChallenge)

  function handleAnswer(challengeId: string, optionId: string) {
    setAnswers(prev => ({ ...prev, [challengeId]: optionId }))
  }

  function handleSubmit(challengeId: string) {
    setSubmitted(prev => ({ ...prev, [challengeId]: true }))
  }

  function getScore() {
    const answered = challenges.filter(c => submitted[c.id])
    const correct = answered.filter(c => {
      const selected = answers[c.id]
      return c.options.find(o => o.id === selected)?.correct
    })
    return { total: challenges.length, answered: answered.length, correct: correct.length }
  }

  const score = getScore()

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary/learn" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("secondary.backToLearn")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{t("secondary.criticalThinking")}</h1>
          <p className="text-sm text-gray-500">{t("secondary.analyzeAndEvaluateArguments")}</p>
        </div>
      </div>

      {score.answered > 0 && (
        <div className="flex gap-3">
          <div className="flex-1 rounded-xl bg-green-50 p-3 text-center">
            <p className="text-xl font-bold text-green-600">{score.correct}</p>
            <p className="text-[10px] font-medium text-green-700">{t("secondary.correct")}</p>
          </div>
          <div className="flex-1 rounded-xl bg-amber-50 p-3 text-center">
            <p className="text-xl font-bold text-amber-600">{score.answered - score.correct}</p>
            <p className="text-[10px] font-medium text-amber-700">{t("secondary.incorrect")}</p>
          </div>
          <div className="flex-1 rounded-xl bg-indigo-50 p-3 text-center">
            <p className="text-xl font-bold text-indigo-600">{score.answered}/{score.total}</p>
            <p className="text-[10px] font-medium text-indigo-700">{t("secondary.completed")}</p>
          </div>
        </div>
      )}

      {selectedChallenge && activeChallenge ? (
        <div className="space-y-4">
          <button onClick={() => setSelectedChallenge(null)} className="text-sm font-semibold text-indigo-600 hover:text-indigo-700" aria-label={t("secondary.backToChallenges")}>
            {t("secondary.backToChallenges")}
          </button>
          <div className="rounded-2xl border border-gray-100 bg-white p-6">
            <div className="flex items-center gap-2">
              <Brain className="size-5 text-purple-600" />
              <span className="text-xs font-semibold uppercase tracking-wider text-purple-600">
                {activeChallenge.difficulty}
              </span>
            </div>
            <h2 className="mt-2 text-lg font-bold text-gray-900">{activeChallenge.title}</h2>
            <p className="mt-1 text-sm text-gray-600">{activeChallenge.description}</p>
            <div className="mt-4 rounded-xl bg-gray-50 p-4">
              <p className="text-sm text-gray-700 italic">{activeChallenge.scenario}</p>
            </div>

            <div className="mt-4 space-y-2">
              {activeChallenge.options.map(option => {
                const isSelected = answers[activeChallenge.id] === option.id
                const isSubmitted = submitted[activeChallenge.id]
                return (
                  <button
                    key={option.id}
                    onClick={() => !isSubmitted && handleAnswer(activeChallenge.id, option.id)}
                    disabled={isSubmitted}
                    aria-label={`${option.id.toUpperCase()}: ${option.text}`}
                    className={`flex w-full items-center gap-3 rounded-xl border p-3 text-left text-sm transition-all ${
                      isSubmitted && option.correct
                        ? "border-green-200 bg-green-50"
                        : isSubmitted && isSelected && !option.correct
                          ? "border-red-200 bg-red-50"
                          : isSelected
                            ? "border-indigo-200 bg-indigo-50"
                            : "border-gray-100 hover:border-gray-200"
                    }`}
                  >
                    <span className="flex size-6 shrink-0 items-center justify-center rounded-full border text-xs font-bold">
                      {option.id.toUpperCase()}
                    </span>
                    <span className="flex-1">{option.text}</span>
                    {isSubmitted && option.correct && <CheckCircle className="size-4 shrink-0 text-green-600" />}
                    {isSubmitted && isSelected && !option.correct && <XCircle className="size-4 shrink-0 text-red-600" />}
                  </button>
                )
              })}
            </div>

            {!submitted[activeChallenge.id] ? (
              <button
                onClick={() => handleSubmit(activeChallenge.id)}
                disabled={!answers[activeChallenge.id]}
                className="mt-4 w-full rounded-xl bg-indigo-600 py-3 text-sm font-semibold text-white transition-colors hover:bg-indigo-700 disabled:opacity-50"
                aria-label={t("secondary.checkAnswer")}
              >
                {t("secondary.checkAnswer")}
              </button>
            ) : (
              <div className="mt-4 rounded-xl bg-blue-50 p-4">
                <p className="text-sm font-medium text-blue-800">{t("secondary.explanation")}</p>
                <p className="mt-1 text-sm text-blue-700">{activeChallenge.explanation}</p>
              </div>
            )}
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          {challenges.map(challenge => (
            <button
              key={challenge.id}
              onClick={() => setSelectedChallenge(challenge.id)}
              className="flex w-full items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 text-left transition-all hover:border-indigo-200 hover:shadow-sm"
              aria-label={`${challenge.title} - ${challenge.difficulty}`}
            >
              <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-purple-50">
                <Brain className="size-6 text-purple-600" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="font-semibold text-gray-900">{challenge.title}</p>
                <p className="text-xs text-gray-400">{challenge.difficulty} · 4 {t("secondary.options")}</p>
              </div>
              {submitted[challenge.id] ? (
                <CheckCircle className="size-5 shrink-0 text-green-500" />
              ) : (
                <ChevronRight className="size-5 shrink-0 text-gray-300" />
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
