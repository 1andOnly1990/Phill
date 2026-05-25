package com.phillips.phill.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.phillips.phill.domain.enums.JobStatus
import java.util.UUID

@Entity(
    tableName = "jobs",
    foreignKeys = [
        ForeignKey(entity = CustomerEntity::class, parentColumns = ["id"], childColumns = ["customer_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = VehicleEntity::class, parentColumns = ["id"], childColumns = ["vehicle_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("customer_id"), Index("vehicle_id"), Index("status")]
)
data class JobEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "customer_id")
    val customerId: String,
    @ColumnInfo(name = "vehicle_id")
    val vehicleId: String,
    @ColumnInfo(name = "appointment_id")
    val appointmentId: String? = null,
    val status: JobStatus = JobStatus.SCHEDULED,
    val description: String? = null,
    val notes: String? = null,
    @ColumnInfo(name = "created_at_epoch")
    val createdAtEpoch: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at_epoch")
    val completedAtEpoch: Long? = null
)
