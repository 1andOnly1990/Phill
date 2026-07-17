package com.phillips.phill.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.phillips.phill.domain.enums.ExpenseCategory
import java.util.UUID

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(entity = JobEntity::class, parentColumns = ["id"], childColumns = ["job_id"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("job_id"), Index("date_epoch")]
)
data class ExpenseEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "job_id")
    val jobId: String? = null,
    val category: ExpenseCategory,
    val description: String,
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long,
    val vendor: String? = null,
    @ColumnInfo(name = "date_epoch")
    val dateEpoch: Long = System.currentTimeMillis(),
    val notes: String? = null,
    @ColumnInfo(name = "receipt_uri")
    val receiptUri: String? = null
)
