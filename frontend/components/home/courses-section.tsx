import Image from "next/image"
import Link from "next/link"
import { featuredCourses } from "@/lib/data"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

export function CoursesSection() {
  return (
    <section className="bg-muted/50 py-16 lg:py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h2 className="text-3xl font-bold tracking-tight text-foreground">Popular courses</h2>
            <p className="mt-2 max-w-xl text-muted-foreground">
              Self-paced courses taught by expert instructors — learn at your own pace.
            </p>
          </div>
          <Link href="/courses" className={cn(buttonVariants({ variant: "outline" }), "h-10 gap-2 px-4")}>
            Explore Courses
          </Link>
        </div>

        <div className="mt-10 grid gap-6 md:grid-cols-3">
          {featuredCourses.map((course) => (
            <div
              key={course.id}
              className="group overflow-hidden rounded-2xl border border-border bg-card shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg"
            >
              <div className="relative aspect-[16/10] overflow-hidden">
                <Image
                  src={course.image || "/placeholder.svg"}
                  alt={course.title}
                  fill
                  className="object-cover transition-transform duration-500 group-hover:scale-105"
                  sizes="(max-width: 768px) 100vw, 33vw"
                />
                <span className="absolute left-3 top-3 rounded-md bg-primary px-2.5 py-1 text-xs font-semibold text-primary-foreground">
                  {course.tag}
                </span>
              </div>
              <div className="p-5">
                <h3 className="text-base font-semibold text-foreground">{course.title}</h3>
                <p className="mt-1 text-sm text-muted-foreground">{course.instructor}</p>
                <p className="mt-2 text-sm font-semibold text-foreground">{course.rating}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
