# Phill — Agent Instructions

## Authority Hierarchy
Three reference documents govern all decisions. When in doubt, this is the order of authority:
1. .agent/anchor.md — The Prime Directive. Ideology, scope, philosophy. Cannot be overridden.
2. .agent/walkthrough.md — Codebase reference. What exists, how it's built.
3. .agent/mockup.jsx — UI design spec. What screens look like and how they flow.

If anything in this file conflicts with anchor.md, anchor.md wins.

---

## What This App Is
Phil is a digital prefrontal cortex for Phillips Mobile Automotive — a solo-operated
mobile auto repair business in Upstate South Carolina, owned and operated by Devon Phillips.

Phil is the administrative surrogate. Devon wrenches. Phil handles everything else:
customers, vehicles, scheduling, jobs, invoicing, payments, expenses, mileage, SMS.

---

## The Design Triad — Every Feature Must Satisfy All Three
1. Customer-Centric — the customer is the root entity. Everything traces back to a customer.
2. Data-Focused — every field has both a write path AND a read path. No dead tables. Ever.
3. Relation-Minded — context is always one tap away. No cross-referencing between silos.

---

## Non-Negotiable Rules (HITL — Human in the Loop)
These are law. No exceptions. No edge cases.

1. Phil NEVER sends a text, email, or notification without the operator tapping Send.
2. Phil NEVER commits data without an explicit Save / Confirm / Schedule tap.
3. Phil NEVER creates, modifies, or voids a financial record autonomously.
4. Every Phil-populated field must be editable before finalization.
5. A complete manual input path must always exist — no action is automation-only.
6. Draft data (Phil-populated) must be visually distinguishable from operator-entered data.

---

## Fiduciary Rule — Money Storage
- All currency = Long cents. $125.00 → 12500L. NEVER Double or Float for money.
- All quantities = Long thousandths. 1.5 hours → 1500L.
- All rates = Int basis points. 6% → 600. 15% markup → 1500.
- SC tax = 6% on PARTS ONLY. Labor and misc are never taxed.
- The one exception: mileage miles stored as Double (acceptable — not currency).

---

## Tech Stack
- Language: Kotlin 2.1.20
- UI: Jetpack Compose + Material 3 (BOM 2025.05.01)
- Navigation: androidx.navigation3 0.1.0-alpha04 (alpha — handle with care)
- Database: Room 2.7.1
- DI: Hilt 2.56.2
- Serialization: kotlinx.serialization 1.8.1
- Min SDK 26 (Android 8.0) / Target SDK 36
- Target device: Samsung Galaxy A16 (4GB RAM)
- Package: com.phillips.phill

---

## Hardware Constraints (Dev Machine)
Devon's dev machine is a low-spec HP laptop (Intel N200, 4GB RAM, ~500MB free at idle).
- JVM heap MUST stay ≤ 2GB (-Xmx2g). Never suggest raising this.
- No hardware-accelerated emulator (Intel UHD integrated GPU only).
- Storage is limited (~17GB free). Keep builds lean.
- Do not suggest dependencies that significantly increase build size or compile time.

---

## Architecture Pattern
Every feature follows this exact chain — no shortcuts:
Entity → DAO → Repository → ViewModel → Screen

- Entities: data/entity/ — Room @Entity, UUID text PKs (except ShopProfile: Int = 1)
- DAOs: data/dao/ — @Upsert for writes, Flow<List<T>> for lists, suspend for singles
- Repositories: data/repository/ — @Singleton, one domain per repo, IO dispatcher
- ViewModels: ui/<feature>/
- Screens: ui/<feature>/

---

## Repository Domains (Do Not Cross These Lines)
- CustomerRepository — customers + vehicles only
- ScheduleRepository — appointments only
- JobRepository — jobs + clock entries + mileage
- BillingRepository — invoices + line items + payments
- CommsRepository — conversations + messages
- OperationsRepository — shop profile + expenses + mileage summaries

---

## v1 Scope — Locked
In scope: Full CRUD for all entities, SMS comms, scheduling, job queue,
time clock, mileage logging, invoice builder, payment logging, expense tracking,
shop settings, dashboard, analytics overview.

EXPLICITLY OUT OF SCOPE for v1 (do not build, do not suggest):
- Any AI/ML integration
- Automated SMS parsing
- GPS-triggered anything
- PDF generation
- Map/routing API (address display only)
- Cloud sync or multi-device
- Parts inventory
- Customer-facing screens

---

## The Living Mockup — Read This Carefully

.agent/mockup.jsx is a living reference document. It is not a snapshot. It is not final.
It is the first iteration of an evolving visual record that will be updated continuously
as the app grows, screens change, and new features are designed.

Its purpose is twofold:
1. Shared vision — so Devon and the agent always picture the same thing.
2. Design record — a running history of what the app looked like at each stage
   of development, captured in code rather than screenshots.

### Rules for working with the mockup:

- The current mockup.jsx reflects the intended UI at the time it was last updated.
  Treat it as the authoritative picture of NOW, not a picture of the finished product.

- When Devon updates the mockup, the new version replaces the old one in .agent/mockup.jsx.
  The agent should always read the current file — never assume a previous version is still accurate.

- When a new screen or flow is added to the real app, Devon will update the mockup to match.
  If the mockup shows a screen that doesn't exist yet in Kotlin, that screen is PLANNED — not built.

- The agent should never modify mockup.jsx directly unless Devon explicitly asks for it.
  Mockup updates are Devon's decision — they reflect his evolving vision, not implementation reality.

- If the mockup and the real Kotlin code disagree, ask Devon which is correct before proceeding.
  The mockup may be ahead of the code (planned) or behind it (not yet updated). Never assume.

Mockup color → Material 3 token mapping:
- slate-900 → onSurface / onBackground
- blue-600 → primary
- emerald-600 → tertiary
- slate-100/200 → surfaceVariant
- red-600 → error

---

## Screen Build Status
- Dashboard ✅
- Schedule ✅
- Jobs ✅
- Comms ✅
- Customers ✅
- Billing ✅
- Settings ✅
- Analytics 🚧

---

## Cortana Lessons — Do Not Repeat These Mistakes
- No God Objects. No repository with more than ~3 DAO dependencies.
- No hardcoded business values ($125/hr, tax rates, addresses). Everything in ShopProfile.
- No stringly-typed state. All status fields are Kotlin enums.
- No feature that writes data without also having a screen that reads it.
- No metrics rendered on-screen from empty or unverified data sources.
- No automation before the manual path is verified stable end-to-end.
- No schema change without a defined Room migration.

---

## Ask Me Before You Do Any of These
- Any database schema change (new table, new column, column rename, column drop)
- Any new Gradle dependency
- Any change to navigation structure or bottom bar
- Any change to BillingEngine math
- Any change to PhillDatabase version number
- Anything that touches more than one repository domain at once

---

*The customer is the center. The data is the value. The relationships are the product.
Nothing gets built that isn't anchored here. Nothing gets automated that doesn't work manually first.
Nothing gets stored that isn't read. Nothing gets displayed that isn't real.*
