package com.phillips.phill.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phillips.phill.data.entity.JobEntity
import com.phillips.phill.domain.enums.JobStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY created_at_epoch DESC")
    fun observeAll(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE status = :status ORDER BY created_at_epoch DESC")
    fun observeByStatus(status: JobStatus): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE customer_id = :customerId ORDER BY created_at_epoch DESC")
    fun observeByCustomer(customerId: String): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun getById(id: String): JobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: JobEntity): Long

    @Update
    suspend fun update(job: JobEntity)

    @Delete
    suspend fun delete(job: JobEntity)
}
