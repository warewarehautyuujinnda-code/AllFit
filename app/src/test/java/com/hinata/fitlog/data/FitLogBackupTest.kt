package com.hinata.fitlog.data

import com.hinata.fitlog.data.entity.MealEntity
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.data.entity.WeightEntity
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 書き出し（FR-11）・読み込み（FR-12）の JSON 形式。
 * [FitLogRepository] と同じ Json 設定を使い、実際に書き出されるものと同じ形を確かめる。
 */
class FitLogBackupTest {

    private val exportJson = Json { prettyPrint = true; encodeDefaults = true }
    private val importJson = Json { ignoreUnknownKeys = true }

    private val backup = FitLogBackup(
        exportedAt = "2026-08-11T00:00:00Z",
        weight = listOf(WeightEntity(id = "w1", date = "2026-08-11", weight = 70.2, fat = 18.4)),
        strength = listOf(
            StrengthEntity(id = "s1", date = "2026-08-11", ex = "ベンチプレス", weight = 60.0, reps = 10, sets = 3)
        ),
        running = listOf(
            RunningEntity(
                id = "r1",
                date = "2026-08-11",
                dist = 5.0,
                min = 27.5,
                kcal = 300,
                memo = "気持ちよく走れた",
            )
        ),
        meal = listOf(MealEntity(id = "m1", date = "2026-08-11", name = "鶏むね肉", kcal = 620, p = 45.0)),
    )

    @Test
    fun `書き出して読み込むと同じ内容に戻る`() {
        val json = exportJson.encodeToString(FitLogBackup.serializer(), backup)
        assertEquals(backup, importJson.decodeFromString(FitLogBackup.serializer(), json))
    }

    @Test
    fun `4種別すべてが1つのJSONに入る`() {
        assertEquals(4, backup.totalCount)
        val json = exportJson.encodeToString(FitLogBackup.serializer(), backup)
        listOf("\"weight\"", "\"strength\"", "\"running\"", "\"meal\"").forEach {
            assertTrue("$it が書き出されていない", it in json)
        }
    }

    @Test
    fun `未入力の項目も null として明示的に書き出す`() {
        val json = exportJson.encodeToString(FitLogBackup.serializer(), backup)
        // 「項目がない」と「値がない」が混ざると、渡した先での解釈が分かれてしまう
        assertTrue("\"f\": null" in json)
    }

    @Test
    fun `日本語がそのまま読める形で書き出される`() {
        val json = exportJson.encodeToString(FitLogBackup.serializer(), backup)
        assertTrue("ベンチプレス" in json)
    }

    @Test
    fun `JSONでないファイルは読み込みに失敗する`() {
        assertTrue(
            runCatching {
                importJson.decodeFromString(FitLogBackup.serializer(), "これはJSONではありません")
            }.isFailure
        )
    }

    @Test
    fun `構造が違うファイルは読み込みに失敗する`() {
        assertTrue(
            runCatching {
                importJson.decodeFromString(FitLogBackup.serializer(), """{"weight":123}""")
            }.isFailure
        )
    }

    @Test
    fun `必須項目が欠けた記録は読み込みに失敗する`() {
        assertTrue(
            runCatching {
                importJson.decodeFromString(FitLogBackup.serializer(), """{"weight":[{"id":"a"}]}""")
            }.isFailure
        )
    }

    @Test
    fun `知らない項目が増えたファイルは読み飛ばして読み込める`() {
        val decoded = importJson.decodeFromString(
            FitLogBackup.serializer(),
            """{"sleep":[{"h":8}],"weight":[{"id":"a","date":"2026-08-11","weight":70.0}]}""",
        )
        assertEquals(1, decoded.totalCount)
    }

    @Test
    fun `種別が欠けたファイルはその種別を空として読み込める`() {
        assertEquals(0, importJson.decodeFromString(FitLogBackup.serializer(), "{}").totalCount)
    }
}
