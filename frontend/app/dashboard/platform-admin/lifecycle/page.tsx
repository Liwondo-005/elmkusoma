"use client"

import { GitBranch } from "lucide-react"

export default function PlatformLifecyclePage() {
  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-slate-700 text-white"><GitBranch className="size-4" /></span> Lifecycle</h1>
        <p className="mt-1 text-sm text-muted-foreground">Entity lifecycle governance — provisioning, activation, suspension, and deprovisioning. Spec ref: platform_admin.md § lifecycle. Placeholder pending lifecycle orchestrator.</p>
      </div>
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-12 text-center">
          <GitBranch className="size-10 text-muted-foreground/50" />
          <p className="mt-3 text-sm font-semibold text-foreground">No lifecycle workflows</p>
          <p className="mt-1 max-w-md text-xs leading-relaxed text-muted-foreground">Lifecycle requires workflow engine integration. No synthetic transitions are shown — empty state per spec §13. Institution, user, and service lifecycles will be governed here.</p>
        </div>
      </div>
    </div>
  )
}
