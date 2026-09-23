export function announce(message: string) {
  if (typeof document === "undefined") return
  const el = document.getElementById("dashboard-announcer")
  if (!el) return
  el.textContent = ""
  window.setTimeout(() => {
    el.textContent = message
  }, 50)
}
