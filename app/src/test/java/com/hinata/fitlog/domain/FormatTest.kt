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
    fun `大きい数は3桁区切りの整数で表示する`() {
        assertEquals("3,272", formatGrouped(3272.0))
        assertEquals("88", formatGrouped(88.0))
        assertEquals("0", formatGrouped(0.0))
        assertEquals("107", formatGrouped(106.66666))
    }

    @Test
    fun `計算できない値は区切り表示にしない`() {
        assertEquals("-", formatGrouped(Double.NaN))
        assertEquals("-", formatGrouped(Double.POSITIVE_INFINITY))
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
    fun `最近の傾向は増減の向きを矢印で示す`() {
        assertEquals("↗ 1.2", formatTrend(1.2))
        assertEquals("↘ 0.8", formatTrend(-0.8))
        assertEquals("→ 0.0", formatTrend(0.0))
    }

    @Test
    fun `小数第1位で表示できない差は傾向なしとして扱う`() {
        assertEquals("→ 0.0", formatTrend(0.01))
        assertEquals("→ 0.0", formatTrend(-0.04))
    }

    @Test
    fun `傾向の大きさは整数でも小数第1位まで出す`() {
        assertEquals("↘ 2.0", formatTrend(-2.0))
    }

    @Test
    fun `記録日はグラフ用に月日だけの短い形にする`() {
        assertEquals("8/1", formatShortDate("2026-08-01"))
        assertEquals("12/25", formatShortDate("2026-12-25"))
    }

    @Test
    fun `想定外の形式の日付はそのまま返す`() {
        assertEquals("", formatShortDate(""))
        assertEquals("2026/08/01", formatShortDate("2026/08/01"))
        assertEquals("2026-08-", formatShortDate("2026-08-"))
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

    @Test
    fun `ペースの秒換算はformatPaceと同じ式で求める`() {
        // formatPace(10.0, 55.0) は 55*60/10=330秒 → "5'30\""
        assertEquals(330.0, paceSecondsPerKm(10.0, 55.0)!!, 1e-9)
    }

    @Test
    fun `距離や時間が計算できない値ならペースの秒換算も出さない`() {
        assertNull(paceSecondsPerKm(null, 30.0))
        assertNull(paceSecondsPerKm(5.0, null))
        assertNull(paceSecondsPerKm(0.0, 30.0))
        assertNull(paceSecondsPerKm(Double.NaN, 30.0))
        assertNull(paceSecondsPerKm(5.0, Double.POSITIVE_INFINITY))
    }

    @Test
    fun `経過時間は分と秒で表示する`() {
        assertEquals("00:00", formatElapsed(0))
        assertEquals("05:09", formatElapsed(309))
        assertEquals("59:59", formatElapsed(3599))
    }

    @Test
    fun `1時間以上は時を含めて表示する`() {
        assertEquals("1:00:00", formatElapsed(3600))
        assertEquals("2:03:04", formatElapsed(2 * 3600 + 3 * 60 + 4))
    }

    @Test
    fun `負の経過時間は0として扱う`() {
        assertEquals("00:00", formatElapsed(-5))
    }
}
