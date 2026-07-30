package com.hinata.fitlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hinata.fitlog.data.entity.WeightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    /** 履歴を日付降順で取得（FR-09） */
    @Query("SELECT * FROM weight ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weight ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<WeightEntity>

    @Query("SELECT COUNT(*) FROM weight")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WeightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WeightEntity>)

    @Query("DELETE FROM weight WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM weight")
    suspend fun deleteAll()
}
