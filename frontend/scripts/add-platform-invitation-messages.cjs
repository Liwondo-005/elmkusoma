// Adds the platform invitation strings to both catalogues. Must be run with `node`
// rather than PowerShell: Get-Content/WriteAllLines silently mangle the UTF-8 content
// (including the existing Kiswahili entries) in messages/*.json.
const fs = require("node:fs")
const path = require("node:path")

const additions = {
  en: {
    inviteToggleLabel: "Send an invitation instead of setting a password",
    inviteToggleHint:
      "The recipient receives a single-use link that expires, proves they control the address, and then chooses their own password. Prefer this for provider and platform administrator accounts.",
    btnInvite: "Send invitation",
    inviteIssuedTitle: "Invitation created",
    inviteIssuedHint:
      "Activation has been sent to {email}. It expires, and only this invitation can activate it — a new one can be issued if it is lost.",
    inviteTokenLabel: "Activation link token",
    inviteTokenHint:
      "Shown once and never listed again. Pass it to the recipient, or let them request a new code from the activation page.",
  },
  sw: {
    inviteToggleLabel: "Tuma mwaliko badala ya kuweka nenosiri",
    inviteToggleHint:
      "Mpokeaji hupokea kiungo cha matumizi moja kikichaka baada ya muda, anakonyesha anaamiliki anwani hiyo, kisha huwa na nenosiri lake mwenyewe. Ndio bora kwa akaunti za msimamizi wa mtoa huduma na za jukwaa.",
    btnInvite: "Tuma mwaliko",
    inviteIssuedTitle: "Mwaliko umeundwa",
    inviteIssuedHint:
      "Ushiriki wa kuchapa umetumwa kwa {email}. Unakomeshwa baada ya muda, na mwaliko huu ndio unaweza kuiamsha - mwingine unaweza kuombwa ikiwa umepoteka.",
    inviteTokenLabel: "Kitambulisho cha kiungo",
    inviteTokenHint:
      "Kinaonyeshwa mara moja tu na hakitaorodheshwa tena. Mpelee kwa mpokeaji, au waombe anapate msimu mpya.",
  },
}

for (const [locale, entries] of Object.entries(additions)) {
  const file = path.join(__dirname, "..", "messages", `${locale}.json`)
  const messages = JSON.parse(fs.readFileSync(file, "utf8"))
  const form = (messages.platformAdmin ??= {}).userForm ??= (messages.platformAdmin.userForm = {})

  let changed = false
  for (const [key, value] of Object.entries(entries)) {
    if (form[key] === value) continue
    form[key] = value
    changed = true
  }

  if (!changed) {
    console.log(`${locale}: already up to date`)
    continue
  }

  fs.writeFileSync(file, JSON.stringify(messages, null, 2) + "\n", "utf8")
  console.log(`${locale}: added platformAdmin.userForm (${Object.keys(entries).length} keys)`)
}