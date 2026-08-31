package com.hinata.fitlog.data

import androidx.room.withTransaction
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.RunningPointEntity
import com.hinata.fitlog.data.entity.RunningSplitEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * ランニングタブの入り口。記録([RunningEntity])と1分ごとの内訳([RunningSplitEntity])、
 * 経路([RunningPointEntity])は常に1組として保存・削除するため、[FitLogRepository]
 * （4種別をまたぐ操作）とは別に、この3テーブルだけを扱う専用のリポジトリとして持つ。
 */
class RunningRepository(private val db: AppDatabase) {
    private val runningDao = db.runningDao()
    private val splitDao = db.runningSplitDao()
    private val pointDao = db.runningPointDao()

    /** 保存済みの記録（日付降順） */
    fun observeAll(): Flow<List<RunningEntity>> = runningDao.observeAll()

    /** 指定した記録の1分ごとの内訳。GPS計測でない記録は空になる */
    fun observeSplits(runId: String): Flow<List<RunningSplitEntity>> =
        splitDao.observeByRunId(runId)

    /** 指定した記録が通った経路（緯度経度の並び）。GPS計測でない記録は空になる */
    fun observePoints(runId: String): Flow<List<RunningPointEntity>> =
        pointDao.observeByRunId(runId)

    /** 手入力の記録を保存する。内訳・経路は持たない */
    suspend fun saveManual(run: RunningEntity) = withContext(Dispatchers.IO) {
        runningDao.upsert(run)
    }

    /** GPS計測で得た記録・1分ごとの内訳・経路を1つのトランザクションで保存する */
    suspend fun saveTracked(
        run: RunningEntity,
        splits: List<RunningSplitEntity>,
        points: List<RunningPointEntity>,
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            runningDao.upsert(run)
            if (splits.isNotEmpty()) splitDao.upsertAll(splits)
            if (points.isNotEmpty()) pointDao.upsertAll(points)
        }
    }

    /** 記録を削除する。内訳・経路が残らないよう同じトランザクションで消す */
    suspend fun deleteRun(id: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            splitDao.deleteByRunId(id)
            pointDao.deleteByRunId(id)
            runningDao.deleteById(id)
        }
    }
}
