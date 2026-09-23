"use client"

import { Radio } from "lucide-react"

export default function PlatformStreamingPage() {
  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-violet-600 text-white"><Radio className="size-4" /></span> Streaming</h1>
        <p className="mt-1 text-sm text-muted-foreground">Live streaming infrastructure — LiveKit rooms, egress, and recordings. Spec ref: platform_admin.md § streaming. Placeholder pending streaming telemetry.</p>
      </div>
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-12 text-center">
          <Radio className="size-10 text-muted-foreground/50" />
          <p className="mt-3 text-sm font-semibold text-foreground">No streaming telemetry</p>
          <p className="mt-1 max-w-md text-xs leading-relaxed text-muted-foreground">Streaming metrics (active rooms, participants, egress) require LiveKit telemetry aggregation. No fake live data is shown — empty state per spec §13. See Platform Health for LiveKit probe.</p>
        </div>
      </div>
    </div>
  )
}
