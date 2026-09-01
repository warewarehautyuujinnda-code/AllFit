package com.hinata.fitlog.ui.running

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.RunningPointEntity
import com.hinata.fitlog.data.entity.RunningSplitEntity
import com.hinata.fitlog.domain.RunningMetric
import com.hinata.fitlog.domain.RunningTrend
import com.hinata.fitlog.domain.RunningTrendPeriod
import com.hinata.fitlog.domain.monthlyTotalDistance
import com.hinata.fitlog.domain.parseOptionalDouble
import com.hinata.fitlog.domain.parseRequiredDouble
import com.hinata.fitlog.domain.runningTrendOf
import com.hinata.fitlog.running.RunTrackState
import com.hinata.fitlog.running.RunTracker
import com.hinata.fitlog.ui.common.DateUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ランニングの記録。GPS計測（[RunTracker]）と手入力の両方の保存・一覧の取得を担う。
 */
class RunningViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = (app as FitLogApp).runningRepository

    /** 保存済みの記録（日付降順）。DBの変更に追従する */
    val items: StateFlow<List<RunningEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** グラフの表示期間。デフォルトは直近3ヶ月 */
    private val _trendPeriod = MutableStateFlow(RunningTrendPeriod.THREE_MONTHS)
    val trendPeriod: StateFlow<RunningTrendPeriod> = _trendPeriod

    /** グラフで見る指標。デフォルトは今までと同じ距離 */
    private val _trendMetric = MutableStateFlow(RunningMetric.DISTANCE)
    val trendMetric: StateFlow<RunningMetric> = _trendMetric

    /** メイン画面のグラフ用（選択中の期間で絞った、直近30件・古い順） */
    val trend: StateFlow<RunningTrend> = combine(items, _trendPeriod) { list, period ->
        runningTrendOf(list, period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RunningTrend())

    /** 今月の合計距離(km) */
    val monthlyTotalKm: StateFlow<Double> = items
        .map { monthlyTotalDistance(it, DateUtil.today().substring(0, 7)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /** GPS計測中の状態（開始/停止、経過時間、距離） */
    val trackState: StateFlow<RunTrackState> = RunTracker.state

    fun selectTrendPeriod(period: RunningTrendPeriod) {
        _trendPeriod.value = period
    }

    fun selectTrendMetric(metric: RunningMetric) {
        _trendMetric.value = metric
    }

    /** 記録の1分ごとの内訳。GPS計測でない記録は空になる */
    fun splitsFor(runId: String): Flow<List<RunningSplitEntity>> = repository.observeSplits(runId)

    /** 記録が通った経路（緯度経度の並び）。GPS計測でない記録は空になる */
    fun pointsFor(runId: String): Flow<List<RunningPointEntity>> = repository.observePoints(runId)

    /**
     * 手入力で保存する。距離は必須、時間は任意。
     * 任意項目は未入力なら null として保存し、入力があるのに数値として読めない場合は
     * 黙って捨てずに保存を失敗させる。
     * @return 入力が正しく保存できたら true
     */
    fun save(date: String, distText: String, minText: String, memoText: String): Boolean {
        if (date.isBlank()) return false

        val dist = parseRequiredDouble(distText) ?: return false
        val min = parseOptionalDouble(minText) ?: return false

        viewModelScope.launch {
            repository.saveManual(
                RunningEntity(
                    date = date,
                    dist = dist,
                    min = min.value,
                    kcal = null,
                    memo = memoText.trim().ifBlank { null },
                )
            )
        }
        return true
    }

    /** 記録を削除する。GPS内訳があれば一緒に削除する */
    fun delete(item: RunningEntity) {
        viewModelScope.launch { repository.deleteRun(item.id) }
    }
}
