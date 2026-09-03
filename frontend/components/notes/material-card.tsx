import { FileText, Download, Eye, BookOpen, FlaskConical, Calculator, Presentation } from "lucide-react"
import { Button } from "@/components/ui/button"
import type { StudyMaterial } from "@/lib/data"
import { cn } from "@/lib/utils"

const typeConfig: Record<string, { label: string; icon: typeof FileText; tint: string }> = {
  notes: { label: "Notes", icon: BookOpen, tint: "text-primary bg-accent" },
  slides: { label: "Slides", icon: Presentation, tint: "text-purple-600 bg-purple-50" },
  "formula-sheet": { label: "Formula Sheet", icon: Calculator, tint: "text-teal bg-teal/10" },
  practice: { label: "Practice", icon: FlaskConical, tint: "text-orange bg-orange/10" },
}

export function MaterialCard({ material }: { material: StudyMaterial }) {
  const config = typeConfig[material.type] ?? typeConfig.notes
  const Icon = config.icon

  return (
    <div className="group flex flex-col overflow-hidden rounded-2xl border border-border bg-card shadow-xs transition-shadow hover:shadow-md">
      <div className="flex items-start gap-3 p-5 pb-0">
        <div className={cn("flex size-10 shrink-0 items-center justify-center rounded-xl", config.tint)}>
          <Icon className="size-5" />
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-xs font-medium text-muted-foreground">{material.subject}</p>
          <h3 className="mt-0.5 text-sm font-semibold leading-snug text-foreground line-clamp-2">
            {material.title}
          </h3>
        </div>
      </div>

      <div className="flex flex-1 flex-col justify-end p-5 pt-3">
        <div className="flex flex-wrap items-center gap-2">
          <span className={cn("inline-flex items-center rounded-md px-2 py-0.5 text-[11px] font-medium", config.tint)}>
            {config.label}
          </span>
          <span className="inline-flex items-center rounded-md bg-muted px-2 py-0.5 text-[11px] font-medium text-muted-foreground">
            {material.level}
          </span>
          {material.pages && (
            <span className="text-[11px] text-muted-foreground">{material.pages} pages</span>
          )}
        </div>

        <p className="mt-2 text-xs text-muted-foreground">By {material.instructor}</p>

        <div className="mt-4">
          {material.access === "download" ? (
            <Button variant="outline" size="sm" className="w-full gap-1.5">
              <Download className="size-3.5" />
              Download
            </Button>
          ) : (
            <Button variant="ghost" size="sm" className="w-full gap-1.5">
              <Eye className="size-3.5" />
              View Only
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
