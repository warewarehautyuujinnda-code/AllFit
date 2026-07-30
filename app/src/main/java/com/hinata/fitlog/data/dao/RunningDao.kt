package com.hinata.fitlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hinata.fitlog.data.entity.RunningEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunningDao {
    @Query("SELECT * FROM running ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<RunningEntity>>

    @Query("SELECT * FROM running ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<RunningEntity>

    @Query("SELECT COUNT(*) FROM running")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: RunningEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RunningEntity>)

    @Query("DELETE FROM running WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM running")
    suspend fun deleteAll()
}
