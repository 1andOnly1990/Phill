package com.phillips.phill.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a media attachment shared via an external messaging app.
 *
 * Attachments are linked to conversations and optionally to messages and jobs.
 * The file is stored locally and shared via Intent — no MMS is sent directly.
 *
 * Foreign key strategies:
 *   - conversation_id: CASCADE (deleting a conversation deletes all attachments)
 *   - message_id: SET_NULL (deleting a message preserves the attachment record)
 *   - job_id: SET_NULL (deleting a job preserves the attachment record)
 */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(entity = ConversationEntity::class, parentColumns = ["id"], childColumns = ["conversation_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MessageEntity::class, parentColumns = ["id"], childColumns = ["message_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = JobEntity::class, parentColumns = ["id"], childColumns = ["job_id"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("conversation_id"), Index("message_id"), Index("job_id")]
)
data class AttachmentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "message_id")
    val messageId: String? = null,
    @ColumnInfo(name = "job_id")
    val jobId: String? = null,
    @ColumnInfo(name = "file_uri")
    val fileUri: String,
    @ColumnInfo(name = "file_name")
    val fileName: String?,
    @ColumnInfo(name = "mime_type")
    val mimeType: String,
    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long? = null,
    @ColumnInfo(name = "shared_at_epoch")
    val sharedAtEpoch: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "thumbnail_uri")
    val thumbnailUri: String? = null
)
