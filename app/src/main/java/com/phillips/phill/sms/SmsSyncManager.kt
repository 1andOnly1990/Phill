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
    private val commsRepository: CommsRepository
) {
    /**
     * Reads ALL SMS threads from the device's native content provider and imports them
     * into the Phill database. Groups by phone number, creates conversations, and deduplicates
     * against already-stored messages.
     *
     * Call this once after SMS permission is granted to bootstrap the Comms tab with the
     * full bidirectional SMS history from the device.
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

        // Batch: collect all messages first, then write in bulk per conversation
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

        // Group by normalized phone number (last 10 digits for matching)
        val grouped = allMessages.groupBy { it.address.filter { c -> c.isDigit() }.takeLast(10) }

        for ((_, messages) in grouped) {
            if (messages.isEmpty()) continue
            val rawAddress = messages.first().address

            // Get or create conversation for this phone number
            val conversation = commsRepository.getOrCreateConversation(rawAddress)

            // Get existing messages for deduplication
            val existingMessages = commsRepository.getMessagesByConversationIdSync(conversation.id)

            var latestEpoch = conversation.lastMessageEpoch ?: 0L

            for (sms in messages) {
                // Deduplicate: same body and within 5 seconds
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

            // Update conversation's last message timestamp
            if (latestEpoch > (conversation.lastMessageEpoch ?: 0L)) {
                commsRepository.saveConversation(
                    conversation.copy(lastMessageEpoch = latestEpoch)
                )
            }
        }
    }

    /**
     * Syncs SMS messages for a specific conversation by querying the native device database.
     * This ensures Phill captures the complete bidirectional history, including messages sent
     * outside the app.
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
