package com.phillips.phill.sms

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.phillips.phill.data.entity.MessageEntity
import com.phillips.phill.data.repository.CommsRepository
import com.phillips.phill.domain.enums.MessageStatus
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
        private const val MIN_REAL_PHONE_DIGITS = 7
        private const val DEDUP_WINDOW_MS = 10_000L // ±10 seconds

        /** Messaging app package names to monitor. */
        private val MONITORED_PACKAGES = setOf(
            "com.google.android.apps.messaging",  // Google Messages
            "com.samsung.android.messaging"         // Samsung Messages
        )

        /** Matches strings that look like phone numbers. */
        private val PHONE_PATTERN = Regex("^[+]?[\\d\\s\\-().]{7,}$")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        // Filter: only monitor known messaging apps
        if (sbn.packageName !in MONITORED_PACKAGES) return

        val extras = sbn.notification?.extras ?: return
        val body = extractBody(extras)
        if (body.isNullOrBlank()) return

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

            // Normalize
            val normalizedSender = rawSender.filter { it.isDigit() }.takeLast(10)

            // Filter 1: Short code check (unless hash fallback)
            if (!isHashFallback && normalizedSender.length < MIN_REAL_PHONE_DIGITS) return@launch

            // Filter 2: Known personal contact
            if (!isHashFallback && contactResolver.isKnownContact(rawSender)) return@launch

            // Get or create conversation
            val conversation = commsRepository.getOrCreateConversation(
                phone = if (isHashFallback) rawSender else normalizedSender,
                displayName = displayName
            )

            // If hash fallback, update conversation to flag for review
            if (isHashFallback && !conversation.needsReview) {
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
            if (isDuplicate) return@launch

            // Store inbound message
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
     */
    private fun extractBody(extras: Bundle): String? {
        // Try EXTRA_BIG_TEXT first (expanded notification), then EXTRA_TEXT
        return extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
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
        return Triple("GOOG_$hash", fallbackName, true)
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
