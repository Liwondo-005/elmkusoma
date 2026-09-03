import Link from "next/link"
import { ArrowRight, Play } from "lucide-react"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

export function PrimaryCta() {
  return (
    <section className="py-12 lg:py-16">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="grid gap-6 lg:grid-cols-2">
          <div className="flex flex-col justify-between overflow-hidden rounded-2xl bg-primary p-8 text-primary-foreground">
            <div>
              <h2 className="text-2xl font-bold">Try a Free Lesson</h2>
              <p className="mt-3 text-primary-foreground/80">
                Explore sample lessons from Darasa I to Darasa VI — no account needed. See
                how we teach Kiswahili, Hisabati and Sayansi.
              </p>
            </div>
            <Link
              href="/schools/primary/bunge"
              className={cn(
                buttonVariants(),
                "mt-8 h-11 w-full gap-2 bg-background text-foreground hover:bg-background/90 sm:w-auto sm:self-start",
              )}
            >
              <Play className="size-4" />
              Start Learning
            </Link>
          </div>

          <div className="flex flex-col justify-between overflow-hidden rounded-2xl border border-border bg-card p-8">
            <div>
              <h2 className="text-2xl font-bold text-foreground">Enroll in a Primary School</h2>
              <p className="mt-3 text-muted-foreground">
                Register your child for a full primary program with structured lessons,
                progress tracking and certificates from Darasa I through Darasa VI.
              </p>
            </div>
            <Link
              href="/register"
              className={cn(
                buttonVariants({ variant: "outline" }),
                "mt-8 h-11 w-full gap-2 border-primary/30 text-primary hover:bg-accent sm:w-auto sm:self-start",
              )}
            >
              Enroll Now
              <ArrowRight className="size-4" />
            </Link>
          </div>
        </div>
      </div>
    </section>
  )
}
