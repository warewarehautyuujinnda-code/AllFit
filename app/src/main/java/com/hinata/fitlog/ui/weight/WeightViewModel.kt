package com.hinata.fitlog.ui.weight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.data.entity.WeightEntity
import com.hinata.fitlog.domain.TrendPeriod
import com.hinata.fitlog.domain.WeightTrend
import com.hinata.fitlog.domain.parseRequiredDouble
import com.hinata.fitlog.domain.weightTrendOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 体重の記録（FR-01）。保存と一覧の取得、体重推移と目標体重を担う。
 */
class WeightViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as FitLogApp).database.weightDao()
    private val goalStore = (app as FitLogApp).weightGoalStore

    /** 保存済みの記録（日付降順）。DBの変更に追従する */
    val items: StateFlow<List<WeightEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 体重推移グラフ（FR-07）の表示期間。既定は3ヶ月 */
    private val _period = MutableStateFlow(TrendPeriod.THREE_MONTHS)
    val period: StateFlow<TrendPeriod> = _period.asStateFlow()

    /** 体重推移グラフ（FR-07）。一覧・期間と同じ購読から作るのでDBを2度読まない */
    val trend: StateFlow<WeightTrend> = combine(items, _period) { w, p ->
        weightTrendOf(w, p)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightTrend())

    /** 設定中の目標体重(kg)。未設定なら null */
    val goal: StateFlow<Double?> = goalStore.goal

    /** グラフの表示期間を切り替える */
    fun onPeriodChange(period: TrendPeriod) {
        _period.value = period
    }

    /**
     * 保存する。体重は必須。
     * @return 入力が正しく保存できたら true
     */
    fun save(date: String, weightText: String): Boolean {
        if (date.isBlank()) return false
        val weight = parseRequiredDouble(weightText) ?: return false

        viewModelScope.launch {
            // 体脂肪率は入力欄ごと廃止したので常に未記録。列は既存データを守るため残してある
            dao.upsert(WeightEntity(date = date, weight = weight, fat = null))
        }
        return true
    }

    /**
     * 目標体重を設定する。
     * @return 入力が数値として読めたら true
     */
    fun setGoal(text: String): Boolean {
        val value = parseRequiredDouble(text) ?: return false
        goalStore.set(value)
        return true
    }

    /** 目標体重を未設定に戻す */
    fun clearGoal() = goalStore.clear()

    /** 記録を1件削除する（FR-05） */
    fun delete(item: WeightEntity) {
        viewModelScope.launch { dao.deleteById(item.id) }
    }
}
