package com.phillips.phill.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.phillips.phill.domain.enums.AppointmentStatus
import java.util.UUID

@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(entity = CustomerEntity::class, parentColumns = ["id"], childColumns = ["customer_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("customer_id"), Index("scheduled_start_epoch")]
)
data class AppointmentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "customer_id")
    val customerId: String,
    @ColumnInfo(name = "vehicle_id")
    val vehicleId: String? = null,
    @ColumnInfo(name = "job_id")
    val jobId: String? = null,
    val address: String,
    @ColumnInfo(name = "scheduled_start_epoch")
    val scheduledStartEpoch: Long,
    @ColumnInfo(name = "scheduled_end_epoch")
    val scheduledEndEpoch: Long? = null,
    val notes: String? = null,
    val status: AppointmentStatus = AppointmentStatus.PENDING,
    @ColumnInfo(name = "created_at_epoch")
    val createdAtEpoch: Long = System.currentTimeMillis()
)
