package com.hinata.fitlog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {

    @Test
    fun `同じ地点なら距離は0`() {
        assertEquals(0.0, haversineMeters(35.681236, 139.767125, 35.681236, 139.767125), 0.001)
    }

    @Test
    fun `既知の2地点間の距離が概ね正しい`() {
        // 東京駅 -> 新宿駅。実距離は直線で約6.4km
        val meters = haversineMeters(35.681236, 139.767125, 35.690921, 139.700258)
        assertTrue("計算結果: $meters", meters in 6000.0..6700.0)
    }

    @Test
    fun `緯度だけずらした距離が妥当な範囲になる`() {
        // 緯度1度はおよそ111km
        val meters = haversineMeters(35.0, 139.0, 36.0, 139.0)
        assertTrue("計算結果: $meters", meters in 110000.0..112000.0)
    }
}
