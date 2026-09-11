package com.hinata.fitlog.data

import androidx.room.withTransaction
import com.hinata.fitlog.data.entity.ExerciseEntity
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** 種別ごとの記録件数（FR-14） */
data class RecordCounts(
    val weight: Int = 0,
    val strength: Int = 0,
    val running: Int = 0,
    val meal: Int = 0,
) {
    val total: Int get() = weight + strength + running + meal
}

/**
 * 4種別をまたぐデータ操作（FR-11〜14）をまとめた入り口。
 * 個々の記録画面は各 DAO を直接使うが、書き出し・読み込み・全削除・件数は
 * 4テーブルを横断するため、ここに集約して各画面から重複した知識を持たせない。
 */
class FitLogRepository(private val db: AppDatabase) {

    private val weightDao = db.weightDao()
    private val strengthDao = db.strengthDao()
    private val runningDao = db.runningDao()
    private val mealDao = db.mealDao()
    private val runningSplitDao = db.runningSplitDao()
    private val strengthSetDao = db.strengthSetDao()
    private val runningPointDao = db.runningPointDao()
    private val exerciseDao = db.exerciseDao()

    /**
     * 書き出し用の JSON 設定。
     * - prettyPrint: 書き出したファイルを人がそのまま読めるようにする
     * - encodeDefaults: 既定値を持つ項目（id・未入力の kcal など）も必ず書き出し、
     *   AI に渡したときに「項目がない」と「値がない」が混ざらないようにする
     */
    private val exportJson = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * 読み込み用の JSON 設定。
     * ignoreUnknownKeys で、将来項目が増えたファイルも古いアプリで読めるようにする。
     * 必須項目（date・weight など既定値を持たない項目）が欠けたファイルは
     * ここで例外になり、[import] が失敗として扱う。
     */
    private val importJson = Json { ignoreUnknownKeys = true }

    /** 種別ごとの件数。記録の追加・削除に追従する（FR-14） */
    val counts: Flow<RecordCounts> = combine(
        weightDao.observeCount(),
        strengthDao.observeCount(),
        runningDao.observeCount(),
        mealDao.observeCount(),
    ) { w, s, r, m -> RecordCounts(weight = w, strength = s, running = r, meal = m) }

    /** 全記録を JSON 文字列にして返す（FR-11） */
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val backup = FitLogBackup(
            exportedAt = Instant.now().toString(),
            weight = weightDao.getAll(),
            strength = strengthDao.getAll(),
            running = runningDao.getAll(),
            meal = mealDao.getAll(),
            runningSplit = runningSplitDao.getAll(),
            strengthSet = strengthSetDao.getAll(),
            runningPoint = runningPointDao.getAll(),
            exercise = exerciseDao.getAll(),
        )
        exportJson.encodeToString(FitLogBackup.serializer(), backup)
    }

    /**
     * JSON 文字列を読み込んで記録を復元する（FR-12）。
     *
     * 復元は「全置換」ではなく id をキーにしたマージにしている。読み込みが既存の記録を
     * 消してしまうと、バックアップを取るための機能でデータを失うことになるため。
     * id は書き出し時のものをそのまま使うので、同じファイルを2回読んでも重複しない。
     *
     * 解析に失敗した場合は DB に一切触れずに戻る。書き込みも1つのトランザクションに
     * まとめてあるので、途中で失敗しても既存データは壊れない。
     *
     * @return 成功なら読み込んだ内容、失敗なら例外を保持した [Result]
     */
    suspend fun import(json: String): Result<FitLogBackup> = withContext(Dispatchers.IO) {
        val backup = try {
            importJson.decodeFromString(FitLogBackup.serializer(), json)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        runCatching {
            db.withTransaction {
                weightDao.upsertAll(backup.weight)
                strengthDao.upsertAll(backup.strength)
                runningDao.upsertAll(backup.running)
                mealDao.upsertAll(backup.meal)
                runningSplitDao.upsertAll(backup.runningSplit)
                strengthSetDao.upsertAll(backup.strengthSet)
                runningPointDao.upsertAll(backup.runningPoint)
                importExercises(backup.exercise)
            }
            backup
        }
    }

    /**
     * 読み込んだ種目の定義を取り込む。
     *
     * 説明は利用者が書いた文章なので、記録のように id で上書きせず [mergeImportedExercise] で
     * すり合わせる（古いバックアップを読んで新しい説明を失わないようにするため）。
     * 最後に、記録にあるのに定義が無い種目を作り、どの種目にも説明を書ける状態にそろえる。
     */
    private suspend fun importExercises(imported: List<ExerciseEntity>) {
        imported.forEach { incoming ->
            val name = incoming.name.trim()
            if (name.isEmpty()) return@forEach
            val local = exerciseDao.findByName(name)
            if (local == null) {
                exerciseDao.insertIfAbsent(incoming.copy(name = name))
            } else {
                val merged = mergeImportedExercise(local, incoming.copy(name = name))
                if (merged != local) exerciseDao.update(merged)
            }
        }
        exerciseDao.insertMissingFromRecords(isoNow())
    }

    /**
     * すべての記録を削除する（FR-13）。4テーブルが中途半端に消えないよう一括で行う。
     *
     * 種目の定義（説明）はここでも消さない。利用者が書いた種目の説明は記録そのものではなく、
     * 消さずに残す決まりにしている（CLAUDE.md の絶対ルール）。
     */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        db.withTransaction {
            weightDao.deleteAll()
            strengthDao.deleteAll()
            runningDao.deleteAll()
            mealDao.deleteAll()
            runningSplitDao.deleteAll()
            strengthSetDao.deleteAll()
            runningPointDao.deleteAll()
        }
    }
}
