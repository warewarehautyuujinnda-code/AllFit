package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyUiTest {
    @Test fun `次の日付更新までの時間をローカル日付境界から求める`() {
        val now = ZonedDateTime.of(2026, 9, 6, 23, 59, 30, 0, ZoneId.of("Asia/Tokyo"))
        assertEquals(Duration.ofSeconds(30), durationUntilNextDate(now))
    }

    @Test fun `履歴にもプリセットにもない計画種目をよくやる種目に追加する`() {
        val records = listOf(StrengthRecordWithSets(StrengthEntity(date = "2026-09-01", ex = "スクワット"), emptyList()))
        val rows = exercisePickerRows(records, selectedPart = null, plannedExercises = setOf("ターキッシュゲットアップ"))
        assertEquals(listOf("スクワット", "ターキッシュゲットアップ"), rows.map { it.ex })
        assertEquals(null, rows.last().part)
    }

    @Test fun `計画種目は既存行と重複しない`() {
        val records = listOf(StrengthRecordWithSets(StrengthEntity(date = "2026-09-01", ex = "スクワット"), emptyList()))
        assertEquals(1, exercisePickerRows(records, null, setOf(" スクワット ")).count { it.ex == "スクワット" })
    }

    @Test fun `現在体重が目標より重い場合は目標までの差を表示する`() {
        assertEquals("目標 70 kg（あと 2.5 kg）", weightGoalDifferenceLabel(current = 72.5, goal = 70.0))
    }

    @Test fun `現在体重が目標以下なら達成差を表示する`() {
        assertEquals("目標 70 kg（1 kg 達成）", weightGoalDifferenceLabel(current = 69.0, goal = 70.0))
    }

    @Test fun `現在体重がなければ差分文言も表示しない`() {
        assertEquals(null, weightGoalDifferenceLabel(current = null, goal = 70.0))
    }
}
