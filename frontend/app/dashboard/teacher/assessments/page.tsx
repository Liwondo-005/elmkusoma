"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { assessmentApi, assessmentCreateApi, teacherApi, type Assessment, type AssessmentResult, type Question, type TeacherClassGroup, type CreateAssessmentRequest } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { PenTool, Plus, Eye, BarChart3, X, AlertCircle, CheckCircle, List, Trash2, GripVertical } from "lucide-react"

export default function TeacherAssessmentsPage() {
  const { user } = useAuth()
  const t = useTranslations("teacher")
  const tn = useTranslations("nav")
  const tc = useTranslations("common")
  const ts = useTranslations("status")
  const [assessments, setAssessments] = useState<Assessment[]>([])
  const [classes, setClasses] = useState<TeacherClassGroup[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedAssessment, setSelectedAssessment] = useState<string | null>(null)
  const [results, setResults] = useState<AssessmentResult[]>([])
  const [loadingResults, setLoadingResults] = useState(false)
  const [showCreate, setShowCreate] = useState(false)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const [questionsAssessmentId, setQuestionsAssessmentId] = useState<string | null>(null)
  const [questions, setQuestions] = useState<Question[]>([])
  const [loadingQuestions, setLoadingQuestions] = useState(false)
  const [showAddQuestion, setShowAddQuestion] = useState(false)
  const [questionType, setQuestionType] = useState<"MCQ" | "TRUE_FALSE" | "SHORT_ANSWER" | "ESSAY">("MCQ")
  const [questionText, setQuestionText] = useState("")
  const [questionMarks, setQuestionMarks] = useState(10)
  const [questionOptions, setQuestionOptions] = useState<{ text: string; isCorrect: boolean }[]>([
    { text: "", isCorrect: false },
    { text: "", isCorrect: false },
    { text: "", isCorrect: false },
    { text: "", isCorrect: false },
  ])
  const [addingQuestion, setAddingQuestion] = useState(false)

  const [form, setForm] = useState<CreateAssessmentRequest>({
    subjectId: "",
    classGroupId: "",
    title: "",
    description: "",
    totalMarks: 100,
    passMarks: 50,
    timeLimitMinutes: 60,
  })

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const classesData = await teacherApi.getClasses()
      setClasses(classesData)

      const allAssessments: Assessment[] = []
      for (const cls of classesData) {
        try {
          const data = await assessmentApi.getByClass(cls.classGroupId)
          allAssessments.push(...data)
        } catch {
          // skip
        }
      }
      setAssessments(allAssessments)
    } catch {
      setError(t("assessments.loadError"))
    } finally {
      setLoading(false)
    }
  }

  async function viewResults(assessmentId: string) {
    try {
      setLoadingResults(true)
      setSelectedAssessment(assessmentId)
      const data = await assessmentApi.getResults(assessmentId)
      setResults(data)
    } catch {
      setResults([])
    } finally {
      setLoadingResults(false)
    }
  }

  async function loadQuestions(assessmentId: string) {
    try {
      setLoadingQuestions(true)
      setQuestionsAssessmentId(assessmentId)
      setSelectedAssessment(null)
      const data = await assessmentApi.getQuestions(assessmentId)
      setQuestions(data)
    } catch {
      setQuestions([])
    } finally {
      setLoadingQuestions(false)
    }
  }

  async function handleAddQuestion() {
    if (!questionsAssessmentId || !questionText.trim()) return
    try {
      setAddingQuestion(true)
      setError(null)
      const payload: Record<string, unknown> = {
        questionType,
        questionText: questionText.trim(),
        marks: questionMarks,
        sortOrder: questions.length + 1,
      }
      if (questionType === "MCQ" || questionType === "TRUE_FALSE") {
        const opts = (questionType === "TRUE_FALSE"
          ? [{ optionText: "True", isCorrect: questionOptions[0]?.isCorrect ?? false }, { optionText: "False", isCorrect: questionOptions[1]?.isCorrect ?? false }]
          : questionOptions.filter(o => o.text.trim()).map(o => ({ optionText: o.text.trim(), isCorrect: o.isCorrect }))
        ).map((o, i) => ({ optionText: o.optionText, isCorrect: o.isCorrect, sortOrder: i + 1 }))
        payload.options = opts
      }
      await assessmentApi.addQuestion(questionsAssessmentId, payload as Partial<Question>)
      setSuccess(t("assessments.questionAdded"))
      setShowAddQuestion(false)
      resetQuestionForm()
      await loadQuestions(questionsAssessmentId)
      setTimeout(() => setSuccess(null), 2000)
    } catch {
      setError(t("assessments.addQuestionError"))
    } finally {
      setAddingQuestion(false)
    }
  }

  function resetQuestionForm() {
    setQuestionText("")
    setQuestionMarks(10)
    setQuestionType("MCQ")
    setQuestionOptions([
      { text: "", isCorrect: false },
      { text: "", isCorrect: false },
      { text: "", isCorrect: false },
      { text: "", isCorrect: false },
    ])
  }

  async function handleCreate() {
    if (!form.title || !form.classGroupId || !form.subjectId) return
    try {
      setCreating(true)
      setError(null)
      await assessmentCreateApi.create(form)
      setSuccess(t("assessments.createdSuccess"))
      setShowCreate(false)
      setForm({ subjectId: "", classGroupId: "", title: "", description: "", totalMarks: 100, passMarks: 50, timeLimitMinutes: 60 })
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch {
      setError(t("assessments.createError"))
    } finally {
      setCreating(false)
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("assessments")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("assessments.subtitle")}</p>
        </div>
        <Button className="gap-2" onClick={() => { setShowCreate(!showCreate); setQuestionsAssessmentId(null) }}>
          {showCreate ? <X className="size-4" /> : <Plus className="size-4" />}
          {showCreate ? tc("cancel") : t("assessments.createAssessment")}
        </Button>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />{error}
          </div>
        </div>
      )}

      {success && (
        <div className="rounded-2xl border border-teal/20 bg-teal/5 p-4">
          <div className="flex items-center gap-2 text-sm text-teal">
            <CheckCircle className="size-4" />{success}
          </div>
        </div>
      )}

      {showCreate && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <h2 className="text-base font-semibold text-foreground">{t("assessments.newAssessment")}</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("lessons.classLabel")}</label>
              <select
                value={form.classGroupId}
                onChange={(e) => {
                  const cls = classes.find(c => c.classGroupId === e.target.value)
                  setForm({ ...form, classGroupId: e.target.value, subjectId: cls?.subjectId || "" })
                }}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              >
                <option value="">{t("lessons.selectClassOption")}</option>
                {classes.map(c => (
                  <option key={c.classGroupId} value={c.classGroupId}>{c.className} - {c.subjectName}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("announcements.titleLabel")}</label>
              <input
                type="text"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder={t("assessments.titlePlaceholder")}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("lessons.descLabel")}</label>
              <textarea
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder={t("assessments.descPlaceholder")}
                rows={3}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("assessments.totalMarks")}</label>
              <input
                type="number"
                value={form.totalMarks}
                onChange={(e) => setForm({ ...form, totalMarks: Number(e.target.value) })}
                min={1}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("assessments.passMarks")}</label>
              <input
                type="number"
                value={form.passMarks}
                onChange={(e) => setForm({ ...form, passMarks: Number(e.target.value) })}
                min={0}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("assessments.timeLimit")}</label>
              <input
                type="number"
                value={form.timeLimitMinutes}
                onChange={(e) => setForm({ ...form, timeLimitMinutes: Number(e.target.value) })}
                min={1}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={() => setShowCreate(false)}>{tc("cancel")}</Button>
            <Button onClick={handleCreate} disabled={creating || !form.title || !form.classGroupId}>
              {creating ? t("assessments.creating") : t("assessments.createAssessment")}
            </Button>
          </div>
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : assessments.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <PenTool className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("assessments.emptyTitle")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">{t("assessments.emptyDesc")}</p>
        </div>
      ) : (
        <div className="space-y-3">
          {assessments.map((a) => (
            <div key={a.id} className="rounded-2xl border border-border bg-card p-4 transition-all hover:shadow-sm">
              <div className="flex items-center gap-4">
                <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 shrink-0">
                  <PenTool className="size-5 text-primary" />
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-sm font-semibold text-foreground">{a.title}</h3>
                  {a.description && (
                    <p className="mt-0.5 text-xs text-muted-foreground line-clamp-1">{a.description}</p>
                  )}
                  <div className="mt-1.5 flex items-center gap-3 text-xs text-muted-foreground">
                    <span>{t("classDetail.marksCount", { count: a.totalMarks })}</span>
                    <span>{t("assessments.passMarksLabel", { marks: a.passMarks })}</span>
                    {a.timeLimitMinutes && <span>{t("classDetail.minutesCount", { count: a.timeLimitMinutes })}</span>}
                  </div>
                </div>
                <div className="flex gap-2 shrink-0">
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => loadQuestions(a.id)}
                    className="gap-1"
                  >
                    <List className="size-3" /> {t("assessments.questionsBtn")}
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => viewResults(a.id)}
                    className="gap-1"
                  >
                    <BarChart3 className="size-3" /> {t("assessments.resultsBtn")}
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {questionsAssessmentId && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">{t("assessments.manageQuestions")}</h2>
            <div className="flex gap-2">
              <Button size="sm" className="gap-1" onClick={() => setShowAddQuestion(!showAddQuestion)}>
                {showAddQuestion ? <X className="size-3" /> : <Plus className="size-3" />}
                {showAddQuestion ? tc("cancel") : t("assessments.addQuestion")}
              </Button>
              <Button variant="ghost" size="sm" onClick={() => { setQuestionsAssessmentId(null); setQuestions([]) }}>
                {tc("close")}
              </Button>
            </div>
          </div>

          {showAddQuestion && (
            <div className="rounded-xl border border-border bg-background p-4 space-y-3">
              <div className="grid gap-3 sm:grid-cols-3">
                <div>
                  <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("assessments.typeLabel")}</label>
                  <select
                    value={questionType}
                    onChange={(e) => setQuestionType(e.target.value as typeof questionType)}
                    className="h-9 w-full rounded-lg border border-border bg-background px-2 text-sm outline-none focus:border-ring"
                  >
                    <option value="MCQ">{t("assessments.qtypeMcq")}</option>
                    <option value="TRUE_FALSE">{t("assessments.qtypeTf")}</option>
                    <option value="SHORT_ANSWER">{t("assessments.qtypeShort")}</option>
                    <option value="ESSAY">{t("assessments.qtypeEssay")}</option>
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("assessments.marksLabel")}</label>
                  <input
                    type="number"
                    value={questionMarks}
                    onChange={(e) => setQuestionMarks(Number(e.target.value))}
                    min={1}
                    className="h-9 w-full rounded-lg border border-border bg-background px-2 text-sm outline-none focus:border-ring"
                  />
                </div>
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("assessments.questionTextLabel")}</label>
                <textarea
                  value={questionText}
                  onChange={(e) => setQuestionText(e.target.value)}
                  placeholder={t("assessments.questionPlaceholder")}
                  rows={2}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
                />
              </div>
              {(questionType === "MCQ") && (
                <div className="space-y-2">
                  <label className="block text-xs font-medium text-muted-foreground">{t("assessments.optionsLabel")}</label>
                  {questionOptions.map((opt, i) => (
                    <div key={i} className="flex items-center gap-2">
                      <input
                        type="radio"
                        name="correctOption"
                        checked={opt.isCorrect}
                        onChange={() => {
                          const updated = questionOptions.map((o, j) => ({ ...o, isCorrect: j === i }))
                          setQuestionOptions(updated)
                        }}
                        className="size-4 shrink-0"
                      />
                      <input
                        type="text"
                        value={opt.text}
                        onChange={(e) => {
                          const updated = [...questionOptions]
                          updated[i] = { ...updated[i], text: e.target.value }
                          setQuestionOptions(updated)
                        }}
                        placeholder={t("assessments.optionPlaceholder", { index: i + 1 })}
                        className="h-9 flex-1 rounded-lg border border-border bg-background px-2 text-sm outline-none focus:border-ring"
                      />
                    </div>
                  ))}
                </div>
              )}
              {questionType === "TRUE_FALSE" && (
                <div className="space-y-2">
                  <label className="block text-xs font-medium text-muted-foreground">{t("assessments.correctAnswer")}</label>
                  <div className="flex gap-4">
                    <label className="flex items-center gap-2 text-sm">
                      <input
                        type="radio"
                        name="tfAnswer"
                        checked={questionOptions[0]?.isCorrect ?? false}
                        onChange={() => setQuestionOptions([{ text: "True", isCorrect: true }, { text: "False", isCorrect: false }])}
                      /> {t("assessments.trueLabel")}
                    </label>
                    <label className="flex items-center gap-2 text-sm">
                      <input
                        type="radio"
                        name="tfAnswer"
                        checked={questionOptions[1]?.isCorrect ?? false}
                        onChange={() => setQuestionOptions([{ text: "True", isCorrect: false }, { text: "False", isCorrect: true }])}
                      /> {t("assessments.falseLabel")}
                    </label>
                  </div>
                </div>
              )}
              <div className="flex justify-end">
                <Button onClick={handleAddQuestion} disabled={addingQuestion || !questionText.trim()}>
                  {addingQuestion ? t("assessments.adding") : t("assessments.addQuestion")}
                </Button>
              </div>
            </div>
          )}

          {loadingQuestions ? (
            <div className="flex justify-center py-8">
              <div className="size-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            </div>
          ) : questions.length === 0 ? (
            <p className="py-4 text-center text-sm text-muted-foreground">{t("assessments.noQuestions")}</p>
          ) : (
            <div className="space-y-2">
              {questions.map((q, i) => (
                <div key={q.id} className="rounded-xl border border-border p-3">
                  <div className="flex items-start gap-3">
                    <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-medium text-muted-foreground">
                      {i + 1}
                    </span>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-foreground">{q.questionText}</p>
                      <div className="mt-1 flex items-center gap-3 text-xs text-muted-foreground">
                        <span className="rounded bg-muted px-1.5 py-0.5 font-medium">{q.questionType}</span>
                        <span>{t("classDetail.marksCount", { count: q.marks })}</span>
                      </div>
                      {q.options && q.options.length > 0 && (
                        <div className="mt-2 space-y-1">
                          {q.options.map((o) => (
                            <div key={o.id} className={`flex items-center gap-2 text-xs ${o.isCorrect ? "font-medium text-teal" : "text-muted-foreground"}`}>
                              <span className="size-1.5 shrink-0 rounded-full bg-current" />
                              {o.optionText}
                              {o.isCorrect && <span className="text-[10px]">{t("assessments.correctMark")}</span>}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {selectedAssessment && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">{t("assessments.resultsTitle")}</h2>
            <Button variant="ghost" size="sm" onClick={() => { setSelectedAssessment(null); setResults([]) }}>
              {tc("close")}
            </Button>
          </div>
          {loadingResults ? (
            <div className="flex justify-center py-8">
              <div className="size-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            </div>
          ) : results.length === 0 ? (
            <p className="py-4 text-center text-sm text-muted-foreground">{t("assessments.noResults")}</p>
          ) : (
            <div className="mt-4 overflow-hidden rounded-xl border border-border">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-border bg-muted/50">
                  <tr>
                    <th className="px-4 py-3 font-medium text-muted-foreground">{t("gradebook.colStudent")}</th>
                    <th className="px-4 py-3 font-medium text-muted-foreground">{t("assessments.colScore")}</th>
                    <th className="px-4 py-3 font-medium text-muted-foreground">{t("grading.colStatus")}</th>
                    <th className="px-4 py-3 font-medium text-muted-foreground">{t("assessments.colGraded")}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {results.map((r) => (
                    <tr key={r.id}>
                      <td className="px-4 py-3 font-medium text-foreground">{r.studentId.slice(0, 8)}...</td>
                      <td className="px-4 py-3 text-foreground">{r.totalScore}</td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${r.isPassed ? "bg-teal/10 text-teal" : "bg-destructive/10 text-destructive"}`}>
                          {r.isPassed ? t("assessments.passedStatus") : t("assessments.failedStatus")}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-muted-foreground">
                        {r.gradedAt ? new Date(r.gradedAt).toLocaleDateString() : ts("pending")}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
