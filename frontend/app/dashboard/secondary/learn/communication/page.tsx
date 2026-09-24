"use client"

import { useState } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { ArrowLeft, MessageSquare, BookOpen, Mic, PenTool, ChevronRight, CheckCircle } from "lucide-react"

const activities = [
  {
    id: "comm-1",
    title: "Debate Prep: Should Homework Be Banned?",
    type: "debate" as const,
    icon: MessageSquare,
    description: "Prepare arguments for and against banning homework in secondary schools.",
    sections: [
      {
        heading: "Arguments FOR banning homework",
        points: [
          "Reduces stress and burnout from overwork",
          "Allows more time for family, hobbies, and rest",
          "Research shows diminishing returns of excessive homework",
          "Promotes equal opportunity (not all students have quiet study spaces)",
        ],
      },
      {
        heading: "Arguments AGAINST banning homework",
        points: [
          "Reinforces learning through practice and repetition",
          "Develops self-discipline and time management",
          "Prepares students for higher education demands",
          "Helps teachers assess individual understanding",
        ],
      },
    ],
    tip: "Structure your debate: Introduction → Your strongest argument → Counter the opposition → Closing statement.",
  },
  {
    id: "comm-2",
    title: "Essay Writing: The Five-Paragraph Structure",
    type: "writing" as const,
    icon: PenTool,
    description: "Master the classic essay structure used in examinations and academic writing.",
    sections: [
      {
        heading: "Paragraph 1: Introduction",
        points: [
          "Hook the reader with a question, fact, or quote",
          "Provide background context on the topic",
          "End with a clear thesis statement",
        ],
      },
      {
        heading: "Paragraphs 2-4: Body",
        points: [
          "Each paragraph covers ONE main idea",
          "Start with a topic sentence",
          "Provide evidence, examples, or data",
          "Explain how it supports your thesis",
          "Use transition words between paragraphs",
        ],
      },
      {
        heading: "Paragraph 5: Conclusion",
        points: [
          "Restate the thesis in different words",
          "Summarize the main points",
          "End with a thought-provoking statement or call to action",
        ],
      },
    ],
    tip: "Before writing, brainstorm and outline your ideas. A clear plan makes writing faster and more organized.",
  },
  {
    id: "comm-3",
    title: "Public Speaking: Presenting with Confidence",
    type: "speaking" as const,
    icon: Mic,
    description: "Learn techniques for effective oral presentations and speeches.",
    sections: [
      {
        heading: "Before the Presentation",
        points: [
          "Know your audience and their expectations",
          "Practice multiple times, ideally in front of someone",
          "Prepare visual aids (slides, props) if appropriate",
          "Plan your opening and closing lines carefully",
        ],
      },
      {
        heading: "During the Presentation",
        points: [
          "Make eye contact with different parts of the audience",
          "Speak clearly and vary your tone to maintain interest",
          "Use gestures naturally — avoid standing stiffly",
          "Pause briefly after important points for emphasis",
        ],
      },
      {
        heading: "Handling Questions",
        points: [
          "Listen carefully to the full question before answering",
          "It's OK to say 'That's a great question, let me think about that'",
          "Keep answers concise — don't ramble",
          "Admit when you don't know something",
        ],
      },
    ],
    tip: "Nervousness is normal! Channel it into energy. Take deep breaths before starting, and remember: the audience wants you to succeed.",
  },
]

export default function CommunicationPage() {
  const t = useTranslations("secondary")
  const tc = useTranslations("common")
  const [selectedActivity, setSelectedActivity] = useState<string | null>(null)
  const [completedActivities, setCompletedActivities] = useState<Set<string>>(new Set())
  const activeActivity = activities.find(a => a.id === selectedActivity)

  function markComplete(id: string) {
    setCompletedActivities(prev => new Set([...prev, id]))
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary/learn" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToLearn")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{t("communication")}</h1>
          <p className="text-sm text-gray-500">{t("writingSpeakingAndArgumentationSkills")}</p>
        </div>
      </div>

      {completedActivities.size > 0 && (
        <div className="rounded-xl bg-green-50 p-3 text-center">
          <p className="text-sm font-medium text-green-700">
            {completedActivities.size} {tc("of")} {activities.length} {t("activitiesCompleted")}
          </p>
        </div>
      )}

      {selectedActivity && activeActivity ? (
        <div className="space-y-4">
          <button onClick={() => setSelectedActivity(null)} className="text-sm font-semibold text-indigo-600 hover:text-indigo-700" aria-label={t("backToActivities")}>
            {t("backToActivities")}
          </button>
          <div className="rounded-2xl border border-gray-100 bg-white p-6">
            <div className="flex items-center gap-2">
              <activeActivity.icon className="size-5 text-indigo-600" />
              <span className="text-xs font-semibold uppercase tracking-wider text-indigo-600">{activeActivity.type}</span>
            </div>
            <h2 className="mt-2 text-lg font-bold text-gray-900">{activeActivity.title}</h2>
            <p className="mt-1 text-sm text-gray-600">{activeActivity.description}</p>

            <div className="mt-6 space-y-6">
              {activeActivity.sections.map((section, si) => (
                <div key={si}>
                  <h3 className="text-sm font-semibold text-gray-900">{section.heading}</h3>
                  <ul className="mt-2 space-y-2">
                    {section.points.map((point, pi) => (
                      <li key={pi} className="flex items-start gap-2 text-sm text-gray-600">
                        <span className="mt-1.5 size-1.5 shrink-0 rounded-full bg-indigo-400" />
                        {point}
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>

            <div className="mt-6 rounded-xl bg-indigo-50 p-4">
              <p className="text-sm font-medium text-indigo-800">{t("proTip")}</p>
              <p className="mt-1 text-sm text-indigo-700">{activeActivity.tip}</p>
            </div>

            {!completedActivities.has(activeActivity.id) && (
              <button
                onClick={() => markComplete(activeActivity.id)}
                className="mt-4 w-full rounded-xl bg-indigo-600 py-3 text-sm font-semibold text-white transition-colors hover:bg-indigo-700"
                aria-label={t("markAsCompleted")}
              >
                {t("markAsCompleted")}
              </button>
            )}
            {completedActivities.has(activeActivity.id) && (
              <div className="mt-4 flex items-center justify-center gap-2 rounded-xl bg-green-50 py-3">
                <CheckCircle className="size-4 text-green-600" />
                <span className="text-sm font-medium text-green-700">{tc("completed")}</span>
              </div>
            )}
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          {activities.map(activity => (
            <button
              key={activity.id}
              onClick={() => setSelectedActivity(activity.id)}
              className="flex w-full items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 text-left transition-all hover:border-indigo-200 hover:shadow-sm"
              aria-label={`${activity.title} - ${activity.type} ${activity.sections.length} ${t("sections")}`}
            >
              <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-blue-50">
                <activity.icon className="size-6 text-blue-600" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="font-semibold text-gray-900">{activity.title}</p>
                <p className="text-xs text-gray-400">{activity.type} · {activity.sections.length} {t("sections")}</p>
              </div>
              {completedActivities.has(activity.id) ? (
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
