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
 * Per HITL Rule 1: This ONLY stores the message. No autonomous responses.
 * The operator sees it when they open the Comms tab.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var commsRepository: CommsRepository

    @Inject
    lateinit var autoReplyManager: AutoReplyManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            // Group by sender (multi-part SMS may arrive as multiple SmsMessage objects)
            val grouped = messages.groupBy { it.displayOriginatingAddress ?: "unknown" }

            for ((sender, parts) in grouped) {
                val body = parts.joinToString("") { it.displayMessageBody ?: "" }
                if (body.isBlank()) continue

                // Get or create conversation for this phone number
                val conversation = commsRepository.getOrCreateConversation(sender)

                // Store the message
                val message = MessageEntity(
                    conversationId = conversation.id,
                    body = body,
                    timestampEpoch = System.currentTimeMillis(),
                    isInbound = true,
                    status = MessageStatus.RECEIVED
                )
                commsRepository.saveMessage(message)

                // Update conversation's last message time and unread count
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
