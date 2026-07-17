package com.phillips.phill.sms

import android.content.Context
import android.provider.Telephony
import com.phillips.phill.data.entity.ConversationEntity
import com.phillips.phill.data.entity.MessageEntity
import com.phillips.phill.data.repository.CommsRepository
import com.phillips.phill.domain.enums.MessageStatus
import com.phillips.phill.util.PhoneNumberUtils
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
         * Minimum digit count delegated to PhoneNumberUtils.MIN_REAL_PHONE_DIGITS.
         */

        /**
         * Carrier error messages that Android stores as type=1 (INBOX).
         * These are NOT real inbound messages — filter them out during sync.
         */
        private val CARRIER_ERROR_PREFIXES = listOf(
            "Text messaging service has been denied",
            "Message not sent",
            "Unable to send",
            "Free msg: unable to send",
            "Message failed",
            "Service denied"
        )
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

        // Group by normalized phone number
        val grouped = allMessages.groupBy { PhoneNumberUtils.normalize(it.address) }

        for ((normalizedPhone, messages) in grouped) {
            if (messages.isEmpty()) continue

            // --- FILTER 1: Short code / toll-free check ---
            if (PhoneNumberUtils.isTollFreeOrShortCode(normalizedPhone)) continue

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

                // --- FILTER: Carrier error messages stored as inbox type ---
                if (CARRIER_ERROR_PREFIXES.any { prefix -> sms.body.startsWith(prefix, ignoreCase = true) }) continue

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
        val selectionArgs = arrayOf("%${PhoneNumberUtils.normalize(phoneNumber)}")

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

                // --- FILTER: Carrier error messages stored as inbox type ---
                if (CARRIER_ERROR_PREFIXES.any { prefix -> body.startsWith(prefix, ignoreCase = true) }) continue

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
