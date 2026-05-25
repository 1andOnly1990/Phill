package com.phillips.phill.sms

import android.content.Context
import android.provider.Telephony
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
        
        // Filter by phone number. Phone numbers can be tricky, so we use LIKE for flexibility,
        // or just rely on the exact string if normalized. Let's use exact match for now but
        // in a production app this might need phone number normalization.
        // For simplicity, we just fetch all and filter in memory if the dataset is small,
        // but it's better to filter in the query.
        val selection = "${Telephony.Sms.ADDRESS} LIKE ?"
        val selectionArgs = arrayOf("%${phoneNumber.takeLast(10)}")

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

            while (it.moveToNext()) {
                val body = it.getString(bodyIndex) ?: continue
                val date = it.getLong(dateIndex)
                val type = it.getInt(typeIndex)

                val isInbound = type == Telephony.Sms.MESSAGE_TYPE_INBOX

                // We need to check if this message already exists in our DB to avoid duplicates.
                // A simple way is to rely on timestamp and body, or since we don't have a unique
                // native ID stored, we can do a rough check. But for this MVP, we'll assume
                // inserting them will be fine if we can check existence.
                // To keep it efficient, we might just query existing messages and filter.
                
                val existingMessages = commsRepository.getMessagesByConversationIdSync(conversationId)
                
                // Very basic deduplication: same body and within 5 seconds
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
