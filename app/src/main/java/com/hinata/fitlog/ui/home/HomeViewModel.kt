package com.hinata.fitlog.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.domain.HomeSummary
import com.hinata.fitlog.domain.WeightTrend
import com.hinata.fitlog.domain.homeSummaryOf
import com.hinata.fitlog.domain.weightTrendOf
import com.hinata.fitlog.ui.common.DateUtil
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ホーム画面（FR-06〜07）。当日サマリーと体重推移グラフの購読を担う。
 */
class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as FitLogApp).database

    private val weights = db.weightDao().observeAll()
    private val runs = db.runningDao().observeAll()
    private val meals = db.mealDao().observeAll()

    /**
     * 当日サマリー（FR-06）。
     * 「今日」は購読時ではなく集計のたびに評価するので、日付をまたいでも次の更新で正しくなる。
     */
    val summary: StateFlow<HomeSummary> =
        combine(weights, runs, meals) { w, r, m ->
            homeSummaryOf(w, r, m, DateUtil.today())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeSummary())

    /** 体重推移グラフ（FR-07） */
    val weightTrend: StateFlow<WeightTrend> = weights
        .map { weightTrendOf(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightTrend())

    /** 目標体重(kg)。設定の入り口は体重タブにあり、ここでは表示にだけ使う */
    val weightGoal: StateFlow<Double?> = (app as FitLogApp).weightGoalStore.goal
}
