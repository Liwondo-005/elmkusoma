ELMKUSOMA PAYMENTS & TRANSACTIONS

MASTER IMPLEMENTATION PROMPT — COMPLETE AUDIT, SPECIFICATION, IMPLEMENTATION AND VERIFICATION

Repository: "Liwondo-005/elmkusoma"
Master file: "Payment.md" at repository root
Backend: "backend/elmkusoma-core"
Frontend: "frontend"
Stack: Existing Spring Boot, Spring Security, JPA, Flyway, PostgreSQL, Redis, RabbitMQ, Next.js, React, TypeScript and Tailwind architecture, subject to verification against the current repository
Currency: TZS
Implementation ownership: One implementation owner — the repository owner
Execution model: Sequential, evidence-driven, phase-gated implementation
Primary objective: Complete the existing ELMKUSOMA payment ecosystem without duplicating modules, corrupting financial records, weakening authorization or falsely claiming completion.

---

PART I — MISSION AND RULES

1. Your mission

Act as a senior full-stack engineer, payment-systems architect, database migration specialist, application-security engineer, QA engineer and technical auditor.

Your task is to audit, upgrade and maintain the existing root-level "Payment.md" as the single authoritative master specification for completing ELMKUSOMA Payments & Transactions.

The file must describe the entire scope discussed for ELMKUSOMA payments, including:

- Business requirements.
- User journeys.
- Role and permission requirements.
- Frontend navigation and dashboards.
- Backend API contracts.
- Domain models and financial state transitions.
- Pricing and subscription policies.
- Payment gateway integration.
- Orders, transactions and payment attempts.
- Receipts and invoices.
- Courses, resources, modules and live-session purchases.
- Event bookings, group bookings and seat capacity.
- Parent/guardian and child financial relationships.
- Provider, teacher, trainer, content creator and organizer earnings.
- Refunds, cancellations and disputes.
- Revenue allocation and future settlements.
- Platform Admin financial governance.
- Notifications.
- Reconciliation.
- Audit logs.
- Database integrity and migration safety.
- Security and IDOR prevention.
- Automated tests and regression testing.
- Deployment readiness and operational restrictions.
- Requirement tracking and final completion evidence.

This is not a request to create a second payment application or a replacement architecture.

Extend and complete the actual existing ELMKUSOMA system.

2. One owner, no developer split

The user is performing the implementation personally.

Do not divide the project into Developer 01, Developer 02, parallel developers, imaginary teams or independent implementation tracks.

Organize work into sequential phases performed by one implementation owner.

Use clear checkpoints, prerequisites, evidence, completion criteria and next actions.

Do not stop at a high-level plan when implementation is authorized and safe. However, do not perform dangerous database mutations, activate real payments or make irreversible financial changes without the necessary explicit approval.

3. Mandatory engineering process

Follow this sequence:

UNDERSTAND → AUDIT → CLASSIFY → PLAN → VERIFY CONTRACTS → IMPLEMENT → INTEGRATE → TEST → VERIFY → HARDEN → DOCUMENT

Never skip the audit because a requirement sounds familiar.

Never implement a duplicate feature without checking the existing implementation.

Never claim a feature is finished merely because code was added.

4. Existing architecture takes precedence

Inspect the actual current repository before making decisions.

Reuse existing:

- Payment entities.
- Order models.
- Transaction models.
- Payment-attempt records.
- Gateway adapters.
- Subscription services.
- Entitlement services.
- Event booking services.
- Receipt generation.
- Notification infrastructure.
- User and parent-child relationships.
- Provider and institution ownership models.
- RBAC and permission conventions.
- API response conventions.
- Database migration conventions.
- Frontend design system.
- Routes and sidebar navigation.
- Test fixtures and integration-test architecture.

Create a new service, component, endpoint, permission or table only when a demonstrated gap requires it.

Do not rebuild an existing working module just to fit a preferred design.

5. Required status classifications

Every requirement must be assigned one of these statuses:

- "DONE"
- "PARTIAL"
- "MISSING"
- "BROKEN"
- "NEEDS VERIFICATION"
- "N/A"

Definitions:

- "DONE": implemented and verified with relevant evidence.
- "PARTIAL": partially implemented; the missing behavior is identified.
- "MISSING": required capability does not exist.
- "BROKEN": capability exists but fails, violates its contract or creates a material risk.
- "NEEDS VERIFICATION": available evidence is insufficient.
- "N/A": not applicable, with an explicit reason.

Do not mark something "DONE" based only on a class name, route, migration, menu item, comment, mocked response or historical test result.

6. Data, security and operational safeguards

Never:

- Invent financial records.
- Invent users, parents, children, students, bookings or gateway transactions.
- Manufacture success metrics.
- Trust a frontend payment-success message.
- Grant paid access based on an unverified callback.
- Rewrite financial history to make totals appear correct.
- Delete orphan records without an approved data-recovery decision.
- Rewrite historical migrations merely to silence Flyway.
- Run destructive SQL against a shared or production database without approval.
- Disable meaningful tests to get a green build.
- Expose credentials in code, logs, browser bundles or documentation.
- Claim real gateway testing when only mocks were used.
- Activate live collections, refunds or payouts without the required readiness and approval.

---

PART II — PHASE 0: REPOSITORY BASELINE AND DOCUMENT CONTROL

7. Inspect the actual repository

Before editing any files, determine:

1. Repository root.
2. Current Git branch.
3. Current commit.
4. Whether the working tree is clean.
5. Whether the branch is ahead of or behind its upstream.
6. Whether there are staged, unstaged or untracked changes.
7. Whether another process is already building or modifying the repository.
8. The actual paths of the backend and frontend.
9. The current location and contents of "Payment.md".
10. Existing payment-related documentation and matrices.

Preserve all user changes.

Do not reset, clean, force-push, discard, overwrite or revert unrelated work.

If the existing "Payment.md" has valuable requirements absent from this specification, incorporate them unless they are obsolete or contradictory. Record important conflicts rather than silently discarding decisions.

8. Upgrade the existing Payment.md

The deliverable for this phase is an upgraded version of the existing root-level "Payment.md", not a new competing master document.

The upgraded document must:

- Contain all requirements in this prompt.
- Preserve relevant valid requirements already in the file.
- Identify unresolved business decisions.
- Define implementation phases and phase gates.
- Define role permissions and scope.
- Define payment and fulfillment invariants.
- Define frontend and backend requirements.
- Define migration and data-integrity safeguards.
- Define test coverage and acceptance criteria.
- Define production-readiness restrictions.
- Define final evidence and reporting requirements.

Avoid unnecessary duplication.

Use stable requirement identifiers so each requirement can be tracked.

Do not reduce the document to a short summary.

Do not replace detailed requirements with statements such as “implement payment functionality” or “make the system secure.”

Write specific, testable requirements.

After editing, verify the file's actual contents, size and section coverage. If any required section is missing, correct it before declaring Phase 0 complete.

---

PART III — PHASE 1: COMPLETE PAYMENT AUDIT

9. Audit the backend

Search the actual backend for all payment-related implementations, including:

- Controllers.
- Services.
- Service implementations.
- DTOs.
- Entities.
- Repositories.
- Gateway adapters.
- Callback handlers.
- Webhook handlers.
- Security configuration.
- Authorization annotations and permission checks.
- Subscription and entitlement code.
- Pricing services.
- Event booking integration.
- Receipt and invoice generation.
- Refund processing.
- Earnings calculations.
- Notification integration.
- Scheduled jobs.
- Background workers.
- Database migrations.
- Unit tests.
- Integration tests.
- Configuration files.

Identify the actual file paths and explain their responsibilities.

Do not assume package names or classes exist merely because they appear in this prompt or an earlier report.

10. Audit the frontend

Inspect:

- Existing payment routes.
- Transaction history pages.
- Payment forms and initiation flows.
- Purchase flows.
- Subscription pages.
- Receipts and invoice views.
- Refund interfaces.
- Provider earnings.
- Event booking and checkout pages.
- Role-specific dashboards.
- Sidebar configuration.
- Permission-aware navigation.
- API client functions.
- DTO and TypeScript interfaces.
- Loading and error states.
- Responsive behavior.
- Accessibility.
- Frontend tests.

Determine which existing components can be extended and which genuine gaps require implementation.

11. Audit authorization

Identify the actual roles, permissions, scopes and resource relationships.

Document:

- Who may initiate payments.
- Who may view transactions.
- Who may download receipts.
- Who may request refunds.
- Who may approve refunds.
- Who may change pricing.
- Who may access provider earnings.
- Who may view institution-scoped information.
- Who may manage gateway configuration.
- Who may run reconciliation.
- Who may export financial reports.
- Which actions remain disabled.

Inspect both frontend visibility and backend enforcement.

Frontend visibility is not a substitute for backend authorization.

12. Audit payment lifecycle integration

Trace an actual purchase through the code:

"Service → Price Resolution → Order → Payment Attempt → Gateway → Verification → Fulfillment → Entitlement/Booking → Receipt → Notification → Reconciliation"

For each transition, identify:

- Source file.
- Method or endpoint.
- Persisted state.
- Transaction boundary.
- Authorization check.
- Failure behavior.
- Retry behavior.
- Idempotency behavior.
- Relevant tests.

Identify broken or missing links.

Do not infer integration merely because two services exist.

13. Produce an evidence-based audit report

Before implementation, provide:

1. Repository baseline.
2. Existing architecture.
3. Existing payment features.
4. Role and permission matrix.
5. API and database map.
6. Gateway readiness.
7. Test baseline.
8. Security findings.
9. Migration risks.
10. Requirement classification.
11. Prioritized implementation plan.
12. Blockers requiring user decisions.

Do not start risky schema modifications while the baseline remains uncertain.

---

PART IV — PAYMENT DOMAIN AND FINANCIAL INVARIANTS

14. Distinguish the payment domain concepts

Audit whether the existing system correctly separates the following concepts:

1. Service catalogue and pricing policy.
2. Order or purchase.
3. Payment transaction.
4. Payment attempt.
5. Gateway reference.
6. Subscription.
7. Entitlement.
8. Receipt or invoice.
9. Refund or financial adjustment.
10. Event booking and seat allocation.
11. Beneficiary allocation.
12. Earnings.
13. Settlement or payout.
14. Reconciliation exception.
15. Audit event.

These are logical domain responsibilities, not a requirement to create fifteen new database tables.

Reuse existing models where appropriate.

15. Core transaction lifecycle

Implement the appropriate lifecycle for the configured gateway:

1. Authenticate the requester.
2. Authorize the operation and resource scope.
3. Resolve the service from trusted backend data.
4. Validate publication, availability and purchase eligibility.
5. Resolve the applicable approved price.
6. Calculate the amount on the server.
7. Create or reuse the appropriate order.
8. Persist a price and currency snapshot.
9. Create an idempotent payment attempt.
10. Initiate the gateway operation.
11. Persist the initiation result.
12. Verify the gateway's authoritative transaction outcome.
13. Validate the transaction reference, merchant, amount and currency.
14. Persist the verified outcome safely.
15. Fulfil the correct purchase, entitlement or booking exactly once.
16. Generate or finalize the applicable receipt.
17. Emit appropriate notifications.
18. Update authorized transaction views.
19. Reconcile unresolved discrepancies.
20. Preserve audit history.

Adapt the exact sequence to the gateway's documented protocol and the current application architecture.

Do not hold database locks during slow network calls without a justified design.

16. Required invariants

- Frontend success is never authoritative.
- An unverified callback cannot grant paid access.
- A pending payment is not a successful payment.
- A failed payment cannot fulfil a purchase.
- Duplicate callbacks cannot duplicate financial effects.
- A mismatched amount or currency cannot fulfil an order.
- Unknown references must not be silently attached to users.
- A successful payment must not fulfil twice under concurrent processing.
- A notification failure must not reverse verified payment.
- A refund request is not proof of completed refund.
- Earnings are not proof of a payout.
- A receipt must reflect the verified financial state.
- Price changes must not silently rewrite existing orders.
- Financial corrections must preserve the original transaction.
- Invalid or suspicious gateway events must be auditable.
- External gateway calls and local database transactions must have explicit failure and recovery handling.

Define permitted state transitions using the existing domain model. Reject invalid transitions.

---

PART V — PRICING AND COMMERCIAL POLICIES

17. Status of proposed prices

The following values are proposed business defaults from the project discussion. They are not permission to activate live charging automatically.

Inspect the current policy and configuration first.

If a conflicting approved policy already exists, preserve it and report the conflict.

18. Proposed access packages

Package| Proposed price| Duration
Daily Access| TZS 500| Rolling 24 hours
Weekly Access| TZS 1,000| Rolling 7 days
Monthly Access| TZS 5,000| Rolling 30 days

Define exactly which services each package includes before activation.

Do not interpret rolling 30 days as a calendar month.

19. Proposed subscription tiers

Tier| Proposed price| Period
Free| TZS 0| No paid period
Basic| TZS 3,000| 30 days
Plus| TZS 5,000| 30 days
Premium| TZS 7,000| 30 days

The tier benefits, exclusions, upgrade/downgrade rules and auto-renewal policy remain subject to explicit approval.

Do not assume Premium grants every service in the platform.

Do not enable recurring charges without an approved renewal policy and supported gateway behavior.

20. Proposed individual prices

Service| Proposed price
Resource, notes or revision material| TZS 500
Standard course| TZS 2,000
Academic/vocational module| TZS 1,000
Live session| TZS 500
Replay| TZS 1,000
Eligible certificate reprint| TZS 5,000
Seminar/workshop/conference ticket| TZS 2,000
Eligible community/NGO/CBO/religious/corporate event ticket| TZS 200 per participant

Distinguish a certificate reprint from the original certificate award.

Validate the actual service type and eligibility before applying a proposed price.

21. Proposed education price bands

Category| Minimum| Maximum
Primary| TZS 500| TZS 3,000
Secondary| TZS 1,000| TZS 5,000
Higher education/college/university/vocational| TZS 1,000| TZS 15,000

Apply these bands only to applicable education products.

Do not silently clamp a conflicting fixed price to fit a band. Report the conflict and request a policy decision.

22. Proposed bundles

Bundle| Proposed price
Five resources| TZS 5,000
Ten resources| TZS 8,000
Four live classes| TZS 10,000
Three courses| TZS 35,000
Five-seat group booking| TZS 45,000
Ten-seat group booking| TZS 85,000

Before activation, validate bundle membership, quantity, availability, duplicate entries, price snapshots, entitlement fulfillment, expiry and refund behavior.

23. Unresolved commercial decisions

Do not invent:

- Subscription benefits.
- Auto-renewal rules.
- Commission percentages.
- Gateway fee allocation.
- Tax treatment.
- Refund windows.
- Cancellation and proration rules.
- Discounts.
- Payout schedules.
- Minimum payout amounts.
- Provider settlement rules.
- Reserve and dispute policies.

Record each decision as:

- Decision ID.
- Question.
- Existing implementation.
- Options.
- Financial or technical impact.
- Recommended safe default, if appropriate.
- Approval status.
- Required approver.

Keep unapproved policy disabled.

24. Platform Admin pricing governance

Reuse the existing authorization and configuration architecture.

Where supported, provide:

- Price policy creation.
- Review and approval.
- Versioned publication.
- Permitted minimum and maximum prices.
- Service category restrictions.
- Offer activation/deactivation.
- Audit history.
- Policy preview.
- Safe future-price changes.

Preserve order-level price snapshots.

Do not introduce a permanent Super Admin above Platform Admin.

---

PART VI — GATEWAY INTEGRATION AND READINESS

25. Verify the real gateway

Inspect the actual integration, including Stakaba if present.

Verify official documentation for:

- Tanzania support.
- TZS support.
- Merchant onboarding.
- Mobile money.
- Bank payments.
- Card payments.
- Initiation endpoints.
- Transaction-status queries.
- Callback authentication.
- Reference handling.
- Idempotency.
- Refunds.
- Reversals.
- Settlement and payout support.
- Sandbox availability.
- Fees.
- Timeouts.
- Retry rules.
- Reconciliation.

Do not infer production support from a mocked service or configuration placeholder.

26. Gateway security

- Keep credentials outside source code.
- Use the established environment/secrets mechanism.
- Never expose secrets in frontend bundles.
- Never log sensitive credentials.
- Mask gateway identifiers where appropriate.
- Validate the callback according to the gateway protocol.
- Validate reference, amount, currency and merchant.
- Reject or safely quarantine unknown callbacks.
- Make callback processing idempotent.
- Handle replay and out-of-order delivery.
- Reconcile ambiguous outcomes.

If gateway documentation requires a server-side transaction query after a callback, implement that verification.

27. Live-operation restrictions

Until all relevant approvals and readiness checks are complete:

- Do not activate real-money collection.
- Do not execute real refunds.
- Do not execute real payouts.
- Do not represent mock results as sandbox transactions.
- Do not represent sandbox transactions as production payments.
- Do not claim that production onboarding is complete without evidence.

Record separately:

1. Code implemented.
2. Mock tests passed.
3. Sandbox credentials configured.
4. Real sandbox transactions verified.
5. Production credentials configured.
6. Production onboarding approved.
7. Live collections authorized.
8. Refund capability verified.
9. Settlement/payout capability verified.

---

PART VII — UNIFIED FRONTEND AND NAVIGATION

28. One coherent Payments & Transactions area

Use one shared payment module and the existing ELMKUSOMA design system.

Do not build a different duplicate payment application for every role.

The applicable areas should include:

- Overview.
- Transaction history.
- Transaction details.
- Orders and purchases.
- Payment attempts.
- Receipts and invoices.
- Subscriptions and access passes.
- Bundles and entitlements.
- Refunds and cancellations.
- Earnings and allocations.
- Settlements/payouts, only when supported.
- Reconciliation and exceptions for authorized users.
- Notifications.
- Reports and exports.
- Platform Admin controls.

Map these capabilities to actual existing routes. Do not create duplicate pages when an equivalent page already exists.

29. Required transaction information

Display applicable:

- Transaction reference.
- Order reference.
- Service name and type.
- Payer.
- Authorized beneficiary.
- Amount and currency.
- Payment method.
- Current status.
- Relevant timestamps.
- Status timeline.
- Gateway reference, appropriately masked.
- Receipt.
- Subscription or entitlement.
- Event booking or seat.
- Refund history.
- Available actions and their eligibility.

Do not conflate the payer, student, parent, attendee, provider, organizer and beneficiary.

30. Frontend interaction states

Implement:

- Loading state.
- Empty state.
- Error state.
- Retry state.
- Pending state.
- Verified success state.
- Failed state.
- Cancelled state.
- Expired state.
- Refund states supported by the domain.
- Disabled-action explanations.
- Pagination and filtering states.

Do not use color alone to communicate payment status.

Do not show fake metrics.

Do not show success when an operation has failed.

31. Visual and accessibility requirements

- Preserve ELMKUSOMA's existing design language.
- Use the established palette and shared components.
- Avoid unrelated dark admin templates.
- Avoid excessive gradients, shadows and decorative cards.
- Support small mobile screens.
- Support low-bandwidth usage.
- Use accessible labels and keyboard navigation.
- Provide clear status text.
- Avoid unnecessary large payloads.
- Prevent accidental duplicate submissions.
- Ensure responsive tables or equivalent mobile layouts.
- Follow the application's existing localization conventions.

---

PART VIII — ROLE-SPECIFIC REQUIREMENTS

32. Student

Support authorized access to:

- Own transactions.
- Own orders.
- Own payment attempts.
- Own receipts.
- Own subscriptions.
- Own access periods and entitlements.
- Paid courses, modules, resources, live sessions and replays.
- Event bookings.
- Payment retries.
- Eligible refund requests.
- Relevant notifications.

A student cannot access another student's financial records by manipulating an identifier.

33. Parent/guardian

Support:

- Parent's own transactions.
- Verified linked-child selection.
- Child-specific payments and services.
- Authorized receipts.
- Relevant subscriptions and access.
- Pending and failed payments.
- Authorized unpaid-service information where an actual fee model exists.
- Group event bookings.
- Relevant refund information.
- Financial notifications.

Resolve access from authoritative parent-child relationships.

Do not use numeric ID equality as a substitute for a verified relationship.

34. Teacher/trainer/content creator

Support authorized access to:

- Their eligible service sales.
- Gross revenue.
- Refunds.
- Approved fees and commissions.
- Net eligible earnings.
- Pending and held earnings.
- Available earnings where supported.
- Actual settled amounts only where verified.
- Service-level reports.
- Settlement history only when the payout system exists.

Do not expose unnecessary payer personal data.

35. Host/event organizer

Support:

- Event transactions.
- Ticket sales.
- Group bookings.
- Individual bookings.
- Seat capacity.
- Seat reservations.
- Seat utilization.
- Payment status.
- Refunds.
- Gross revenue.
- Approved deductions.
- Net eligible earnings.
- Authorized reports.

Seat counts must come from authoritative booking data.

36. Provider

Support authorized access to:

- Provider-owned service sales.
- Orders and transactions.
- Gross revenue.
- Refunds.
- Approved commissions and fees.
- Net eligible earnings.
- Pending, held, available and settled amounts where implemented.
- Bundle performance.
- Refund cases.
- Reports.
- Authorized institution/service breakdowns.
- Relevant staff activity.

A Provider may represent an organization or authorized individual and is not necessarily an educational institution.

37. Provider Admin

Restrict access to the assigned Provider and explicitly delegated permissions.

Do not grant access to other Providers, global payment controls, gateway secrets or platform-wide financial records without an explicit, justified permission and scope.

38. Institution/College Admin

Where supported, provide:

- Institution-scoped transactions.
- Authorized student/cohort summaries.
- Relevant receipts.
- Applicable pending payments.
- Refund information.
- Institution earnings only if the institution is an approved beneficiary.

Do not assume every institution is entitled to a share of every payment.

39. NGO/CBO, religious and corporate service owners

Reuse the existing Provider/service-owner model wherever possible.

Support their own eligible services, bookings, transactions, refunds and allocations.

Do not create redundant financial roles or modules.

40. Ordinary registered users

Support authorized access to the user's own purchases, transaction history, receipts, subscriptions and entitlements.

41. Platform Admin

Provide global governance and operational visibility over:

- Transactions.
- Payment attempts.
- Gateway health/readiness.
- Callback exceptions.
- Pricing policies.
- Subscriptions and entitlements.
- Refunds.
- Provider and creator earnings.
- Allocations.
- Reconciliation.
- Receipts and invoices.
- Authorized reports and exports.
- Audit logs.
- Relevant payment-related support operations.

Platform Admin is the highest platform governance authority.

Do not create a permanent Super Admin above it.

Do not create separate Finance Staff or Payment Support Staff roles.

42. Other existing roles

Inspect all actual roles.

Document each role's required navigation, accessible records, permitted actions and restrictions.

Do not invent roles.

Where a user has multiple roles, combine only the permissions legitimately granted by those roles and their scopes.

---

PART IX — BACKEND API CONTRACTS

43. API audit

Inventory existing endpoints before proposing new ones.

For each endpoint, record:

- Route and HTTP method.
- Controller.
- Request DTO.
- Response DTO.
- Service method.
- Repository dependencies.
- Authentication.
- Authorization.
- Ownership/scope.
- Validation.
- Transaction boundary.
- Error handling.
- Idempotency.
- Audit behavior.
- Tests.
- Frontend consumers.

Reuse established API conventions.

Do not break existing consumers unnecessarily.

44. Required API capabilities

Where missing and justified by the actual architecture, provide the relevant contracts for:

- Listing authorized transactions.
- Retrieving a transaction.
- Listing orders.
- Creating an eligible purchase.
- Initiating a payment attempt.
- Checking payment status.
- Retrying an eligible attempt.
- Viewing receipts.
- Viewing subscriptions.
- Viewing entitlements.
- Requesting a refund.
- Reviewing refunds with proper permissions.
- Viewing authorized earnings.
- Viewing reconciliation exceptions.
- Updating approved pricing policy.
- Producing authorized reports.

These are capability requirements, not mandatory endpoint names. Follow existing naming conventions.

45. API validation

- Resolve prices server-side.
- Validate amounts and currency.
- Validate order ownership.
- Validate beneficiary eligibility.
- Validate event capacity.
- Validate subscription eligibility.
- Validate idempotency keys where supported.
- Reject unexpected state transitions.
- Enforce permissions for exports and downloads.
- Prevent mass assignment of financial state.
- Never accept arbitrary "success=true" or "paid=true" as proof of payment.
- Use consistent errors.
- Avoid leaking private record existence.

---

PART X — DATABASE, FLYWAY AND MIGRATION SAFETY

46. Audit the actual schema

Inspect:

- Flyway migration files.
- Actual database migration history.
- Physical tables and columns.
- Constraints.
- Indexes.
- Foreign keys.
- Unique constraints.
- Extensions.
- Existing financial records.
- Orphan relationships.
- Entity-to-schema mappings.

Do not assume local schema and migration history are synchronized.

47. Historical V174–V177 warnings

Earlier reports raised possible discrepancies involving:

- "V174__parent_student_link_integrity.sql".
- A recorded V174 checksum reportedly different from the local file checksum.
- Different historical meanings for V175.
- "V175__allow_independent_teachers".
- "V176__allow_institutionless_audit_logs".
- "V177__subscription_lifecycle".
- Subscription lifecycle schema artifacts.
- "ex_subscriptions_active_no_overlap".
- "btree_gist".
- Potential missing independent-teacher schema artifacts.
- Possible orphan parent-child links.

These reports are not current-state proof.

Recheck the current repository and database.

Do not blindly run Flyway repair.

Do not rewrite an applied historical migration.

Do not delete Flyway history.

Do not baseline an uncertain database.

Do not invent student records.

Do not delete orphan links without a verified recovery decision.

Do not mutate a shared database before the discrepancy has been documented and the recovery plan explicitly approved.

48. Safe migration procedure

For every required schema change:

1. Identify the verified defect or requirement.
2. Inspect the actual starting schema.
3. Determine whether the target database is disposable or shared.
4. Create an incremental migration following repository conventions.
5. Rehearse the migration on a suitable disposable database.
6. Verify constraints, indexes and data preservation.
7. Test upgrade behavior from supported states.
8. Record any required backup or rollback/recovery procedure.
9. Obtain approval before risky shared-database changes.
10. Run applicable integration tests.
11. Document the result.

Do not add duplicate schema objects to conceal an unresolved migration-history conflict.

---

PART XI — SUBSCRIPTIONS AND ENTITLEMENTS

49. Subscription lifecycle

Verify and implement the approved behavior for:

- First purchase.
- Renewal.
- Expiry.
- Access checks.
- Period extension.
- Failed renewal.
- Duplicate payment events.
- Concurrent activation.
- Cancellation.
- Refund interaction.
- Audit history.

A payment and an entitlement must remain distinguishable.

50. Period calculations

Where confirmed as the existing approved rule:

- Renewal during an active paid period starts after the existing period ends.
- A purchase after the previous period has elapsed starts at the approved effective time.
- Use the persisted duration snapshot, such as "payment.getAccessDays()", where that field is authoritative.
- Use "[start, end)" boundaries.
- The exact end instant does not grant access.
- Preserve previous periods.
- Avoid duplicate or invalid overlapping periods.
- Do not shorten an already purchased period through an unrelated price update.

Verify these rules against actual code and tests before modifying implementation.

51. Expiry scheduler

Inspect the existing scheduled jobs and service callers.

Earlier reports mentioned an existing "CoreScheduler" with approximately fourteen "@Scheduled" methods and a possible missing "expireElapsed()" operational caller. Recheck the actual repository.

Reuse an appropriate existing scheduler if available.

Add a new scheduler only if a verified gap exists.

Test idempotency, concurrency, exact boundaries, failures and recovery.

52. Concurrency

Review existing locking and database constraints.

Verify that:

- Concurrent first purchases cannot create invalid duplicate subscriptions.
- Concurrent renewals follow the approved business rule.
- Duplicate callbacks cannot duplicate subscription activation.
- Transactions roll back safely when activation fails.
- Database constraints match application-level invariants.

A historical passing test is not sufficient; rerun relevant tests on the current tree.

---

PART XII — EVENT BOOKINGS AND SEAT CONFIRMATION

53. Payment and booking relationships

Support both:

- Organizer-paid group bookings.
- Individually paid participant bookings.

Keep payer, parent, student, attendee, organizer, provider, order, payment and booking relationships distinct.

Never assume the same numeric identifier represents the same person across tables.

54. Historical integration warning

Earlier reports raised a possible issue involving "Payment.parentId" and "studentId" both being populated with an attendee user ID, despite representing different identifier spaces.

Recheck the actual code.

Where the authoritative model uses "parent_student_links" and "students.user_id", use those relationships to resolve identity and eligibility.

Do not invent student records or silently delete orphan relationships.

55. Gateway-to-booking integration

Verify the actual gateway-to-"EventBookingService.confirmSeat" path or its current equivalent.

Test:

- Pending payments do not confirm paid seats.
- Failed payments do not confirm paid seats.
- Invalid callbacks do not confirm paid seats.
- Amount mismatches do not confirm seats.
- Verified success confirms the correct booking once.
- Duplicate callbacks do not duplicate seat allocation.
- Concurrent bookings cannot oversell capacity.
- Organizer-paid groups follow the approved group rules.
- Individual payments remain linked to the correct participant.
- Refund/cancellation effects follow approved policy.

Use database locking, constraints or another appropriate concurrency mechanism consistent with the current architecture.

---

PART XIII — RECEIPTS, REFUNDS AND AUDIT

56. Receipts and invoices

Provide the applicable capabilities for:

- Unique receipt reference.
- Correct order and transaction association.
- Correct payer and service.
- Amount and currency.
- Transaction status and dates.
- Authorized view/download/print.
- Verification where supported.
- Refund references.
- Reissue/correction history.

Never issue a final paid receipt for an unverified transaction.

Never expose receipts through unrestricted or guessable download URLs.

57. Refund lifecycle

Use the actual established domain states where possible.

Support the applicable states for request, review, decision, gateway submission, pending confirmation, completion and failure.

Do not invent eligibility or processing windows.

Prevent duplicate refunds beyond the permitted amount.

Preserve the original transaction.

Do not mark a refund complete until the required authoritative confirmation exists.

Do not execute live refunds until the gateway, policy, authorization and testing are approved.

58. Financial audit

Audit events must record the appropriate actor, action, target, timestamp, result and correlation identifiers.

Protect logs from tampering and avoid unnecessary personal information.

Financial history must remain traceable.

Use explicit adjustments instead of silently editing historical transactions.

---

PART XIV — EARNINGS AND FUTURE PAYOUTS

59. Revenue model

The intended direction is platform-first collection, with future provider payouts.

The discussed model is platform/provider pricing with per-service revenue sharing.

Teacher and host fees are optional and require explicit approval.

Do not assume any commission percentage.

60. Required distinctions

Where supported, report separately:

- Gross sales.
- Refunds.
- Gateway fees.
- Approved platform commission.
- Approved beneficiary allocation.
- Net eligible earnings.
- Pending earnings.
- Held earnings.
- Available earnings.
- Settled amounts.
- Actual payout transactions.

Use consistent server-side calculations.

Avoid different dashboards independently calculating incompatible totals.

An allocation is not a payout.

An earnings balance is not a transfer.

A pending payout is not a completed payout.

Do not activate actual payout operations until policy, gateway support, approvals and operational safeguards exist.

---

PART XV — NOTIFICATIONS AND PARENT DASHBOARD INTEGRATION

61. Reuse existing notifications

Audit and reuse the current notification infrastructure.

Support relevant events such as:

- Payment initiated.
- Payment verified.
- Payment failed.
- Payment cancelled or expired.
- Receipt available.
- Subscription activated or expiring.
- Booking confirmed.
- Refund status changed.

Make duplicate notifications safe and respect the existing preference model.

Notifications must not independently grant access.

62. Parent financial notifications

Verify the actual integration of "ParentFinancialNotificationAssembler".

Earlier reports mentioned approximately seventeen unit tests and a possible gap in integration with "ParentDashboardServiceImpl", including "getChildNotifications" and "getAllChildrenNotifications".

These are historical findings; recheck them.

Where relevant, verify integration with:

"tz.elmkusoma.parent.dto.response.ParentNotificationResponse"

Requirements:

- Respect verified linked-child relationships.
- Respect "feeAlerts" or the actual established equivalent.
- Preserve assignments, attendance, live classes and announcements.
- Use stable, deduplicated notification identifiers.
- Avoid financial writes or duplicate side effects during ordinary dashboard reads.
- Add integration tests for the actual dashboard/API response.

Unit tests for a standalone assembler do not prove integration.

---

PART XVI — SECURITY AND IDOR TESTING

63. Mandatory security matrix

Test at least:

1. User A reads User B's transaction.
2. User A downloads User B's receipt.
3. Parent A accesses an unlinked child.
4. Provider A accesses Provider B's earnings.
5. Provider Admin changes global pricing.
6. Teacher accesses another teacher's sales.
7. Organizer modifies an unrelated event.
8. Institution Admin accesses another institution.
9. Unauthorized user requests a refund.
10. Unauthorized user approves a refund.
11. Unauthorized user exports financial records.
12. User manipulates transaction IDs.
13. User manipulates order IDs.
14. User manipulates gateway references.
15. User changes amounts or currencies.
16. User changes beneficiary identifiers.
17. User supplies forged payment-success values.
18. A callback is replayed.
19. A callback has a mismatched amount.
20. A callback references an unknown transaction.
21. Unauthorized users access reconciliation.
22. Unauthorized users initiate or approve payouts.
23. Parent/child access uses an invalid relationship.
24. Provider/institution filters are manipulated.
25. An unauthorized user changes payment policy.

Enforce all relevant checks in the backend.

Test both allowed and denied behavior.

Do not rely on frontend menu hiding.

---

PART XVII — AUTOMATED TESTING AND REGRESSION

64. Baseline before changes

Record actual current results for:

- Backend compilation.
- Backend unit tests.
- Backend integration tests.
- Flyway/schema validation.
- Frontend type checking.
- Frontend linting where configured.
- Frontend production build.
- Relevant end-to-end tests.
- Security tests.
- Migration tests.

Historical reports have included different backend test totals and frontend build results. They are not evidence of the current repository's state.

Rerun the relevant commands and record actual results.

65. Payment lifecycle tests

Cover:

- Verified success.
- Pending.
- Failure.
- Cancellation.
- Timeout.
- Duplicate callbacks.
- Replayed callbacks.
- Unknown references.
- Amount mismatch.
- Currency mismatch.
- Incorrect merchant.
- Out-of-order callbacks.
- Retry.
- Duplicate fulfillment.
- Receipt consistency.
- Notification failure.
- Reconciliation.
- Concurrent attempts.
- Partial-processing recovery.

66. Subscription tests

Cover:

- First purchase.
- Renewal before expiry.
- Renewal after expiry.
- Exact time boundaries.
- Persisted duration.
- Concurrent renewal.
- Duplicate callback.
- Expiry scheduler.
- Idempotency.
- Recovery.
- Database constraints.
- Entitlement correctness.

67. Event tests

Cover:

- Group organizer-paid booking.
- Individual participant payment.
- Correct identity relationships.
- Verified seat confirmation.
- Invalid payment rejection.
- Duplicate callback.
- Concurrent capacity limits.
- Cancellation and approved refund effects.
- Correct provider/organizer scope.

68. Frontend tests

Cover:

- Role navigation.
- Permission-dependent actions.
- Transaction listing.
- Filtering and pagination.
- Transaction details.
- Receipts.
- Subscription access.
- Refund states.
- Loading and empty states.
- Errors and retries.
- Mobile responsiveness.
- Accessibility.
- No fabricated financial values.
- No false success messages.
- No unauthorized record exposure.

69. Regression rules

Never disable tests simply to get a green build.

Never misrepresent mocked tests as gateway tests.

Inspect existing build processes before starting competing builds.

Do not terminate a running process without authorization.

For every test command, record the command, exit code, results, failures and skipped tests.

---

PART XVIII — RECONCILIATION AND OPERATIONAL READINESS

70. Reconciliation

Where the gateway supports it, compare authoritative gateway records with local transactions.

Identify:

- Gateway success with missing local fulfillment.
- Local success with uncertain gateway outcome.
- Unknown gateway references.
- Amount mismatches.
- Currency mismatches.
- Stale pending attempts.
- Duplicate gateway events.
- Refund inconsistencies.
- Earnings allocation discrepancies.

Do not silently modify balances to force reconciliation.

Classify exceptions and use authorized, auditable recovery procedures.

71. Operational visibility

Provide appropriate monitoring and logs for:

- Gateway initiation failures.
- Callback verification failures.
- Unknown references.
- Duplicate callbacks.
- Stale pending transactions.
- Fulfillment failures.
- Receipt failures.
- Notification failures.
- Refund discrepancies.
- Subscription expiry failures.
- Seat-capacity conflicts.
- Reconciliation exceptions.

Use correlation IDs.

Do not log secrets, authentication tokens or unnecessary personal data.

---

PART XIX — PHASED IMPLEMENTATION

72. Phase 0 — Master document and repository baseline

Deliver:

- Existing document review.
- Repository baseline.
- Upgraded "Payment.md".
- Preserved valid requirements.
- Identified unresolved decisions.
- No unnecessary duplicate documentation.

Exit gate: The specification is complete and the actual repository state is known.

73. Phase 1 — Complete audit and requirement matrix

Deliver:

- Backend map.
- Frontend map.
- API map.
- Database map.
- Role/permission matrix.
- Gateway readiness report.
- Baseline test results.
- Requirement classifications.
- Prioritized blockers.

Exit gate: Each requirement has a known status or explicit verification blocker.

74. Phase 2 — Critical security and data-integrity fixes

Address verified defects involving:

- IDOR.
- Gateway verification.
- Duplicate payment processing.
- Incorrect identity relationships.
- Subscription concurrency.
- Event seat integrity.
- Migration conflicts.
- Financial history.

Exit gate: Critical risks are fixed and tested, or explicitly blocked with documented evidence and required approval.

75. Phase 3 — Payment lifecycle and gateway

Complete the actual order/payment/attempt flow, server-side pricing, verification, idempotency, safe fulfillment and failure handling.

Exit gate: Lifecycle tests pass and gateway limitations are accurately recorded.

76. Phase 4 — Subscriptions and entitlements

Complete applicable subscriptions, access passes, expiry processing, purchase fulfillment and bundles.

Exit gate: Entitlements match verified purchases and subscription periods.

77. Phase 5 — Events, receipts and notifications

Complete event payment integration, booking integrity, receipts and applicable notifications.

Exit gate: Payment, booking, receipt and notification behavior is integrated and tested.

78. Phase 6 — Unified frontend

Complete role-aware Payments & Transactions navigation, dashboards, transaction history, details, receipts, subscriptions and applicable financial views.

Exit gate: Every applicable existing role has verified navigation and backend-enforced access.

79. Phase 7 — Earnings and financial governance

Complete applicable earnings, pricing controls, refunds and reconciliation.

Keep unsupported payouts disabled.

Exit gate: Financial figures are consistent and operational states are represented truthfully.

80. Phase 8 — Hardening and regression

Run the relevant backend, frontend, integration, security and migration tests.

Resolve regressions.

Exit gate: Completion claims are supported by current evidence.

81. Phase 9 — Final documentation and handover

Update the single requirements matrix, record completed work and document remaining blockers.

Exit gate: The repository and documentation accurately describe implementation and production readiness.

---

PART XX — REQUIREMENT MATRIX

82. Maintain one authoritative matrix

Use an existing appropriate matrix if one already exists. Otherwise, add the matrix to "Payment.md" or the established project documentation location.

Do not create competing matrices.

Use the following fields:

Field| Required content
Requirement ID| Stable identifier
Domain| Payment, UI, security, subscription, event, etc.
Requirement| Specific expected behavior
Role| Applicable role(s)
Navigation| Sidebar/dashboard entry
Frontend| Actual route/component
Backend| Actual API/service
Database| Actual entity/migration
Permission| Authorization and scope
Tests| Relevant verification
Status| Required status classification
Evidence| Actual result or code location
Gap| Missing or broken behavior
Priority| Critical/High/Medium/Low
Next action| Concrete step

Every material requirement must be traceable.

Do not mark requirements DONE without evidence.

---

PART XXI — FINAL ACCEPTANCE CRITERIA

83. Functional acceptance

Verify applicable:

- Purchase creation.
- Payment initiation.
- Gateway verification.
- Transaction history.
- Transaction details.
- Receipts.
- Subscription activation.
- Entitlement fulfillment.
- Renewal and expiry.
- Individual purchases.
- Bundles.
- Event bookings.
- Group payments.
- Seat integrity.
- Parent/guardian scope.
- Provider/teacher/organizer financial views.
- Pricing governance.
- Refund workflow where approved.
- Reconciliation.
- Notifications.

84. Security acceptance

Verify:

- Backend authorization.
- Resource ownership.
- Provider/institution scope.
- Parent-child relationships.
- IDOR protection.
- Callback authentication.
- Amount and currency validation.
- Idempotency.
- Safe receipt downloads.
- Export authorization.
- Refund and payout authorization.
- Audit logging.
- Secret protection.

85. Quality acceptance

Verify:

- Backend tests.
- Frontend type checks.
- Frontend build.
- Relevant integration tests.
- Security tests.
- Migration tests.
- Responsive UI.
- Accessibility.
- Failure recovery.
- Documentation consistency.

86. Production-readiness acceptance

Production readiness must be reported separately from code completion.

Do not claim production readiness unless the relevant requirements are actually verified, including:

- Gateway onboarding.
- Approved pricing policy.
- Approved refund policy.
- Required production configuration.
- Secret management.
- Reconciliation readiness.
- Monitoring.
- Operational recovery.
- Security verification.
- Required financial approvals.

---

PART XXII — REQUIRED FINAL REPORT

87. Final report format

Provide:

A. Repository state

- Branch.
- Starting and final commit where applicable.
- Changed files.
- Preserved user changes.
- Database/migration baseline.

B. Audit findings

- Existing functionality.
- Missing features.
- Broken behavior.
- Security risks.
- Migration conflicts.
- Blockers.

C. Implementation

- Actual files changed.
- Backend changes.
- Frontend changes.
- Database changes.
- API changes.
- Documentation changes.

D. Role coverage

- Applicable roles.
- Accessible records.
- Permitted actions.
- Navigation.
- Test evidence.

E. Financial integrity

- Payment lifecycle.
- Idempotency.
- Subscriptions.
- Entitlements.
- Event bookings.
- Receipts.
- Refunds.
- Earnings.
- Reconciliation.

F. Security

- Authorization.
- IDOR tests.
- Callback verification.
- Cross-tenant isolation.
- Remaining risks.

G. Tests

- Actual commands.
- Exit codes.
- Test counts.
- Failures.
- Skipped tests.
- Unavailable test infrastructure.

H. Gateway readiness

- Mock status.
- Sandbox status.
- Production configuration.
- Production approval.
- Live collection.
- Refund readiness.
- Payout readiness.

I. Remaining work

- Requirement.
- Severity.
- Evidence.
- User decision required.
- Safe next action.

J. Final classification

- DONE.
- PARTIAL.
- MISSING.
- BROKEN.
- NEEDS VERIFICATION.
- N/A.

Do not claim 100% completion if required behavior remains unverified.

Do not conceal a critical security or financial defect behind a completion percentage.

---

PART XXIII — EXECUTION INSTRUCTIONS

88. Start correctly

Begin with Phase 0 and Phase 1.

1. Inspect the existing "Payment.md".
2. Inspect the actual repository state.
3. Preserve useful existing requirements.
4. Replace the old contents with the complete master specification.
5. Verify that all sections are present.
6. Audit actual code, database history and tests.
7. Build the requirement matrix.
8. Prioritize critical blockers.
9. Implement only after the relevant contracts and risks are understood.

Do not jump straight into large-scale implementation.

89. Work incrementally

For each implementation task:

1. State the requirement.
2. Identify existing files and contracts.
3. State the current defect or gap.
4. Define the smallest safe change.
5. Implement the change.
6. Add or update relevant tests.
7. Run the targeted tests.
8. Run relevant regression tests.
9. Review security and data-integrity implications.
10. Record evidence.
11. Update the requirement matrix.
12. Proceed to the next task.

Do not bundle unrelated changes into one opaque patch.

90. Completion rule

The goal is not merely to create a large "Payment.md" or generate code.

The goal is to complete ELMKUSOMA Payments & Transactions through a controlled, verifiable implementation.

Preserve the existing system.

Avoid duplicate architecture.

Protect real financial data.

Do not activate unsupported real-money operations.

Do not invent policy.

Do not fabricate tests or results.

Do not split the work between developers.

Use this document as the single master specification and continue until every safely implementable requirement is verified or every remaining blocker is explicitly documented.

END OF MASTER IMPLEMENTATION PROMPT
