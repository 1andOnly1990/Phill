package com.phillips.phill.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phillips.phill.data.entity.LineItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LineItemDao {
    @Query("SELECT * FROM line_items WHERE invoice_id = :invoiceId ORDER BY sort_order ASC")
    fun observeByInvoice(invoiceId: String): Flow<List<LineItemEntity>>

    @Query("SELECT * FROM line_items WHERE id = :id")
    suspend fun getById(id: String): LineItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lineItem: LineItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lineItems: List<LineItemEntity>)

    @Update
    suspend fun update(lineItem: LineItemEntity)

    @Delete
    suspend fun delete(lineItem: LineItemEntity)

    @Query("DELETE FROM line_items WHERE invoice_id = :invoiceId")
    suspend fun deleteByInvoice(invoiceId: String)
}
