# Project Phill — Complete Codebase Walkthrough

> **Studied:** May 26, 2026
> **Codebase:** `C:\Users\devon\Projects\Phill` (repo lives in `C:\Users\devon\Projects\ghost`)
> **Package:** `com.phillips.phill`
> **Total:** ~83 Kotlin files, ~7,814 lines of code

---

## Quick Summary

Phill is a **native Android** shop management app for **Phillips Mobile Automotive** — a solo-operated mobile auto repair business in Upstate South Carolina. It's a ground-up rebuild of a failed predecessor ("Project Cortana"), inheriting its domain knowledge but none of its code.

The app serves as a **"digital prefrontal cortex"** — handling customers, vehicles, scheduling, job tracking, invoicing, payments, expenses, mileage, SMS communications, and a dashboard — so the operator (Devon Phillips) can focus on wrenching.

**Key design principles:**
- **Customer-centric** — the customer is the root entity, everything traces back
- **Manual-first** — no AI/automation in v1, every action requires an explicit tap
- **Offline-first** — Room/SQLite local database, no network dependency
- **HITL (Human-in-the-Loop)** — 6 non-negotiable safety rules prevent autonomous actions
- **Fiduciary Rule** — all money stored as `Long` integer cents, never floats

---

## Architecture at a Glance

```
com.phillips.phill/
├── MainActivity.kt              # Single activity, Hilt entry point
├── PhillApplication.kt          # @HiltAndroidApp
├── data/
│   ├── entity/     (13 files)   # Room @Entity classes
│   ├── dao/        (13 files)   # Room @Dao interfaces
│   ├── database/   (2 files)    # PhillDatabase + Converters
│   └── repository/ (6 files)    # Domain repositories
├── domain/
│   ├── enums/      (7 files)    # Type-safe state machines
│   └── billing/    (1 file)     # BillingEngine (pure math)
├── di/             (2 files)    # Hilt modules
├── navigation/     (2 files)    # Nav3 keys + graph
├── sms/            (2 files)    # SmsReceiver + SmsSyncManager
└── ui/
    ├── theme/      (3 files)    # Material 3 theming
    ├── components/ (1 file)     # PhillBottomBar
    ├── dashboard/  (2 files)    # Dashboard screen + VM
    ├── schedule/   (4 files)    # Schedule + Appointment form
    ├── jobs/       (4 files)    # Job queue + Job detail
    ├── comms/      (4 files)    # Conversation list + detail
    ├── customers/  (8 files)    # Customer list/detail/form + vehicle form
    ├── billing/    (10 files)   # Invoice builder/detail + expense + payment log + billing hub
    ├── settings/   (2 files)    # Shop settings
    └── analytics/  (2 files)    # Analytics overview
```

---

## Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.1.20 |
| UI | Jetpack Compose + Material 3 | BOM 2025.05.01 |
| Navigation | `androidx.navigation3` | 0.1.0-alpha04 |
| Database | Room | 2.7.1 |
| DI | Hilt (Dagger) | 2.56.2 |
| Build | Gradle (Kotlin DSL) | 9.1.0 |
| Serialization | kotlinx.serialization | 1.8.1 |
| Min SDK | 26 (Android 8.0) |  |
| Target SDK | 36 |  |
| Target Device | Samsung Galaxy A16 | 4GB RAM |

---

## 1. Data Layer

### 1.1 Room Entities (13 tables)

| Entity | Table | PK | Key Fields | FKs |
|---|---|---|---|---|
| [CustomerEntity](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/CustomerEntity.kt) | `customers` | UUID text | firstName, lastName, phone (unique), email, address, notes | — (root) |
| [VehicleEntity](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/VehicleEntity.kt) | `vehicles` | UUID text | year, make, model, engine, vin, color, notes | customer_id → CASCADE |
| [AppointmentEntity](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/AppointmentEntity.kt) | `appointments` | UUID text | address, scheduledStart/End, status, notes, jobId | customer_id → CASCADE |
| [JobEntity](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/JobEntity.kt) | `jobs` | UUID text | status, description, notes, completedAt | customer_id, vehicle_id → CASCADE |
| [ClockEntryEntity](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/ClockEntryEntity.kt) | `clock_entries` | UUID text | clockInEpoch, clockOutEpoch (nullable = active), notes | job_id → CASCADE |
| [MileageEntryEntity](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/MileageEntryEntity.kt) | `mileage_entries` | UUID text | miles (Double), purpose | job_id → SET NULL |
| [InvoiceEntity](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/InvoiceEntity.kt) | `invoices` | UUID text | status, subtotalCents, taxCents, totalCents, serviceFeeCents | job_id, customer_id → CASCADE |
| [LineItemEntity](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/LineItemEntity.kt) | `line_items` | UUID text | type, description, qtyThousandths, unitPriceCents, totalCents, isTaxable, sortOrder | invoice_id → CASCADE |
| [PaymentEntity](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/PaymentEntity.kt) | `payments` | UUID text | amountCents, method, referenceNumber, paidAtEpoch | invoice_id → CASCADE |
| [ExpenseEntity](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/ExpenseEntity.kt) | `expenses` | UUID text | category, description, amountCents, vendor, dateEpoch | job_id → SET NULL |
| [ConversationEntity](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/ConversationEntity.kt) | `conversations` | UUID text | phoneNumber, displayName, lastMessageEpoch, unreadCount | customer_id → SET NULL |
| [MessageEntity](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/MessageEntity.kt) | `messages` | UUID text | body, timestampEpoch, isInbound, status | conversation_id → CASCADE |
| [ShopProfileEntity](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/ShopProfileEntity.kt) | `shop_profile` | Int (always 1) | laborRateCents, serviceFeeCents, partsMarkupBP, taxRateBP, businessName/Address, taxId, license, ownerName/Phone | — (singleton) |

**Monetary storage:** All currency values stored as `Long` cents (e.g., `$125.00` → `12500L`). Quantities use thousandths (e.g., `1.5 hours` → `1500L`). Tax rate and markup use basis points (e.g., `6%` → `600`).

### 1.2 DAOs (13 interfaces)

Each entity has a corresponding DAO. Key patterns:
- All use `@Upsert` for insert-or-update (no separate insert/update)
- List queries return `Flow<List<T>>` for reactive observation
- Single lookups use `suspend` functions returning nullable
- Date-range and status-filtered queries where needed
- [MileageEntryDao](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/dao/MileageEntryDao.kt) has `totalMilesInRange()` returning `Double`
- [PaymentDao](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/dao/PaymentDao.kt) has `totalPaidForInvoice()` returning `Long`
- [ExpenseDao](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/dao/ExpenseDao.kt) has `totalExpensesInRange()` returning `Long`

### 1.3 Database

[PhillDatabase](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/database/PhillDatabase.kt) — Room database v1, registers all 13 entities, provides 13 abstract DAO accessors.

[Converters](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/database/Converters.kt) — Type converters for all 7 enums (String ↔ enum via `.name` / `valueOf()`).

### 1.4 Repositories (6 classes)

| Repository | Scope | DAOs Injected | Domain |
|---|---|---|---|
| [CustomerRepository](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/repository/CustomerRepository.kt) | `@Singleton` | Customer, Vehicle | Customer CRUD, vehicle CRUD, search |
| [ScheduleRepository](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/repository/ScheduleRepository.kt) | `@Singleton` | Appointment | Appointment CRUD, date-range queries |
| [JobRepository](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/repository/JobRepository.kt) | `@Singleton` | Job, ClockEntry, MileageEntry | Job CRUD, clock in/out, mileage |
| [BillingRepository](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/repository/BillingRepository.kt) | `@Singleton` | Invoice, LineItem, Payment | Invoice CRUD, line items, payments |
| [CommsRepository](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/repository/CommsRepository.kt) | `@Singleton` | Conversation, Message | SMS threads, message CRUD, getOrCreate |
| [OperationsRepository](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/repository/OperationsRepository.kt) | `@Singleton` | ShopProfile, Expense, MileageEntry | Settings, expenses, mileage summaries |

All repos wrap DAO calls in `withContext(Dispatchers.IO)`.

---

## 2. Domain Layer

### 2.1 Enums (7 type-safe state machines)

| Enum | Values |
|---|---|
| [JobStatus](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/domain/enums/JobStatus.kt) | `SCHEDULED`, `EN_ROUTE`, `ON_SITE`, `COMPLETE` |
| [InvoiceStatus](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/domain/enums/InvoiceStatus.kt) | `ESTIMATE`, `INVOICE`, `PAID`, `VOID` |
| [AppointmentStatus](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/domain/enums/AppointmentStatus.kt) | `PENDING`, `CONFIRMED`, `CANCELLED` |
| [LineItemType](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/domain/enums/LineItemType.kt) | `LABOR`, `PARTS`, `MISC` |
| [PaymentMethod](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/domain/enums/PaymentMethod.kt) | `CASH`, `CHECK`, `CARD`, `DIGITAL` |
| [ExpenseCategory](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/domain/enums/ExpenseCategory.kt) | `PARTS`, `FUEL`, `SUPPLIES`, `TOOLS`, `OTHER` |
| [MessageStatus](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/domain/enums/MessageStatus.kt) | `SENT`, `DELIVERED`, `FAILED`, `RECEIVED` |

All stored as `TEXT` in Room via the [Converters](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/database/Converters.kt) class.

### 2.2 BillingEngine

[BillingEngine](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/domain/billing/BillingEngine.kt) — **pure, stateless, static math**. No DI, no side effects. All integer cents arithmetic.

**Core methods:**
| Method | Formula |
|---|---|
| `calculateLineItemTotal(qtyThousandths, unitPriceCents)` | `qty × price / 1000` |
| `calculateTax(taxableSubtotalCents, taxRateBP)` | `subtotal × rate / 10000` |
| `applyPartsMarkup(costCents, markupBP)` | `cost × markup / 10000` |
| `calculateInvoiceTotal(lineItems, serviceFeeCents, taxRateBP)` | Sum labor + parts + misc subtotals, add fee, add tax (parts only) |

**Formatting utilities:**
| Method | Example |
|---|---|
| `formatCents(12500L)` | `"$125.00"` |
| `parseDollarsToCents("125.00")` | `12500L` |
| `formatBasisPoints(600)` | `"6.00%"` |
| `parsePercentToBasisPoints("6.0")` | `600` |
| `formatThousandths(1500L)` | `"1.500"` → displayed as `"1.5"` |
| `parseHoursToThousandths("1.5")` | `1500L` |

Returns an `InvoiceTotals` data class with: laborSubtotal, partsSubtotal, miscSubtotal, serviceFeeCents, taxCents, grandTotal.

---

## 3. DI (Hilt)

### [DatabaseModule](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/di/DatabaseModule.kt) (`@InstallIn(SingletonComponent)`)

Provides:
- `PhillDatabase` singleton (built with `Room.databaseBuilder`)
- All 13 DAO instances (extracted from the database)

### [RepositoryModule](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/di/RepositoryModule.kt) (`@InstallIn(SingletonComponent)`)

Binds all 6 repositories as `@Singleton`.

---

## 4. SMS Infrastructure

### [SmsReceiver](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/sms/SmsReceiver.kt)

A `BroadcastReceiver` registered in the manifest with `android.provider.Telephony.SMS_RECEIVED` intent filter (priority 999). Extracts sender phone number and message body from the SMS PDU, then stores in Room via `CommsRepository.getOrCreateConversation()` + `saveMessage()`.

Runs in a `goAsync()` + coroutine scope to safely perform DB writes on the broadcast thread.

### [SmsSyncManager](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/sms/SmsSyncManager.kt)

Syncs existing SMS history from the device's content provider (`content://sms`) into the app's local database on first launch or on-demand. Reads all SMS threads and imports them as conversations + messages.

---

## 5. Navigation

### [NavKeys](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/navigation/NavKeys.kt)

18 navigation keys using `@Serializable` for Navigation 3:
- **Objects** (no params): `DashboardKey`, `ScheduleKey`, `JobQueueKey`, `CommsKey`, `CustomerListKey`, `BillingKey`, `AnalyticsKey`, `ShopSettingsKey`, `PaymentLogKey`
- **Data classes** (with params): `CustomerDetailKey(customerId)`, `CustomerFormKey(customerId?)`, `VehicleFormKey(customerId, vehicleId?)`, `AppointmentFormKey(appointmentId?)`, `JobDetailKey(jobId)`, `InvoiceBuilderKey(jobId, invoiceId?)`, `InvoiceDetailKey(invoiceId)`, `ConversationKey(conversationId)`, `ExpenseFormKey(jobId?, expenseId?)`

### [PhillNavGraph](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/navigation/PhillNavGraph.kt)

Uses `NavDisplay` (Navigation 3 alpha) with a `rememberMutableStateListOf(DashboardKey)` back stack. Each entry maps a key type to a `NavEntry` composable. Contains `onNavigate` lambdas threaded to each screen.

---

## 6. UI Layer

### 6.1 Bottom Navigation

[PhillBottomBar](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/components/PhillBottomBar.kt) — Material 3 `NavigationBar` with 4 primary tabs:
1. 🏠 **Dashboard** → `DashboardKey`
2. 📅 **Schedule** → `ScheduleKey`
3. 🔧 **Jobs** → `JobQueueKey`
4. 💬 **Messages** → `CommsKey`

Plus overflow menu items for: Customers, Billing, Analytics, Settings.

### 6.2 All Screens (18 composables, 15 ViewModels)

#### Dashboard
| File | Purpose |
|---|---|
| [DashboardScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/dashboard/DashboardScreen.kt) | Today's overview: active jobs, today's revenue, today's appointments, unpaid invoices, outstanding balance |
| [DashboardViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/dashboard/DashboardViewModel.kt) | Aggregates from Job, Schedule, Billing, Payment, Operations repos |

#### Schedule
| File | Purpose |
|---|---|
| [ScheduleScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/ScheduleScreen.kt) | Day/week/month calendar views, 15-min buffer display, appointment cards |
| [ScheduleViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/ScheduleViewModel.kt) | Date navigation, appointment queries by date range |
| [AppointmentFormScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/AppointmentFormScreen.kt) | Create/edit appointment with customer search, vehicle select, date/time pickers |
| [AppointmentFormViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/AppointmentFormViewModel.kt) | Saves appointment + auto-creates linked Job in SCHEDULED status |

#### Jobs
| File | Purpose |
|---|---|
| [JobQueueScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/jobs/JobQueueScreen.kt) | Filterable job list (All/Scheduled/En Route/On Site/Complete) |
| [JobQueueViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/jobs/JobQueueViewModel.kt) | Job list observation with status filter |
| [JobDetailScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/jobs/JobDetailScreen.kt) | Full job detail: status progression buttons, clock in/out, mileage logging, linked invoices |
| [JobDetailViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/jobs/JobDetailViewModel.kt) | Job CRUD, status transitions, clock management, mileage |

#### Customers
| File | Purpose |
|---|---|
| [CustomerListScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/CustomerListScreen.kt) | Searchable customer directory |
| [CustomerListViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/CustomerListViewModel.kt) | Search query + filtered list |
| [CustomerDetailScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/CustomerDetailScreen.kt) | Single pane of glass: contact info, vehicles, jobs, balance, quick actions |
| [CustomerDetailViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/CustomerDetailViewModel.kt) | Loads customer + vehicles + jobs + invoices |
| [CustomerFormScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/CustomerFormScreen.kt) | Create/edit customer form |
| [CustomerFormViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/CustomerFormViewModel.kt) | Save customer with validation |
| [VehicleFormScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/VehicleFormScreen.kt) | Create/edit vehicle form |
| [VehicleFormViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/VehicleFormViewModel.kt) | Save vehicle linked to customer |

#### Billing
| File | Purpose |
|---|---|
| [BillingHubScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/BillingHubScreen.kt) | Overview: unpaid invoices, recent payments, expenses, totals |
| [BillingHubViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/BillingHubViewModel.kt) | Aggregated billing queries |
| [InvoiceBuilderScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/InvoiceBuilderScreen.kt) | Add labor/parts/misc line items, real-time totals, save estimate / finalize invoice |
| [InvoiceBuilderViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/InvoiceBuilderViewModel.kt) | BillingEngine integration, line item management, invoice save |
| [InvoiceDetailScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/InvoiceDetailScreen.kt) | Invoice read-only view with payment history, remaining balance, log payment action |
| [InvoiceDetailViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/InvoiceDetailViewModel.kt) | Invoice + line items + payments, finalize, void, log payment |
| [ExpenseFormScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/ExpenseFormScreen.kt) | Create/edit expense form |
| [ExpenseFormViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/ExpenseFormViewModel.kt) | Expense CRUD |
| [PaymentLogScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/PaymentLogScreen.kt) | All payments list view |
| [PaymentLogViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/PaymentLogViewModel.kt) | Payment history observation |

#### Communications (SMS)
| File | Purpose |
|---|---|
| [ConversationListScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/comms/ConversationListScreen.kt) | SMS thread list with unread badges |
| [ConversationListViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/comms/ConversationListViewModel.kt) | Conversation list observation |
| [ConversationDetailScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/comms/ConversationDetailScreen.kt) | Chat bubble view + compose bar + send button |
| [ConversationDetailViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/comms/ConversationDetailViewModel.kt) | Messages for thread, send SMS via `SmsManager`, mark read |

#### Settings
| File | Purpose |
|---|---|
| [ShopSettingsScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/settings/ShopSettingsScreen.kt) | All configurable business variables |
| [ShopSettingsViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/settings/ShopSettingsViewModel.kt) | Load/save ShopProfile, BillingEngine formatting |

#### Analytics
| File | Purpose |
|---|---|
| [AnalyticsScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/analytics/AnalyticsScreen.kt) | Business overview: revenue trends, efficiency metrics, mileage summaries |
| [AnalyticsViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/analytics/AnalyticsViewModel.kt) | Aggregated analytics queries |

### 6.3 Theme

| File | Contents |
|---|---|
| [Color.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/theme/Color.kt) | Material 3 color scheme (light + dark) |
| [Theme.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/theme/Theme.kt) | `PhillTheme` composable, dynamic color support |
| [Type.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/theme/Type.kt) | Typography definitions |

---

## 7. Data Flow — End to End

### Customer Intake → Job → Invoice → Payment

```
SMS arrives → SmsReceiver stores to Room
  → Operator opens Comms tab → sees conversation
  → Links to existing customer (or creates new)
  → From Customer Detail → adds vehicle
  → Creates appointment (Schedule tab)
    → Job auto-created in SCHEDULED status
  → Day-of: Job queue → tap job
    → Status: SCHEDULED → EN_ROUTE → ON_SITE → COMPLETE
    → Clock in/out while ON_SITE (internal tracking)
    → Log mileage per trip
  → From Job Detail → Create Estimate
    → Add labor line items (book hours × rate from ShopSettings)
    → Add parts line items (cost × markup from ShopSettings)
    → Add misc line items
    → Service fee auto-applied from ShopSettings
    → Tax auto-calculated (6% on PARTS ONLY)
    → Save as Estimate → later Finalize as Invoice
  → From Invoice Detail → Log Payment
    → Amount, method (Cash/Check/Card/Digital), reference #
    → When balance = $0 → status auto-changes to PAID
```

---

## 8. Entity Relationship Summary

```
Customer (ROOT)
 ├── Vehicle(s)         [CASCADE delete]
 │    └── Job(s)        [CASCADE delete]
 │         ├── ClockEntry(s)     [CASCADE delete]
 │         ├── MileageEntry(s)   [SET NULL on delete]
 │         ├── Invoice(s)        [CASCADE delete]
 │         │    ├── LineItem(s)  [CASCADE delete]
 │         │    └── Payment(s)   [CASCADE delete]
 │         └── Expense(s)        [SET NULL on delete]
 ├── Appointment(s)     [CASCADE delete]
 │    └── linked Job (jobId field)
 └── Conversation(s)    [SET NULL on delete]
      └── Message(s)    [CASCADE delete]

ShopProfile (singleton, id=1, no FKs)
```

---

## 9. Permissions & Manifest

[AndroidManifest.xml](file:///C:/Users/devon/Projects/Phill/app/src/main/AndroidManifest.xml):

**Permissions:**
- `READ_SMS` — read existing SMS threads
- `SEND_SMS` — send outbound SMS (operator-initiated only)
- `RECEIVE_SMS` — real-time inbound SMS capture
- `READ_CONTACTS` — display contact names

**Components:**
- `MainActivity` (launcher, single-activity architecture)
- `SmsReceiver` (broadcast receiver for `SMS_RECEIVED`, priority 999)

---

## 10. Key Design Decisions & Observations

### ✅ What's Done Well

1. **Fiduciary Rule** — all money is `Long` cents, all quantities `Long` thousandths. No `Double` or `Float` for currency anywhere.
2. **Type-safe state machines** — all status fields use Kotlin enums, not strings. Typos are compile errors.
3. **Clean separation** — entities, DAOs, repos, ViewModels, screens are all in their own files with clear responsibilities.
4. **Data-Focused adherence** — every table has both write paths (forms) and read paths (list/detail screens). No dead tables.
5. **HITL enforcement** — SMS send requires explicit tap, invoice finalization requires explicit tap, all forms require Save.
6. **Single responsibility repos** — each repository owns one domain (unlike Cortana's 15-param TriageRepository).
7. **Offline-first** — pure Room, no network calls, no cloud dependencies.

### ⚠️ Areas to Watch

1. **Navigation 3 is alpha** (`0.1.0-alpha04`) — API may change significantly before stable release.
2. **No migration strategy** — database is v1 with no migrations defined. Schema changes will require either destructive recreation or implementing migrations.
3. **`SmsSyncManager` imports ALL SMS** — on devices with large SMS history, this could be slow or memory-intensive on the 4GB target device.
4. **No input validation guards** — phone number uniqueness is enforced by the database index, but UI-level validation/error handling for duplicates should be verified.
5. **Mileage stored as `Double`** — this is the one place where floating-point is used. `miles` field in `mileage_entries` is `REAL`. This is acceptable for mileage (not currency) but worth noting.
6. **No unit tests present** — the `test/` and `androidTest/` directories exist but appear empty or minimal. BillingEngine in particular would benefit from the 22-test suite pattern inherited from Cortana's MarginEngine.

---

## 11. File Inventory (83 Kotlin files)

| Layer | File Count |
|---|---|
| Entities | 13 |
| DAOs | 13 |
| Database + Converters | 2 |
| Repositories | 6 |
| Enums | 7 |
| BillingEngine | 1 |
| DI Modules | 2 |
| Navigation | 2 |
| SMS Infrastructure | 2 |
| Screens (Composables) | 18 |
| ViewModels | 15 |
| Theme | 3 |
| Components | 1 |
| App Entry (MainActivity, PhillApplication) | 2 |
| **Total** | **~83** |

---

*End of walkthrough. The codebase is clean, well-structured, and faithfully implements the anchoring document's vision. All 5 core functions are present. The Manual-First philosophy is respected throughout — no AI, no automation, no network calls.*
