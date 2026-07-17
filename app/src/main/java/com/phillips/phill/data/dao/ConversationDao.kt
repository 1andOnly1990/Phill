package com.phillips.phill.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phillips.phill.data.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY last_message_epoch DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE customer_id = :customerId ORDER BY last_message_epoch DESC")
    fun observeByCustomer(customerId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE job_id = :jobId ORDER BY last_message_epoch DESC")
    fun observeByJob(jobId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE phone_number = :phone")
    suspend fun getByPhone(phone: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: String): ConversationEntity?

    @Query("""
        SELECT * FROM conversations 
        WHERE phone_number LIKE '%' || :query || '%' 
           OR display_name LIKE '%' || :query || '%'
        ORDER BY last_message_epoch DESC
    """)
    fun search(query: String): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity): Long

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Delete
    suspend fun delete(conversation: ConversationEntity)
}
