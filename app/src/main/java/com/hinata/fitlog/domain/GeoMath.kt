package com.hinata.fitlog.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** 地球の半径(m)。Haversine公式で使う */
private const val EARTH_RADIUS_M = 6371000.0

/**
 * 2地点間の距離(m)をHaversine公式で求める。
 * GPSの緯度経度から走行距離を積算するために使う。Android に依存しない素の計算なので
 * 実機なしでテストできる。
 */
fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_M * c
}

/** 経路の描画用に正規化した1点(0..1)。実際のCanvasサイズへの変換は呼び出し側で行う */
data class NormalizedPoint(val x: Float, val y: Float)

/** [projectRoute] が経路として描けると判断する最小の広がり(度)。これ未満はGPSのブレとみなす */
private const val MIN_ROUTE_SPAN_DEGREES = 1e-5

/**
 * GPSで計測した経路(緯度, 経度)の並びを、実際の縦横比を保ったまま 0..1 の正方形に収まるよう
 * 正規化する。地図タイルを使わず経路の「形」だけをCanvasで描くために使う（[haversineMeters]と
 * 同じくAndroidに依存しない素の計算）。
 *
 * 範囲が1回のランの距離程度（数km以内）であることを前提に、平均緯度でcosスケールした単純な
 * 等距離図法で近似する。範囲が狭いランではこの近似で十分な精度になる。
 *
 * @return 点が2件未満なら経路として描けないため空リスト
 */
fun projectRoute(points: List<Pair<Double, Double>>): List<NormalizedPoint> {
    if (points.size < 2) return emptyList()

    val avgLatRad = Math.toRadians(points.map { it.first }.average())
    val lonScale = cos(avgLatRad)

    // 経度はcosスケールして緯度と同じ「距離あたりの角度」に揃える。
    // これをしないと緯度が高い地域で経路が東西に潰れて描かれる。
    val xs = points.map { it.second * lonScale }
    val ys = points.map { it.first }

    val spanX = xs.max() - xs.min()
    val spanY = ys.max() - ys.min()
    val span = maxOf(spanX, spanY)

    // ほぼ同じ地点に留まっている場合（GPSのブレのみ等）は0除算を避け、中央の1点として扱う
    if (span < MIN_ROUTE_SPAN_DEGREES) {
        return points.map { NormalizedPoint(0.5f, 0.5f) }
    }

    // 実際の縦横比を保つため、広い方の軸を0..1いっぱいに使い、狭い方は中央に寄せる
    val offsetX = (span - spanX) / 2
    val offsetY = (span - spanY) / 2
    val minX = xs.min()
    val minY = ys.min()

    return points.indices.map { i ->
        val nx = ((xs[i] - minX + offsetX) / span).toFloat()
        // 緯度は北ほど値が大きいが、Canvasのy軸は下ほど大きいため上下を反転する
        val ny = 1f - ((ys[i] - minY + offsetY) / span).toFloat()
        NormalizedPoint(nx, ny)
    }
}
