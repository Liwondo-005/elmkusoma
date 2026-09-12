"use client"

import { useAuth } from "@/lib/auth"
import { LoadingState } from "@/components/learner/shared"
import { Settings, User, Mail, Shield, GraduationCap } from "lucide-react"

export default function LearnerSettingsPage() {
  const { user, loading: authLoading } = useAuth()

  if (authLoading || user?.role !== "Other Learner") {
    return <LoadingState />
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Settings</h1>
        <p className="mt-1 text-sm text-muted-foreground">Manage your account settings.</p>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="text-lg font-semibold text-foreground mb-4">Account Information</h2>
        <div className="space-y-4">
          <div className="flex items-center gap-4 rounded-xl border border-border p-4">
            <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10">
              <User className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-sm font-medium text-foreground">Full Name</p>
              <p className="text-sm text-muted-foreground">{user?.firstName} {user?.lastName}</p>
            </div>
          </div>
          <div className="flex items-center gap-4 rounded-xl border border-border p-4">
            <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10">
              <Mail className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-sm font-medium text-foreground">Email</p>
              <p className="text-sm text-muted-foreground">{user?.email}</p>
            </div>
          </div>
          <div className="flex items-center gap-4 rounded-xl border border-border p-4">
            <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10">
              <Shield className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-sm font-medium text-foreground">Role</p>
              <p className="text-sm text-muted-foreground">{user?.role}</p>
            </div>
          </div>
          <div className="flex items-center gap-4 rounded-xl border border-border p-4">
            <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10">
              <GraduationCap className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-sm font-medium text-foreground">Learning Level</p>
              <p className="text-sm text-muted-foreground">{user?.learningLevel || "Not set"}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="text-lg font-semibold text-foreground mb-4">About</h2>
        <p className="text-sm text-muted-foreground">
          ELMKUSOMA Education Platform. Manage your learning journey and track your progress.
        </p>
        <p className="mt-2 text-xs text-muted-foreground">
          For account changes or deletion requests, please contact your administrator.
        </p>
      </div>
    </div>
  )
}
