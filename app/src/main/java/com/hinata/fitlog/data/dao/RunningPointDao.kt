package com.hinata.fitlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hinata.fitlog.data.entity.RunningPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunningPointDao {
    @Query("SELECT * FROM running_point WHERE runId = :runId ORDER BY sequence ASC")
    fun observeByRunId(runId: String): Flow<List<RunningPointEntity>>

    @Query("SELECT * FROM running_point ORDER BY runId ASC, sequence ASC")
    suspend fun getAll(): List<RunningPointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RunningPointEntity>)

    @Query("DELETE FROM running_point WHERE runId = :runId")
    suspend fun deleteByRunId(runId: String)

    @Query("DELETE FROM running_point")
    suspend fun deleteAll()
}
