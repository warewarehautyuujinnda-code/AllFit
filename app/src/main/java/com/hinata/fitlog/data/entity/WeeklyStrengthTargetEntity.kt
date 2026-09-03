package com.hinata.fitlog.data.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "weekly_strength_target",
    primaryKeys = ["weekPlanId", "exerciseName"],
    indices = [Index("weekPlanId")],
)
data class WeeklyStrengthTargetEntity(
    val weekPlanId: String,
    val exerciseName: String,
    val targetReps: Int? = null,
    val targetSets: Int? = null,
)
