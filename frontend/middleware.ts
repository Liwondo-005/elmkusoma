import { NextResponse } from "next/server"
import type { NextRequest } from "next/server"

export function middleware(request: NextRequest) {
  const accessToken = request.cookies.get("elmkusoma_access_token")
  const currentUser = request.cookies.get("elmkusoma_current_user")

  if (request.nextUrl.pathname.startsWith("/dashboard") && !accessToken && !currentUser) {
    const loginUrl = new URL("/login", request.url)
    loginUrl.searchParams.set("redirect", request.nextUrl.pathname)
    return NextResponse.redirect(loginUrl)
  }

  return NextResponse.next()
}

export const config = {
  matcher: ["/dashboard/:path*"],
}
