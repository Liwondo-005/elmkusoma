// Workspace resolver — spec §10: AUTHENTICATED USER → WORKSPACE RESOLVER →
// DEDICATED WORKSPACE. Single source of truth for post-login and /dashboard
// redirects. Returns null when the user should stay on /dashboard (their own
// workspace home, e.g. primary students).
import type { AuthUser } from "@/lib/auth"

export function resolveWorkspace(
  user: Pick<AuthUser, "role" | "learningLevel"> | null | undefined,
): string | null {
  if (!user) return null
  switch (user.role) {
    // Backend ADMIN (platform admin) → dedicated platform workspace.
    case "Admin":
      return "/dashboard/platform-admin"
    case "Institution Admin":
      return "/dashboard/admin"
    case "Parent":
      return "/dashboard/parent"
    case "Teacher":
    case "Instructor":
      return "/dashboard/teacher"
    case "Other Learner":
      return "/dashboard/learner"
    case "National Admin":
      return "/dashboard/national"
    case "Regional Admin":
      return "/dashboard/regional"
    case "District Admin":
      return "/dashboard/district"
    case "Student": {
      const level = (user.learningLevel || "").toUpperCase()
      if (level === "NURSERY") return "/dashboard/nursery"
      if (level === "SECONDARY") return "/dashboard/secondary"
      if (level === "COLLEGE" || level === "UNIVERSITY" || level === "VETA") return "/dashboard/learner"
      return null
    }
    default:
      return null
  }
}
