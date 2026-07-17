package com.phillips.phill.data.repository

import com.phillips.phill.data.dao.ConversationDao
import com.phillips.phill.data.dao.MessageDao
import com.phillips.phill.data.entity.ConversationEntity
import com.phillips.phill.data.entity.MessageEntity
import com.phillips.phill.util.PhoneNumberUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommsRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    // ── Conversations ──────────────────────────────────────────────────

    fun observeAllConversations(): Flow<List<ConversationEntity>> = conversationDao.observeAll()

    fun observeConversationsByCustomer(customerId: String): Flow<List<ConversationEntity>> =
        conversationDao.observeByCustomer(customerId)

    fun observeConversationsByJob(jobId: String): Flow<List<ConversationEntity>> =
        conversationDao.observeByJob(jobId)

    fun searchConversations(query: String): Flow<List<ConversationEntity>> =
        conversationDao.search(query)

    suspend fun getConversationById(id: String): ConversationEntity? =
        conversationDao.getById(id)

    suspend fun getConversationByPhone(phone: String): ConversationEntity? =
        conversationDao.getByPhone(PhoneNumberUtils.normalize(phone))

    suspend fun saveConversation(conversation: ConversationEntity) {
        val existing = conversationDao.getById(conversation.id)
        if (existing != null) conversationDao.update(conversation) else conversationDao.insert(conversation)
    }

    suspend fun deleteConversation(conversation: ConversationEntity) =
        conversationDao.delete(conversation)

    // ── Linking ─────────────────────────────────────────────────────────

    /**
     * Link a conversation to a customer. Updates customerId and optionally displayName.
     */
    suspend fun linkConversationToCustomer(conversationId: String, customerId: String, displayName: String? = null) {
        val convo = conversationDao.getById(conversationId) ?: return
        conversationDao.update(convo.copy(customerId = customerId, displayName = displayName ?: convo.displayName))
    }

    /**
     * Link a conversation to an active job.
     */
    suspend fun linkConversationToJob(conversationId: String, jobId: String) {
        val convo = conversationDao.getById(conversationId) ?: return
        conversationDao.update(convo.copy(jobId = jobId))
    }

    /**
     * Link a conversation to an appointment.
     */
    suspend fun linkConversationToAppointment(conversationId: String, appointmentId: String) {
        val convo = conversationDao.getById(conversationId) ?: return
        conversationDao.update(convo.copy(appointmentId = appointmentId))
    }

    // ── Merge / Dedup ───────────────────────────────────────────────────

    /**
     * Merge two conversations into one. All messages from [mergeId] are moved to [keepId].
     * The merged conversation is deleted.
     */
    suspend fun mergeConversations(keepId: String, mergeId: String) {
        val keep = conversationDao.getById(keepId) ?: return
        val merge = conversationDao.getById(mergeId) ?: return

        // Move all messages from merge to keep
        val mergeMessages = messageDao.getByConversationId(mergeId)
        for (msg in mergeMessages) {
            messageDao.insert(msg.copy(conversationId = keepId))
        }

        // Update unread count and last message epoch
        val latestEpoch = maxOf(keep.lastMessageEpoch ?: 0L, merge.lastMessageEpoch ?: 0L)
        conversationDao.update(
            keep.copy(
                unreadCount = keep.unreadCount + merge.unreadCount,
                lastMessageEpoch = latestEpoch
            )
        )

        // Delete the merged conversation (CASCADE deletes its old messages)
        conversationDao.delete(merge)
    }

    // ── Messages ────────────────────────────────────────────────────────

    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.observeByConversation(conversationId)

    fun searchMessages(query: String): Flow<List<MessageEntity>> =
        messageDao.searchAll(query)

    suspend fun getMessagesByConversationIdSync(conversationId: String): List<MessageEntity> =
        messageDao.getByConversationId(conversationId)

    suspend fun saveMessage(message: MessageEntity) {
        val existing = messageDao.getById(message.id)
        if (existing != null) messageDao.update(message) else messageDao.insert(message)
    }

    suspend fun deleteMessage(message: MessageEntity) = messageDao.delete(message)

    // ── Get or Create ───────────────────────────────────────────────────

    /**
     * Get or create a conversation for a phone number.
     * Uses [PhoneNumberUtils.normalize] for consistent storage.
     * GOOG_ identifiers (Google RCS caller IDs) are stored as-is.
     */
    suspend fun getOrCreateConversation(phone: String, displayName: String? = null): ConversationEntity {
        val normalized = PhoneNumberUtils.normalize(phone)
        val existing = conversationDao.getByPhone(normalized)
        if (existing != null) return existing
        val source = if (PhoneNumberUtils.isGoogleId(normalized)) "RCS" else "SMS"
        val conversation = ConversationEntity(
            phoneNumber = normalized,
            displayName = displayName,
            source = source,
            needsReview = PhoneNumberUtils.isGoogleId(normalized)
        )
        conversationDao.insert(conversation)
        return conversation
    }
}
