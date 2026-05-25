package com.phillips.phill.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phillips.phill.data.entity.ShopProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopProfileDao {
    @Query("SELECT * FROM shop_profile WHERE id = 1")
    fun observe(): Flow<ShopProfileEntity?>

    @Query("SELECT * FROM shop_profile WHERE id = 1")
    suspend fun get(): ShopProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ShopProfileEntity): Long

    @Update
    suspend fun update(profile: ShopProfileEntity)
}
