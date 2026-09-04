import Link from "next/link"
import { ArrowLeft } from "lucide-react"
import { Logo } from "@/components/logo"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

export default function NotFound() {
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
            Page not found
          </h1>
          <p className="mt-3 text-sm text-muted-foreground">
            The page you&apos;re looking for doesn&apos;t exist or has been moved.
          </p>
          <Link
            href="/"
            className={cn(
              buttonVariants(),
              "mt-8 inline-flex h-11 gap-2 px-6"
            )}
          >
            <ArrowLeft className="mr-2 size-4" />
            Back to Home
          </Link>
        </div>
      </main>
    </div>
  )
}
