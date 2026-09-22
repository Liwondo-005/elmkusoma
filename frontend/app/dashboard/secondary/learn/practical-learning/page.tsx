"use client"

import { useState } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { ArrowLeft, FlaskConical, Wrench, Cpu, Lightbulb, ChevronRight } from "lucide-react"

const experiments = [
  {
    id: "exp-1",
    title: "Acid-Base Titration",
    subject: "Chemistry",
    objective: "Determine the concentration of an unknown acid using a standard base solution.",
    materials: ["Burette", "Pipette", "Conical flask", "Phenolphthalein indicator", "NaOH solution", "HCl solution"],
    steps: [
      "Rinse the burette with NaOH solution and fill it up to the zero mark.",
      "Pipette 25 mL of HCl into a conical flask.",
      "Add 2-3 drops of phenolphthalein indicator.",
      "Slowly add NaOH from the burette while swirling the flask.",
      "Record the volume of NaOH when the solution turns pink.",
      "Repeat three times and calculate the average.",
    ],
    status: "available" as const,
  },
  {
    id: "exp-2",
    title: "Simple Electric Circuit",
    subject: "Physics",
    objective: "Build a working circuit and measure voltage, current, and resistance.",
    materials: ["Battery", "Wires", "Bulb", "Switch", "Ammeter", "Voltmeter"],
    steps: [
      "Connect the battery, switch, and bulb in series using wires.",
      "Close the switch and observe the bulb lighting up.",
      "Connect the ammeter in series to measure current.",
      "Connect the voltmeter in parallel across the bulb.",
      "Record readings and verify Ohm's Law: V = IR.",
      "Test with different batteries and compare results.",
    ],
    status: "available" as const,
  },
  {
    id: "exp-3",
    title: "Soil pH Testing",
    subject: "Biology",
    objective: "Test the pH of different soil samples and determine suitability for various crops.",
    materials: ["Soil samples", "Distilled water", "pH meter or litmus paper", "Beakers", "Stirring rod"],
    steps: [
      "Collect soil samples from different locations.",
      "Mix 10g of soil with 50mL of distilled water in a beaker.",
      "Stir well and let it settle for 30 minutes.",
      "Test the pH using a pH meter or litmus paper.",
      "Record results and classify soil as acidic, neutral, or alkaline.",
      "Research which crops grow best in each soil type.",
    ],
    status: "available" as const,
  },
]

export default function PracticalLearningPage() {
  const t = useTranslations("secondary")
  const tc = useTranslations("common")
  const [selectedExperiment, setSelectedExperiment] = useState<string | null>(null)
  const activeExperiment = experiments.find(e => e.id === selectedExperiment)

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary/learn" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("secondary.backToLearn")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{t("secondary.practicalLearning")}</h1>
          <p className="text-sm text-gray-500">{t("secondary.handsOnExperimentsAndLabWork")}</p>
        </div>
      </div>

      {selectedExperiment && activeExperiment ? (
        <div className="space-y-4">
          <button
            onClick={() => setSelectedExperiment(null)}
            className="text-sm font-semibold text-indigo-600 hover:text-indigo-700"
            aria-label={t("secondary.backToExperiments")}
          >
            {t("secondary.backToExperiments")}
          </button>
          <div className="rounded-2xl border border-gray-100 bg-white p-6">
            <div className="flex items-center gap-2">
              <FlaskConical className="size-5 text-indigo-600" />
              <span className="text-xs font-semibold uppercase tracking-wider text-indigo-600">{activeExperiment.subject}</span>
            </div>
            <h2 className="mt-2 text-lg font-bold text-gray-900">{activeExperiment.title}</h2>
            <p className="mt-1 text-sm text-gray-600">{activeExperiment.objective}</p>

            <div className="mt-4">
              <h3 className="text-sm font-semibold text-gray-900">{t("secondary.materialsNeeded")}</h3>
              <ul className="mt-2 space-y-1">
                {activeExperiment.materials.map((m, i) => (
                  <li key={i} className="flex items-center gap-2 text-sm text-gray-600">
                    <span className="size-1.5 shrink-0 rounded-full bg-indigo-400" />
                    {m}
                  </li>
                ))}
              </ul>
            </div>

            <div className="mt-4">
              <h3 className="text-sm font-semibold text-gray-900">{t("secondary.procedure")}</h3>
              <ol className="mt-2 space-y-3">
                {activeExperiment.steps.map((step, i) => (
                  <li key={i} className="flex items-start gap-3">
                    <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-xs font-bold text-indigo-600">
                      {i + 1}
                    </span>
                    <p className="text-sm text-gray-600 pt-0.5">{step}</p>
                  </li>
                ))}
              </ol>
            </div>

            <div className="mt-6 rounded-xl bg-amber-50 p-4">
              <p className="text-sm font-medium text-amber-800">
                {t("secondary.safetyNote")}
              </p>
            </div>
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          <div className="rounded-2xl border border-gray-100 bg-white p-6 text-center">
            <FlaskConical className="mx-auto size-12 text-indigo-300" />
            <h2 className="mt-3 text-lg font-bold text-gray-800">{t("secondary.labExperiments")}</h2>
            <p className="mt-1 text-sm text-gray-500">{t("secondary.chooseExperimentToBegin")}</p>
          </div>
          {experiments.map(exp => (
            <button
              key={exp.id}
              onClick={() => setSelectedExperiment(exp.id)}
              className="flex w-full items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 text-left transition-all hover:border-indigo-200 hover:shadow-sm"
              aria-label={`${exp.title} - ${exp.subject} ${exp.materials.length} ${t("secondary.materials")}`}
            >
              <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-indigo-50">
                <FlaskConical className="size-6 text-indigo-600" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="font-semibold text-gray-900">{exp.title}</p>
                <p className="text-xs text-gray-400">{exp.subject} · {exp.materials.length} {t("secondary.materials")}</p>
              </div>
              <ChevronRight className="size-5 shrink-0 text-gray-300" />
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
