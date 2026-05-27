# Phill App — Full Spectrum Test Report
## Phillips Mobile Automotive — Shop Management Platform
### Test Date: May 27, 2026 | Device: Samsung Galaxy A16 (SM-A166U1)

---

## Test Environment
| Detail | Value |
|---|---|
| **App Package** | com.phillips.phill |
| **Device** | Samsung Galaxy A16 (SM-A166U1, 4GB RAM) |
| **Serial** | R5CY83J68ST |
| **Connection** | USB |
| **Android CLI** | v1.0.15433482 |
| **SDK** | C:\Users\devon\AppData\Local\Android\Sdk |
| **Test Method** | ADB commands + Android CLI (layout, screen capture) |

---

## 🔴 Critical Bugs

### BUG-001: Customer Detail Shows "Customer not found"
- **Severity:** 🔴 CRITICAL
- **Screen:** Customer Detail
- **Steps:** More → Customers → Tap "Ronald Locke"
- **Expected:** Customer detail screen with name, vehicles, jobs, invoices
- **Actual:** Shows "Customer not found" — blank page with only a back arrow

````carousel
![Customer list showing Ronald Locke exists](C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/05_more_menu.png)
<!-- slide -->
![Customer not found error after tapping](C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/06_customer_detail.png)
````

> [!CAUTION]
> **Root Cause Identified in Code:** [CustomerDetailViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/CustomerDetailViewModel.kt#L41) uses `savedStateHandle.get<String>("customerId")` to retrieve the customer ID. With `navigation3` alpha, `SavedStateHandle` keys may not match the data class property names from `@Serializable` nav keys. The `CustomerDetailKey(val customerId: String)` parameter likely isn't being deserialized into SavedStateHandle correctly.

**Impact:** This bug renders the **entire customer detail hub** non-functional. Users cannot view any customer's details, vehicles, jobs, or invoices from the customer list. This is the "single pane of glass" for customer relationships per the design spec.

**Scope:** This likely affects **ALL ViewModels** using `SavedStateHandle` for nav args:
- `CustomerDetailViewModel` (`customerId`)
- `CustomerFormViewModel` (`customerId`, `initialPhone`)
- `VehicleFormViewModel` (`customerId`, `vehicleId`)
- `JobDetailViewModel` (`jobId`)
- `InvoiceBuilderViewModel` (`jobId`, `invoiceId`)
- `InvoiceDetailViewModel` (`invoiceId`)
- `ConversationDetailViewModel` (`conversationId`)
- `ExpenseFormViewModel` (`jobId`, `expenseId`)
- `AppointmentFormViewModel` (`appointmentId`, `initialCustomerId`)

---

### BUG-002: Settings Screen Unreachable
- **Severity:** 🔴 CRITICAL
- **Screen:** Shop Settings
- **Steps:** There is NO path to reach Settings from the UI
- **Expected:** Settings accessible via More menu
- **Actual:** "More" tab navigates directly to CustomerListKey, no menu exists

> [!IMPORTANT]
> **Root Cause:** The [PhillBottomBar.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/components/PhillBottomBar.kt#L58) defines `MORE(CustomerListKey, "More", Icons.Filled.Menu)` — the More tab goes directly to Customers with no hub/menu. [ShopSettingsKey](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/navigation/NavKeys.kt#L16) exists in code but has zero references for navigation from any UI element. The code comment says *"Full 'More' menu (Clients, Billing, Analytics, Settings) will be built on the CustomerListScreen itself in Phase 3."*

**Impact:** Users cannot configure labor rates, service fees, parts markup, tax rates, business info, or any shop profile data.

---

### BUG-003: Billing Hub & Analytics Unreachable
- **Severity:** 🔴 CRITICAL  
- **Screens:** Billing Hub, Analytics, Payment Log
- **Steps:** No path exists from UI to reach these screens
- **Expected:** Accessible via More menu
- **Actual:** `BillingKey` and `AnalyticsKey` have nav graph entries but zero UI paths to navigate to them

**Impact:** Expense tracking, payment log viewing, and analytics overview screens are fully implemented but completely inaccessible to the user.

---

## 🟡 Moderate Bugs

### BUG-004: Duplicate Appointments Displayed
- **Severity:** 🟡 MODERATE
- **Screen:** Schedule (Week & Month views)
- **Observation:** Two identical appointments for "Ronald Locke" at the same time (3:25 PM), same address (361 Snead Rd Walhalla SC 29691), both PENDING

![Two duplicate appointments in schedule](C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/03_schedule.png)

**Possible causes:**
1. Actual duplicate records in the database (data integrity issue)
2. The appointment save flow creates duplicates on rapid taps

---

### BUG-005: Appointment Form Title Always Says "New Appointment"
- **Severity:** 🟡 MODERATE
- **Screen:** Appointment Form (edit mode)
- **Steps:** Schedule → Tap existing appointment card
- **Expected:** Title says "Edit Appointment" when editing
- **Actual:** Title always says "New Appointment" even when editing an existing appointment

![Appointment edit showing "New Appointment" title](C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/13_appointment_edit.png)

---

### BUG-006: Validation Error Shown Despite Valid Data
- **Severity:** 🟡 MODERATE
- **Screen:** Appointment Form
- **Observation:** Red text "Please select a customer" appears at bottom of form even when a customer (Ronald Locke) IS selected and displayed at the top
- **Possible cause:** Form validation state not properly updating when customer is pre-loaded

---

### BUG-007: Dashboard Shows 0 Active Jobs Despite Appointments Existing
- **Severity:** 🟡 MODERATE
- **Screen:** Dashboard
- **Observation:** Dashboard shows "0 Active Jobs" and "0 Today's Appts" while there are 2 pending appointments in the Schedule
- **Possible cause:** Per the appointment form warning ("No vehicle selected — a job will not be created"), jobs are only auto-created when a vehicle is selected. The existing appointments have no vehicle, so no jobs were created.

---

### BUG-008: No Vehicle Linked to Appointments
- **Severity:** 🟡 MODERATE
- **Screen:** Appointment Form
- **Observation:** Warning says "No vehicle selected — a job will not be created for this appointment." Customer has no vehicles added.
- **Impact:** Without a vehicle, no job is created from the appointment, rendering the appointment partially useless in the workflow chain (Appointment → Job → Invoice)

---

## 🔵 Minor Issues / UX Observations

### ISSUE-009: Phone Number Not Formatted
- **Severity:** 🔵 MINOR
- **Screen:** Customer List, Appointment Form
- **Observation:** Phone "8438105578" displayed raw without formatting
- **Expected:** Should be formatted as "(843) 810-5578" for readability

---

### ISSUE-010: Month View Shows List, Not Calendar Grid
- **Severity:** 🔵 MINOR
- **Screen:** Schedule → Month view
- **Observation:** Month view shows a flat list of appointments for the entire month rather than a traditional calendar grid with day cells
- **Expected per docs:** "Calendar grid" per Section 11.4 of the operator's manual

![Month view showing flat list](C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/10_schedule_month_view.png)

---

### ISSUE-011: Customer List "Back" Arrow Navigation
- **Severity:** 🔵 MINOR
- **Screen:** Customer List
- **Observation:** The back arrow on the Customers screen doesn't navigate anywhere meaningful — it stays on the same Customers screen. Since the "More" tab IS the Customers screen, the back arrow is confusing.

---

### ISSUE-012: No "Outstanding Balance" Card on Dashboard  
- **Severity:** 🔵 MINOR
- **Screen:** Dashboard
- **Observation:** Per the documentation (Section 11.10), the dashboard should show an "Outstanding Balance" card with "⚠️ Yellow alert with total dollars owed". This card is not visible in the current UI.
- **Note:** This may simply be because there are no unpaid invoices, so the card may only appear conditionally.

---

## ✅ What's Working Well

### Navigation & Layout
| Test | Result | Notes |
|---|---|---|
| Bottom nav renders | ✅ PASS | All 5 tabs visible with icons and labels |
| Tab switching | ✅ PASS | All tabs respond to taps and switch correctly |
| Tab highlight state | ✅ PASS | Selected tab shows pill/indicator highlight |
| Back arrow navigation | ✅ PASS | Back arrows work on detail screens |
| FAB buttons visible | ✅ PASS | + buttons render on Schedule and Customers |

### Dashboard
| Test | Result | Notes |
|---|---|---|
| Business name display | ✅ PASS | "Phillips Mobile Automotive" correctly shown |
| Date display | ✅ PASS | "Wednesday, May 27" — correct |
| Metric cards layout | ✅ PASS | 4 cards in 2×2 grid, color-coded |
| Card icons | ✅ PASS | Wrench, $, calendar, receipt icons |
| Empty state message | ✅ PASS | "No active jobs / Schedule an appointment to get started" |

![Dashboard screen](C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/02_dashboard.png)

### Schedule
| Test | Result | Notes |
|---|---|---|
| Day/Week/Month toggles | ✅ PASS | All three views switch correctly |
| Date navigation arrows | ✅ PASS | ← → arrows visible and functional |
| Appointment cards | ✅ PASS | Show time, customer, address, status |
| 15-minute buffer display | ✅ PASS | "Arrive by 3:10 PM (15 min setup)" shown |
| FAB for new appointment | ✅ PASS | Opens appointment form |
| Day view empty state | ✅ PASS | "No appointments" with calendar icon |

### Appointment Form
| Test | Result | Notes |
|---|---|---|
| Customer selection | ✅ PASS | Pre-fills from existing customer |
| "Change" customer button | ✅ PASS | Visible and accessible |
| Date/Time pickers | ✅ PASS | Material 3 date/time picker icons present |
| Service address pre-fill | ✅ PASS | Pre-fills from customer record |
| Status chips | ✅ PASS | Pending/Confirmed/Cancelled |
| Vehicle warning | ✅ PASS | Warning shows when no vehicle selected |
| Notes field | ✅ PASS | Text area visible |
| Save FAB | ✅ PASS | Floppy disk icon, properly positioned |

### Customer List
| Test | Result | Notes |
|---|---|---|
| Search bar | ✅ PASS | "Search customers" with magnifying glass |
| Customer cards | ✅ PASS | Name and phone displayed |
| Customer avatar | ✅ PASS | Blue person icon |
| Add customer FAB | ✅ PASS | + button with "Add Customer" content-desc |
| Long-press support | ✅ PASS | Cards support long-clickable interaction |

### Comms Screen
| Test | Result | Notes |
|---|---|---|
| Empty state | ✅ PASS | "No conversations / Incoming SMS will appear here" |
| Header | ✅ PASS | "Messages" |

### Visual Design Quality
| Aspect | Rating | Notes |
|---|---|---|
| Dark theme consistency | ⭐⭐⭐⭐⭐ | Beautiful dark theme throughout |
| Color palette | ⭐⭐⭐⭐ | Blue header, green revenue, purple appointments |
| Typography | ⭐⭐⭐⭐ | Clean hierarchy, readable |
| Card design | ⭐⭐⭐⭐ | Rounded corners, proper spacing |
| Bottom nav | ⭐⭐⭐⭐⭐ | Material 3 pill indicator, clean icons |
| Status badges | ⭐⭐⭐⭐ | "PENDING" in distinct color |
| Form layout | ⭐⭐⭐⭐ | Well-organized sections with headers |
| Empty states | ⭐⭐⭐⭐⭐ | Meaningful icons + text, not just blank |

---

## 📊 Code Analysis Findings

### Architecture Pattern Compliance
- ✅ Entity → DAO → Repository → ViewModel → Screen pattern followed
- ✅ 6 repositories with clean domain boundaries
- ✅ Hilt DI properly used
- ✅ Room database with 13 entities

### Navigation3 Alpha Concerns
> [!WARNING]
> The app uses `navigation3` v0.1.0-alpha04 which is experimental. The `SavedStateHandle` integration with `@Serializable` nav keys appears broken — parameters passed via data class nav keys are not being deserialized correctly into `SavedStateHandle`. This affects ALL detail/form screens that accept navigation parameters.

### Files Reviewed
| File | Status | Notes |
|---|---|---|
| [NavKeys.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/navigation/NavKeys.kt) | ✅ Well-structured | 18 nav keys, proper serialization |
| [PhillNavGraph.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/navigation/PhillNavGraph.kt) | ⚠️ Missing nav paths | Billing, Analytics, Settings entries exist but have no UI trigger |
| [PhillBottomBar.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/components/PhillBottomBar.kt) | ⚠️ Missing More menu | Goes directly to CustomerList, no hub |
| [CustomerDetailViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/customers/CustomerDetailViewModel.kt) | 🔴 Broken nav args | SavedStateHandle key mismatch |

---

## 📋 Screenshots Captured

| # | Screen | File |
|---|---|---|
| 01 | Job Queue (initial state) | [01_initial_jobs_screen.png](file:///C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/01_initial_jobs_screen.png) |
| 02 | Dashboard | [02_dashboard.png](file:///C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/02_dashboard.png) |
| 03 | Schedule (Week view) | [03_schedule.png](file:///C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/03_schedule.png) |
| 04 | Comms (empty) | [04_comms.png](file:///C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/04_comms.png) |
| 05 | Customer List | [05_more_menu.png](file:///C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/05_more_menu.png) |
| 06 | Customer Detail (BUG) | [06_customer_detail.png](file:///C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/06_customer_detail.png) |
| 07 | Customer List (after back) | [07_after_back.png](file:///C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/07_after_back.png) |
| 08 | Customer List (More hub) | [08_more_hub.png](file:///C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/08_more_hub.png) |
| 09 | Schedule (Day view) | [09_schedule_day_view.png](file:///C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/09_schedule_day_view.png) |
| 10 | Schedule (Month view) | [10_schedule_month_view.png](file:///C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/10_schedule_month_view.png) |
| 11 | Appointment Form (new) | [11_appointment_form.png](file:///C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/11_appointment_form.png) |
| 12 | Schedule Month (current) | [12_current_state.png](file:///C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/12_current_state.png) |
| 13 | Appointment Form (edit) | [13_appointment_edit.png](file:///C:/Users/devon/.gemini/antigravity/brain/0a1713a9-6bf1-4b1a-833d-768c60f79e8d/screenshots/13_appointment_edit.png) |

---

## 🎯 Recommended Priority Fixes

### P0 — Fix Before Any Other Work
1. **BUG-001**: Fix `SavedStateHandle` parameter retrieval in ALL ViewModels. Either:
   - Use `navigation3`'s proper API for type-safe args (if available in alpha04)
   - Or use `key.customerId` directly from the nav key in the `entry<>` lambda instead of SavedStateHandle
2. **BUG-002/003**: Build the "More" hub menu with links to Customers, Billing, Analytics, Settings

### P1 — Fix This Sprint
3. **BUG-004**: Investigate and clean up duplicate appointment data
4. **BUG-005**: Dynamic title for Appointment Form (new vs. edit)
5. **BUG-006**: Fix validation display when customer is pre-loaded
6. **BUG-008**: Prompt user to add a vehicle to the customer record

### P2 — Quality Polish
7. **ISSUE-009**: Add phone number formatting utility
8. **ISSUE-010**: Consider calendar grid for Month view
9. **ISSUE-011**: Remove or fix back arrow on Customer List root
10. **ISSUE-012**: Verify Outstanding Balance card appears with data

---

## Screens Not Tested (Blocked)

The following screens could not be tested due to the navigation/SavedStateHandle bug (BUG-001) or missing UI paths (BUG-002/003):

| Screen | Reason Blocked |
|---|---|
| Customer Detail (full) | BUG-001: "Customer not found" |
| Customer Edit Form | Cannot reach from detail |
| Vehicle Form | Cannot reach from detail |
| Job Detail | No jobs exist (BUG-007/008) |
| Invoice Builder | No jobs to create invoices from |
| Invoice Detail | No invoices exist |
| Payment Log | No UI path (BUG-003) |
| Shop Settings | No UI path (BUG-002) |
| Billing Hub | No UI path (BUG-003) |
| Analytics | No UI path (BUG-003) |
| Expense Form | No UI path (BUG-003) |

---

*End of Full Spectrum Test Report — May 27, 2026*
