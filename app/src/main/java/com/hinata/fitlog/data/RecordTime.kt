package com.hinata.fitlog.data

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * 記録を登録した日時・種目の作成更新日時に使う現在時刻（ISO-8601・秒まで・UTC）。
 *
 * 桁が揃うので、書き出したJSONを人やAIが読んでも分かり、文字列のままでも前後が狂わない。
 * 記録日（`date` = yyyy-MM-dd）とは別の情報で、こちらは「いつ入力したか」を持つ。
 * 新しく記録のテーブルを足すときも、登録日時はこの関数で入れる。
 */
fun isoNow(): String = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
