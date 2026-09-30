package com.hinata.fitlog.domain

/**
 * 設定タブのフィードバック欄（アンケート）で扱う内容。
 *
 * 画面キャプチャに赤線を書き込んで送るフィードバックは「今見えているここがおかしい」を伝えるためのもので、
 * 「このタブにこういう機能がほしい」のような画面に写らない要望の置き場が無かった。
 * ここはその受け皿で、答えをそのまま読める1本の文章に組み立てて共有シートへ渡す。
 * 送信先のサーバーは持たないため、この層はネットワークも Android の API も触らない。
 */

/** 「どのタブについての話か」の選択肢。下部ナビゲーションのタブ + アプリ全体。 */
enum class FeedbackTarget(val label: String) {
    HOME("ホーム"),
    WEIGHT("体重"),
    STRENGTH("筋トレ"),
    RUNNING("ラン"),
    MEAL("食事"),
    DATA("データ"),
    SETTINGS("設定"),
    WHOLE_APP("アプリ全体"),
}

/** 「どれくらい欲しいか」の選択肢。直す順番を決めるために聞く。 */
enum class FeedbackWant(val label: String) {
    NICE_TO_HAVE("あると嬉しい"),
    OFTEN_STUCK("よく困っている"),
    BLOCKER("これが無いと困る"),
}

/**
 * アンケートの回答。[target] と [request] が必須で、残りは任意。
 * 任意の項目は空のまま送れるようにして、ひとこと書いて送るだけでも成り立つようにしている。
 */
data class FeedbackSurveyAnswers(
    val target: FeedbackTarget? = null,
    val request: String = "",
    val problem: String = "",
    val want: FeedbackWant? = null,
    val other: String = "",
)

/** 送信できる状態か（どのタブの話かと、追加してほしい機能が埋まっているか）。 */
val FeedbackSurveyAnswers.canSubmit: Boolean
    get() = target != null && request.isNotBlank()

/**
 * 回答を共有用の文章にする。
 * 受け取った側（人でも AI でも）が設問を知らなくても読めるよう、設問の見出しごと文章に含める。
 * 任意の項目は書かれたものだけ出す（空欄の見出しだけが並ぶのを避けるため）。
 */
fun buildFeedbackSurveyText(
    answers: FeedbackSurveyAnswers,
    appVersion: String = "",
): String {
    val sections = buildList {
        add("■ どのタブについて" to (answers.target?.label ?: "未選択"))
        add("■ 追加してほしい機能" to answers.request.trim())
        if (answers.problem.isNotBlank()) {
            add("■ 今どうしていて、何に困っているか" to answers.problem.trim())
        }
        answers.want?.let { add("■ どれくらい欲しいか" to it.label) }
        if (answers.other.isNotBlank()) {
            add("■ その他ひとこと" to answers.other.trim())
        }
    }

    val body = sections.joinToString("\n\n") { (heading, value) -> "$heading\n$value" }
    val footer = if (appVersion.isBlank()) "" else "\n\n---\nアプリのバージョン: $appVersion"
    return "FitLog への要望（設定タブのアンケート）\n\n$body$footer"
}
