import { NextResponse } from "next/server"
import type { NextRequest } from "next/server"

const teacherRoutes = ["/dashboard/teacher"]
const adminRoutes = ["/dashboard/admin", "/dashboard/audit"]
const studentRoutes = ["/dashboard/courses", "/dashboard/lessons", "/dashboard/assignments", "/dashboard/assessments", "/dashboard/results", "/dashboard/attendance", "/dashboard/live-classes", "/dashboard/progress", "/dashboard/messages", "/dashboard/bookmarks"]

export function middleware(request: NextRequest) {
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
        const isStudentRoute = studentRoutes.some((r) => pathname.startsWith(r))

        if (isTeacherRoute && role !== "Teacher" && role !== "Instructor") {
          return NextResponse.redirect(new URL("/dashboard", request.url))
        }
        if (isAdminRoute && role !== "Admin" && role !== "Institution Admin") {
          return NextResponse.redirect(new URL("/dashboard", request.url))
        }
        if (isStudentRoute && (role === "Teacher" || role === "Instructor" || role === "Admin" || role === "Institution Admin")) {
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
