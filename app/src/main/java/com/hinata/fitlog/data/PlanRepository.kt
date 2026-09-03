package com.hinata.fitlog.data

import com.hinata.fitlog.data.entity.GoalEntity
import com.hinata.fitlog.data.entity.WeeklyPlanEntity
import com.hinata.fitlog.data.entity.WeeklyStrengthTargetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PlanRepository(private val db: AppDatabase) {
    private val goalDao = db.goalDao()
    private val weeklyPlanDao = db.weeklyPlanDao()
    private val strengthTargetDao = db.weeklyStrengthTargetDao()

    fun observeGoals(): Flow<List<GoalEntity>> = goalDao.observeAll()
    fun observeWeeklyPlans(): Flow<List<WeeklyPlanEntity>> = weeklyPlanDao.observeAll()
    fun observeStrengthTargets(): Flow<List<WeeklyStrengthTargetEntity>> = strengthTargetDao.observeAll()

    suspend fun syncSeeds() = withContext(Dispatchers.IO) {
        goalDao.insertAll(PlanSeeds.goals)
        weeklyPlanDao.insertAll(PlanSeeds.weeklyPlans)
        strengthTargetDao.insertAll(PlanSeeds.strengthTargets)
    }
}
