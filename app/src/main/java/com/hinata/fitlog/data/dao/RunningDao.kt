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

    /**
     * メモだけを書き換える。記録まるごとの上書き（[upsert]）にしないのは、
     * 距離・時間・登録日時といった他の項目を巻き込んで書き換えてしまわないようにするため。
     */
    @Query("UPDATE running SET memo = :memo WHERE id = :id")
    suspend fun updateMemo(id: String, memo: String?)

    @Query("DELETE FROM running WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM running")
    suspend fun deleteAll()
}
