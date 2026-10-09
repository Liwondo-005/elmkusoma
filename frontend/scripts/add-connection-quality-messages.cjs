const fs = require("fs")
const path = require("path")

const msgsDir = path.join(__dirname, "..", "messages")

// LiveKit measures connection quality from real media statistics (packet loss, RTT,
// jitter) and reports Poor/Good/Excellent for the local participant. These labels are
// added through Node so the existing UTF-8 catalogues are never re-encoded (a prior
// PowerShell rewrite corrupted them).
const additions = {
  "en.json": {
    connectionQualityLabel: "Connection quality",
    connectionQualityPoor: "Poor",
    connectionQualityGood: "Good",
    connectionQualityExcellent: "Excellent",
    connectionQualityUnknown: "Measuring",
  },
  "sw.json": {
    connectionQualityLabel: "Ubora wa muunganisho",
    connectionQualityPoor: "Duni",
    connectionQualityGood: "Nzuri",
    connectionQualityExcellent: "Bora sana",
    connectionQualityUnknown: "Inapima",
  },
}

let failed = false
for (const [file, keys] of Object.entries(additions)) {
  const filePath = path.join(msgsDir, file)
  const before = fs.readFileSync(filePath, "utf8")
  const catalogue = JSON.parse(before)

  if (!catalogue.live || typeof catalogue.live !== "object") {
    console.error(`${file}: missing "live" namespace`)
    failed = true
    continue
  }

  let changed = false
  for (const [key, value] of Object.entries(keys)) {
    if (catalogue.live[key] === undefined) {
      catalogue.live[key] = value
      changed = true
    }
  }

  if (!changed) {
    console.log(`${file}: already present, no change`)
    continue
  }

  fs.writeFileSync(filePath, JSON.stringify(catalogue, null, 2) + "\n", "utf8")
  console.log(`${file}: added ${Object.keys(keys).join(", ")}`)
}

process.exit(failed ? 1 : 0)
