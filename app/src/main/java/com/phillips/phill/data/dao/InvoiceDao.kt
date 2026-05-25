package com.phillips.phill.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phillips.phill.data.entity.InvoiceEntity
import com.phillips.phill.domain.enums.InvoiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY created_at_epoch DESC")
    fun observeAll(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE job_id = :jobId")
    fun observeByJob(jobId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE customer_id = :customerId ORDER BY created_at_epoch DESC")
    fun observeByCustomer(customerId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE status = :status ORDER BY created_at_epoch DESC")
    fun observeByStatus(status: InvoiceStatus): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getById(id: String): InvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: InvoiceEntity): Long

    @Update
    suspend fun update(invoice: InvoiceEntity)

    @Delete
    suspend fun delete(invoice: InvoiceEntity)
}
