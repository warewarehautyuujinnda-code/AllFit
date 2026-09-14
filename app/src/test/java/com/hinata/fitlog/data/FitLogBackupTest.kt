package com.hinata.fitlog.data

import com.hinata.fitlog.data.entity.ExerciseEntity
import com.hinata.fitlog.data.entity.MealEntity
import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.StrengthEntity
import com.hinata.fitlog.data.entity.StrengthSetEntity
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
        weight = listOf(
            WeightEntity(
                id = "w1",
                date = "2026-08-11",
                weight = 70.2,
                fat = 18.4,
                createdAt = "2026-08-11T21:30:00Z",
            )
        ),
        strength = listOf(
            StrengthEntity(
                id = "s1",
                date = "2026-08-11",
                ex = "ベンチプレス",
                memo = "肩甲骨を寄せる意識がつかめた",
                createdAt = "2026-08-11T12:05:00Z",
            )
        ),
        exercise = listOf(
            ExerciseEntity(
                name = "ベンチプレス",
                part = "chest",
                description = "ワイドグリップ。肩甲骨を寄せて胸を張る",
                createdAt = "2026-08-11T00:00:00Z",
                updatedAt = "2026-08-11T00:00:00Z",
            )
        ),
        strengthSet = listOf(
            StrengthSetEntity(id = "ss1", recordId = "s1", setIndex = 0, weight = 60.0, reps = 10),
            StrengthSetEntity(id = "ss2", recordId = "s1", setIndex = 1, weight = 55.0, reps = 8),
        ),
        running = listOf(
            RunningEntity(
                id = "r1",
                date = "2026-08-11",
                dist = 5.0,
                min = 27.5,
                kcal = 300,
                memo = "気持ちよく走れた",
                createdAt = "2026-08-11T06:40:00Z",
            )
        ),
        meal = listOf(
            MealEntity(
                id = "m1",
                date = "2026-08-11",
                name = "鶏むね肉",
                kcal = 620,
                p = 45.0,
                createdAt = "2026-08-11T19:20:00Z",
            )
        ),
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
    fun `種目の説明と記録のメモも書き出して読み込める`() {
        val json = exportJson.encodeToString(FitLogBackup.serializer(), backup)
        assertTrue("\"exercise\"" in json)
        assertTrue("ワイドグリップ。肩甲骨を寄せて胸を張る" in json)
        assertTrue("肩甲骨を寄せる意識がつかめた" in json)

        val decoded = importJson.decodeFromString(FitLogBackup.serializer(), json)
        assertEquals(backup.exercise, decoded.exercise)
        assertEquals("肩甲骨を寄せる意識がつかめた", decoded.strength.single().memo)
    }

    @Test
    fun `種目の説明が無い古いファイルも読み込める`() {
        val decoded = importJson.decodeFromString(
            FitLogBackup.serializer(),
            """{"strength":[{"id":"s1","date":"2026-08-11","ex":"ベンチプレス"}]}""",
        )
        assertTrue(decoded.exercise.isEmpty())
        assertEquals(null, decoded.strength.single().memo)
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

    @Test
    fun `記録を登録した日時も4種別すべて書き出して読み込める`() {
        val json = exportJson.encodeToString(FitLogBackup.serializer(), backup)
        assertTrue("\"createdAt\": \"2026-08-11T21:30:00Z\"" in json)

        val decoded = importJson.decodeFromString(FitLogBackup.serializer(), json)
        assertEquals("2026-08-11T21:30:00Z", decoded.weight.single().createdAt)
        assertEquals("2026-08-11T12:05:00Z", decoded.strength.single().createdAt)
        assertEquals("2026-08-11T06:40:00Z", decoded.running.single().createdAt)
        assertEquals("2026-08-11T19:20:00Z", decoded.meal.single().createdAt)
    }

    @Test
    fun `登録した日時が無い古いファイルも読み込める`() {
        val decoded = importJson.decodeFromString(
            FitLogBackup.serializer(),
            """{"weight":[{"id":"w1","date":"2026-08-11","weight":70.0}],
               "strength":[{"id":"s1","date":"2026-08-11","ex":"ベンチプレス"}],
               "running":[{"id":"r1","date":"2026-08-11","dist":5.0}],
               "meal":[{"id":"m1","date":"2026-08-11","name":"鶏むね肉"}]}""",
        )
        assertEquals(4, decoded.totalCount)
        assertEquals(null, decoded.weight.single().createdAt)
        assertEquals(null, decoded.strength.single().createdAt)
        assertEquals(null, decoded.running.single().createdAt)
        assertEquals(null, decoded.meal.single().createdAt)
    }
}
