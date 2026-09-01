package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.MealEntity
import com.hinata.fitlog.data.entity.WeightEntity
import java.time.LocalDate

/**
 * 記録から表示用の集計を作る処理。Android に依存しない素の Kotlin で書いてあるので、
 * 画面や ViewModel を持ち出さずにそのまま動かして確かめられる。
 *
 * date は yyyy-MM-dd 固定のため、文字列の辞書順がそのまま日付順になる。
 * 以下の並べ替えはすべてその前提に乗っている。
 */

/** ホームの当日サマリー（FR-06） */
data class HomeSummary(
    /** 直近の体重記録。1件もなければ null */
    val latestWeight: WeightEntity? = null,
    /** 当日の摂取カロリー（食事の合計） */
    val intakeKcal: Int = 0,
)

/** 体重推移グラフ（FR-07）に渡す、直近30件を古い順に並べたデータ */
data class WeightTrend(
    /** 古い→新しい順の体重。グラフは左から右に時間が進む */
    val points: List<WeightEntity> = emptyList(),
) {
    /** 期間の増減(kg)。2件未満では増減が定義できないため null */
    val delta: Double?
        get() = if (points.size < 2) null else points.last().weight - points.first().weight

    companion object {
        const val MAX_POINTS = 30

        /**
         * 期間指定（[weightTrendOf]の period 版）での上限。期間は暦日数で区切るため
         * 件数が事前に読めず、長期間・高頻度で記録するユーザーでも Canvas の描画が
         * 重くならないよう安全弁として設ける
         */
        const val MAX_POINTS_FOR_PERIOD = 200
    }
}

/** 食事の合計（FR-10）。未入力の項目は合計に含めない */
data class MealTotals(
    val kcal: Int = 0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
)

/**
 * 当日サマリー（FR-06）を組み立てる。
 *
 * @param weights 日付降順の体重記録。先頭が最新体重になる
 * @param today 集計する日（yyyy-MM-dd）
 */
fun homeSummaryOf(
    weights: List<WeightEntity>,
    meals: List<MealEntity>,
    today: String,
): HomeSummary = HomeSummary(
    latestWeight = weights.firstOrNull(),
    // 未入力の kcal は 0 として足す。合計なので「未入力＝加算なし」で意味が通る
    intakeKcal = meals.filter { it.date == today }.sumOf { it.kcal ?: 0 },
)

/**
 * 体重推移（FR-07）を組み立てる。
 * @param weights 日付降順の体重記録。直近30件を取ってから反転し、古い→新しい順にする
 */
fun weightTrendOf(weights: List<WeightEntity>): WeightTrend =
    WeightTrend(weights.take(WeightTrend.MAX_POINTS).reversed())

/**
 * 体重推移（FR-07）を表示期間で絞って組み立てる。
 * @param weights 日付降順の体重記録
 * @param period 表示期間。ALL は絞り込みなし
 * @param today 期間の下限日を決める基準日。呼び出し側は通常省略し、テストでのみ固定する
 */
fun weightTrendOf(
    weights: List<WeightEntity>,
    period: TrendPeriod,
    today: LocalDate = LocalDate.now(),
): WeightTrend {
    val cutoff = period.cutoffDate(today)?.toString()
    val inPeriod = if (cutoff == null) weights else weights.filter { it.date >= cutoff }
    return WeightTrend(inPeriod.take(WeightTrend.MAX_POINTS_FOR_PERIOD).reversed())
}

/**
 * 選択中の期間より前にも体重の記録があるか。
 *
 * 期間チップで絞り込んだ結果グラフに出ていないだけなのに、記録自体が無い・消えたと
 * 誤解されないよう、画面側で「もっと前にも記録がある」旨を伝えるために使う。
 * @param weights 体重記録（順不同で可）
 */
fun hasWeightBeforePeriod(
    weights: List<WeightEntity>,
    period: TrendPeriod,
    today: LocalDate = LocalDate.now(),
): Boolean {
    val cutoff = period.cutoffDate(today)?.toString() ?: return false
    return weights.any { it.date < cutoff }
}

/**
 * 指定日の食事の合計（FR-10）を求める。
 * 未入力（null）の項目は合計に含めない。すべて未入力なら 0 になる。
 */
fun mealTotalsOf(meals: List<MealEntity>, date: String): MealTotals {
    val ofDay = meals.filter { it.date == date }
    return MealTotals(
        kcal = ofDay.sumOf { it.kcal ?: 0 },
        protein = ofDay.sumOf { it.p ?: 0.0 },
        fat = ofDay.sumOf { it.f ?: 0.0 },
        carbs = ofDay.sumOf { it.c ?: 0.0 },
    )
}
