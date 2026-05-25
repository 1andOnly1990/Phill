package com.phillips.phill.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "mileage_entries",
    foreignKeys = [
        ForeignKey(entity = JobEntity::class, parentColumns = ["id"], childColumns = ["job_id"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("job_id")]
)
data class MileageEntryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "job_id")
    val jobId: String? = null,
    val miles: Double,
    val purpose: String? = null,
    @ColumnInfo(name = "recorded_at_epoch")
    val recordedAtEpoch: Long = System.currentTimeMillis()
)
