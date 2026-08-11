package com.hinata.fitlog.ui.running

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.domain.parseOptionalDouble
import com.hinata.fitlog.domain.parseOptionalInt
import com.hinata.fitlog.domain.parseRequiredDouble
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ランニングの記録（FR-03）。保存と一覧の取得、ペースの計算を担う。
 */
class RunningViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as FitLogApp).database.runningDao()

    /** 保存済みの記録（日付降順）。DBの変更に追従する */
    val items: StateFlow<List<RunningEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 保存する。距離は必須、時間・消費カロリーは任意。
     * 任意項目は未入力なら null として保存し、入力があるのに数値として読めない場合は
     * 黙って捨てずに保存を失敗させる。
     * ペースは距離と時間から都度計算できるため保存しない（データ要件 8.3）。
     * @return 入力が正しく保存できたら true
     */
    fun save(
        date: String,
        distText: String,
        minText: String,
        kcalText: String,
    ): Boolean {
        if (date.isBlank()) return false

        // 不正な値をDBに残すと履歴の表示側で毎回問題になるので、保存時点で拒否する
        val dist = parseRequiredDouble(distText) ?: return false

        val min = parseOptionalDouble(minText) ?: return false
        val kcal = parseOptionalInt(kcalText) ?: return false

        viewModelScope.launch {
            dao.upsert(
                RunningEntity(
                    date = date,
                    dist = dist,
                    min = min.value,
                    kcal = kcal.value,
                )
            )
        }
        return true
    }

    /** 記録を1件削除する（FR-05） */
    fun delete(item: RunningEntity) {
        viewModelScope.launch { dao.deleteById(item.id) }
    }
}
