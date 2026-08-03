package com.hinata.fitlog.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hinata.fitlog.FitLogApp
import com.hinata.fitlog.ui.common.DateUtil
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * ホームに表示する当日サマリー（FR-06）。
 */
data class HomeSummary(
    /** サマリーの対象日（yyyy-MM-dd） */
    val date: String = DateUtil.today(),
    /** 最新の体重(kg)。記録が1件も無ければ null */
    val latestWeight: Double? = null,
    /** 最新体重の記録日（yyyy-MM-dd）。記録が1件も無ければ null */
    val latestWeightDate: String? = null,
    /** 当日の摂取カロリー(kcal)。食事記録の合計 */
    val intakeKcal: Int = 0,
    /** 当日の消費カロリー(kcal)。ランニング記録の合計 */
    val burnedKcal: Int = 0,
    /** 当日の筋トレ種目数。同じ種目を複数行記録しても1種目として数える */
    val strengthExerciseCount: Int = 0,
)

/**
 * ホームの当日サマリー（FR-06）。4種別の記録から表示値を組み立てる。
 */
class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as FitLogApp).database

    /**
     * 当日サマリー。4種別すべての Flow を購読しているため、どの画面で記録を
     * 追加・削除しても即時反映される。
     */
    val summary: StateFlow<HomeSummary> = combine(
        db.weightDao().observeAll(),
        db.strengthDao().observeAll(),
        db.runningDao().observeAll(),
        db.mealDao().observeAll(),
    ) { weights, strengths, runs, meals ->
        // 記録が変わるたびに今日の日付を求め直す（起動しっぱなしで日付をまたいでも次の更新で追従する）
        val today = DateUtil.today()
        // observeAll() は日付降順のため、先頭が最新の体重
        val latest = weights.firstOrNull()
        HomeSummary(
            date = today,
            latestWeight = latest?.weight,
            latestWeightDate = latest?.date,
            // カロリーは任意項目のため、未入力（null）は 0 として合計する
            intakeKcal = meals.filter { it.date == today }.sumOf { it.kcal ?: 0 },
            burnedKcal = runs.filter { it.date == today }.sumOf { it.kcal ?: 0 },
            strengthExerciseCount = strengths.filter { it.date == today }
                .map { it.ex.trim() }
                .distinct()
                .size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeSummary())
}
