"use client"

import { useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { LearnerHeader, EmptyState } from "@/components/learner/shared"
import { Monitor, Code, Network, Database, Palette, Shield, Wrench, ExternalLink, Search } from "lucide-react"

interface SoftwareTool {
  name: string
  purpose: string
  category: string
  url: string
  icon: typeof Code
}

const toolCategories = [
  { label: "All", value: "all" },
  { label: "Programming", value: "Programming" },
  { label: "Networking", value: "Networking" },
  { label: "Databases", value: "Databases" },
  { label: "Design", value: "Design" },
  { label: "Security", value: "Security" },
  { label: "Utilities", value: "Utilities" },
]

const softwareTools: SoftwareTool[] = [
  { name: "Visual Studio Code", purpose: "Lightweight, extensible code editor for multiple languages", category: "Programming", url: "https://code.visualstudio.com", icon: Code },
  { name: "IntelliJ IDEA", purpose: "Full-featured IDE for Java and Kotlin development", category: "Programming", url: "https://www.jetbrains.com/idea", icon: Code },
  { name: "Python / Jupyter", purpose: "Data science, scripting, and interactive notebooks", category: "Programming", url: "https://jupyter.org", icon: Code },
  { name: "Wireshark", purpose: "Network protocol analyzer for troubleshooting and analysis", category: "Networking", url: "https://www.wireshark.org", icon: Network },
  { name: "Cisco Packet Tracer", purpose: "Network simulation and visualization tool", category: "Networking", url: "https://www.netacad.com", icon: Network },
  { name: "MySQL Workbench", purpose: "Visual database design and SQL development tool", category: "Databases", url: "https://www.mysql.com/products/workbench", icon: Database },
  { name: "pgAdmin", purpose: "PostgreSQL administration and development platform", category: "Databases", url: "https://www.pgadmin.org", icon: Database },
  { name: "MongoDB Compass", purpose: "GUI for exploring and managing MongoDB data", category: "Databases", url: "https://www.mongodb.com/products/compass", icon: Database },
  { name: "Figma", purpose: "Collaborative UI/UX design and prototyping tool", category: "Design", url: "https://www.figma.com", icon: Palette },
  { name: "GIMP", purpose: "Free and open-source image editing software", category: "Design", url: "https://www.gimp.org", icon: Palette },
  { name: "Nmap", purpose: "Network discovery and security auditing tool", category: "Security", url: "https://nmap.org", icon: Shield },
  { name: "Postman", purpose: "API development and testing environment", category: "Utilities", url: "https://www.postman.com", icon: Wrench },
  { name: "Docker Desktop", purpose: "Containerized application development and deployment", category: "Utilities", url: "https://www.docker.com/products/docker-desktop", icon: Monitor },
  { name: "Git & GitHub Desktop", purpose: "Version control and collaborative code management", category: "Utilities", url: "https://desktop.github.com", icon: Wrench },
]

export default function SoftwareToolsPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [category, setCategory] = useState("all")
  const [search, setSearch] = useState("")

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"

  const filtered = softwareTools.filter((tool) => {
    const matchCat = category === "all" || tool.category === category
    const matchSearch = tool.name.toLowerCase().includes(search.toLowerCase()) || tool.purpose.toLowerCase().includes(search.toLowerCase())
    return matchCat && matchSearch
  })

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle={t("softwareTools.subtitle")} />

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder={t("softwareTools.searchPlaceholder")}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label={t("softwareTools.searchPlaceholder")}
            className="w-full rounded-xl border border-border bg-card py-2.5 pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-primary/20"
          />
        </div>
        <div className="flex gap-2 overflow-x-auto">
          {toolCategories.map((cat) => (
            <button
              key={cat.value}
              onClick={() => setCategory(cat.value)}
              aria-label={cat.label}
              aria-pressed={category === cat.value}
              className={`shrink-0 rounded-xl border px-4 py-2 text-xs font-medium transition ${
                category === cat.value
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-card text-muted-foreground hover:bg-muted"
              }`}
            >
              {cat.label}
            </button>
          ))}
        </div>
      </div>

      {filtered.length === 0 ? (
        <EmptyState
          icon={<Monitor className="size-8" />}
          title={t("softwareTools.noTools")}
          description={t("softwareTools.noToolsDesc")}
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((tool) => {
            const Icon = tool.icon
            return (
              <a
                key={tool.name}
                href={tool.url}
                target="_blank"
                rel="noopener noreferrer"
                aria-label={`${tool.name} - ${t("softwareTools.openExternal")}`}
                className="rounded-2xl border border-border bg-card p-5 shadow-xs transition hover:shadow-md group"
              >
                <div className="flex items-start justify-between">
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                    <Icon className="size-5 text-primary" />
                  </div>
                  <ExternalLink className="size-4 text-muted-foreground opacity-0 transition group-hover:opacity-100" />
                </div>
                <h3 className="mt-3 font-semibold text-foreground">{tool.name}</h3>
                <p className="mt-1 text-sm text-muted-foreground line-clamp-2">{tool.purpose}</p>
                <span className="mt-3 inline-block rounded-full bg-muted px-2.5 py-0.5 text-[10px] font-medium text-muted-foreground">
                  {tool.category}
                </span>
              </a>
            )
          })}
        </div>
      )}
    </div>
  )
}
