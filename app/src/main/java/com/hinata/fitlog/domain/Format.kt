package com.hinata.fitlog.domain

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * 記録の数値を表示用の文字列にする。
 * Double をそのまま表示すると「70.0 kg」「12 回」が「12.0 回」のように見えてしまうため、
 * 小数が不要な値は整数として、必要な値は小数第1位までで表示する。
 */
fun formatAmount(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format(Locale.US, "%.1f", value)

/**
 * 大きい数を3桁区切りの整数で表示する（例: 3272.0 → 3,272）。
 * 筋トレのボリュームや推定1RM は kg 単位で桁が大きくなるため、小数を落として読みやすさを取る。
 */
fun formatGrouped(value: Double): String {
    if (!value.isFinite()) return "-"
    return String.format(Locale.US, "%,d", value.roundToLong())
}

/**
 * 増減を符号付きで表示する（例: +1.2 / -0.8 / ±0）。体重推移の期間増減（FR-07）で使う。
 */
fun formatDelta(value: Double): String = when {
    // 表示は小数第1位までのため、それより小さい差は「変化なし」として ±0 と出す
    abs(value) < 0.05 -> "±0"
    value > 0 -> "+${formatAmount(value)}"
    else -> "-${formatAmount(abs(value))}"
}

/**
 * 増減を向きの分かる矢印付きで表示する（例: ↗ 1.2 / ↘ 0.8 / → 0.0）。
 * 体重推移のサマリー「最近の傾向」で使う。増減の大きさは常に小数第1位まで出す。
 */
fun formatTrend(value: Double): String {
    // formatDelta と同じく、小数第1位に出ない差は「変化なし」として扱う
    val arrow = when {
        abs(value) < 0.05 -> "→"
        value > 0 -> "↗"
        else -> "↘"
    }
    return "$arrow ${String.format(Locale.US, "%.1f", abs(value))}"
}

/**
 * 記録日（yyyy-MM-dd）をグラフの軸などに収まる短い形（M/d）にする。
 * 想定外の形式はそのまま返す。
 */
fun formatShortDate(date: String): String {
    val parts = date.split("-")
    if (parts.size != 3) return date
    val month = parts[1].trimStart('0')
    val day = parts[2].trimStart('0')
    if (month.isEmpty() || day.isEmpty()) return date
    return "$month/$day"
}

/**
 * ペース（分/km）を 5'30" 形式で返す（FR-03）。
 * 距離・時間のどちらかが未入力・0以下・非有限値の場合は計算できないため null を返す。
 *
 * 保存前の入力中の文字列からも呼ばれるため、検証を通っていない値が渡ってくる前提で書く。
 * NaN は大小比較がすべて false になり 0以下チェックをすり抜けるうえ、
 * roundToInt() に渡すと例外になるので isFinite() で先に弾く。
 */
fun formatPace(dist: Double?, min: Double?): String? {
    if (dist == null || min == null) return null
    if (!dist.isFinite() || !min.isFinite()) return null
    if (dist <= 0.0 || min <= 0.0) return null
    // 秒に丸めてから分と秒に分けることで、59.6秒が「0'60"」になるのを防ぐ
    val totalSeconds = (min * 60.0 / dist).roundToInt()
    return String.format(Locale.US, "%d'%02d\"", totalSeconds / 60, totalSeconds % 60)
}
