package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.RunningEntity
import java.time.LocalDate

/**
 * ランニングタブのグラフ（距離・スピードの推移）に渡す、直近30件を古い順に並べたデータ。
 * 構造は [WeightTrend] と同じ。
 */
data class RunningTrend(
    /** 古い→新しい順の記録。グラフは左から右に時間が進む */
    val points: List<RunningEntity> = emptyList(),
) {
    companion object {
        const val MAX_POINTS = 30
    }
}

/** グラフの表示期間。体重・筋トレの推移グラフと同じ4段階を切り替える */
enum class RunningTrendPeriod(val label: String) {
    ONE_MONTH("1ヶ月"),
    THREE_MONTHS("3ヶ月"),
    ONE_YEAR("1年"),
    ALL("全期間"),
}

/** グラフで見る指標。距離はそのままの推移、スピードはその回の平均速度(km/h)の推移 */
enum class RunningMetric(val label: String) {
    DISTANCE("距離"),
    SPEED("スピード"),
}

/**
 * 選択中の指標での、その記録のグラフ用の値。
 * スピードは時間(min)が未入力だと計算できず null になるため、呼び出し側で
 * null の記録をグラフの対象から除く（[speedKmh] 参照）。
 */
fun RunningMetric.valueFor(record: RunningEntity): Double? = when (this) {
    RunningMetric.DISTANCE -> record.dist
    RunningMetric.SPEED -> speedKmh(record.dist, record.min)
}

/**
 * この回の平均スピード(km/h)。距離(km)と時間(分)から算出する。
 * ペース（分/kmで小さいほど速い）ではなくスピードにしているのは、折れ線が上に伸びる＝
 * 速くなった、とそのまま読めるようにするため。
 * 時間が未入力・0以下・非有限値なら計算できないため null（[formatPace] と同じ判定を共有する）。
 */
fun speedKmh(dist: Double, min: Double?): Double? =
    paceSecondsPerKm(dist, min)?.let { secondsPerKm -> 3600.0 / secondsPerKm }

/**
 * ランニングの推移を組み立てる（期間指定なし）。
 * @param records 日付降順の記録。直近30件を取ってから反転し、古い→新しい順にする
 */
fun runningTrendOf(records: List<RunningEntity>): RunningTrend =
    RunningTrend(records.take(RunningTrend.MAX_POINTS).reversed())

/**
 * 表示期間で絞ったランニングの推移を組み立てる。
 * date は yyyy-MM-dd 固定で辞書順＝日付順のため、カットオフを文字列にして比較するだけで絞り込める。
 * 全期間を選んでも件数が際限なく増えないよう、他の期間と同じく直近30件までに絞る。
 *
 * @param records 日付降順の記録
 * @param period 表示期間
 * @param today 基準日。テストで固定できるよう引数にしている
 */
fun runningTrendOf(
    records: List<RunningEntity>,
    period: RunningTrendPeriod,
    today: LocalDate = LocalDate.now(),
): RunningTrend {
    val cutoff = when (period) {
        RunningTrendPeriod.ONE_MONTH -> today.minusMonths(1)
        RunningTrendPeriod.THREE_MONTHS -> today.minusMonths(3)
        RunningTrendPeriod.ONE_YEAR -> today.minusYears(1)
        RunningTrendPeriod.ALL -> null
    }?.toString()
    val filtered = if (cutoff == null) records else records.filter { it.date >= cutoff }
    return RunningTrend(filtered.take(RunningTrend.MAX_POINTS).reversed())
}

/**
 * 指定した月（yyyy-MM）の合計距離(km)。メイン画面のグラフのサマリーで使う。
 * @param records ランニングの記録
 * @param yearMonth 対象月（例: "2026-08"）。date の先頭7文字と前方一致で絞り込む
 */
fun monthlyTotalDistance(records: List<RunningEntity>, yearMonth: String): Double =
    records.filter { it.date.startsWith(yearMonth) }.sumOf { it.dist }
