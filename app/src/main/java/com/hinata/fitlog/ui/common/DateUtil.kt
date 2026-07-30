package com.hinata.fitlog.ui.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 記録日の保存形式は ISO-8601（yyyy-MM-dd）。この文字列は辞書順＝日付順のため
 * DAO の `ORDER BY date` がそのまま日付の並びになる。
 */
object DateUtil {
    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** 今日の日付（yyyy-MM-dd） */
    fun today(): String = LocalDate.now().format(ISO)

    /** ミリ秒（UTC）→ yyyy-MM-dd。DatePicker の選択値変換用 */
    fun fromEpochMillis(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate().format(ISO)

    /** yyyy-MM-dd → ミリ秒（UTC）。DatePicker の初期選択用。不正な文字列は今日にフォールバック */
    fun toEpochMillis(date: String): Long {
        val local = runCatching { LocalDate.parse(date, ISO) }.getOrDefault(LocalDate.now())
        return local.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    }
}
