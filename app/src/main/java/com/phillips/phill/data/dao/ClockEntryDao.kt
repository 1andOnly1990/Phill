package com.phillips.phill.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phillips.phill.data.entity.ClockEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClockEntryDao {
    @Query("SELECT * FROM clock_entries WHERE job_id = :jobId ORDER BY clock_in_epoch DESC")
    fun observeByJob(jobId: String): Flow<List<ClockEntryEntity>>

    @Query("SELECT * FROM clock_entries WHERE clock_out_epoch IS NULL LIMIT 1")
    suspend fun getActiveEntry(): ClockEntryEntity?

    @Query("SELECT * FROM clock_entries WHERE id = :id")
    suspend fun getById(id: String): ClockEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ClockEntryEntity): Long

    @Update
    suspend fun update(entry: ClockEntryEntity)

    @Delete
    suspend fun delete(entry: ClockEntryEntity)
}
