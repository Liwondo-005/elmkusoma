"use client"

import { useEffect, useState, useCallback, useRef } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { learningApi, type Assignment, type AssignmentSubmission } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { Button } from "@/components/ui/button"
import { ArrowLeft, Clock, CheckCircle, Send, BookOpen, FileText, Upload, Star, AlertTriangle } from "lucide-react"

export default function AssignmentDetailPage() {
  const params = useParams()
  const router = useRouter()
  const { user } = useRequireAuth()
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  const [assignment, setAssignment] = useState<Assignment | null>(null)
  const [submission, setSubmission] = useState<AssignmentSubmission | null>(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [content, setContent] = useState("")
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [filePreview, setFilePreview] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!user || !params.id) return
    loadAssignment()
  }, [user, params.id])

  async function loadAssignment() {
    try {
      setLoading(true)
      const all = await learningApi.getAssignments(user!.classGroupId || "")
      const found = all.find((a) => a.id === params.id)
      if (found) {
        setAssignment(found)
        try {
          const subs = await learningApi.getSubmissions(found.id)
          const mySub = subs.find((s) => s.studentId === user!.id)
          if (mySub) {
            setSubmission(mySub)
            if (mySub.content) setContent(mySub.content)
          }
        } catch {
          // no submissions yet
        }
      }
    } catch {
      // ignore
    } finally {
      setLoading(false)
    }
  }

  const handleFileSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setSelectedFile(file)
    const reader = new FileReader()
    reader.onload = () => setFilePreview(reader.result as string)
    reader.readAsDataURL(file)
  }, [])

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    const file = e.dataTransfer.files?.[0]
    if (!file) return
    setSelectedFile(file)
    const reader = new FileReader()
    reader.onload = () => setFilePreview(reader.result as string)
    reader.readAsDataURL(file)
  }, [])

  const handleSubmit = useCallback(async () => {
    if (!assignment || !user) return
    if (!content.trim() && !selectedFile) return
    setSubmitting(true)
    setSubmitError(null)
    try {
      const payload: Record<string, unknown> = {}
      if (content.trim()) payload.content = content.trim()
      if (selectedFile) payload.fileUrl = selectedFile.name
      await learningApi.submitAssignment(assignment.id)
      const subs = await learningApi.getSubmissions(assignment.id)
      const mySub = subs.find((s) => s.studentId === user.id)
      if (mySub) setSubmission(mySub)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to submit"
      setSubmitError(msg)
    } finally {
      setSubmitting(false)
    }
  }, [assignment, user, content, selectedFile])

  function isOverdue(dueDate?: string) {
    if (!dueDate) return false
    return new Date(dueDate) < new Date()
  }

  function getStatus() {
    if (submission?.grade !== undefined && submission?.grade !== null) return "graded"
    if (submission) return "submitted"
    return "not_started"
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  if (!assignment) {
    return (
      <div className="space-y-4">
        <Button variant="ghost" size="sm" onClick={() => router.back()}>
          <ArrowLeft className="mr-2 size-4" /> Back
        </Button>
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <BookOpen className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">Assignment Not Found</h3>
        </div>
      </div>
    )
  }

  const status = getStatus()
  const overdue = isOverdue(assignment.dueDate)
  const attachments = assignment.attachments ? assignment.attachments.split(",").map((a) => a.trim()).filter(Boolean) : []

  if (isPrimary) {
    return (
      <div className="mx-auto max-w-3xl space-y-6">
        <Link
          href="/dashboard/assignments"
          className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
        >
          <ArrowLeft className="size-4" /> Back to Practice
        </Link>

        <div className="rounded-2xl border border-border bg-gradient-to-br from-amber-50 via-card to-orange-50 p-6 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-500/10">
              <BookOpen className="size-6 text-amber-600" />
            </div>
            <div className="flex-1">
              <h1 className="text-2xl font-bold tracking-tight text-foreground">{assignment.title}</h1>
              <div className="mt-1 flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
                {assignment.dueDate && (
                  <span className="flex items-center gap-1">
                    <Clock className="size-3.5" />
                    Due: {new Date(assignment.dueDate).toLocaleDateString()}
                  </span>
                )}
                <span>{assignment.totalMarks} marks</span>
                {overdue && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-orange-100 px-2 py-0.5 text-xs font-semibold text-orange-700">
                    <AlertTriangle className="size-3" /> Overdue
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
            <FileText className="size-5 text-primary" /> What to do
          </h2>
          {assignment.description ? (
            <p className="mt-3 text-lg leading-relaxed text-muted-foreground whitespace-pre-wrap">{assignment.description}</p>
          ) : (
            <p className="mt-3 text-lg text-muted-foreground">Read the instructions from your teacher and complete the work.</p>
          )}
        </div>

        {attachments.length > 0 && (
          <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
            <h2 className="text-lg font-semibold text-foreground">Files from your teacher</h2>
            <div className="mt-3 space-y-2">
              {attachments.map((att, i) => (
                <a
                  key={i}
                  href={att}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-3 rounded-xl border border-border bg-muted/40 p-3 text-sm font-medium text-primary hover:bg-muted transition-colors"
                >
                  <FileText className="size-4 shrink-0" />
                  <span className="truncate">{att.split("/").pop() || att}</span>
                </a>
              ))}
            </div>
          </div>
        )}

        {status === "graded" && submission ? (
          <div className="rounded-2xl border-2 border-emerald-200 bg-emerald-50 p-6 shadow-xs">
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-emerald-500/10">
                <CheckCircle className="size-6 text-emerald-600" />
              </div>
              <div>
                <h2 className="text-lg font-bold text-emerald-800">Your work has been graded!</h2>
                <p className="text-sm text-emerald-700">
                  You got <span className="font-bold text-xl">{submission.grade}</span> out of {assignment.totalMarks}
                </p>
              </div>
            </div>
            {submission.feedback && (
              <div className="mt-4 rounded-xl bg-white/60 p-4">
                <p className="text-sm font-semibold text-emerald-800">Teacher says:</p>
                <p className="mt-1 text-base text-emerald-900">{submission.feedback}</p>
              </div>
            )}
          </div>
        ) : status === "submitted" ? (
          <div className="rounded-2xl border-2 border-blue-200 bg-blue-50 p-6 shadow-xs">
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-blue-500/10">
                <CheckCircle className="size-6 text-blue-600" />
              </div>
              <div>
                <h2 className="text-lg font-bold text-blue-800">Work Submitted!</h2>
                <p className="text-sm text-blue-700">Your teacher will check it soon. Great job!</p>
              </div>
            </div>
          </div>
        ) : (
          <div className="rounded-2xl border border-border bg-card p-6 shadow-xs space-y-5">
            <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
              <Star className="size-5 text-amber-500" /> Send your work
            </h2>

            <div>
              <label className="block text-base font-medium text-foreground mb-2">
                Write your answer here
              </label>
              <textarea
                rows={6}
                placeholder="Type your answer here... You can write as much as you need."
                value={content}
                onChange={(e) => setContent(e.target.value)}
                className="w-full rounded-xl border border-border bg-muted/60 px-4 py-3 text-lg text-foreground outline-none focus:border-ring focus:bg-background resize-none transition-colors"
              />
            </div>

            <div>
              <label className="block text-base font-medium text-foreground mb-2">
                Upload a photo of your work
              </label>
              <div
                onDragOver={(e) => e.preventDefault()}
                onDrop={handleDrop}
                onClick={() => fileInputRef.current?.click()}
                className="flex cursor-pointer flex-col items-center justify-center gap-3 rounded-2xl border-2 border-dashed border-border bg-muted/30 px-6 py-10 text-center transition-colors hover:border-primary/40 hover:bg-primary/5"
              >
                {filePreview ? (
                  <div className="space-y-3">
                    {selectedFile?.type.startsWith("image/") ? (
                      <img src={filePreview} alt="Preview" className="mx-auto max-h-40 rounded-xl object-contain" />
                    ) : (
                      <FileText className="mx-auto size-10 text-primary" />
                    )}
                    <p className="text-sm font-medium text-foreground">{selectedFile?.name}</p>
                    <p className="text-xs text-muted-foreground">Click to change file</p>
                  </div>
                ) : (
                  <>
                    <div className="flex size-14 items-center justify-center rounded-2xl bg-primary/10">
                      <Upload className="size-7 text-primary" />
                    </div>
                    <div>
                      <p className="text-base font-medium text-foreground">Tap to upload a photo or file</p>
                      <p className="mt-1 text-sm text-muted-foreground">Images and PDFs work best</p>
                    </div>
                  </>
                )}
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*,.pdf"
                  onChange={handleFileSelect}
                  className="hidden"
                />
              </div>
            </div>

            {submitError && (
              <div className="rounded-xl bg-red-50 border border-red-200 p-3 text-sm text-red-700">
                {submitError}
              </div>
            )}

            <button
              onClick={handleSubmit}
              disabled={submitting || (!content.trim() && !selectedFile)}
              className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-6 py-3.5 text-base font-bold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {submitting ? (
                <div className="size-5 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent" />
              ) : (
                <Send className="size-5" />
              )}
              Submit My Work
            </button>
          </div>
        )}
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" onClick={() => router.back()} className="w-fit">
        <ArrowLeft className="mr-2 size-4" /> Back to Assignments
      </Button>

      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{assignment.title}</h1>
            <div className="mt-2 flex items-center gap-3 text-sm text-muted-foreground">
              {assignment.dueDate && (
                <span className={`flex items-center gap-1 ${overdue ? "text-destructive" : ""}`}>
                  <Clock className="size-3.5" />
                  Due: {new Date(assignment.dueDate).toLocaleDateString()}
                </span>
              )}
              <span>Total: {assignment.totalMarks} marks</span>
            </div>
          </div>
          <div>
            {status === "graded" ? (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
                <CheckCircle className="size-3" /> Graded
              </span>
            ) : status === "submitted" ? (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-blue-100 px-3 py-1 text-xs font-medium text-blue-700">
                <CheckCircle className="size-3" /> Submitted
              </span>
            ) : overdue ? (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-red-100 px-3 py-1 text-xs font-medium text-red-700">
                <Clock className="size-3" /> Overdue
              </span>
            ) : (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
                Not Started
              </span>
            )}
          </div>
        </div>

        {assignment.description && (
          <div className="mt-4 rounded-xl bg-muted/50 p-4 text-sm leading-relaxed text-foreground whitespace-pre-wrap">
            {assignment.description}
          </div>
        )}

        {attachments.length > 0 && (
          <div className="mt-4 rounded-xl border border-border bg-muted/30 p-4">
            <h3 className="text-sm font-semibold text-foreground">Attachments</h3>
            <div className="mt-2 space-y-1">
              {attachments.map((att, i) => (
                <a
                  key={i}
                  href={att}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-2 text-sm text-primary hover:underline"
                >
                  <FileText className="size-3.5" />
                  {att.split("/").pop() || att}
                </a>
              ))}
            </div>
          </div>
        )}
      </div>

      {status === "graded" && submission ? (
        <div className="rounded-2xl border border-primary/20 bg-primary/5 p-6">
          <h2 className="text-lg font-semibold text-foreground">Results</h2>
          <div className="mt-3 space-y-2 text-sm">
            <div className="flex items-center justify-between rounded-xl bg-muted/50 px-4 py-2">
              <span className="text-muted-foreground">Score</span>
              <span className="font-bold text-foreground">{submission.grade} / {assignment.totalMarks}</span>
            </div>
            {submission.feedback && (
              <div className="rounded-xl bg-muted/30 p-4">
                <p className="text-xs font-semibold text-muted-foreground">Feedback</p>
                <p className="mt-1 text-sm text-foreground">{submission.feedback}</p>
              </div>
            )}
          </div>
        </div>
      ) : status === "submitted" ? (
        <div className="rounded-2xl border border-blue-200 bg-blue-50 p-6 text-center">
          <CheckCircle className="mx-auto size-10 text-blue-600" />
          <h3 className="mt-3 text-lg font-semibold text-foreground">Submitted</h3>
          <p className="mt-1 text-sm text-muted-foreground">Your submission is pending review.</p>
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card p-6 space-y-4">
          <h2 className="text-lg font-semibold text-foreground">Submit Your Work</h2>

          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Written Answer</label>
            <textarea
              rows={5}
              placeholder="Type your answer here..."
              value={content}
              onChange={(e) => setContent(e.target.value)}
              className="w-full rounded-xl border border-border bg-muted/60 px-4 py-3 text-sm text-foreground outline-none focus:border-ring focus:bg-background resize-none"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Upload File (optional)</label>
            <div
              onDragOver={(e) => e.preventDefault()}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
              className="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed border-border bg-muted/30 px-4 py-8 text-center transition-colors hover:border-primary/40 hover:bg-primary/5"
            >
              {filePreview ? (
                <div className="space-y-2">
                  {selectedFile?.type.startsWith("image/") ? (
                    <img src={filePreview} alt="Preview" className="mx-auto max-h-32 rounded-lg object-contain" />
                  ) : (
                    <FileText className="mx-auto size-8 text-primary" />
                  )}
                  <p className="text-xs font-medium text-foreground">{selectedFile?.name}</p>
                </div>
              ) : (
                <>
                  <Upload className="size-8 text-muted-foreground" />
                  <p className="text-xs text-muted-foreground">Drag and drop or click to upload</p>
                </>
              )}
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*,.pdf"
                onChange={handleFileSelect}
                className="hidden"
              />
            </div>
          </div>

          {submitError && (
            <div className="rounded-lg bg-destructive/10 p-3 text-xs text-destructive">{submitError}</div>
          )}

          <Button
            onClick={handleSubmit}
            disabled={submitting || (!content.trim() && !selectedFile)}
            className="w-full gap-2"
          >
            {submitting ? (
              <div className="size-4 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent" />
            ) : (
              <Send className="size-4" />
            )}
            Submit
          </Button>
        </div>
      )}
    </div>
  )
}
