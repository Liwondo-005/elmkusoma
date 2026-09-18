"use client"

import { useState } from "react"
import { ArrowLeft, Calendar, Plus, Clock, BookOpen, Trash2 } from "lucide-react"
import Link from "next/link"

interface StudySession {
  id: string
  subject: string
  topic: string
  date: string
  time: string
  duration: number
  status: "planned" | "done"
}

export default function StudyPlannerPage() {
  const [sessions, setSessions] = useState<StudySession[]>([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ subject: "", topic: "", date: "", time: "", duration: 30 })

  const planned = sessions.filter(s => s.status === "planned")
  const done = sessions.filter(s => s.status === "done")

  function addSession() {
    if (!form.subject || !form.topic || !form.date) return
    setSessions(prev => [...prev, {
      id: Date.now().toString(),
      subject: form.subject,
      topic: form.topic,
      date: form.date,
      time: form.time || "00:00",
      duration: form.duration,
      status: "planned",
    }])
    setForm({ subject: "", topic: "", date: "", time: "", duration: 30 })
    setShowForm(false)
  }

  function toggleDone(id: string) {
    setSessions(prev => prev.map(s =>
      s.id === id ? { ...s, status: s.status === "planned" ? "done" as const : "planned" as const } : s
    ))
  }

  function removeSession(id: string) {
    setSessions(prev => prev.filter(s => s.id !== id))
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary/revision" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div className="flex-1">
          <h1 className="text-xl font-bold text-gray-900">Study Planner</h1>
          <p className="text-sm text-gray-500">Plan your study sessions</p>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="flex items-center gap-1.5 rounded-xl bg-indigo-600 px-3 py-2 text-sm font-medium text-white hover:bg-indigo-700"
        >
          <Plus className="size-4" /> Add Session
        </button>
      </div>

      {/* Weekly Overview */}
      <div className="rounded-2xl bg-gradient-to-r from-indigo-600 to-purple-500 p-5 text-white">
        <h2 className="font-bold">This Week</h2>
        <div className="mt-3 grid grid-cols-3 gap-3">
          <div className="rounded-xl bg-white/10 p-3 text-center">
            <p className="text-2xl font-bold">{planned.length}</p>
            <p className="text-xs text-white/70">Planned</p>
          </div>
          <div className="rounded-xl bg-white/10 p-3 text-center">
            <p className="text-2xl font-bold">{done.length}</p>
            <p className="text-xs text-white/70">Completed</p>
          </div>
          <div className="rounded-xl bg-white/10 p-3 text-center">
            <p className="text-2xl font-bold">{planned.reduce((sum, s) => sum + s.duration, 0)}</p>
            <p className="text-xs text-white/70">Minutes planned</p>
          </div>
        </div>
      </div>

      {/* Add Form */}
      {showForm && (
        <div className="rounded-2xl border border-indigo-200 bg-indigo-50 p-5">
          <h3 className="mb-3 font-semibold text-gray-900">New Study Session</h3>
          <div className="mt-3 grid gap-3 sm:grid-cols-2">
            <input type="text" placeholder="Subject" value={form.subject} onChange={e => setForm(f => ({ ...f, subject: e.target.value }))} className="rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm" />
            <input type="text" placeholder="Topic" value={form.topic} onChange={e => setForm(f => ({ ...f, topic: e.target.value }))} className="rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm" />
            <input type="date" value={form.date} onChange={e => setForm(f => ({ ...f, date: e.target.value }))} className="rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm" />
            <input type="time" value={form.time} onChange={e => setForm(f => ({ ...f, time: e.target.value }))} className="rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm" />
          </div>
          <div className="mt-3 flex gap-2">
            <button onClick={addSession} className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700">Save Session</button>
            <button onClick={() => setShowForm(false)} className="rounded-xl bg-gray-100 px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-200">Cancel</button>
          </div>
        </div>
      )}

      {/* Planned Sessions */}
      {planned.length > 0 && (
        <section>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">Planned</h2>
          <div className="space-y-2">
            {planned.map(s => (
              <div key={s.id} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-indigo-50">
                  <BookOpen className="size-5 text-indigo-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-900">{s.topic}</p>
                  <p className="text-xs text-gray-400">
                    {s.subject} · {new Date(s.date).toLocaleDateString([], { weekday: "short", month: "short", day: "numeric" })} · {s.time} · {s.duration} min
                  </p>
                </div>
                <button onClick={() => toggleDone(s.id)} className="rounded-lg bg-green-50 px-3 py-1.5 text-xs font-medium text-green-600 hover:bg-green-100">Done</button>
                <button onClick={() => removeSession(s.id)} className="text-gray-300 hover:text-red-500"><Trash2 className="size-4" /></button>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Completed */}
      {done.length > 0 && (
        <section>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">Completed</h2>
          <div className="space-y-2">
            {done.map(s => (
              <div key={s.id} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 opacity-60">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-green-50">
                  <Clock className="size-5 text-green-500" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-600 line-through">{s.topic}</p>
                  <p className="text-xs text-gray-400">{s.subject} · {s.duration} min</p>
                </div>
                <button onClick={() => toggleDone(s.id)} className="rounded-lg bg-gray-100 px-3 py-1.5 text-xs font-medium text-gray-500 hover:bg-gray-200">Undo</button>
              </div>
            ))}
          </div>
        </section>
      )}

      {sessions.length === 0 && !showForm && (
        <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
          <Calendar className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">No sessions planned</h3>
          <p className="mt-1 text-sm text-gray-500">Click &quot;Add Session&quot; to start planning your study time.</p>
        </div>
      )}
    </div>
  )
}
