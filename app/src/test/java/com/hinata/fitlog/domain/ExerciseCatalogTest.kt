package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.ExerciseEntity
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 種目の定義（説明・一覧からの削除）と、それを踏まえた種目選択画面の一覧 */
class ExerciseCatalogTest {

    private fun record(ex: String, date: String = "2026-09-01", part: String? = null) =
        StrengthRecordWithSets(
            record = StrengthEntity(id = ex + date, date = date, ex = ex, part = part),
            sets = emptyList(),
        )

    private fun exercise(
        name: String,
        part: String? = null,
        description: String? = null,
        hiddenAt: String? = null,
        createdAt: String? = null,
    ) = ExerciseEntity(
        name = name,
        part = part,
        description = description,
        hiddenAt = hiddenAt,
        createdAt = createdAt,
    )

    // ---- 一覧からの削除（論理削除） ----

    @Test
    fun `一覧から削除した種目はよくやる種目にも部位タブにも出ない`() {
        val records = listOf(record("ベンチプレス", part = "chest"), record("スクワット", part = "leg"))
        val exercises = listOf(exercise("ベンチプレス", part = "chest", hiddenAt = "2026-09-02T00:00:00Z"))

        assertEquals(
            listOf("スクワット"),
            exercisePickerRows(records, null, emptySet(), exercises).map { it.ex },
        )
        assertTrue(exercisePickerRows(records, BodyPart.CHEST, emptySet(), exercises).isEmpty())
    }

    @Test
    fun `一覧から削除しても記録そのものは残る`() {
        val records = listOf(record("ベンチプレス", part = "chest"))
        val exercises = listOf(exercise("ベンチプレス", hiddenAt = "2026-09-02T00:00:00Z"))

        // 一覧からは消えるが、集計に使う記録は1件もフィルタされない
        assertTrue(exercisePickerRows(records, null, emptySet(), exercises).isEmpty())
        assertEquals(1, dayStrengthOf(records, "2026-09-01").exercises.size)
    }

    @Test
    fun `一覧から削除した種目は計画に入っていても出さない`() {
        val exercises = listOf(exercise("ベンチプレス", hiddenAt = "2026-09-02T00:00:00Z"))
        assertTrue(
            exercisePickerRows(emptyList(), null, setOf("ベンチプレス"), exercises).isEmpty()
        )
    }

    @Test
    fun `定義が無い種目や表示中の種目はこれまでどおり出る`() {
        val records = listOf(record("ベンチプレス", part = "chest"))
        val exercises = listOf(exercise("ベンチプレス", part = "chest", description = "ワイドグリップ"))
        assertEquals(
            listOf("ベンチプレス"),
            exercisePickerRows(records, BodyPart.CHEST, emptySet(), exercises).map { it.ex },
        )
        assertEquals(
            listOf("ベンチプレス"),
            exercisePickerRows(records, BodyPart.CHEST, emptySet(), emptyList()).map { it.ex },
        )
    }

    // ---- 追加したがまだ記録していない種目 ----

    @Test
    fun `説明を書いて追加しただけの種目も部位タブには出る`() {
        val exercises = listOf(
            exercise("ナローベンチプレス", part = "chest", description = "手幅を狭く", createdAt = "2026-09-02T00:00:00Z")
        )
        assertEquals(
            listOf("ナローベンチプレス"),
            exercisePickerRows(emptyList(), BodyPart.CHEST, emptySet(), exercises).map { it.ex },
        )
    }

    @Test
    fun `部位はプリセットからも引く`() {
        val exercises = listOf(exercise("デッドリフト"))
        assertEquals(
            listOf("デッドリフト"),
            exercisePickerRows(emptyList(), BodyPart.BACK, emptySet(), exercises).map { it.ex },
        )
        assertTrue(exercisePickerRows(emptyList(), BodyPart.LEG, emptySet(), exercises).isEmpty())
    }

    @Test
    fun `記録済みの種目を定義の分で二重に出さない`() {
        val records = listOf(record("スクワット", part = "leg"))
        val exercises = listOf(exercise("スクワット", part = "leg"))
        assertEquals(
            listOf("スクワット"),
            exercisePickerRows(records, BodyPart.LEG, emptySet(), exercises).map { it.ex },
        )
    }

    // ---- 種目名の入力チェック ----

    @Test
    fun `空の種目名は保存できない`() {
        assertEquals("種目名を入力してください", exerciseNameError("  ", null, emptySet(), emptySet()))
    }

    @Test
    fun `自分と同じ名前のままなら問題なし`() {
        assertNull(exerciseNameError("ベンチプレス", "ベンチプレス", setOf("ベンチプレス"), emptySet()))
    }

    @Test
    fun `ほかの種目と同じ名前にはできない`() {
        assertEquals(
            "同じ名前の種目がすでにあります",
            exerciseNameError("スクワット", "ベンチプレス", setOf("スクワット"), emptySet()),
        )
    }

    @Test
    fun `一覧から削除した種目とぶつかるときは戻し方を伝える`() {
        val message = exerciseNameError("スクワット", "ベンチプレス", emptySet(), setOf("スクワット"))
        assertTrue(message!!.contains("一覧から削除した種目"))
    }

    @Test
    fun `hiddenAtが入っている種目だけを非表示として数える`() {
        val exercises = listOf(
            exercise("ベンチプレス", hiddenAt = "2026-09-02T00:00:00Z"),
            exercise("スクワット"),
        )
        assertEquals(setOf("ベンチプレス"), hiddenExerciseNames(exercises))
    }
}
