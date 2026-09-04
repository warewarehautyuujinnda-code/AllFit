package com.hinata.fitlog.ui.common

import com.hinata.fitlog.domain.durationUntilNextDate
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 購読開始時とローカル日付が変わる境界で現在日を通知する。
 * 次の深夜まで suspend するためポーリングせず、購読解除時は待機もキャンセルされる。
 */
fun currentDateFlow(zoneId: ZoneId = ZoneId.systemDefault()): Flow<LocalDate> = flow {
    while (true) {
        val now = ZonedDateTime.now(zoneId)
        emit(now.toLocalDate())
        delay(durationUntilNextDate(now).toMillis().coerceAtLeast(1L))
    }
}
