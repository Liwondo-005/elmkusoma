// Adds the Platform Admin public-site management strings.
// Must be run with `node` - PowerShell mangles UTF-8 in messages/*.json.
const fs = require("node:fs")
const path = require("node:path")

const additions = {
  en: {
    title: "Public site, legal & support",
    subtitle: "Contact details shown to visitors, the legal documents they read, and the enquiry inbox.",
    tabSettings: "Contact & social",
    tabLegal: "Legal content",
    tabInbox: "Enquiry inbox",
    loading: "Loading...",
    loadFailed: "Could not load. Check your connection and try again.",
    saveFailed: "Could not save. Check your connection and try again.",
    save: "Save",
    invalidValue: "This value is not in a format the site can safely use.",
    unsetNotice:
      "An empty value means the detail is not published. Unset phone numbers, WhatsApp numbers and social links are hidden from visitors rather than shown as placeholders, so please only fill in details that are real.",
    group: {
      PUBLIC_CONTACT: "Public contact details",
      PUBLIC_SOCIAL: "Official social media links",
      SUPPORT_INTERNAL: "Support routing (never public)",
    },
    documents: "Documents",
    draft: "Draft",
    published: "Published",
    notPublished: "not published yet",
    createDraft: "Create draft",
    editDraft: "Edit draft",
    selectDocument: "Choose a document to edit, or create a draft for one that does not exist yet.",
    draftHeading: "Working copy",
    draftExplainer:
      "Edits change the draft only. Visitors keep reading the published version until you publish.",
    draftSaved: "Draft saved. Publish it when you are ready for visitors to see the change.",
    publishSuccess: "Published. Visitors now see this version.",
    reverted: "Earlier version restored into the draft. Publish it to make it live.",
    titleLabel: "Title",
    effectiveDateLabel: "Effective date",
    contentLabel: "Content",
    contentFormatNote:
      "Plain text. Paragraphs are separated by a blank line. HTML is not accepted and is never rendered as markup.",
    saveDraft: "Save draft",
    publish: "Publish",
    history: "Published history",
    historyExplainer:
      "Every published version is kept. Restoring an older version copies it into the draft; it does not erase anything that was published before.",
    noVersions: "Nothing has been published yet.",
    restoreIntoDraft: "Restore into draft",
    newDocumentBody:
      "Draft placeholder. Replace this text with the reviewed legal content before publishing.",
    legalType: {
      TERMS: "Terms & Conditions",
      PRIVACY: "Privacy Policy",
      COOKIE: "Cookie Policy",
      SUPPORT_POLICY: "Support Policy",
    },
    filterStatus: "Status",
    allStatuses: "All statuses",
    inboxEmpty: "No enquiries have been received yet.",
    selectMessage: "Select an enquiry to read it.",
    retryNotification: "Retry notification",
    status: {
      NEW: "New",
      IN_PROGRESS: "In progress",
      AWAITING_RESPONSE: "Awaiting response",
      RESOLVED: "Resolved",
      CLOSED: "Closed",
    },
    notification: {
      SENT: "Support team notified",
      PENDING: "Notification pending",
      FAILED: "Notification failed",
      NOT_CONFIGURED: "No recipient configured - nobody was notified",
    },
  },
  sw: {
    title: "Tovuti ya umma, sheria na msaudo",
    subtitle: "Mawasiliano ya mawasilisho kwa wageni, waraka sheria wanazosoma, na sanduku ya ujumbe.",
    tabSettings: "Mawasiliano na mitandao",
    tabLegal: "Maudhui ya sheria",
    tabInbox: "Sanduku ya ujumbe",
    loading: "Inapakia...",
    loadFailed: "Haikuwezi kupakia. Angalia muunganisho wako na jaribu tena.",
    saveFailed: "Haikuwezi kuhifadhi. Angalia muunganisho wako na jaribu tena.",
    save: "Hifadhi",
    invalidValue: "Thamani hii si katika muundo ambao tovuti inaweza kutumia kwa usalama.",
    unsetNotice:
      "Thamani tupu maana maelezo hayo hayajaochapishwa. Namba za simu, WhatsApp na viungo vya mitandao ambavyo havijasetwa hufichwa badala ya kuonyeshwa kama mifano, kwa hivyo tafadhali weka tu taarifa halisi.",
    group: {
      PUBLIC_CONTACT: "Mawasiliano ya umma",
      PUBLIC_SOCIAL: "Viungo rasmi vya mitandao ya kijamii",
      SUPPORT_INTERNAL: "Usambazaji wa msaudo (ha chapishwi kwa umma)",
    },
    documents: "Waraka",
    draft: "Rasimu",
    published: "Imechapishwa",
    notPublished: "bado haijachapishwa",
    createDraft: "Tengeneza rasimu",
    editDraft: "Hariri rasimu",
    selectDocument: "Chagua waraka kuhariri, au tengeneza rasimu kwa yenye bado haipo.",
    draftHeading: "Nakala ya kazi",
    draftExplainer:
      "Mabadiliko yatabadilisha rasimu pekee. Wageni wataendelea kusoma toleo lililochapishwa hadi uchapishwe.",
    draftSaved: "Rasimu imehifadhiwa. Chapisha pale unapopelekwa na wageni wauone mabadiliko.",
    publishSuccess: "Imechapishwa. Wageni sasa wanaona toleo hili.",
    reverted: "Toleo la awali limerudishwa kwenye rasimu. Chapisha ili kuwa live.",
    titleLabel: "Kichwa",
    effectiveDateLabel: "Tarehe ya kuanza",
    contentLabel: "Yaliyomo",
    contentFormatNote:
      "Maandishi rahisi. Vipara hufuanywa kwa mstari mtu. HTML haipokelewi wala kuonyeshwa kama markup.",
    saveDraft: "Hifadhi rasimu",
    publish: "Chapisha",
    history: "Historia ya vilivyochapishwa",
    historyExplainer:
      "Kila toleo lililochapishwa linahifadhiwa. Kurudisha toleo la awali kunakili kwenye rasimu; hakuna kilichofutwa.",
    noVersions: "Hakuna kitu kilichochapishwa bado.",
    restoreIntoDraft: "Rudisha kwenye rasimu",
    newDocumentBody:
      "Mahali pa rasimu. Badilisha maandishi haya na maudhui ya sheria yaliyopitiwa kabla ya kuchapisha.",
    legalType: {
      TERMS: "Masharti na Vigezo",
      PRIVACY: "Sera ya Faragha",
      COOKIE: "Sera ya Kuki",
      SUPPORT_POLICY: "Sera ya Msaudo",
    },
    filterStatus: "Hali",
    allStatuses: "Hali zote",
    inboxEmpty: "Hakujapokea ujumbe bado.",
    selectMessage: "Chagua ujumba ili kuusoma.",
    retryNotification: "Jaribu tena kumtaarifa",
    status: {
      NEW: "Mpya",
      IN_PROGRESS: "Inaendelea",
      AWAITING_RESPONSE: "Inasubiri jibu",
      RESOLVED: "Imesuluhishwa",
      CLOSED: "Imefungwa",
    },
    notification: {
      SENT: "Timu ya msaudo imeambishwa",
      PENDING: "Taarifa inasubiri",
      FAILED: "Imeshindwa kutumia taarifa",
      NOT_CONFIGURED: "Hakuna mpokeaji aliyewekwa - hakuna aliyeyeambishwa",
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
  console.log(`${locale}: added platformAdmin.publicSite`)
}