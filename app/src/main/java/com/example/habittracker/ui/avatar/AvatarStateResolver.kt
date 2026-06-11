// 경로: com/example/habittracker/ui/avatar/AvatarStateResolver.kt
package com.example.habittracker.ui.avatar

import com.example.habittracker.domain.model.DigitalTodayStatus
import com.example.habittracker.domain.model.MealTodayStatus
import com.example.habittracker.domain.model.StretchTodayStatus
import com.example.habittracker.domain.model.WaterTodayStatus
import com.example.habittracker.domain.usecase.meal.MealCurrentInterventionStatus
import com.example.habittracker.domain.usecase.meal.MealInterventionIntensity
import com.example.habittracker.data.model.MealType

import com.example.habittracker.domain.model.WaterInterventionStatus
import com.example.habittracker.domain.model.WaterShortageLevel
import com.example.habittracker.domain.model.StretchInterventionStatus

object AvatarStateResolver {

    private const val DEFAULT_DIGITAL_LIMIT_MINUTES = 120

    fun resolve(
        mealIntervention: MealCurrentInterventionStatus,
        waterIntervention: WaterInterventionStatus,
        digitalStatus: DigitalTodayStatus,
        stretchIntervention: StretchInterventionStatus,
        priorityOrder: List<String>,
        digitalLimitMinutes: Int = DEFAULT_DIGITAL_LIMIT_MINUTES,
        isThreeDaySkip: Boolean = false,
        allMeals3Days: Boolean = false,
    ): AvatarResolveResult {
        val isMealLack = mealIntervention.isActionable
        val isWaterLack = waterIntervention.shortageLevel != WaterShortageLevel.NONE
        val isDigitalOveruse = digitalStatus.totalUsageMinutes > digitalLimitMinutes
        val isStretchLack = stretchIntervention.isNeedStretch

        val activeStates = buildList {
            if (isMealLack) add(AvatarState.MEAL_LACK)
            if (isWaterLack) add(AvatarState.WATER_LACK)
            if (isDigitalOveruse) add(AvatarState.DIGITAL_OVERUSE)
            if (isStretchLack) add(AvatarState.STRETCH_LACK)
        }

        val primaryState = when {
            allMeals3Days -> AvatarState.GOOD
            isThreeDaySkip && mealIntervention.isActionable -> AvatarState.WARNING
            else -> {
                // 우선순위가 높은 순서대로 부족한 상태가 발견되면 그것을 primaryState로 지정
                var state = AvatarState.GOOD
                for (category in priorityOrder) {
                    val isLack = when (category.uppercase()) {
                        "MEAL" -> isMealLack
                        "WATER" -> isWaterLack
                        "DIGITAL" -> isDigitalOveruse
                        "STRETCH" -> isStretchLack
                        else -> false
                    }
                    if (isLack) {
                        state = when (category.uppercase()) {
                            "MEAL" -> AvatarState.MEAL_LACK
                            "WATER" -> AvatarState.WATER_LACK
                            "DIGITAL" -> AvatarState.DIGITAL_OVERUSE
                            "STRETCH" -> AvatarState.STRETCH_LACK
                            else -> AvatarState.GOOD
                        }
                        break
                    }
                }
                state
            }
        }

        return AvatarResolveResult(primaryState = primaryState, activeStates = activeStates)
    }

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
