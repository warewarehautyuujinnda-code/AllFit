package com.hinata.fitlog.data

import com.hinata.fitlog.data.entity.WeightEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 読み込み（FR-12）で、記録の登録日時（createdAt）を古いバックアップに消されないことの検査。
 * 登録日時は version 9 で足した項目なので、それより前に書き出したファイルには入っていない。
 */
class ImportedRecordMergeTest {

    private fun merge(imported: List<WeightEntity>, local: List<WeightEntity>) =
        keepKnownCreatedAt(
            imported, local,
            { it.id }, { it.createdAt }, { r, at -> r.copy(createdAt = at) },
        )

    @Test
    fun `登録日時が無い記録は端末にある登録日時を残す`() {
        val local = listOf(
            WeightEntity(id = "w1", date = "2026-09-10", weight = 70.0, createdAt = "2026-09-10T21:30:00Z")
        )
        val imported = listOf(WeightEntity(id = "w1", date = "2026-09-10", weight = 70.0))

        assertEquals("2026-09-10T21:30:00Z", merge(imported, local).single().createdAt)
    }

    @Test
    fun `読み込んだ側に登録日時があればそちらを使う`() {
        val local = listOf(
            WeightEntity(id = "w1", date = "2026-09-10", weight = 70.0, createdAt = "2026-09-10T21:30:00Z")
        )
        val imported = listOf(
            WeightEntity(id = "w1", date = "2026-09-10", weight = 70.5, createdAt = "2026-09-11T07:00:00Z")
        )

        val merged = merge(imported, local).single()
        assertEquals("2026-09-11T07:00:00Z", merged.createdAt)
        // 登録日時以外は読み込んだ内容がそのまま入る
        assertEquals(70.5, merged.weight, 0.0)
    }

    @Test
    fun `端末に無い記録はそのまま読み込む`() {
        val imported = listOf(WeightEntity(id = "new", date = "2026-09-12", weight = 69.8))

        assertNull(merge(imported, emptyList()).single().createdAt)
    }

    @Test
    fun `別の記録の登録日時を借りてこない`() {
        val local = listOf(
            WeightEntity(id = "w1", date = "2026-09-10", weight = 70.0, createdAt = "2026-09-10T21:30:00Z")
        )
        val imported = listOf(WeightEntity(id = "w2", date = "2026-09-11", weight = 70.1))

        assertNull(merge(imported, local).single().createdAt)
    }

    @Test
    fun `登録日時以外の項目は書き換えない`() {
        val local = listOf(
            WeightEntity(id = "w1", date = "2026-09-10", weight = 70.0, createdAt = "2026-09-10T21:30:00Z")
        )
        val imported = listOf(WeightEntity(id = "w1", date = "2026-09-10", weight = 70.0, fat = 18.0))

        val merged = merge(imported, local).single()
        assertEquals(18.0, merged.fat!!, 0.0)
        assertEquals(imported.single().copy(createdAt = "2026-09-10T21:30:00Z"), merged)
    }
}
