import { NextResponse } from "next/server"
import type { NextRequest } from "next/server"

const teacherRoutes = ["/dashboard/teacher"]
const adminRoutes = ["/dashboard/admin", "/dashboard/audit"]
const platformAdminRoutes = ["/dashboard/platform-admin"]
const learnerRoutes = ["/dashboard/learner"]
const studentRoutes = ["/dashboard/courses", "/dashboard/lessons", "/dashboard/assignments", "/dashboard/assessments", "/dashboard/results", "/dashboard/attendance", "/dashboard/progress", "/dashboard/messages", "/dashboard/profile", "/dashboard/settings", "/dashboard/bookmarks"]
const parentRoutes = ["/dashboard/parent"]
const nationalRoutes = ["/dashboard/national"]
const regionalRoutes = ["/dashboard/regional"]
const regionalAdminRoutes = ["/dashboard/regional-admin"]
const districtRoutes = ["/dashboard/district"]
// Nationaladmin.md §7/§39 — the authority command center is only for the
// education-authority roles (plus platform Admin, matching the backend's
// OversightScopeResolver). The APIs re-check every request; this gate keeps
// unauthorized shells from rendering at all.
const oversightRoles = ["National Admin", "Regional Admin", "District Admin", "Admin"]

export function proxy(request: NextRequest) {
  const accessToken = request.cookies.get("elmkusoma_access_token")
  const currentUser = request.cookies.get("elmkusoma_current_user")
  const pathname = request.nextUrl.pathname

  if (pathname.startsWith("/oversight")) {
    if (!accessToken && !currentUser) {
      const loginUrl = new URL("/login", request.url)
      loginUrl.searchParams.set("redirect", pathname)
      return NextResponse.redirect(loginUrl)
    }

    if (currentUser) {
      try {
        const user = JSON.parse(decodeURIComponent(currentUser.value))
        // Routing hint only (backend re-checks) — fail closed on unexpected shape.
        if (!user || typeof user !== "object" || Array.isArray(user) || typeof user.role !== "string") {
          throw new Error("Invalid user cookie shape")
        }
        if (!oversightRoles.includes(user.role)) {
          return NextResponse.redirect(new URL("/dashboard", request.url))
        }
      } catch {
        // Invalid cookie, treat as unauthenticated
        const loginUrl = new URL("/login", request.url)
        loginUrl.searchParams.set("redirect", pathname)
        return NextResponse.redirect(loginUrl)
      }
    }
  }

  if (pathname.startsWith("/dashboard")) {
    if (!accessToken && !currentUser) {
      const loginUrl = new URL("/login", request.url)
      loginUrl.searchParams.set("redirect", pathname)
      return NextResponse.redirect(loginUrl)
    }

    if (currentUser) {
      try {
        const user = JSON.parse(decodeURIComponent(currentUser.value))
        // Routing hint only (backend re-checks) — fail closed on unexpected shape.
        if (!user || typeof user !== "object" || Array.isArray(user) || typeof user.role !== "string") {
          throw new Error("Invalid user cookie shape")
        }
        const role = user.role

        const isTeacherRoute = teacherRoutes.some((r) => pathname.startsWith(r))
        const isAdminRoute = adminRoutes.some((r) => pathname.startsWith(r))
        const isPlatformAdminRoute = platformAdminRoutes.some((r) => pathname.startsWith(r))
        const isLearnerRoute = learnerRoutes.some((r) => pathname.startsWith(r))
        const isStudentRoute = studentRoutes.some((r) => pathname.startsWith(r))
        const isParentRoute = parentRoutes.some((r) => pathname.startsWith(r))
        const isNationalRoute = nationalRoutes.some((r) => pathname.startsWith(r))
        const isRegionalRoute = regionalRoutes.some(
          (r) => pathname.startsWith(r) && !pathname.startsWith("/dashboard/regional-admin"),
        )
        const isRegionalAdminRoute = regionalAdminRoutes.some((r) => pathname.startsWith(r))
        const isDistrictRoute = districtRoutes.some((r) => pathname.startsWith(r))

        if (isTeacherRoute && role !== "Teacher" && role !== "Instructor") {
          return NextResponse.redirect(new URL("/dashboard", request.url))
        }
        if (isAdminRoute && role !== "Admin" && role !== "Institution Admin") {
          return NextResponse.redirect(new URL("/dashboard", request.url))
        }
        if (isPlatformAdminRoute && role !== "Admin") {
          return NextResponse.redirect(new URL("/dashboard", request.url))
        }
        if (isLearnerRoute && role !== "Other Learner" && role !== "Student") {
          return NextResponse.redirect(new URL("/dashboard", request.url))
        }
        if (isStudentRoute && role === "Other Learner") {
          return NextResponse.redirect(new URL("/dashboard/learner", request.url))
        }
        if (isStudentRoute && (role === "Teacher" || role === "Instructor" || role === "Admin" || role === "Institution Admin")) {
          return NextResponse.redirect(new URL("/dashboard", request.url))
        }
        if (isParentRoute && role !== "Parent") {
          return NextResponse.redirect(new URL("/dashboard", request.url))
        }
        if (isNationalRoute && role !== "National Admin") {
          return NextResponse.redirect(new URL("/dashboard", request.url))
        }
        if (isRegionalAdminRoute && role !== "Regional Admin" && role !== "District Admin") {
          return NextResponse.redirect(new URL("/dashboard", request.url))
        }
        if (isRegionalRoute && role !== "Regional Admin") {
          return NextResponse.redirect(new URL("/dashboard", request.url))
        }
        if (isDistrictRoute && role !== "District Admin") {
          return NextResponse.redirect(new URL("/dashboard", request.url))
        }
      } catch {
        // Invalid cookie: fail closed to /login (was: fall through authenticated).
        const loginUrl = new URL("/login", request.url)
        loginUrl.searchParams.set("redirect", pathname)
        return NextResponse.redirect(loginUrl)
      }
    }
  }

  return NextResponse.next()
}

export const config = {
  matcher: ["/dashboard/:path*", "/oversight/:path*"],
}
