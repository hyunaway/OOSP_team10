// 경로: com/example/habittracker/ui/avatar/AvatarStateResolver.kt
package com.example.habittracker.ui.avatar

import com.example.habittracker.domain.model.DigitalTodayStatus
import com.example.habittracker.domain.model.MealTodayStatus
import com.example.habittracker.domain.model.StretchTodayStatus
import com.example.habittracker.domain.model.WaterTodayStatus

object AvatarStateResolver {

    private const val WATER_LACK_THRESHOLD = 0.5f
    private const val DEFAULT_DIGITAL_LIMIT_MINUTES = 120
    private const val MEAL_MIN_LOGGED_COUNT = 2

    fun resolve(
        mealStatus: MealTodayStatus,
        waterStatus: WaterTodayStatus,
        digitalStatus: DigitalTodayStatus,
        stretchStatus: StretchTodayStatus,
        digitalLimitMinutes: Int = DEFAULT_DIGITAL_LIMIT_MINUTES,
    ): AvatarResolveResult {
        val activeStates = buildList {
            if (isMealLacking(mealStatus)) add(AvatarState.MEAL_LACK)
            if (isWaterLacking(waterStatus)) add(AvatarState.WATER_LACK)
            if (isDigitalOveruse(digitalStatus, digitalLimitMinutes)) add(AvatarState.DIGITAL_OVERUSE)
            if (isStretchLacking(stretchStatus)) add(AvatarState.STRETCH_LACK)
        }

        val primaryState = activeStates.minByOrNull { it.priority } ?: AvatarState.GOOD
        return AvatarResolveResult(primaryState = primaryState, activeStates = activeStates)
    }

    private fun isMealLacking(status: MealTodayStatus): Boolean {
        val loggedCount = listOf(status.breakfastLogged, status.lunchLogged, status.dinnerLogged)
            .count { it }
        return loggedCount < MEAL_MIN_LOGGED_COUNT
    }

    private fun isWaterLacking(status: WaterTodayStatus): Boolean =
        status.achievementRate < WATER_LACK_THRESHOLD

    private fun isDigitalOveruse(status: DigitalTodayStatus, limitMinutes: Int): Boolean =
        status.totalUsageMinutes > limitMinutes

    private fun isStretchLacking(status: StretchTodayStatus): Boolean =
        status.totalCount == 0
}

data class AvatarResolveResult(
    val primaryState: AvatarState,
    val activeStates: List<AvatarState>,
)
