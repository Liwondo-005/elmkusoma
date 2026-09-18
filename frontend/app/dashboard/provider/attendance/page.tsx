"use client"
import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { nfeApi } from "@/lib/nfe-api"
import { Loader2, Plus, Search } from "lucide-react"

export default function AttendancePage() {
  const [records, setRecords] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")

  useEffect(() => {
    loadAttendance()
  }, [])

  async function loadAttendance() {
    try {
      setLoading(true)
      const data = await nfeApi.listAttendance()
      setRecords(data)
    } catch { /* empty */ } finally {
      setLoading(false)
    }
  }

  const filtered = records.filter((r) =>
    (r.learnerName || r.studentName || "").toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Attendance</h1>
          <p className="text-muted-foreground">Track learner attendance</p>
        </div>
        <button className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
          <Plus className="size-4" /> Mark Attendance
        </button>
      </div>
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Search className="size-4 text-muted-foreground" />
            <input
              placeholder="Search by learner name..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
            />
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-8"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
          ) : filtered.length === 0 ? (
            <p className="py-4 text-center text-sm text-muted-foreground">No attendance records found.</p>
          ) : (
            <div className="space-y-3">
              {filtered.map((record) => (
                <div key={record.id} className="flex items-center justify-between rounded-lg border border-border p-4">
                  <div>
                    <p className="text-sm font-medium text-foreground">{record.learnerName || record.studentName}</p>
                    <p className="text-xs text-muted-foreground">{record.date || record.attendanceDate}</p>
                  </div>
                  <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${record.status === "PRESENT" ? "bg-green-100 text-green-700" : record.status === "ABSENT" ? "bg-red-100 text-red-700" : "bg-yellow-100 text-yellow-700"}`}>
                    {record.status || "Unknown"}
                  </span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
