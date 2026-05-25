package com.phillips.phill.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "clock_entries",
    foreignKeys = [
        ForeignKey(entity = JobEntity::class, parentColumns = ["id"], childColumns = ["job_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("job_id")]
)
data class ClockEntryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "job_id")
    val jobId: String,
    @ColumnInfo(name = "clock_in_epoch")
    val clockInEpoch: Long,
    @ColumnInfo(name = "clock_out_epoch")
    val clockOutEpoch: Long? = null,
    val notes: String? = null
)
