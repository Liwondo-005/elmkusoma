// Adds the replay chapter authoring strings to both catalogues. Must be run with `node`
// rather than PowerShell: Get-Content/WriteAllLines silently mangle the UTF-8 content
// (including the existing Kiswahili entries) in messages/*.json.
const fs = require("node:fs")
const path = require("node:path")

const additions = {
  en: {
    title: "Chapter markers",
    description: "Mark the moments worth jumping back to. Learners see these beside the recording and can skip straight to them.",
    recording: "Recording",
    duration: "Length {duration}",
    noRecording: "This class has no published recording yet, so there is nothing to chapter.",
    addSection: "Add a chapter",
    listSection: "Chapters",
    titleLabel: "Chapter title",
    titlePlaceholder: "e.g. Worked example",
    positionLabel: "Position",
    positionPlaceholder: "mm:ss",
    positionHint: "Whole seconds from the start, or a timecode such as 1:30 or 1:02:03.",
    add: "Add chapter",
    remove: "Remove chapter",
    empty: "No chapters yet.",
    invalidPosition: "Enter the position as a timecode such as 1:30, or as whole seconds.",
    pastEnd: "{duration} is past the end of this recording.",
    duplicate: "There is already a chapter at {time}.",
    added: "Chapter {title} added",
    removed: "Chapter removed",
  },
  sw: {
    title: "Alama za sehemu",
    description: "Weka alama za mambo yanayostahili kurejewa. Wanafunzi huona hizi kando ya rekodi na wanaweza kuruka moja kwa moja.",
    recording: "Rekodi",
    duration: "Muda {duration}",
    noRecording: "Darasa hili bado halina rekodi iliyochapishwa, hivyo hakuna cha kugawa.",
    addSection: "Ongeza sehemu",
    listSection: "Sehemu",
    titleLabel: "Kichwa cha sehemu",
    titlePlaceholder: "k.m. Mfano wa kufanya kazi",
    positionLabel: "Nafasi",
    positionPlaceholder: "dd:mm",
    positionHint: "Sekunde kamili kutoka mwanzo, au muda kama 1:30 au 1:02:03.",
    add: "Ongeza sehemu",
    remove: "Ondoa sehemu",
    empty: "Bado hakuna sehemu.",
    invalidPosition: "Weka nafasi kwa muda kama 1:30, au kwa sekunde kamili.",
    pastEnd: "Kutoka {duration} ni baada ya mwisho wa rekodi hii.",
    duplicate: "Tayari kuna sehemu saa {time}.",
    added: "Sehemu {title} imeongezwa",
    removed: "Sehemu imeondolewa",
  },
}

for (const [locale, entries] of Object.entries(additions)) {
  const file = path.join(__dirname, "..", "messages", `${locale}.json`)
  const messages = JSON.parse(fs.readFileSync(file, "utf8"))
  const chapters = (messages.teacher ??= {}).chapters ??= (messages.teacher.chapters = {})

  let changed = false
  for (const [key, value] of Object.entries(entries)) {
    if (chapters[key] === value) continue
    chapters[key] = value
    changed = true
  }

  if (!changed) {
    console.log(`${locale}: already up to date`)
    continue
  }

  fs.writeFileSync(file, JSON.stringify(messages, null, 2) + "\n", "utf8")
  console.log(`${locale}: added teacher.chapters (${Object.keys(entries).length} keys)`)
}
