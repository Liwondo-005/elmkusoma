"use client"

import { useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { type LearningLevel } from "@/lib/learner-config"
import { Zap, Clock, Brain, Timer, Lightbulb, Trophy, ArrowRight } from "lucide-react"

const challengeTypes = [
  { id: "quick", name: "Quick Challenges", description: "1-minute problems", icon: Timer, color: "text-blue-600", bg: "bg-blue-50", border: "border-blue-200" },
  { id: "brain", name: "Brain Teasers", description: "Logic puzzles", icon: Brain, color: "text-purple-600", bg: "bg-purple-50", border: "border-purple-200" },
  { id: "speed", name: "Speed Round", description: "Timed challenges", icon: Zap, color: "text-amber-600", bg: "bg-amber-50", border: "border-amber-200" },
  { id: "creative", name: "Creative Challenge", description: "Open-ended problems", icon: Lightbulb, color: "text-pink-600", bg: "bg-pink-50", border: "border-pink-200" },
]

const challenges = [
  { id: "1", title: "Quick Math: What is 7 x 8?", type: "quick", difficulty: "Easy", points: 10, answer: "56", options: ["48", "56", "64", "54"] },
  { id: "2", title: "Brain Teaser: I have cities, but no houses. I have mountains, but no trees. What am I?", type: "brain", difficulty: "Medium", points: 20, answer: "A map", options: ["A photo", "A map", "A book", "A window"] },
  { id: "3", title: "Speed Round: What is the capital of Tanzania?", type: "speed", difficulty: "Easy", points: 10, answer: "Dodoma", options: ["Dar es Salaam", "Dodoma", "Arusha", "Mwanza"] },
  { id: "4", title: "Creative: If you could发明 one thing to help your classroom, what would it be?", type: "creative", difficulty: "Hard", points: 30, answer: null, options: [] },
  { id: "5", title: "Quick Math: What is 144 / 12?", type: "quick", difficulty: "Easy", points: 10, answer: "12", options: ["10", "11", "12", "13"] },
  { id: "6", title: "Brain Teaser: What has keys but no locks?", type: "brain", difficulty: "Medium", points: 20, answer: "A piano", options: ["A door", "A piano", "A computer", "A car"] },
  { id: "7", title: "Speed Round: How many legs does a spider have?", type: "speed", difficulty: "Easy", points: 10, answer: "8", options: ["6", "8", "10", "12"] },
  { id: "8", title: "Creative: Draw your dream school and describe it", type: "creative", difficulty: "Hard", points: 30, answer: null, options: [] },
]

export default function ChallengeZonePage() {
  const { user } = useRequireAuth()
  const [selectedType, setSelectedType] = useState<string | null>(null)
  const [activeChallenge, setActiveChallenge] = useState<typeof challenges[0] | null>(null)
  const [selectedAnswer, setSelectedAnswer] = useState("")
  const [showResult, setShowResult] = useState(false)
  const [scores, setScores] = useState<Record<string, number>>({})
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  const filtered = selectedType
    ? challenges.filter((c) => c.type === selectedType)
    : challenges

  const totalScore = Object.values(scores).reduce((sum, s) => sum + s, 0)
  const completedCount = Object.keys(scores).length

  function handleSubmit() {
    if (!activeChallenge) return
    setShowResult(true)
    if (activeChallenge.answer && selectedAnswer === activeChallenge.answer) {
      setScores((prev) => ({ ...prev, [activeChallenge.id]: activeChallenge.points }))
    }
  }

  function handleNext() {
    setActiveChallenge(null)
    setSelectedAnswer("")
    setShowResult(false)
  }

  if (!isPrimary) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-2xl bg-amber-500/10">
            <Zap className="size-5 text-amber-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Challenge Zone</h1>
            <p className="text-sm text-muted-foreground">Ready for a challenge?</p>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <Zap className="size-12 text-muted-foreground/30" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">Challenge Zone is for Primary learners</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Switch to a primary learner account to take on challenges.
          </p>
        </div>
      </div>
    )
  }

  if (activeChallenge) {
    const cat = challengeTypes.find((c) => c.id === activeChallenge.type)
    const CatIcon = cat?.icon || Zap
    return (
      <div className="mx-auto max-w-4xl space-y-6">
        <button
          onClick={handleNext}
          className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
        >
          Back to Challenges
        </button>

        <div className="rounded-2xl border border-border bg-card p-8 shadow-xs">
          <div className="flex items-center gap-3 mb-6">
            <div className={`flex size-12 items-center justify-center rounded-2xl ${cat?.bg || "bg-muted"}`}>
              <CatIcon className={`size-6 ${cat?.color || "text-muted-foreground"}`} />
            </div>
            <div>
              <h1 className="text-lg font-bold text-foreground">{activeChallenge.title}</h1>
              <div className="mt-1 flex items-center gap-2">
                <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
                  {activeChallenge.difficulty}
                </span>
                <span className="flex items-center gap-1 text-xs text-muted-foreground">
                  <Trophy className="size-3" /> {activeChallenge.points} pts
                </span>
              </div>
            </div>
          </div>

          {!showResult ? (
            <div className="space-y-4">
              {activeChallenge.options.length > 0 ? (
                <div className="grid gap-3 sm:grid-cols-2">
                  {activeChallenge.options.map((opt) => (
                    <button
                      key={opt}
                      onClick={() => setSelectedAnswer(opt)}
                      className={`rounded-xl border p-4 text-left text-sm font-medium transition-all ${
                        selectedAnswer === opt
                          ? "border-primary bg-primary/5 text-primary ring-2 ring-primary/20"
                          : "border-border bg-background hover:border-primary/30"
                      }`}
                    >
                      {opt}
                    </button>
                  ))}
                </div>
              ) : (
                <textarea
                  value={selectedAnswer}
                  onChange={(e) => setSelectedAnswer(e.target.value)}
                  placeholder="Write your answer here..."
                  rows={4}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
                />
              )}
              <button
                onClick={handleSubmit}
                disabled={!selectedAnswer.trim()}
                className="inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
              >
                Submit Answer
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              {activeChallenge.answer ? (
                <div className={`rounded-xl p-6 text-center ${selectedAnswer === activeChallenge.answer ? "bg-green-50 border border-green-200" : "bg-red-50 border border-red-200"}`}>
                  <h3 className={`text-lg font-bold ${selectedAnswer === activeChallenge.answer ? "text-green-800" : "text-red-800"}`}>
                    {selectedAnswer === activeChallenge.answer ? "Correct!" : "Not quite!"}
                  </h3>
                  <p className={`mt-1 text-sm ${selectedAnswer === activeChallenge.answer ? "text-green-700" : "text-red-700"}`}>
                    The answer is: {activeChallenge.answer}
                  </p>
                  {selectedAnswer === activeChallenge.answer && (
                    <p className="mt-2 text-sm font-medium text-green-600">+{activeChallenge.points} points!</p>
                  )}
                </div>
              ) : (
                <div className="rounded-xl bg-purple-50 border border-purple-200 p-6 text-center">
                  <h3 className="text-lg font-bold text-purple-800">Great thinking!</h3>
                  <p className="mt-1 text-sm text-purple-700">Creative challenges encourage imagination.</p>
                  <p className="mt-2 text-sm font-medium text-purple-600">+{activeChallenge.points} points!</p>
                </div>
              )}
              <button
                onClick={handleNext}
                className="inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
              >
                Next Challenge <ArrowRight className="size-4" />
              </button>
            </div>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-amber-500/5 via-card to-blue-500/5 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-500/10">
            <Zap className="size-6 text-amber-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Challenge Zone</h1>
            <p className="text-sm text-muted-foreground">Ready for a challenge? Try these harder problems!</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-amber-50">
              <Trophy className="size-5 text-amber-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{totalScore}</p>
              <p className="text-xs text-muted-foreground">Total Score</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-green-50">
              <Zap className="size-5 text-green-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{completedCount}</p>
              <p className="text-xs text-muted-foreground">Challenges Done</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-blue-50">
              <Clock className="size-5 text-blue-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{challenges.length - completedCount}</p>
              <p className="text-xs text-muted-foreground">Remaining</p>
            </div>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {challengeTypes.map((type) => {
          const Icon = type.icon
          const count = challenges.filter((c) => c.type === type.id).length
          return (
            <button
              key={type.id}
              onClick={() => setSelectedType(selectedType === type.id ? null : type.id)}
              className={`rounded-2xl border p-5 text-left transition-all hover:shadow-md ${
                selectedType === type.id
                  ? `${type.border} ${type.bg} ring-2 ring-primary/20`
                  : "border-border bg-card hover:border-primary/30"
              }`}
            >
              <div className={`flex size-10 items-center justify-center rounded-xl ${type.bg}`}>
                <Icon className={`size-5 ${type.color}`} />
              </div>
              <h3 className="mt-3 text-sm font-bold text-foreground">{type.name}</h3>
              <p className="mt-1 text-xs text-muted-foreground">{type.description}</p>
              <p className="mt-2 text-xs text-muted-foreground">{count} challenges</p>
            </button>
          )
        })}
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {filtered.map((challenge) => {
          const cat = challengeTypes.find((c) => c.id === challenge.type)
          const CatIcon = cat?.icon || Zap
          const isCompleted = scores[challenge.id] !== undefined
          return (
            <button
              key={challenge.id}
              onClick={() => setActiveChallenge(challenge)}
              className="rounded-2xl border border-border bg-card p-5 text-left shadow-xs transition-all hover:shadow-md hover:border-primary/30"
            >
              <div className="flex items-start gap-3">
                <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${cat?.bg || "bg-muted"}`}>
                  <CatIcon className={`size-5 ${cat?.color || "text-muted-foreground"}`} />
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-sm font-bold text-foreground line-clamp-2">{challenge.title}</h3>
                </div>
              </div>
              <div className="mt-3 flex items-center gap-2">
                <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                  {challenge.difficulty}
                </span>
                <span className="flex items-center gap-1 text-[10px] text-muted-foreground">
                  <Trophy className="size-3" /> {challenge.points} pts
                </span>
                {isCompleted && (
                  <span className="rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-medium text-green-700">
                    +{scores[challenge.id]}
                  </span>
                )}
              </div>
            </button>
          )
        })}
      </div>

      {totalScore > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
            <Trophy className="size-5 text-amber-500" />
            Leaderboard
          </h2>
          <div className="mt-4 space-y-3">
            <div className="flex items-center gap-3 rounded-xl bg-primary/5 p-3">
              <span className="flex size-8 items-center justify-center rounded-full bg-amber-100 text-sm font-bold text-amber-700">1</span>
              <div className="flex-1">
                <p className="text-sm font-bold text-foreground">You</p>
                <p className="text-xs text-muted-foreground">{completedCount} challenges completed</p>
              </div>
              <span className="text-lg font-bold text-primary">{totalScore} pts</span>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
