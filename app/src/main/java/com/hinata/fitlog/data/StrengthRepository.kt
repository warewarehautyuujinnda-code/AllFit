package com.hinata.fitlog.data

import androidx.room.withTransaction
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
 */
class StrengthRepository(private val db: AppDatabase) {
    private val strengthDao = db.strengthDao()
    private val setDao = db.strengthSetDao()

    /** 保存済みの記録とセットごとの内訳（日付降順）。DBの変更に追従する */
    fun observeAll(): Flow<List<StrengthRecordWithSets>> = strengthDao.observeAll()

    /** 記録とセットごとの内訳を1つのトランザクションで保存する */
    suspend fun save(record: StrengthEntity, sets: List<StrengthSetEntity>) =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                strengthDao.upsert(record)
                if (sets.isNotEmpty()) setDao.upsertAll(sets)
            }
        }

    /** 記録を削除する。セットの内訳が残らないよう同じトランザクションで消す */
    suspend fun deleteRecords(ids: List<String>) = withContext(Dispatchers.IO) {
        db.withTransaction {
            setDao.deleteByRecordIds(ids)
            strengthDao.deleteByIds(ids)
        }
    }
}
