package com.phillips.phill.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.phillips.phill.domain.enums.InvoiceStatus
import java.util.UUID

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(entity = JobEntity::class, parentColumns = ["id"], childColumns = ["job_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CustomerEntity::class, parentColumns = ["id"], childColumns = ["customer_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = VehicleEntity::class, parentColumns = ["id"], childColumns = ["vehicle_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = AppointmentEntity::class, parentColumns = ["id"], childColumns = ["appointment_id"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("job_id"), Index("customer_id"), Index("status"), Index("vehicle_id"), Index("appointment_id"), Index("invoice_number")]
)
data class InvoiceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "job_id")
    val jobId: String,
    @ColumnInfo(name = "customer_id")
    val customerId: String,
    val status: InvoiceStatus = InvoiceStatus.ESTIMATE,
    @ColumnInfo(name = "subtotal_cents")
    val subtotalCents: Long = 0L,
    @ColumnInfo(name = "tax_cents")
    val taxCents: Long = 0L,
    @ColumnInfo(name = "total_cents")
    val totalCents: Long = 0L,
    @ColumnInfo(name = "service_fee_cents")
    val serviceFeeCents: Long = 0L,
    @ColumnInfo(name = "created_at_epoch")
    val createdAtEpoch: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "finalized_at_epoch")
    val finalizedAtEpoch: Long? = null,
    @ColumnInfo(name = "terms_text")
    val termsText: String? = null,
    @ColumnInfo(name = "vehicle_id")
    val vehicleId: String? = null,
    @ColumnInfo(name = "appointment_id")
    val appointmentId: String? = null,
    @ColumnInfo(name = "invoice_number")
    val invoiceNumber: String? = null
)
