package com.hinata.fitlog.domain

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * 体重グラフ（FR-07）の縦軸。
 *
 * @property min 軸の下端(kg)。一番下の横罫線と同じ値
 * @property max 軸の上端(kg)。一番上の横罫線と同じ値
 * @property lines 横罫線を引く値（下から順）
 * @property labels 数値を添える値。[lines] の部分集合
 */
data class WeightAxis(
    val min: Double,
    val max: Double,
    val lines: List<Double>,
    val labels: List<Double>,
)

/** 横罫線の区間数の上限。これを超えない範囲でいちばん細かい刻みを選ぶ */
internal const val MAX_AXIS_INTERVALS = 8

/** 数値を全部の罫線に添えられる区間数の上限。超えたら数値同士が詰まるので1本おきにする */
internal const val MAX_LABELED_INTERVALS = 6

/**
 * 表示する値の最小・最大から、体重グラフの縦軸を決める。
 *
 * 体重の推移は「66kgを切った」「68kg台に戻った」のように整数kgを基準に読むため、
 * 横罫線は整数kgの位置に引く。値幅が広くて1kg刻みでは線が詰まりすぎるときだけ
 * 2・5・10kg…刻みに広げる（どの刻みでも罫線は必ず整数kgに乗る）。
 * 軸の上下端も刻みにそろえるので、グラフの一番上と一番下は必ず数値つきの罫線になる。
 *
 * - 値がすべて同じで上下端が一致してしまう（例: 68.0kgだけ）ときは、上下に1刻みずつ
 *   広げて線がグラフの真ん中に来るようにする。
 * - 体重・目標体重は上限を検証していない（[parseRequiredDouble] は正の有限数なら通す）ため、
 *   極端な値で刻みが求まらないときは実測の最小・最大をそのまま軸にする安全弁を持つ。
 */
fun weightAxisOf(dataMin: Double, dataMax: Double): WeightAxis {
    var magnitude = 1.0
    while (magnitude < 1e300) {
        for (unit in STEP_UNITS) {
            val step = unit * magnitude
            var lo = floor(dataMin / step) * step
            var hi = ceil(dataMax / step) * step
            if (hi - lo < step / 2) {
                lo -= step
                hi += step
            }
            val intervals = ((hi - lo) / step).roundToInt()
            if (intervals <= MAX_AXIS_INTERVALS) return axisOf(lo, hi, step, intervals)
        }
        magnitude *= 10
    }
    val fallback = listOf(dataMin, dataMax).distinct()
    return WeightAxis(dataMin, dataMax, fallback, fallback)
}

private val STEP_UNITS = doubleArrayOf(1.0, 2.0, 5.0)

private fun axisOf(lo: Double, hi: Double, step: Double, intervals: Int): WeightAxis {
    val lines = (0..intervals).map { lo + step * it }
    val labels = if (intervals <= MAX_LABELED_INTERVALS) {
        lines
    } else {
        // 1本おきに数値を添える。どこから数えても同じ値に付くよう、刻みの偶数倍（1kg刻みなら偶数kg）を選ぶ
        lines.filter { (it / step).roundToLong() % 2 == 0L }
    }
    return WeightAxis(lo, hi, lines, labels)
}
