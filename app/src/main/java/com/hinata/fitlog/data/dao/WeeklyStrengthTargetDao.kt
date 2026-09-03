package com.hinata.fitlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hinata.fitlog.data.entity.WeeklyStrengthTargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyStrengthTargetDao {
    @Query("SELECT * FROM weekly_strength_target")
    fun observeAll(): Flow<List<WeeklyStrengthTargetEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<WeeklyStrengthTargetEntity>)
}
