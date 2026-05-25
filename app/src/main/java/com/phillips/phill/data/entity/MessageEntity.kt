package com.phillips.phill.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.phillips.phill.domain.enums.MessageStatus
import java.util.UUID

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(entity = ConversationEntity::class, parentColumns = ["id"], childColumns = ["conversation_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("conversation_id"), Index("timestamp_epoch")]
)
data class MessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    val body: String,
    @ColumnInfo(name = "timestamp_epoch")
    val timestampEpoch: Long,
    @ColumnInfo(name = "is_inbound")
    val isInbound: Boolean,
    val status: MessageStatus = MessageStatus.RECEIVED
)
