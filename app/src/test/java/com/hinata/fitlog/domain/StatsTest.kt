package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.MealEntity
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.data.entity.WeightEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** ホームの集計（FR-06〜08）と食事の当日合計（FR-10） */
class StatsTest {

    private val today = "2026-08-11"

    // DAO と同じ日付降順で渡す
    private val weights = listOf(
        WeightEntity(id = "w2", date = "2026-08-11", weight = 70.0, fat = 18.0),
        WeightEntity(id = "w1", date = "2026-08-01", weight = 71.2),
    )
    private val strengths = listOf(
        StrengthEntity(id = "s1", date = today, ex = "ベンチプレス", weight = 60.0, reps = 10, sets = 3),
        StrengthEntity(id = "s2", date = today, ex = "ベンチプレス", weight = 65.0, reps = 8, sets = 1),
        StrengthEntity(id = "s3", date = today, ex = "スクワット"),
        StrengthEntity(id = "s4", date = "2026-08-10", ex = "デッドリフト"),
    )
    private val runs = listOf(
        RunningEntity(id = "r1", date = today, dist = 5.0, min = 27.5, kcal = 300),
        RunningEntity(id = "r2", date = today, dist = 2.0),
    )
    private val meals = listOf(
        MealEntity(id = "m1", date = today, name = "朝食", kcal = 500, p = 30.0, f = 10.0, c = 60.0),
        MealEntity(id = "m2", date = today, name = "昼食"),
        MealEntity(id = "m3", date = "2026-08-10", name = "夕食", kcal = 800),
    )

    // ---- FR-06 ホームサマリー ----

    @Test
    fun `サマリーは当日の記録だけを集計する`() {
        val summary = homeSummaryOf(weights, strengths, runs, meals, today)
        assertEquals(500, summary.intakeKcal)
        assertEquals(300, summary.burnedKcal)
    }

    @Test
    fun `最新体重は日付降順の先頭を使う`() {
        assertEquals(70.0, homeSummaryOf(weights, strengths, runs, meals, today).latestWeight?.weight)
    }

    @Test
    fun `同じ種目を複数セットに分けて記録しても1種目として数える`() {
        assertEquals(2, homeSummaryOf(weights, strengths, runs, meals, today).strengthExerciseCount)
    }

    @Test
    fun `記録が1件もなくてもサマリーは作れる`() {
        val summary = homeSummaryOf(emptyList(), emptyList(), emptyList(), emptyList(), today)
        assertNull(summary.latestWeight)
        assertEquals(0, summary.intakeKcal)
        assertEquals(0, summary.burnedKcal)
        assertEquals(0, summary.strengthExerciseCount)
    }

    // ---- FR-07 体重推移グラフ ----

    @Test
    fun `推移は古い順に並べ替える`() {
        assertEquals(listOf("w1", "w2"), weightTrendOf(weights).points.map { it.id })
    }

    @Test
    fun `期間の増減は最初と最後の差`() {
        assertEquals(70.0 - 71.2, weightTrendOf(weights).delta!!, 1e-9)
    }

    @Test
    fun `記録が0件や1件でも増減は出さずに落ちない`() {
        assertNull(weightTrendOf(emptyList()).delta)
        assertEquals(0, weightTrendOf(emptyList()).points.size)
        assertNull(weightTrendOf(weights.take(1)).delta)
    }

    @Test
    fun `推移は直近30件までに絞る`() {
        val many = (1..40).map { WeightEntity(id = "x$it", date = "2026-08-11", weight = 70.0 + it) }
        assertEquals(WeightTrend.MAX_POINTS, weightTrendOf(many).points.size)
    }

    // ---- FR-08 最近の記録一覧 ----

    @Test
    fun `4種別すべてが1つのリストに入る`() {
        val recent = buildRecentRecords(weights, strengths, runs, meals)
        assertEquals(weights.size + strengths.size + runs.size + meals.size, recent.size)
        assertEquals(RecordKind.entries.toSet(), recent.map { it.kind }.toSet())
    }

    @Test
    fun `日付降順で並ぶ`() {
        val dates = buildRecentRecords(weights, strengths, runs, meals).map { it.date }
        assertEquals(dates.sortedDescending(), dates)
    }

    @Test
    fun `同じ日付の中は種別の定義順で安定する`() {
        val kinds = buildRecentRecords(weights, strengths, runs, meals)
            .filter { it.date == today }
            .map { it.kind }
            .distinct()
        assertEquals(
            listOf(RecordKind.WEIGHT, RecordKind.STRENGTH, RecordKind.RUNNING, RecordKind.MEAL),
            kinds,
        )
    }

    @Test
    fun `件数が多いときは直近ぶんだけに絞る`() {
        val many = (1..40).map { WeightEntity(id = "x$it", date = "2026-08-11", weight = 70.0) }
        assertEquals(RECENT_RECORD_LIMIT, buildRecentRecords(many, emptyList(), emptyList(), emptyList()).size)
    }

    @Test
    fun `任意項目が未入力の記録は詳細を空にする`() {
        val recent = buildRecentRecords(weights, strengths, runs, meals)
        assertEquals("", recent.first { it.id == "s3" }.detail)
    }

    @Test
    fun `ランの詳細には時間とペースと消費カロリーが並ぶ`() {
        val recent = buildRecentRecords(weights, strengths, runs, meals)
        assertEquals("27.5 分  /  5'30\" /km  /  300 kcal", recent.first { it.id == "r1" }.detail)
    }

    // ---- FR-10 食事の当日合計 ----

    @Test
    fun `当日の合計はカロリーと PFC をまとめて出す`() {
        val totals = mealTotalsOf(meals, today)
        assertEquals(500, totals.kcal)
        assertEquals(30.0, totals.protein, 1e-9)
        assertEquals(10.0, totals.fat, 1e-9)
        assertEquals(60.0, totals.carbs, 1e-9)
    }

    @Test
    fun `未入力の項目があっても合計は壊れない`() {
        val onlyBlank = listOf(MealEntity(id = "b", date = today, name = "間食"))
        val totals = mealTotalsOf(onlyBlank, today)
        assertEquals(0, totals.kcal)
        assertEquals(0.0, totals.protein, 1e-9)
    }

    @Test
    fun `記録がない日の合計は0になる`() {
        assertEquals(0, mealTotalsOf(meals, "1999-01-01").kcal)
    }
}
