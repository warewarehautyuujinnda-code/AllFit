package com.hinata.fitlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hinata.fitlog.data.entity.StrengthSetEntity

@Dao
interface StrengthSetDao {
    @Query("SELECT * FROM strength_set ORDER BY recordId ASC, setIndex ASC")
    suspend fun getAll(): List<StrengthSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<StrengthSetEntity>)

    @Query("DELETE FROM strength_set WHERE recordId IN (:recordIds)")
    suspend fun deleteByRecordIds(recordIds: List<String>)

    @Query("DELETE FROM strength_set")
    suspend fun deleteAll()
}
