package com.phillips.phill.sms

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.phillips.phill.data.entity.MessageEntity
import com.phillips.phill.data.repository.CommsRepository
import com.phillips.phill.domain.enums.MessageStatus
import com.phillips.phill.util.PhoneNumberUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import kotlin.math.abs

/**
 * Captures inbound RCS messages by observing notifications from messaging apps.
 *
 * Android does not provide a broadcast intent for RCS like it does for SMS.
 * The only reliable way to detect RCS messages is via NotificationListenerService,
 * which reads notification content from Google Messages and Samsung Messages.
 *
 * ## Permission
 * Requires the user to grant Notification Access in system settings
 * (Settings → Notifications → Notification access → Phill).
 * This is prompted via a banner in the Comms tab.
 *
 * ## HITL Compliance
 * Per HITL Rule 1: this service ONLY stores inbound messages. It never sends
 * messages autonomously. The only automated response is the operator-configured
 * auto-reply (same as SmsReceiver), which is rate-limited and template-based.
 *
 * ## Deduplication
 * SMS messages arrive via both SmsReceiver AND as notifications. To prevent
 * duplicates, we check if the same body already exists in the conversation
 * within ±10 seconds of the current timestamp.
 *
 * ## Phone Number Extraction
 * Notification extras are parsed using a 5-step priority chain:
 *   1. android.messages → sender_person → tel: URI
 *   2. android.subText for phone number pattern
 *   3. Notification EXTRA_TITLE for phone number pattern
 *   4. Title matches phone pattern → use as number
 *   5. LAST RESORT: generate GOOG_<hash>, flag needsReview=true
 *
 * ## Google Caller ID Resolution
 * When Google Messages replaces a phone number with a caller-ID name (common
 * with RCS), Step 5 fires and we store a GOOG_<hash> conversation. On every
 * inbound message we attempt to resolve the GOOG_ conversation to a real phone
 * by checking:
 *   a) The notification subText (Google Messages sometimes puts the real number there)
 *   b) The contact resolver (Android contacts may map the display name to a number)
 *   c) SMS that arrived from the same person (SmsReceiver stores the real phone)
 * If resolved, the GOOG_ conversation is merged into the real-phone conversation.
 *
 * Uses @EntryPoint for Hilt injection (per peer review fix #7 — safer than
 * @AndroidEntryPoint for NotificationListenerService lifecycle).
 */
class RcsNotificationListener : NotificationListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RcsListenerEntryPoint {
        fun commsRepository(): CommsRepository
        fun contactResolver(): ContactResolver
        fun autoReplyManager(): AutoReplyManager
    }

    companion object {
        private const val TAG = "PhillRcsListener"
        private const val DEDUP_WINDOW_MS = 10_000L // ±10 seconds

        /** Messaging app package names to monitor. */
        private val MONITORED_PACKAGES = setOf(
            "com.google.android.apps.messaging",  // Google Messages
            "com.samsung.android.messaging",        // Samsung Messages
            "com.google.android.dialer"              // Google Dialer (Caller ID)
        )

        /** Matches strings that look like phone numbers. */
        private val PHONE_PATTERN = Regex("^[+]?[\\d\\s\\-().]{7,}$")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        Log.d(TAG, "onNotificationPosted: pkg=${sbn.packageName} tag=${sbn.tag} id=${sbn.id}")

        // Filter: only monitor known messaging apps
        if (sbn.packageName !in MONITORED_PACKAGES) {
            Log.v(TAG, "Skipping non-monitored package: ${sbn.packageName}")
            return
        }

        Log.i(TAG, "Processing notification from ${sbn.packageName}")

        // For Google Dialer (Caller ID), log the notification details but don't store as message
        if (sbn.packageName == "com.google.android.dialer") {
            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            val subText = extras.getCharSequence("android.subText")?.toString()
            Log.i(TAG, "CALLER_ID: title=$title text=$text subText=$subText tag=${sbn.tag}")
            // TODO: Could store caller ID info in a separate table for enrichment
            return
        }

        val extras = sbn.notification?.extras ?: return
        val body = extractBody(extras)
        if (body.isNullOrBlank()) {
            Log.d(TAG, "Empty body, skipping")
            return
        }

        // Android 15+ (API 35) marks messaging notifications as "sensitive content",
        // redacting the body and extras from NotificationListenerService.
        // When this happens, the body is replaced with system text like
        // "Sensitive notification content hidden" or "Messages is doing work in the background".
        // Skip these — the SmsReceiver handles SMS directly; this listener is only
        // needed for genuine RCS messages with readable content.
        val redactedPatterns = listOf(
            "Sensitive notification content hidden",
            "Messages is doing work in the background",
            "Checking messages"
        )
        if (redactedPatterns.any { body.contains(it, ignoreCase = true) }) {
            Log.d(TAG, "Skipping redacted/system notification: ${body.take(60)}")
            return
        }

        Log.d(TAG, "Message body: ${body.take(50)}...")

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext, RcsListenerEntryPoint::class.java
        )

        val commsRepository = entryPoint.commsRepository()
        val contactResolver = entryPoint.contactResolver()
        val autoReplyManager = entryPoint.autoReplyManager()

        CoroutineScope(Dispatchers.IO).launch {
            // Extract phone number using priority chain
            val extractionResult = extractPhoneNumber(extras, sbn)
            val rawSender = extractionResult.first
            val displayName = extractionResult.second
            val isHashFallback = extractionResult.third

            Log.i(TAG, "Extracted: sender=$rawSender displayName=$displayName isHash=$isHashFallback")

            // Normalize using centralized PhoneNumberUtils
            val normalizedSender = PhoneNumberUtils.normalize(rawSender)

            // Filter 1: Short code / toll-free check (PhoneNumberUtils handles GOOG_ correctly)
            if (!isHashFallback && PhoneNumberUtils.isTollFreeOrShortCode(rawSender)) {
                Log.d(TAG, "Filtered: short code/toll-free ($normalizedSender)")
                return@launch
            }

            // Filter 2: Known personal contact
            if (!isHashFallback && contactResolver.isKnownContact(rawSender)) {
                Log.d(TAG, "Filtered: known contact ($rawSender)")
                return@launch
            }

            // ── Google Caller ID Resolution ──────────────────────────────
            // If this is a GOOG_ hash (Google replaced the phone number with a name),
            // attempt to resolve back to a real phone number.
            var resolvedPhone: String? = null
            if (isHashFallback && displayName != null) {
                resolvedPhone = tryResolveGoogleCallerId(
                    extras, displayName, contactResolver
                )
                if (resolvedPhone != null) {
                    Log.i(TAG, "RESOLVED GOOG_ '$displayName' → $resolvedPhone")
                }
            }

            // Determine the conversation phone to use
            val conversationPhone = resolvedPhone ?: normalizedSender

            // Get or create conversation
            val conversation = commsRepository.getOrCreateConversation(
                phone = conversationPhone,
                displayName = displayName
            )

            // If we just resolved a GOOG_ to a real phone, merge any existing GOOG_ conversation
            if (resolvedPhone != null) {
                val googConvo = commsRepository.getConversationByPhone(normalizedSender)
                if (googConvo != null && googConvo.id != conversation.id) {
                    Log.i(TAG, "MERGING GOOG_ conversation ${googConvo.id} → ${conversation.id}")
                    commsRepository.mergeConversations(
                        keepId = conversation.id,
                        mergeId = googConvo.id
                    )
                }
            }

            // If still a hash fallback (couldn't resolve), flag for review
            if (isHashFallback && resolvedPhone == null && !conversation.needsReview) {
                commsRepository.saveConversation(
                    conversation.copy(needsReview = true, source = "RCS")
                )
            }

            // Deduplication: check for same body within ±10 seconds
            val existingMessages = commsRepository.getMessagesByConversationIdSync(conversation.id)
            val now = System.currentTimeMillis()
            val isDuplicate = existingMessages.any { msg ->
                msg.body == body && abs(msg.timestampEpoch - now) < DEDUP_WINDOW_MS
            }
            if (isDuplicate) {
                Log.d(TAG, "Filtered: duplicate message")
                return@launch
            }

            // Store inbound message
            Log.i(TAG, "STORING message in conversation ${conversation.id} from $conversationPhone")
            val message = MessageEntity(
                conversationId = conversation.id,
                body = body,
                timestampEpoch = now,
                isInbound = true,
                status = MessageStatus.RECEIVED
            )
            commsRepository.saveMessage(message)

            // Update conversation
            commsRepository.saveConversation(
                conversation.copy(
                    lastMessageEpoch = now,
                    unreadCount = conversation.unreadCount + 1,
                    source = "RCS"
                )
            )

            // Trigger auto-reply check
            launch { autoReplyManager.maybeAutoReply(conversation.id) }
        }
    }

    /**
     * Extracts the message body from notification extras.
     * Filters out Google Messages reaction/quote notifications that are not real messages.
     */
    private fun extractBody(extras: Bundle): String? {
        // Try EXTRA_BIG_TEXT first (expanded notification), then EXTRA_TEXT
        val body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: return null

        // Filter Google Messages reaction/quote notifications.
        // When someone reacts to or quotes a message, Google Messages sends a
        // notification with body like 'Questioned "original message"' or '👍 message'.
        // These are not real inbound messages and should be skipped.
        val reactionPrefixes = listOf(
            "Questioned \"", "Reacted ", "Liked \"", "Loved \"",
            "Disliked \"", "Emphasized \"", "Laughed at \"",
            "\uD83D\uDC4D ", "❤\uFE0F ", "\uD83D\uDE02 ", "\uD83D\uDE2E ", "\uD83D\uDE22 ", "\uD83D\uDE21 "
        )
        if (reactionPrefixes.any { body.startsWith(it) }) {
            Log.d(TAG, "Filtered reaction/quote notification: ${body.take(40)}")
            return null
        }

        return body
    }

    /**
     * 5-step phone number extraction from notification extras.
     *
     * Returns Triple(phoneOrHash, displayName?, isHashFallback)
     */
    private fun extractPhoneNumber(
        extras: Bundle,
        sbn: StatusBarNotification
    ): Triple<String, String?, Boolean> {

        // Step 1: Check android.messages for sender_person with tel: URI
        @Suppress("DEPRECATION")
        val messages = extras.getParcelableArray("android.messages")
        if (messages != null) {
            for (msg in messages) {
                if (msg is Bundle) {
                    val senderPerson = msg.get("sender_person")
                    if (senderPerson != null) {
                        // Person object may have a URI like "tel:+15551234567"
                        val personStr = senderPerson.toString()
                        val telMatch = Regex("tel:([+\\d]+)").find(personStr)
                        if (telMatch != null) {
                            return Triple(telMatch.groupValues[1], null, false)
                        }
                    }
                    // Also check "sender" key directly
                    val sender = msg.getCharSequence("sender")?.toString()
                    if (sender != null && PHONE_PATTERN.matches(sender.trim())) {
                        return Triple(sender.trim(), null, false)
                    }
                }
            }
        }

        // Step 2: Check android.subText for phone pattern
        val subText = extras.getCharSequence("android.subText")?.toString()
        if (subText != null && PHONE_PATTERN.matches(subText.trim())) {
            return Triple(subText.trim(), null, false)
        }

        // Step 3: Check EXTRA_TITLE for phone number
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        if (title != null && PHONE_PATTERN.matches(title.trim())) {
            return Triple(title.trim(), null, false)
        }

        // Step 4: Try extracting phone from title with mixed content
        if (title != null) {
            val phoneInTitle = Regex("[+]?[\\d\\-().\\s]{7,}").find(title)
            if (phoneInTitle != null) {
                return Triple(phoneInTitle.value.trim(), title, false)
            }
        }

        // Step 5: LAST RESORT — hash the title as a fallback identifier
        val fallbackName = title ?: sbn.key
        val hash = md5(fallbackName).take(8)
        return Triple("${PhoneNumberUtils.GOOGLE_ID_PREFIX}$hash", fallbackName, true)
    }

    /**
     * Attempts to resolve a Google caller-ID display name back to a real phone number.
     *
     * Tries multiple strategies:
     * 1. Check subText field — Google Messages sometimes puts the real phone there
     * 2. Look up the display name in Android contacts
     * 3. Look up by display name in existing Phill conversations
     */
    private suspend fun tryResolveGoogleCallerId(
        extras: Bundle,
        displayName: String,
        contactResolver: ContactResolver
    ): String? {
        // Strategy 1: subText often contains the real phone even when title shows caller ID
        val subText = extras.getCharSequence("android.subText")?.toString()
        if (subText != null) {
            val phone = Regex("[+]?[\\d\\-().\\s]{7,}").find(subText)
            if (phone != null) {
                val normalized = PhoneNumberUtils.normalize(phone.value)
                if (!PhoneNumberUtils.isShortCode(normalized)) {
                    Log.d(TAG, "Resolved via subText: $normalized")
                    return normalized
                }
            }
        }

        // Strategy 2: Check android.messages bundle for any tel: URIs we might have missed
        @Suppress("DEPRECATION")
        val messages = extras.getParcelableArray("android.messages")
        if (messages != null) {
            for (msg in messages) {
                if (msg is Bundle) {
                    // Some messaging apps put the phone in "sender" even when title shows name
                    val sender = msg.getCharSequence("sender")?.toString()
                    if (sender != null) {
                        val normalized = PhoneNumberUtils.normalize(sender)
                        if (normalized.length >= PhoneNumberUtils.MIN_REAL_PHONE_DIGITS) {
                            Log.d(TAG, "Resolved via message sender: $normalized")
                            return normalized
                        }
                    }
                }
            }
        }

        // Strategy 3: Reverse-lookup display name in Android contacts
        val contactPhone = contactResolver.getPhoneForName(displayName)
        if (contactPhone != null) {
            val normalized = PhoneNumberUtils.normalize(contactPhone)
            if (!PhoneNumberUtils.isShortCode(normalized)) {
                Log.d(TAG, "Resolved via contacts: $normalized")
                return normalized
            }
        }

        return null
    }

    /**
     * Simple MD5 hash for generating deterministic fallback identifiers.
     */
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
