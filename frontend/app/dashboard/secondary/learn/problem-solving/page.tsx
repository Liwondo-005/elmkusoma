"use client"

import { ArrowLeft, Brain, CheckCircle, XCircle } from "lucide-react"
import Link from "next/link"
import { useState } from "react"

const PROBLEMS = [
  {
    id: "1", subject: "Mathematics", difficulty: "Medium",
    question: "Solve: 2x + 5 = 15",
    options: ["x = 5", "x = 10", "x = 7.5", "x = 2.5"],
    correct: 0,
    explanation: "Subtract 5 from both sides: 2x = 10. Divide by 2: x = 5."
  },
  {
    id: "2", subject: "Science", difficulty: "Easy",
    question: "What is the chemical formula for water?",
    options: ["H2O", "CO2", "NaCl", "O2"],
    correct: 0,
    explanation: "Water is made of 2 hydrogen atoms and 1 oxygen atom: H₂O."
  },
  {
    id: "3", subject: "English", difficulty: "Easy",
    question: "Which word is a synonym of 'happy'?",
    options: ["Sad", "Joyful", "Angry", "Tired"],
    correct: 1,
    explanation: "Joyful means feeling or showing great happiness, same as happy."
  },
]

export default function ProblemSolvingPage() {
  const [current, setCurrent] = useState(0)
  const [selected, setSelected] = useState<number | null>(null)
  const [score, setScore] = useState(0)
  const [showExplanation, setShowExplanation] = useState(false)
  const problem = PROBLEMS[current]

  function handleAnswer(idx: number) {
    if (selected !== null) return
    setSelected(idx)
    setShowExplanation(true)
    if (idx === problem.correct) setScore(s => s + 1)
  }

  function nextProblem() {
    if (current < PROBLEMS.length - 1) {
      setCurrent(c => c + 1)
      setSelected(null)
      setShowExplanation(false)
    }
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Problem Solving</h1>
          <p className="text-sm text-gray-500">Practice solving problems step by step</p>
        </div>
      </div>

      {/* Progress */}
      <div className="flex items-center gap-3">
        <div className="h-2 flex-1 overflow-hidden rounded-full bg-gray-100">
          <div className="h-full rounded-full bg-indigo-500 transition-all" style={{ width: `${((current + 1) / PROBLEMS.length) * 100}%` }} />
        </div>
        <span className="text-sm font-medium text-gray-500">{current + 1}/{PROBLEMS.length}</span>
      </div>

      {/* Problem */}
      <div className="rounded-2xl border border-gray-100 bg-white p-6">
        <div className="flex items-center gap-2 text-xs font-semibold text-indigo-600">
          <Brain className="size-4" />
          {problem.subject} · {problem.difficulty}
        </div>
        <h2 className="mt-3 text-lg font-bold text-gray-900">{problem.question}</h2>

        <div className="mt-4 space-y-2">
          {problem.options.map((opt, i) => (
            <button
              key={i}
              onClick={() => handleAnswer(i)}
              disabled={selected !== null}
              className={`flex w-full items-center gap-3 rounded-xl border-2 p-4 text-left transition-all ${
                selected === null ? "border-gray-100 hover:border-indigo-200" :
                i === problem.correct ? "border-green-300 bg-green-50" :
                i === selected ? "border-red-300 bg-red-50" : "border-gray-100 opacity-50"
              }`}
            >
              <div className={`flex size-8 shrink-0 items-center justify-center rounded-full text-sm font-bold ${
                selected === null ? "bg-gray-100 text-gray-500" :
                i === problem.correct ? "bg-green-200 text-green-700" :
                i === selected ? "bg-red-200 text-red-700" : "bg-gray-100 text-gray-400"
              }`}>
                {selected !== null && i === problem.correct ? <CheckCircle className="size-5" /> :
                 selected !== null && i === selected ? <XCircle className="size-5" /> :
                 String.fromCharCode(65 + i)}
              </div>
              <span className="text-sm font-medium text-gray-700">{opt}</span>
            </button>
          ))}
        </div>

        {showExplanation && (
          <div className="mt-4 rounded-xl bg-blue-50 p-4">
            <p className="text-sm font-semibold text-blue-800">Explanation</p>
            <p className="mt-1 text-sm text-blue-700">{problem.explanation}</p>
          </div>
        )}
      </div>

      {selected !== null && current < PROBLEMS.length - 1 && (
        <button onClick={nextProblem} className="w-full rounded-xl bg-indigo-600 py-3 text-sm font-bold text-white hover:bg-indigo-700">
          Next Problem
        </button>
      )}

      {selected !== null && current === PROBLEMS.length - 1 && (
        <div className="rounded-2xl bg-gradient-to-r from-indigo-600 to-purple-500 p-6 text-center text-white">
          <p className="text-2xl font-bold">{score}/{PROBLEMS.length}</p>
          <p className="mt-1 text-sm text-white/70">problems solved correctly</p>
          <button onClick={() => { setCurrent(0); setSelected(null); setScore(0); setShowExplanation(false) }} className="mt-4 rounded-xl bg-white/20 px-6 py-2 text-sm font-medium text-white hover:bg-white/30">
            Try Again
          </button>
        </div>
      )}
    </div>
  )
}
