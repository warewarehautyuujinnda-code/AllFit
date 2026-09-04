package com.hinata.fitlog.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.domain.HomeSummary
import com.hinata.fitlog.domain.TrendPeriod
import com.hinata.fitlog.domain.WeightTrend
import com.hinata.fitlog.domain.WeeklyGoalSummary
import com.hinata.fitlog.domain.hasWeightBeforePeriod
import com.hinata.fitlog.domain.homeSummaryOf
import com.hinata.fitlog.domain.weightTrendOf
import com.hinata.fitlog.domain.runningDistanceOf
import com.hinata.fitlog.domain.strengthProgressOf
import com.hinata.fitlog.domain.weekStartOf
import com.hinata.fitlog.domain.weeklyPlanFor
import com.hinata.fitlog.ui.common.DateUtil
import com.hinata.fitlog.ui.common.currentDateFlow
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * ホーム画面（FR-06〜07）。当日サマリーと体重推移グラフの購読を担う。
 */
class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as FitLogApp).database

    private val weights = db.weightDao().observeAll()
    private val meals = db.mealDao().observeAll()

    /**
     * 当日サマリー（FR-06）。
     * 「今日」は購読時ではなく集計のたびに評価するので、日付をまたいでも次の更新で正しくなる。
     */
    val summary: StateFlow<HomeSummary> =
        combine(weights, meals) { w, m ->
            homeSummaryOf(w, m, DateUtil.today())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeSummary())

    /** 体重推移グラフ（FR-07）の表示期間。既定は3ヶ月 */
    private val _weightTrendPeriod = MutableStateFlow(TrendPeriod.THREE_MONTHS)
    val weightTrendPeriod: StateFlow<TrendPeriod> = _weightTrendPeriod.asStateFlow()

    /** 体重推移グラフ（FR-07） */
    val weightTrend: StateFlow<WeightTrend> = combine(weights, _weightTrendPeriod) { w, p ->
        weightTrendOf(w, p)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightTrend())

    /** 選択中の期間より前にも記録があるか。あればグラフ側で「全期間で見る」の案内を出す */
    val hasOlderWeightRecords: StateFlow<Boolean> = combine(weights, _weightTrendPeriod) { w, p ->
        hasWeightBeforePeriod(w, p)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 体重推移グラフの表示期間を切り替える */
    fun onWeightTrendPeriodChange(period: TrendPeriod) {
        _weightTrendPeriod.value = period
    }

    /** 目標体重(kg)。設定の入り口は体重タブにあり、ここでは表示にだけ使う */
    val weightGoal: StateFlow<Double?> = (app as FitLogApp).weightGoalStore.goal

    private val planRepository = (app as FitLogApp).planRepository
    private val strengthRecords = db.strengthDao().observeAll()
    private val runningRecords = db.runningDao().observeAll()
    private val currentDate = currentDateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now())

    private val planContextWithoutDate = combine(
        planRepository.observeGoals(),
        planRepository.observeWeeklyPlans(),
        planRepository.observeStrengthTargets(),
    ) { goals, plans, targets -> Triple(goals, plans, targets) }

    private val planContext = combine(planContextWithoutDate, currentDate) { context, date -> context to date }

    val weeklyGoalSummary: StateFlow<WeeklyGoalSummary> = combine(
        planContext, strengthRecords, runningRecords, weights, weightGoal,
    ) { (context, date), strengthRecs, runningRecs, weightRecs, wGoal ->
        val (goals, plans, targets) = context
        val weekStart = weekStartOf(date)
        val plan = weeklyPlanFor(plans, weekStart)
        val latestGoal = goals.maxByOrNull { it.createdAt }
        WeeklyGoalSummary(
            goalTitle = latestGoal?.title,
            goalTargetDate = latestGoal?.targetDate,
            strength = plan?.let { strengthProgressOf(targets, strengthRecs, weekStart) },
            runningActualKm = plan?.targetRunningKm?.let { runningDistanceOf(runningRecs, weekStart) },
            runningTargetKm = plan?.targetRunningKm,
            weightCurrent = weightRecs.firstOrNull()?.weight,
            weightGoal = wGoal,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyGoalSummary())
}
