package com.hinata.fitlog.data

import com.hinata.fitlog.data.entity.ExerciseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 読み込み（FR-12）で、端末にある種目の定義と読み込んだ定義をすり合わせる規則。
 * 種目の説明は利用者が書いた文章なので、古いバックアップの読み込みで消えないことを確かめる。
 */
class ExerciseMergeTest {

    private fun exercise(
        description: String? = null,
        part: String? = null,
        hiddenAt: String? = null,
        createdAt: String? = null,
        updatedAt: String? = null,
    ) = ExerciseEntity(
        name = "ベンチプレス",
        part = part,
        description = description,
        hiddenAt = hiddenAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    @Test
    fun `読み込んだ側が新しければそちらを採用する`() {
        val local = exercise(description = "古い説明", updatedAt = "2026-09-01T00:00:00Z")
        val imported = exercise(description = "新しい説明", updatedAt = "2026-09-05T00:00:00Z")
        assertEquals("新しい説明", mergeImportedExercise(local, imported).description)
    }

    @Test
    fun `端末側が新しければ端末の説明を残す`() {
        val local = exercise(description = "端末で書いた説明", updatedAt = "2026-09-05T00:00:00Z")
        val imported = exercise(description = "古いバックアップの説明", updatedAt = "2026-09-01T00:00:00Z")
        assertEquals("端末で書いた説明", mergeImportedExercise(local, imported).description)
    }

    @Test
    fun `端末側が空の項目は読み込んだ値で埋める`() {
        val local = exercise(updatedAt = "2026-09-05T00:00:00Z")
        val imported = exercise(
            description = "バックアップの説明",
            part = "chest",
            updatedAt = "2026-09-01T00:00:00Z",
        )
        val merged = mergeImportedExercise(local, imported)
        assertEquals("バックアップの説明", merged.description)
        assertEquals("chest", merged.part)
    }

    @Test
    fun `日時が無い・読めない定義は古いものとして扱う`() {
        val local = exercise(description = "端末の説明", updatedAt = "2026-09-01T00:00:00Z")
        val imported = exercise(description = "日時なしの説明", updatedAt = null)
        assertEquals("端末の説明", mergeImportedExercise(local, imported).description)

        val brokenDate = exercise(description = "壊れた日時の説明", updatedAt = "2026/09/09")
        assertEquals("端末の説明", mergeImportedExercise(local, brokenDate).description)
    }

    @Test
    fun `端末側に日時が無ければ読み込んだ側を新しいとみなす`() {
        val local = exercise(description = "日時なしの端末の説明", updatedAt = null)
        val imported = exercise(description = "バックアップの説明", updatedAt = "2026-09-01T00:00:00Z")
        assertEquals("バックアップの説明", mergeImportedExercise(local, imported).description)
    }

    @Test
    fun `作成日時は端末側のものを残す`() {
        val local = exercise(createdAt = "2026-08-01T00:00:00Z", updatedAt = "2026-09-01T00:00:00Z")
        val imported = exercise(createdAt = "2026-09-09T00:00:00Z", updatedAt = "2026-09-09T00:00:00Z")
        assertEquals("2026-08-01T00:00:00Z", mergeImportedExercise(local, imported).createdAt)
    }

    @Test
    fun `書き出した時刻は秒までの形にそろえる`() {
        // 桁が揃っていないと、文字列のまま比べたときに新旧が狂う
        assertTrue(isoNow().matches(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z""")))
    }
}
