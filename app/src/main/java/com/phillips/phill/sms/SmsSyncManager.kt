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
