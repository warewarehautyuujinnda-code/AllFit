package com.hinata.fitlog.data

import com.hinata.fitlog.data.entity.GoalEntity
import com.hinata.fitlog.data.entity.WeeklyPlanEntity
import com.hinata.fitlog.data.entity.WeeklyStrengthTargetEntity

/**
 * /weekly-plan スキルが追記する長期目標・週次計画。
 * 起動時にRoomへ追記専用で同期される。
 */
object PlanSeeds {
    val goals: List<GoalEntity> = emptyList()
    val weeklyPlans: List<WeeklyPlanEntity> = emptyList()
    val strengthTargets: List<WeeklyStrengthTargetEntity> = emptyList()
}
