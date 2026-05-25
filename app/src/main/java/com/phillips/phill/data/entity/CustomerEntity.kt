package com.phillips.phill.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "customers",
    indices = [Index(value = ["phone_number"], unique = true)]
)
data class CustomerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "first_name")
    val firstName: String,
    @ColumnInfo(name = "last_name")
    val lastName: String,
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null,
    @ColumnInfo(name = "created_at_epoch")
    val createdAtEpoch: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_epoch")
    val updatedAtEpoch: Long = System.currentTimeMillis()
)
