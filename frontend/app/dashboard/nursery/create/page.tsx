"use client"

import { useState } from "react"
import { ArrowLeft, Palette, Music, Pencil, Eraser, Undo2, Redo2, Download, Trash2 } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"

const COLORS = [
  "#ef4444", "#f97316", "#eab308", "#22c55e", "#06b6d4",
  "#3b82f6", "#8b5cf6", "#ec4899", "#000000", "#ffffff",
]

const BRUSH_SIZES = [4, 8, 12, 20]

const STAMPS = ["\u2B50", "\u2764\uFE0F", "\u{1F338}", "\u{1F98B}", "\u{1F308}", "\u{1F3B5}", "\u{1F31F}", "\u{1F431}", "\u{1F436}", "\u{1F33B}", "\u{1F34E}", "\u{1F388}"]

const MUSICAL_NOTES = ["Do", "Re", "Mi", "Fa", "Sol", "La", "Ti"]

export default function CreateStudioPage() {
  const t = useTranslations("nursery")
  const tc = useTranslations("common")
  const [activeTab, setActiveTab] = useState<"draw" | "music">("draw")
  const [selectedColor, setSelectedColor] = useState("#3b82f6")
  const [brushSize, setBrushSize] = useState(8)
  const [selectedStamp, setSelectedStamp] = useState<string | null>(null)
  const [isErasing, setIsErasing] = useState(false)
  const [playingNote, setPlayingNote] = useState<string | null>(null)

  function handleDraw(e: React.MouseEvent<HTMLCanvasElement>) {
    const canvas = e.currentTarget
    const ctx = canvas.getContext("2d")
    if (!ctx) return

    const rect = canvas.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top

    if (selectedStamp) {
      ctx.font = `${brushSize * 2}px serif`
      ctx.fillText(selectedStamp, x, y)
    } else {
      ctx.beginPath()
      ctx.arc(x, y, isErasing ? brushSize * 2 : brushSize / 2, 0, Math.PI * 2)
      ctx.fillStyle = isErasing ? "#ffffff" : selectedColor
      ctx.fill()
    }
  }

  function clearCanvas(e: React.MouseEvent<HTMLCanvasElement>) {
    const canvas = e.currentTarget
    const ctx = canvas.getContext("2d")
    if (ctx) {
      ctx.fillStyle = "#ffffff"
      ctx.fillRect(0, 0, canvas.width, canvas.height)
    }
  }

  function playNote(note: string) {
    setPlayingNote(note)
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)()
      const oscillator = audioCtx.createOscillator()
      const gainNode = audioCtx.createGain()
      oscillator.connect(gainNode)
      gainNode.connect(audioCtx.destination)
      oscillator.frequency.value = 261.63 * Math.pow(2, MUSICAL_NOTES.indexOf(note) / 12)
      oscillator.type = "sine"
      gainNode.gain.value = 0.3
      oscillator.start()
      gainNode.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.5)
      oscillator.stop(audioCtx.currentTime + 0.5)
    } catch {}
    setTimeout(() => setPlayingNote(null), 500)
  }

  return (
    <div className="mx-auto max-w-4xl space-y-4 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToList")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">{t("createStudio")}</h1>
          <p className="text-sm text-gray-500">{t("subtitle.create")}</p>
        </div>
      </div>

      <div className="flex rounded-2xl bg-gray-100 p-1">
        <button
          onClick={() => setActiveTab("draw")}
          aria-pressed={activeTab === "draw"}
          aria-label={t("draw")}
          className={`flex flex-1 items-center justify-center gap-2 rounded-xl py-3 text-sm font-bold transition-colors ${
            activeTab === "draw" ? "bg-white text-primary shadow-sm" : "text-gray-500"
          }`}
        >
          <Pencil className="size-4" /> {t("draw")}
        </button>
        <button
          onClick={() => setActiveTab("music")}
          aria-pressed={activeTab === "music"}
          aria-label={t("musicTab")}
          className={`flex flex-1 items-center justify-center gap-2 rounded-xl py-3 text-sm font-bold transition-colors ${
            activeTab === "music" ? "bg-white text-primary shadow-sm" : "text-gray-500"
          }`}
        >
          <Music className="size-4" /> {t("musicTab")}
        </button>
      </div>

      {activeTab === "draw" ? (
        <div className="space-y-4">
          <div className="flex flex-wrap gap-2">
            {COLORS.map((color) => (
              <button
                key={color}
                onClick={() => { setSelectedColor(color); setIsErasing(false); setSelectedStamp(null) }}
                aria-label={`Color ${color}`}
                aria-pressed={selectedColor === color && !isErasing}
                className={`size-10 rounded-full border-2 transition-transform hover:scale-110 ${
                  selectedColor === color && !isErasing ? "border-gray-800 scale-110" : "border-gray-200"
                }`}
                style={{ backgroundColor: color }}
              />
            ))}
          </div>

          <div className="flex items-center gap-3">
            <span className="text-xs font-bold text-gray-500">{t("size")}</span>
            {BRUSH_SIZES.map((size) => (
              <button
                key={size}
                onClick={() => setBrushSize(size)}
                aria-label={`${t("size")} ${size}`}
                className={`flex items-center justify-center rounded-full border-2 transition-all ${
                  brushSize === size ? "border-primary bg-primary/10" : "border-gray-200"
                }`}
                style={{ width: size + 20, height: size + 20 }}
              >
                <div
                  className="rounded-full bg-gray-800"
                  style={{ width: size, height: size }}
                />
              </button>
            ))}
          </div>

          <div>
            <p className="mb-2 text-xs font-bold text-gray-500">{t("stamps")}</p>
            <div className="flex flex-wrap gap-2">
              {STAMPS.map((stamp) => (
                <button
                  key={stamp}
                  onClick={() => { setSelectedStamp(selectedStamp === stamp ? null : stamp); setIsErasing(false) }}
                  aria-label={`Stamp ${stamp}`}
                  aria-pressed={selectedStamp === stamp}
                  className={`flex size-10 items-center justify-center rounded-xl text-xl transition-all ${
                    selectedStamp === stamp ? "bg-primary/10 ring-2 ring-primary" : "bg-gray-50 hover:bg-gray-100"
                  }`}
                >
                  {stamp}
                </button>
              ))}
            </div>
          </div>

          <div className="nursery-card overflow-hidden rounded-2xl border-2 border-gray-200 bg-white">
            <canvas
              width={600}
              height={400}
              className="w-full cursor-crosshair"
              onMouseMove={handleDraw}
              onDoubleClick={clearCanvas}
              aria-label="Drawing canvas"
            />
          </div>
          <p className="text-center text-[10px] text-gray-400">{t("doubleClickClear")}</p>
        </div>
      ) : (
        <div className="space-y-4">
          <p className="text-center text-sm font-bold text-gray-600">{t("tapKeys")}</p>
          <div className="grid grid-cols-7 gap-2">
            {MUSICAL_NOTES.map((note, i) => (
              <button
                key={note}
                onClick={() => playNote(note)}
                aria-label={`${note} note`}
                className={`flex h-32 flex-col items-center justify-center rounded-2xl font-bold transition-all ${
                  playingNote === note
                    ? "scale-95 bg-primary text-white shadow-lg"
                    : i % 2 === 0
                      ? "bg-white text-gray-800 shadow-md hover:shadow-lg"
                      : "bg-gray-800 text-white shadow-md hover:shadow-lg"
                }`}
              >
                <span className="text-2xl">{note}</span>
                <span className="mt-2 text-[10px] opacity-60">\u266A</span>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
