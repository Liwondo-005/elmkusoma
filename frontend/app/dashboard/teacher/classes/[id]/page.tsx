"use client"

import { useEffect, useState } from "react"
import { useParams } from "next/navigation"
import { Users, BookOpen, Loader2, CheckCircle, XCircle, Clock, FileText, PenTool, Video, AlertCircle } from "lucide-react"
import { teacherFetch, type ClassGroupInfo, type StudentInClass, type AttendanceSummary } from "@/lib/teacher-api"

interface Lesson { id: string; title: string; description: string | null; isPublished: boolean; sortOrder: number; createdAt: string }
interface Assignment { id: string; title: string; description: string | null; dueDate: string | null; totalMarks: number; createdAt: string }
interface Assessment { id: string; title: string; description: string | null; totalMarks: number; timeLimitMinutes: number; createdAt: string }
interface LiveClass { id: string; title: string; scheduledAt: string; status: string; durationMinutes: number }

type Tab = "students" | "attendance" | "lessons" | "assignments" | "assessments" | "live-classes"

export default function TeacherClassDetailPage() {
  const params = useParams()
  const classId = params.id as string
  const [cls, setCls] = useState<ClassGroupInfo | null>(null)
  const [students, setStudents] = useState<StudentInClass[]>([])
  const [summaries, setSummaries] = useState<AttendanceSummary[]>([])
  const [lessons, setLessons] = useState<Lesson[]>([])
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [assessments, setAssessments] = useState<Assessment[]>([])
  const [liveClasses, setLiveClasses] = useState<LiveClass[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState<Tab>("students")

  useEffect(() => {
    async function load() {
      try {
        const [allClasses, studs, attSummaries, classLessons, classAssignments, classAssessments] = await Promise.all([
          teacherFetch<ClassGroupInfo[]>("/v1/academic/class-groups").catch(() => []),
          teacherFetch<StudentInClass[]>(`/v1/students?classId=${classId}`).catch(() => []),
          teacherFetch<AttendanceSummary[]>(`/v1/attendance/summary/class/${classId}`).catch(() => []),
          teacherFetch<Lesson[]>(`/v1/learning/lessons/class/${classId}`).catch(() => []),
          teacherFetch<Assignment[]>(`/v1/learning/assignments/class/${classId}`).catch(() => []),
          teacherFetch<Assessment[]>(`/v1/assessments/class/${classId}`).catch(() => []),
        ])
        setCls(allClasses.find((c) => c.id === classId) || null)
        setStudents(studs)
        setSummaries(attSummaries)
        setLessons(classLessons)
        setAssignments(classAssignments)
        setAssessments(classAssessments)
      } catch {
        setError("Failed to load class details")
      } finally { setLoading(false) }
    }
    load()
  }, [classId])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  const tabs: { key: Tab; label: string; icon: typeof Users; count: number }[] = [
    { key: "students", label: "Students", icon: Users, count: students.length },
    { key: "attendance", label: "Attendance", icon: CheckCircle, count: summaries.length },
    { key: "lessons", label: "Lessons", icon: BookOpen, count: lessons.length },
    { key: "assignments", label: "Assignments", icon: FileText, count: assignments.length },
    { key: "assessments", label: "Assessments", icon: PenTool, count: assessments.length },
  ]

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{cls?.name || "Class Details"}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{cls?.gradeName} &middot; {students.length} students &middot; {lessons.length} lessons &middot; {assignments.length} assignments</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      <div className="flex gap-2 overflow-x-auto border-b border-border">
        {tabs.map((tab) => (
          <button key={tab.key} onClick={() => setActiveTab(tab.key)} className={`flex items-center gap-1.5 border-b-2 whitespace-nowrap px-4 py-2.5 text-sm font-medium transition-colors ${activeTab === tab.key ? "border-primary text-primary" : "text-muted-foreground hover:text-foreground"}`}>
            <tab.icon className="size-4" /> {tab.label} ({tab.count})
          </button>
        ))}
      </div>

      {activeTab === "students" && (
        <div className="rounded-2xl border border-border bg-card shadow-xs">
          {students.length === 0 ? (
            <div className="py-12 text-center"><Users className="mx-auto mb-3 size-8 text-muted-foreground" /><p className="text-sm font-medium text-foreground">No students in this class</p></div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead><tr className="border-b border-border text-left text-xs font-medium uppercase tracking-wider text-muted-foreground"><th className="px-4 py-3">Name</th><th className="px-4 py-3">Admission No.</th><th className="px-4 py-3">Email</th></tr></thead>
                <tbody>{students.map((s) => (<tr key={s.id} className="border-b border-border last:border-0"><td className="px-4 py-3 font-medium text-foreground">{s.firstName} {s.lastName}</td><td className="px-4 py-3 text-muted-foreground">{s.admissionNumber}</td><td className="px-4 py-3 text-muted-foreground">{s.email || "—"}</td></tr>))}</tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {activeTab === "attendance" && (
        <div className="rounded-2xl border border-border bg-card shadow-xs">
          {summaries.length === 0 ? (
            <div className="py-12 text-center"><Clock className="mx-auto mb-3 size-8 text-muted-foreground" /><p className="text-sm font-medium text-foreground">No attendance data yet</p></div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead><tr className="border-b border-border text-left text-xs font-medium uppercase tracking-wider text-muted-foreground"><th className="px-4 py-3">Student</th><th className="px-4 py-3">Present</th><th className="px-4 py-3">Absent</th><th className="px-4 py-3">Late</th><th className="px-4 py-3">Rate</th></tr></thead>
                <tbody>{summaries.map((s) => (<tr key={s.studentId} className="border-b border-border last:border-0"><td className="px-4 py-3 font-medium text-foreground">{s.studentName}</td><td className="px-4 py-3 text-teal">{s.presentDays}</td><td className="px-4 py-3 text-red-500">{s.absentDays}</td><td className="px-4 py-3 text-orange">{s.lateDays}</td><td className="px-4 py-3 font-medium text-foreground">{Math.round(s.attendancePercentage)}%</td></tr>))}</tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {activeTab === "lessons" && (
        <div className="rounded-2xl border border-border bg-card shadow-xs">
          {lessons.length === 0 ? (
            <div className="py-12 text-center"><BookOpen className="mx-auto mb-3 size-8 text-muted-foreground" /><p className="text-sm font-medium text-foreground">No lessons yet</p><p className="mt-1 text-xs text-muted-foreground">Create lessons from the Lessons page.</p></div>
          ) : (
            <div className="divide-y divide-border">{lessons.sort((a, b) => a.sortOrder - b.sortOrder).map((l) => (<div key={l.id} className="flex items-center justify-between px-4 py-3"><div className="flex items-center gap-3"><span className="flex size-7 items-center justify-center rounded-lg bg-primary/10 text-xs font-bold text-primary">{l.sortOrder}</span><div><p className="text-sm font-medium text-foreground">{l.title}</p>{l.description && <p className="text-xs text-muted-foreground line-clamp-1">{l.description}</p>}</div></div><span className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${l.isPublished ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-600"}`}>{l.isPublished ? "Published" : "Draft"}</span></div>))}</div>
          )}
        </div>
      )}

      {activeTab === "assignments" && (
        <div className="rounded-2xl border border-border bg-card shadow-xs">
          {assignments.length === 0 ? (
            <div className="py-12 text-center"><FileText className="mx-auto mb-3 size-8 text-muted-foreground" /><p className="text-sm font-medium text-foreground">No assignments yet</p></div>
          ) : (
            <div className="divide-y divide-border">{assignments.map((a) => (<div key={a.id} className="flex items-center justify-between px-4 py-3"><div><p className="text-sm font-medium text-foreground">{a.title}</p>{a.description && <p className="text-xs text-muted-foreground line-clamp-1">{a.description}</p>}</div><div className="text-right text-xs text-muted-foreground"><p>{a.totalMarks} marks</p>{a.dueDate && <p>Due: {new Date(a.dueDate).toLocaleDateString()}</p>}</div></div>))}</div>
          )}
        </div>
      )}

      {activeTab === "assessments" && (
        <div className="rounded-2xl border border-border bg-card shadow-xs">
          {assessments.length === 0 ? (
            <div className="py-12 text-center"><PenTool className="mx-auto mb-3 size-8 text-muted-foreground" /><p className="text-sm font-medium text-foreground">No assessments yet</p></div>
          ) : (
            <div className="divide-y divide-border">{assessments.map((a) => (<div key={a.id} className="flex items-center justify-between px-4 py-3"><div><p className="text-sm font-medium text-foreground">{a.title}</p>{a.description && <p className="text-xs text-muted-foreground line-clamp-1">{a.description}</p>}</div><div className="text-right text-xs text-muted-foreground"><p>{a.totalMarks} marks</p>{a.timeLimitMinutes > 0 && <p>{a.timeLimitMinutes} min</p>}</div></div>))}</div>
          )}
        </div>
      )}
    </div>
  )
}
