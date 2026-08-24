package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.RunningEntity
import org.junit.Assert.assertEquals
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
}
