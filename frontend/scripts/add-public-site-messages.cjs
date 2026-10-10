// Adds the public-site strings introduced by the contact/legal finalization.
// Must be run with `node` - PowerShell mangles UTF-8 in messages/*.json.
const fs = require("node:fs")
const path = require("node:path")

const additions = {
  en: {
    siteContact: {
      messageTooShort: "Please write at least 20 characters so we can help properly.",
      submitFailed: "We could not send your message. Please try again.",
      receivedTitle: "Message received",
      storedReference: "Your reference is {reference}. Keep it for any follow-up.",
      notified: "Our support team has been notified and will get back to you.",
      storedNotNotified:
        "Your message has been stored with the reference above, but no support address is configured yet, so nobody has been notified. Please email us directly using the address on this page.",
      storedNotNotifiedFailed:
        "Your message has been stored with the reference above, but notifying the support team failed. Please email us directly using the address on this page so we do not miss it.",
      categoryLabel: "What is this about?",
      categoryRequired: "Please choose a category.",
      category: {
        GENERAL: "General enquiry",
        SUPPORT: "Technical support",
        PARTNERSHIP: "Partnership",
        FEEDBACK: "Feedback",
        COMPLAINT: "Complaint",
      },
      whatsappOpensChat: "Opens a chat you send yourself. Nothing is sent automatically.",
      sending: "Sending...",
      sendAnother: "Send another message",
    },
    legal: {
      loading: "Loading the current published version...",
      versionMeta: "Version {version} · effective {effectiveDate}",
      noPublishedVersion:
        "No version has been published on the platform yet, so the built-in text below is shown. A Platform Administrator must review and publish the official version before this page can be considered the current terms.",
    },
  },
  sw: {
    siteContact: {
      messageTooShort: "Tafadhali andika angalau herufi 20 ili tuweze kukusaidia vizuri.",
      submitFailed: "Hatukuweza kutuma ujumbe wako. Tafadhali jaribu tena.",
      receivedTitle: "Ujumbe umepokelewa",
      storedReference: "Kumbukumbu yako ni {reference}. Iweka kwa mazungumzo yoyote yaliyoendelea.",
      notified: "Timu yetu ya msaudo imeambishwa na itajibu haraka.",
      storedNotNotified:
        "Ujumbe wako umehifadhiwa kwa kumbukumbu iliyo juu, lakini anwani ya msaudo bado haijasarazishwa, kwa hivyo hakuna aliyeyeambishwa. Tafadhali tutumie barua pepe moja kwa moja kwa anwani iliyo kwenye ukurasa huu.",
      storedNotNotifiedFailed:
        "Ujumbe wako umehifadhiwa kwa kumbukumbu iliyo juu, lakini kumpeleka taarifa kwa timu ya msaudo kumeshindwa. Tafadhali tutumie barua pepe moja kwa moja ili tusikose.",
      categoryLabel: "Hili ni kuhusu nini?",
      categoryRequired: "Tafadhali chagua kategoria.",
      category: {
        GENERAL: "Hoji ya kawaida",
        SUPPORT: "Msaudo wa kiufunzi",
        PARTNERSHIP: "Ushirikiano",
        FEEDBACK: "Maoni",
        COMPLAINT: "Malalamiko",
      },
      whatsappOpensChat:
        "Hufungua mazungumzo unayotuma mwenyewe. Hakuna chochote kinachotumwa kiotomatiki.",
      sending: "Inatuma...",
      sendAnother: "Tuma ujumbe mwingine",
    },
    legal: {
      loading: "Inapakia toleo lililochapishwa la sasa...",
      versionMeta: "Toleo {version} · linaanza {effectiveDate}",
      noPublishedVersion:
        "Hakuna tofeo lililochapishwa bado kwenye mfumo, kwa hivyo maandishi yaliyojiriwa hapa chini yanaonyeshwa. Msimamizi wa Jukwaa lazima aupithe na achapishie toleo rasmi kabla ya ukurasa huu kukubaliwa kama masharti yanayotumika.",
    },
  },
}

function assign(target, source) {
  for (const [key, value] of Object.entries(source)) {
    if (value && typeof value === "object" && !Array.isArray(value)) {
      if (!target[key] || typeof target[key] !== "object") target[key] = {}
      assign(target[key], value)
    } else {
      target[key] = value
    }
  }
}

for (const [locale, namespaces] of Object.entries(additions)) {
  const file = path.join(__dirname, "..", "messages", `${locale}.json`)
  const messages = JSON.parse(fs.readFileSync(file, "utf8"))
  assign(messages, namespaces)
  fs.writeFileSync(file, JSON.stringify(messages, null, 2) + "\n", "utf8")
  console.log(`${locale}: added siteContact + legal`)
}