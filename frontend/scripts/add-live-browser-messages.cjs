const fs = require("fs")
const path = require("path")

const msgsDir = path.join(__dirname, "..", "messages")

// Locale-aware additions. Inserted as key/value pairs so JSON.stringify keeps the exact
// formatting/indentation style of the existing catalogues and never re-encodes existing
// UTF-8 characters (an earlier PowerShell rewrite corrupted "©" into "Ã‚Â©").
const additions = {
  "en.json": {
    liveBrowser: {
      badgeLiveNow: "LIVE NOW",
      badgeStarting: "STARTING",
      badgeRecorded: "RECORDED",
      badgeCancelled: "CANCELLED",
      endedLabel: "Ended",
      watchReplay: "Watch replay",
      viewSession: "View session",
      loading: "Loading live classes",
      loadFailed: "We could not load live classes",
      retry: "Try again",
      signInRequired:
        "Live classes are available to your institution. Sign in to see what is live now, coming up, and past sessions.",
      signInAction: "Sign in",
    },
    learner: {
      liveClassImageAltNoTeacher: "{title} live class",
      liveClassActionAria: "{action}: {title}",
    },
    liveClassesIndexTitle: null, // marker handled below
  },
  "sw.json": {
    liveBrowser: {
      badgeLiveNow: "INAENDELEA SASA",
      badgeStarting: "INAANZA",
      badgeRecorded: "IMERECODEDWA",
      badgeCancelled: "IMEGHIRIMWA",
      endedLabel: "Imalizwa",
      watchReplay: "Tazama kurudiwa",
      viewSession: "Angalia kipindi",
      loading: "Inapakia madarasa ya moja kwa moja",
      loadFailed: "Hatukuweza kupakia madarasa ya moja kwa moja",
      retry: "Jaribu tena",
      signInRequired:
        "Madarasa ya moja kwa moja yanaopatikana taasisi yako. Ingia ili kuona kilichokuwa kinaendelea, kijaja na vipindi vilivyopita.",
      signInAction: "Ingia",
    },
    learner: {
      liveClassImageAltNoTeacher: "Darasa la moja kwa moja {title}",
      liveClassActionAria: "{action}: {title}",
    },
  },
}

const livePageTitle = { "en.json": "Live learning", "sw.json": "Masomo ya moja kwa moja" }

function setIn(obj, dottedPath, value) {
  const parts = dottedPath.split(".")
  let node = obj
  for (let i = 0; i < parts.length - 1; i++) {
    if (typeof node[parts[i]] !== "object" || node[parts[i]] === null) return false
    node = node[parts[i]]
  }
  node[parts[parts.length - 1]] = value
  return true
}

let failed = false
for (const [file, spec] of Object.entries(additions)) {
  const filePath = path.join(msgsDir, file)
  const raw = fs.readFileSync(filePath, "utf8")
  const before = raw
  let json
  try {
    json = JSON.parse(raw)
  } catch (e) {
    console.error(`${file}: INVALID JSON on disk -> ${e.message}`)
    failed = true
    continue
  }

  for (const [group, entries] of Object.entries(spec)) {
    if (group === "liveClassesIndexTitle") continue
    for (const [key, value] of Object.entries(entries)) {
      const path = group === "liveBrowser" ? `ui.liveBrowser.${key}` : `${group}.${key}`
      if (!setIn(json, path, value)) {
        console.error(`${file}: cannot place ${path}`)
        failed = true
      }
    }
  }

  // The Live landing page H1 said "Get in touch"/"Wasiliana nasi" (contact copy).
  setIn(json, "public.liveClassesIndex.title", livePageTitle[file])

  const out = JSON.stringify(json, null, 2) + "\n"
  if (out === before) {
    console.log(`${file}: already up to date`)
    continue
  }
  fs.writeFileSync(filePath, out, "utf8")

  // Re-parse to prove the written file is valid JSON.
  const verify = JSON.parse(fs.readFileSync(filePath, "utf8"))
  console.log(
    `${file}: OK - liveBrowser keys=${Object.keys(verify.ui.liveBrowser).length}, ` +
      `signInAction="${verify.ui.liveBrowser.signInAction}", ` +
      `altNoTeacher="${verify.learner.liveClassImageAltNoTeacher}", ` +
      `pageTitle="${verify.public.liveClassesIndex.title}", ` +
      `copyrightIntact=${verify.common.copyright.includes("\u00a9")}`
  )
}

process.exit(failed ? 1 : 0)
