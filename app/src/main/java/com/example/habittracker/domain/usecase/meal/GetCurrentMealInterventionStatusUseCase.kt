package com.example.habittracker.domain.usecase.meal

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.model.MealType
import com.example.habittracker.domain.repository.MealRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class MealCurrentInterventionStatus(
    val isActionable: Boolean,
    val actionableMealType: MealType?,
    val currentWindow: ExpectedMealWindow?,
    val reason: String,
    val intensity: MealInterventionIntensity = MealInterventionIntensity.NORMAL,
)

enum class MealInterventionIntensity {
    NONE,
    SOFT,
    NORMAL,
}

@Singleton
class GetCurrentMealInterventionStatusUseCase @Inject constructor(
    private val mealRepository: MealRepository,
    private val userPreferenceManager: UserPreferenceManager,
    private val dailyMealPlanCalculator: DailyMealPlanCalculator,
    private val analyzeMealLogsUseCase: AnalyzeMealLogsUseCase,
) {

    suspend operator fun invoke(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): MealCurrentInterventionStatus {
        val localDateTime = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDateTime()
        val currentMinutes = localDateTime.hour * 60 + localDateTime.minute
        val wakeTimeMinutes = parseTimeToMinutes(
            value = userPreferenceManager.wakeTimeFlow.first(),
            fallback = DEFAULT_WAKE_TIME_MINUTES,
        )
        val bedTimeMinutes = parseTimeToMinutes(
            value = userPreferenceManager.bedTimeFlow.first(),
            fallback = DEFAULT_BED_TIME_MINUTES,
            midnightAsEndOfDay = true,
        )
        val plan = dailyMealPlanCalculator.calculate(
            date = LocalDate.now(zoneId).toString(),
            wakeTimeMinutes = wakeTimeMinutes,
            bedTimeMinutes = bedTimeMinutes,
            todayActiveStartedAtMillis = userPreferenceManager.todayActiveStartedAtFlow.first(),
            zoneId = zoneId,
        )
        val currentWindow = plan.expectedMeals
            .firstOrNull { it.type != MealType.LATE_NIGHT && it.contains(currentMinutes) }
            ?: return MealCurrentInterventionStatus(
            isActionable = false,
            actionableMealType = null,
            currentWindow = null,
            reason = "not_in_meal_window",
            intensity = MealInterventionIntensity.NONE,
        )

        val todayLogs = mealRepository.getLogsByMealDate(LocalDate.now(zoneId).toString())
        val analysis = analyzeMealLogsUseCase(
            logs = todayLogs,
            plan = plan,
            zoneId = zoneId,
        )
        val alreadyLogged = analysis.analyzedLogs.any { analyzed ->
            analyzed.mealType == currentWindow.type &&
                analyzed.role != MealLogRole.LATE_NIGHT_EVENT
        }
        return if (alreadyLogged) {
            MealCurrentInterventionStatus(
                isActionable = false,
                actionableMealType = currentWindow.type,
                currentWindow = currentWindow,
                reason = "already_completed",
                intensity = MealInterventionIntensity.NONE,
            )
        } else {
            val softReason = softInterventionReason(
                analysis = analysis,
                currentMealType = currentWindow.type,
                nowMillis = nowMillis,
            )
            MealCurrentInterventionStatus(
                isActionable = true,
                actionableMealType = currentWindow.type,
                currentWindow = currentWindow,
                reason = softReason ?: "normal_actionable",
                intensity = if (softReason == null) {
                    MealInterventionIntensity.NORMAL
                } else {
                    MealInterventionIntensity.SOFT
                },
            )
        }
    }

    private fun softInterventionReason(
        analysis: MealLogAnalysis,
        currentMealType: MealType,
        nowMillis: Long,
    ): String? {
        if (analysis.hasIrregularIntake) return "irregular_intake"
        if (analysis.nextMealDelayMinutesByType[currentMealType] != null) return "recent_additional_intake"
        val hasRecentAdditionalIntake = analysis.analyzedLogs.any { analyzed ->
            val isAdditional = analyzed.role == MealLogRole.ADDITIONAL_INTAKE ||
                analyzed.role == MealLogRole.EARLY_NEXT_MEAL
            isAdditional &&
                analyzed.log.timestamp <= nowMillis &&
                nowMillis - analyzed.log.timestamp <= RECENT_ADDITIONAL_INTAKE_MILLIS
        }
        return if (hasRecentAdditionalIntake) "recent_additional_intake" else null
    }

    private fun parseTimeToMinutes(
        value: String,
        fallback: Int,
        midnightAsEndOfDay: Boolean = false,
    ): Int {
        val parts = value.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()
        val minute = parts.getOrNull(1)?.toIntOrNull()
        if (hour == null || minute == null || hour !in 0..24 || minute !in 0..59) {
            return fallback
        }
        if (hour == 24 && minute != 0) return fallback
        if (midnightAsEndOfDay && hour == 0 && minute == 0) return MINUTES_PER_DAY
        return hour * 60 + minute
    }

    companion object {
        private const val MINUTES_PER_DAY = 24 * 60
        private const val DEFAULT_WAKE_TIME_MINUTES = 8 * 60
        private const val DEFAULT_BED_TIME_MINUTES = 24 * 60
        private const val RECENT_ADDITIONAL_INTAKE_MILLIS = 2 * 60 * 60 * 1000L
    }
}
