ELMKUSOMA PAYMENT SYSTEM — MASTER AUDIT, IMPLEMENTATION, SECURITY, AND DELIVERY PROMPT

1. YOUR ROLE AND MISSION

Act as a Principal Software Architect, Senior Full-Stack Engineer, Senior Java/Spring Boot Security Engineer, Senior React/Next.js Engineer, Database Architect, Payment Systems Engineer, Financial Controls Specialist, QA Automation Engineer, and Technical Delivery Lead.

You are working on the EXISTING ELMKUSOMA production-oriented education and digital services ecosystem.

Your mission is to audit, improve, implement, test, secure, and integrate the ELMKUSOMA Payment System into the existing application at the highest practical engineering standard.

Target: deliver the maximum verified implementation quality, using approximately 98% of your available capability efficiently. Work decisively and systematically. Do not waste time on repetitive planning, unnecessary rewrites, cosmetic-only work, or endless reports. However, never sacrifice security, correctness, existing functionality, or verification for speed.

The required engineering process is:

UNDERSTAND → AUDIT → CLASSIFY → PLAN → DEFINE CONTRACTS → IMPLEMENT → INTEGRATE → TEST → VERIFY → HARDEN → DOCUMENT.

The expected outcome is a real, integrated, functional implementation—not merely a requirements document, architecture proposal, attractive mockup, or list of recommendations.

2. NON-NEGOTIABLE RULES

1. Inspect the actual repository before making assumptions.
2. Preserve the existing ELMKUSOMA architecture, naming conventions, design system, authentication, authorization, database structure, API conventions, and reusable components wherever appropriate.
3. Do not rebuild the entire application or replace working modules unnecessarily.
4. Do not create duplicate payment, purchase, subscription, receipt, notification, reporting, or entitlement modules before inspecting existing implementations.
5. Do not invent entities, APIs, routes, permissions, roles, financial data, service records, or successful test results.
6. Never present mock financial data as real production data.
7. Never grant paid access merely because the frontend displays a success message.
8. Never enable live payment collection, production gateway credentials, actual money transfers, or automatic payouts during this implementation phase.
9. Do not perform destructive database changes or apply migrations before the database and migration audit, migration plan, and required approval.
10. Do not introduce a hidden Super Admin role, secret account, backdoor, undocumented commission, concealed fee, secret financial diversion, or unauthorized transfer mechanism.
11. All commissions and fees must follow an approved, documented, transparent, auditable business policy.
12. Do not weaken authentication, authorization, validation, audit logging, or existing tests to make implementation easier.
13. Do not remove existing features to make new tests pass.
14. Do not claim that a feature works until you have verified it with appropriate evidence.
15. Do not expose secrets, credentials, personal data, or sensitive payment information in logs, reports, frontend code, source control, or generated documentation.
16. Do not stop after the audit. Continue into implementation of verified gaps, subject to the approval boundaries defined below.
17. Do not stop after creating a visually attractive frontend. Connect it to real, authorized backend functionality.
18. Do not mark incomplete integrations as complete.
19. Do not create unnecessary abstractions, speculative frameworks, duplicate APIs, or migrations without a demonstrated need.
20. Keep every existing unrelated feature working.

If a requirement already exists and works, reuse it. If it exists partially, improve it. If it is broken, diagnose and repair it. If it is missing, implement it where it falls within the authorized scope. If verification is impossible, document the precise blocker instead of guessing.

3. PROJECT DISCOVERY AND AUDIT

Start by examining the repository structure, Git status, current branch, project instructions, existing architecture, dependency versions, tests, configuration, and available development tools.

Inspect at minimum:

- Backend modules, controllers, services, repositories, entities, DTOs, validators, security configuration, exceptions, and API conventions.
- Frontend routes, layouts, dashboards, navigation, design tokens, forms, API clients, permissions, and reusable components.
- Authentication, user roles, permissions, provider membership, institution scope, ownership checks, and jurisdiction restrictions.
- Existing course, programme, module, live class, recorded lesson, event, booking, and service catalogue functionality.
- Existing payments, purchases, orders, subscriptions, access grants, pricing, commissions, earnings, and refunds.
- Existing receipts, invoices, PDF generation, downloads, transaction history, and associated permissions.
- Existing notification infrastructure, asynchronous processing, scheduled tasks, and audit logging.
- Database schema, relationships, indexes, constraints, migration history, and migration naming conventions.
- Existing admin analytics, reports, filters, exports, and dashboard APIs.
- Existing frontend theme, layouts, color palette, typography, spacing, buttons, tables, badges, charts, and responsive patterns.
- Existing unit, integration, end-to-end, authorization, security, and regression tests.
- Current environment configuration and payment-provider integration readiness.

Never print secret values while inspecting environment files. Record variable names and configuration presence only.

Classify every relevant feature as:

- DONE — implemented and verified.
- PARTIAL — implemented but incomplete.
- MISSING — no suitable implementation exists.
- BROKEN — implementation exists but fails.
- NEEDS VERIFICATION — insufficient evidence.

Produce a concise baseline report identifying verified behavior, affected files, risks, dependencies, failing tests, and actual implementation gaps.

After the initial audit and a safe implementation plan, continue implementing non-destructive, in-scope improvements. Do not treat the planning-only language in the supplied requirements document as a permanent prohibition on coding; this task explicitly authorizes controlled implementation. Destructive database changes, live payment activation, production credentials, actual money collection, and payouts remain gated.

4. TWO-DEVELOPER WORK DIVISION — STRICT FILE OWNERSHIP

Divide the work into two clearly separated workstreams. Every task, issue, progress entry, change report, test report, and outstanding item must explicitly identify either "[Developer 01]" or "[Developer 02]".

The work must cover all applicable user roles and personas, but each developer must work only within their assigned technical ownership.

[Developer 01] — BACKEND, DATABASE, SECURITY, AND FINANCIAL DOMAIN

Owns all backend and data-layer implementation, including:

- Payment-domain architecture and business rules.
- Existing payment and purchase flow improvements.
- Service pricing and pricing governance APIs.
- Orders, purchases, transaction references, and payment statuses.
- Subscription lifecycle and expiry logic.
- Entitlement and access-control enforcement.
- Bundles, packages, and service eligibility.
- Education and event payment-domain integration.
- Group bookings, seat reservations, and capacity protection.
- Receipt and invoice backend integration, reusing existing functionality.
- Refund requests, approvals, partial-refund records, and adjustments.
- Provider, teacher, trainer, and host earnings calculations.
- Approved revenue-sharing and commission policies.
- Historical pricing and revenue-policy version references.
- Financial reports, summaries, filters, and backend aggregation.
- Reconciliation data models and exception workflows.
- Notification-event integration with existing infrastructure.
- Authorization, ownership, provider scope, jurisdiction, and financial approval limits.
- Audit logs, idempotency, transaction consistency, validation, rate limiting, and security.
- Database changes and migrations, only after the audit, technical design, and required approval.
- Backend unit, integration, authorization, security, and regression tests.
- A documented API contract for Developer 02.

Developer 01 MUST NOT edit frontend source files, UI components, frontend layouts, stylesheets, frontend routes, or frontend-specific tests.

If a frontend requirement exposes a backend gap, Developer 02 must document the required contract change and submit it to Developer 01. Developer 01 implements the backend change within the backend ownership boundary.

[Developer 02] — FRONTEND, DASHBOARDS, UX, AND API INTEGRATION

Owns all frontend implementation, including:

- Payment-related navigation and page integration.
- Purchase and checkout interfaces.
- Pricing and service details.
- Subscription management screens.
- Transaction history and payment-status interfaces.
- Receipt and invoice views and downloads through existing APIs.
- Refund-request interfaces and status tracking.
- Learner/student payment dashboard.
- Parent/guardian payment dashboard.
- Teacher/trainer earnings dashboard.
- Host/event organizer dashboard.
- Provider dashboard.
- Provider Admin dashboard.
- Platform Admin Financial Command Center.
- Authorized finance/support views where supported by the existing role model.
- Pricing management and approval interfaces.
- Service monetization and publication-status interfaces.
- Event ticketing and group booking interfaces.
- Revenue and transaction reports.
- Date filters, search, pagination, loading states, empty states, error handling, and exports through available APIs.
- Responsive layouts, mobile usability, accessibility, low-bandwidth behavior, and frontend tests.
- Integration with the existing backend API contract.
- Preservation of existing navigation, dashboard layout, theme, and visual conventions.

Developer 02 MUST NOT edit backend source files, backend tests, database entities, database migrations, repository interfaces, backend security configuration, or server-side business logic.

Developer 02 must never calculate authoritative earnings, commissions, payment success, refund eligibility, or access authorization solely in the frontend.

Integration and Coordination Rules

Before parallel work begins:

1. Record the initial Git status and existing changes.
2. Map file ownership and likely shared dependencies.
3. Identify existing API conventions.
4. Define the backend API contract, DTOs, status enums, pagination, error formats, and authorization expectations.
5. Record dependencies between the two workstreams.
6. Identify shared configuration files that must not be concurrently edited.

Developer 01 publishes the agreed backend contract. Developer 02 consumes it.

If an API change becomes necessary, communicate the requirement and update the contract before integration. Do not independently modify the other developer's files.

If OpenCode supports genuine independent agents or worktrees, use separate isolated workspaces with explicit file ownership and controlled integration. If it does not, execute the workstreams sequentially while enforcing the same boundaries. Never pretend that two independent developers or agents ran when they did not.

The integration lead must review the combined changes, resolve contract mismatches, run regression tests, and produce one consolidated delivery report.

5. EXISTING ELMKUSOMA ARCHITECTURE AND AUTHORIZATION

Respect the existing production-oriented modular monolith and technology stack.

Discover the actual versions and conventions in the repository before selecting implementation details. The known stack includes Spring Boot, Spring Security, JPA, Flyway, PostgreSQL, Redis, RabbitMQ, LiveKit, Next.js, React, TypeScript, and Tailwind, but verify actual project configuration.

Do not replace the existing architecture with a new framework or separate application unless a verified architectural requirement justifies it.

Authorization must respect the existing model, including:

ROLE + PERMISSION + ORGANIZATION/JURISDICTION SCOPE + RESOURCE OWNERSHIP + RESOURCE STATE + APPROVAL LIMITS.

A role alone is not sufficient.

Preserve existing roles and their semantics. The Platform Admin remains the highest-level platform governance authority. Provider Admin remains restricted to the relevant provider. Do not introduce a separate permanent Super Admin role or hidden financial authority.

Test unauthorized access by manipulating resource identifiers, query parameters, request bodies, and API paths.

6. BUSINESS SCOPE OF THE PAYMENT SYSTEM

Implement and integrate the following requirements where they are missing and authorized.

Service catalogue

Support existing or approved service types, including:

- Learning resources, notes, and study materials.
- Courses, programmes, modules, and learning packages.
- Live classes, training sessions, and live-class series.
- Recorded lessons and replay access.
- Approved certificate-related services.
- Seminars, workshops, conferences, and educational events.
- Religious, community, NGO/CBO, and corporate events.
- Provider services and approved premium platform tools.
- Other services approved by the Platform Admin.

Do not invent purchasable services. Use genuine catalogue records and existing service ownership.

Each monetized service must have the appropriate fields, whether already implemented or added after review:

- Unique identifier and name.
- Type and description.
- Owner/provider.
- Pricing model, amount, and currency.
- Duration and access rules.
- Publication and approval status.
- Refund policy.
- Revenue-sharing policy.
- Eligibility and availability.
- Included and excluded package features.

Pricing models

Support applicable models:

- Free access.
- One-time purchase.
- Daily access.
- Weekly access.
- Monthly subscription.
- Approved custom durations.
- Individual purchases.
- Bundles and tiered packages.
- Subscriptions providing defined service access.
- Individual event tickets.
- Group event bookings.

Daily access requires an explicit business decision between a rolling 24-hour duration and a calendar day. Do not silently choose a business policy when the decision materially affects customers.

A monthly subscription lasts exactly 30 days from activation, as specified in the requirements, not automatically until the end of a calendar month.

Preserve the historical price, currency, terms, and policy version applicable to each purchase.

Providers may set prices only within approved Platform Admin policies, limits, eligibility requirements, and publication rules.

Discounts require defined permissions, limits, eligibility, validity periods, usage rules, and audit history.

Do not make every education service paid. Free services must continue working without fabricated payment transactions.

7. PAYMENT LIFECYCLE AND TRANSACTION INTEGRITY

Audit the current payment lifecycle and improve the actual gaps.

The target lifecycle is:

1. User selects a genuine service.
2. Backend verifies service availability, publication status, eligibility, and ownership.
3. Backend resolves the authoritative price, currency, duration, applicable fees, and policy versions.
4. Backend creates or reuses an appropriate purchase/order.
5. Payment attempt creation remains safely separated from confirmed payment.
6. Payment stays pending until trusted verification is complete.
7. The backend records the verified outcome.
8. Appropriate access entitlements are granted exactly once.
9. Existing receipt functionality is reused or improved.
10. Relevant notifications are attempted.
11. Transaction information becomes available for authorized reporting and reconciliation.
12. Revenue allocations are recorded according to the applicable approved policy.

Inspect the current transaction-state model before adding or renaming statuses. It may require states such as created, pending, processing, successful, failed, cancelled, expired, refund pending, partially refunded, refunded, or disputed.

Do not blindly introduce incompatible states if the existing implementation already has a suitable model.

Implement or verify:

- Server-side price validation.
- Unique transaction references.
- Idempotency controls.
- Safe repeated requests.
- Safe retry behavior.
- Transactional consistency.
- Duplicate callback protection.
- Late callback handling.
- Mismatched reference detection.
- Correct pending/failed/successful states.
- Recovery from interrupted browser sessions.
- Recovery when notifications or receipt generation fail.
- Controlled handling of successful payments with missing entitlements.
- Detection of possible duplicate payments.

A notification failure must not turn a confirmed payment into a failed payment. A receipt-generation failure must not silently erase a successful transaction.

Never create duplicate entitlements, receipts, or financial allocations when a request or callback is repeated.

8. PAYMENT GATEWAY BOUNDARIES

For this phase, do not connect a live gateway, collect real money, configure production credentials, or execute payouts.

Audit gateway-related code and establish a safe integration boundary for future mobile money, bank, and card providers.

If gateway interfaces or configuration already exist, reuse them where appropriate. Otherwise, implement only the safe, necessary abstraction and readiness documentation justified by the architecture.

Do not fabricate a sandbox provider, pretend that API credentials exist, or report a successful gateway test without actually performing one.

Gateway selection remains an open business decision. Before any sandbox connection, document the candidate provider, Tanzania support, merchant onboarding, supported payment methods, fees, refunds, callback security, settlement rules, documentation, and compliance requirements.

Once a provider is selected, prepare a separate sandbox integration stage. Obtain and configure sandbox credentials only through an approved secure method, using environment variables or the approved secrets manager. Never ask the user to paste secrets into source code or public chat.

Do not begin credential-dependent testing until the sandbox provider is connected and the required credentials are securely configured. Once sandbox linking is available and authorized, perform real sandbox tests, verify callbacks and transaction reconciliation, and report actual outcomes.

Live activation and production credentials require explicit approval for a separate release phase. Sandbox success does not authorize live payments.

Payouts are also a separate future phase. Prepare appropriate data and reporting only where justified. Do not transfer funds or represent prepared balances as already paid.

9. SUBSCRIPTIONS, PURCHASES, AND ACCESS ENTITLEMENTS

Audit and implement missing functionality for:

- Individual purchases.
- Bundles and packages.
- Tiered access.
- Subscriptions.
- Subscription expiry.
- Renewal rules.
- Cancellation.
- Access restrictions.
- Revocation.
- Historical purchase records.
- Entitlement history.

A payment record and an access entitlement are distinct concepts.

Each entitlement should correctly reference the authorized recipient, service, purchase/subscription source, activation time, expiry where applicable, current status, and revocation reason.

Paid entitlements must be granted only after verified payment and the relevant business conditions are satisfied.

Free access must follow the existing enrollment policy without fake payment records.

Support parent-paid purchases where the parent-child relationship and authority are verified. Distinguish the payer, account owner, service recipient, and learner receiving access.

Expired subscriptions must follow the service's access policy. Cancellation and refunds must remain separate operations.

Automatic renewal must remain disabled unless a later authorized phase establishes provider support, user consent, cancellation controls, failure handling, notifications, and compliance readiness.

For courses, modules, and programmes, respect prerequisites and existing education rules.

For certificates, payment must never replace academic completion, assessment, or authorized issuance requirements.

For recorded classes, enforce backend access control even when someone knows or shares the recording URL.

10. EVENTS, TICKETS, AND GROUP BOOKINGS

Support the existing event architecture and improve missing capabilities.

Individual bookings

Track the relevant event, attendee, ticket type, amount, payment status, registration status, access authorization, cancellation, and attendance.

Organizer-paid group bookings

Support an authorized organizer purchasing multiple seats.

Track the organizer/payer, booking reference, seat quantity, payment status, assigned participants, confirmed participants, unused seats, transfer policy, and refund policy.

Individually paid group registrations

Support a group registration process in which every attendee has an independently tracked payment and registration status.

Capacity and attendance

Prevent event overselling. If pending payments reserve seats, implement a defined reservation-expiry and release policy.

Keep payment, registration, seat assignment, and attendance as separate states.

Event cancellation must follow the applicable refund policy and preserve the reason, affected attendees, decisions, and audit history.

11. REVENUE SHARING, COMMISSIONS, AND EARNINGS

Implement backend-authoritative calculations only where the approved policy is sufficiently defined.

Revenue-sharing policies may vary by service, provider, teacher, trainer, host, or approved agreement.

For each applicable transaction, preserve the necessary financial breakdown:

- Original gross amount.
- Discount.
- Refunds and adjustments.
- Processing fees.
- Approved platform commission.
- Other explicitly approved fees.
- Provider/teacher/host share.
- Net attributable amount.
- Applicable policy/version reference.
- Allocation status and audit history.

Do not silently assume who bears payment processing fees. Record this as an unresolved policy where the business has not decided.

Optional provider/teacher platform fees must not be activated without an approved billing rule.

Never implement undisclosed commissions, hidden fees, secret percentage deductions, or concealed transfers. Financial terms shown to customers and service providers must accurately reflect the approved policies and relevant contractual terms.

Historical allocations must not be silently recalculated when a new policy is introduced.

Corrections must use authorized adjustment records with reasons and audit history rather than silently overwriting or deleting financial records.

Distinguish gross sales, confirmed collections, refunds, net collections, platform revenue, provider shares, pending balances, liabilities, expenses, and profit/loss.

Do not calculate official profit from gross sales minus provider shares alone. If required accounting data or an approved methodology is missing, display the metric as unavailable or explicitly identified as an operational estimate—not as an authoritative financial figure.

12. RECEIPTS, INVOICES, AND REFUNDS

Receipts and invoices

Before changing anything, inspect the existing receipt implementation, including entities, APIs, frontend pages, transaction links, PDF/download support, permissions, and tests.

Reuse and improve it wherever appropriate. Do not create a duplicate receipt module.

A receipt must be linked to the correct genuine transaction and contain the appropriate available information:

- Unique receipt number.
- Transaction reference.
- Payer and service details.
- Amount and currency.
- Transaction date.
- Applicable payment channel.
- Accurate transaction status.
- Provider information where required.
- Refund reference where applicable.

An invoice represents an amount requested or due. A receipt documents a payment received or confirmed under the applicable policy.

Do not silently alter historical receipts after a refund. Preserve the original transaction and create appropriate refund documentation, credit notes, or adjustment records according to the existing accounting/documentation design.

Refunds

Audit and improve the complete refund lifecycle:

1. Request.
2. Eligibility check.
3. Evidence review where required.
4. Approval or rejection.
5. Refund status tracking.
6. Financial adjustment.
7. Entitlement adjustment according to policy.
8. Refund documentation.
9. Notification.
10. Audit history.

Support service-specific refund policies, approval limits, partial refunds where required, duplicate-refund protection, and future dispute/chargeback readiness.

A refund request is not a completed refund. Because no live payment gateway is enabled, do not claim that funds have actually been returned. Clearly distinguish refund requested, approved, processing, completed, rejected, and other appropriate verified states.

13. USER DASHBOARDS — ALL RELEVANT PERSONAS

Every dashboard must be integrated with existing ELMKUSOMA layouts, navigation, role permissions, and design conventions.

Do not force every user into the same dashboard or display financial controls to unauthorized roles.

Learner/Student — [Developer 02]

Implement or improve the relevant views:

- My Purchases.
- My Subscriptions.
- Payment History.
- Receipts.
- Pending Payments.
- Expiring Access.
- Refund Requests.
- Available Premium Services.

Show genuine purchases, clear status labels, service access details, expiry, and relevant actions.

Parent/Guardian — [Developer 02]

Show authorized children's purchases, subscriptions, receipts, and access status. Provide child selection where supported by the existing design.

Enforce the parent-child relationship in the backend; never rely on the selected child ID alone.

Teacher/Trainer/Instructor — [Developer 02]

Show authorized paid services, sales, transaction history, applicable fees, earnings, pending balances, refunds, and reports.

Host/Event Organizer — [Developer 02]

Show authorized events, tickets, registrations, paid attendees, group bookings, seat allocation, event sales, cancellations, refunds, and earnings reports.

Provider — [Developer 02]

Show authorized services, pricing, publication/approval state, sales, subscription performance, gross sales, refunds, platform fees, net attributable earnings, pending balances, and reports.

Provider Admin — [Developer 02]

Show financial and service-management information only for the provider organization they are authorized to administer.

Do not give Provider Admin automatic platform-wide financial access.

Finance/Support and Other Authorized Staff — [Developer 02]

Use existing roles and explicit permissions. Provide only the views and operations justified by the actual responsibilities. Mask sensitive information where appropriate.

Platform Admin Financial Command Center — [Developer 02]

Build a dedicated platform-wide financial operations area. This is not a learner dashboard.

Include the relevant verified metrics:

- Confirmed collections.
- Pending collections.
- Successful and failed transactions.
- Gross sales.
- Refunds.
- Net collections.
- Platform revenue.
- Provider, teacher, and host shares.
- Processing fees.
- Pending provider balances/liabilities where appropriately defined.
- Reconciliation exceptions.
- Recent transactions and exceptions.
- Service/provider performance.
- Financial reporting and authorized exports.

Provide useful filters:

- Today and yesterday.
- This week and previous week.
- This month and previous month.
- Custom date range.
- Service and category.
- Provider.
- Teacher or host.
- Subscription plan and bundle.
- Event.
- Payment method.
- Transaction status.

Support authorized search by transaction reference, receipt number, user reference, provider, service, date range, and status.

Use backend pagination, filtering, and aggregation appropriate to the existing API. Avoid loading the entire transaction history into the browser.

All dashboard totals must be derived from genuine backend data and clearly defined calculations. Do not invent figures to make the dashboard look populated.

Where there is no genuine data, show a clear empty state such as “No transactions found for this period.” Distinguish zero from unavailable or not-yet-implemented data.

14. EXISTING DESIGN SYSTEM AND PROFESSIONAL UX

[Developer 02] Frontend design responsibility

First inspect the existing ELMKUSOMA theme, layouts, dashboards, color tokens, typography, component library, navigation, responsive rules, and visual hierarchy.

Reuse the existing approved ELMKUSOMA design system. The known brand direction is a premium light African EdTech experience using blue, teal, orange accents, white, light backgrounds, and dark readable text. Known reference colors include:

- Blue: "#2563EB"
- Teal: "#0D9488"
- Orange: "#F59E0B"
- Light background: "#F8FAFC"
- Main text: "#1E293B"

Treat these as references, not permission to overwrite a different verified existing theme.

The interface must be:

- Professional, clean, premium, and production-oriented.
- Consistent with the current application.
- Easy to understand for Tanzanian users.
- Responsive across desktop, tablet, and mobile.
- Lightweight for low-bandwidth connections.
- Accessible through keyboard navigation, labels, clear contrast, and meaningful status indicators.
- Consistent in spacing, typography, cards, tables, forms, filters, and page headers.

Avoid:

- A completely unrelated dashboard template.
- Excessive gradients, shadows, oversized cards, and decorative charts.
- Fake AI visuals or arbitrary animations.
- Random icon styles.
- Decorative financial metrics without reliable data.
- Hard-coded demo transactions in production interfaces.
- Buttons that look functional but do nothing.
- Empty chart areas with misleading values.

Use genuine API data, clear loading states, useful empty states, actionable error messages, and safe retry behavior.

Show prices, currency, duration, inclusions, exclusions, applicable fees, refund terms, and renewal rules before purchase.

Never show pending transactions as successful. Preserve transaction references when a connection fails and allow users to check status without creating a duplicate purchase.

15. PLATFORM ADMIN PRICING AND GOVERNANCE

Backend — [Developer 01]

Implement or improve authorized controls for:

- Pricing policies and limits.
- Allowed pricing models.
- Service categories requiring approval.
- Commission and fee policies.
- Revenue-sharing policy versions.
- Refund policy templates.
- Discount rules.
- Service monetization eligibility.
- Financial approval limits.
- Auditable financial corrections.
- Relevant policy history.

Frontend — [Developer 02]

Provide clear, permission-controlled screens for managing and reviewing these settings.

Providers may set prices only within applicable rules. The Platform Admin can govern platform-wide pricing policies and financial rules without a hidden role or backdoor.

Preserve existing approval workflows. Do not invent a final commission percentage, refund time limit, tax rule, processing-fee allocation, or provider settlement rule when business approval is missing.

Where a business decision is unresolved, implement safe configuration/readiness support only if justified, document the decision, and prevent unauthorized activation.

16. NOTIFICATIONS AND OPERATIONAL RECOVERY

Integrate with the existing notification system.

Support applicable notifications for:

- Purchase initiated.
- Payment pending, successful, failed, or cancelled.
- Subscription activation and expiry reminders.
- Receipt availability.
- Refund request, decision, and verified completion.
- Booking confirmation and group-seat allocation.
- Authorized sales and earnings updates.
- Reconciliation exceptions requiring attention.

Use existing channels such as in-app notifications, email, or SMS only where configured and supported.

Notification retries must not duplicate financial effects. A notification failure must not change the payment outcome.

Define and test recovery paths for browser closure, lost internet, delayed responses, repeated callbacks, receipt failure, missing entitlement, unresolved refunds, duplicate payments, changed services, and event cancellation.

Support personnel must use controlled, authorized workflows instead of uncontrolled direct database edits.

17. FINANCIAL REPORTING AND RECONCILIATION

Backend — [Developer 01]

Build or improve authorized reporting and reconciliation capabilities using genuine transaction records.

Reconciliation should be ready to identify, where source data is available:

- External transactions missing internally.
- Unresolved pending transactions.
- Amount mismatches.
- Duplicate external references.
- Confirmed payments with missing entitlements.
- Refund mismatches.
- Fee mismatches.
- Inconsistent transaction statuses.

Record each exception's reference, type, amount where applicable, discovery date, status, assigned owner where necessary, resolution notes, and audit history.

Do not silently modify financial records to make reports balance.

Frontend — [Developer 02]

Provide usable reports, filters, exception details, transaction search, and authorized exports.

Implement CSV, Excel, or PDF exports only through existing supported capabilities or a justified, tested extension. Protect export endpoints and record sensitive financial operations where required.

Use Tanzania's timezone ("Africa/Dar_es_Salaam") for relevant business date boundaries. Verify how the existing system stores timestamps and converts timezones before making changes.

18. SECURITY REQUIREMENTS

[Developer 01] Backend enforcement

Verify and test:

- Role and permission enforcement.
- Provider/institution/jurisdiction scope.
- Resource ownership and IDOR protections.
- Service publication and approval requirements.
- Server-side pricing and fee calculations.
- Purchase eligibility.
- Subscription and entitlement authorization.
- Refund permissions and approval limits.
- Financial report authorization.
- Parent-child access restrictions.
- Transactional consistency and idempotency.
- Duplicate payment/refund prevention.
- Input validation and safe error handling.
- Rate limiting where supported by the architecture.
- Audit history for sensitive financial operations.
- Secure handling of gateway secrets.
- Protection against unauthorized financial adjustments.
- No card CVV storage or unnecessary sensitive payment data.
- No secret values in logs or responses.

Test maliciously modified IDs, prices, amounts, status values, provider IDs, recipient IDs, and refund requests.

Never trust client-supplied values for authoritative prices, commissions, earnings, payment outcomes, or permissions.

[Developer 02] Frontend security and UX

Hide or disable unauthorized controls appropriately, but do not treat frontend visibility as the authorization boundary.

Handle 401, 403, validation errors, pending transactions, timeouts, expired sessions, and API failures correctly.

Do not store gateway secrets in the frontend. Do not expose unauthorized transaction details through client-side state, URLs, logs, or error messages.

19. DATABASE AND MIGRATION GOVERNANCE

[Developer 01]

Before changing the database:

1. Inspect existing entities, tables, relationships, constraints, indexes, and migration history.
2. Identify reusable structures and data duplication risks.
3. Document the current schema and the verified gap.
4. Propose the smallest necessary data-model change.
5. Check referential integrity, uniqueness, transaction consistency, and historical-data compatibility.
6. Review migration ordering and naming conventions.
7. Define rollback or recovery procedures where feasible.
8. Obtain the required approval before applying destructive or materially risky migrations.

Do not recreate existing tables or rename existing fields casually.

Never delete transaction history to simplify implementation.

Use precise decimal types and safe currency handling appropriate to the existing backend. Do not use floating-point arithmetic for authoritative financial calculations.

Store original currency and amount accurately. Do not add foreign-exchange calculations without an approved requirement.

If no schema change is necessary, do not create one simply to demonstrate work.

20. TESTING AND QUALITY GATES

Both developers must write and run tests within their own ownership boundaries.

[Developer 01] Backend tests

Test relevant behavior for:

- Pricing policies and price changes.
- Free and paid service handling.
- Subscription activation and exact 30-day monthly expiry.
- Entitlement creation, expiration, and revocation.
- Service eligibility and approval.
- Group seat capacity and reservation expiry.
- Transaction state transitions.
- Duplicate requests and idempotency.
- Trusted payment-verification boundaries.
- Refund permissions, partial refunds, and duplicate-refund prevention.
- Revenue allocation and policy versioning.
- Receipt reuse and transaction linking.
- Parent-child authorization.
- Provider ownership and financial data isolation.
- Platform Admin financial permissions.
- IDOR and privilege escalation.
- Reconciliation and audit records.
- Notification failure handling.
- Migration and backward compatibility where applicable.

[Developer 02] Frontend tests

Test relevant behavior for:

- Role-specific dashboard visibility.
- API loading, empty, success, and error states.
- Pricing and purchase flows.
- Clear pending/success/failed/refunded statuses.
- Subscription expiry and cancellation interfaces.
- Transaction history and receipt access.
- Refund request workflows.
- Parent/child switching and data visibility.
- Provider and teacher earnings displays.
- Platform Admin filters and transaction search.
- Mobile layout and accessibility.
- Safe retry behavior without duplicate purchases.
- Unauthorized actions and API error handling.

Integration and regression testing

The integration lead must verify the complete user journey across both workstreams.

Use the project's existing test tools and conventions. Inspect package scripts and build configuration before choosing commands.

Run the relevant:

- Unit tests.
- Backend compilation and tests.
- Frontend type checking and linting where configured.
- Frontend production build.
- Integration tests.
- Security/authorization tests.
- Existing regression tests.
- End-to-end tests where available.

Compare results against the baseline. Distinguish pre-existing failures from regressions introduced by these changes.

Never weaken or delete tests simply to obtain a passing build.

If a test cannot run due to missing infrastructure, credentials, unavailable services, or environment limitations, report the exact reason and mark it unverified.

Do not claim that an entire suite passed when only a subset ran.

21. IMPLEMENTATION EXECUTION PLAN

Work through the following phases efficiently.

Phase A — Audit and baseline

Owner: Integration Lead, with findings assigned to [Developer 01] and [Developer 02].

Inspect the repository and identify the verified implementation gaps. Preserve unrelated working changes. Do not overwrite user modifications.

Deliver a concise baseline report and file-ownership map.

Phase B — Contract and technical plan

[Developer 01]: Define backend/domain requirements, API contracts, security rules, data-model assessment, and required migrations.

[Developer 02]: Map the existing UI, design system, routes, personas, frontend dependencies, and screens requiring integration.

Identify unresolved business decisions and dependencies before implementing dependent functionality.

Phase C — Backend implementation

Owner: [Developer 01].

Implement the highest-priority verified gaps in a controlled sequence. Add appropriate tests with each change.

Keep payment collection and payout execution disabled.

Phase D — Frontend implementation

Owner: [Developer 02].

Implement the corresponding interfaces using existing layouts, colors, components, and agreed API contracts.

Do not substitute hard-coded demo data for missing backend endpoints.

Phase E — Integration and verification

Integration Lead, with backend work owned by [Developer 01] and frontend work owned by [Developer 02].

Resolve contract mismatches, verify all relevant personas, execute regression tests, inspect Git diffs, and correct introduced defects.

Phase F — Hardening and delivery

[Developer 01]: Report backend/security/data changes and verified backend results.

[Developer 02]: Report frontend/screens/API integration and verified UI results.

Integration Lead: Produce one consolidated report covering implementation, test evidence, risks, and outstanding work.

Do not stop after Phase A or B unless a genuine blocker prevents safe implementation. Do not claim all phases are complete unless the evidence supports that claim.

22. PRIORITY ORDER

Use this order unless the audit identifies a critical security or architectural dependency that requires another sequence:

1. Preserve existing functionality and establish a trustworthy baseline.
2. Fix critical authorization, ownership, payment-integrity, or financial-data defects.
3. Establish the backend contract and core transaction/purchase lifecycle.
4. Integrate existing receipt functionality and subscription/entitlement logic.
5. Implement missing pricing, refund, earnings, and reporting capabilities.
6. Build the role-specific dashboards and connect them to real APIs.
7. Implement events/group bookings and remaining applicable service types.
8. Complete notifications, reconciliation readiness, and failure recovery.
9. Run regression tests, accessibility checks, and security hardening.
10. Document sandbox readiness, open business decisions, and release gates.

Within each priority, prefer the smallest complete, testable change that preserves the existing architecture.

23. ACCEPTANCE CRITERIA

Treat the work as complete only when the relevant applicable criteria have evidence.

Pricing and services

- Platform Admin pricing governance respects permissions.
- Providers cannot exceed authorized limits.
- Free services continue to work.
- Historical purchase terms remain accurate.
- Prices, duration, inclusions, exclusions, and fees are clear before purchase.

Transactions and entitlements

- Frontend messages cannot independently confirm payment.
- Failed or pending payments do not improperly grant paid access.
- Repeated requests do not create duplicate financial effects.
- Purchases, transactions, and entitlements are linked correctly.
- Recorded content and other protected resources enforce backend authorization.

Subscriptions

- Approved durations work.
- Monthly subscriptions last 30 days from activation.
- Expiry and cancellation are handled correctly.
- Cancellation does not automatically imply a refund.
- Access follows the approved policy.

Events and group bookings

- Individual tickets and both group-payment models work where implemented.
- Capacity limits prevent overselling.
- Payment, registration, seat assignment, and attendance remain distinct.
- Cancellations follow the applicable policy.

Earnings and financial reports

- Calculations use genuine transactions and approved policies.
- Provider shares are distinguishable from platform revenue.
- Historical policy versions are preserved.
- No hidden fees or unauthorized deductions exist.
- Missing accounting information is not replaced with invented profit figures.

Receipts and refunds

- Existing receipt functionality is reused appropriately.
- Receipts are linked to the correct transactions.
- Ownership checks prevent unauthorized receipt access.
- Refunds preserve original history and require appropriate authorization.
- No live refund is claimed without actual verified processing.

Dashboards and security

- Each relevant persona sees only authorized information.
- The Platform Admin has a dedicated financial command center.
- Providers cannot see other providers' private financial records.
- Parent access respects verified relationships.
- Reports and exports enforce permissions.
- Dashboards use genuine backend data.
- The frontend uses the existing ELMKUSOMA design system.

Release governance

- Relevant tests pass, with results recorded.
- Existing features are not regressed.
- Changes and migrations are documented.
- Live gateways, production credentials, actual payment collection, and payouts remain disabled.
- Sandbox testing is reported honestly and is performed only after authorized provider linking and secure credential configuration.

24. REQUIRED FINAL REPORT

At the end, produce a concise but technically detailed report with the following sections.

A. Executive summary

What was actually audited, implemented, tested, and verified.

B. Initial audit findings

A table containing:

- Feature/component.
- Initial status: DONE / PARTIAL / MISSING / BROKEN / NEEDS VERIFICATION.
- Evidence.
- Risk or gap.
- Assigned owner: Developer 01 or Developer 02.

C. Implementation report

For every task:

- Task ID.
- Assigned developer.
- Requirement.
- Files changed.
- What changed and why.
- API/database impact.
- Security considerations.
- Verification performed.
- Status: DONE / PARTIAL / BLOCKED / NOT STARTED.

D. Dashboard and persona coverage

Report the verified implementation status of:

- Learner/student.
- Parent/guardian.
- Teacher/trainer.
- Host/event organizer.
- Provider.
- Provider Admin.
- Platform Admin.
- Authorized finance/support roles where applicable.

E. API and integration report

Document the APIs used or introduced, frontend integration, contract mismatches, and outstanding dependencies.

F. Database and migration report

List any migration files, schema changes, compatibility considerations, and whether changes were merely proposed or actually applied.

G. Testing evidence

For every executed command, report the actual command, result, pass/fail counts where available, and any failures. Separate pre-existing failures from newly introduced regressions when evidence allows.

H. Security and financial-integrity report

Report authorization, IDOR, idempotency, refund, entitlement, receipt, financial calculation, audit logging, and secret-handling verification.

I. Remaining work and blockers

List every incomplete or unverified requirement, its owner, priority, dependency, and next action.

J. Sandbox and production readiness

State explicitly:

- Whether a payment provider has been selected.
- Whether sandbox credentials have been configured.
- Which sandbox tests were actually run.
- Whether any live gateway is enabled.
- Whether real money was collected.
- Whether payouts were executed.

The expected current result is that live payment collection and payouts remain disabled.

K. Final status classification

Summarize remaining items as DONE, PARTIAL, MISSING, BROKEN, or NEEDS VERIFICATION.

Never report 100% completion unless all claimed requirements have actually been verified. Report real progress rather than a fabricated percentage.

25. FINAL EXECUTION INSTRUCTION

Start now by auditing the actual ELMKUSOMA repository and checking the current Git status. Establish the baseline, assign the work to Developer 01 and Developer 02, define the interface contract, and then implement the highest-priority verified gaps within the ownership boundaries.

Use maximum practical reasoning and available tooling. Work quickly but carefully. Do not repeatedly ask the user questions that the repository can answer. Resolve technical details independently where safe; document business decisions that genuinely require approval.

Do not silently cross developer ownership boundaries. Do not skip tests. Do not invent data. Do not fabricate progress. Do not stop at planning. Do not redesign unrelated parts of ELMKUSOMA.

Deliver the strongest practical, secure, integrated, maintainable implementation that the existing repository and available environment support.

The final deliverable must be working code with verification evidence, plus a clear consolidated report—not merely a prompt, mockup, or recommendation list.
