package com.phillips.phill.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phillips.phill.data.entity.MileageEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MileageEntryDao {
    @Query("SELECT * FROM mileage_entries ORDER BY recorded_at_epoch DESC")
    fun observeAll(): Flow<List<MileageEntryEntity>>

    @Query("SELECT * FROM mileage_entries WHERE job_id = :jobId")
    fun observeByJob(jobId: String): Flow<List<MileageEntryEntity>>

    @Query("SELECT * FROM mileage_entries WHERE id = :id")
    suspend fun getById(id: String): MileageEntryEntity?

    @Query("SELECT SUM(miles) FROM mileage_entries WHERE recorded_at_epoch >= :startEpoch AND recorded_at_epoch <= :endEpoch")
    suspend fun totalMilesInRange(startEpoch: Long, endEpoch: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MileageEntryEntity): Long

    @Update
    suspend fun update(entry: MileageEntryEntity)

    @Delete
    suspend fun delete(entry: MileageEntryEntity)
}
