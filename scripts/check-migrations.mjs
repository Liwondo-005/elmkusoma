#!/usr/bin/env node
/**
 * check-migrations.mjs — Flyway migration guard for ELMKUSOMA.
 *
 * WHY THIS EXISTS
 * ---------------
 * In Oct 2026 two branches independently claimed migration versions V161-V163.
 * The merge resolved it by RENAMING files that databases had already applied
 * (V161->V165 etc.), which made every dev database fail validation at boot
 * ("Migration checksum mismatch for migration version 161") until the history
 * table was manually repaired. Applied migrations are immutable; the fix for a
 * version collision is always to renumber the file that has NOT been applied
 * yet (the new one), never the one already in flyway_schema_history.
 *
 * WHAT IT ENFORCES (per backend module, each has its own Flyway history)
 * ---------------------------------------------------------------------
 *  1. Filename convention: V<digits>__<description>.sql (or R__<description>.sql
 *     for repeatable migrations). Flyway rejects anything else.
 *  2. No two versioned migrations in a module share the same version number.
 *  3. Files that exist on the base branch are immutable: no modify, delete or
 *     rename (git statuses M/D/R).
 *  4. A newly added file's version number must not already exist on the base
 *     branch under a different filename (catches cross-branch collisions
 *     *before* the merge, while renumbering is still painless).
 *
 * Gaps in the version sequence and out-of-order versions stay legal —
 * application.yml runs with out-of-order: true, and merges legitimately bring
 * in lower versions (that is exactly how V161-V164 certificate migrations
 * arrive after V165-V167).
 *
 * USAGE
 *   node scripts/check-migrations.mjs [--base <git-ref>]
 *   Base defaults to $MIGRATION_BASE_REF, then origin/main, then main.
 *   Exit 0 = pass, exit 1 = violation (messages on stdout).
 */
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const VERSIONED_RE = /^V(\d+)__[A-Za-z0-9].*\.sql$/;
const REPEATABLE_RE = /^R__[A-Za-z0-9].*\.sql$/;
const ZERO_SHA = /^0{7,}$/;

let failures = [];
const fail = (msg) => failures.push(msg);

function git(args) {
  return execFileSync("git", args, {
    cwd: repoRoot,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  }).trimEnd();
}

function gitOk(args) {
  try {
    return { ok: true, out: git(args) };
  } catch (err) {
    return { ok: false, out: (err.stdout || "") + (err.stderr || "") };
  }
}

/** Every backend module that ships its own src/main/resources/db/migration. */
function migrationDirs() {
  const dirs = [];
  const backend = path.join(repoRoot, "backend");
  if (!fs.existsSync(backend)) return dirs;
  for (const entry of fs.readdirSync(backend, { withFileTypes: true })) {
    if (!entry.isDirectory()) continue;
    const dir = path.join(backend, entry.name, "src", "main", "resources", "db", "migration");
    if (fs.existsSync(dir)) dirs.push(dir);
  }
  return dirs;
}

const toPosix = (p) => p.split(path.sep).join("/");

/** version -> relative path, parsed from a list of repo-relative file paths. */
function versionMap(paths, moduleDirRel) {
  const map = new Map();
  for (const p of paths) {
    const name = path.posix.basename(p);
    const m = VERSIONED_RE.exec(name);
    if (m && path.posix.dirname(p) === moduleDirRel) {
      map.set(parseInt(m[1], 10), p);
    }
  }
  return map;
}

// ---------------------------------------------------------------- args
const argIndex = process.argv.indexOf("--base");
let baseRef = argIndex >= 0 ? process.argv[argIndex + 1] : process.env.MIGRATION_BASE_REF;

// ------------------------------------------------------- 1. conventions
const dirs = migrationDirs();
if (dirs.length === 0) {
  console.error("MIGRATION GUARD: no backend module with db/migration found — check repo layout.");
  process.exit(1);
}

const worktreeFiles = new Map(); // moduleDirRel -> [repo-relative paths]
for (const absDir of dirs) {
  const relDir = toPosix(path.relative(repoRoot, absDir));
  const files = [];
  for (const name of fs.readdirSync(absDir)) {
    if (!name.endsWith(".sql")) continue;
    if (!VERSIONED_RE.test(name) && !REPEATABLE_RE.test(name)) {
      fail(
        `[filename] "${relDir}/${name}" is not V<version>__<description>.sql (or R__<description>.sql). ` +
          `Flyway will reject it — rename the file before committing.`,
      );
    }
    files.push(`${relDir}/${name}`);
  }
  worktreeFiles.set(relDir, files);
}

// ----------------------------------------------------- 2. duplicate versions
for (const [relDir, files] of worktreeFiles) {
  const seen = new Map();
  for (const p of files) {
    const m = VERSIONED_RE.exec(path.posix.basename(p));
    if (!m) continue;
    const version = parseInt(m[1], 10);
    if (seen.has(version)) {
      fail(
        `[duplicate] Version ${version} exists twice in ${relDir}:\n` +
          `    ${seen.get(version)}\n    ${p}\n` +
          `    Two files with one version = one of them never applies (or fails validation). ` +
          `Renumber the file that is NOT on the base branch (never an applied one).`,
      );
    } else {
      seen.set(version, p);
    }
  }
}

// ------------------------------------------------ 3+4. compare vs base branch
if (baseRef && ZERO_SHA.test(baseRef)) baseRef = undefined;
if (!baseRef) {
  for (const candidate of ["origin/main", "main"]) {
    if (gitOk(["rev-parse", "--verify", "--quiet", `${candidate}^{commit}`]).ok) {
      baseRef = candidate;
      break;
    }
  }
}

if (!baseRef) {
  console.error("MIGRATION GUARD: no base ref available (tried --base, origin/main, main).");
  process.exit(1);
}
const baseVerify = gitOk(["rev-parse", "--verify", "--quiet", `${baseRef}^{commit}`]);
if (!baseVerify.ok) {
  console.error(
    `MIGRATION GUARD: base ref "${baseRef}" does not resolve to a commit. ` +
      `Pass --base <ref> or fetch the base branch (CI uses fetch-depth: 0).`,
  );
  process.exit(1);
}

const relDirArgs = dirs.map((d) => toPosix(path.relative(repoRoot, d)));

// Untracked migration files never show up in git diff — treat them as added.
const untracked = gitOk(["ls-files", "--others", "--exclude-standard", "--", ...relDirArgs])
  .out.split("\n")
  .filter((p) => p.endsWith(".sql"));

// name-status with rename detection: lines like "M\tpath", "D\tpath", "R100\told\tnew", "A\tpath"
const diff = gitOk(["diff", "--name-status", "-M", baseRef, "--", ...relDirArgs]).out
  .split("\n")
  .filter(Boolean);

const added = [];
for (const line of diff) {
  const [status, ...paths] = line.split("\t");
  if (status.startsWith("A")) {
    added.push(paths[paths.length - 1]);
    continue;
  }
  if (status.startsWith("R")) {
    fail(
      `[renamed] ${paths[0]} -> ${paths[1]}\n` +
        `    Migrations already on ${baseRef} may be applied to a database. Renaming them desyncs ` +
        `flyway_schema_history (checksum/description mismatch at boot). Revert the rename; if the ` +
        `name collides with another branch, renumber YOUR new file instead.`,
    );
  } else if (status.startsWith("M")) {
    fail(
      `[modified] ${paths[0]}\n` +
        `    Applied migrations are immutable — editing one changes its checksum and breaks every ` +
        `database that already ran it. Add a NEW V<next> migration instead of editing history.`,
    );
  } else if (status.startsWith("D")) {
    fail(
      `[deleted] ${paths[0]}\n` +
        `    A migration on ${baseRef} may already be applied; deleting it makes history point at a ` +
        `missing file ("Applied but not resolved"). Restore the file.`,
    );
  }
}
added.push(...untracked);

if (added.length > 0) {
  const baseFiles = gitOk(["ls-tree", "-r", "--name-only", baseRef, "--", ...relDirArgs])
    .out.split("\n")
    .filter((p) => p.endsWith(".sql"));

  for (const relDir of worktreeFiles.keys()) {
    const baseVersions = versionMap(baseFiles, relDir);
    for (const p of added) {
      if (path.posix.dirname(p) !== relDir) continue;
      const m = VERSIONED_RE.exec(path.posix.basename(p));
      if (!m) continue;
      const version = parseInt(m[1], 10);
      const collision = baseVersions.get(version);
      if (collision) {
        fail(
          `[collision] ${p} claims version ${version}, which ${baseRef} already uses for:\n` +
          `    ${collision}\n` +
          `    Both branches applied their own migration under one number — the merge would force a ` +
          `rename of an applied file and break every database. Renumber YOUR new file to the next ` +
          `free version (both files can coexist; version gaps are fine).`,
        );
      }
    }
  }
}

// ------------------------------------------------------------- report
if (failures.length > 0) {
  console.log(`MIGRATION GUARD FAILED (${failures.length} violation(s)) — base: ${baseRef}\n`);
  failures.forEach((f, i) => console.log(`${i + 1}. ${f}\n`));
  console.log(
    "Applied migrations are immutable. Fix collisions by renumbering the migration that is NOT yet " +
      "on the base branch, then force-add the renamed file:\n" +
      "  git mv backend/<module>/src/main/resources/db/migration/V<old>__x.sql .../V<next>__x.sql",
  );
  process.exit(1);
}

const total = [...worktreeFiles.values()].reduce((n, f) => n + f.length, 0);
console.log(
  `MIGRATION GUARD PASS — ${worktreeFiles.size} module(s), ${total} migration file(s), ` +
    `base ${baseRef}, ${added.length} added / ${diff.length - added.length} changed vs base.`,
);
