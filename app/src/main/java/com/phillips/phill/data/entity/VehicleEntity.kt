package com.phillips.phill.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "vehicles",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customer_id"])]
)
data class VehicleEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "customer_id")
    val customerId: String,
    val year: Int? = null,
    val make: String,
    val model: String,
    val engine: String? = null,
    val vin: String? = null,
    val color: String? = null,
    val notes: String? = null,
    @ColumnInfo(name = "created_at_epoch")
    val createdAtEpoch: Long = System.currentTimeMillis()
)
