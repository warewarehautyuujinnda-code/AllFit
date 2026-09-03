package com.hinata.fitlog.domain

import com.hinata.fitlog.data.entity.RunningEntity
import com.hinata.fitlog.data.entity.StrengthRecordWithSets
import com.hinata.fitlog.data.entity.WeeklyPlanEntity
import com.hinata.fitlog.data.entity.WeeklyStrengthTargetEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

fun weekStartOf(date: LocalDate): String =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()

private fun weekEndExclusiveOf(weekStart: String): String =
    LocalDate.parse(weekStart).plusDays(7).toString()

fun weeklyPlanFor(plans: List<WeeklyPlanEntity>, weekStart: String): WeeklyPlanEntity? =
    plans.firstOrNull { it.weekStart == weekStart }

data class StrengthPlanProgress(
    val plannedExercises: List<String>,
    val doneExercises: Set<String>,
) {
    val plannedCount: Int get() = plannedExercises.size
    val doneCount: Int get() = plannedExercises.count { it in doneExercises }
    val pendingExercises: List<String> get() = plannedExercises.filterNot { it in doneExercises }
}

fun strengthProgressOf(
    targets: List<WeeklyStrengthTargetEntity>,
    records: List<StrengthRecordWithSets>,
    weekStart: String,
): StrengthPlanProgress {
    val weekEndExclusive = weekEndExclusiveOf(weekStart)
    val planned = targets.filter { it.weekPlanId == weekStart }.map { it.exerciseName.trim() }
    val done = records
        .filter { it.record.date >= weekStart && it.record.date < weekEndExclusive }
        .map { it.record.ex.trim() }
        .toSet()
    return StrengthPlanProgress(planned, done)
}

fun runningDistanceOf(records: List<RunningEntity>, weekStart: String): Double {
    val weekEndExclusive = weekEndExclusiveOf(weekStart)
    return records.filter { it.date >= weekStart && it.date < weekEndExclusive }.sumOf { it.dist }
}

data class WeeklyRunningProgress(val actualKm: Double, val targetKm: Double) {
    val remainingKm: Double get() = (targetKm - actualKm).coerceAtLeast(0.0)
}

data class WeeklyGoalSummary(
    val goalTitle: String? = null,
    val goalTargetDate: String? = null,
    val strength: StrengthPlanProgress? = null,
    val runningActualKm: Double? = null,
    val runningTargetKm: Double? = null,
    val weightCurrent: Double? = null,
    val weightGoal: Double? = null,
)
