"use client"

import { useState } from "react"
import { ArrowLeft, BookOpen, Star, Volume2, VolumeX } from "lucide-react"
import Link from "next/link"

const STORIES = [
  {
    id: "1",
    title: "The Little Seed",
    category: "Nature",
    color: "from-green-400 to-emerald-500",
    emoji: "🌱",
    pages: [
      { text: "Once upon a time, there was a tiny seed.", illustration: "🌱" },
      { text: "The seed was planted in warm, soft soil.", illustration: "🌍" },
      { text: "Every day, the sun gave it light and the rain gave it water.", illustration: "☀️🌧️" },
      { text: "Slowly, the seed grew roots and a little green shoot.", illustration: "🌿" },
      { text: "One day, the seed bloomed into a beautiful flower!", illustration: "🌸" },
      { text: "The flower was happy and all the butterflies came to visit.", illustration: "🦋" },
      { text: "The End! You are like the little seed - growing every day!", illustration: "🌟" },
    ],
  },
  {
    id: "2",
    title: "Bouncy the Bunny",
    category: "Animals",
    color: "from-pink-400 to-rose-500",
    emoji: "🐰",
    pages: [
      { text: "Bouncy was a little rabbit with soft white fur.", illustration: "🐰" },
      { text: "Bouncy loved to hop around the garden.", illustration: "🌻" },
      { text: "One day, Bouncy found a shiny red apple.", illustration: "🍎" },
      { text: "Bouncy shared the apple with all friends.", illustration: "🐿️" },
      { text: "They all ate together and were very happy.", illustration: "😊" },
      { text: "The End! Sharing makes everything more fun!", illustration: "💛" },
    ],
  },
  {
    id: "3",
    title: "Colors Everywhere",
    category: "Learning",
    color: "from-blue-400 to-indigo-500",
    emoji: "🎨",
    pages: [
      { text: "Look around you - the world is full of colors!", illustration: "🌈" },
      { text: "The sky is blue like the ocean.", illustration: "🔵" },
      { text: "The grass is green like a fresh leaf.", illustration: "🟢" },
      { text: "The sun is yellow like a happy face.", illustration: "🟡" },
      { text: "Flowers are red like a warm heart.", illustration: "🔴" },
      { text: "What is your favorite color? Every color is beautiful!", illustration: "🎨" },
    ],
  },
  {
    id: "4",
    title: "Counting Stars",
    category: "Numbers",
    color: "from-purple-400 to-violet-500",
    emoji: "⭐",
    pages: [
      { text: "At night, look up at the sky!", illustration: "🌙" },
      { text: "One little star twinkles bright.", illustration: "⭐" },
      { text: "Two little stars dance together.", illustration: "⭐⭐" },
      { text: "Three little stars play hide and seek.", illustration: "⭐⭐⭐" },
      { text: "Four little stars sing a lullaby.", illustration: "⭐⭐⭐⭐" },
      { text: "Five little stars say goodnight!", illustration: "⭐⭐⭐⭐⭐" },
      { text: "The End! Can you count to five on your fingers?", illustration: "✋" },
    ],
  },
  {
    id: "5",
    title: "My Happy Body",
    category: "Self",
    color: "from-amber-400 to-orange-500",
    emoji: "😊",
    pages: [
      { text: "I have two eyes to see the world.", illustration: "👀" },
      { text: "I have two ears to hear birds singing.", illustration: "👂" },
      { text: "I have a nose to smell flowers.", illustration: "👃" },
      { text: "I have a mouth to smile and eat yummy food.", illustration: "😊" },
      { text: "I have hands to clap and wave hello!", illustration: "👋" },
      { text: "I have feet to run and jump!", illustration: "🦶" },
      { text: "My body is amazing - take good care of it!", illustration: "💪" },
    ],
  },
]

export default function StoriesPage() {
  const [selectedStory, setSelectedStory] = useState<typeof STORIES[0] | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [muted, setMuted] = useState(true)

  function openStory(story: typeof STORIES[0]) {
    setSelectedStory(story)
    setCurrentPage(0)
  }

  function nextPage() {
    if (selectedStory && currentPage < selectedStory.pages.length - 1) {
      setCurrentPage(currentPage + 1)
    } else {
      setSelectedStory(null)
    }
  }

  function prevPage() {
    if (currentPage > 0) {
      setCurrentPage(currentPage - 1)
    }
  }

  if (selectedStory) {
    const page = selectedStory.pages[currentPage]
    return (
      <div className="fixed inset-0 z-50 flex flex-col bg-gradient-to-b from-indigo-900 to-purple-900">
        <div className="flex items-center justify-between p-4">
          <button
            onClick={() => setSelectedStory(null)}
            className="flex size-10 items-center justify-center rounded-full bg-white/20 text-white"
          >
            ✕
          </button>
          <p className="text-sm text-white/70">
            Page {currentPage + 1} of {selectedStory.pages.length}
          </p>
          <button
            onClick={() => setMuted(!muted)}
            className="flex size-10 items-center justify-center rounded-full bg-white/20 text-white"
          >
            {muted ? <VolumeX className="size-5" /> : <Volume2 className="size-5" />}
          </button>
        </div>

        <div className="flex flex-1 flex-col items-center justify-center px-6">
          <div className="mb-6 text-8xl">{page.illustration}</div>
          <p className="max-w-md text-center text-xl font-medium leading-relaxed text-white">
            {page.text}
          </p>
        </div>

        <div className="flex items-center justify-between p-6">
          <button
            onClick={prevPage}
            disabled={currentPage === 0}
            className="rounded-full bg-white/20 px-6 py-3 text-sm font-bold text-white disabled:opacity-40"
          >
            Back
          </button>
          <div className="flex gap-1.5">
            {selectedStory.pages.map((_, i) => (
              <div
                key={i}
                className={`size-2 rounded-full ${i === currentPage ? "bg-white" : "bg-white/30"}`}
              />
            ))}
          </div>
          <button
            onClick={nextPage}
            className="rounded-full bg-white px-6 py-3 text-sm font-bold text-purple-900"
          >
            {currentPage === selectedStory.pages.length - 1 ? "Done!" : "Next"}
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">Stories</h1>
          <p className="text-sm text-gray-500">Read magical tales together</p>
        </div>
      </div>

      <div className="space-y-3">
        {STORIES.map((story) => (
          <button
            key={story.id}
            onClick={() => openStory(story)}
            className="nursery-card flex w-full items-center gap-4 rounded-2xl bg-white p-4 text-left transition-all hover:shadow-lg"
          >
            <div className={`flex size-16 items-center justify-center rounded-2xl bg-gradient-to-br ${story.color} text-3xl`}>
              {story.emoji}
            </div>
            <div className="flex-1">
              <p className="font-bold text-gray-800">{story.title}</p>
              <p className="mt-0.5 text-xs text-gray-500">{story.category} • {story.pages.length} pages</p>
            </div>
            <BookOpen className="size-5 text-gray-400" />
          </button>
        ))}
      </div>
    </div>
  )
}
