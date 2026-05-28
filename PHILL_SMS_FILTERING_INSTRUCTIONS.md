# PHILL APP — SMS FILTERING & CONTACT DISPLAY FIX
## Fail-Proof Instructions for Gemini — DO NOT THINK, JUST EXECUTE

---

## WHAT YOU ARE FIXING AND WHY

**Problem 1 — Contact names not shown:**
All conversations in Phill Comms show raw phone numbers (e.g. "8649033117") instead of names. SmsSyncManager never looks up Android Contacts when importing threads. ConversationEntity already has a `displayName` column and the list screen already uses it — the lookup code just never runs.

**Problem 2 — No filtering:**
SmsSyncManager imports ALL SMS threads with no filtering. This pulls in:
- Personal saved contacts (Amanda Thompson, Janie James, Stephanie Cook, etc.) — these are personal, not business leads
- Short codes (69534, 778273) — these are spam, marketing, or verification codes, never real customers
- All other personal messages

**Intended design:**
Phill Comms is ONLY for unknown/unsaved phone numbers who are potential business leads. Personal contacts and short codes must be excluded.

**The fix adds:**
1. A new `ContactResolver` class that queries Android Contacts to look up names and check if a number is saved
2. Filter logic in `SmsSyncManager` to skip short codes and saved contacts on bulk sync
3. Filter logic in `SmsReceiver` to skip short codes and saved contacts on real-time inbound
4. A database cleanup query to remove conversations that were already imported incorrectly

---

## COMPLETE LIST OF FILES TO CHANGE

**1 NEW FILE (create from scratch):**
1. `Phill/app/src/main/java/com/phillips/phill/sms/ContactResolver.kt`

**2 EXISTING FILES TO MODIFY:**
2. `Phill/app/src/main/java/com/phillips/phill/sms/SmsSyncManager.kt`
3. `Phill/app/src/main/java/com/phillips/phill/sms/SmsReceiver.kt`

**AndroidManifest.xml — NO CHANGES NEEDED.** `READ_CONTACTS` permission is already declared.

---

## STEP 1 — Create NEW FILE: ContactResolver.kt

**Create this file at the exact path:**
`Phill/app/src/main/java/com/phillips/phill/sms/ContactResolver.kt`

**The complete file content is:**
```kotlin
package com.phillips.phill.sms

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queries the Android system Contacts database to:
 *   1. Check whether a phone number belongs to a saved contact (isKnownContact)
 *   2. Look up the display name for a phone number (getDisplayName)
 *
 * Uses ContactsContract.PhoneLookup which handles all phone number format variants
 * (with/without country code, with/without dashes, etc.) natively.
 *
 * READ_CONTACTS permission is already declared in AndroidManifest.xml.
 */
@Singleton
class ContactResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Returns true if this phone number is saved in the device's contacts.
     * Saved contacts are personal — they should be EXCLUDED from Phill Comms.
     *
     * Uses ContactsContract.PhoneLookup which does fuzzy number matching
     * (handles +1 prefix, dashes, spaces, etc.).
     */
    fun isKnownContact(phone: String): Boolean {
        if (phone.isBlank()) return false
        return getDisplayName(phone) != null
    }

    /**
     * Returns the contact display name for this phone number, or null if not found.
     * Returns null for short codes — they should never be in contacts but guard anyway.
     */
    fun getDisplayName(phone: String): String? {
        if (phone.isBlank()) return null

        val lookupUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phone)
        )

        val cursor = try {
            context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )
        } catch (e: Exception) {
            return null // ContentResolver failure — treat as unknown
        }

        return cursor?.use {
            if (it.moveToFirst()) {
                it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            } else {
                null
            }
        }
    }
}
```

**Save the file.**

---

## STEP 2 — Replace SmsSyncManager.kt entirely

**File to edit:** `Phill/app/src/main/java/com/phillips/phill/sms/SmsSyncManager.kt`

**DELETE the entire current content of this file and replace it with:**

```kotlin
package com.phillips.phill.sms

import android.content.Context
import android.provider.Telephony
import com.phillips.phill.data.entity.ConversationEntity
import com.phillips.phill.data.entity.MessageEntity
import com.phillips.phill.data.repository.CommsRepository
import com.phillips.phill.domain.enums.MessageStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val commsRepository: CommsRepository,
    private val contactResolver: ContactResolver
) {
    companion object {
        /**
         * Minimum digit count for a real phone number.
         * Anything with fewer than 7 digits is a short code (spam, verification, marketing).
         * Examples of short codes: "69534" (5 digits), "778273" (6 digits).
         * Real US numbers have 10 digits; international has more.
         */
        private const val MIN_REAL_PHONE_DIGITS = 7
    }

    /**
     * Reads SMS threads from the device's native content provider and imports BUSINESS-RELEVANT
     * threads into the Phill database.
     *
     * FILTERING RULES (a thread is skipped if ANY of these are true):
     *   1. The address normalizes to fewer than 7 digits → short code (spam/verification/marketing)
     *   2. The phone number is found in Android Contacts → personal contact, not a business lead
     *
     * Only unknown numbers (potential new customers who have not been saved in contacts) are imported.
     *
     * Call this once after SMS permission is granted to bootstrap the Comms tab.
     */
    suspend fun syncAllSmsThreads() = withContext(Dispatchers.IO) {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        val cursor = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} ASC"
        ) ?: return@withContext

        data class RawSms(
            val address: String,
            val body: String,
            val date: Long,
            val isInbound: Boolean
        )

        val allMessages = mutableListOf<RawSms>()
        cursor.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
            val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)

            while (it.moveToNext()) {
                val address = it.getString(addressIndex)?.trim() ?: continue
                val body = it.getString(bodyIndex) ?: continue
                val date = it.getLong(dateIndex)
                val type = it.getInt(typeIndex)
                val isInbound = type == Telephony.Sms.MESSAGE_TYPE_INBOX
                allMessages.add(RawSms(address, body, date, isInbound))
            }
        }

        // Group by normalized phone number (digits only, last 10 for 10-digit numbers)
        val grouped = allMessages.groupBy { it.address.filter { c -> c.isDigit() }.takeLast(10) }

        for ((normalizedPhone, messages) in grouped) {
            if (messages.isEmpty()) continue

            // --- FILTER 1: Short code check ---
            // If normalized number has fewer than MIN_REAL_PHONE_DIGITS digits, skip.
            // Short codes are 5–6 digit numbers used by spam, marketing, and 2FA services.
            if (normalizedPhone.length < MIN_REAL_PHONE_DIGITS) continue

            // --- FILTER 2: Known personal contact check ---
            // If this number is saved in the device contacts, it is a personal contact.
            // Skip it — Phill Comms is only for unknown potential customers.
            val rawAddress = messages.first().address
            if (contactResolver.isKnownContact(rawAddress)) continue

            // --- PASSED FILTERS: Import this conversation as a potential lead ---
            val conversation = commsRepository.getOrCreateConversation(normalizedPhone)

            val existingMessages = commsRepository.getMessagesByConversationIdSync(conversation.id)
            var latestEpoch = conversation.lastMessageEpoch ?: 0L

            for (sms in messages) {
                val exists = existingMessages.any { existing ->
                    existing.body == sms.body && Math.abs(existing.timestampEpoch - sms.date) < 5000
                }

                if (!exists) {
                    val message = MessageEntity(
                        conversationId = conversation.id,
                        body = sms.body,
                        timestampEpoch = sms.date,
                        isInbound = sms.isInbound,
                        status = if (sms.isInbound) MessageStatus.RECEIVED else MessageStatus.SENT
                    )
                    commsRepository.saveMessage(message)
                }

                if (sms.date > latestEpoch) {
                    latestEpoch = sms.date
                }
            }

            if (latestEpoch > (conversation.lastMessageEpoch ?: 0L)) {
                commsRepository.saveConversation(
                    conversation.copy(lastMessageEpoch = latestEpoch)
                )
            }
        }
    }

    /**
     * Syncs SMS messages for a specific conversation by querying the native device database.
     * Used to refresh a single thread without a full re-sync.
     * No additional filtering here — the conversation already passed filters when created.
     */
    suspend fun syncConversation(conversationId: String) = withContext(Dispatchers.IO) {
        val conversation = commsRepository.getConversationById(conversationId) ?: return@withContext
        val phoneNumber = conversation.phoneNumber

        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        val selection = "${Telephony.Sms.ADDRESS} LIKE ?"
        val selectionArgs = arrayOf("%${phoneNumber.filter { it.isDigit() }.takeLast(10)}")

        val cursor = context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} ASC"
        )

        cursor?.use {
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
            val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)

            val existingMessages = commsRepository.getMessagesByConversationIdSync(conversationId)

            while (it.moveToNext()) {
                val body = it.getString(bodyIndex) ?: continue
                val date = it.getLong(dateIndex)
                val type = it.getInt(typeIndex)
                val isInbound = type == Telephony.Sms.MESSAGE_TYPE_INBOX

                val exists = existingMessages.any { existing ->
                    existing.body == body && Math.abs(existing.timestampEpoch - date) < 5000
                }

                if (!exists) {
                    val message = MessageEntity(
                        conversationId = conversation.id,
                        body = body,
                        timestampEpoch = date,
                        isInbound = isInbound,
                        status = if (isInbound) MessageStatus.RECEIVED else MessageStatus.SENT
                    )
                    commsRepository.saveMessage(message)
                }
            }
        }
    }
}
```

**Save the file.**

---

## STEP 3 — Replace SmsReceiver.kt entirely

**IMPORTANT: Read this before editing.**
If you previously added `autoReplyManager` injection to SmsReceiver as part of the auto-reply feature, that code is included in the replacement below. Do not worry — this replacement includes the auto-reply wiring. If auto-reply was NOT yet added, this file will add it for the first time. Either way, replace the whole file with the content below.

**File to edit:** `Phill/app/src/main/java/com/phillips/phill/sms/SmsReceiver.kt`

**DELETE the entire current content of this file and replace it with:**

```kotlin
package com.phillips.phill.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.phillips.phill.data.entity.MessageEntity
import com.phillips.phill.data.repository.CommsRepository
import com.phillips.phill.domain.enums.MessageStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives inbound SMS messages and stores them in the local database.
 *
 * FILTERING RULES (message is ignored if ANY of these are true):
 *   1. Sender normalizes to fewer than 7 digits → short code (spam/verification/marketing)
 *   2. Sender is found in Android Contacts → personal contact, not a business lead
 *
 * Only messages from unknown numbers (potential new customers) reach the Phill Comms tab.
 *
 * Per HITL Rule 1: This ONLY stores the message. No autonomous responses except the
 * configured auto-reply which is operator-defined and rate-limited.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var commsRepository: CommsRepository

    @Inject
    lateinit var contactResolver: ContactResolver

    @Inject
    lateinit var autoReplyManager: AutoReplyManager

    companion object {
        private const val MIN_REAL_PHONE_DIGITS = 7
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val grouped = messages.groupBy { it.displayOriginatingAddress ?: "unknown" }

            for ((sender, parts) in grouped) {
                val body = parts.joinToString("") { it.displayMessageBody ?: "" }
                if (body.isBlank()) continue

                // --- FILTER 1: Short code check ---
                val normalizedSender = sender.filter { it.isDigit() }.takeLast(10)
                if (normalizedSender.length < MIN_REAL_PHONE_DIGITS) continue

                // --- FILTER 2: Known personal contact check ---
                if (contactResolver.isKnownContact(sender)) continue

                // --- PASSED FILTERS: Store as potential business lead ---
                val conversation = commsRepository.getOrCreateConversation(normalizedSender)

                val message = MessageEntity(
                    conversationId = conversation.id,
                    body = body,
                    timestampEpoch = System.currentTimeMillis(),
                    isInbound = true,
                    status = MessageStatus.RECEIVED
                )
                commsRepository.saveMessage(message)

                commsRepository.saveConversation(
                    conversation.copy(
                        lastMessageEpoch = System.currentTimeMillis(),
                        unreadCount = conversation.unreadCount + 1
                    )
                )

                // Check if an auto-reply should be sent (after hours, rate-limited)
                launch { autoReplyManager.maybeAutoReply(conversation.id) }
            }
        }
    }
}
```

**Save the file.**

---

## STEP 4 — Clean up bad data already in the database

The current database has conversations that were imported before filtering existed — short codes and personal contacts. These need to be deleted so they don't keep appearing in the Comms list.

### 4A — Wipe the database and re-sync (RECOMMENDED — fastest and cleanest)

This clears all SMS conversations and messages from Phill's database, then forces a fresh filtered re-sync on next app launch. No customer records, jobs, invoices, or settings are touched.

Run this ADB command while the app is NOT running:
```
adb shell run-as com.phillips.phill sqlite3 /data/data/com.phillips.phill/databases/phill.db "DELETE FROM messages; DELETE FROM conversations;"
```

Then launch the app. The SmsSyncManager will re-run and this time only import filtered conversations.

**Verify it ran:** After running, confirm the tables are empty:
```
adb shell run-as com.phillips.phill sqlite3 /data/data/com.phillips.phill/databases/phill.db "SELECT COUNT(*) FROM conversations;"
```
Expected output: `0`

### 4B — Check if SmsSyncManager is triggered on app start

SmsSyncManager.syncAllSmsThreads() must be called after the app launches to re-populate. Check if there is a call to it in `MainActivity.kt` or the app's ViewModel that initializes on startup.

Run:
```
adb shell run-as com.phillips.phill sqlite3 /data/data/com.phillips.phill/databases/phill.db "SELECT COUNT(*) FROM conversations;"
```

If after launching the app the count is still 0, the sync was not triggered. In that case, look for a "Sync" button or permission dialog in the app and tap it, OR check MainActivity.kt and find where `SmsSyncManager.syncAllSmsThreads()` is called and confirm it runs.

---

## STEP 5 — Build and verify

### 5A — Sync and build
In Android Studio: File → Sync Project with Gradle Files. Wait for sync to complete.
Then Build → Make Project (Ctrl+F9 / Cmd+F9).

**The build must succeed with zero errors before proceeding.**

### 5B — Install on device
```
adb install -r path/to/app-debug.apk
```
Or use Run → Run 'app' in Android Studio.

### 5C — Grant contacts permission at runtime
On first launch after install, if a permission dialog appears asking for Contacts access, tap "Allow." The READ_CONTACTS permission is already in AndroidManifest but Android still requires a runtime grant on Android 6+.

To check current permission state:
```
adb shell dumpsys package com.phillips.phill | grep READ_CONTACTS
```
Expected output contains: `READ_CONTACTS: granted=true`

If not granted, run:
```
adb shell pm grant com.phillips.phill android.permission.READ_CONTACTS
```

### 5D — Verification checklist

Open the Phill app → tap Comms tab. Check the following:

**✅ Short codes are gone:**
Numbers like "69534" and "778273" (5–6 digit numbers) must NOT appear in the list.

**✅ Personal contacts are gone:**
Numbers that are saved in the phone's contacts (visible in Google Messages with real names) must NOT appear in Phill Comms. Open Google Messages and compare — any number showing a contact name in Google Messages should NOT be in Phill Comms.

**✅ Only unknown numbers remain:**
The conversations showing in Phill Comms should only be phone numbers that are NOT saved in the device's contacts.

**✅ New inbound test:**
From a phone number that is NOT saved in the device's contacts, send an SMS to the device. After a few seconds, it should appear as a new conversation in Phill Comms.

**✅ Personal contact inbound test:**
From a phone number that IS saved in the device's contacts (like your own personal phone), send an SMS to the device. It should NOT appear in Phill Comms. It will still appear in Google Messages normally — Phill just ignores it.

---

## TROUBLESHOOTING

**Build error: "Unresolved reference: ContactResolver" in SmsSyncManager or SmsReceiver**
The ContactResolver.kt file was not created, or was saved in the wrong location. Verify the file is at exactly: `Phill/app/src/main/java/com/phillips/phill/sms/ContactResolver.kt` and that the first line reads `package com.phillips.phill.sms`

**Build error: "Unresolved reference: AutoReplyManager" in SmsReceiver**
The AutoReplyManager.kt file from the auto-reply feature has not been created yet. Either apply the auto-reply instructions first, OR temporarily remove the `autoReplyManager` injection from SmsReceiver by deleting these 3 lines:
```kotlin
    @Inject
    lateinit var autoReplyManager: AutoReplyManager
```
and this line:
```kotlin
                launch { autoReplyManager.maybeAutoReply(conversation.id) }
```

**Contacts permission not granted at runtime:**
Run `adb shell pm grant com.phillips.phill android.permission.READ_CONTACTS` and restart the app.

**All conversations still showing after clean + re-sync:**
The data clear command may not have run. Verify with `SELECT COUNT(*) FROM conversations;` — should return 0 before launch. If still showing old data, use ADB to kill the app first: `adb shell am force-stop com.phillips.phill`, then run the DELETE commands, then launch.

**Some personal contacts still showing:**
ContactsContract.PhoneLookup handles most number format variants, but in rare cases a number stored in Contacts with a non-standard format (e.g. "+1 (864) 903-3117" with spaces and parentheses) might not match the raw SMS address. This is uncommon. If it happens for a specific number, manually delete that conversation row:
```
adb shell run-as com.phillips.phill sqlite3 /data/data/com.phillips.phill/databases/phill.db "DELETE FROM messages WHERE conversation_id IN (SELECT id FROM conversations WHERE phone_number = '8649033117'); DELETE FROM conversations WHERE phone_number = '8649033117';"
```
Replace `8649033117` with the actual number.

**App crashes with NullPointerException in ContactResolver:**
The ContentResolver query failed silently. Check logcat: `adb logcat -s ContactResolver`. The catch block in getDisplayName() prevents crashes — if you still see a crash, verify the READ_CONTACTS permission is granted (see Step 5C above).
