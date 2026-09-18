"use client"

import { useAuth } from "@/lib/auth"
import { LearnerHeader, EmptyState } from "@/components/learner/shared"
import { Briefcase, TrendingUp, Award, GraduationCap, Building2, Wrench, Star } from "lucide-react"

const industries = [
  { name: "Technology & IT", description: "Software engineering, data science, cybersecurity, and emerging technologies." },
  { name: "Healthcare", description: "Clinical practice, public health, biomedical research, and health administration." },
  { name: "Engineering", description: "Civil, mechanical, electrical, and industrial engineering roles." },
  { name: "Business & Finance", description: "Management, accounting, banking, investment, and entrepreneurship." },
  { name: "Education", description: "Teaching, curriculum development, educational leadership, and training." },
  { name: "Agriculture", description: "Agricultural science, food security, agribusiness, and sustainable farming." },
]

const professionalSkills = [
  { name: "Leadership & Management", icon: TrendingUp },
  { name: "Communication & Presentation", icon: Star },
  { name: "Critical Thinking & Problem Solving", icon: Wrench },
  { name: "Data Analysis & Research", icon: Award },
  { name: "Project Management", icon: Briefcase },
  { name: "Digital Literacy", icon: GraduationCap },
]

const certifications = [
  "Professional certifications in your field",
  "Industry-recognized credentials",
  "Research publication opportunities",
  "Internship and work placement programs",
]

const careerPaths = [
  "Academic research and teaching",
  "Industry professional roles",
  "Entrepreneurship and startups",
  "Government and public service",
  "International organizations",
  "NGO and community development",
]

export default function CareerPage() {
  const { user } = useAuth()

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle="Explore career paths, professional skills, and development opportunities." />

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-center gap-3 mb-4">
          <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
            <Building2 className="size-5 text-primary" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-foreground">Industries</h2>
            <p className="text-sm text-muted-foreground">Explore opportunities across key sectors</p>
          </div>
        </div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {industries.map((industry) => (
            <div key={industry.name} className="rounded-xl border border-border bg-muted/30 p-4">
              <h3 className="font-semibold text-foreground text-sm">{industry.name}</h3>
              <p className="mt-1 text-xs text-muted-foreground">{industry.description}</p>
            </div>
          ))}
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-center gap-3 mb-4">
          <div className="flex size-10 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30">
            <Wrench className="size-5 text-blue-600 dark:text-blue-400" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-foreground">Professional Skills</h2>
            <p className="text-sm text-muted-foreground">Develop skills that employers value</p>
          </div>
        </div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {professionalSkills.map((skill) => (
            <div key={skill.name} className="flex items-center gap-3 rounded-xl border border-border bg-muted/30 p-4">
              <skill.icon className="size-5 text-primary shrink-0" />
              <span className="text-sm font-medium text-foreground">{skill.name}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <div className="flex items-center gap-3 mb-4">
            <div className="flex size-10 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30">
              <Award className="size-5 text-emerald-600 dark:text-emerald-400" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-foreground">Certifications</h2>
              <p className="text-sm text-muted-foreground">Build your credentials</p>
            </div>
          </div>
          <ul className="space-y-2">
            {certifications.map((cert) => (
              <li key={cert} className="flex items-start gap-2 text-sm text-muted-foreground">
                <span className="mt-1 size-1.5 shrink-0 rounded-full bg-emerald-500" />
                {cert}
              </li>
            ))}
          </ul>
        </div>

        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <div className="flex items-center gap-3 mb-4">
            <div className="flex size-10 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/30">
              <GraduationCap className="size-5 text-amber-600 dark:text-amber-400" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-foreground">Career Paths</h2>
              <p className="text-sm text-muted-foreground">Where your degree can take you</p>
            </div>
          </div>
          <ul className="space-y-2">
            {careerPaths.map((path) => (
              <li key={path} className="flex items-start gap-2 text-sm text-muted-foreground">
                <span className="mt-1 size-1.5 shrink-0 rounded-full bg-amber-500" />
                {path}
              </li>
            ))}
          </ul>
        </div>
      </div>

      <EmptyState
        icon={<Briefcase className="size-8" />}
        title="Connect your career profile"
        description="Link your career goals, uploaded CV, and professional documents to personalize your career world."
      />
    </div>
  )
}
