package com.hinata.fitlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hinata.fitlog.data.entity.RunningSplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunningSplitDao {
    @Query("SELECT * FROM running_split WHERE runId = :runId ORDER BY minuteIndex ASC")
    fun observeByRunId(runId: String): Flow<List<RunningSplitEntity>>

    @Query("SELECT * FROM running_split ORDER BY runId ASC, minuteIndex ASC")
    suspend fun getAll(): List<RunningSplitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RunningSplitEntity>)

    @Query("DELETE FROM running_split WHERE runId = :runId")
    suspend fun deleteByRunId(runId: String)

    @Query("DELETE FROM running_split")
    suspend fun deleteAll()
}
