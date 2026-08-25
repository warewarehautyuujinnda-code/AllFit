package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.RunningEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunningStatsTest {

    // DAO と同じ日付降順で渡す。月間合計のフィルタを検証できるよう月をまたがせてある
    private val records = listOf(
        RunningEntity(id = "r2", date = "2026-08-20", dist = 5.2, min = 28.0),
        RunningEntity(id = "r1", date = "2026-07-01", dist = 5.0, min = 30.0),
    )

    @Test
    fun `推移は古い順に並べ替える`() {
        assertEquals(listOf("r1", "r2"), runningTrendOf(records).points.map { it.id })
    }

    @Test
    fun `推移は直近30件までに絞る`() {
        val many = (1..40).map {
            RunningEntity(id = "x$it", date = "2026-08-20", dist = 5.0)
        }
        assertEquals(RunningTrend.MAX_POINTS, runningTrendOf(many).points.size)
    }

    @Test
    fun `記録が0件でも推移は空で落ちない`() {
        assertEquals(0, runningTrendOf(emptyList()).points.size)
    }

    @Test
    fun `月間合計距離は日付の年月が一致する記録だけを足す`() {
        assertEquals(5.2, monthlyTotalDistance(records, "2026-08"), 1e-9)
    }

    @Test
    fun `一致する記録がなければ月間合計は0`() {
        assertEquals(0.0, monthlyTotalDistance(records, "1999-01"), 1e-9)
    }

    @Test
    fun `期間1ヶ月はちょうど1ヶ月前の記録まで含める`() {
        val today = LocalDate.of(2026, 8, 25)
        val periodRecords = listOf(
            RunningEntity(id = "in", date = "2026-07-25", dist = 5.0),
            RunningEntity(id = "out", date = "2026-07-24", dist = 5.0),
        )
        assertEquals(
            listOf("in"),
            runningTrendOf(periodRecords, RunningTrendPeriod.ONE_MONTH, today).points.map { it.id },
        )
    }

    @Test
    fun `期間3ヶ月はちょうど3ヶ月前の記録まで含める`() {
        val today = LocalDate.of(2026, 8, 25)
        val periodRecords = listOf(
            RunningEntity(id = "in", date = "2026-05-25", dist = 5.0),
            RunningEntity(id = "out", date = "2026-05-24", dist = 5.0),
        )
        assertEquals(
            listOf("in"),
            runningTrendOf(periodRecords, RunningTrendPeriod.THREE_MONTHS, today).points.map { it.id },
        )
    }

    @Test
    fun `期間1年はちょうど1年前の記録まで含める`() {
        val today = LocalDate.of(2026, 8, 25)
        val periodRecords = listOf(
            RunningEntity(id = "in", date = "2025-08-25", dist = 5.0),
            RunningEntity(id = "out", date = "2025-08-24", dist = 5.0),
        )
        assertEquals(
            listOf("in"),
            runningTrendOf(periodRecords, RunningTrendPeriod.ONE_YEAR, today).points.map { it.id },
        )
    }

    @Test
    fun `期間が全期間なら日付で絞り込まない`() {
        val today = LocalDate.of(2026, 8, 25)
        val periodRecords = listOf(
            RunningEntity(id = "recent", date = "2026-08-01", dist = 5.0),
            RunningEntity(id = "old", date = "2000-01-01", dist = 5.0),
        )
        assertEquals(
            listOf("old", "recent"),
            runningTrendOf(periodRecords, RunningTrendPeriod.ALL, today).points.map { it.id },
        )
    }

    @Test
    fun `全期間でも推移は直近30件までに絞る`() {
        val many = (1..40).map { RunningEntity(id = "x$it", date = "2026-08-20", dist = 5.0) }
        assertEquals(RunningTrend.MAX_POINTS, runningTrendOf(many, RunningTrendPeriod.ALL).points.size)
    }

    @Test
    fun `期間を指定しても記録が0件なら推移は空で落ちない`() {
        assertEquals(0, runningTrendOf(emptyList(), RunningTrendPeriod.ONE_MONTH).points.size)
    }

    @Test
    fun `スピードは距離を時間(時)で割ったkmhになる`() {
        // 5km を25分（=25/60時間）で走った場合の時速
        assertEquals(12.0, speedKmh(5.0, 25.0)!!, 1e-9)
    }

    @Test
    fun `時間が未入力・0以下・非有限値ならスピードは計算できない`() {
        assertNull(speedKmh(5.0, null))
        assertNull(speedKmh(5.0, 0.0))
        assertNull(speedKmh(5.0, -1.0))
        assertNull(speedKmh(0.0, 30.0))
        assertNull(speedKmh(5.0, Double.NaN))
    }

    @Test
    fun `指標が距離ならその記録の距離をそのまま返す`() {
        val r = RunningEntity(id = "r", date = "2026-08-20", dist = 5.2, min = 30.0)
        assertEquals(5.2, RunningMetric.DISTANCE.valueFor(r)!!, 1e-9)
    }

    @Test
    fun `指標がスピードで時間が未入力なら値が求まらない`() {
        val r = RunningEntity(id = "r", date = "2026-08-20", dist = 5.2, min = null)
        assertNull(RunningMetric.SPEED.valueFor(r))
    }
}
