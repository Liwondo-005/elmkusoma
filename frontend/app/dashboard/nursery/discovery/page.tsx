"use client"

import { useState } from "react"
import { ArrowLeft, Star, Sun, CloudRain, TreePine, Dog, Cat, Fish, Bird, Bug, Flower2 } from "lucide-react"
import Link from "next/link"

const DISCOVERY_CARDS = [
  {
    id: "1",
    title: "The Sun",
    category: "Nature",
    icon: Sun,
    color: "from-yellow-400 to-orange-500",
    emoji: "☀️",
    facts: [
      "The sun is a big ball of hot gas",
      "It gives us light so we can see",
      "It keeps us warm",
      "The sun is very far away",
    ],
    funFact: "The sun is so big that 1 million Earths could fit inside it!",
  },
  {
    id: "2",
    title: "Rain",
    category: "Weather",
    icon: CloudRain,
    color: "from-blue-400 to-cyan-500",
    emoji: "🌧️",
    facts: [
      "Rain is water falling from clouds",
      "Plants need rain to grow",
      "We can splash in puddles when it rains",
      "Rainbows appear after rain",
    ],
    funFact: "A raindrop falls at about 14 miles per hour!",
  },
  {
    id: "3",
    title: "Trees",
    category: "Nature",
    icon: TreePine,
    color: "from-green-400 to-emerald-500",
    emoji: "🌳",
    facts: [
      "Trees give us oxygen to breathe",
      "Birds live in trees",
      "Trees have trunks, branches, and leaves",
      "Some trees give us fruit",
    ],
    funFact: "The oldest tree in the world is over 5,000 years old!",
  },
  {
    id: "4",
    title: "Dogs",
    category: "Animals",
    icon: Dog,
    color: "from-amber-400 to-brown-500",
    emoji: "🐕",
    facts: [
      "Dogs are our best friends",
      "They can learn tricks",
      "Dogs bark to talk to us",
      "They wag their tails when happy",
    ],
    funFact: "Dogs can smell 10,000 times better than humans!",
  },
  {
    id: "5",
    title: "Cats",
    category: "Animals",
    icon: Cat,
    color: "from-purple-400 to-pink-500",
    emoji: "🐱",
    facts: [
      "Cats love to nap",
      "They purr when they are happy",
      "Cats are very graceful",
      "They can see in the dark",
    ],
    funFact: "Cats spend 70% of their lives sleeping!",
  },
  {
    id: "6",
    title: "Fish",
    category: "Animals",
    icon: Fish,
    color: "from-cyan-400 to-blue-500",
    emoji: "🐟",
    facts: [
      "Fish live in water",
      "They breathe through gills",
      "Fish can swim very fast",
      "Some fish are very colorful",
    ],
    funFact: "Clownfish and sea anemones are best friends!",
  },
  {
    id: "7",
    title: "Birds",
    category: "Animals",
    icon: Bird,
    color: "from-sky-400 to-indigo-500",
    emoji: "🐦",
    facts: [
      "Birds can fly in the sky",
      "They build nests in trees",
      "Birds sing beautiful songs",
      "Feathers keep them warm",
    ],
    funFact: "Hummingbirds can fly backwards!",
  },
  {
    id: "8",
    title: "Bugs",
    category: "Animals",
    icon: Bug,
    color: "from-lime-400 to-green-500",
    emoji: "🐛",
    facts: [
      "Bugs are tiny creatures",
      "Bees make honey for us",
      "Butterflies have colorful wings",
      "Ants work together as a team",
    ],
    funFact: "A caterpillar turns into a butterfly!",
  },
  {
    id: "9",
    title: "Flowers",
    category: "Nature",
    icon: Flower2,
    color: "from-pink-400 to-rose-500",
    emoji: "🌸",
    facts: [
      "Flowers are beautiful and colorful",
      "Bees visit flowers for nectar",
      "Flowers make seeds to grow new plants",
      "Some flowers smell wonderful",
    ],
    funFact: "The sunflower always faces the sun!",
  },
]

export default function DiscoveryPage() {
  const [selectedCard, setSelectedCard] = useState<typeof DISCOVERY_CARDS[0] | null>(null)
  const [currentFact, setCurrentFact] = useState(0)

  function openCard(card: typeof DISCOVERY_CARDS[0]) {
    setSelectedCard(card)
    setCurrentFact(0)
  }

  function nextFact() {
    if (selectedCard && currentFact < selectedCard.facts.length - 1) {
      setCurrentFact(currentFact + 1)
    } else {
      setSelectedCard(null)
    }
  }

  if (selectedCard) {
    return (
      <div className="fixed inset-0 z-50 flex flex-col bg-gradient-to-b from-indigo-900 to-purple-900">
        <div className="flex items-center justify-between p-4">
          <button
            onClick={() => setSelectedCard(null)}
            className="flex size-10 items-center justify-center rounded-full bg-white/20 text-white"
          >
            ✕
          </button>
          <p className="text-sm text-white/70">
            Fact {currentFact + 1} of {selectedCard.facts.length}
          </p>
          <div />
        </div>

        <div className="flex flex-1 flex-col items-center justify-center px-6">
          <div className="mb-4 text-8xl">{selectedCard.emoji}</div>
          <h2 className="mb-2 text-2xl font-bold text-white">{selectedCard.title}</h2>
          <div className="mb-6 rounded-2xl bg-white/10 px-4 py-2">
            <p className="text-sm text-white/70">{selectedCard.category}</p>
          </div>
          <p className="max-w-md text-center text-xl leading-relaxed text-white">
            {selectedCard.facts[currentFact]}
          </p>
          {currentFact === selectedCard.facts.length - 1 && (
            <div className="mt-6 rounded-2xl bg-yellow-500/20 p-4 text-center">
              <p className="text-xs font-bold text-yellow-300">Fun Fact!</p>
              <p className="mt-1 text-sm text-yellow-200">{selectedCard.funFact}</p>
            </div>
          )}
        </div>

        <div className="flex items-center justify-between p-6">
          <button
            onClick={() => setCurrentFact(Math.max(0, currentFact - 1))}
            disabled={currentFact === 0}
            className="rounded-full bg-white/20 px-6 py-3 text-sm font-bold text-white disabled:opacity-40"
          >
            Back
          </button>
          <div className="flex gap-1.5">
            {selectedCard.facts.map((_, i) => (
              <div
                key={i}
                className={`size-2 rounded-full ${i === currentFact ? "bg-white" : "bg-white/30"}`}
              />
            ))}
          </div>
          <button
            onClick={nextFact}
            className="rounded-full bg-white px-6 py-3 text-sm font-bold text-purple-900"
          >
            {currentFact === selectedCard.facts.length - 1 ? "Done!" : "Next"}
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
          <h1 className="text-xl font-bold text-gray-800">Discovery</h1>
          <p className="text-sm text-gray-500">Explore the amazing world around you</p>
        </div>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {DISCOVERY_CARDS.map((card) => (
          <button
            key={card.id}
            onClick={() => openCard(card)}
            className="nursery-card flex flex-col items-center gap-3 rounded-2xl bg-white p-5 text-center transition-all hover:shadow-lg hover:scale-105"
          >
            <div className={`flex size-16 items-center justify-center rounded-2xl bg-gradient-to-br ${card.color} text-3xl`}>
              {card.emoji}
            </div>
            <div>
              <p className="font-bold text-gray-800">{card.title}</p>
              <p className="mt-0.5 text-xs text-gray-500">{card.category}</p>
            </div>
            <div className="flex gap-0.5">
              {card.facts.map((_, i) => (
                <Star key={i} className="size-2 text-yellow-400" />
              ))}
            </div>
          </button>
        ))}
      </div>
    </div>
  )
}
