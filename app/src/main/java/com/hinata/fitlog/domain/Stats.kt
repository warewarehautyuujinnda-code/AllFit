package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.MealEntity
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.data.entity.WeightEntity

/**
 * 記録から表示用の集計を作る処理。Android に依存しない素の Kotlin で書いてあるので、
 * 画面や ViewModel を持ち出さずにそのまま動かして確かめられる。
 *
 * date は yyyy-MM-dd 固定のため、文字列の辞書順がそのまま日付順になる。
 * 以下の並べ替えはすべてその前提に乗っている。
 */

/** 記録の種別。最近の記録一覧（FR-08）で種別を見分けるために使う */
enum class RecordKind(val label: String) {
    WEIGHT("体重"),
    STRENGTH("筋トレ"),
    RUNNING("ラン"),
    MEAL("食事"),
}

/** 最近の記録一覧（FR-08）の1行。種別が違っても同じ形で並べられるようにした表示用の型 */
data class RecentRecord(
    val id: String,
    val kind: RecordKind,
    val date: String,
    val title: String,
    val detail: String,
)

/** ホームの当日サマリー（FR-06） */
data class HomeSummary(
    /** 直近の体重記録。1件もなければ null */
    val latestWeight: WeightEntity? = null,
    /** 当日の摂取カロリー（食事の合計） */
    val intakeKcal: Int = 0,
    /** 当日の消費カロリー（ランニングの合計） */
    val burnedKcal: Int = 0,
    /** 当日の筋トレ種目数 */
    val strengthExerciseCount: Int = 0,
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
    }
}

/** 食事の合計（FR-10）。未入力の項目は合計に含めない */
data class MealTotals(
    val kcal: Int = 0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
)

/** 一覧に出す件数。全件並べても振り返りには使えないため、直近ぶんだけに絞る */
const val RECENT_RECORD_LIMIT = 30

/**
 * 当日サマリー（FR-06）を組み立てる。
 *
 * @param weights 日付降順の体重記録。先頭が最新体重になる
 * @param today 集計する日（yyyy-MM-dd）
 */
fun homeSummaryOf(
    weights: List<WeightEntity>,
    strengths: List<StrengthEntity>,
    runs: List<RunningEntity>,
    meals: List<MealEntity>,
    today: String,
): HomeSummary = HomeSummary(
    latestWeight = weights.firstOrNull(),
    // 未入力の kcal は 0 として足す。合計なので「未入力＝加算なし」で意味が通る
    intakeKcal = meals.filter { it.date == today }.sumOf { it.kcal ?: 0 },
    burnedKcal = runs.filter { it.date == today }.sumOf { it.kcal ?: 0 },
    // 同じ種目を複数セットに分けて記録することがあるため、種目名で重複を除く
    strengthExerciseCount = strengths.filter { it.date == today }.distinctBy { it.ex }.size,
)

/**
 * 体重推移（FR-07）を組み立てる。
 * @param weights 日付降順の体重記録。直近30件を取ってから反転し、古い→新しい順にする
 */
fun weightTrendOf(weights: List<WeightEntity>): WeightTrend =
    WeightTrend(weights.take(WeightTrend.MAX_POINTS).reversed())

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

/**
 * 4種別を1つの表示用リストにまとめる（FR-08）。
 * 並びは日付降順。同じ日付の中は種別の定義順（体重→筋トレ→ラン→食事）で安定させ、
 * 記録を足すたびに同日の並びが入れ替わらないようにする。
 */
fun buildRecentRecords(
    weights: List<WeightEntity>,
    strengths: List<StrengthEntity>,
    runs: List<RunningEntity>,
    meals: List<MealEntity>,
    limit: Int = RECENT_RECORD_LIMIT,
): List<RecentRecord> {
    val all = ArrayList<RecentRecord>(weights.size + strengths.size + runs.size + meals.size)

    weights.mapTo(all) { item ->
        RecentRecord(
            id = item.id,
            kind = RecordKind.WEIGHT,
            date = item.date,
            title = "${formatAmount(item.weight)} kg",
            detail = item.fat?.let { "体脂肪率 ${formatAmount(it)} %" } ?: "",
        )
    }

    strengths.mapTo(all) { item ->
        RecentRecord(
            id = item.id,
            kind = RecordKind.STRENGTH,
            date = item.date,
            title = item.ex,
            detail = listOfNotNull(
                item.weight?.let { "${formatAmount(it)} kg" },
                item.reps?.let { "$it 回" },
                item.sets?.let { "$it セット" },
            ).joinToString("  /  "),
        )
    }

    runs.mapTo(all) { item ->
        RecentRecord(
            id = item.id,
            kind = RecordKind.RUNNING,
            date = item.date,
            title = "${formatAmount(item.dist)} km",
            detail = listOfNotNull(
                item.min?.let { "${formatAmount(it)} 分" },
                formatPace(item.dist, item.min)?.let { "$it /km" },
                item.kcal?.let { "$it kcal" },
            ).joinToString("  /  "),
        )
    }

    meals.mapTo(all) { item ->
        RecentRecord(
            id = item.id,
            kind = RecordKind.MEAL,
            date = item.date,
            title = item.name,
            detail = listOfNotNull(
                item.kcal?.let { "$it kcal" },
                item.p?.let { "P ${formatAmount(it)}g" },
                item.f?.let { "F ${formatAmount(it)}g" },
                item.c?.let { "C ${formatAmount(it)}g" },
            ).joinToString("  /  "),
        )
    }

    return all
        .sortedWith(compareByDescending<RecentRecord> { it.date }.thenBy { it.kind })
        .take(limit)
}
