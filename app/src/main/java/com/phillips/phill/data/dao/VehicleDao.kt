package com.phillips.phill.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phillips.phill.data.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles WHERE customer_id = :customerId")
    fun observeByCustomer(customerId: String): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getById(id: String): VehicleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehicle: VehicleEntity): Long

    @Update
    suspend fun update(vehicle: VehicleEntity)

    @Delete
    suspend fun delete(vehicle: VehicleEntity)
}
