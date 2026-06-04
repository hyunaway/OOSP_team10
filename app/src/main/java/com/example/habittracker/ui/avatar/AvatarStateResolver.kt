// 경로: com/example/habittracker/ui/avatar/AvatarStateResolver.kt
package com.example.habittracker.ui.avatar

import com.example.habittracker.domain.model.DigitalTodayStatus
import com.example.habittracker.domain.model.MealTodayStatus
import com.example.habittracker.domain.model.StretchTodayStatus
import com.example.habittracker.domain.model.WaterTodayStatus
import com.example.habittracker.domain.usecase.meal.MealCurrentInterventionStatus
import com.example.habittracker.domain.usecase.meal.MealInterventionIntensity
import com.example.habittracker.data.model.MealType

object AvatarStateResolver {

    private const val WATER_LACK_THRESHOLD = 0.5f
    private const val DEFAULT_DIGITAL_LIMIT_MINUTES = 120
    private const val MEAL_MIN_LOGGED_COUNT = 2
    private const val STRETCH_GOAL_COUNT = 5

    fun resolve(
        mealStatus: MealTodayStatus,
        waterStatus: WaterTodayStatus,
        digitalStatus: DigitalTodayStatus,
        stretchStatus: StretchTodayStatus,
        digitalLimitMinutes: Int = DEFAULT_DIGITAL_LIMIT_MINUTES,
        isMealActionable: Boolean = true,
    ): AvatarResolveResult {
        val activeStates = buildList {
            if (isMealActionable && isMealLacking(mealStatus)) add(AvatarState.MEAL_LACK)
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
        status.totalCount < STRETCH_GOAL_COUNT

    fun bubbleMessageFor(
        primaryState: AvatarState,
        mealInterventionStatus: MealCurrentInterventionStatus,
    ): String {
        if (primaryState != AvatarState.MEAL_LACK || !mealInterventionStatus.isActionable) {
            return primaryState.bubbleMessage
        }
        return when (mealInterventionStatus.intensity) {
            MealInterventionIntensity.SOFT ->
                "오늘은 식사 리듬을 조금 여유 있게 볼게요.\n가볍게 챙길 수 있을 때만 챙겨요."
            MealInterventionIntensity.NORMAL ->
                "아직 ${mealLabel(mealInterventionStatus.actionableMealType)}을 챙기지 않았어요.\n가볍게 챙겨볼까요? 🍽️"
            MealInterventionIntensity.NONE ->
                primaryState.bubbleMessage
        }
    }

    private fun mealLabel(type: MealType?): String =
        when (type) {
            MealType.BREAKFAST -> "아침"
            MealType.LUNCH -> "점심"
            MealType.DINNER -> "저녁"
            MealType.LATE_NIGHT,
            null -> "식사"
        }
}

data class AvatarResolveResult(
    val primaryState: AvatarState,
    val activeStates: List<AvatarState>,
)
