"use client"

import { ArrowLeft, Globe, MapPin, Mountain, TreePine, PawPrint } from "lucide-react"
import Link from "next/link"

const DISCOVERIES = [
  { name: "Mount Kilimanjaro", desc: "The tallest mountain in Africa, in Tanzania", emoji: "🏔️", facts: ["5,895 meters tall", "Has three volcanic cones", "Snow caps near the equator"] },
  { name: "Serengeti", desc: "Famous for great wildebeest migration", emoji: "🦁", facts: ["Home to the Big Five", "Over 1.5 million wildebeest", "UNESCO World Heritage Site"] },
  { name: "Zanzibar", desc: "Beautiful island with white sand beaches", emoji: "🏖️", facts: ["Known as the Spice Island", "Famous for cloves and spices", "Historic Stone Town"] },
  { name: "Ngorongoro Crater", desc: "World's largest inactive volcanic crater", emoji: "🌋", facts: ["250 km² wide", "Home to 30,000 animals", "One of the Seven Natural Wonders of Africa"] },
  { name: "Lake Victoria", desc: "Largest lake in Africa", emoji: "🌊", facts: ["Shared by Tanzania, Kenya, Uganda", "Source of the Nile River", "Home to many fish species"] },
  { name: "Baobab Trees", desc: "Ancient trees found across Tanzania", emoji: "🌳", facts: ["Can live over 1,000 years", "Store water in their trunks", "Called the Tree of Life"] },
]

export default function TanzaniaDiscoveryPage() {
  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">Tanzania Discovery</h1>
          <p className="text-sm text-gray-500">Explore our beautiful country</p>
        </div>
      </div>

      <div className="nursery-card rounded-2xl bg-gradient-to-r from-green-500 to-emerald-400 p-5 text-white">
        <div className="flex items-center gap-3">
          <Globe className="size-8" />
          <div>
            <h2 className="text-lg font-bold">Jambo! Welcome to Tanzania</h2>
            <p className="text-sm text-white/70">Discover the wonders of our homeland</p>
          </div>
        </div>
      </div>

      <div className="space-y-3">
        {DISCOVERIES.map(d => (
          <div key={d.name} className="nursery-card rounded-2xl bg-white p-5">
            <div className="flex items-center gap-3">
              <span className="text-3xl">{d.emoji}</span>
              <div>
                <p className="font-bold text-gray-800">{d.name}</p>
                <p className="text-xs text-gray-500">{d.desc}</p>
              </div>
            </div>
            <div className="mt-3 space-y-1">
              {d.facts.map((fact, i) => (
                <div key={i} className="flex items-center gap-2 text-sm text-gray-600">
                  <span className="size-1.5 rounded-full bg-green-400" />
                  {fact}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
