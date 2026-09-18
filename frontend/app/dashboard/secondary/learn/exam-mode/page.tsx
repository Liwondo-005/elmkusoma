"use client"

import { ArrowLeft, Clock, AlertTriangle } from "lucide-react"
import Link from "next/link"
import { useState, useEffect } from "react"

const EXAM_QUESTIONS = [
  { q: "What is the powerhouse of the cell?", options: ["Nucleus", "Mitochondria", "Ribosome", "Cell membrane"], correct: 1 },
  { q: "Simplify: 3(2x - 4)", options: ["6x - 4", "6x - 12", "6x - 8", "3x - 12"], correct: 1 },
  { q: "Which planet is closest to the Sun?", options: ["Venus", "Mars", "Mercury", "Earth"], correct: 2 },
]

export default function ExamModePage() {
  const [started, setStarted] = useState(false)
  const [current, setCurrent] = useState(0)
  const [selected, setSelected] = useState<number | null>(null)
  const [answers, setAnswers] = useState<(number | null)[]>([])
  const [timeLeft, setTimeLeft] = useState(300)
  const [submitted, setSubmitted] = useState(false)

  useEffect(() => {
    if (!started || submitted || timeLeft <= 0) return
    const timer = setInterval(() => setTimeLeft(t => t - 1), 1000)
    return () => clearInterval(timer)
  }, [started, submitted, timeLeft])

  useEffect(() => {
    if (timeLeft === 0 && started && !submitted) handleSubmit()
  }, [timeLeft, started, submitted])

  function handleSelect(idx: number) {
    if (submitted) return
    setSelected(idx)
  }

  function handleSubmit() {
    setSubmitted(true)
  }

  function getScore() {
    return answers.reduce((score, ans, i) => score + (ans === EXAM_QUESTIONS[i].correct ? 1 : 0), 0)
  }

  if (!started) {
    return (
      <div className="mx-auto max-w-3xl space-y-6 p-4 pb-24">
        <div className="flex items-center gap-3">
          <Link href="/dashboard/secondary/revision" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
            <ArrowLeft className="size-5 text-gray-600" />
          </Link>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Exam Mode</h1>
            <p className="text-sm text-gray-500">Timed practice exam</p>
          </div>
        </div>

        <div className="rounded-2xl bg-gradient-to-r from-red-600 to-orange-500 p-6 text-white">
          <h2 className="text-lg font-bold">Practice Exam</h2>
          <p className="mt-1 text-sm text-white/70">{EXAM_QUESTIONS.length} questions · 5 minutes</p>
          <div className="mt-4 space-y-2 text-sm">
            <p>• No going back to previous questions</p>
            <p>• Timer cannot be paused</p>
            <p>• Answer all questions before time runs out</p>
          </div>
        </div>

        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4">
          <div className="flex items-center gap-2">
            <AlertTriangle className="size-5 text-amber-600" />
            <p className="text-sm font-semibold text-amber-800">Exam Conditions</p>
          </div>
          <p className="mt-1 text-sm text-amber-700">Treat this like a real exam. No notes, no help.</p>
        </div>

        <button onClick={() => { setStarted(true); setAnswers(new Array(EXAM_QUESTIONS.length).fill(null)) }} className="w-full rounded-xl bg-red-600 py-3 text-sm font-bold text-white hover:bg-red-700">
          Start Exam
        </button>
      </div>
    )
  }

  const q = EXAM_QUESTIONS[current]

  return (
    <div className="mx-auto max-w-3xl space-y-6 p-4 pb-24">
      {/* Timer */}
      <div className={`flex items-center justify-between rounded-2xl p-4 ${timeLeft < 60 ? "bg-red-100" : "bg-gray-100"}`}>
        <span className="text-sm font-medium text-gray-600">Question {current + 1}/{EXAM_QUESTIONS.length}</span>
        <div className="flex items-center gap-2">
          <Clock className={`size-5 ${timeLeft < 60 ? "text-red-500" : "text-gray-500"}`} />
          <span className={`text-lg font-bold ${timeLeft < 60 ? "text-red-600" : "text-gray-800"}`}>
            {Math.floor(timeLeft / 60)}:{(timeLeft % 60).toString().padStart(2, "0")}
          </span>
        </div>
      </div>

      {/* Question */}
      <div className="rounded-2xl border border-gray-100 bg-white p-6">
        <h2 className="text-lg font-bold text-gray-900">{q.q}</h2>
        <div className="mt-4 space-y-2">
          {q.options.map((opt, i) => (
            <button
              key={i}
              onClick={() => handleSelect(i)}
              disabled={submitted}
              className={`flex w-full items-center gap-3 rounded-xl border-2 p-4 text-left transition-all ${
                submitted && i === q.correct ? "border-green-300 bg-green-50" :
                submitted && i === selected ? "border-red-300 bg-red-50" :
                selected === i ? "border-indigo-300 bg-indigo-50" : "border-gray-100 hover:border-indigo-200"
              }`}
            >
              <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-gray-100 text-sm font-bold text-gray-500">
                {String.fromCharCode(65 + i)}
              </span>
              <span className="text-sm font-medium text-gray-700">{opt}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Actions */}
      {!submitted ? (
        <div className="flex gap-3">
          <button
            onClick={() => { setAnswers(a => { const n = [...a]; n[current] = selected; return n }); setSelected(null); setCurrent(c => c + 1) }}
            disabled={current === EXAM_QUESTIONS.length - 1}
            className="flex-1 rounded-xl bg-indigo-600 py-3 text-sm font-bold text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            Next
          </button>
          {current === EXAM_QUESTIONS.length - 1 && (
            <button onClick={handleSubmit} className="flex-1 rounded-xl bg-red-600 py-3 text-sm font-bold text-white hover:bg-red-700">
              Submit Exam
            </button>
          )}
        </div>
      ) : (
        <div className="rounded-2xl bg-gradient-to-r from-indigo-600 to-purple-500 p-6 text-center text-white">
          <p className="text-3xl font-bold">{getScore()}/{EXAM_QUESTIONS.length}</p>
          <p className="mt-1 text-sm text-white/70">correct answers</p>
          <Link href="/dashboard/secondary/revision" className="mt-4 inline-block rounded-xl bg-white/20 px-6 py-2 text-sm font-medium text-white hover:bg-white/30">
            Back to Revision
          </Link>
        </div>
      )}
    </div>
  )
}
