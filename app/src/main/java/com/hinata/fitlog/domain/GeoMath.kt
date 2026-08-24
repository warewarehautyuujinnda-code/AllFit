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
