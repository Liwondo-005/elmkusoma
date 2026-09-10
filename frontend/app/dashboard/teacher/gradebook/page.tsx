"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import {
  teacherApi,
  learningApi,
  assessmentApi,
  type TeacherClassGroup,
  type TeacherStudent,
  type Assignment,
  type Assessment,
  type AssignmentSubmission,
  type AssessmentResult,
} from "@/lib/api"
import { Button } from "@/components/ui/button"
import { BarChart3, BookOpen, PenTool, Users, AlertCircle } from "lucide-react"

interface GradebookSubmission {
  studentId: string
  grades: Record<string, number | undefined>
}

export default function TeacherGradebookPage() {
  const { user } = useAuth()
  const [classes, setClasses] = useState<TeacherClassGroup[]>([])
  const [selectedClass, setSelectedClass] = useState<string | null>(null)
  const [loadingClasses, setLoadingClasses] = useState(true)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [students, setStudents] = useState<TeacherStudent[]>([])
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [assessments, setAssessments] = useState<Assessment[]>([])
  const [assignmentGrades, setAssignmentGrades] = useState<
    Record<string, Record<string, number | undefined>>
  >({})
  const [assessmentScores, setAssessmentScores] = useState<
    Record<string, Record<string, number | undefined>>
  >({})

  useEffect(() => {
    if (!user) return
    loadClasses()
  }, [user])

  useEffect(() => {
    if (!selectedClass) return
    loadGradebook(selectedClass)
  }, [selectedClass])

  async function loadClasses() {
    try {
      setLoadingClasses(true)
      setError(null)
      const data = await teacherApi.getClasses()
      setClasses(data)
      if (data.length > 0) {
        setSelectedClass(data[0].classGroupId)
      }
    } catch {
      setError("Failed to load classes")
      setClasses([])
    } finally {
      setLoadingClasses(false)
    }
  }

  async function loadGradebook(classGroupId: string) {
    try {
      setLoading(true)
      setError(null)
      setStudents([])
      setAssignments([])
      setAssessments([])
      setAssignmentGrades({})
      setAssessmentScores({})

      const allStudents = await teacherApi.getStudents()
      const classStudents = allStudents.filter(
        (s) => s.classGroupId === classGroupId
      )
      setStudents(classStudents)

      const [assignmentsData, assessmentsData] = await Promise.allSettled([
        learningApi.getAssignments(classGroupId),
        assessmentApi.getByClass(classGroupId),
      ])

      const loadedAssignments =
        assignmentsData.status === "fulfilled" ? assignmentsData.value : []
      const loadedAssessments =
        assessmentsData.status === "fulfilled" ? assessmentsData.value : []

      setAssignments(loadedAssignments)
      setAssessments(loadedAssessments)

      const subGrades: Record<string, Record<string, number | undefined>> = {}
      for (const assignment of loadedAssignments) {
        try {
          const submissions = await learningApi.getSubmissions(assignment.id)
          for (const sub of submissions) {
            if (!subGrades[sub.studentId]) subGrades[sub.studentId] = {}
            subGrades[sub.studentId][assignment.id] = sub.grade
          }
        } catch {
          // skip failed assignment submissions
        }
      }
      setAssignmentGrades(subGrades)

      const resScores: Record<string, Record<string, number | undefined>> = {}
      for (const assessment of loadedAssessments) {
        try {
          const results = await assessmentApi.getResults(assessment.id)
          for (const result of results) {
            if (!resScores[result.studentId]) resScores[result.studentId] = {}
            resScores[result.studentId][assessment.id] = result.totalScore
          }
        } catch {
          // skip failed assessment results
        }
      }
      setAssessmentScores(resScores)
    } catch {
      setError("Failed to load gradebook data")
    } finally {
      setLoading(false)
    }
  }

  function getStudentAverage(studentId: string): string {
    const values: number[] = []

    for (const a of assignments) {
      const g = assignmentGrades[studentId]?.[a.id]
      if (g !== undefined && g !== null) {
        values.push(g)
      }
    }
    for (const asmt of assessments) {
      const s = assessmentScores[studentId]?.[asmt.id]
      if (s !== undefined && s !== null) {
        values.push(s)
      }
    }

    if (values.length === 0) return "-"
    const avg = values.reduce((sum, v) => sum + v, 0) / values.length
    return avg.toFixed(1)
  }

  const selectedClassName =
    classes.find((c) => c.classGroupId === selectedClass)?.className || ""

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          Gradebook
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          View and track student grades across assignments and assessments.
        </p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      <div className="flex items-center gap-3">
        {loadingClasses ? (
          <div className="size-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
        ) : classes.length === 0 ? (
          <p className="text-sm text-muted-foreground">No classes found.</p>
        ) : (
          <select
            value={selectedClass || ""}
            onChange={(e) => setSelectedClass(e.target.value)}
            className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
          >
            {classes.map((c) => (
              <option key={c.classGroupId} value={c.classGroupId}>
                {c.className} - {c.subjectName}
              </option>
            ))}
          </select>
        )}
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : !selectedClass ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <BarChart3 className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">
            Select a Class
          </h3>
          <p className="mt-2 text-sm text-muted-foreground">
            Choose a class to view the gradebook.
          </p>
        </div>
      ) : students.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <Users className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">
            No Students
          </h3>
          <p className="mt-2 text-sm text-muted-foreground">
            No students are enrolled in this class.
          </p>
        </div>
      ) : assignments.length === 0 && assessments.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <BookOpen className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">
            No Grade Data
          </h3>
          <p className="mt-2 text-sm text-muted-foreground">
            No assignments or assessments found for this class.
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-3">
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <Users className="mb-2 size-5 text-muted-foreground" />
              <p className="text-2xl font-extrabold text-foreground">
                {students.length}
              </p>
              <p className="text-sm font-medium text-foreground">Students</p>
              <p className="text-xs text-muted-foreground">{selectedClassName}</p>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <BookOpen className="mb-2 size-5 text-muted-foreground" />
              <p className="text-2xl font-extrabold text-foreground">
                {assignments.length}
              </p>
              <p className="text-sm font-medium text-foreground">Assignments</p>
              <p className="text-xs text-muted-foreground">Total created</p>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <PenTool className="mb-2 size-5 text-muted-foreground" />
              <p className="text-2xl font-extrabold text-foreground">
                {assessments.length}
              </p>
              <p className="text-sm font-medium text-foreground">Assessments</p>
              <p className="text-xs text-muted-foreground">Total created</p>
            </div>
          </div>

          <div className="overflow-hidden rounded-2xl border border-border bg-card">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-border bg-muted/50">
                  <tr>
                    <th className="sticky left-0 z-10 bg-muted/50 px-4 py-3 font-medium text-muted-foreground min-w-[160px]">
                      Student Name
                    </th>
                    {assignments.map((a) => (
                      <th
                        key={a.id}
                        className="px-4 py-3 font-medium text-muted-foreground min-w-[100px] text-center"
                      >
                        <div className="flex flex-col items-center">
                          <span className="text-xs">{a.title}</span>
                          <span className="text-[10px] text-muted-foreground/70">
                            /{a.totalMarks}
                          </span>
                        </div>
                      </th>
                    ))}
                    {assessments.map((a) => (
                      <th
                        key={a.id}
                        className="px-4 py-3 font-medium text-muted-foreground min-w-[100px] text-center"
                      >
                        <div className="flex flex-col items-center">
                          <span className="text-xs">{a.title}</span>
                          <span className="text-[10px] text-muted-foreground/70">
                            /{a.totalMarks}
                          </span>
                        </div>
                      </th>
                    ))}
                    <th className="px-4 py-3 font-medium text-muted-foreground min-w-[80px] text-center">
                      Average
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {students.map((student) => {
                    const avg = getStudentAverage(student.studentId)
                    return (
                      <tr
                        key={student.studentId}
                        className="hover:bg-muted/30"
                      >
                        <td className="sticky left-0 z-10 bg-card px-4 py-3">
                          <p className="font-medium text-foreground">
                            {student.fullName}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            {student.admissionNumber}
                          </p>
                        </td>
                        {assignments.map((a) => {
                          const grade =
                            assignmentGrades[student.studentId]?.[a.id]
                          return (
                            <td
                              key={a.id}
                              className="px-4 py-3 text-center"
                            >
                              {grade !== undefined && grade !== null ? (
                                <span className="inline-flex items-center rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
                                  {grade}
                                </span>
                              ) : (
                                <span className="text-muted-foreground/50">
                                  -
                                </span>
                              )}
                            </td>
                          )
                        })}
                        {assessments.map((a) => {
                          const score =
                            assessmentScores[student.studentId]?.[a.id]
                          return (
                            <td
                              key={a.id}
                              className="px-4 py-3 text-center"
                            >
                              {score !== undefined && score !== null ? (
                                <span
                                  className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                                    score >= a.passMarks
                                      ? "bg-teal/10 text-teal"
                                      : "bg-destructive/10 text-destructive"
                                  }`}
                                >
                                  {score}
                                </span>
                              ) : (
                                <span className="text-muted-foreground/50">
                                  -
                                </span>
                              )}
                            </td>
                          )
                        })}
                        <td className="px-4 py-3 text-center">
                          <span className="text-sm font-semibold text-foreground">
                            {avg}
                          </span>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
