package com.phillips.phill.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phillips.phill.data.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE invoice_id = :invoiceId ORDER BY paid_at_epoch DESC")
    fun observeByInvoice(invoiceId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY paid_at_epoch DESC")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getById(id: String): PaymentEntity?

    @Query("SELECT SUM(amount_cents) FROM payments WHERE invoice_id = :invoiceId")
    suspend fun totalPaidForInvoice(invoiceId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long

    @Update
    suspend fun update(payment: PaymentEntity)

    @Delete
    suspend fun delete(payment: PaymentEntity)
}
