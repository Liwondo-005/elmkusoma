const fs = require("fs")
const path = require("path")
const { execSync } = require("child_process")

const repo = path.join(__dirname, "..", "..")
const msgs = path.join(repo, "frontend", "messages")

function flatten(obj, prefix = "", out = {}) {
  for (const [k, v] of Object.entries(obj || {})) {
    const key = prefix ? `${prefix}.${k}` : k
    if (v && typeof v === "object" && !Array.isArray(v)) flatten(v, key, out)
    else out[key] = v
  }
  return out
}

// git's HEAD version (pre-rewrite) vs the working tree, compared as PARSED objects so a
// reformat cannot masquerade as a change and a genuinely dropped key cannot hide in it.
const files = ["en.json", "sw.json"]
let problems = 0

const expectedNew = {
  "ui.liveBrowser.badgeLiveNow": 1,
  "ui.liveBrowser.signInAction": 1,
  "ui.liveBrowser.watchReplay": 1,
  "learner.liveClassImageAltNoTeacher": 1,
  "learner.liveClassActionAria": 1,
}

for (const file of files) {
  const headRaw = execSync(`git show HEAD:frontend/messages/${file}`, { cwd: repo, maxBuffer: 64 * 1024 * 1024 }).toString("utf8")
  const headFlat = flatten(JSON.parse(headRaw))
  const nowRaw = fs.readFileSync(path.join(msgs, file), "utf8")
  const nowFlat = flatten(JSON.parse(nowRaw))

  const removed = Object.keys(headFlat).filter((k) => !(k in nowFlat))
  const added = Object.keys(nowFlat).filter((k) => !(k in headFlat))
  const changed = Object.keys(nowFlat).filter((k) => k in headFlat && headFlat[k] !== nowFlat[k])

  console.log(`\n${file}: head=${Object.keys(headFlat).length} now=${Object.keys(nowFlat).length}`)
  console.log(`  removed (${removed.length}): ${removed.join(", ") || "none"}`)
  console.log(`  added   (${added.length}): ${added.join(", ") || "none"}`)
  console.log(`  changed (${changed.length}): ${changed.map((k) => `${k}: ${JSON.stringify(headFlat[k])} -> ${JSON.stringify(nowFlat[k])}`).join(" | ") || "none"}`)

  for (const k of removed) {
    // A key may only disappear if the source had it twice under the same object with the same
    // value (a latent duplicate that JSON.parse already collapsed).
    console.log(`  NOTE removed key ${k} had value ${JSON.stringify(headFlat[k])} - verify it was a duplicate`)
    problems++
  }
  for (const k of expectedNew ? Object.keys(expectedNew) : []) {
    if (!(k in nowFlat)) {
      console.log(`  MISSING expected new key ${k}`)
      problems++
    }
  }
  // public.liveClassesIndex.title is the intended content change.
  const titleKey = "public.liveClassesIndex.title"
  if (!(changed.includes(titleKey))) {
    console.log(`  MISSING intended title change on ${titleKey}`)
    problems++
  }
}

console.log(problems === 0 ? "\nRESULT: no keys lost" : `\nRESULT: ${problems} issue(s) to review`)
