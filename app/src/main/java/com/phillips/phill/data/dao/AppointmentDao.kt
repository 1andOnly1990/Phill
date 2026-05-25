package com.phillips.phill.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phillips.phill.data.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments WHERE scheduled_start_epoch >= :startEpoch AND scheduled_start_epoch <= :endEpoch ORDER BY scheduled_start_epoch ASC")
    fun observeByDateRange(startEpoch: Long, endEpoch: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE customer_id = :customerId ORDER BY scheduled_start_epoch DESC")
    fun observeByCustomer(customerId: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getById(id: String): AppointmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appointment: AppointmentEntity): Long

    @Update
    suspend fun update(appointment: AppointmentEntity)

    @Delete
    suspend fun delete(appointment: AppointmentEntity)
}
