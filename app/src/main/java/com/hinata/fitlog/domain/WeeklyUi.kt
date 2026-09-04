package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import java.time.Duration
import java.time.ZonedDateTime

/** 現在時刻から次のローカル日付境界までの時間。DST のあるタイムゾーンも日付基準で扱う。 */
fun durationUntilNextDate(now: ZonedDateTime): Duration =
    Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(now.zone))

/**
 * 種目選択画面の行。よくやる種目には未記録の計画種目も追加し、
 * 部位タブにはその部位だと判定できる計画種目だけを追加する。
 */
fun exercisePickerRows(
    records: List<StrengthRecordWithSets>,
    selectedPart: BodyPart?,
    plannedExercises: Set<String>,
): List<ExerciseRef> {
    val base = if (selectedPart == null) {
        frequentExercises(records)
    } else {
        exercisesOf(selectedPart, records).map { ExerciseRef(it, selectedPart) }
    }
    val existingNames = base.mapTo(mutableSetOf()) { it.ex }
    val planned = plannedExercises
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .map { ExerciseRef(it, presetPartOf(it)) }
        .filter { it.ex !in existingNames && (selectedPart == null || it.part == selectedPart) }
    return base + planned
}

/** 体重の現在値から減量目標までの差を、方向が分かる文言にする。 */
fun weightGoalDifferenceLabel(current: Double?, goal: Double): String? {
    current ?: return null
    val difference = current - goal
    return when {
        difference > 0.0 -> "目標 ${formatAmount(goal)} kg（あと ${formatAmount(difference)} kg）"
        difference < 0.0 -> "目標 ${formatAmount(goal)} kg（${formatAmount(-difference)} kg 達成）"
        else -> "目標 ${formatAmount(goal)} kg（達成）"
    }
}
