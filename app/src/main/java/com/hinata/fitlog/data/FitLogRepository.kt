package com.hinata.fitlog.data

import androidx.room.withTransaction
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
            }
            backup
        }
    }

    /** すべての記録を削除する（FR-13）。4テーブルが中途半端に消えないよう一括で行う */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        db.withTransaction {
            weightDao.deleteAll()
            strengthDao.deleteAll()
            runningDao.deleteAll()
            mealDao.deleteAll()
            runningSplitDao.deleteAll()
        }
    }
}
