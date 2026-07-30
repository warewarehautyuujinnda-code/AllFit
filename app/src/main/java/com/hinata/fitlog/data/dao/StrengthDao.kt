package com.hinata.fitlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hinata.fitlog.data.entity.StrengthEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StrengthDao {
    @Query("SELECT * FROM strength ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<StrengthEntity>>

    @Query("SELECT * FROM strength ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<StrengthEntity>

    @Query("SELECT COUNT(*) FROM strength")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: StrengthEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<StrengthEntity>)

    @Query("DELETE FROM strength WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM strength")
    suspend fun deleteAll()
}
