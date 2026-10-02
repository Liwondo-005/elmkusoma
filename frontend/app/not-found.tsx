import Link from "next/link"
import { ArrowLeft } from "lucide-react"
import { cookies } from "next/headers"
import { getTranslations } from "next-intl/server"
import { Logo } from "@/components/logo"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

export default async function NotFound() {
  const cookieStore = await cookies()
  const cookieLocale = cookieStore.get("NEXT_LOCALE")?.value
  const t = await getTranslations({ locale: cookieLocale === "sw" ? "sw" : "en", namespace: "common" })
  return (
    <div className="flex min-h-dvh flex-col bg-muted/40">
      <header className="border-b border-border bg-background/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center px-4 sm:px-6 lg:px-8">
          <Logo />
        </div>
      </header>

      <main className="flex flex-1 items-center justify-center px-4 py-12">
        <div className="text-center">
          <p className="text-8xl font-extrabold text-primary">404</p>
          <h1 className="mt-4 text-2xl font-bold tracking-tight text-foreground">
            {t("notFound.title")}
          </h1>
          <p className="mt-3 text-sm text-muted-foreground">
            {t("notFound.description")}
          </p>
          <Link
            href="/"
            className={cn(
              buttonVariants(),
              "mt-8 inline-flex h-11 gap-2 px-6"
            )}
          >
            <ArrowLeft className="mr-2 size-4" />
            {t("verifyBackHome")}
          </Link>
        </div>
      </main>
    </div>
  )
}
