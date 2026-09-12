package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.ExerciseEntity
import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import java.time.Duration
import java.time.ZonedDateTime

/** 現在時刻から次のローカル日付境界までの時間。DST のあるタイムゾーンも日付基準で扱う。 */
fun durationUntilNextDate(now: ZonedDateTime): Duration =
    Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(now.zone))

/**
 * 種目選択画面の行。
 *
 * 一覧から削除（非表示）にした種目は出さない。記録そのものは残っているので、
 * ここで弾くだけで履歴や集計には影響しない。
 * 部位タブには、まだ1回も記録していないが説明を書いて追加した種目も出す
 * （鉛筆ボタンで追加してすぐ画面を戻しても、種目が行方不明にならないようにするため）。
 * よくやる種目には未記録の計画種目を追加し、部位タブにはその部位だと判定できる計画種目だけを追加する。
 */
fun exercisePickerRows(
    records: List<StrengthRecordWithSets>,
    selectedPart: BodyPart?,
    plannedExercises: Set<String>,
    exercises: List<ExerciseEntity> = emptyList(),
): List<ExerciseRef> {
    val hidden = hiddenExerciseNames(exercises)
    val shown = records.filterNot { it.record.ex.trim() in hidden }
    val base = if (selectedPart == null) {
        frequentExercises(shown)
    } else {
        exercisesOf(selectedPart, shown).map { ExerciseRef(it, selectedPart) }
    }
    val recorded = records.mapTo(mutableSetOf()) { it.record.ex.trim() }
    val defined = if (selectedPart == null) {
        emptyList()
    } else {
        exercises
            .filter { it.hiddenAt == null }
            .filter { it.name.trim().isNotEmpty() && it.name.trim() !in recorded }
            .filter { exercisePartOf(it) == selectedPart }
            .sortedBy { it.createdAt.orEmpty() }
            .map { ExerciseRef(it.name.trim(), selectedPart) }
    }
    val existingNames = (base + defined).mapTo(mutableSetOf()) { it.ex }
    val planned = plannedExercises
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .filterNot { it in hidden }
        .map { ExerciseRef(it, presetPartOf(it)) }
        .filter { it.ex !in existingNames && (selectedPart == null || it.part == selectedPart) }
    return base + defined + planned
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
