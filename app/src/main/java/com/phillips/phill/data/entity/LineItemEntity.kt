package com.phillips.phill.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.phillips.phill.domain.enums.LineItemType
import java.util.UUID

@Entity(
    tableName = "line_items",
    foreignKeys = [
        ForeignKey(entity = InvoiceEntity::class, parentColumns = ["id"], childColumns = ["invoice_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("invoice_id")]
)
data class LineItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "invoice_id")
    val invoiceId: String,
    val type: LineItemType,
    val description: String,
    @ColumnInfo(name = "quantity_thousandths")
    val quantityThousandths: Long = 1000L,
    @ColumnInfo(name = "unit_price_cents")
    val unitPriceCents: Long,
    @ColumnInfo(name = "total_cents")
    val totalCents: Long = 0L,
    @ColumnInfo(name = "is_taxable")
    val isTaxable: Boolean = false,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0
)
