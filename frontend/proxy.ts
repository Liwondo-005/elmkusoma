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
const districtRoutes = ["/dashboard/district"]

export function proxy(request: NextRequest) {
  const accessToken = request.cookies.get("elmkusoma_access_token")
  const currentUser = request.cookies.get("elmkusoma_current_user")
  const pathname = request.nextUrl.pathname

  if (pathname.startsWith("/dashboard")) {
    if (!accessToken && !currentUser) {
      const loginUrl = new URL("/login", request.url)
      loginUrl.searchParams.set("redirect", pathname)
      return NextResponse.redirect(loginUrl)
    }

    if (currentUser) {
      try {
        const user = JSON.parse(decodeURIComponent(currentUser.value))
        const role = user.role

        const isTeacherRoute = teacherRoutes.some((r) => pathname.startsWith(r))
        const isAdminRoute = adminRoutes.some((r) => pathname.startsWith(r))
        const isPlatformAdminRoute = platformAdminRoutes.some((r) => pathname.startsWith(r))
        const isLearnerRoute = learnerRoutes.some((r) => pathname.startsWith(r))
        const isStudentRoute = studentRoutes.some((r) => pathname.startsWith(r))
        const isParentRoute = parentRoutes.some((r) => pathname.startsWith(r))
        const isNationalRoute = nationalRoutes.some((r) => pathname.startsWith(r))
        const isRegionalRoute = regionalRoutes.some((r) => pathname.startsWith(r))
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
        if (isRegionalRoute && role !== "Regional Admin") {
          return NextResponse.redirect(new URL("/dashboard", request.url))
        }
        if (isDistrictRoute && role !== "District Admin") {
          return NextResponse.redirect(new URL("/dashboard", request.url))
        }
      } catch {
        // Invalid cookie, treat as unauthenticated
      }
    }
  }

  return NextResponse.next()
}

export const config = {
  matcher: ["/dashboard/:path*"],
}
