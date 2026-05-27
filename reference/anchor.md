# Project Phil — Anchoring Document
## Phillips Mobile Automotive Shop Management Platform
### Native Android Application

---

> **Status:** Finalized — May 25, 2026
> **Owner:** Devon Phillips — Owner/Operator, Phillips Mobile Automotive
> **Lineage:** Clean rebuild succeeding Project Cortana (`C:\Users\devon\Projects\Cortana`)
> **Authority:** This document is the single source of truth for all design, architecture, and implementation decisions. Any development that contradicts the principles defined here must be rejected.

---

## 1. Project Identity

| Field | Value |
|---|---|
| **App Codename** | Phil |
| **Business** | Phillips Mobile Automotive |
| **Website** | phillipsmobileauto.com |
| **Operator** | Devon Phillips — sole owner, sole technician, sole employee |
| **Service Area** | Upstate South Carolina |
| **Business Model** | Solo mobile auto repair — operator drives to the customer |
| **Platform** | Native Android |
| **Target Device** | Samsung Galaxy A16 (4GB RAM) |

**Lineage:** Phil is the canonical name established in the [Cortana anchor document](file:///C:/Users/devon/Projects/Cortana/Progression/anchor.md). All prior references to "Project Cortana" or "Project Stanley" are subsumed under the identity of Phil. This project is a **ground-up rebuild** — it inherits Cortana's ideology, domain knowledge, and lessons learned, but none of its code or technical debt.

---

## 2. The Prime Directive — Cognitive Offloading

Phil is a **digital prefrontal cortex** designed exclusively for a solo-operated, mobile-only enterprise.

Devon performs all mechanical repair work personally. While diagnosing a fault or turning a wrench, he cannot simultaneously answer customer texts, check his schedule, log mileage, or generate an invoice. Phil exists as the **administrative surrogate** — performing the roles that a brick-and-mortar shop would staff with 3–4 desk employees — so the operator can reserve 100% of mental bandwidth for the physical work that earns revenue.

**What Phil Is:**
- The operator's institutional memory — it remembers every customer, every vehicle, every job, every conversation
- A passive organizer that captures, structures, and surfaces business data
- A semi-automated system with a human always in the loop
- The single pane of glass for all non-wrench business operations

**What Phil Is Not:**
- Not a vehicle diagnostic tool, scan tool, or repair manual
- Not a chatbot — Phil never communicates with customers autonomously
- Not a scheduling agent — Phil surfaces data, the operator decides
- Not a billing system — Phil populates drafts, the operator finalizes
- Not a full accounting or ERP platform

---

## 3. The Design Triad

Every design decision, feature priority, and architectural choice must satisfy all three principles simultaneously. If a feature serves one principle but violates another, it is redesigned — not shipped.

### Customer-Centric (Structure)

> The customer is the root entity.

Every screen, every action, every data point traces back to a customer record. The app's primary organizing question is **"who am I serving?"** — not "what's on my calendar" or "how much did I make." Calendar views, job queues, and financial summaries are *lenses* on customer data, not independent silos.

The customer-vehicle pair is the center of gravity. Devon doesn't service "customers" — he services **Mrs. Johnson's 2018 Camry**. The vehicle is the recurring touchpoint — the thing that breaks down repeatedly, that has a service history, that needs the right labor times. A customer may own multiple vehicles; each has its own independent story.

### Data-Focused (Substance)

> The app's value is its data, not its interface.

Every interaction — a text received, a job clocked, a payment logged, miles driven — must produce **structured, permanent, queryable data** that compounds in value over time. No dead tables. No write-without-read paths. No metrics derived from empty sources. If a field exists in the database, something writes to it and something reads from it — or the field doesn't exist.

The interface serves the data. The data does not serve the interface.

After one year of use, Devon should have a living, searchable record of every customer relationship, every vehicle serviced, every dollar earned, every mile driven, and every hour worked. That data IS the business — more valuable than the app's UI will ever be.

### Relation-Minded (Connection)

> Data is never siloed. Context is always one tap away.

Every entity is connected to its full context chain:

```
Customer
 ├── Vehicle(s)
 │    └── Job(s)
 │         ├── Appointment
 │         ├── Clock entries (internal time tracking)
 │         ├── Mileage log
 │         ├── Invoice → Line Items → Payment
 │         └── Notes
 ├── Conversations (SMS threads)
 └── Contact info + address(es)
```

Pulling up any single entity instantly surfaces its relational web. A customer view shows their vehicles, their jobs, their outstanding balance, their last conversation. A job view shows the customer, the vehicle, the clock entries, the mileage, the invoice. **No cross-referencing between separate screens required.**

This also applies to the human relationship: Phil acts as Devon's institutional memory. It remembers what Devon would forget — so every returning customer feels known.

---

## 4. Development Philosophy — Manual First, Automate Later

> Build for hands. Wire for brains later.

Every feature ships as a **fully manual interaction**. The operator taps, types, confirms, and approves every action. No background magic, no AI inference, no automated triggers.

Automation is introduced **only after the manual foundation is verified stable**, and it is layered **on top of** the manual path — never replacing it.

| Phase | Approach | Example |
|---|---|---|
| **v1 (This Build)** | Every function requires explicit human input | Operator manually types customer name, vehicle info, appointment time |
| **Phase 2 (Future)** | Automation accelerates the same manual paths | AI extracts customer name from SMS → pre-populates the field → operator confirms or edits before saving |

> [!CAUTION]
> **The Cortana Lesson:** Cortana built a 4-tier AI extraction waterfall (Gemini → OpenAI → Heuristic → Raw), a geofenced state machine, and a labor analytics engine — all feeding into data paths that were broken, empty, or producing false metrics. The sophistication was real; the foundation was not. Phil will never repeat this. If a feature cannot be performed manually end-to-end, it is not ready for automation.

---

## 5. Human-in-the-Loop (HITL) Gatekeeping

Inherited from the [Cortana anchor](file:///C:/Users/devon/Projects/Cortana/Progression/anchor.md) and carried forward as **non-negotiable law**:

1. **No autonomous customer communication.** Phil never sends a text, email, or notification to a customer without the operator explicitly tapping "Send."
2. **No autonomous data commitment.** Moving data from draft to permanent record requires an explicit operator action — "Save," "Confirm," "Schedule."
3. **No autonomous financial actions.** Invoices, payments, and ledger entries require manual creation and approval.
4. **All Phil-populated fields must be editable** by the operator at any time before finalization.
5. **A complete manual input path must always exist** in parallel to any automated path — for phone calls, walk-ups, corrections, or when automation is wrong.
6. **Phil-populated data in draft state must be visually distinguishable** from manually entered data, so the operator can verify at a glance what came from automation vs. what they typed.

> [!IMPORTANT]
> **The Anchoring Rule:** If a development update attempts to bypass an HITL gate, causes Phil to act autonomously beyond these boundaries, or removes the manual parallel path, it violates the core architecture and must be rejected as feature drift.

---

## 6. Business Context — Mobile vs. Brick-and-Mortar

Phil accomplishes for a mobile business what desk staff do in a brick-and-mortar shop. These differences shape every design decision:

| Brick-and-Mortar | Mobile (Phillips) | Design Implication |
|---|---|---|
| Customers walk in | Operator drives to the customer | Route awareness and mileage tracking are core features |
| Fixed bays, multiple techs | One technician, one job at a time | Sequential job queue, not a multi-bay dispatcher |
| Walk-in and phone traffic | All leads arrive via SMS/phone | Native communication is the primary intake funnel |
| Receptionist answers phones live | Operator is often under a car | Asynchronous review — intake happens, operator reviews when free |
| Multiple staff on separate devices | Solo operator, single device | Single-user, single-device for v1 |
| Landline + POS terminal | Android phone in a pocket | Mobile-first, thumb-operable, offline-capable |
| Customers see the shop | Customers never see "the office" | Phil's UI is operator-only — no customer-facing surface |

---

## 7. The Billing Model — Flat Rate

Devon charges **flat rate** — a fixed price per repair operation based on published labor times (book hours). The customer pays the same whether the physical work takes 45 minutes or 2 hours.

### Per-Job Pricing Formula

```
Job Total = Σ(Labor Line Items) + Σ(Parts Line Items) + Σ(Misc Line Items)
          + Onsite Service Fee + Tax

Where:
  Labor Line Item  = Book Hours × Labor Rate ($/hr)
  Parts Line Item  = Part Cost × Markup Multiplier (or manual price)
  Misc Line Item   = Flat amount (shop supplies, disposal fees, etc.)
  Onsite Service Fee = Flat fee per job (covers travel)
  Tax              = SC 6% on PARTS ONLY (SC Code Regs. § 117-306)
                     Labor and service fees are NOT taxed.
```

### Configurable Variables (Shop Settings)

**Every variable in the pricing formula lives in Shop Settings.** None are hardcoded.

| Setting | Description | Example |
|---|---|---|
| Labor Rate | Dollars per book-hour | $125.00/hr |
| Onsite Service Fee | Flat per-job travel fee | $60.00 |
| Parts Markup Multiplier | Multiplier on parts cost | 1.4× |
| Tax Rate | State sales tax on parts | 6.0% (600 basis points) |
| Business Name | For invoices/documents | Phillips Mobile Automotive |
| Business Address | Operator's base location | [configurable] |
| Tax ID | For invoices/documents | [configurable] |
| License Number | For invoices/documents | [configurable] |

### The Fiduciary Rule

Inherited from Cortana and **non-negotiable**:

> All monetary values are stored as **64-bit integer cents** (`Long`). No floating-point currency math, ever.

- `$125.00/hr` → stored as `12500L`
- Tax rate 6% → stored as `600L` basis points (applied as `taxCents = partsCents * 600 / 10000`)
- This eliminates rounding errors in tax and margin calculations

---

## 8. The Time Clock — Internal Tracking

The time clock is **completely decoupled from billing**. The customer never sees it. Billing uses book hours (flat rate). The clock exists for Devon's internal operational awareness:

| What It Tracks | Why It Matters |
|---|---|
| Actual time on-site per job | Efficiency — am I beating book time or falling behind? |
| Total daily work hours | Personal time management — how long was my day? |
| Book time vs. actual time ratio | Profitability signal — if book says 2.0 hrs and I finished in 1.2, my effective hourly rate is higher |

**v1:** Manual — tap "Clock In" when starting a job, tap "Clock Out" when done.
**Phase 2:** GPS-triggered — auto-start on arrival at job site, auto-stop on departure. Manual buttons remain as override/fallback.

---

## 9. Mileage Tracking

Every job requires driving. Mileage is:
- A **tax-deductible business expense** (IRS standard mileage rate)
- A real **operating cost** (fuel, vehicle wear)
- A data point for evaluating whether the onsite service fee adequately covers travel

**v1:** Manual entry — total miles per trip, linked to a specific job or marked as general business driving.
**Phase 2:** GPS-based automatic mileage logging with manual override.

Mileage data feeds into:
- **Pillar 3 (Billing):** Expense tracking and categorization
- **Pillar 5 (Analytics):** Daily/weekly/monthly mileage summaries for tax reporting

---

## 10. The Five Core Functions

### Function 1: Communications (The Intake Funnel)
*Replaces: Receptionist answering phones*

The phone is the storefront. Every lead, every scheduling conversation, every "running 20 minutes late" goes through text. This is the **root node** of the data web — the channel through which customers enter the system.

| Feature | v1 (Manual) | Phase 2 (Automated) |
|---|---|---|
| View incoming SMS | ✅ | ✅ |
| Compose and send SMS | ✅ Operator types and taps send | Reply templates with auto-populated fields |
| Link conversation to customer record | ✅ Manual linking | Auto-match by phone number |
| Create customer from conversation | ✅ Manual entry | AI-extracted pre-populated fields (operator confirms) |
| Conversation history per customer | ✅ | ✅ |

### Function 2: Scheduling & Customer Management
*Replaces: Receptionist managing calendar and customer files*

| Feature | v1 (Manual) | Phase 2 (Automated) |
|---|---|---|
| Customer directory (name, phone, address) | ✅ Manual CRUD | Auto-populated from SMS intake |
| Vehicle registry (year, make, model, engine) | ✅ Manual CRUD | AI-extracted from conversation |
| Vehicle linked to customer | ✅ | ✅ |
| Appointment calendar (day/week view) | ✅ | ✅ |
| Create/edit/cancel appointments | ✅ | ✅ |
| Appointment reminders | ✅ Manual SMS compose | Automated SMS with operator approval |
| Customer + vehicle history on one screen | ✅ | ✅ |

### Function 3: Job Execution & Field Tracking
*Replaces: Dispatcher managing workflow*

| Feature | v1 (Manual) | Phase 2 (Automated) |
|---|---|---|
| Sequential job queue (today's jobs in order) | ✅ | ✅ |
| Job status progression (Scheduled → En Route → On Site → Complete) | ✅ Manual tap to advance | GPS-triggered transitions |
| Time clock (clock in/out per job) | ✅ Manual tap | GPS-triggered with manual override |
| Mileage logging (per job) | ✅ Manual odometer/miles entry | GPS-based auto-logging |
| Job notes | ✅ | ✅ |
| Route awareness (addresses, sequence) | ✅ Address display | Map integration with drive-time estimates |

### Function 4: Billing & Financial Records
*Replaces: Office administrator handling invoices and books*

| Feature | v1 (Manual) | Phase 2 (Automated) |
|---|---|---|
| Estimate/Invoice builder | ✅ | ✅ |
| Labor line items (book hours × labor rate) | ✅ Manual hour entry | CHARM catalog lookup |
| Parts line items (cost + markup) | ✅ | ✅ |
| Misc line items (flat amounts) | ✅ | ✅ |
| Onsite service fee (from Shop Settings) | ✅ Auto-applied | ✅ |
| Tax calculation (SC 6% parts only) | ✅ Auto-calculated | ✅ |
| All rates from Shop Settings | ✅ | ✅ |
| Payment logging (cash, check, card, digital) | ✅ | ✅ |
| Mark invoice as paid | ✅ | ✅ |
| Expense tracking (parts, fuel, supplies) | ✅ Manual entry | Receipt photo OCR |
| Daily revenue summary | ✅ | ✅ |
| All math in integer cents | ✅ | ✅ |

### Function 5: Business Oversight & Analytics
*Replaces: Shop manager reviewing KPIs*

| Feature | v1 (Manual) | Phase 2 (Automated) |
|---|---|---|
| Dashboard — today's jobs, outstanding invoices, revenue snapshot | ✅ | ✅ |
| Efficiency tracking — actual time vs. book time (internal) | ✅ | ✅ |
| Mileage summaries — daily/weekly/monthly for tax reporting | ✅ | ✅ |
| Job history — searchable by customer, vehicle, date, type | ✅ | ✅ |
| Financial trends — revenue and expense over time | ✅ | ✅ |
| Shop Settings — all configurable business variables | ✅ | ✅ |
| Customer insights — repeat visit frequency, lifetime value | Deferred | ✅ |

---

## 11. Cortana Reference Ledger

### Ideological DNA — Preserved

| Concept | Origin | Status in Phil |
|---|---|---|
| "Phil is a digital prefrontal cortex" | [Cortana anchor §2](file:///C:/Users/devon/Projects/Cortana/Progression/anchor.md) | ✅ Core identity |
| HITL gatekeeping | [Cortana anchor §6](file:///C:/Users/devon/Projects/Cortana/Progression/anchor.md) | ✅ Non-negotiable law |
| Fiduciary Rule (integer cents) | [Cortana architecture](file:///C:/Users/devon/Projects/Cortana/Progression/ARCHITECTURE_OVERVIEW.md) | ✅ Adopted |
| SC tax compliance (6% parts only) | [MarginEngine](file:///C:/Users/devon/Projects/Cortana/shared/src/commonMain/kotlin/com/phillips/cortana/logic) | ✅ Adopted |
| Local-first / offline-capable | [Cortana anchor §3](file:///C:/Users/devon/Projects/Cortana/Progression/anchor.md) | ✅ Core requirement |
| Configurable business profile | [Cortana anchor §7](file:///C:/Users/devon/Projects/Cortana/Progression/anchor.md) | ✅ Shop Settings — zero hardcoded values |
| Comms as the root data node | [Cortana anchor §3](file:///C:/Users/devon/Projects/Cortana/Progression/anchor.md) | ✅ Communications is Function 1 |
| Burden of Proof audit methodology | [Layoffs Audit](file:///C:/Users/devon/Projects/Cortana/Layoffs_Autdit_Plan.md) | ✅ Ongoing code quality standard |
| Occam's Razor audit framework | [OCCAMS_RAZOR_AUDIT.md](file:///C:/Users/devon/Projects/Cortana/OCCAMS_RAZOR_AUDIT.md) | ✅ Reusable on Phil |

### Cortana Failures — How Phil Avoids Them

| What Went Wrong | Root Cause | Phil's Safeguard |
|---|---|---|
| AI pipeline built before manual paths worked | Wrong build order | Manual-first philosophy — no automation until manual is verified |
| Dead data paths (tables written but never read, or read but never written) | Feature scaffolding without wiring | **Data-Focused principle** — every field has a write AND read path before it ships |
| God Object (`TriageRepository` with 15 constructor params) | Responsibility creep | Each repository owns one domain |
| Hardcoded business values (`$125/hr`, `"123 Main St"`) baked into logic | Laziness during prototyping | All business values in Shop Settings entity, accessed via DAO. Zero hardcoded rates. |
| Stringly-typed state machines (`LedgerEntity.state` as bare strings) | No compile-time safety | Enum classes for all state fields — typos are compile errors |
| KMP `commonMain` violations (`android.util.Log` in shared code) | Sloppy source-set discipline | Clean separation from day one |
| Feature creep before foundation (PDF export, template editors, analytics dashboards before CRUD worked) | No scope discipline | v1 scope is locked — five functions, manual CRUD, nothing else |
| JVM heap (4GB) exceeded physical RAM (3GB) | Ignored hardware constraints | JVM heap ≤ 2GB for dev builds |
| False metrics displayed on-screen from empty tables | No data integrity validation | **Data-Focused principle** — if a metric has no source data, it does not render |

### Cortana Assets — Reference Only (Not Copied)

| Asset | Reference Value for Phil |
|---|---|
| [MarginEngine + 22 unit tests](file:///C:/Users/devon/Projects/Cortana/shared/src/commonMain/kotlin/com/phillips/cortana/logic) | Financial math patterns — tax isolation, cents arithmetic, markup formulas |
| [CHARM labor catalog + FTS5 search](file:///C:/Users/devon/Projects/Cortana/shared/src/commonMain/kotlin/com/phillips/cortana/data) | Labor catalog schema and full-text search approach (Phase 2) |
| [DagStateMachine + Haversine geofencing](file:///C:/Users/devon/Projects/Cortana/shared/src/commonMain/kotlin/com/phillips/cortana/logic) | GPS-driven job lifecycle state machine (Phase 2) |
| [TicketBuilderScreen + atomic commit](file:///C:/Users/devon/Projects/Cortana/shared/src/commonMain/kotlin/com/phillips/cortana/presentation) | Multi-table atomic write pattern for job creation |
| [ER diagram + schema v10](file:///C:/Users/devon/Projects/Cortana/Progression/ARCHITECTURE_OVERVIEW.md) | Relational data model reference |
| [StanleyHeuristic + make/model index](file:///C:/Users/devon/Projects/Cortana/shared/src/commonMain/kotlin/com/phillips/cortana/logic) | Vehicle identification from text (Phase 2) |

---

## 12. Technical Constraints

From the [system constraints ledger](file:///C:/Users/devon/Projects/Cortana/system_constraints_ledger.md):

| Constraint | Value |
|---|---|
| Dev machine | HP Laptop 15-fd0xxx, Intel N200, 4GB LPDDR4, 119GB NVMe |
| Dev OS | Windows 11 Home (Build 26200) |
| Available RAM | ~3.7GB OS-visible, ~500MB free at idle |
| Target device | Samsung Galaxy A16 (4GB RAM) |
| JVM heap limit (safe) | ≤ 2GB (`-Xmx2g`) |
| Storage remaining | ~17GB free — builds must be lean |
| GPU | Integrated Intel UHD — no hardware-accelerated emulator |
| Network | Wi-Fi 6 + Tailscale VPN |

---

## 13. v1 Scope Boundaries

### In Scope — v1 (Manual Foundation)

- Full CRUD: Customers, Vehicles, Appointments, Jobs, Invoices, Payments, Expenses
- Native SMS communication (view, compose, send — operator-driven)
- Conversation history linked to customer records
- Appointment calendar with day view
- Sequential job queue with manual status progression
- Time clock (manual clock in/out per job, internal tracking only)
- Mileage logging (manual entry per job or per day)
- Flat-rate invoice builder (labor book hours × rate, parts + markup, misc, service fee, SC tax)
- Payment logging and daily revenue summary
- Expense tracking with categorization
- Business profile / Shop Settings (all configurable variables)
- Offline-first local Room database
- Material 3 / Jetpack Compose UI
- Single-user, single-device

### Explicitly Out of Scope — v1

| Excluded | Rationale | Phase |
|---|---|---|
| AI/ML (Gemini, OpenAI, heuristic extraction) | Manual-first — automation after foundation is verified | 2 |
| Automated SMS parsing or intake extraction | Manual-first | 2 |
| GPS-triggered clock/mileage/status changes | Manual-first | 2 |
| CHARM labor catalog integration | Labor times entered manually in v1 | 2 |
| Map/routing API integration (Google Maps) | Address display only in v1 | 2 |
| PDF generation / document export | Copy/paste or screenshot in v1 | 2 |
| Multi-device sync / cloud backend | Single-device in v1 | 3 |
| Parts inventory management | Out of project scope entirely | — |
| Customer-facing surfaces | Phil is operator-only | — |

---

## 14. Open Questions

> [!WARNING]
> These need answers before architecture and implementation planning begin.

1. **App name / branding?** Is "Phil" the user-facing app name, or should there be a different name on the launcher icon? Color scheme or brand direction?
2. **Target Android version?** Minimum SDK — API 26 (Android 8.0) covers 95%+ of active devices. Acceptable?
3. **Pure Android or Kotlin Multiplatform?** Cortana used KMP. For a single-device Android app with no iOS plans, pure Kotlin + Compose is simpler and avoids the source-set headaches that plagued Cortana. Recommendation: **pure Android.**
4. **Database engine?** Room worked in Cortana (despite some KMP friction). For pure Android, Room is the natural choice. Acceptable?
5. **Repository location?** Should Phil live in `C:\Users\devon\Projects\ghost`, a new repo, or reuse Cortana's repo with a clean branch?

---

*End of Anchoring Document.*

*Nothing gets built that isn't anchored here. Nothing gets automated that doesn't work manually first. Nothing gets stored that isn't read. Nothing gets displayed that isn't real. The customer is the center. The data is the value. The relationships are the product.*
