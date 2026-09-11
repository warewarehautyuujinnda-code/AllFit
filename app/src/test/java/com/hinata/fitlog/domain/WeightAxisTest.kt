package com.hinata.fitlog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 体重グラフの縦軸（FR-07） */
class WeightAxisTest {

    @Test
    fun `数kgの増減なら1kg刻みの整数に罫線を引き全部に数値を添える`() {
        val axis = weightAxisOf(66.3, 69.4)
        assertEquals(listOf(66.0, 67.0, 68.0, 69.0, 70.0), axis.lines)
        assertEquals(axis.lines, axis.labels)
    }

    @Test
    fun `軸の上下端は刻みにそろえ一番上と一番下の罫線と一致させる`() {
        val axis = weightAxisOf(66.3, 69.4)
        assertEquals(66.0, axis.min, 0.0)
        assertEquals(70.0, axis.max, 0.0)
    }

    @Test
    fun `1kgに満たない増減でも上下の整数kgで挟む`() {
        val axis = weightAxisOf(68.2, 68.9)
        assertEquals(listOf(68.0, 69.0), axis.lines)
    }

    @Test
    fun `すべて同じ整数kgなら上下に1kgずつ広げて真ん中に置く`() {
        val axis = weightAxisOf(68.0, 68.0)
        assertEquals(listOf(67.0, 68.0, 69.0), axis.lines)
    }

    @Test
    fun `区間が多いときは罫線は1kgごとのまま数値だけ偶数kgに間引く`() {
        // 62〜70kg = 8区間。罫線は全部引き、数値は1本おき
        val axis = weightAxisOf(62.4, 69.8)
        assertEquals((62..70).map { it.toDouble() }, axis.lines)
        assertEquals(listOf(62.0, 64.0, 66.0, 68.0, 70.0), axis.labels)
    }

    @Test
    fun `値幅が広いときは刻みを広げても罫線は整数kgに乗る`() {
        // 目標体重が遠い・全期間で数年分の記録があるなど
        val axis = weightAxisOf(55.0, 82.3)
        assertTrue(axis.lines.size - 1 <= MAX_AXIS_INTERVALS)
        assertTrue(axis.lines.all { it == Math.floor(it) })
        assertTrue(axis.min <= 55.0 && axis.max >= 82.3)
        assertEquals(listOf(55.0, 60.0, 65.0, 70.0, 75.0, 80.0, 85.0), axis.lines)
    }

    @Test
    fun `極端な値でも落ちずに実測の範囲を軸にする`() {
        val axis = weightAxisOf(60.0, 1.7e308)
        assertEquals(60.0, axis.min, 0.0)
        assertEquals(1.7e308, axis.max, 0.0)
    }
}
