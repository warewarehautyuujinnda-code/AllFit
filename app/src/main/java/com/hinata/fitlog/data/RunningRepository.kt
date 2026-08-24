package com.hinata.fitlog.data

import androidx.room.withTransaction
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.RunningSplitEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * ランニングタブの入り口。記録([RunningEntity])と1分ごとの内訳([RunningSplitEntity])は
 * 常に1組として保存・削除するため、[FitLogRepository]（4種別をまたぐ操作）とは別に
 * この2テーブルだけを扱う専用のリポジトリとして持つ。
 */
class RunningRepository(private val db: AppDatabase) {
    private val runningDao = db.runningDao()
    private val splitDao = db.runningSplitDao()

    /** 保存済みの記録（日付降順） */
    fun observeAll(): Flow<List<RunningEntity>> = runningDao.observeAll()

    /** 指定した記録の1分ごとの内訳。GPS計測でない記録は空になる */
    fun observeSplits(runId: String): Flow<List<RunningSplitEntity>> =
        splitDao.observeByRunId(runId)

    /** 手入力の記録を保存する。内訳は持たない */
    suspend fun saveManual(run: RunningEntity) = withContext(Dispatchers.IO) {
        runningDao.upsert(run)
    }

    /** GPS計測で得た記録と1分ごとの内訳を1つのトランザクションで保存する */
    suspend fun saveTracked(run: RunningEntity, splits: List<RunningSplitEntity>) =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                runningDao.upsert(run)
                if (splits.isNotEmpty()) splitDao.upsertAll(splits)
            }
        }

    /** 記録を削除する。内訳が残らないよう同じトランザクションで消す */
    suspend fun deleteRun(id: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            splitDao.deleteByRunId(id)
            runningDao.deleteById(id)
        }
    }
}
