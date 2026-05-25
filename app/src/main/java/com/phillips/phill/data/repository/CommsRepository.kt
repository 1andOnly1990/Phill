package com.phillips.phill.data.repository

import com.phillips.phill.data.dao.ConversationDao
import com.phillips.phill.data.dao.MessageDao
import com.phillips.phill.data.entity.ConversationEntity
import com.phillips.phill.data.entity.MessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommsRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    fun observeAllConversations(): Flow<List<ConversationEntity>> = conversationDao.observeAll()

    suspend fun getConversationById(id: String): ConversationEntity? =
        conversationDao.getById(id)

    suspend fun getConversationByPhone(phone: String): ConversationEntity? =
        conversationDao.getByPhone(phone)

    suspend fun saveConversation(conversation: ConversationEntity) {
        val existing = conversationDao.getById(conversation.id)
        if (existing != null) conversationDao.update(conversation) else conversationDao.insert(conversation)
    }

    suspend fun deleteConversation(conversation: ConversationEntity) =
        conversationDao.delete(conversation)

    // Messages
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.observeByConversation(conversationId)

    suspend fun saveMessage(message: MessageEntity) {
        val existing = messageDao.getById(message.id)
        if (existing != null) messageDao.update(message) else messageDao.insert(message)
    }

    suspend fun deleteMessage(message: MessageEntity) = messageDao.delete(message)

    /**
     * Get or create a conversation for a phone number.
     * Used by SMS receiver to route inbound messages.
     */
    suspend fun getOrCreateConversation(phone: String, displayName: String? = null): ConversationEntity {
        val existing = conversationDao.getByPhone(phone)
        if (existing != null) return existing
        val conversation = ConversationEntity(phoneNumber = phone, displayName = displayName)
        conversationDao.insert(conversation)
        return conversation
    }
}
