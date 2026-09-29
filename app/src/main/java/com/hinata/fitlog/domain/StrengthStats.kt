package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 筋トレ記録の集計。Android に依存しない素の Kotlin で書いてあるので、
 * 画面や ViewModel を持ち出さずにそのまま動かして確かめられる。
 *
 * 重量・回数はセットごとの任意入力なので、未入力（null）が混ざる前提で書く。
 * 合計系は「未入力＝加算なし」として 0 で足し、最大値系は計算できるセットが
 * 1件も無ければ null を返す。
 */

/** 1日1種目分の集計。選択日のカードに出す5指標 */
data class ExerciseStats(
    val ex: String,
    val part: BodyPart?,
    /** セット数の合計 */
    val sets: Int,
    /** ボリューム = Σ(セットごとの 重量 × 回数) */
    val volume: Double,
    /** 推定1RM（Epley式）の最大値。計算できるセットが無ければ null */
    val oneRepMax: Double?,
    /** 総レップ数 = Σ(セットごとの回数) */
    val totalReps: Int,
    /** 最大レップ数。回数が全セット未入力なら null */
    val maxReps: Int?,
    /** この集計の元になった記録のID。削除に使う */
    val recordIds: List<String>,
)

/** 選択日の記録まとめ */
data class DayStrength(
    val date: String,
    val totalVolume: Double,
    val exercises: List<ExerciseStats>,
)

/** カレンダーの日付セルに出す、部位ごとの種目数バッジ */
data class PartBadge(val part: BodyPart?, val exerciseCount: Int)

/** 種目選択画面に出す1行分（種目名とその部位） */
data class ExerciseRef(val ex: String, val part: BodyPart?)

/** ボリューム = Σ(セットごとの 重量 × 回数)。未入力を含むセットは 0 として足す */
fun volumeOf(records: List<StrengthRecordWithSets>): Double = records.sumOf { r ->
    r.sets.sumOf { (it.weight ?: 0.0) * (it.reps ?: 0).toDouble() }
}

/**
 * 推定1RM（Epley式 `重量 × (1 + 回数 / 30)`）の最大値。
 * 重量か回数が未入力のセットは計算できないため対象外。1件も計算できなければ null。
 */
fun oneRepMaxOf(records: List<StrengthRecordWithSets>): Double? = records
    .flatMap { it.sets }
    .mapNotNull { s ->
        val weight = s.weight ?: return@mapNotNull null
        val reps = s.reps ?: return@mapNotNull null
        weight * (1.0 + reps / 30.0)
    }
    .maxOrNull()

/** 総レップ数 = Σ(セットごとの回数)。未入力は 0 */
fun totalRepsOf(records: List<StrengthRecordWithSets>): Int =
    records.sumOf { r -> r.sets.sumOf { it.reps ?: 0 } }

/** 最大レップ数。回数が全セット未入力なら null */
fun maxRepsOf(records: List<StrengthRecordWithSets>): Int? =
    records.flatMap { it.sets }.mapNotNull { it.reps }.maxOrNull()

/** セット数の合計 */
fun setCountOf(records: List<StrengthRecordWithSets>): Int = records.sumOf { it.sets.size }

/** 同じ種目の記録から部位を決める。部位カラムが空でも種目名から引ける記録があればそれを使う */
fun partOfExercise(records: List<StrengthRecordWithSets>): BodyPart? =
    records.firstNotNullOfOrNull { bodyPartOf(it.record) }

/** 同じ種目の記録をまとめて5指標にする */
fun exerciseStatsOf(ex: String, records: List<StrengthRecordWithSets>): ExerciseStats = ExerciseStats(
    ex = ex,
    part = partOfExercise(records),
    sets = setCountOf(records),
    volume = volumeOf(records),
    oneRepMax = oneRepMaxOf(records),
    totalReps = totalRepsOf(records),
    maxReps = maxRepsOf(records),
    recordIds = records.map { it.record.id },
)

/**
 * 選択日の記録をまとめる。種目は部位順（その他は最後）→ 種目名順に並べる。
 * 入力順は記録の追加順に左右されるため、並びが毎回変わらないようにしている。
 */
fun dayStrengthOf(records: List<StrengthRecordWithSets>, date: String): DayStrength {
    val ofDay = records.filter { it.record.date == date }
    val exercises = ofDay.groupBy { it.record.ex }
        .map { (ex, list) -> exerciseStatsOf(ex, list) }
        .sortedWith(compareBy({ it.part?.ordinal ?: Int.MAX_VALUE }, { it.ex }))
    return DayStrength(date = date, totalVolume = volumeOf(ofDay), exercises = exercises)
}

/** 日付ごとの部位バッジ。カレンダーは月内の全日を描くため、1日ずつ絞らずまとめて作る */
fun partBadgesByDate(records: List<StrengthRecordWithSets>): Map<String, List<PartBadge>> =
    records.groupBy { it.record.date }.mapValues { (_, ofDay) ->
        ofDay.groupBy { bodyPartOf(it.record) }
            .map { (part, list) -> PartBadge(part, list.map { it.record.ex }.distinct().size) }
            .sortedBy { it.part?.ordinal ?: Int.MAX_VALUE }
    }

/** 種目ごとの最終実施日（yyyy-MM-dd）。日付文字列は辞書順＝日付順なので最大値がそのまま最新 */
fun lastPerformedByExercise(records: List<StrengthRecordWithSets>): Map<String, String> =
    records.groupBy { it.record.ex }.mapValues { (_, list) -> list.maxOf { it.record.date } }

/**
 * 最終実施日からの経過の文言。未実施（null）や読めない日付は null を返して何も出さない。
 * 基準日を引数で受けるのは、日付をまたいだときに表示がずれないようにするため。
 * 未来日の記録は「今日」に寄せる（「-3日前」と出しても読めないため）。
 */
fun elapsedLabelOf(lastDate: String?, today: LocalDate): String? {
    if (lastDate == null) return null
    val date = runCatching { LocalDate.parse(lastDate) }.getOrNull() ?: return null
    val days = ChronoUnit.DAYS.between(date, today)
    return if (days <= 0L) "今日" else "${days}日前"
}

/**
 * よくやる種目。記録件数の多い順、同数なら最終実施日が新しい順に並べる。
 * @param limit 返す最大件数
 */
fun frequentExercises(records: List<StrengthRecordWithSets>, limit: Int = 20): List<ExerciseRef> = records
    .groupBy { it.record.ex }
    .entries
    .sortedWith(
        compareByDescending<Map.Entry<String, List<StrengthRecordWithSets>>> { it.value.size }
            .thenByDescending { entry -> entry.value.maxOf { it.record.date } }
    )
    .take(limit)
    .map { (ex, list) -> ExerciseRef(ex, partOfExercise(list)) }

/**
 * 最大負荷 = その記録で扱った一番重い重量。回数に関わらず重量だけで見る。
 * 重量が全セット未入力なら null。
 */
fun maxWeightOf(records: List<StrengthRecordWithSets>): Double? =
    records.flatMap { it.sets }.mapNotNull { it.weight }.maxOrNull()

/**
 * 種目別推移グラフの1日分。
 * 重量や回数が未入力の日もあるため、計算できない指標は null にしてグラフに点を打たない。
 */
data class ExerciseTrendPoint(
    val date: LocalDate,
    /** ボリューム。重量と回数がそろったセットが1つも無ければ null（0 と描くと「やっていない」に見えるため） */
    val volume: Double?,
    val oneRepMax: Double?,
    val maxWeight: Double?,
)

/**
 * 種目別推移の表示期間。選んだ月の月末から数えて何ヶ月分を出すか。
 */
enum class ExerciseTrendPeriod(val label: String, val months: Long) {
    ONE_MONTH("1ヶ月", 1),
    THREE_MONTHS("3ヶ月", 3),
    SIX_MONTHS("6ヶ月", 6),
    ONE_YEAR("1年", 12),
    ;

    /** 基準月 [endMonth] を最後の月として含む期間（初日, 末日）。例: 2026年9月・1年 → 2025-10-01〜2026-09-30 */
    fun rangeEndingAt(endMonth: YearMonth): Pair<LocalDate, LocalDate> =
        endMonth.minusMonths(months - 1).atDay(1) to endMonth.atEndOfMonth()
}

/**
 * 1種目の記録を日ごとにまとめて推移にする。日付の古い順。
 * 同じ日に同じ種目を複数回記録していても1点にまとめる（カレンダーのカードと同じ単位）。
 * 日付が読めない記録は位置を決められないので除く。
 */
fun exerciseTrendOf(records: List<StrengthRecordWithSets>, ex: String): List<ExerciseTrendPoint> = records
    .filter { it.record.ex.trim() == ex.trim() }
    .groupBy { it.record.date }
    .mapNotNull { (date, ofDay) ->
        val day = runCatching { LocalDate.parse(date) }.getOrNull() ?: return@mapNotNull null
        val hasVolume = ofDay.any { r -> r.sets.any { it.weight != null && it.reps != null } }
        ExerciseTrendPoint(
            date = day,
            volume = if (hasVolume) volumeOf(ofDay) else null,
            oneRepMax = oneRepMaxOf(ofDay),
            maxWeight = maxWeightOf(ofDay),
        )
    }
    .sortedBy { it.date }
