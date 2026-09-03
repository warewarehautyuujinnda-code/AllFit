package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import com.hinata.fitlog.data.entity.WeeklyPlanEntity
import com.hinata.fitlog.data.entity.WeeklyStrengthTargetEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanProgressTest {
    private fun record(date: String, ex: String) =
        StrengthRecordWithSets(StrengthEntity(date = date, ex = ex), emptyList())

    @Test fun `週の途中の日付から月曜日を求める`() =
        assertEquals("2026-08-31", weekStartOf(LocalDate.of(2026, 9, 3)))

    @Test fun `月曜日はそのまま週初日になる`() =
        assertEquals("2026-08-31", weekStartOf(LocalDate.of(2026, 8, 31)))

    @Test fun `週初日が一致する計画を返す`() {
        val plans = listOf(WeeklyPlanEntity("2026-08-24"), WeeklyPlanEntity("2026-08-31", targetRunningKm = 10.0))
        assertEquals(10.0, weeklyPlanFor(plans, "2026-08-31")?.targetRunningKm)
    }

    @Test fun `該当する計画がなければnullを返す`() =
        assertNull(weeklyPlanFor(listOf(WeeklyPlanEntity("2026-08-24")), "2026-08-31"))

    @Test fun `週内に記録した種目は実施済みになる`() {
        val targets = listOf(
            WeeklyStrengthTargetEntity("2026-08-31", "ベンチプレス"),
            WeeklyStrengthTargetEntity("2026-08-31", "スクワット"),
        )
        val result = strengthProgressOf(targets, listOf(record("2026-09-02", "ベンチプレス")), "2026-08-31")
        assertEquals(2, result.plannedCount)
        assertEquals(1, result.doneCount)
        assertEquals(listOf("スクワット"), result.pendingExercises)
    }

    @Test fun `週外の筋トレ記録は実施済みに数えない`() {
        val targets = listOf(WeeklyStrengthTargetEntity("2026-08-31", "ベンチプレス"))
        val result = strengthProgressOf(targets, listOf(record("2026-08-30", "ベンチプレス"), record("2026-09-07", "ベンチプレス")), "2026-08-31")
        assertEquals(0, result.doneCount)
    }

    @Test fun `種目名の前後空白を無視して一致判定する`() {
        val targets = listOf(WeeklyStrengthTargetEntity("2026-08-31", " ベンチプレス "))
        assertEquals(1, strengthProgressOf(targets, listOf(record("2026-09-01", "ベンチプレス")), "2026-08-31").doneCount)
    }

    @Test fun `別の週の計画は数えない`() {
        val targets = listOf(WeeklyStrengthTargetEntity("2026-08-24", "ベンチプレス"))
        assertEquals(0, strengthProgressOf(targets, listOf(record("2026-09-01", "ベンチプレス")), "2026-08-31").plannedCount)
    }

    @Test fun `週内のラン距離を合計する`() {
        val records = listOf(RunningEntity(date = "2026-08-31", dist = 5.0), RunningEntity(date = "2026-09-03", dist = 3.5), RunningEntity(date = "2026-09-06", dist = 2.0))
        assertEquals(10.5, runningDistanceOf(records, "2026-08-31"), 0.001)
    }

    @Test fun `週外のラン記録は合計しない`() {
        val records = listOf(RunningEntity(date = "2026-08-30", dist = 5.0), RunningEntity(date = "2026-09-07", dist = 5.0))
        assertEquals(0.0, runningDistanceOf(records, "2026-08-31"), 0.001)
    }
}
