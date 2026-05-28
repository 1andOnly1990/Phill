# PHILL APP — 5-ISSUE ENHANCEMENT PLAN
## Fail-Proof Instructions for Gemini AI Agent Execution

---

> **Status:** DRAFT — Awaiting Devon's review and approval
> **Scope:** 5 distinct issues, each analyzed individually, then cross-checked for integration
> **Database:** Currently at version 3. These changes will require version 4.
> **HITL Compliance:** All changes verified against anchor.md rules

---

## TABLE OF CONTENTS

1. [Issue 1: RCS via NotificationListener](#issue-1)
2. [Issue 2: Auto-Reply Character Limit + Bulletproof Business Hours](#issue-2)
3. [Issue 3: Appointment End Time + System Time Pickers for Store Hours](#issue-3)
4. [Issue 4: Estimate/Invoice/Job Relational Chain](#issue-4)
5. [Issue 5: Sending Media & Business Documents to Customers](#issue-5)
6. [Integration Cross-Check](#integration-cross-check)
7. [Database Migration Summary (v3 → v4)](#migration-summary)

---

## Issue 1: RCS via NotificationListener {#issue-1}

### THE PROBLEM

Phill Comms currently only captures SMS via `SmsReceiver` (a `BroadcastReceiver` listening for `android.provider.Telephony.SMS_RECEIVED`). RCS messages — which Google Messages uses by default for capable contacts — are completely invisible to Phill. There is no Android API equivalent to `SMS_RECEIVED` for RCS.

### THE APPROACH

Use a `NotificationListenerService` to intercept Google Messages notifications. When a notification arrives from Google Messages, extract the sender phone number and message body, then route it through the same pipeline as SMS.

### THE GOOGLE CALLER ID PROBLEM

Google Messages sometimes replaces raw phone numbers with its own "Caller ID" lookup name for unsaved numbers. Example: `864-555-3333` arrives but the notification shows "Fat Uncle Ron" as the sender title instead of the phone number. The NotificationListener sees the notification — not the raw SMS/RCS payload — so it may receive a name instead of a number.

**Solution — Phone Number Priority Chain:**
1. Check the notification's `android.people` extras — this sometimes contains a `tel:` URI with the real number
2. Check `Telephony.Sms.CONTENT_URI` for a recent SMS from the same timestamp (RCS fallback to SMS sometimes works)
3. Check the notification `subText` or `summaryText` fields — Google Messages sometimes puts the number there
4. Parse the title text — if it matches a phone number pattern (digits, dashes, parentheses, plus sign), use it as the number
5. **Last resort only:** If all above fail, use the notification title as a display name, generate a placeholder phone ID like `GOOG_<hash>`, and flag the conversation for operator review

### FILES TO CREATE/MODIFY

#### [NEW] [RcsNotificationListener.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/sms/RcsNotificationListener.kt)

New `NotificationListenerService` subclass. This is the core of the RCS capture.

**What it does:**
- Extends `NotificationListenerService`
- Filters notifications: only processes notifications from Google Messages (`com.google.android.apps.messaging`)
- Extracts sender info using the priority chain described above
- Applies the same filtering rules as `SmsReceiver`: skip short codes (< 7 digits), skip known contacts
- Routes to `CommsRepository.getOrCreateConversation()` and `saveMessage()` — same path as SMS
- Uses `@AndroidEntryPoint` for Hilt injection of `CommsRepository`, `ContactResolver`, `AutoReplyManager`
- Deduplicates against messages already received via `SmsReceiver` (checks for same body + timestamp within 10 seconds)

**Key implementation details:**
```
Package: com.phillips.phill.sms
Class: RcsNotificationListener extends NotificationListenerService
Injected deps: CommsRepository, ContactResolver, AutoReplyManager
Notification filter: packageName == "com.google.android.apps.messaging"

Phone number extraction priority:
1. extras.getParcelableArray("android.messages") → each message's "sender_person" → Person.getUri() → parse tel: URI
2. extras.getString("android.subText") → regex for phone pattern
3. extras.getString("android.text") check if notification title is a phone pattern
4. Notification.extras.getString(Notification.EXTRA_TITLE) → phone regex
5. Fallback: title as display name, generate placeholder ID

Dedup: Before saving, check if a message with the same body exists in this conversation
        within ±10 seconds of the notification's .when timestamp
```

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/devon/Projects/Phill/app/src/main/AndroidManifest.xml)

Add the NotificationListenerService declaration inside `<application>`:

```xml
<!-- RCS capture via notification listener -->
<service
    android:name=".sms.RcsNotificationListener"
    android:exported="true"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
    </intent-filter>
</service>
```

**No new `<uses-permission>` needed.** NotificationListenerService access is granted by the user in system settings, not via manifest permission.

#### [MODIFY] [ConversationEntity.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/ConversationEntity.kt)

Add a `source` column to track where the conversation originated (SMS vs RCS vs NOTIFICATION):

```kotlin
@ColumnInfo(name = "source", defaultValue = "SMS")
val source: String = "SMS"  // "SMS", "RCS", "NOTIFICATION"
```

> [!IMPORTANT]
> **This is a schema change.** It will be part of the MIGRATION_3_4. The column has a default value so existing rows are unaffected.

#### [MODIFY] UI — Permission Request Flow

The user must manually grant NotificationListener access. This is not a runtime permission dialog — it requires navigating to Android Settings. The app needs to:
1. Detect if notification access is granted (via `NotificationManagerCompat.getEnabledListenerPackages()`)
2. If not granted, show a banner/card in the Comms tab explaining what it does and a button to open `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`
3. On return, re-check access status

> [!WARNING]
> **Devon — Decision Required:** NotificationListenerService is a powerful permission. Android will show a scary system dialog saying the app can "read all notifications." This is unavoidable for RCS capture. The alternative is to only support SMS (current behavior). Do you want to proceed with the NotificationListener approach?

> [!IMPORTANT]
> **Devon — Decision Required:** Google Messages package name. The standard is `com.google.android.apps.messaging`, but Samsung devices sometimes have `com.samsung.android.messaging`. Should we filter for both, or just Google Messages? On your Galaxy A16, which messaging app do you use?

---

## Issue 2: Auto-Reply Character Limit + Bulletproof Business Hours {#issue-2}

### THE PROBLEM (Character Limit)

The auto-reply message field is currently capped at 160 characters — a single SMS segment. The AutoReplyManager already uses `smsManager.sendMultipartTextMessage()` which handles multi-segment messages. The 160 cap is artificial and lives only in the UI/ViewModel.

### THE FIX (Character Limit)

Increase the cap from 160 to 480 characters (3 SMS segments). Update the counter, warning colors, and ViewModel clamp.

> [!IMPORTANT]
> **Devon — Decision Required:** What character limit do you want? Options:
> - **480** (3 SMS segments) — enough for a paragraph, reasonable cost
> - **640** (4 SMS segments) — very generous
> - **No hard limit** — just show segment count, let operator decide
> 
> The AutoReplyManager's `sendMultipartTextMessage` handles any length, so technically no limit is needed. But each 160-char segment is a separate SMS charge on the carrier bill.

#### Files to modify for character limit:

**[ShopSettingsScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/settings/ShopSettingsScreen.kt):**
- Line 259: Change `"${state.autoReplyMessage.length}/160"` → `"${state.autoReplyMessage.length}/480"`
- Line 260: Change `state.autoReplyMessage.length > 140` → `state.autoReplyMessage.length > 440`
- Line 267: Change `state.autoReplyMessage.length >= 160` → `state.autoReplyMessage.length >= 480`
- Add segment count display: `"(${(state.autoReplyMessage.length / 160) + 1} SMS segment(s))"`

**[ShopSettingsViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/settings/ShopSettingsViewModel.kt):**
- Line 169: Change `value.take(160)` → `value.take(480)`

---

### THE PROBLEM (Business Hours)

Business hours are currently entered as free-text `OutlinedTextField` fields with the hint "24-hr HH:MM". The user can type anything — "abc", "25:99", "8am" — and the only validation is in `hhmmToMinutes()` which silently falls back to 8AM/5PM defaults. Devon says there needs to be "absolutely zero screw up possible" for setting business hours.

### THE FIX (Business Hours)

Replace the free-text `OutlinedTextField` for open/close times with the Material 3 `TimePicker` dialog — the same native Android time picker already used in the appointment scheduling screen. The user taps the time field, a clock face appears, they pick the time visually, and it's impossible to enter invalid input.

> [!NOTE]
> This is covered in **Issue 3** as well (appointment end time uses the same TimePicker pattern). The business hours fix here and the appointment fix in Issue 3 share the same approach — both replace text input with Material 3 TimePicker dialogs. They are listed under both issues since you asked to analyze each individually.

#### Files to modify for bulletproof business hours:

**[ShopSettingsScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/settings/ShopSettingsScreen.kt) — `DayHoursRow` composable (lines 300-369):**

Replace the two `OutlinedTextField` fields ("Opens" / "Closes") with **read-only tappable fields** that open a `TimePicker` dialog on tap. Exact same pattern as [AppointmentFormScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/AppointmentFormScreen.kt) lines 370-402.

The new `DayHoursRow`:
- Each time field is a read-only `OutlinedTextField` that displays the time in 12-hour format ("8:00 AM")
- Tapping either field opens a `TimePicker` dialog (Material 3)
- On confirm, the selected hour/minute is formatted back to "HH:MM" 24-hour string and passed to the ViewModel
- **Validation:** Close time must be after open time. If user picks a close time ≤ open time, show an error inline and don't save.
- No keyboard input — no way to type invalid values

**[ShopSettingsViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/settings/ShopSettingsViewModel.kt):**

`updateDayOpensAt` and `updateDayClosesAt` already accept String — no change needed. The new UI just guarantees the string is always valid "HH:MM" format.

Add validation in `save()`: for each open day, verify `closeMinutes > openMinutes`. If not, set a validation error and don't save.

---

## Issue 3: Appointment End Time + System Time Pickers for Store Hours {#issue-3}

### THE PROBLEM (Appointment End Time)

The appointment form has a start time but no end time. [AppointmentEntity](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/AppointmentEntity.kt) already has a `scheduled_end_epoch` column (nullable Long), but the form UI never sets it. It's always null.

### THE FIX (Appointment End Time)

Add a second time picker ("End Time") to [AppointmentFormScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/AppointmentFormScreen.kt). The schema already supports it — this is a **UI-only change**.

#### Files to modify:

**[AppointmentFormScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/AppointmentFormScreen.kt):**

After the existing Date/Time row (lines 241-283), add:

1. A new `OutlinedTextField` labeled "End Time" — read-only, tappable, opens a `TimePicker` dialog
2. State variables: `showEndTimePicker`, `selectedEndHour`, `selectedEndMinute`
3. A new `TimePicker` dialog (identical pattern to the existing one at lines 370-402)
4. On confirm, calculate `scheduledEndEpoch` from the selected date + end hour/minute
5. Pass to ViewModel via new method `updateScheduledEndEpoch(epoch: Long)`
6. **Validation:** End time must be after start time. If not, show error.
7. Display the duration between start and end: "⏱ 2.5 hours estimated"

**[AppointmentFormViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/AppointmentFormViewModel.kt):**

Add:
```kotlin
fun updateScheduledEndEpoch(epoch: Long) {
    _uiState.value = _uiState.value.copy(scheduledEndEpoch = epoch, validationError = null)
}
```

Add validation in `save()`: if `scheduledEndEpoch` is set and `scheduledEndEpoch <= scheduledStartEpoch`, set validation error "End time must be after start time" and return.

The `save()` method already passes `state.scheduledEndEpoch` to the entity — it just needs validation.

**[ScheduleScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/ScheduleScreen.kt):**

Update the appointment card display to show end time when present:
- Currently shows: "9:00 AM"
- Should show: "9:00 AM – 11:30 AM" when end time exists

### THE PROBLEM (Store Hours Text Input)

Same as Issue 2 — free-text "HH:MM" input for business hours is error-prone. Already addressed in Issue 2.

### CROSS-REFERENCE

The `TimePicker` dialog pattern is used in three places now:
1. Appointment start time (already exists)
2. **NEW:** Appointment end time (this issue)
3. **NEW:** Business hours open/close times (Issue 2)

All three use the exact same `TimePicker` dialog from Material 3. Consider extracting a reusable `TimePickerDialog` composable in [ui/components/](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/components/) to avoid duplication.

---

## Issue 4: Estimate/Invoice/Job Relational Chain {#issue-4}

### THE PROBLEM

Devon's business model: a customer contacts → gets an appointment → the appointment creates a job → the job gets an estimate → the estimate becomes an invoice → the invoice gets paid. Currently:

- **No estimate entity exists.** The `InvoiceEntity` has `status: InvoiceStatus` with values `ESTIMATE, INVOICE, PAID, VOID`. The "estimate" is just an invoice with status ESTIMATE.
- **Invoice → Job link exists** (`job_id` FK on InvoiceEntity)
- **Invoice → Customer link exists** (`customer_id` FK on InvoiceEntity)
- **Job → Customer + Vehicle + Appointment links exist**
- **BUT:** There's no direct link from Invoice to Vehicle, no phone number stored on invoice, no address stored on invoice.

Devon wants full traceability: Invoice → Job → Appointment → Customer → Vehicle → Phone → Address → Estimate/Work Order.

### THE ANALYSIS

The relational chain **already mostly exists** through foreign keys:

```
Invoice.job_id → Job
Invoice.customer_id → Customer
Job.vehicle_id → Vehicle
Job.appointment_id → Appointment
Customer.phone_number → Phone
Customer.address → Address / Appointment.address → Service Address
```

**What's missing for the paper trail:**
1. Invoice has no `vehicle_id` — you can get it through Job, but for record-keeping clarity and query efficiency, a direct FK would help
2. Invoice has no `appointment_id` — same rationale
3. There's no "estimate number" or "work order number" field for human-readable reference
4. InvoiceStatus.ESTIMATE works fine for the estimate concept — an estimate IS a draft invoice

### THE FIX

#### Add columns to [InvoiceEntity.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/InvoiceEntity.kt):

```kotlin
@ColumnInfo(name = "vehicle_id")
val vehicleId: String? = null,

@ColumnInfo(name = "appointment_id")
val appointmentId: String? = null,

@ColumnInfo(name = "invoice_number")
val invoiceNumber: String? = null,  // Human-readable: "EST-001", "INV-001"
```

Add foreign keys:
```kotlin
ForeignKey(entity = VehicleEntity::class, parentColumns = ["id"], childColumns = ["vehicle_id"], onDelete = ForeignKey.SET_NULL),
ForeignKey(entity = AppointmentEntity::class, parentColumns = ["id"], childColumns = ["appointment_id"], onDelete = ForeignKey.SET_NULL)
```

Add indices: `Index("vehicle_id"), Index("appointment_id")`

#### Add to [InvoiceDao.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/dao/InvoiceDao.kt):

```kotlin
@Query("SELECT * FROM invoices WHERE vehicle_id = :vehicleId ORDER BY created_at_epoch DESC")
fun observeByVehicle(vehicleId: String): Flow<List<InvoiceEntity>>

@Query("SELECT MAX(CAST(SUBSTR(invoice_number, 5) AS INTEGER)) FROM invoices WHERE invoice_number LIKE :prefix || '%'")
suspend fun getMaxNumberForPrefix(prefix: String): Int?
```

#### Add to [BillingRepository.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/repository/BillingRepository.kt):

```kotlin
suspend fun generateInvoiceNumber(isEstimate: Boolean): String {
    val prefix = if (isEstimate) "EST-" else "INV-"
    val maxNum = invoiceDao.getMaxNumberForPrefix(prefix) ?: 0
    return "$prefix${String.format("%03d", maxNum + 1)}"
}
```

#### Billing UI Changes

When creating an invoice/estimate, auto-populate `vehicleId` and `appointmentId` from the Job's linked data. The invoice creation flow is:

1. Job detail screen → "Create Estimate" or "Create Invoice" button
2. Pre-populate: `jobId`, `customerId`, `vehicleId` (from Job), `appointmentId` (from Job)
3. Auto-generate `invoiceNumber` (EST-001, INV-001)
4. Operator adds line items, reviews, saves

When viewing an invoice, the detail screen should show the full chain:
- Customer name + phone
- Vehicle (year make model)
- Appointment date/address
- Job description
- All line items
- Payment status

> [!IMPORTANT]
> **Devon — Decision Required:** Do you want the estimate → invoice transition to create a NEW record with a new INV-number, or do you want to mutate the existing record's status from ESTIMATE to INVOICE and change its number? Mutating is simpler. Creating a new record preserves the original estimate for audit trail.

---

## Issue 5: Sending Media & Business Documents to Customers {#issue-5}

### THE PROBLEM

Devon needs to send customers:
- Photos of broken parts (evidence of damage)
- Videos or audio recordings showing issues
- PDF estimates and invoices
- Any business-related file

Currently, [ConversationDetailViewModel.sendMessage()](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/comms/ConversationDetailViewModel.kt#L145-L192) only sends plain text SMS via `SmsManager.sendMultipartTextMessage()`.

### THE APPROACH

MMS (Multimedia Messaging Service) is the standard way to send images/files via the native Android messaging API. However:

1. **MMS via SmsManager** requires the app to be the **default SMS app**, which means it must handle ALL SMS/MMS for the device. That's a massive scope expansion.
2. **Sharing via Intent** — use `Intent.ACTION_SEND` / `Intent.ACTION_SEND_MULTIPLE` to open Google Messages (or the default messaging app) with the file pre-attached and the phone number pre-filled. The operator reviews and taps Send in Google Messages. **This is the HITL-compliant approach.**

### THE FIX

**Phase 1 (v1 — recommended):** Use Android's share Intent system. This is the simplest, most reliable, and HITL-compliant approach:

1. Add an "Attach" button (📎) in the conversation detail screen
2. Tapping it opens a file/image picker (via `ActivityResultContracts.GetContent` or `PickVisualMedia`)
3. After selecting a file, compose a share Intent:
   ```kotlin
   Intent(Intent.ACTION_SEND).apply {
       type = mimeType // "image/jpeg", "application/pdf", etc.
       putExtra(Intent.EXTRA_STREAM, fileUri)
       putExtra("address", phoneNumber) // Some apps honor this
       setPackage("com.google.android.apps.messaging") // Target Google Messages
   }
   ```
4. This opens Google Messages with the file attached and the conversation pre-selected
5. The operator reviews and taps Send **in Google Messages** (HITL compliant)
6. **Record the share in Phill's database:** Save an `AttachmentEntity` with the local file URI, mime type, timestamp, and conversation ID — so Phill has a record that the file was shared

#### New entity: AttachmentEntity

```kotlin
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(entity = MessageEntity::class, parentColumns = ["id"], childColumns = ["message_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ConversationEntity::class, parentColumns = ["id"], childColumns = ["conversation_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("message_id"), Index("conversation_id")]
)
data class AttachmentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "message_id")
    val messageId: String? = null,
    @ColumnInfo(name = "file_uri")
    val fileUri: String,    // content:// or file:// URI
    @ColumnInfo(name = "file_name")
    val fileName: String?,
    @ColumnInfo(name = "mime_type")
    val mimeType: String,   // "image/jpeg", "video/mp4", "application/pdf"
    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long? = null,
    @ColumnInfo(name = "shared_at_epoch")
    val sharedAtEpoch: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "thumbnail_uri")
    val thumbnailUri: String? = null  // For images/videos: path to generated thumbnail
)
```

#### Job-related file sharing

For sending estimates, invoices, or job photos:
- From the **Job Detail** screen, add a "Share with Customer" action
- From the **Invoice Detail** screen, add a "Send to Customer" action
- These create a share Intent targeted at the customer's phone number
- The shared file is also saved as an `AttachmentEntity` linked to the conversation

For **taking photos/videos on the job:**
- Add a "Camera" button on the Job Detail screen
- Uses `ActivityResultContracts.TakePicture` / `TakeVideo`
- Saves the media to app-internal storage
- Links it to the job via a `JobAttachmentEntity` or reuses `AttachmentEntity` with a `job_id` column

> [!IMPORTANT]
> **Devon — Decision Required:** For v1, do you want:
> 
> **Option A:** Share via Intent (opens Google Messages, operator sends there) — simplest, fully HITL compliant, works for all file types
> 
> **Option B:** Native MMS sending inside Phill — requires Phill to become the default SMS app, which is a massive change that means Phill must handle ALL messaging for the device
> 
> **Recommendation:** Option A for v1. Option B would be a Phase 2 effort.

> [!WARNING]
> **PDF Generation** of estimates/invoices is listed as explicitly out of scope for v1 in [anchor.md](file:///C:/Users/devon/Projects/Phill/Development%20documentation/reference/anchor.md) (line 387). The share Intent can share any file that already exists (photos, videos, audio). For PDFs, you'd need to either:
> 1. Generate PDFs in-app (out of v1 scope per anchor)
> 2. Take a screenshot of the invoice screen and share that image
> 3. Use a third-party estimate/invoice tool and share its PDF
> 
> Which approach do you prefer?

---

## Integration Cross-Check {#integration-cross-check}

### Do these 5 issues conflict with each other?

| Issue | New Entities | Modified Entities | Schema Changes | New Files |
|---|---|---|---|---|
| 1 (RCS) | — | ConversationEntity (+source) | 1 column | RcsNotificationListener.kt |
| 2 (Auto-reply) | — | — | 0 columns | — (UI-only changes) |
| 3 (End time) | — | — | 0 columns | — (UI-only, schema exists) |
| 4 (Estimate chain) | — | InvoiceEntity (+3 cols) | 3 columns | — |
| 5 (Media) | AttachmentEntity | — | 1 new table | AttachmentEntity.kt, AttachmentDao.kt |

**No conflicts.** Each issue touches different entities and different screens. The only shared concern is the database migration — all schema changes (Issues 1, 4, 5) will be combined into a single `MIGRATION_3_4`.

### Repository domain check (per CLAUDE.md rules):

- Issue 1: `CommsRepository` — ✅ within domain
- Issue 2: `OperationsRepository` (shop profile) — ✅ within domain
- Issue 3: `ScheduleRepository` — ✅ within domain
- Issue 4: `BillingRepository` — ✅ within domain
- Issue 5: `CommsRepository` + new `AttachmentDao` — ✅ attachment is comms-adjacent

### HITL compliance check:

| Issue | HITL Rule | Compliance |
|---|---|---|
| 1 (RCS) | Rule 1: no auto-send | ✅ Listener only stores messages, never sends |
| 1 (RCS) | Auto-reply | ✅ Same `AutoReplyManager` — operator-configured, rate-limited |
| 2 (Auto-reply) | Rule 1 | ✅ Auto-reply is operator-configured text, not AI-generated |
| 3 (End time) | Rule 2 | ✅ Operator picks time via system picker, confirms save |
| 4 (Estimate) | Rule 3 | ✅ All financial records require manual creation + approval |
| 5 (Media) | Rule 1 | ✅ Share Intent opens Google Messages — operator taps Send there |

---

## Database Migration Summary (v3 → v4) {#migration-summary}

All schema changes from Issues 1, 4, and 5 combined into one migration:

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Issue 1: Track conversation source (SMS vs RCS)
        db.execSQL("ALTER TABLE conversations ADD COLUMN source TEXT NOT NULL DEFAULT 'SMS'")

        // Issue 4: Link invoices directly to vehicle and appointment for paper trail
        db.execSQL("ALTER TABLE invoices ADD COLUMN vehicle_id TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE invoices ADD COLUMN appointment_id TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE invoices ADD COLUMN invoice_number TEXT DEFAULT NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_invoices_vehicle_id ON invoices(vehicle_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_invoices_appointment_id ON invoices(appointment_id)")

        // Issue 5: Attachment storage for media files shared with customers
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS attachments (
                id TEXT NOT NULL PRIMARY KEY,
                conversation_id TEXT NOT NULL,
                message_id TEXT DEFAULT NULL,
                file_uri TEXT NOT NULL,
                file_name TEXT,
                mime_type TEXT NOT NULL,
                file_size_bytes INTEGER,
                shared_at_epoch INTEGER NOT NULL,
                thumbnail_uri TEXT,
                FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
                FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_conversation_id ON attachments(conversation_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_message_id ON attachments(message_id)")
    }
}
```

**Database version bump:** 3 → 4 in [PhillDatabase.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/database/PhillDatabase.kt)

---

## Open Questions Summary

These need your answers before execution begins:

1. **Issue 1:** Do you want to proceed with NotificationListenerService for RCS? (Requires scary Android permission)
2. **Issue 1:** Which messaging app do you use on your Galaxy A16 — Google Messages only, or Samsung Messages too?
3. **Issue 2:** What auto-reply character limit? 480 (3 segments), 640 (4 segments), or unlimited?
4. **Issue 4:** Estimate → Invoice: mutate existing record, or create a new one and preserve the original?
5. **Issue 5:** Share via Intent (Option A, recommended) or native MMS (Option B, massive scope)?
6. **Issue 5:** How to handle PDF estimates/invoices? Screenshot, in-app generation (out of v1 scope per anchor), or external tool?

---

## Execution Order (Recommended)

Once approved, these should be implemented in this order to minimize risk:

1. **Issue 3** (Appointment end time) — UI-only, zero schema changes, lowest risk
2. **Issue 2** (Auto-reply fixes) — UI-only, zero schema changes, low risk
3. **Issue 4** (Invoice chain) — Schema change, moderate risk
4. **Issue 5** (Media sharing) — New table + new UI, moderate risk
5. **Issue 1** (RCS listener) — New service + permission + dedup logic, highest complexity

All schema changes (3, 4, 5 above) are batched into one MIGRATION_3_4 regardless of execution order.
