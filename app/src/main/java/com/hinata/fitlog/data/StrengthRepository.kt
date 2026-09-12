package com.hinata.fitlog.data

import androidx.room.withTransaction
import com.hinata.fitlog.data.entity.ExerciseEntity
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import com.hinata.fitlog.data.entity.StrengthSetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * 筋トレタブの入り口。記録（[StrengthEntity]）とセットごとの内訳（[StrengthSetEntity]）は
 * 常に1組として保存・削除するため、[FitLogRepository]（4種別をまたぐ操作）とは別に
 * この2テーブルだけを扱う専用のリポジトリとして持つ。
 * version 8 からは種目の定義（[ExerciseEntity]）も同じ入り口で扱う。記録の保存と種目の追加は
 * 同じトランザクションで整合させたいため。
 */
class StrengthRepository(private val db: AppDatabase) {
    private val strengthDao = db.strengthDao()
    private val setDao = db.strengthSetDao()
    private val exerciseDao = db.exerciseDao()

    /** 保存済みの記録とセットごとの内訳（日付降順）。DBの変更に追従する */
    fun observeAll(): Flow<List<StrengthRecordWithSets>> = strengthDao.observeAll()

    /** 種目の定義（説明・一覧からの非表示）。DBの変更に追従する */
    fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()

    /** 記録とセットごとの内訳を1つのトランザクションで保存する */
    suspend fun save(record: StrengthEntity, sets: List<StrengthSetEntity>) =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                strengthDao.upsert(record)
                if (sets.isNotEmpty()) setDao.upsertAll(sets)
                // 記録した種目には必ず定義がある状態にしておく（あとから説明を書き足せるように）
                val now = isoNow()
                exerciseDao.insertIfAbsent(
                    ExerciseEntity(
                        name = record.ex,
                        part = record.part,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            }
        }

    /** 記録を削除する。セットの内訳が残らないよう同じトランザクションで消す */
    suspend fun deleteRecords(ids: List<String>) = withContext(Dispatchers.IO) {
        db.withTransaction {
            setDao.deleteByRecordIds(ids)
            strengthDao.deleteByIds(ids)
        }
    }

    /**
     * 種目を追加する（種目選択画面の鉛筆ボタン）。
     * すでにある種目なら説明はそのままにし、一覧から削除していた場合だけ再び表示する。
     */
    suspend fun addExercise(name: String, part: String?) = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext
        db.withTransaction {
            val now = isoNow()
            exerciseDao.insertIfAbsent(
                ExerciseEntity(name = trimmed, part = part, createdAt = now, updatedAt = now)
            )
            exerciseDao.unhide(trimmed, now)
        }
    }

    /**
     * 種目名・説明を変更する。名前を変えた場合は、これまでの記録もすべて新しい名前に付け替える
     * （記録と種目は名前で紐づくため）。記録そのものは1件も消さない。
     *
     * @return 保存できたら true。名前が空、または別の種目（一覧から削除したものを含む）と
     *   同じ名前になる場合は、記録が混ざってしまうため保存せず false
     */
    suspend fun updateExercise(
        currentName: String,
        newName: String,
        description: String?,
        part: String?,
    ): Boolean = withContext(Dispatchers.IO) {
        val name = newName.trim()
        if (name.isEmpty()) return@withContext false
        db.withTransaction {
            val renamed = name != currentName
            if (renamed &&
                (exerciseDao.findByName(name) != null || strengthDao.countByExercise(name) > 0)
            ) {
                return@withTransaction false
            }
            val now = isoNow()
            val current = exerciseDao.findByName(currentName)
            if (current == null) {
                // 記録も定義も無い種目（今週の計画だけにある種目など）はここで定義を作る
                exerciseDao.insertIfAbsent(
                    ExerciseEntity(
                        name = name,
                        part = part,
                        description = description,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            } else {
                exerciseDao.rename(
                    currentName = currentName,
                    newName = name,
                    part = part,
                    description = description,
                    updatedAt = now,
                )
            }
            if (renamed) strengthDao.renameExercise(currentName, name, current?.part ?: part)
            true
        }
    }

    /**
     * 種目を一覧から削除する。**表示から外すだけで、定義・記録・説明・メモはすべて残す**
     * （CLAUDE.md の絶対ルール）。同じ名前で追加し直せば、説明も記録もそのままに再び一覧へ出る。
     */
    suspend fun hideExercise(name: String, part: String?) = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext
        db.withTransaction {
            val now = isoNow()
            exerciseDao.insertIfAbsent(
                ExerciseEntity(name = trimmed, part = part, createdAt = now, updatedAt = now)
            )
            exerciseDao.hide(trimmed, now)
        }
    }
}
