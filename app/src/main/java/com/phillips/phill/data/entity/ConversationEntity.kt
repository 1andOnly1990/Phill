package com.phillips.phill.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "conversations",
    foreignKeys = [
        ForeignKey(entity = CustomerEntity::class, parentColumns = ["id"], childColumns = ["customer_id"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("customer_id"), Index("phone_number")]
)
data class ConversationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "customer_id")
    val customerId: String? = null,
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,
    @ColumnInfo(name = "display_name")
    val displayName: String? = null,
    @ColumnInfo(name = "last_message_epoch")
    val lastMessageEpoch: Long? = null,
    @ColumnInfo(name = "unread_count")
    val unreadCount: Int = 0,
    @ColumnInfo(name = "source", defaultValue = "SMS")
    val source: String = "SMS",
    @ColumnInfo(name = "needs_review", defaultValue = "0")
    val needsReview: Boolean = false,
    @ColumnInfo(name = "last_auto_reply_epoch")
    val lastAutoReplyEpoch: Long? = null
)
