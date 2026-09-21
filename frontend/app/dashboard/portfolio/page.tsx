"use client"

import { useEffect, useState, useRef, useCallback } from "react"
import { useRequireAuth } from "@/lib/auth"
import { primaryApi, type PortfolioItem } from "@/lib/api"
import { type LearningLevel, primarySubjects } from "@/lib/learner-config"
import { Hammer, Plus, Palette, BookOpen, Wrench, Camera, Mic, FileText, Star, Trash2, X, Loader2, FolderOpen, ChevronRight, ChevronLeft, Check, Circle, Disc, Eraser, Undo2 } from "lucide-react"

const typeConfig: Record<string, { icon: typeof Palette; color: string; bgColor: string; label: string }> = {
  DRAWING: { icon: Palette, color: "text-pink-600", bgColor: "bg-pink-50", label: "Drawing" },
  STORY: { icon: BookOpen, color: "text-blue-600", bgColor: "bg-blue-50", label: "Story" },
  PROJECT: { icon: Wrench, color: "text-green-600", bgColor: "bg-green-50", label: "Project" },
  PHOTO: { icon: Camera, color: "text-amber-600", bgColor: "bg-amber-50", label: "Photo" },
  VOICE_RECORDING: { icon: Mic, color: "text-purple-600", bgColor: "bg-purple-50", label: "Voice Recording" },
  ESSAY: { icon: FileText, color: "text-teal-600", bgColor: "bg-teal-50", label: "Essay" },
}

const filterOptions = ["All", "Drawing", "Story", "Project", "Photo", "Voice Recording", "Essay"]

interface CreationActivity {
  type: string
  label: string
  icon: typeof Palette
  color: string
  bgColor: string
  borderColor: string
}

const creationActivities: CreationActivity[] = [
  { type: "DRAWING", label: "Draw Something", icon: Palette, color: "text-pink-600", bgColor: "bg-pink-50", borderColor: "border-pink-200 hover:border-pink-400" },
  { type: "STORY", label: "Write a Story", icon: BookOpen, color: "text-blue-600", bgColor: "bg-blue-50", borderColor: "border-blue-200 hover:border-blue-400" },
  { type: "PROJECT", label: "Build a Project", icon: Wrench, color: "text-green-600", bgColor: "bg-green-50", borderColor: "border-green-200 hover:border-green-400" },
  { type: "PHOTO", label: "Take a Photo", icon: Camera, color: "text-amber-600", bgColor: "bg-amber-50", borderColor: "border-amber-200 hover:border-amber-400" },
  { type: "VOICE_RECORDING", label: "Record Your Voice", icon: Mic, color: "text-purple-600", bgColor: "bg-purple-50", borderColor: "border-purple-200 hover:border-purple-400" },
  { type: "ESSAY", label: "Write an Essay", icon: FileText, color: "text-teal-600", bgColor: "bg-teal-50", borderColor: "border-teal-200 hover:border-teal-400" },
]

const drawingColors = ["#000000", "#ef4444", "#f97316", "#eab308", "#22c55e", "#3b82f6", "#8b5cf6", "#ec4899", "#ffffff"]

export default function PortfolioPage() {
  const { user } = useRequireAuth()
  const [items, setItems] = useState<PortfolioItem[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState("All")
  const [submitting, setSubmitting] = useState(false)
  const level = user?.learningLevel as LearningLevel | null

  const [wizardOpen, setWizardOpen] = useState(false)
  const [wizardStep, setWizardStep] = useState(1)
  const [wizardType, setWizardType] = useState("")
  const [wizardTitle, setWizardTitle] = useState("")
  const [wizardDescription, setWizardDescription] = useState("")
  const [wizardSubject, setWizardSubject] = useState("")
  const [wizardColor, setWizardColor] = useState("#3b82f6")
  const [wizardContent, setWizardContent] = useState("")
  const [isRecording, setIsRecording] = useState(false)
  const [recordingTimer, setRecordingTimer] = useState(0)

  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [isDrawing, setIsDrawing] = useState(false)
  const [drawTool, setDrawTool] = useState<"pencil" | "eraser">("pencil")
  const [brushSize, setBrushSize] = useState(4)
  const [drawingHistory, setDrawingHistory] = useState<ImageData[]>([])

  useEffect(() => {
    if (!user) return
    loadPortfolio()
  }, [user])

  useEffect(() => {
    let interval: NodeJS.Timeout | undefined
    if (isRecording) {
      interval = setInterval(() => {
        setRecordingTimer((prev) => prev + 1)
      }, 1000)
    }
    return () => {
      if (interval) clearInterval(interval)
    }
  }, [isRecording])

  const initCanvas = useCallback(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const rect = canvas.parentElement?.getBoundingClientRect()
    if (!rect) return
    canvas.width = rect.width - 16
    canvas.height = Math.round(canvas.width * 0.6)
    const ctx = canvas.getContext("2d")
    if (!ctx) return
    ctx.fillStyle = "#ffffff"
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.lineCap = "round"
    ctx.lineJoin = "round"
  }, [])

  useEffect(() => {
    if (wizardStep === 3 && wizardType === "DRAWING") {
      requestAnimationFrame(() => initCanvas())
    }
  }, [wizardStep, wizardType, initCanvas])

  function getCanvasCtx() {
    const canvas = canvasRef.current
    if (!canvas) return null
    return canvas.getContext("2d")
  }

  function saveCanvasState() {
    const ctx = getCanvasCtx()
    const canvas = canvasRef.current
    if (!ctx || !canvas) return
    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
    setDrawingHistory((prev) => [...prev.slice(-30), imageData])
  }

  function startDrawing(e: React.PointerEvent<HTMLCanvasElement>) {
    const ctx = getCanvasCtx()
    const canvas = canvasRef.current
    if (!ctx || !canvas) return
    saveCanvasState()
    canvas.setPointerCapture(e.pointerId)
    setIsDrawing(true)
    ctx.beginPath()
    const rect = canvas.getBoundingClientRect()
    ctx.moveTo(e.clientX - rect.left, e.clientY - rect.top)
  }

  function draw(e: React.PointerEvent<HTMLCanvasElement>) {
    if (!isDrawing) return
    const ctx = getCanvasCtx()
    const canvas = canvasRef.current
    if (!ctx || !canvas) return
    const rect = canvas.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    ctx.lineWidth = drawTool === "eraser" ? brushSize * 4 : brushSize
    ctx.strokeStyle = drawTool === "eraser" ? "#ffffff" : wizardColor
    ctx.globalCompositeOperation = drawTool === "eraser" ? "source-over" : "source-over"
    ctx.lineTo(x, y)
    ctx.stroke()
  }

  function stopDrawing() {
    setIsDrawing(false)
  }

  function undoDrawing() {
    const ctx = getCanvasCtx()
    const canvas = canvasRef.current
    if (!ctx || !canvas || drawingHistory.length === 0) return
    const last = drawingHistory[drawingHistory.length - 1]
    ctx.putImageData(last, 0, 0)
    setDrawingHistory((prev) => prev.slice(0, -1))
  }

  function clearCanvas() {
    const ctx = getCanvasCtx()
    const canvas = canvasRef.current
    if (!ctx || !canvas) return
    saveCanvasState()
    ctx.fillStyle = "#ffffff"
    ctx.fillRect(0, 0, canvas.width, canvas.height)
  }

  function getCanvasDataUrl(): string | undefined {
    const canvas = canvasRef.current
    if (!canvas) return undefined
    return canvas.toDataURL("image/png")
  }

  async function loadPortfolio() {
    try {
      setLoading(true)
      const data = await primaryApi.getPortfolio()
      setItems(data)
    } catch {
      setItems([])
    } finally {
      setLoading(false)
    }
  }

  function openWizard(type: string) {
    setWizardType(type)
    setWizardStep(1)
    setWizardTitle("")
    setWizardDescription("")
    setWizardSubject("")
    setWizardColor("#3b82f6")
    setWizardContent("")
    setIsRecording(false)
    setRecordingTimer(0)
    setDrawTool("pencil")
    setBrushSize(4)
    setDrawingHistory([])
    setWizardOpen(true)
  }

  function closeWizard() {
    setWizardOpen(false)
    setWizardStep(1)
    setWizardType("")
  }

  async function handleCreateItem() {
    if (!wizardTitle.trim()) return
    try {
      setSubmitting(true)
      let fileUrl: string | undefined
      let content: string | undefined
      if (wizardType === "DRAWING") {
        fileUrl = getCanvasDataUrl()
      } else if (wizardContent.trim()) {
        content = wizardContent.trim()
      }
      const created = await primaryApi.addPortfolioItem({
        title: wizardTitle.trim(),
        description: wizardDescription.trim() || undefined,
        fileUrl,
        content,
        portfolioType: wizardType,
        subjectName: wizardSubject.trim() || undefined,
      })
      setItems((prev) => [created, ...prev])
      closeWizard()
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err)
      console.error("Failed to save portfolio item:", msg)
      alert("Failed to save: " + msg)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(itemId: string) {
    try {
      await primaryApi.deletePortfolioItem(itemId)
      setItems((prev) => prev.filter((item) => item.id !== itemId))
    } catch {
      // failed to delete
    }
  }

  const filteredItems = items.filter((item) => {
    if (filter === "All") return true
    const config = typeConfig[item.portfolioType]
    return config?.label === filter
  })

  const totalSteps = 5
  const activeConfig = typeConfig[wizardType] || typeConfig.DRAWING
  const ActiveIcon = activeConfig.icon

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center gap-3">
        <div className="flex size-12 items-center justify-center rounded-2xl bg-primary/10">
          <Hammer className="size-6 text-primary" />
        </div>
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Create & Build</h1>
          <p className="text-sm text-muted-foreground">Make something amazing today!</p>
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="text-lg font-semibold text-foreground mb-1">What would you like to create?</h2>
        <p className="text-sm text-muted-foreground mb-4">Choose an activity to get started</p>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {creationActivities.map((activity) => {
            const Icon = activity.icon
            return (
              <button
                key={activity.type}
                type="button"
                onClick={() => openWizard(activity.type)}
                className={`group flex items-center gap-4 rounded-2xl border-2 ${activity.borderColor} ${activity.bgColor} p-5 text-left transition-all hover:shadow-md`}
              >
                <div className={`flex size-12 items-center justify-center rounded-xl ${activity.bgColor} ring-1 ring-black/5`}>
                  <Icon className={`size-6 ${activity.color}`} />
                </div>
                <div className="flex-1">
                  <p className="text-sm font-semibold text-foreground">{activity.label}</p>
                  <p className="text-xs text-muted-foreground">Click to start</p>
                </div>
                <ChevronRight className="size-4 text-muted-foreground transition-transform group-hover:translate-x-1" />
              </button>
            )
          })}
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {filterOptions.map((opt) => (
          <button
            key={opt}
            type="button"
            onClick={() => setFilter(opt)}
            className={`rounded-full px-4 py-1.5 text-sm font-medium transition-colors ${
              filter === opt
                ? "bg-primary text-primary-foreground"
                : "border border-border bg-card text-muted-foreground hover:bg-muted"
            }`}
          >
            {opt}
          </button>
        ))}
      </div>

      {filteredItems.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10">
            <FolderOpen className="size-8 text-primary" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">Your backpack is empty</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Start creating drawings, stories, projects, and more to build your learning collection.
          </p>
          <button
            type="button"
            onClick={() => openWizard("DRAWING")}
            className="mt-6 inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            <Plus className="size-4" /> Create Your First Item
          </button>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filteredItems.map((item) => {
            const config = typeConfig[item.portfolioType] || typeConfig.DRAWING
            const Icon = config.icon
            return (
              <div
                key={item.id}
                className="group rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:shadow-md"
              >
                <div className="flex items-start justify-between">
                  <div className={`flex size-10 items-center justify-center rounded-xl ${config.bgColor}`}>
                    <Icon className={`size-5 ${config.color}`} />
                  </div>
                  <div className="flex items-center gap-1">
                    {item.isFeatured && (
                      <Star className="size-4 fill-amber-400 text-amber-400" />
                    )}
                    <button
                      type="button"
                      onClick={() => handleDelete(item.id)}
                      className="rounded-lg p-1.5 text-muted-foreground opacity-0 transition-all group-hover:opacity-100 hover:bg-red-50 hover:text-red-600"
                    >
                      <Trash2 className="size-3.5" />
                    </button>
                  </div>
                </div>
                <h3 className="mt-3 text-sm font-semibold text-foreground">{item.title}</h3>
                {item.description && (
                  <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{item.description}</p>
                )}
                {item.fileUrl && item.portfolioType === "DRAWING" && (
                  <div className="mt-2 overflow-hidden rounded-lg border border-border">
                    <img src={item.fileUrl} alt={item.title} className="w-full h-auto object-contain" />
                  </div>
                )}
                {item.content && (
                  <p className="mt-2 text-xs text-muted-foreground line-clamp-3 whitespace-pre-wrap">{item.content}</p>
                )}
                <div className="mt-3 flex items-center gap-2">
                  <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold ${config.bgColor} ${config.color}`}>
                    {config.label}
                  </span>
                  {item.subjectName && (
                    <span className="inline-flex items-center rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                      {item.subjectName}
                    </span>
                  )}
                </div>
                <p className="mt-3 text-[10px] text-muted-foreground">
                  {new Date(item.createdAt).toLocaleDateString()}
                </p>
              </div>
            )
          })}
        </div>
      )}

      {wizardOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-lg rounded-2xl border border-border bg-card p-6 shadow-xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <div className={`flex size-8 items-center justify-center rounded-lg ${activeConfig.bgColor}`}>
                  <ActiveIcon className={`size-4 ${activeConfig.color}`} />
                </div>
                <span className="text-sm font-medium text-muted-foreground">{activeConfig.label}</span>
              </div>
              <button type="button" onClick={closeWizard} className="rounded-lg p-1 text-muted-foreground hover:bg-muted">
                <X className="size-5" />
              </button>
            </div>

            <div className="flex items-center gap-2 mb-5">
              {Array.from({ length: totalSteps }).map((_, i) => (
                <div key={i} className="flex items-center gap-2">
                  <div className={`flex size-6 items-center justify-center rounded-full text-xs font-semibold ${
                    i + 1 < wizardStep
                      ? "bg-green-500 text-white"
                      : i + 1 === wizardStep
                        ? "bg-primary text-primary-foreground"
                        : "bg-muted text-muted-foreground"
                  }`}>
                    {i + 1 < wizardStep ? <Check className="size-3" /> : i + 1}
                  </div>
                  {i < totalSteps - 1 && (
                    <div className={`h-0.5 w-6 ${i + 1 < wizardStep ? "bg-green-500" : "bg-muted"}`} />
                  )}
                </div>
              ))}
            </div>

            {wizardStep === 1 && (
              <div className="space-y-4">
                <div>
                  <h3 className="text-lg font-semibold text-foreground">What is it about?</h3>
                  <p className="text-sm text-muted-foreground">Give your creation a name</p>
                </div>
                <input
                  type="text"
                  value={wizardTitle}
                  onChange={(e) => setWizardTitle(e.target.value)}
                  placeholder="My amazing creation..."
                  autoFocus
                  className="w-full rounded-lg border border-border bg-background px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && wizardTitle.trim()) setWizardStep(2)
                  }}
                />
              </div>
            )}

            {wizardStep === 2 && (
              <div className="space-y-4">
                <div>
                  <h3 className="text-lg font-semibold text-foreground">Tell us more</h3>
                  <p className="text-sm text-muted-foreground">Add a description (optional)</p>
                </div>
                <textarea
                  value={wizardDescription}
                  onChange={(e) => setWizardDescription(e.target.value)}
                  placeholder="What did you create? How did you make it?"
                  rows={4}
                  className="w-full rounded-lg border border-border bg-background px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary resize-none"
                />
              </div>
            )}

            {wizardStep === 3 && (
              <div className="space-y-4">
                <div>
                  <h3 className="text-lg font-semibold text-foreground">Add your work</h3>
                  <p className="text-sm text-muted-foreground">Show us what you created</p>
                </div>

                {wizardType === "DRAWING" && (
                  <div className="space-y-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <div className="flex items-center gap-1 rounded-lg border border-border bg-muted p-1">
                        <button type="button" onClick={() => setDrawTool("pencil")} className={`inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-medium transition-colors ${drawTool === "pencil" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-background"}`}>
                          <Palette className="size-3.5" /> Pencil
                        </button>
                        <button type="button" onClick={() => setDrawTool("eraser")} className={`inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-medium transition-colors ${drawTool === "eraser" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-background"}`}>
                          <Eraser className="size-3.5" /> Eraser
                        </button>
                      </div>
                      <div className="flex items-center gap-1 rounded-lg border border-border bg-muted p-1">
                        {[2, 4, 8, 16].map((size) => (
                          <button key={size} type="button" onClick={() => setBrushSize(size)} className={`rounded-md px-2 py-1 text-xs font-medium transition-colors ${brushSize === size ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-background"}`}>
                            {size}px
                          </button>
                        ))}
                      </div>
                      <button type="button" onClick={undoDrawing} disabled={drawingHistory.length === 0} className="rounded-md border border-border bg-muted p-1.5 text-muted-foreground hover:bg-background disabled:opacity-50">
                        <Undo2 className="size-3.5" />
                      </button>
                      <button type="button" onClick={clearCanvas} className="rounded-md border border-border bg-muted px-2 py-1 text-xs font-medium text-muted-foreground hover:bg-background">
                        Clear
                      </button>
                    </div>
                    <div className="flex flex-wrap gap-1.5">
                      {drawingColors.map((c) => (
                        <button
                          key={c}
                          type="button"
                          onClick={() => { setWizardColor(c); setDrawTool("pencil") }}
                          className={`size-7 rounded-full border-2 transition-all ${
                            wizardColor === c && drawTool === "pencil" ? "border-primary scale-110 ring-2 ring-primary/30" : "border-border"
                          }`}
                          style={{ backgroundColor: c }}
                        />
                      ))}
                    </div>
                    <div className="rounded-xl border border-border bg-white p-1">
                      <canvas
                        ref={canvasRef}
                        onPointerDown={startDrawing}
                        onPointerMove={draw}
                        onPointerUp={stopDrawing}
                        onPointerLeave={stopDrawing}
                        className="w-full cursor-crosshair rounded-lg touch-none"
                        style={{ height: "auto", aspectRatio: "5/3" }}
                      />
                    </div>
                    <input
                      type="text"
                      value={wizardContent}
                      onChange={(e) => setWizardContent(e.target.value)}
                      placeholder="Describe your drawing..."
                      className="w-full rounded-lg border border-border bg-background px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                    />
                  </div>
                )}

                {wizardType === "STORY" && (
                  <textarea
                    value={wizardContent}
                    onChange={(e) => setWizardContent(e.target.value)}
                    placeholder="Once upon a time..."
                    rows={8}
                    className="w-full rounded-lg border border-border bg-background px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary resize-none"
                  />
                )}

                {wizardType === "PROJECT" && (
                  <div className="space-y-3">
                    <textarea
                      value={wizardContent}
                      onChange={(e) => setWizardContent(e.target.value)}
                      placeholder="Describe your project..."
                      rows={5}
                      className="w-full rounded-lg border border-border bg-background px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary resize-none"
                    />
                    <div className="rounded-xl border-2 border-dashed border-border p-6 text-center">
                      <Wrench className="mx-auto size-8 text-muted-foreground/50" />
                      <p className="mt-2 text-sm text-muted-foreground">Drop files here or click to upload (optional)</p>
                    </div>
                  </div>
                )}

                {wizardType === "PHOTO" && (
                  <div className="rounded-xl border-2 border-dashed border-border p-8 text-center">
                    <Camera className="mx-auto size-10 text-muted-foreground/50" />
                    <p className="mt-3 text-sm font-medium text-foreground">Take or upload a photo</p>
                    <p className="mt-1 text-xs text-muted-foreground">Click to select an image from your device</p>
                    <input
                      type="file"
                      accept="image/*"
                      className="mt-4 text-sm text-muted-foreground"
                    />
                  </div>
                )}

                {wizardType === "VOICE_RECORDING" && (
                  <div className="flex flex-col items-center space-y-4 py-4">
                    <button
                      type="button"
                      onClick={() => {
                        setIsRecording(!isRecording)
                        if (isRecording) setRecordingTimer(0)
                      }}
                      className={`flex size-20 items-center justify-center rounded-full transition-all ${
                        isRecording
                          ? "bg-red-500 text-white animate-pulse"
                          : "bg-primary/10 text-primary hover:bg-primary/20"
                      }`}
                    >
                      {isRecording ? <Disc className="size-8" /> : <Mic className="size-8" />}
                    </button>
                    <p className="text-sm font-medium text-foreground">
                      {isRecording ? `Recording... ${Math.floor(recordingTimer / 60)}:${(recordingTimer % 60).toString().padStart(2, "0")}` : "Tap to start recording"}
                    </p>
                    {isRecording && (
                      <div className="flex items-center gap-1">
                        {Array.from({ length: 12 }).map((_, i) => (
                          <div
                            key={i}
                            className="w-1 rounded-full bg-red-400 animate-pulse"
                            style={{
                              height: `${12 + Math.random() * 20}px`,
                              animationDelay: `${i * 0.1}s`,
                            }}
                          />
                        ))}
                      </div>
                    )}
                  </div>
                )}

                {wizardType === "ESSAY" && (
                  <textarea
                    value={wizardContent}
                    onChange={(e) => setWizardContent(e.target.value)}
                    placeholder="Start writing your essay..."
                    rows={10}
                    className="w-full rounded-lg border border-border bg-background px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary resize-none"
                  />
                )}
              </div>
            )}

            {wizardStep === 4 && (
              <div className="space-y-4">
                <div>
                  <h3 className="text-lg font-semibold text-foreground">Which subject?</h3>
                  <p className="text-sm text-muted-foreground">Choose a subject for this work</p>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  {primarySubjects.map((subject) => {
                    const SubIcon = subject.icon
                    return (
                      <button
                        key={subject.name}
                        type="button"
                        onClick={() => setWizardSubject(subject.name)}
                        className={`flex items-center gap-3 rounded-xl border-2 p-3 text-left transition-all ${
                          wizardSubject === subject.name
                            ? "border-primary bg-primary/5"
                            : "border-border hover:border-primary/30"
                        }`}
                      >
                        <div className={`flex size-8 items-center justify-center rounded-lg ${subject.bgColor}`}>
                          <SubIcon className={`size-4 ${subject.color}`} />
                        </div>
                        <span className="text-sm font-medium text-foreground">{subject.name}</span>
                      </button>
                    )
                  })}
                </div>
                <button
                  type="button"
                  onClick={() => setWizardSubject("")}
                  className="text-sm text-muted-foreground hover:text-foreground"
                >
                  Skip this step
                </button>
              </div>
            )}

            {wizardStep === 5 && (
              <div className="space-y-4">
                <div className="text-center py-4">
                  <div className="mx-auto flex size-16 items-center justify-center rounded-full bg-green-100">
                    <Check className="size-8 text-green-600" />
                  </div>
                  <h3 className="mt-4 text-lg font-semibold text-foreground">Save to My Backpack</h3>
                  <p className="mt-1 text-sm text-muted-foreground">Ready to save your creation?</p>
                  <div className="mt-4 rounded-xl bg-muted/50 p-4 text-left">
                    <p className="text-sm font-semibold text-foreground">{wizardTitle}</p>
                    {wizardDescription && (
                      <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{wizardDescription}</p>
                    )}
                    <div className="mt-2 flex items-center gap-2">
                      <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold ${activeConfig.bgColor} ${activeConfig.color}`}>
                        {activeConfig.label}
                      </span>
                      {wizardSubject && (
                        <span className="inline-flex items-center rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                          {wizardSubject}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            )}

            <div className="mt-6 flex items-center justify-between">
              {wizardStep > 1 ? (
                <button
                  type="button"
                  onClick={() => setWizardStep((s) => s - 1)}
                  className="inline-flex items-center gap-2 rounded-lg border border-border px-4 py-2.5 text-sm font-medium text-muted-foreground hover:bg-muted transition-colors"
                >
                  <ChevronLeft className="size-4" />
                  Back
                </button>
              ) : (
                <div />
              )}
              {wizardStep < totalSteps ? (
                <button
                  type="button"
                  onClick={() => setWizardStep((s) => s + 1)}
                  disabled={wizardStep === 1 && !wizardTitle.trim()}
                  className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
                >
                  Next
                  <ChevronRight className="size-4" />
                </button>
              ) : (
                <button
                  type="button"
                  onClick={handleCreateItem}
                  disabled={submitting}
                  className="inline-flex items-center gap-2 rounded-lg bg-green-600 px-5 py-2.5 text-sm font-medium text-white transition-colors hover:bg-green-700 disabled:opacity-50"
                >
                  {submitting ? <Loader2 className="size-4 animate-spin" /> : <Check className="size-4" />}
                  {submitting ? "Saving..." : "Save to Backpack"}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
