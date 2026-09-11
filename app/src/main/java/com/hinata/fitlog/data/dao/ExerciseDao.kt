package com.hinata.fitlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hinata.fitlog.data.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

/**
 * 種目の定義（[ExerciseEntity]）。
 *
 * **このDAOには削除のクエリを置かない。** 種目の「削除」は [hide] による論理削除だけで行い、
 * 行そのものは消さない（CLAUDE.md の絶対ルール）。REPLACE も競合行の削除を伴うため使わない。
 * この決まりは ExerciseRetentionRuleTest が検査している。
 */
@Dao
interface ExerciseDao {
    /** 種目の定義すべて。一覧の表示・編集に使う。DBの変更に追従する */
    @Query("SELECT * FROM exercise ORDER BY name ASC")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise ORDER BY name ASC")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise WHERE name = :name")
    suspend fun findByName(name: String): ExerciseEntity?

    /** すでにある種目には触れない追加。記録の保存時に定義が無ければ作るために使う */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(item: ExerciseEntity)

    /** 説明・部位・表示状態の更新。主キー（name）が同じ行を書き換えるだけで、行は消さない */
    @Update
    suspend fun update(item: ExerciseEntity)

    /**
     * 種目名と説明の変更。名前を変えない場合も currentName と newName に同じ値を渡してこれを使う。
     * 記録側（strength.ex）の付け替えは [StrengthDao.renameExercise] と同じトランザクションで行う。
     */
    @Query(
        "UPDATE exercise SET name = :newName, part = COALESCE(part, :part), " +
            "description = :description, updatedAt = :updatedAt WHERE name = :currentName"
    )
    suspend fun rename(
        currentName: String,
        newName: String,
        part: String?,
        description: String?,
        updatedAt: String,
    )

    /** 一覧から削除（非表示）にする。定義・記録・説明・メモはすべて残したまま表示だけ外す */
    @Query("UPDATE exercise SET hiddenAt = :hiddenAt, updatedAt = :hiddenAt WHERE name = :name")
    suspend fun hide(name: String, hiddenAt: String)

    /** 同じ名前で追加し直したときに再び一覧へ出す */
    @Query("UPDATE exercise SET hiddenAt = NULL, updatedAt = :updatedAt WHERE name = :name AND hiddenAt IS NOT NULL")
    suspend fun unhide(name: String, updatedAt: String)

    /**
     * 記録にあるのに定義が無い種目を取り込む。読み込み（FR-12）のあとに実行して
     * 「記録した種目には必ず説明を書ける定義がある」状態を保つ。部位は直近の記録から引く。
     */
    @Query(
        """
        INSERT OR IGNORE INTO exercise (name, part, description, hiddenAt, createdAt, updatedAt)
        SELECT s.ex,
               (SELECT s2.part FROM strength AS s2
                 WHERE s2.ex = s.ex AND s2.part IS NOT NULL
                 ORDER BY s2.date DESC LIMIT 1),
               NULL, NULL, :now, :now
        FROM strength AS s GROUP BY s.ex
        """
    )
    suspend fun insertMissingFromRecords(now: String)
}
