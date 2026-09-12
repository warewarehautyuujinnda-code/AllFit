package com.hinata.fitlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import kotlinx.coroutines.flow.Flow

@Dao
interface StrengthDao {
    /** 記録とセットごとの内訳（日付降順）。セットの並びは各記録内で呼び出し側が並べ替える */
    @Transaction
    @Query("SELECT * FROM strength ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<StrengthRecordWithSets>>

    @Query("SELECT * FROM strength ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<StrengthEntity>

    @Query("SELECT COUNT(*) FROM strength")
    fun observeCount(): Flow<Int>

    /** その種目名の記録件数。種目名を変えるときに、同じ名前の記録が既にないかを見るのに使う */
    @Query("SELECT COUNT(*) FROM strength WHERE ex = :ex")
    suspend fun countByExercise(ex: String): Int

    /**
     * 種目名の変更にともなう記録の付け替え（記録と種目は名前で紐づくため）。
     * 記録そのものは消さない。部位が未設定の古い記録には、変更前の名前から引けていた部位を入れておく
     * （種目名を変えるとプリセットから部位を引けなくなり、「その他」に落ちてしまうため）。
     */
    @Query("UPDATE strength SET ex = :newName, part = COALESCE(part, :fallbackPart) WHERE ex = :currentName")
    suspend fun renameExercise(currentName: String, newName: String, fallbackPart: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: StrengthEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<StrengthEntity>)

    @Query("DELETE FROM strength WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM strength WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM strength")
    suspend fun deleteAll()
}
