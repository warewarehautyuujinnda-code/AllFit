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

    @Test
    fun `点が2件未満なら経路として描けない`() {
        assertEquals(0, projectRoute(emptyList()).size)
        assertEquals(0, projectRoute(listOf(35.0 to 139.0)).size)
    }

    @Test
    fun `真北に進んだ経路は下から上に描かれる`() {
        val route = projectRoute(listOf(35.0 to 139.0, 35.01 to 139.0))
        assertEquals(0.5f, route[0].x, 0.001f)
        assertEquals(1f, route[0].y, 0.001f)
        assertEquals(0.5f, route[1].x, 0.001f)
        assertEquals(0f, route[1].y, 0.001f)
    }

    @Test
    fun `真東に進んだ経路は左から右に描かれる`() {
        val route = projectRoute(listOf(35.0 to 139.0, 35.0 to 139.01))
        assertEquals(0f, route[0].x, 0.001f)
        assertEquals(0.5f, route[0].y, 0.001f)
        assertEquals(1f, route[1].x, 0.001f)
        assertEquals(0.5f, route[1].y, 0.001f)
    }

    @Test
    fun `同じ地点に留まる経路は中央にまとまる`() {
        val route = projectRoute(listOf(35.0 to 139.0, 35.0 to 139.0, 35.0 to 139.0))
        route.forEach {
            assertEquals(0.5f, it.x, 0.001f)
            assertEquals(0.5f, it.y, 0.001f)
        }
    }

    @Test
    fun `往復した経路は始点と終点が同じ座標になる`() {
        val route = projectRoute(listOf(35.0 to 139.0, 35.01 to 139.01, 35.0 to 139.0))
        assertEquals(route[0].x, route[2].x, 0.001f)
        assertEquals(route[0].y, route[2].y, 0.001f)
    }
}
