"use client"

import { ArrowLeft, Users, BookOpen, Star } from "lucide-react"
import Link from "next/link"

export default function ParentLearningPage() {
  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">Family Learning</h1>
          <p className="text-sm text-gray-500">Learn with your family at home</p>
        </div>
      </div>

      <div className="nursery-card rounded-2xl bg-gradient-to-r from-pink-400 to-rose-400 p-5 text-white">
        <div className="flex items-center gap-3">
          <Users className="size-8" />
          <div>
            <h2 className="text-lg font-bold">Learn Together at Home</h2>
            <p className="text-sm text-white/70">Activities you can do with your family</p>
          </div>
        </div>
      </div>

      <div className="space-y-3">
        {[
          { icon: BookOpen, title: "Story Time with Family", desc: "Read a story with your parent or guardian", color: "bg-blue-100 text-blue-500" },
          { icon: Star, title: "Count Together", desc: "Count objects around the house together", color: "bg-amber-100 text-amber-500" },
          { icon: Users, title: "Cook Together", desc: "Help prepare a simple meal with family", color: "bg-green-100 text-green-500" },
          { icon: Star, title: "Garden Together", desc: "Plant a seed and watch it grow", color: "bg-emerald-100 text-emerald-500" },
        ].map(item => (
          <div key={item.title} className="nursery-card flex items-center gap-4 rounded-2xl bg-white p-4">
            <div className={`flex size-12 items-center justify-center rounded-2xl ${item.color}`}>
              <item.icon className="size-6" />
            </div>
            <div>
              <p className="font-bold text-gray-800">{item.title}</p>
              <p className="text-xs text-gray-500">{item.desc}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="nursery-card rounded-2xl bg-white p-8 text-center">
        <Users className="mx-auto size-12 text-gray-300" />
        <h3 className="mt-3 text-lg font-bold text-gray-800">Family activities coming soon</h3>
        <p className="mt-1 text-sm text-gray-500">Your teacher will share activities you can do with your family at home.</p>
      </div>
    </div>
  )
}
