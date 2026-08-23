package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.StrengthEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 筋トレの集計（総ボリューム・推定1RM・レップ数・カレンダーのバッジ・最終実施日） */
class StrengthStatsTest {

    private fun record(
        id: String = "x",
        date: String = "2026-08-20",
        ex: String = "ベンチプレス",
        part: String? = null,
        weight: Double? = null,
        reps: Int? = null,
        sets: Int? = null,
    ) = StrengthEntity(id, date, ex, part, weight, reps, sets)

    // ---- 総ボリューム ----

    @Test
    fun `ボリュームは重量と回数とセット数の積を足し合わせる`() {
        val records = listOf(
            record(id = "a", weight = 60.0, reps = 10, sets = 3),
            record(id = "b", weight = 50.0, reps = 12, sets = 2),
        )
        assertEquals(60.0 * 10 * 3 + 50.0 * 12 * 2, volumeOf(records), 1e-9)
    }

    @Test
    fun `未入力を含む記録はボリューム0として足す`() {
        val records = listOf(
            record(id = "a", weight = 60.0, reps = 10, sets = 3),
            record(id = "b", reps = 10, sets = 3),
            record(id = "c", weight = 60.0, sets = 3),
            record(id = "d", weight = 60.0, reps = 10),
        )
        assertEquals(1800.0, volumeOf(records), 1e-9)
    }

    @Test
    fun `記録が0件ならボリュームは0`() {
        assertEquals(0.0, volumeOf(emptyList()), 1e-9)
    }

    // ---- 推定1RM（Epley式） ----

    @Test
    fun `推定1RMは種目内の最大値を取る`() {
        val records = listOf(
            record(id = "a", weight = 60.0, reps = 10, sets = 3),
            record(id = "b", weight = 80.0, reps = 3, sets = 1),
        )
        // 60×(1+10/30)=80.0 と 80×(1+3/30)=88.0 の大きい方
        assertEquals(88.0, oneRepMaxOf(records)!!, 1e-9)
    }

    @Test
    fun `重量か回数が未入力の記録は推定1RMの対象外`() {
        val records = listOf(
            record(id = "a", weight = 100.0, sets = 3),
            record(id = "b", reps = 20, sets = 3),
            record(id = "c", weight = 60.0, reps = 10, sets = 3),
        )
        assertEquals(80.0, oneRepMaxOf(records)!!, 1e-9)
    }

    @Test
    fun `1件も計算できなければ推定1RMは出さない`() {
        assertNull(oneRepMaxOf(emptyList()))
        assertNull(oneRepMaxOf(listOf(record(id = "a"), record(id = "b", sets = 3))))
    }

    @Test
    fun `記録が1件でも推定1RMは出せる`() {
        assertEquals(80.0, oneRepMaxOf(listOf(record(weight = 60.0, reps = 10)))!!, 1e-9)
    }

    // ---- レップ数・セット数 ----

    @Test
    fun `総レップ数は回数とセット数の積の合計`() {
        val records = listOf(
            record(id = "a", weight = 60.0, reps = 10, sets = 3),
            record(id = "b", weight = 50.0, reps = 12, sets = 2),
        )
        assertEquals(54, totalRepsOf(records))
    }

    @Test
    fun `未入力があっても総レップ数とセット数は0として足す`() {
        val records = listOf(
            record(id = "a", reps = 10),
            record(id = "b", sets = 3),
            record(id = "c", reps = 8, sets = 2),
        )
        assertEquals(16, totalRepsOf(records))
        assertEquals(5, setCountOf(records))
    }

    @Test
    fun `最大レップ数は回数が全て未入力なら出さない`() {
        assertEquals(12, maxRepsOf(listOf(record(id = "a", reps = 10), record(id = "b", reps = 12))))
        assertNull(maxRepsOf(listOf(record(id = "a", sets = 3), record(id = "b"))))
        assertNull(maxRepsOf(emptyList()))
    }

    // ---- 選択日の集計 ----

    @Test
    fun `選択日の集計はその日の記録だけを種目ごとにまとめる`() {
        val records = listOf(
            record(id = "a", date = "2026-08-20", ex = "ベンチプレス", weight = 60.0, reps = 10, sets = 3),
            record(id = "b", date = "2026-08-20", ex = "ベンチプレス", weight = 80.0, reps = 3, sets = 1),
            record(id = "c", date = "2026-08-20", ex = "スクワット", weight = 100.0, reps = 5, sets = 2),
            record(id = "d", date = "2026-08-19", ex = "ベンチプレス", weight = 60.0, reps = 10, sets = 3),
        )
        val day = dayStrengthOf(records, "2026-08-20")

        assertEquals(2, day.exercises.size)
        assertEquals(60.0 * 10 * 3 + 80.0 * 3 + 100.0 * 5 * 2, day.totalVolume, 1e-9)

        val bench = day.exercises.first { it.ex == "ベンチプレス" }
        assertEquals(4, bench.sets)
        assertEquals(88.0, bench.oneRepMax!!, 1e-9)
        assertEquals(33, bench.totalReps)
        assertEquals(10, bench.maxReps)
        assertEquals(listOf("a", "b"), bench.recordIds)
    }

    @Test
    fun `記録がない日の集計は空になる`() {
        val day = dayStrengthOf(listOf(record(date = "2026-08-20")), "1999-01-01")
        assertEquals(0.0, day.totalVolume, 1e-9)
        assertTrue(day.exercises.isEmpty())
    }

    @Test
    fun `種目は部位順に並び その他は最後に来る`() {
        val records = listOf(
            record(id = "a", ex = "自作トレ"),
            record(id = "b", ex = "スクワット", part = "leg"),
            record(id = "c", ex = "ベンチプレス", part = "chest"),
        )
        assertEquals(
            listOf("ベンチプレス", "スクワット", "自作トレ"),
            dayStrengthOf(records, "2026-08-20").exercises.map { it.ex },
        )
    }

    // ---- カレンダーのバッジ ----

    @Test
    fun `バッジは日付ごとに部位別の種目数を数える`() {
        val records = listOf(
            record(id = "a", date = "2026-08-20", ex = "ベンチプレス", part = "chest"),
            record(id = "b", date = "2026-08-20", ex = "ベンチプレス", part = "chest"),
            record(id = "c", date = "2026-08-20", ex = "ダンベルフライ", part = "chest"),
            record(id = "d", date = "2026-08-20", ex = "スクワット", part = "leg"),
            record(id = "e", date = "2026-08-03", ex = "懸垂", part = "back"),
        )
        val badges = partBadgesByDate(records)

        // 同じ種目を複数セット記録しても種目数は1
        assertEquals(
            listOf(PartBadge(BodyPart.CHEST, 2), PartBadge(BodyPart.LEG, 1)),
            badges["2026-08-20"],
        )
        assertEquals(listOf(PartBadge(BodyPart.BACK, 1)), badges["2026-08-03"])
        assertNull(badges["2026-08-01"])
    }

    @Test
    fun `部位が未設定でも種目名がマスタにあればその部位として数える`() {
        val badges = partBadgesByDate(listOf(record(id = "a", ex = "ベンチプレス")))
        assertEquals(listOf(PartBadge(BodyPart.CHEST, 1)), badges["2026-08-20"])
    }

    @Test
    fun `部位も種目名も手掛かりが無い記録はその他として数える`() {
        val badges = partBadgesByDate(listOf(record(id = "a", ex = "謎のトレ")))
        assertEquals(listOf(PartBadge(null, 1)), badges["2026-08-20"])
    }

    // ---- 最終実施日と経過 ----

    @Test
    fun `最終実施日は種目ごとの最新の日付`() {
        val records = listOf(
            record(id = "a", date = "2026-08-20", ex = "ベンチプレス"),
            record(id = "b", date = "2026-08-11", ex = "ベンチプレス"),
            record(id = "c", date = "2026-08-01", ex = "スクワット"),
        )
        val last = lastPerformedByExercise(records)
        assertEquals("2026-08-20", last["ベンチプレス"])
        assertEquals("2026-08-01", last["スクワット"])
    }

    @Test
    fun `経過は今日ならその旨を出し それ以外は日数で出す`() {
        val today = LocalDate.of(2026, 8, 20)
        assertEquals("今日", elapsedLabelOf("2026-08-20", today))
        assertEquals("9日前", elapsedLabelOf("2026-08-11", today))
        assertEquals("352日前", elapsedLabelOf("2025-09-02", today))
    }

    @Test
    fun `未実施や読めない日付には経過を出さない`() {
        val today = LocalDate.of(2026, 8, 20)
        assertNull(elapsedLabelOf(null, today))
        assertNull(elapsedLabelOf("2026/08/20", today))
        assertNull(elapsedLabelOf("", today))
    }

    @Test
    fun `未来の日付は今日として扱う`() {
        assertEquals("今日", elapsedLabelOf("2026-08-25", LocalDate.of(2026, 8, 20)))
    }

    // ---- よくやる種目 ----

    @Test
    fun `よくやる種目は記録件数の多い順`() {
        val records = listOf(
            record(id = "a", date = "2026-08-20", ex = "スクワット"),
            record(id = "b", date = "2026-08-19", ex = "ベンチプレス"),
            record(id = "c", date = "2026-08-18", ex = "ベンチプレス"),
            record(id = "d", date = "2026-08-17", ex = "ベンチプレス"),
            record(id = "e", date = "2026-08-16", ex = "懸垂"),
            record(id = "f", date = "2026-08-15", ex = "懸垂"),
        )
        assertEquals(
            listOf("ベンチプレス", "懸垂", "スクワット"),
            frequentExercises(records).map { it.ex },
        )
    }

    @Test
    fun `件数が同じなら最終実施日が新しい順`() {
        val records = listOf(
            record(id = "a", date = "2026-08-01", ex = "懸垂"),
            record(id = "b", date = "2026-08-20", ex = "スクワット"),
        )
        assertEquals(listOf("スクワット", "懸垂"), frequentExercises(records).map { it.ex })
    }

    @Test
    fun `よくやる種目は件数の上限で打ち切る`() {
        val many = (1..30).map { record(id = "x$it", ex = "種目$it") }
        assertEquals(5, frequentExercises(many, limit = 5).size)
        assertTrue(frequentExercises(emptyList()).isEmpty())
    }

    @Test
    fun `よくやる種目にも部位の色を出せるよう部位を解決する`() {
        val records = listOf(
            record(id = "a", ex = "ベンチプレス"),
            record(id = "b", ex = "謎のトレ"),
        )
        val refs = frequentExercises(records)
        assertEquals(BodyPart.CHEST, refs.first { it.ex == "ベンチプレス" }.part)
        assertNull(refs.first { it.ex == "謎のトレ" }.part)
    }

    // ---- 部位の解決と種目マスタ ----

    @Test
    fun `保存された部位は種目名より優先する`() {
        assertEquals(BodyPart.LEG, bodyPartOf(record(ex = "ベンチプレス", part = "leg")))
        assertEquals(BodyPart.CHEST, bodyPartOf(record(ex = "ベンチプレス")))
        assertNull(bodyPartOf(record(ex = "謎のトレ", part = "unknown")))
    }

    @Test
    fun `部位タブの種目一覧はマスタに無い記録済みの種目も末尾に足す`() {
        val records = listOf(
            record(id = "a", ex = "自作の胸トレ", part = "chest"),
            record(id = "b", ex = "ベンチプレス", part = "chest"),
            record(id = "c", ex = "スクワット", part = "leg"),
        )
        val chest = exercisesOf(BodyPart.CHEST, records)
        assertEquals(presetExercises(BodyPart.CHEST) + listOf("自作の胸トレ"), chest)
        assertEquals(1, chest.count { it == "ベンチプレス" })
    }

    @Test
    fun `記録が無くてもプリセットの種目は並ぶ`() {
        BodyPart.entries.forEach { part ->
            assertTrue("${part.label} のプリセットが空", exercisesOf(part, emptyList()).isNotEmpty())
        }
    }
}
