import { notFound } from "next/navigation"
import { SiteHeader } from "@/components/site-header"
import { liveClasses } from "@/lib/data"
import { LiveClassroom } from "@/components/live/live-classroom"

export function generateStaticParams() {
  return liveClasses.map((c) => ({ id: c.id }))
}

export default async function ClassroomPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params
  const item = liveClasses.find((c) => c.id === id) ?? liveClasses.find((c) => c.status === "live")
  if (!item) notFound()

  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1 bg-muted/40">
        <LiveClassroom item={item} />
      </main>
    </div>
  )
}
