package com.hinata.fitlog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 設定タブのフィードバック欄（アンケート）の回答まとめ */
class FeedbackSurveyTest {

    private val full = FeedbackSurveyAnswers(
        target = FeedbackTarget.STRENGTH,
        request = "前回の重量をすぐ見たい",
        problem = "カレンダーから探し直している",
        want = FeedbackWant.OFTEN_STUCK,
        other = "毎日使っています",
    )

    @Test
    fun `タブの選択と要望が埋まっていれば送れる`() {
        assertTrue(full.canSubmit)
        assertFalse(full.copy(target = null).canSubmit)
        assertFalse(full.copy(request = "   ").canSubmit)
    }

    @Test
    fun `任意の項目が空でも送れる`() {
        val minimal = FeedbackSurveyAnswers(
            target = FeedbackTarget.WHOLE_APP,
            request = "起動を速くしてほしい",
        )
        assertTrue(minimal.canSubmit)
    }

    @Test
    fun `設問の見出しごと読める文章にする`() {
        val expected = """
            FitLog への要望（設定タブのアンケート）

            ■ どのタブについて
            筋トレ

            ■ 追加してほしい機能
            前回の重量をすぐ見たい

            ■ 今どうしていて、何に困っているか
            カレンダーから探し直している

            ■ どれくらい欲しいか
            よく困っている

            ■ その他ひとこと
            毎日使っています

            ---
            アプリのバージョン: 1.0 (local)
        """.trimIndent()
        assertEquals(expected, buildFeedbackSurveyText(full, "1.0 (local)"))
    }

    @Test
    fun `書かれていない任意の項目は見出しごと出さない`() {
        val text = buildFeedbackSurveyText(
            FeedbackSurveyAnswers(
                target = FeedbackTarget.MEAL,
                request = "写真から記録したい",
            ),
        )
        val expected = """
            FitLog への要望（設定タブのアンケート）

            ■ どのタブについて
            食事

            ■ 追加してほしい機能
            写真から記録したい
        """.trimIndent()
        assertEquals(expected, text)
    }

    @Test
    fun `前後の空白は落としてから文章に入れる`() {
        val text = buildFeedbackSurveyText(
            FeedbackSurveyAnswers(
                target = FeedbackTarget.WEIGHT,
                request = "  朝晩で分けて記録したい  ",
            ),
        )
        assertTrue(text.contains("■ 追加してほしい機能\n朝晩で分けて記録したい"))
        assertFalse(text.contains("  朝晩"))
    }
}
