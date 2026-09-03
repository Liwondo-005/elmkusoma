import Link from "next/link"
import Image from "next/image"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

export function AboutCta() {
  return (
    <section className="pb-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="overflow-hidden rounded-3xl border border-border bg-primary text-primary-foreground">
          <div className="grid items-center gap-8 lg:grid-cols-2">
            <div className="p-8 sm:p-12">
              <h2 className="text-balance text-3xl font-bold tracking-tight sm:text-4xl">
                Ready to start your learning journey?
              </h2>
              <p className="mt-4 max-w-md text-pretty text-primary-foreground/80">
                Join thousands of African learners gaining new skills, earning certificates and advancing their futures
                with ELMKUSOMA.
              </p>
              <div className="mt-7 flex flex-wrap gap-3">
                <Link
                  href="/register"
                  className={cn(buttonVariants(), "h-12 bg-background px-6 text-base text-foreground hover:bg-background/90")}
                >
                  Get Started Free
                </Link>
                <Link
                  href="/live-classes"
                  className={cn(
                    buttonVariants({ variant: "outline" }),
                    "h-12 border-primary-foreground/30 bg-transparent px-6 text-base text-primary-foreground hover:bg-primary-foreground/10 hover:text-primary-foreground",
                  )}
                >
                  Explore Live Classes
                </Link>
              </div>
            </div>

            <div className="relative hidden h-full min-h-80 lg:block">
              <Image
                src="/images/cta-learner.png"
                alt="Students learning on ELMKUSOMA"
                fill
                className="object-cover"
                sizes="50vw"
              />
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
