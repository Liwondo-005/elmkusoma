"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { learnerApi, type Resource } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { FileText, Video, Music, Image, Download, ExternalLink, Search, Filter, AlertCircle } from "lucide-react"

export default function LearnerResourcesPage() {
  const { user, loading: authLoading } = useAuth()
  const [resources, setResources] = useState<Resource[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [typeFilter, setTypeFilter] = useState<string>("all")

  useEffect(() => {
    if (!user || user.role !== "Other Learner") return
    loadResources()
  }, [user])

  async function loadResources() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getResources()
      setResources(data)
    } catch {
      setError("Failed to load resources")
    } finally {
      setLoading(false)
    }
  }

  function getResourceIcon(type: string) {
    switch (type?.toUpperCase()) {
      case "VIDEO": return <Video className="size-5 text-red-500" />
      case "AUDIO": return <Music className="size-5 text-purple-500" />
      case "IMAGE": return <Image className="size-5 text-blue-500" />
      default: return <FileText className="size-5 text-teal" />
    }
  }

  function getResourceTypeBadge(type: string) {
    const colors: Record<string, string> = {
      DOCUMENT: "bg-blue-500/10 text-blue-500",
      VIDEO: "bg-red-500/10 text-red-500",
      AUDIO: "bg-purple-500/10 text-purple-500",
      IMAGE: "bg-orange/10 text-orange",
    }
    return colors[type?.toUpperCase()] || "bg-muted text-muted-foreground"
  }

  const types = [...new Set(resources.map((r) => r.resourceType).filter(Boolean))]

  const filteredResources = resources.filter((resource) => {
    const matchesSearch = search === "" || 
      resource.title.toLowerCase().includes(search.toLowerCase()) ||
      (resource.description?.toLowerCase().includes(search.toLowerCase()) ?? false)
    const matchesType = typeFilter === "all" || resource.resourceType === typeFilter
    return matchesSearch && matchesType
  })

  if (authLoading || user?.role !== "Other Learner") {
    return <LoadingState />
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Resources</h1>
        <p className="mt-1 text-sm text-muted-foreground">Access learning materials and resources.</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search resources..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="h-10 w-full rounded-lg border border-border bg-background pl-10 pr-3 text-sm outline-none focus:border-ring"
          />
        </div>
        <select
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value)}
          className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
        >
          <option value="all">All Types</option>
          {types.map((type) => (
            <option key={type} value={type}>{type}</option>
          ))}
        </select>
      </div>

      {loading ? (
        <LoadingState />
      ) : filteredResources.length === 0 ? (
        <EmptyState
          icon={<FileText className="size-8" />}
          title="No resources found"
          description={search ? "Try adjusting your search or filters." : "No resources available yet."}
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filteredResources.map((resource) => (
            <div key={resource.id} className="rounded-2xl border border-border bg-card p-4 shadow-xs">
              <div className="flex items-start gap-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-muted">
                  {getResourceIcon(resource.resourceType)}
                </div>
                <div className="min-w-0 flex-1">
                  <h3 className="text-sm font-semibold text-foreground truncate">{resource.title}</h3>
                  {resource.description && (
                    <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{resource.description}</p>
                  )}
                </div>
              </div>
              <div className="mt-3 flex items-center justify-between">
                <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${getResourceTypeBadge(resource.resourceType)}`}>
                  {resource.resourceType}
                </span>
                <div className="flex items-center gap-2">
                  <a
                    href={resource.fileUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
                  >
                    <Download className="size-3" />
                    Download
                  </a>
                </div>
              </div>
              <p className="mt-2 text-[10px] text-muted-foreground">
                Added {new Date(resource.createdAt).toLocaleDateString()}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
