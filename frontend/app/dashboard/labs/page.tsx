"use client"

import { useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { type LearningLevel } from "@/lib/learner-config"
import { FlaskConical, Leaf, Droplets, Zap, Bug, ArrowLeft, Beaker, CheckCircle } from "lucide-react"

const categories = [
  { id: "nature", name: "Nature & Environment", icon: Leaf, color: "text-green-600", bg: "bg-green-50", border: "border-green-200" },
  { id: "materials", name: "Materials & Properties", icon: Droplets, color: "text-blue-600", bg: "bg-blue-50", border: "border-blue-200" },
  { id: "forces", name: "Forces & Motion", icon: Zap, color: "text-orange-600", bg: "bg-orange-50", border: "border-orange-200" },
  { id: "living", name: "Living Things", icon: Bug, color: "text-teal-600", bg: "bg-teal-50", border: "border-teal-200" },
]

const experiments = [
  { id: "1", title: "Why do plants need sunlight?", category: "nature", hypothesis: "Plants need sunlight to make food through photosynthesis.", materials: ["A small plant", "A cardboard box", "A window with sunlight"], steps: ["Place the plant near a sunny window for 3 days", "Cover the plant with the cardboard box", "Wait 3 days and observe the differences", "Compare the leaves of both conditions"], result: "The plant without sunlight becomes pale and weak, while the sunlit plant stays green and healthy." },
  { id: "2", title: "What makes ice melt?", category: "materials", hypothesis: "Heat makes ice melt faster than room temperature.", materials: ["Ice cubes", "A plate", "A hair dryer (with adult help)", "Salt"], steps: ["Place 3 ice cubes on the plate", "Leave one at room temperature", "Pour salt on one, use hair dryer on another", "Record which melts first"], result: "The hair dryer melts ice fastest, then salt, then room temperature." },
  { id: "3", title: "How do magnets work?", category: "forces", hypothesis: "Magnets attract certain metals like iron and steel.", materials: ["A magnet", "Various objects (paperclip, coin, eraser, spoon)", "A paper"], steps: ["Test each object with the magnet", "Record which objects stick to the magnet", "Sort objects into magnetic and non-magnetic", "Draw your findings"], result: "Iron and steel objects stick to the magnet. Plastic, wood, and rubber do not." },
  { id: "4", title: "Where do birds sleep?", category: "living", hypothesis: "Birds sleep in trees and nests at night.", materials: ["Binoculars (if available)", "A notebook", "A pencil", "A cozy spot to observe"], steps: ["Find a quiet place with trees nearby", "Observe in the early morning or evening", "Note where birds go as it gets dark", "Draw the sleeping spots you find"], result: "Many birds sleep in tree branches, some in nests, and ground birds hide in bushes." },
  { id: "5", title: "What happens when you mix colors?", category: "materials", hypothesis: "Mixing two primary colors creates a new color.", materials: ["Red, blue, and yellow paint", "White paper", "Paint brushes", "Water cup"], steps: ["Put a small amount of red and blue paint on paper", "Mix them together slowly", "Repeat with red and yellow, then blue and yellow", "Record the new colors you created"], result: "Red + blue = purple, red + yellow = orange, blue + yellow = green." },
  { id: "6", title: "How fast does sound travel?", category: "forces", hypothesis: "Sound travels faster through solids than air.", materials: ["A metal spoon", "A long table", "A friend to help", "A ruler"], steps: ["Place your ear on one end of the table", "Have your friend tap the spoon at the other end", "Then tap the spoon in the air near your ear", "Compare which you hear first"], result: "Sound travels much faster through the solid table than through the air." },
]

export default function LabsPage() {
  const { user } = useRequireAuth()
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null)
  const [selectedExperiment, setSelectedExperiment] = useState<typeof experiments[0] | null>(null)
  const [attempted, setAttempted] = useState<Set<string>>(new Set())
  const [notes, setNotes] = useState<Record<string, string>>({})
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  const filteredExperiments = selectedCategory
    ? experiments.filter((e) => e.category === selectedCategory)
    : experiments

  const categoryCount = (catId: string) => experiments.filter((e) => e.category === catId).length

  if (!isPrimary) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-2xl bg-teal-500/10">
            <FlaskConical className="size-5 text-teal-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">ELMKUSOMA Labs</h1>
            <p className="text-sm text-muted-foreground">Explore, experiment, and discover!</p>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <FlaskConical className="size-12 text-muted-foreground/30" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">Labs is for Primary learners</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Switch to a primary learner account to start experimenting.
          </p>
        </div>
      </div>
    )
  }

  if (selectedExperiment) {
    const cat = categories.find((c) => c.id === selectedExperiment.category)
    const CatIcon = cat?.icon || FlaskConical
    return (
      <div className="mx-auto max-w-4xl space-y-6">
        <button
          onClick={() => setSelectedExperiment(null)}
          className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
        >
          <ArrowLeft className="size-4" />
          Back to Labs
        </button>

        <div className="rounded-2xl border border-border bg-card p-8 shadow-xs">
          <div className="flex items-center gap-3">
            <div className={`flex size-12 items-center justify-center rounded-2xl ${cat?.bg || "bg-muted"}`}>
              <CatIcon className={`size-6 ${cat?.color || "text-muted-foreground"}`} />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-foreground">{selectedExperiment.title}</h1>
              {cat && <p className="text-sm text-muted-foreground">{cat.name}</p>}
            </div>
          </div>

          <div className="mt-6 space-y-6">
            <div className="rounded-xl bg-amber-50 border border-amber-200 p-4">
              <h3 className="text-sm font-bold text-amber-800">Hypothesis</h3>
              <p className="mt-1 text-sm text-amber-700">{selectedExperiment.hypothesis}</p>
            </div>

            <div>
              <h3 className="text-sm font-bold text-foreground flex items-center gap-2">
                <Beaker className="size-4" /> Materials Needed
              </h3>
              <ul className="mt-2 space-y-1">
                {selectedExperiment.materials.map((m, i) => (
                  <li key={i} className="flex items-center gap-2 text-sm text-muted-foreground">
                    <span className="size-1.5 rounded-full bg-primary" />
                    {m}
                  </li>
                ))}
              </ul>
            </div>

            <div>
              <h3 className="text-sm font-bold text-foreground">Steps</h3>
              <ol className="mt-2 space-y-2">
                {selectedExperiment.steps.map((s, i) => (
                  <li key={i} className="flex gap-3 text-sm text-muted-foreground">
                    <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">
                      {i + 1}
                    </span>
                    <span className="pt-0.5">{s}</span>
                  </li>
                ))}
              </ol>
            </div>

            <div className="rounded-xl bg-green-50 border border-green-200 p-4">
              <h3 className="text-sm font-bold text-green-800">Expected Result</h3>
              <p className="mt-1 text-sm text-green-700">{selectedExperiment.result}</p>
            </div>

            <div className="flex items-center gap-4">
              <button
                onClick={() => setAttempted((prev) => new Set(prev).add(selectedExperiment.id))}
                className={`inline-flex items-center gap-2 rounded-lg px-5 py-2.5 text-sm font-medium transition-colors ${
                  attempted.has(selectedExperiment.id)
                    ? "bg-green-100 text-green-700"
                    : "bg-primary text-primary-foreground hover:bg-primary/90"
                }`}
              >
                {attempted.has(selectedExperiment.id) ? (
                  <><CheckCircle className="size-4" /> Attempted</>
                ) : (
                  "Try It!"
                )}
              </button>
            </div>

            <div>
              <label className="text-sm font-bold text-foreground">What I Learned</label>
              <textarea
                value={notes[selectedExperiment.id] || ""}
                onChange={(e) => setNotes((prev) => ({ ...prev, [selectedExperiment.id]: e.target.value }))}
                placeholder="Write down what you discovered..."
                rows={4}
                className="mt-2 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
              />
            </div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-teal-500/5 via-card to-green-500/5 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-teal-500/10">
            <FlaskConical className="size-6 text-teal-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">ELMKUSOMA Labs</h1>
            <p className="text-sm text-muted-foreground">Explore, experiment, and discover!</p>
          </div>
        </div>
      </div>

      {selectedCategory && (
        <button
          onClick={() => setSelectedCategory(null)}
          className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
        >
          <ArrowLeft className="size-4" />
          All Categories
        </button>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {categories.map((cat) => {
          const Icon = cat.icon
          return (
            <button
              key={cat.id}
              onClick={() => setSelectedCategory(selectedCategory === cat.id ? null : cat.id)}
              className={`rounded-2xl border p-5 text-left transition-all hover:shadow-md ${
                selectedCategory === cat.id
                  ? `${cat.border} ${cat.bg} ring-2 ring-primary/20`
                  : "border-border bg-card hover:border-primary/30"
              }`}
            >
              <div className={`flex size-10 items-center justify-center rounded-xl ${cat.bg}`}>
                <Icon className={`size-5 ${cat.color}`} />
              </div>
              <h3 className="mt-3 text-sm font-bold text-foreground">{cat.name}</h3>
              <p className="mt-1 text-xs text-muted-foreground">{categoryCount(cat.id)} experiments</p>
            </button>
          )
        })}
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {filteredExperiments.map((exp) => {
          const cat = categories.find((c) => c.id === exp.category)
          const CatIcon = cat?.icon || FlaskConical
          return (
            <button
              key={exp.id}
              onClick={() => setSelectedExperiment(exp)}
              className="rounded-2xl border border-border bg-card p-5 text-left shadow-xs transition-all hover:shadow-md hover:border-primary/30"
            >
              <div className="flex items-start gap-3">
                <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${cat?.bg || "bg-muted"}`}>
                  <CatIcon className={`size-5 ${cat?.color || "text-muted-foreground"}`} />
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-sm font-bold text-foreground line-clamp-2">{exp.title}</h3>
                  <p className="mt-1 text-xs text-muted-foreground">{cat?.name}</p>
                </div>
              </div>
              <div className="mt-3 flex items-center gap-2">
                {attempted.has(exp.id) && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-medium text-green-700">
                    <CheckCircle className="size-3" /> Attempted
                  </span>
                )}
              </div>
            </button>
          )
        })}
      </div>
    </div>
  )
}
