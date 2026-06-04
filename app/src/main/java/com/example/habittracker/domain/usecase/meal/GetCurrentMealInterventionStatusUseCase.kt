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
    val reason: String,
)

@Singleton
class GetCurrentMealInterventionStatusUseCase @Inject constructor(
    private val mealRepository: MealRepository,
    private val userPreferenceManager: UserPreferenceManager,
    private val mealWindowCalculator: MealWindowCalculator,
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
        val currentWindow = mealWindowCalculator.findCurrentWindow(
            windows = mealWindowCalculator.calculate(wakeTimeMinutes, bedTimeMinutes),
            currentMinutes = currentMinutes,
        ) ?: return MealCurrentInterventionStatus(
            isActionable = false,
            actionableMealType = null,
            reason = "No current meal window.",
        )

        val todayLogs = mealRepository.getLogsByMealDate(LocalDate.now(zoneId).toString())
        val alreadyLogged = todayLogs.any { log ->
            log.type == currentWindow.mealType && !log.isLateNight
        }
        return if (alreadyLogged) {
            MealCurrentInterventionStatus(
                isActionable = false,
                actionableMealType = currentWindow.mealType,
                reason = "Current meal already logged.",
            )
        } else {
            MealCurrentInterventionStatus(
                isActionable = true,
                actionableMealType = currentWindow.mealType,
                reason = "Current meal window is active and unlogged.",
            )
        }
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
    }
}
