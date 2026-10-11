// Adds the replay chapter marker strings to both catalogues. Must be run with `node`
// rather than PowerShell: Get-Content/WriteAllLines silently mangle the UTF-8 content
// (including the existing Kiswahili entries) in messages/*.json.
const fs = require("node:fs")
const path = require("node:path")

const additions = {
  en: { chapters: "Chapters", chaptersAnnounce: "Jumped to {time}" },
  sw: { chapters: "Sehemu", chaptersAnnounce: "Imefika {time}" },
}

for (const [locale, entries] of Object.entries(additions)) {
  const file = path.join(__dirname, "..", "messages", `${locale}.json`)
  const messages = JSON.parse(fs.readFileSync(file, "utf8"))
  const viewer = (messages.events ??= {}).viewer ??= (messages.events.viewer = {})

  let changed = false
  for (const [key, value] of Object.entries(entries)) {
    if (viewer[key] === value) continue
    viewer[key] = value
    changed = true
  }

  if (!changed) {
    console.log(`${locale}: already up to date`)
    continue
  }

  fs.writeFileSync(file, JSON.stringify(messages, null, 2) + "\n", "utf8")
  console.log(`${locale}: added ${Object.keys(entries).join(", ")}`)
}
