# PHILL 5-Issue Enhancement — Implementation Plan

Based on [5issueplan.md](file:///C:/Users/devon/Projects/Phill/5issueplan.md) and verified against [peer_review5.md](file:///C:/Users/devon/Projects/Phill/peer_review5.md). All peer review corrections incorporated.

---

## Open Questions — Need Your Answers Before Execution

> [!IMPORTANT]
> These questions from the original plan remain unanswered. I've listed my **recommended defaults** — approve or override each.

| # | Question | Recommended Default |
|---|---|---|
| 1 | Proceed with NotificationListenerService for RCS? (scary Android permission) | **Yes** — only way to capture RCS |
| 2 | Which messaging app on your Galaxy A16? | **Both** — `com.google.android.apps.messaging` + `com.samsung.android.messaging` |
| 3 | Auto-reply character limit? | **480** (3 SMS segments) |
| 4 | Estimate → Invoice: mutate existing record or create new? | **Mutate** — simpler for v1, change status + number |
| 5 | Media sharing: Share via Intent (A) or native MMS (B)? | **Option A** — Intent, HITL compliant |
| 6 | PDF estimates/invoices handling? | **Defer** — out of v1 scope per [anchor.md](file:///C:/Users/devon/Projects/Phill/Development%20documentation/reference/anchor.md) L387 |

---

## Execution Order

```
Step 0 → Extract reusable TimePickerDialog composable (pre-work)
Step 1 → Issue 3: Appointment End Time (UI-only, zero schema)
Step 2 → Issue 2: Auto-Reply 480 chars + TimePicker business hours (UI-only, zero schema)
Step 3 → Issue 4: Invoice relational chain (schema change)
Step 4 → Issue 5: Media sharing via Intent + AttachmentEntity (schema + new table)
Step 5 → Issue 1: RCS NotificationListener (new service, highest complexity)
Step 6 → MIGRATION_3_4 + PhillDatabase registration (all schema batched)
```

> [!NOTE]
> Step 6 (migration) is written first but deployed alongside Steps 3–5 in a single commit. All schema changes are batched into one migration.

---

## Step 0: Extract Reusable TimePickerDialog

*Per peer review recommendation — avoids copy-paste bugs across 3 usage sites.*

### [NEW] [TimePickerDialog.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/components/TimePickerDialog.kt)

A shared composable in `ui/components/`:

```kotlin
@Composable
fun PhillTimePickerDialog(
    title: String = "Select Time",
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
)
```

- Uses Material 3 `TimePicker` + `Dialog` (same pattern as [AppointmentFormScreen.kt L370-402](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/AppointmentFormScreen.kt#L370-L402))
- Used by: Appointment start time (refactor), appointment end time (Issue 3), business hours (Issue 2)

---

## Step 1: Issue 3 — Appointment End Time

*UI-only change. Schema already has `scheduledEndEpoch` on [AppointmentEntity](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/AppointmentEntity.kt) and [AppointmentFormUiState](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/AppointmentFormViewModel.kt#L23-L39).*

### [MODIFY] [AppointmentFormScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/AppointmentFormScreen.kt)

1. **Refactor existing TimePicker** (L370-402) → use new `PhillTimePickerDialog`
2. **Add End Time field** after the Date & Time row (after L283):
   - Read-only `OutlinedTextField` labeled "End Time", tappable → opens `PhillTimePickerDialog`
   - New state: `showEndTimePicker`, `selectedEndHour`, `selectedEndMinute`
   - On confirm → calculate `scheduledEndEpoch` from selected date + end time
   - Call `viewModel.updateScheduledEndEpoch(epoch)`
3. **Duration display**: When both start and end are set, show "⏱ 2.5 hours estimated"
4. **Validation**: End time must be after start time

### [MODIFY] [AppointmentFormViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/AppointmentFormViewModel.kt)

Add method:
```kotlin
fun updateScheduledEndEpoch(epoch: Long) {
    _uiState.value = _uiState.value.copy(scheduledEndEpoch = epoch, validationError = null)
}
```

Add validation in `save()` (after L170):
```kotlin
if (state.scheduledEndEpoch != null && state.scheduledEndEpoch <= state.scheduledStartEpoch) {
    _uiState.value = state.copy(validationError = "End time must be after start time")
    return
}
```

### [MODIFY] [ScheduleScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/ScheduleScreen.kt)

Update `AppointmentCard` (L219-223) to show end time when present:
- Currently: `"9:00 AM"`
- New: `"9:00 AM – 11:30 AM"` when `scheduledEndEpoch != null`

---

## Step 2: Issue 2 — Auto-Reply 480 Chars + TimePicker Business Hours

### Character Limit Changes

#### [MODIFY] [ShopSettingsScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/settings/ShopSettingsScreen.kt)

| Line | Current | New |
|---|---|---|
| 259 | `"${state.autoReplyMessage.length}/160"` | `"${state.autoReplyMessage.length}/480"` |
| 260 | `state.autoReplyMessage.length > 140` | `state.autoReplyMessage.length > 440` |
| 267 | `state.autoReplyMessage.length >= 160` | `state.autoReplyMessage.length >= 480` |

Add segment count display inside the `supportingText` Row (between L257 and L258):
```kotlin
Text("(${(state.autoReplyMessage.length / 160) + 1} SMS)")
```

#### [MODIFY] [ShopSettingsViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/settings/ShopSettingsViewModel.kt)

- **L169**: `value.take(160)` → `value.take(480)`

### Bulletproof Business Hours — TimePicker Replacement

#### [MODIFY] [ShopSettingsScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/settings/ShopSettingsScreen.kt) — `DayHoursRow` (L300-369)

Replace the two `OutlinedTextField` fields (L347-366) with **read-only tappable fields** that open `PhillTimePickerDialog`:

- Each field displays time in 12-hour format ("8:00 AM")
- Tapping opens `PhillTimePickerDialog`
- On confirm, format back to "HH:MM" 24-hour string and call `onOpensAtChange` / `onClosesAtChange`
- **No keyboard input** — impossible to enter invalid values

#### [MODIFY] [ShopSettingsViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/settings/ShopSettingsViewModel.kt)

> [!IMPORTANT]
> **Peer review fix #1**: Add `validationError` field to `ShopSettingsUiState` (currently missing).

Add to `ShopSettingsUiState` (after L57):
```kotlin
val validationError: String? = null
```

Add validation in `save()` (inside the `viewModelScope.launch`, before building the profile):
```kotlin
// Validate close > open for each open day
for (day in state.businessHours) {
    if (!day.isOpen) continue
    val openMin = hhmmToMinutes(day.opensAt)
    val closeMin = hhmmToMinutes(day.closesAt)
    if (closeMin <= openMin) {
        _uiState.value = state.copy(
            isSaving = false,
            validationError = "${day.dayName}: close time must be after open time"
        )
        return@launch
    }
}
```

Display `validationError` in `ShopSettingsScreen` (before the FAB spacer).

---

## Step 3: Issue 4 — Invoice Relational Chain

### [MODIFY] [InvoiceEntity.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/InvoiceEntity.kt)

Add 3 columns:
```kotlin
@ColumnInfo(name = "vehicle_id")
val vehicleId: String? = null,

@ColumnInfo(name = "appointment_id")
val appointmentId: String? = null,

@ColumnInfo(name = "invoice_number")
val invoiceNumber: String? = null  // "EST-001", "INV-001"
```

Add 2 foreign keys (with `SET_NULL` — intentionally different from existing CASCADE FKs, because deleting a vehicle should not cascade-delete its invoices):
```kotlin
ForeignKey(entity = VehicleEntity::class, parentColumns = ["id"], childColumns = ["vehicle_id"], onDelete = ForeignKey.SET_NULL),
ForeignKey(entity = AppointmentEntity::class, parentColumns = ["id"], childColumns = ["appointment_id"], onDelete = ForeignKey.SET_NULL)
```

Add indices: `Index("vehicle_id"), Index("appointment_id"), Index("invoice_number")`

> [!NOTE]
> **Peer review fix #5**: `invoice_number` index added (was missing from original plan).

### [MODIFY] [InvoiceDao.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/dao/InvoiceDao.kt)

Add:
```kotlin
@Query("SELECT * FROM invoices WHERE vehicle_id = :vehicleId ORDER BY created_at_epoch DESC")
fun observeByVehicle(vehicleId: String): Flow<List<InvoiceEntity>>

@Query("SELECT MAX(CAST(SUBSTR(invoice_number, 5) AS INTEGER)) FROM invoices WHERE invoice_number LIKE :prefix || '%'")
suspend fun getMaxNumberForPrefix(prefix: String): Int?
```

### [MODIFY] [BillingRepository.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/repository/BillingRepository.kt)

Add:
```kotlin
fun observeInvoicesByVehicle(vehicleId: String): Flow<List<InvoiceEntity>> =
    invoiceDao.observeByVehicle(vehicleId)

suspend fun generateInvoiceNumber(isEstimate: Boolean): String {
    val prefix = if (isEstimate) "EST-" else "INV-"
    val maxNum = invoiceDao.getMaxNumberForPrefix(prefix) ?: 0
    return "$prefix${String.format("%03d", maxNum + 1)}"
}
```

---

## Step 4: Issue 5 — Media Sharing via Intent

### [NEW] [AttachmentEntity.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/AttachmentEntity.kt)

> [!IMPORTANT]
> **Peer review fixes applied**:
> - Fix #4: Added `job_id` column (was mentioned but missing from entity definition)
> - Fix #6: Changed `message_id` FK from `CASCADE` to `SET_NULL` (deleting a message shouldn't delete the attachment record)

```kotlin
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(entity = ConversationEntity::class, parentColumns = ["id"], childColumns = ["conversation_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MessageEntity::class, parentColumns = ["id"], childColumns = ["message_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = JobEntity::class, parentColumns = ["id"], childColumns = ["job_id"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("message_id"), Index("conversation_id"), Index("job_id")]
)
data class AttachmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "message_id") val messageId: String? = null,
    @ColumnInfo(name = "job_id") val jobId: String? = null,
    @ColumnInfo(name = "file_uri") val fileUri: String,
    @ColumnInfo(name = "file_name") val fileName: String?,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "file_size_bytes") val fileSizeBytes: Long? = null,
    @ColumnInfo(name = "shared_at_epoch") val sharedAtEpoch: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "thumbnail_uri") val thumbnailUri: String? = null
)
```

### [NEW] [AttachmentDao.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/dao/AttachmentDao.kt)

Standard CRUD + observe by conversation and by job.

### [NEW] [file_paths.xml](file:///C:/Users/devon/Projects/Phill/app/src/main/res/xml/file_paths.xml)

> [!IMPORTANT]
> **Peer review fix #3**: FileProvider is required for sharing `content://` URIs on Android 7+.

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <files-path name="shared_media" path="shared_media/" />
    <cache-path name="shared_cache" path="shared_media/" />
</paths>
```

### [MODIFY] [AndroidManifest.xml](file:///C:/Users/devon/Projects/Phill/app/src/main/AndroidManifest.xml)

Add inside `<application>`:
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### [MODIFY] [ConversationDetailScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/comms/ConversationDetailScreen.kt)

Add an **Attach button** (📎) next to the Send button in the bottom bar (L105-133):
- Tapping opens `ActivityResultContracts.GetContent("*/*")`
- After selecting, composes `Intent.ACTION_SEND` targeted at Google Messages with `EXTRA_STREAM` (content:// URI via FileProvider) and `address` extra
- Records the share as an `AttachmentEntity`

### [MODIFY] [ConversationDetailViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/comms/ConversationDetailViewModel.kt)

Add method to record attachment sharing and create share Intent data.

---

## Step 5: Issue 1 — RCS via NotificationListener

### [NEW] [RcsNotificationListener.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/sms/RcsNotificationListener.kt)

> [!IMPORTANT]
> **Peer review fix #7**: Uses `@EntryPoint` + `EntryPointAccessors.fromApplication()` instead of `@AndroidEntryPoint` for safer Hilt injection in `NotificationListenerService`.

Key design:
- Extends `NotificationListenerService`
- Filters: only `com.google.android.apps.messaging` and `com.samsung.android.messaging`
- Phone number extraction priority chain (5 steps from plan)
- Same filtering as `SmsReceiver`: skip short codes, skip known contacts
- Deduplication: check for same body within ±10 seconds
- Routes to `CommsRepository.getOrCreateConversation()` + `saveMessage()`
- Triggers `AutoReplyManager.maybeAutoReply()`
- Flags `GOOG_<hash>` fallback conversations with `needsReview = true` on ConversationEntity

### [MODIFY] [ConversationEntity.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/ConversationEntity.kt)

Add:
```kotlin
@ColumnInfo(name = "source", defaultValue = "SMS")
val source: String = "SMS",  // "SMS", "RCS", "NOTIFICATION"

@ColumnInfo(name = "needs_review", defaultValue = "0")
val needsReview: Boolean = false  // True for GOOG_<hash> fallback conversations
```

> [!NOTE]
> **Peer review fix #8**: `needsReview` field added for operator review flagging of unresolvable sender numbers.

### [MODIFY] [AndroidManifest.xml](file:///C:/Users/devon/Projects/Phill/app/src/main/AndroidManifest.xml)

Add inside `<application>`:
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

### UI — Permission Request Banner

In the Comms tab, detect if notification access is granted via `NotificationManagerCompat.getEnabledListenerPackages()`. If not, show a banner explaining what it does + button to open `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.

---

## Step 6: Database Migration (v3 → v4)

### [MODIFY] [PhillDatabase.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/database/PhillDatabase.kt)

> [!IMPORTANT]
> **Peer review fix #2**: Add `AttachmentEntity::class` to entities array + `abstract fun attachmentDao(): AttachmentDao`.

1. **Version**: `3` → `4`
2. **Entities array**: Add `AttachmentEntity::class`
3. **DAO abstract**: Add `abstract fun attachmentDao(): AttachmentDao`
4. **Import**: Add `AttachmentEntity`, `AttachmentDao`
5. **Migration**:

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Issue 1: Conversation source + review flag
        db.execSQL("ALTER TABLE conversations ADD COLUMN source TEXT NOT NULL DEFAULT 'SMS'")
        db.execSQL("ALTER TABLE conversations ADD COLUMN needs_review INTEGER NOT NULL DEFAULT 0")

        // Issue 4: Invoice relational chain
        db.execSQL("ALTER TABLE invoices ADD COLUMN vehicle_id TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE invoices ADD COLUMN appointment_id TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE invoices ADD COLUMN invoice_number TEXT DEFAULT NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_invoices_vehicle_id ON invoices(vehicle_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_invoices_appointment_id ON invoices(appointment_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_invoices_invoice_number ON invoices(invoice_number)")

        // Issue 5: Attachments table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS attachments (
                id TEXT NOT NULL PRIMARY KEY,
                conversation_id TEXT NOT NULL,
                message_id TEXT DEFAULT NULL,
                job_id TEXT DEFAULT NULL,
                file_uri TEXT NOT NULL,
                file_name TEXT,
                mime_type TEXT NOT NULL,
                file_size_bytes INTEGER,
                shared_at_epoch INTEGER NOT NULL,
                thumbnail_uri TEXT,
                FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
                FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE SET NULL,
                FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE SET NULL
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_conversation_id ON attachments(conversation_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_message_id ON attachments(message_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_job_id ON attachments(job_id)")
    }
}
```

---

## Files Summary

| Step | Action | File |
|---|---|---|
| 0 | NEW | [TimePickerDialog.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/components/TimePickerDialog.kt) |
| 1 | MODIFY | [AppointmentFormScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/AppointmentFormScreen.kt) |
| 1 | MODIFY | [AppointmentFormViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/AppointmentFormViewModel.kt) |
| 1 | MODIFY | [ScheduleScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/schedule/ScheduleScreen.kt) |
| 2 | MODIFY | [ShopSettingsScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/settings/ShopSettingsScreen.kt) |
| 2 | MODIFY | [ShopSettingsViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/settings/ShopSettingsViewModel.kt) |
| 3 | MODIFY | [InvoiceEntity.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/InvoiceEntity.kt) |
| 3 | MODIFY | [InvoiceDao.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/dao/InvoiceDao.kt) |
| 3 | MODIFY | [BillingRepository.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/repository/BillingRepository.kt) |
| 4 | NEW | [AttachmentEntity.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/AttachmentEntity.kt) |
| 4 | NEW | [AttachmentDao.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/dao/AttachmentDao.kt) |
| 4 | NEW | [file_paths.xml](file:///C:/Users/devon/Projects/Phill/app/src/main/res/xml/file_paths.xml) |
| 4 | MODIFY | [AndroidManifest.xml](file:///C:/Users/devon/Projects/Phill/app/src/main/AndroidManifest.xml) |
| 4 | MODIFY | [ConversationDetailScreen.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/comms/ConversationDetailScreen.kt) |
| 4 | MODIFY | [ConversationDetailViewModel.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/ui/comms/ConversationDetailViewModel.kt) |
| 5 | NEW | [RcsNotificationListener.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/sms/RcsNotificationListener.kt) |
| 5 | MODIFY | [ConversationEntity.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/entity/ConversationEntity.kt) |
| 5 | MODIFY | [AndroidManifest.xml](file:///C:/Users/devon/Projects/Phill/app/src/main/AndroidManifest.xml) |
| 6 | MODIFY | [PhillDatabase.kt](file:///C:/Users/devon/Projects/Phill/app/src/main/java/com/phillips/phill/data/database/PhillDatabase.kt) |

---

## Verification Plan

### Build Check
```
cd C:\Users\devon\Projects\Phill && .\gradlew assembleDebug
```

### Manual Verification
- **Step 0-1**: Open appointment form → verify start and end time pickers work, duration displays, validation blocks save when end ≤ start
- **Step 2**: Open shop settings → verify 480 char limit, segment counter, TimePicker for business hours, close > open validation
- **Step 3**: Create invoice from job → verify vehicle_id/appointment_id populated, invoice number auto-generated
- **Step 4**: Open conversation → tap 📎 → select image → verify share Intent opens Google Messages with file
- **Step 5**: Send an RCS message to test device → verify it appears in Phill Comms tab
- **Migration**: Verify app launches cleanly on existing v3 database
