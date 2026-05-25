package com.phillips.phill.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phillips.phill.data.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date_epoch DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE job_id = :jobId ORDER BY date_epoch DESC")
    fun observeByJob(jobId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: String): ExpenseEntity?

    @Query("SELECT SUM(amount_cents) FROM expenses WHERE date_epoch >= :startEpoch AND date_epoch <= :endEpoch")
    suspend fun totalInRange(startEpoch: Long, endEpoch: Long): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)
}
