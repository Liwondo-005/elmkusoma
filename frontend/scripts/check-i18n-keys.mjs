/**
 * Checks every statically-referenced translation key against the catalogues.
 *
 * This exists because a missing key is invisible to the type checker and to `next build`, and
 * only shows up at runtime as a MISSING_MESSAGE in the UI. The `status` namespace was referenced
 * by 84 files and present in neither catalogue, so every status badge in the product rendered a
 * raw key while the build stayed green.
 *
 * Run: node scripts/check-i18n-keys.mjs
 *
 * Scope handling: a file may bind the same variable name to different namespaces in different
 * components (components/learner/shared.tsx binds `t` to both "learner" and "common"). Attributing
 * keys by variable name alone would then report every key as missing from the wrong namespace. So
 * when a name maps to exactly one namespace the attribution is exact; when it maps to several, a
 * key counts as satisfied if it exists in any of them. The second case can under-report, never
 * over-report, which is the right way for a build gate to be wrong.
 *
 * Dynamic keys (`t(someVariable)`) cannot be resolved statically. They are counted and reported so
 * full coverage is not implied, but they are not failures.
 */
import fs from "node:fs"
import path from "node:path"
import { fileURLToPath } from "node:url"

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..")
const SCAN_DIRS = ["app", "components", "lib", "hooks"]
const SKIP = /node_modules|\.next|test-results|\.git/
const LOCALES = ["en", "sw"]

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (SKIP.test(full)) continue
    if (entry.isDirectory()) walk(full, out)
    else if (/\.(ts|tsx)$/.test(entry.name)) out.push(full)
  }
  return out
}

function lookup(catalogue, dottedKey) {
  let node = catalogue
  for (const part of dottedKey.split(".")) {
    if (node === null || typeof node !== "object" || !(part in node)) return undefined
    node = node[part]
  }
  return node
}

const catalogues = {}
for (const locale of LOCALES) {
  catalogues[locale] = JSON.parse(
    fs.readFileSync(path.join(ROOT, "messages", `${locale}.json`), "utf8")
  )
}

const files = SCAN_DIRS.filter((d) => fs.existsSync(path.join(ROOT, d))).flatMap((d) =>
  walk(path.join(ROOT, d))
)

const missing = new Map() // "ns.key" -> { locales:Set, file }
let bindingCount = 0
let dynamicCount = 0
let ambiguousBindings = 0

for (const file of files) {
  const src = fs.readFileSync(file, "utf8")
  const rel = path.relative(ROOT, file).replace(/\\/g, "/")

  // variable name -> namespaces it is bound to in this file
  const bindings = new Map()
  const bind =
    /const\s+(?:(\w+)|\[\s*(\w+)\s*\])\s*=\s*useTranslations\(\s*["'`]([^"'`]+)["'`]\s*\)\s*;?/g
  let b
  while ((b = bind.exec(src)) !== null) {
    bindingCount++
    const name = b[1] || b[2]
    const namespace = b[3]
    if (!bindings.has(name)) bindings.set(name, new Set())
    bindings.get(name).add(namespace)
  }
  for (const namespaces of bindings.values()) {
    if (namespaces.size > 1) ambiguousBindings++
  }

  for (const [name, namespaces] of bindings) {
    const call = new RegExp(`\\b${name}(?:\\.rich|\\.has|\\.raw)?\\(\\s*([^)]*?)\\)`, "g")
    let k
    while ((k = call.exec(src)) !== null) {
      const arg = k[1].trim()
      const quoted = arg.match(/^["'`]([^"'`]*)["'`]$/)
      if (!quoted) {
        // A template literal is a runtime value; anything else is a computed key.
        if (arg.startsWith("`") || arg.startsWith("(") || arg !== "") dynamicCount++
        else dynamicCount++
        continue
      }
      const key = quoted[1]
      if (key.includes("${")) {
        dynamicCount++
        continue
      }
      // Satisfied if the key exists in any namespace this name is bound to.
      const present = [...namespaces].filter((ns) =>
        LOCALES.every((locale) => lookup(catalogues[locale], `${ns}.${key}`) !== undefined)
      )
      if (present.length > 0) continue

      for (const ns of namespaces) {
        const full = `${ns}.${key}`
        const locales = LOCALES.filter(
          (locale) => lookup(catalogues[locale], full) === undefined
        )
        if (locales.length === 0) continue
        if (!missing.has(full)) missing.set(full, { file: rel })
      }
    }
  }
}

console.log(`scanned ${files.length} source files, ${bindingCount} useTranslations() bindings`)
console.log(`ambiguous bindings (same name, >1 namespace): ${ambiguousBindings}`)
console.log(`dynamic (unresolvable) call sites: ${dynamicCount}`)

if (missing.size === 0) {
  console.log("\nOK - every statically-referenced key exists in en and sw.")
  process.exit(0)
}

const byNamespace = new Map()
for (const [full, info] of missing) {
  const ns = full.split(".")[0]
  if (!byNamespace.has(ns)) byNamespace.set(ns, [])
  byNamespace.get(ns).push({ full, ...info })
}

console.log(`\nMISSING KEYS: ${missing.size} across ${byNamespace.size} namespaces\n`)
for (const [ns, entries] of [...byNamespace].sort((a, b) => b[1].length - a[1].length)) {
  console.log(`  ${ns}  (${entries.length})`)
  for (const e of entries) {
    // Derived from the catalogues at print time rather than accumulated during the scan, so the
    // reported locales cannot drift from what is actually on disk.
    const absent = LOCALES.filter((locale) => lookup(catalogues[locale], e.full) === undefined)
    console.log(`     ${e.full}   [${absent.join(", ")}]  <- ${e.file}`)
  }
}

process.exit(1)