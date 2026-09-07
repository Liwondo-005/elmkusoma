"use client"

import { useState } from "react"
import { useAuth } from "@/lib/auth"
import { Camera } from "lucide-react"

export default function DashboardProfilePage() {
  const { user } = useAuth()
  const [saved, setSaved] = useState(false)

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">My Profile</h1>
        <p className="mt-1 text-sm text-muted-foreground">Manage your personal information.</p>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        {/* Avatar */}
        <div className="flex items-center gap-5">
          <div className="relative">
            <div className="flex size-20 items-center justify-center rounded-full bg-primary/10 text-2xl font-bold text-primary">
              {user?.firstName?.[0] || user?.name?.[0] || "S"}
            </div>
            <button className="absolute bottom-0 right-0 flex size-7 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-xs">
              <Camera className="size-3.5" />
            </button>
          </div>
          <div>
            <h2 className="text-lg font-semibold text-foreground">{user?.name || "Student"}</h2>
            <p className="text-sm text-muted-foreground">{user?.email || "student@example.com"}</p>
            <span className="mt-1 inline-flex rounded-full bg-primary/10 px-2.5 py-0.5 text-[10px] font-semibold text-primary capitalize">
              {user?.role || "Student"}
            </span>
          </div>
        </div>

        {/* Form */}
        <form className="mt-8 space-y-5" onSubmit={handleSubmit}>
          <div className="grid gap-5 sm:grid-cols-2">
            <div>
              <label htmlFor="name" className="block text-sm font-medium text-foreground">Full Name</label>
              <input id="name" type="text" defaultValue={user?.name || ""} className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none focus:border-ring focus:bg-background" readOnly />
            </div>
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-foreground">Email</label>
              <input id="email" type="email" defaultValue={user?.email || ""} className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none focus:border-ring focus:bg-background" readOnly />
            </div>
            <div>
              <label htmlFor="phone" className="block text-sm font-medium text-foreground">Phone</label>
              <input id="phone" type="tel" placeholder="+255 700 000 000" className="mt-1.5 h-11 w-full rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none placeholder:text-muted-foreground focus:border-ring focus:bg-background" />
            </div>
            <div>
              <label htmlFor="level" className="block text-sm font-medium text-foreground">Education Level</label>
              <select id="level" defaultValue={user?.educationLevel || ""} className="mt-1.5 h-11 w-full appearance-none rounded-lg border border-border bg-muted/60 px-3.5 text-sm text-foreground outline-none focus:border-ring focus:bg-background">
                <option value="">Select level</option>
                <option>Nursery School</option>
                <option>Primary School</option>
                <option>Lower Secondary School</option>
                <option>Advanced Secondary School</option>
                <option>College</option>
                <option>Vocational</option>
                <option>University</option>
              </select>
            </div>
          </div>
          <div>
            <label htmlFor="bio" className="block text-sm font-medium text-foreground">Bio</label>
            <textarea id="bio" rows={3} placeholder="Tell us about yourself..." className="mt-1.5 w-full rounded-lg border border-border bg-muted/60 px-3.5 py-3 text-sm text-foreground outline-none placeholder:text-muted-foreground focus:border-ring focus:bg-background" />
          </div>
          <div className="flex items-center gap-3">
            <button type="submit" className="h-11 rounded-lg bg-primary px-6 text-sm font-medium text-primary-foreground hover:bg-primary/90">
              Save Changes
            </button>
            {saved && <span className="text-sm text-teal font-medium">Saved!</span>}
          </div>
        </form>
      </div>
    </div>
  )
}
