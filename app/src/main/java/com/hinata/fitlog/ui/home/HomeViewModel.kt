package com.hinata.fitlog.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.domain.HomeSummary
import com.hinata.fitlog.domain.RecentRecord
import com.hinata.fitlog.domain.WeightTrend
import com.hinata.fitlog.domain.buildRecentRecords
import com.hinata.fitlog.domain.homeSummaryOf
import com.hinata.fitlog.domain.weightTrendOf
import com.hinata.fitlog.ui.common.DateUtil
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ホーム画面（FR-06〜08）。4種別すべてを購読し、集計は domain の関数に任せる。
 */
class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as FitLogApp).database

    private val weights = db.weightDao().observeAll()
    private val strengths = db.strengthDao().observeAll()
    private val runs = db.runningDao().observeAll()
    private val meals = db.mealDao().observeAll()

    /**
     * 当日サマリー（FR-06）。
     * 「今日」は購読時ではなく集計のたびに評価するので、日付をまたいでも次の更新で正しくなる。
     */
    val summary: StateFlow<HomeSummary> =
        combine(weights, strengths, runs, meals) { w, s, r, m ->
            homeSummaryOf(w, s, r, m, DateUtil.today())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeSummary())

    /** 体重推移グラフ（FR-07） */
    val weightTrend: StateFlow<WeightTrend> = weights
        .map { weightTrendOf(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightTrend())

    /** 最近の記録一覧（FR-08） */
    val recentRecords: StateFlow<List<RecentRecord>> =
        combine(weights, strengths, runs, meals) { w, s, r, m ->
            buildRecentRecords(w, s, r, m)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
