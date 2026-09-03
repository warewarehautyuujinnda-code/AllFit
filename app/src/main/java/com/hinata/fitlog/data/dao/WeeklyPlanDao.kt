package com.hinata.fitlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hinata.fitlog.data.entity.WeeklyPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyPlanDao {
    @Query("SELECT * FROM weekly_plan ORDER BY weekStart DESC")
    fun observeAll(): Flow<List<WeeklyPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<WeeklyPlanEntity>)
}
