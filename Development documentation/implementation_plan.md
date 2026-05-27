# Sprint Plan: Unblock All Test-Blocked Screens

> **Goal:** Fix the 2 root causes that block 11 screens from being tested
> **Blocked screens:** Customer Detail, Customer Edit, Vehicle Form, Job Detail, Invoice Builder, Invoice Detail, Payment Log, Shop Settings, Billing Hub, Analytics, Expense Form

---

## Root Cause Analysis

The [test log](file:///C:/Users/devon/Projects/Phill/Development%20documentation/test_log.md) identifies 11 screens that couldn't be tested. **Every single blocked screen** traces back to exactly **two root causes**:

### Root Cause 1 — SavedStateHandle × Navigation3 Mismatch (BUG-001)

**The problem:** All 9 ViewModels that take nav parameters use `savedStateHandle.get<String>("key")` to retrieve them. This pattern works with the _stable_ Navigation Compose library, which auto-populates `SavedStateHandle` from route arguments. **Navigation3 alpha (`0.1.0-alpha04`) does NOT do this.** The `entry<Key> { key -> }` lambda in [PhillNavGraph.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/navigation/PhillNavGraph.kt) receives the typed key object — but that key is never wired into the ViewModel's `SavedStateHandle`.

**Result:** Every parameterized ViewModel reads `null` or `""` for all nav args → every detail/form screen either shows "not found" or behaves as "new" mode.

**Cascade effect confirmed:** BUG-005 (Appointment title always "New Appointment") is NOT a separate bug. The [AppointmentFormScreen](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/AppointmentFormScreen.kt#L80) already has `if (state.isEditMode) "Edit Appointment" else "New Appointment"`. The `isEditMode` flag is only set when `appointmentId != null` — but `savedStateHandle.get<String>("appointmentId")` always returns `null`, so `isEditMode` is never `true`.

Similarly, BUG-006 (validation error despite valid data) is caused by the same issue — when editing an existing appointment, the customer is loaded from the appointment record. But since `appointmentId` is `null`, `loadExisting()` is never called, and the validation state starts fresh.

**Affected ViewModels (9 total):**

| ViewModel | Parameters via SavedStateHandle | File |
|---|---|---|
| [CustomerDetailViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/CustomerDetailViewModel.kt#L41) | `customerId` | customers/ |
| [CustomerFormViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/CustomerFormViewModel.kt#L34-L35) | `customerId`, `initialPhone` | customers/ |
| [VehicleFormViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/VehicleFormViewModel.kt#L34-L35) | `customerId`, `vehicleId` | customers/ |
| [AppointmentFormViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/AppointmentFormViewModel.kt#L49-L50) | `appointmentId`, `initialCustomerId` | schedule/ |
| [JobDetailViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/jobs/JobDetailViewModel.kt#L53) | `jobId` | jobs/ |
| [InvoiceBuilderViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/InvoiceBuilderViewModel.kt#L63-L64) | `jobId`, `invoiceId` | billing/ |
| [InvoiceDetailViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/InvoiceDetailViewModel.kt#L41) | `invoiceId` | billing/ |
| [ConversationDetailViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/comms/ConversationDetailViewModel.kt#L52) | `conversationId` | comms/ |
| [ExpenseFormViewModel](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/ExpenseFormViewModel.kt#L34-L35) | `jobId`, `expenseId` | billing/ |

---

### Root Cause 2 — Missing More Hub (BUG-002 / BUG-003)

**The problem:** [PhillBottomBar.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/components/PhillBottomBar.kt#L58) defines `MORE(CustomerListKey, "More", Icons.Filled.Menu)` — the More tab navigates directly to `CustomerListKey`. There is no hub/menu screen with links to Settings, Billing, Analytics.

**Result:** 4 fully implemented screens are completely unreachable:
- Shop Settings → via [ShopSettingsKey](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/navigation/NavKeys.kt#L16)
- Billing Hub → via [BillingKey](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/navigation/NavKeys.kt#L14)
- Analytics → via [AnalyticsKey](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/navigation/NavKeys.kt#L15)
- Payment Log → via [PaymentLogKey](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/navigation/NavKeys.kt#L28)

---

## Open Questions

> [!IMPORTANT]
> **Q1: ViewModel parameter injection strategy.** The cleanest fix for Root Cause 1 is to remove `SavedStateHandle` from all 9 ViewModels and instead pass the nav key's parameters via a public `init()` method called from the screen composable. The screen composable receives the key from the `entry<Key> { key -> }` lambda and passes `key.customerId` (etc.) to the ViewModel. **Is this approach acceptable, or would you prefer to explore pre-populating SavedStateHandle via a custom `NavEntryDecorator` or `CreationExtras`?**
>
> The `init()` approach is simpler, avoids fighting the alpha API, and matches the pattern already half-present in the nav graph (the `key` parameter is captured but never used). The downside is that ViewModels become dependent on explicit initialization rather than self-initializing from SavedStateHandle.

> [!IMPORTANT]
> **Q2: More Hub design.** The anchor document and walkthrough reference a "More" menu with Customers, Billing, Analytics, Settings. Should the More Hub be:
> - **Option A:** A simple list-based menu screen (4–5 rows with icons) — fast to build, functional
> - **Option B:** A card-grid layout matching the dashboard aesthetic — more polished but more work
>
> Both options introduce a new `MoreHubKey`, `MoreHubScreen`, and `MoreHubViewModel` (or stateless screen). The bottom bar's MORE tab would navigate to `MoreHubKey` instead of `CustomerListKey`.

> [!IMPORTANT]
> **Q3: ISSUE-011 — Back arrow on Customer List.** Once the More Hub exists, the Customer List will be reached from the hub (not directly from the bottom bar). The back arrow should then navigate back to the More Hub. **Should I fix this as part of this sprint, or defer it?** It's trivial to fix alongside Root Cause 2.

---

## Proposed Changes

### Sprint 1: Fix Root Cause 1 — Nav Arg Parameter Passing

The fix pattern for each ViewModel:

1. **Remove** `SavedStateHandle` from the constructor
2. **Add** a public `fun initialize(param1: String, param2: String? = null)` method
3. **Move** the `init { }` block logic into `initialize()`, guarded by a `private var initialized = false` flag to prevent re-initialization on recomposition
4. **In the Screen composable**, call `viewModel.initialize(key.paramName)` from a `LaunchedEffect(Unit)` or directly before collecting state

Each screen composable needs its signature updated to accept the nav key's parameters and thread them to the ViewModel.

---

#### [MODIFY] [CustomerDetailViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/CustomerDetailViewModel.kt)
- Remove `savedStateHandle: SavedStateHandle` from constructor
- Add `fun initialize(customerId: String)` that sets the private `customerId` field and calls `loadCustomer()`
- Guard with `initialized` flag

#### [MODIFY] [CustomerDetailScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/CustomerDetailScreen.kt)
- Add `customerId: String` parameter
- Call `viewModel.initialize(customerId)` in `LaunchedEffect(Unit)`

#### [MODIFY] [CustomerFormViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/CustomerFormViewModel.kt)
- Remove `savedStateHandle`, add `fun initialize(customerId: String?, initialPhone: String?)`

#### [MODIFY] [CustomerFormScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/CustomerFormScreen.kt)
- Add `customerId: String?`, `initialPhone: String?` parameters, thread to ViewModel

#### [MODIFY] [VehicleFormViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/VehicleFormViewModel.kt)
- Remove `savedStateHandle`, add `fun initialize(customerId: String, vehicleId: String?)`

#### [MODIFY] [VehicleFormScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/VehicleFormScreen.kt)
- Add `customerId: String`, `vehicleId: String?` parameters, thread to ViewModel

#### [MODIFY] [AppointmentFormViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/AppointmentFormViewModel.kt)
- Remove `savedStateHandle`, add `fun initialize(appointmentId: String?, initialCustomerId: String?)`

#### [MODIFY] [AppointmentFormScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/AppointmentFormScreen.kt)
- Add `appointmentId: String?`, `initialCustomerId: String?` parameters, thread to ViewModel

#### [MODIFY] [JobDetailViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/jobs/JobDetailViewModel.kt)
- Remove `savedStateHandle`, add `fun initialize(jobId: String)`

#### [MODIFY] [JobDetailScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/jobs/JobDetailScreen.kt)
- Add `jobId: String` parameter, thread to ViewModel

#### [MODIFY] [InvoiceBuilderViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/InvoiceBuilderViewModel.kt)
- Remove `savedStateHandle`, add `fun initialize(jobId: String, invoiceId: String?)`

#### [MODIFY] [InvoiceBuilderScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/InvoiceBuilderScreen.kt)
- Add `jobId: String`, `invoiceId: String?` parameters, thread to ViewModel

#### [MODIFY] [InvoiceDetailViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/InvoiceDetailViewModel.kt)
- Remove `savedStateHandle`, add `fun initialize(invoiceId: String)`

#### [MODIFY] [InvoiceDetailScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/InvoiceDetailScreen.kt)
- Add `invoiceId: String` parameter, thread to ViewModel

#### [MODIFY] [ConversationDetailViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/comms/ConversationDetailViewModel.kt)
- Remove `savedStateHandle`, add `fun initialize(conversationId: String)`

#### [MODIFY] [ConversationDetailScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/comms/ConversationDetailScreen.kt)
- Add `conversationId: String` parameter, thread to ViewModel

#### [MODIFY] [ExpenseFormViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/ExpenseFormViewModel.kt)
- Remove `savedStateHandle`, add `fun initialize(jobId: String?, expenseId: String?)`

#### [MODIFY] [ExpenseFormScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/billing/ExpenseFormScreen.kt)
- Add `jobId: String?`, `expenseId: String?` parameters, thread to ViewModel

---

#### [MODIFY] [PhillNavGraph.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/navigation/PhillNavGraph.kt)

Update every `entry<DataClassKey> { key -> }` lambda to pass the key's parameters to the screen composable:

```kotlin
// BEFORE:
entry<CustomerDetailKey> { key ->
    CustomerDetailScreen(
        onNavigateBack = { backStack.removeLastOrNull() },
        ...
    )
}

// AFTER:
entry<CustomerDetailKey> { key ->
    CustomerDetailScreen(
        customerId = key.customerId,
        onNavigateBack = { backStack.removeLastOrNull() },
        ...
    )
}
```

This change applies to all 9 `entry<>` blocks for parameterized keys.

---

### Sprint 2: Fix Root Cause 2 — Build the More Hub

#### [NEW] [MoreHubKey](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/navigation/NavKeys.kt) (add to existing file)
- `@Serializable data object MoreHubKey : NavKey`

#### [NEW] [MoreHubScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/more/MoreHubScreen.kt)
- Stateless composable — no ViewModel needed
- Menu items: **Customers**, **Billing**, **Analytics**, **Settings**
- Each item: icon + label + chevron → calls `onNavigate(key)` lambda
- Material 3 card-based list matching the app's dark theme aesthetic

#### [MODIFY] [PhillBottomBar.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/components/PhillBottomBar.kt#L58)
- Change `MORE(CustomerListKey, "More", Icons.Filled.Menu)` → `MORE(MoreHubKey, "More", Icons.Filled.Menu)`
- Add import for `MoreHubKey`

#### [MODIFY] [PhillNavGraph.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/navigation/PhillNavGraph.kt)
- Add `entry<MoreHubKey>` that renders `MoreHubScreen` with navigation lambdas for each menu item
- Wire `onCustomers = { backStack.add(CustomerListKey) }`, `onBilling = { backStack.add(BillingKey) }`, etc.

#### [MODIFY] [CustomerListScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/CustomerListScreen.kt)
- Fix back arrow to navigate back to More Hub (this is already wired via `onNavigateBack`)

---

## Bugs Resolved by This Sprint

| Bug | Root Cause | Fix |
|---|---|---|
| **BUG-001**: Customer Detail "not found" | RC1 — SavedStateHandle | ✅ Sprint 1 |
| **BUG-002**: Settings unreachable | RC2 — Missing More Hub | ✅ Sprint 2 |
| **BUG-003**: Billing & Analytics unreachable | RC2 — Missing More Hub | ✅ Sprint 2 |
| **BUG-005**: Appointment title always "New" | RC1 — SavedStateHandle (cascade) | ✅ Sprint 1 |
| **BUG-006**: Validation error on pre-loaded data | RC1 — SavedStateHandle (cascade) | ✅ Sprint 1 |
| **ISSUE-011**: Back arrow on Customer List | RC2 — Resolved by hub structure | ✅ Sprint 2 |

## Screens Unblocked by This Sprint

| Screen | Was Blocked By | Unblocked By |
|---|---|---|
| Customer Detail | BUG-001 | Sprint 1 |
| Customer Edit Form | BUG-001 (cascade) | Sprint 1 |
| Vehicle Form | BUG-001 (cascade) | Sprint 1 |
| Job Detail | BUG-001 | Sprint 1 |
| Invoice Builder | BUG-001 | Sprint 1 |
| Invoice Detail | BUG-001 | Sprint 1 |
| Conversation Detail | BUG-001 | Sprint 1 |
| Shop Settings | BUG-002 | Sprint 2 |
| Billing Hub | BUG-003 | Sprint 2 |
| Analytics | BUG-003 | Sprint 2 |
| Expense Form | BUG-003 | Sprint 2 |

---

## Bugs NOT Addressed (Separate Root Causes)

| Bug | Reason Deferred |
|---|---|
| **BUG-004**: Duplicate appointments | Data integrity issue — requires DB investigation, not a code pattern fix |
| **BUG-007**: Dashboard shows 0 active jobs | Correct behavior — no vehicle = no job (by design per appointment form warning) |
| **BUG-008**: No vehicle linked to appointments | UX improvement — prompt to add vehicle. Separate from nav/routing bugs |
| **ISSUE-009**: Phone number formatting | Polish — separate utility function |
| **ISSUE-010**: Month view calendar grid | Feature enhancement — significant UI work |
| **ISSUE-012**: Outstanding Balance card | Likely correct — conditional render when data exists |

---

## Verification Plan

### Build Verification
1. Run `gradlew assembleDebug` — project must compile cleanly with all changes
2. Install on device via `adb install -r app-debug.apk`

### Functional Verification (Sprint 1)
1. **Customers → tap customer → Customer Detail** must show full customer info (not "Customer not found")
2. **Customer Detail → Edit** must open form pre-populated with existing data
3. **Customer Detail → Add Vehicle** must open vehicle form with correct `customerId`
4. **Schedule → tap appointment** must open form with title "Edit Appointment" and pre-filled data
5. **Jobs → tap job** must open job detail with correct job data
6. **Comms → tap conversation** must open conversation detail with messages

### Functional Verification (Sprint 2)
1. **More tab** must open a hub menu (not Customer List directly)
2. **More → Customers** must open Customer List
3. **More → Billing** must open Billing Hub
4. **More → Analytics** must open Analytics screen
5. **More → Settings** must open Shop Settings screen
6. **Customer List → back** must return to More Hub

---

## File Change Summary

| Category | Files Modified | Files Created |
|---|---|---|
| ViewModels (remove SavedStateHandle) | 9 | 0 |
| Screens (add nav params) | 9 | 1 (MoreHubScreen) |
| Navigation | 2 (NavKeys, PhillNavGraph) | 0 |
| Bottom bar | 1 (PhillBottomBar) | 0 |
| **Total** | **21** | **1** |
