package com.hinata.fitlog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 表示用の整形（履歴一覧・グラフ・ペース） */
class FormatTest {

    @Test
    fun `小数が不要な値は整数として表示する`() {
        assertEquals("70", formatAmount(70.0))
        assertEquals("70.3", formatAmount(70.25))
    }

    @Test
    fun `期間の増減は符号付きで表示する`() {
        assertEquals("+1.2", formatDelta(1.2))
        assertEquals("-0.8", formatDelta(-0.8))
        assertEquals("±0", formatDelta(0.0))
    }

    @Test
    fun `小数第1位で表示できない差は変化なしとして扱う`() {
        assertEquals("±0", formatDelta(0.01))
        assertEquals("±0", formatDelta(-0.04))
    }

    @Test
    fun `ペースは分と秒に分けて表示する`() {
        assertEquals("5'30\"", formatPace(10.0, 55.0))
    }

    @Test
    fun `秒を丸めてから分に繰り上げるので 0分60秒にならない`() {
        assertEquals("1'00\"", formatPace(1.0, 0.9933))
    }

    @Test
    fun `距離や時間が計算できない値ならペースを出さない`() {
        assertNull(formatPace(null, 30.0))
        assertNull(formatPace(5.0, null))
        assertNull(formatPace(0.0, 30.0))
        assertNull(formatPace(Double.NaN, 30.0))
        assertNull(formatPace(5.0, Double.POSITIVE_INFINITY))
    }
}
