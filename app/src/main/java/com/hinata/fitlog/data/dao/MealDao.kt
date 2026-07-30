package com.hinata.fitlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hinata.fitlog.data.entity.MealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meal ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meal ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<MealEntity>

    @Query("SELECT COUNT(*) FROM meal")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MealEntity>)

    @Query("DELETE FROM meal WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM meal")
    suspend fun deleteAll()
}
