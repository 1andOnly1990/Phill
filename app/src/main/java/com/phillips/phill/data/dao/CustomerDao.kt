package com.phillips.phill.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phillips.phill.data.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY last_name ASC, first_name ASC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE phone_number = :phone")
    suspend fun getByPhone(phone: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE first_name LIKE '%' || :query || '%' OR last_name LIKE '%' || :query || '%' OR phone_number LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Delete
    suspend fun delete(customer: CustomerEntity)
}
