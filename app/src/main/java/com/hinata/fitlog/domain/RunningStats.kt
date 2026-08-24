package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.RunningEntity

/**
 * ランニングタブのグラフ（距離の推移）に渡す、直近30件を古い順に並べたデータ。
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

/**
 * ランニングの距離推移を組み立てる。
 * @param records 日付降順の記録。直近30件を取ってから反転し、古い→新しい順にする
 */
fun runningTrendOf(records: List<RunningEntity>): RunningTrend =
    RunningTrend(records.take(RunningTrend.MAX_POINTS).reversed())

/**
 * 指定した月（yyyy-MM）の合計距離(km)。メイン画面のグラフのサマリーで使う。
 * @param records ランニングの記録
 * @param yearMonth 対象月（例: "2026-08"）。date の先頭7文字と前方一致で絞り込む
 */
fun monthlyTotalDistance(records: List<RunningEntity>, yearMonth: String): Double =
    records.filter { it.date.startsWith(yearMonth) }.sumOf { it.dist }
