package com.example.habittracker.domain.usecase.meal

import com.example.habittracker.data.entity.MealLogEntity
import com.example.habittracker.data.model.MealType
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class AnalyzeMealLogsUseCase @Inject constructor() {

    operator fun invoke(
        logs: List<MealLogEntity>,
        plan: DailyMealPlan,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): MealLogAnalysis {
        val sortedLogs = logs.sortedBy { it.timestamp }
        val primaryByType = mutableMapOf<MealType, MealLogEntity>()
        val analyzedLogs = sortedLogs.map { log ->
            when {
                log.isLateNight || log.type == MealType.LATE_NIGHT -> {
                    AnalyzedMealLog(
                        log = log,
                        role = MealLogRole.LATE_NIGHT_EVENT,
                        mealType = MealType.LATE_NIGHT,
                        minutesFromPrimary = null,
                        affectsNextMealWindow = false,
                    )
                }
                log.type !in ordinaryMealTypes -> {
                    AnalyzedMealLog(
                        log = log,
                        role = MealLogRole.LATE_NIGHT_EVENT,
                        mealType = log.type,
                        minutesFromPrimary = null,
                        affectsNextMealWindow = false,
                    )
                }
                log.type !in primaryByType -> {
                    primaryByType[log.type] = log
                    AnalyzedMealLog(
                        log = log,
                        role = MealLogRole.PRIMARY_MEAL,
                        mealType = log.type,
                        minutesFromPrimary = 0,
                        affectsNextMealWindow = false,
                    )
                }
                else -> {
                    val minutesFromPrimary = ((log.timestamp - primaryByType.getValue(log.type).timestamp) / MILLIS_PER_MINUTE)
                        .toInt()
                    val affectsNextWindow = affectsNextMealWindow(log, log.type, plan, zoneId)
                    AnalyzedMealLog(
                        log = log,
                        role = if (affectsNextWindow) MealLogRole.EARLY_NEXT_MEAL else MealLogRole.ADDITIONAL_INTAKE,
                        mealType = log.type,
                        minutesFromPrimary = minutesFromPrimary,
                        affectsNextMealWindow = affectsNextWindow,
                    )
                }
            }
        }

        val additionalLogs = analyzedLogs.filter {
            it.role == MealLogRole.ADDITIONAL_INTAKE || it.role == MealLogRole.EARLY_NEXT_MEAL
        }
        val delayByType = additionalLogs
            .mapNotNull { analyzed ->
                nextExpectedMealAfter(analyzed.mealType, plan)?.type?.let { nextType ->
                    val delay = if (analyzed.affectsNextMealWindow) EARLY_NEXT_DELAY_MINUTES else ADDITIONAL_DELAY_MINUTES
                    nextType to delay
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, delays) -> delays.sum().coerceAtMost(MAX_DELAY_MINUTES) }

        return MealLogAnalysis(
            analyzedLogs = analyzedLogs,
            primaryMealTypes = analyzedLogs
                .filter { it.role == MealLogRole.PRIMARY_MEAL }
                .map { it.mealType }
                .toSet(),
            additionalIntakeCount = additionalLogs.size,
            earlyNextMealCandidates = additionalLogs
                .filter { it.role == MealLogRole.EARLY_NEXT_MEAL }
                .mapNotNull { nextExpectedMealAfter(it.mealType, plan)?.type }
                .toSet(),
            hasIrregularIntake = additionalLogs.size >= IRREGULAR_ADDITIONAL_COUNT,
            nextMealDelayMinutesByType = delayByType,
        )
    }

    private fun affectsNextMealWindow(
        log: MealLogEntity,
        mealType: MealType,
        plan: DailyMealPlan,
        zoneId: ZoneId,
    ): Boolean {
        val nextWindow = nextExpectedMealAfter(mealType, plan) ?: return false
        val logMinutes = minutesOfDay(log.timestamp, zoneId)
        val candidateStart = (nextWindow.startMinutes - EARLY_NEXT_LEAD_MINUTES).coerceAtLeast(0)
        val candidateEnd = (nextWindow.startMinutes + EARLY_NEXT_WINDOW_GRACE_MINUTES)
            .coerceAtMost(nextWindow.endMinutes)
        return logMinutes in candidateStart..candidateEnd
    }

    private fun nextExpectedMealAfter(
        mealType: MealType,
        plan: DailyMealPlan,
    ): ExpectedMealWindow? {
        val index = plan.expectedMeals.indexOfFirst { it.type == mealType }
        if (index < 0) return null
        return plan.expectedMeals.drop(index + 1).firstOrNull { it.type in ordinaryMealTypes }
    }

    private fun minutesOfDay(timestamp: Long, zoneId: ZoneId): Int {
        val localTime = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalTime()
        return localTime.hour * 60 + localTime.minute
    }

    companion object {
        private val ordinaryMealTypes = setOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER)
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val EARLY_NEXT_LEAD_MINUTES = 90
        private const val EARLY_NEXT_WINDOW_GRACE_MINUTES = 60
        private const val ADDITIONAL_DELAY_MINUTES = 30
        private const val EARLY_NEXT_DELAY_MINUTES = 60
        private const val MAX_DELAY_MINUTES = 90
        private const val IRREGULAR_ADDITIONAL_COUNT = 2
    }
}
