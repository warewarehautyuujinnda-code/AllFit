package com.hinata.fitlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 週間プランのヘッダー。weekStart（週の開始日、月曜）をそのまま主キーにする。
 * 1週間に1件だけ存在する前提と一致するため、別IDは持たない。
 */
@Entity(tableName = "weekly_plan")
data class WeeklyPlanEntity(
    @PrimaryKey val weekStart: String,
    /** 参照先は [GoalEntity.createdAt]。外部キー制約にはせず、疎結合な識別子として保持する。 */
    val goalId: String? = null,
    val targetRunningKm: Double? = null,
    val memo: String? = null,
)
