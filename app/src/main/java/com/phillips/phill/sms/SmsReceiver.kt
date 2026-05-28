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
