package com.phillips.phill.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.phillips.phill.domain.enums.PaymentMethod
import java.util.UUID

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(entity = InvoiceEntity::class, parentColumns = ["id"], childColumns = ["invoice_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("invoice_id")]
)
data class PaymentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "invoice_id")
    val invoiceId: String,
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long,
    val method: PaymentMethod,
    @ColumnInfo(name = "reference_number")
    val referenceNumber: String? = null,
    @ColumnInfo(name = "paid_at_epoch")
    val paidAtEpoch: Long = System.currentTimeMillis(),
    val notes: String? = null
)
