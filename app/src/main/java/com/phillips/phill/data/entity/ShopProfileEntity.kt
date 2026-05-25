package com.phillips.phill.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_profile")
data class ShopProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(name = "business_name")
    val businessName: String? = null,
    @ColumnInfo(name = "business_address")
    val businessAddress: String? = null,
    @ColumnInfo(name = "labor_rate_cents", defaultValue = "12500")
    val laborRateCents: Long = 12500L,
    @ColumnInfo(name = "service_fee_cents", defaultValue = "6000")
    val serviceFeeCents: Long = 6000L,
    @ColumnInfo(name = "parts_markup_basis_points", defaultValue = "14000")
    val partsMarkupBasisPoints: Int = 14000,
    @ColumnInfo(name = "tax_rate_basis_points", defaultValue = "600")
    val taxRateBasisPoints: Int = 600,
    @ColumnInfo(name = "tax_id")
    val taxId: String? = null,
    @ColumnInfo(name = "license_number")
    val licenseNumber: String? = null,
    @ColumnInfo(name = "owner_name")
    val ownerName: String? = null,
    @ColumnInfo(name = "owner_phone")
    val ownerPhone: String? = null
)
