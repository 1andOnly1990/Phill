package com.phillips.phill.sms

import android.app.Application
import android.telephony.SmsManager
import com.phillips.phill.data.entity.MessageEntity
import com.phillips.phill.data.repository.CommsRepository
import com.phillips.phill.data.repository.OperationsRepository
import com.phillips.phill.domain.enums.MessageStatus
import com.phillips.phill.domain.model.BusinessHoursSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoReplyManager @Inject constructor(
    private val operationsRepository: OperationsRepository,
    private val commsRepository: CommsRepository,
    private val application: Application
) {

    companion object {
        /** Minimum milliseconds between auto-replies to the same conversation (2 hours). */
        private const val RATE_LIMIT_MS = 2L * 60L * 60L * 1000L
    }

    /**
     * Called after an inbound SMS is stored in the database.
     * Fetches the latest conversation state fresh from the DB to avoid race conditions.
     * Sends an auto-reply SMS if ALL of the following are true:
     *   1. Auto-reply is enabled in shop settings
     *   2. A non-blank reply message is configured
     *   3. We have NOT already sent an auto-reply to this conversation in the last 2 hours
     *   4. The current time is outside the configured business hours
     *
     * Per HITL Rule 1: this sends an automated message, but it is configured in advance
     * by the operator. No AI generates the reply text.
     */
    suspend fun maybeAutoReply(conversationId: String) = withContext(Dispatchers.IO) {
        // Check profile settings first (cheap DB read, fail fast)
        val profile = operationsRepository.getShopProfile() ?: return@withContext
        if (!profile.autoReplyEnabled) return@withContext

        val messageText = profile.autoReplyMessage?.takeIf { it.isNotBlank() } ?: return@withContext

        // Fetch the conversation fresh (SmsReceiver may have just updated it)
        val conversation = commsRepository.getConversationById(conversationId) ?: return@withContext

        // Rate limit check: skip if we already auto-replied within the last 2 hours
        val now = System.currentTimeMillis()
        val lastReply = conversation.lastAutoReplyEpoch ?: 0L
        if (now - lastReply < RATE_LIMIT_MS) return@withContext

        // Business hours check: only reply if we are currently outside business hours
        if (!isOutsideBusinessHours(profile.businessHoursJson)) return@withContext

        // --- Send the auto-reply SMS ---
        try {
            val smsManager = application.getSystemService(SmsManager::class.java)
            val parts = smsManager.divideMessage(messageText)
            smsManager.sendMultipartTextMessage(
                conversation.phoneNumber,
                null,
                parts,
                null,
                null
            )

            // Store the auto-reply as an outbound message so it appears in the thread
            val replyMessage = MessageEntity(
                conversationId = conversationId,
                body = messageText,
                timestampEpoch = now,
                isInbound = false,
                status = MessageStatus.SENT
            )
            commsRepository.saveMessage(replyMessage)

            // Re-fetch conversation one more time to get the absolute latest state
            // (avoids overwriting unreadCount that SmsReceiver incremented)
            val freshConversation = commsRepository.getConversationById(conversationId)
                ?: return@withContext

            // Update: stamp lastAutoReplyEpoch and lastMessageEpoch
            commsRepository.saveConversation(
                freshConversation.copy(
                    lastAutoReplyEpoch = now,
                    lastMessageEpoch = now
                )
            )
        } catch (e: Exception) {
            // Silently fail — operator will still see the unread incoming message in Comms
        }
    }

    /**
     * Returns true if the current local time is OUTSIDE the configured business hours.
     * "Outside hours" means auto-reply SHOULD be sent.
     * Returns true (outside hours) when:
     *   - No schedule is configured (null JSON) — uses Mon-Fri 8-5 defaults
     *   - JSON is malformed — falls back to defaults
     *   - Today is marked as a closed day
     *   - Current time is before opening time or at/after closing time
     */
    private fun isOutsideBusinessHours(businessHoursJson: String?): Boolean {
        val schedule = if (businessHoursJson != null) {
            try {
                Json.decodeFromString<BusinessHoursSchedule>(businessHoursJson)
            } catch (e: Exception) {
                BusinessHoursSchedule() // malformed JSON: use defaults
            }
        } else {
            BusinessHoursSchedule() // no schedule configured: use defaults
        }

        val cal = Calendar.getInstance()
        // Calendar.DAY_OF_WEEK values: 1=Sunday, 2=Monday, ..., 7=Saturday
        // Our list index:              0=Sunday, 1=Monday, ..., 6=Saturday
        val dayIndex = cal.get(Calendar.DAY_OF_WEEK) - 1
        val minutesNow = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val daySchedule = schedule.days.getOrNull(dayIndex)
            ?: return true // day index out of range = treat as closed

        if (!daySchedule.isOpen) return true // this day is marked closed

        val openMin = daySchedule.openMinutes()
        val closeMin = daySchedule.closeMinutes()

        // Outside hours = before open time OR at/after close time
        return minutesNow < openMin || minutesNow >= closeMin
    }
}
