# D-06 — V175 Database History Recovery Procedure

**Status: BLOCKED. Read-only until the database owner authorises a recovery action.**

Nothing in this document has been executed against a shared database. `flyway_schema_history`
has never been edited, deleted, baselined or repaired. The application has never been pointed at
`elmkusoma` in a way that would trigger a Flyway repair.

**Prepared:** 2026-10-20 · **Repo:** `main` @ `9391b424` · **Migration chain:** V1 → V183

---

## 1. The observed database record

Read-only inspection of `elmkusoma`.`flyway_schema_history`:

```
installed_rank | version | description               | checksum  | success
---------------+---------+---------------------------+-----------+--------
170            | 174     | parent student link integ | 797143152 | true
171            | 175     | subscription lifecycle    | 222681029 | true
```

`elmkusoma` physical state confirms the *former* V175 actually ran: it has the `subscriptions`
table, `btree_gist`, and `ex_subscriptions_active_no_overlap`. The independent-teacher and
institutionless-audit-log DDL was **never applied**.

## 2. The repository's migration identity

| Repo file | Size | What it does |
|---|---|---|
| `V175__allow_independent_teachers.sql` | 1,367 B | `ALTER TABLE teachers ALTER COLUMN institution_id DROP NOT NULL`; `ALTER TABLE live_classes ALTER COLUMN institution_id DROP NOT NULL`; `CREATE UNIQUE INDEX IF NOT EXISTS uq_teachers_user_independent ON teachers (user_id) WHERE institution_id IS NULL` |
| `V176__allow_institutionless_audit_logs.sql` | — | Makes audit-log rows legal without an institution |
| `V177__subscription_lifecycle.sql` | — | Line 1: `-- V177: subscription lifecycle records.` |

### 2.1 This is a version collision, not merely a checksum mismatch

`V177__subscription_lifecycle.sql` line 1 describes **exactly the migration the shared database
recorded at version 175**. `PAYMENT-PROGRESS.md` §3 records why:

> **`V175` collision.** Upstream and payment work both claimed V175 — Flyway refuses to start.
> Renamed payment migration to V177.

So two different pieces of content both claim version 175:

| Version | `elmkusoma` (shared) | Repository (current) |
|---|---|---|
| 175 | `subscription lifecycle` (applied, checksum `222681029`) | `allow_independent_teachers` (**never applied**) |
| 176 | *not yet read — see §4* | `allow_institutionless_audit_logs` |
| 177 | *not yet read — see §4* | `subscription_lifecycle` (the renamed migration) |

Consequence: `flyway validate` fails **twice over** — a checksum mismatch at 175, and a
content mismatch for `subscription_lifecycle` between 175 and 177. Fixing only the checksum would
leave the collision in place.

## 3. Relevant files and dependency chain

All under `backend/elmkusoma-core/src/main/resources/db/migration/`:

- `V169__pricing_policies.sql` → `V170__financial_integrity_refunds_receipts_commission.sql`
  → `V171__refund_integrity.sql` → `V172__stakaba_gateway_integrity.sql`
  → `V173__event_monetisation.sql` → `V174__parent_student_link_integrity.sql`
  → **`V175__allow_independent_teachers.sql`** → `V176__allow_institutionless_audit_logs.sql`
  → **`V177__subscription_lifecycle.sql`** → `V178__payment_purchaser_attribution.sql`
  → `V179__payment_beneficiary_attribution.sql` → `V180__bundles.sql` → `V181__email_outbox.sql`
  → `V182__security_events_institution_nullable.sql` → `V183__bundle_refund_split.sql`

`V174` is **verified correct**: a test re-applies the chain to a disposable database and confirms
the computed checksum equals the recorded `797143152`.

## 4. How to validate the discrepancy safely

Perform **read-only** first. Nothing below writes to `elmkusoma`.

**Step 1 — read the full version range (read-only):**

```sql
SELECT installed_rank, version, description, checksum, success, installed_on
FROM flyway_schema_history
WHERE version >= '174'
ORDER BY installed_rank;
```

This fills the two *"not yet read"* cells in §2.1 and is the single most important missing fact.

**Step 2 — confirm what the shared database physically has (read-only):**

```sql
SELECT column_name, is_nullable FROM information_schema.columns
WHERE table_name IN ('teachers','live_classes') AND column_name = 'institution_id';

SELECT indexname FROM pg_indexes WHERE tablename = 'teachers'
  AND indexname = 'uq_teachers_user_independent';

SELECT to_regclass('public.subscriptions') IS NOT NULL AS has_subscriptions;
```

Expected today: `institution_id` **NOT NULL** on both tables, the partial unique index **absent**,
`has_subscriptions` **true** — i.e. the repo's V175 never ran here.

**Step 3 — validate against a scratch schema, never against `elmkusoma`:**

A standalone Flyway 10.20.1 runner (the same harness used to validate V183) pointed at an
**empty scratch schema** already applied the full chain cleanly:

```
migrationsExecuted = 179, targetSchemaVersion = 183
```

That proves the repository's chain is internally consistent in isolation. Repeat it, then repeat
against a scratch schema seeded to mirror `elmkusoma`'s recorded 174/175 rows — the second run is
the one that reproduces the collision without touching shared state.

**Never** run `flyway repair`, `flyway repair -repairCallbacks`, delete rows, edit a checksum,
or `baseline` against a shared database. All of these destroy the only record of what actually ran.

## 5. Candidate recovery options and their risks

### Option A — Re-point the repo's teacher migration to a free version (RECOMMENDED)

Rename `V175__allow_independent_teachers.sql` → `V184__allow_independent_teachers.sql` (and
`V176__allow_institutionless_audit_logs.sql` → `V185__…` if §4 step 1 shows 176 also collides),
leaving `V177__subscription_lifecycle.sql` where it is.

- **Fixes:** the checksum mismatch at 175 disappears because the repo no longer claims 175.
- **Does not by itself fix:** the recorded `175 = subscription lifecycle` row has no file in the
  classpath, so Flyway reports *"Detected applied migration not resolved locally: 175"*. That
  still needs §5 Option B's authorisation to reconcile the row.
- **Risk:** low — the DDL is idempotent (`DROP NOT NULL`, `IF NOT EXISTS`) and forward-only.
- **Consequence:** the shared database receives the teacher/audit DDL at 184/185, applied for the
  first time, under a version nobody has recorded.

### Option B — Reconcile the recorded 175 row to point at `subscription_lifecycle`

Rewrite `flyway_schema_history` so version 175 carries the description/checksum of the migration
that actually ran, and let the repo's `V177` be understood as that same migration at a different
rank — **or** delete the stale 175 row and re-apply as 177.

- **Risk: HIGH.** Every variant edits history. A wrong checksum silently marks an unapplied
  migration as applied.
- **Only acceptable if** §4 step 1 shows the shared database's 176/177 rows are empty or
  consistent with `subscription_lifecycle` having run once, and the owner accepts the edit.
- **This is the step that requires explicit database-owner authorisation.**

### Option C — Do nothing, keep the app pointed elsewhere

Valid only while nothing needs to boot against `elmkusoma`. No data risk, no progress on D-06.

### Option D — Rejected outright

Editing a checksum to match, deleting `flyway_schema_history` rows, or baselining over the
mismatch. All three destroy the audit trail of what actually executed, and `Payment.md` §58
requires financial history to remain traceable. **Not to be performed under any circumstance.**

## 6. Exact approval required

The database owner must state, in writing, **one** of:

1. *"Authorise Option A + Option B against `elmkusoma`: rename the repo migrations as specified,
   then reconcile the recorded version-175 row as specified."*
2. *"Authorise Option A only; leave history untouched and keep the application off `elmkusoma`."*
3. *"Defer — no changes."*

Approval must name the **database** (`elmkusoma`), the **action**, and the **person** authorising
it. It does not extend to `elmkusoma_test` or any other database.

## 7. Rollback and post-recovery verification

**Rollback (before any Flyway run):**

```sql
CREATE TABLE flyway_schema_history_backup_<date> AS
  SELECT * FROM flyway_schema_history;
```
plus `pg_dump --schema-only elmkusoma > backup_<date>.sql`. Repo renames are revertible by
`git revert` — never by force-push.

**Post-recovery verification:**

1. `flyway validate` against `elmkusoma` exits 0 with no checksum or resolve errors.
2. `SELECT * FROM flyway_schema_history WHERE version >= '174'` shows each version exactly once,
   with `success = true` and no duplicate version numbers.
3. §4 step 2 re-run shows `institution_id` nullable on `teachers` and `live_classes`,
   `uq_teachers_user_independent` present, `subscriptions` unchanged.
4. The application boots against `elmkusoma`.
5. `SubscriptionMigrationConstraintTest` passes (9/9) — it asserts no gap from V170 upward.
6. `FlywayMigrationValidationTest` passes — naming convention, monotonic versions, declared columns.
7. A scratch-schema chain run still reports `targetSchemaVersion` equal to the highest repo
   migration and `migrationsExecuted` matching the file count.

D-06 moves off BLOCKED only when steps 1–7 pass **and** the authorisation in §6 is on record.
