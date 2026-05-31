// 경로: com/example/habittracker/widget/WidgetResourceMapper.kt
package com.example.habittracker.widget

import com.example.habittracker.R
import com.example.habittracker.domain.model.WaterShortageLevel

object WidgetResourceMapper {

    const val WATER_GOAL_ML = 2000
    const val STRETCH_GOAL_COUNT = 5

    /** 기본 카테고리 우선순위: MEAL > WATER > DIGITAL > STRETCH */
    val DEFAULT_PRIORITY_ORDER: List<WidgetHabitCategory> = listOf(
        WidgetHabitCategory.MEAL,
        WidgetHabitCategory.WATER,
        WidgetHabitCategory.DIGITAL,
        WidgetHabitCategory.STRETCH,
    )

    fun parsePriorityOrder(raw: String): List<WidgetHabitCategory> {
        val parsed = raw.split(",")
            .mapNotNull { name ->
                runCatching { WidgetHabitCategory.valueOf(name.trim()) }.getOrNull()
            }
            .filter { it != WidgetHabitCategory.GOOD }
        return parsed.ifEmpty { DEFAULT_PRIORITY_ORDER }
    }

    fun speechBubbleText(category: WidgetHabitCategory): String = when (category) {
        WidgetHabitCategory.MEAL -> "밥 먹을 시간이야! 오늘 식사는 챙겼어?"
        WidgetHabitCategory.WATER -> "물 한 잔 마시면 컨디션이 좋아질 거야!"
        WidgetHabitCategory.DIGITAL -> "눈이 피곤해 보여. 잠깐 쉬어볼까?"
        WidgetHabitCategory.STRETCH -> "몸이 굳었어! 가볍게 기지개 켜볼까?"
        WidgetHabitCategory.GOOD -> "오늘 습관 상태 좋아! 계속 유지해보자!"
    }

    private fun goodText(category: WidgetHabitCategory): String = when (category) {
        WidgetHabitCategory.MEAL -> "오늘 식사 잘 챙겼어!"
        WidgetHabitCategory.WATER -> "수분 섭취 잘하고 있어!"
        WidgetHabitCategory.DIGITAL -> "디지털 사용 양호해!"
        WidgetHabitCategory.STRETCH -> "스트레칭 잘하고 있어!"
        WidgetHabitCategory.GOOD -> "오늘 습관 상태 좋아! 계속 유지해보자!"
    }

    fun avatarResId(category: WidgetHabitCategory, gender: WidgetGender): Int = when (gender) {
        WidgetGender.MALE -> when (category) {
            WidgetHabitCategory.GOOD -> R.drawable.widget_avatar_male_good
            WidgetHabitCategory.MEAL -> R.drawable.widget_avatar_male_meal_lack
            WidgetHabitCategory.WATER -> R.drawable.widget_avatar_male_water_lack
            WidgetHabitCategory.DIGITAL -> R.drawable.widget_avatar_male_digital_overuse
            WidgetHabitCategory.STRETCH -> R.drawable.widget_avatar_male_stretch_lack
        }
        WidgetGender.FEMALE -> when (category) {
            WidgetHabitCategory.GOOD -> R.drawable.widget_avatar_female_good
            WidgetHabitCategory.MEAL -> R.drawable.widget_avatar_female_meal_lack
            WidgetHabitCategory.WATER -> R.drawable.widget_avatar_female_water_lack
            WidgetHabitCategory.DIGITAL -> R.drawable.widget_avatar_female_digital_overuse
            WidgetHabitCategory.STRETCH -> R.drawable.widget_avatar_female_stretch_lack
        }
    }

    fun resolveDominantCategory(
        mealStatus: MealStatus,
        waterShortageLevel: WaterShortageLevel,
        isDigitalOveruse: Boolean,
        stretchStatus: StretchStatus,
        priorityOrder: List<WidgetHabitCategory> = DEFAULT_PRIORITY_ORDER,
    ): WidgetHabitCategory {
        val riskMap = mapOf(
            WidgetHabitCategory.MEAL to (mealStatus == MealStatus.LACK),
            WidgetHabitCategory.WATER to (waterShortageLevel != WaterShortageLevel.NONE),
            WidgetHabitCategory.DIGITAL to isDigitalOveruse,
            WidgetHabitCategory.STRETCH to (stretchStatus == StretchStatus.LACK),
        )
        return priorityOrder.firstOrNull { riskMap[it] == true } ?: WidgetHabitCategory.GOOD
    }

    /** 카테고리별 WidgetHabitState 목록을 생성한다 (StackView 등 다중 항목 표시용). */
    fun buildCategoryStates(
        mealStatus: MealStatus,
        waterShortageLevel: WaterShortageLevel,
        waterTotalMl: Int,
        isDigitalOveruse: Boolean,
        digitalUsageMinutes: Int,
        stretchStatus: StretchStatus,
        stretchCount: Int,
    ): List<WidgetHabitState> {
        val mealRisk = mealStatus == MealStatus.LACK
        val waterRisk = waterShortageLevel != WaterShortageLevel.NONE
        val stretchRisk = stretchStatus == StretchStatus.LACK
        return listOf(
            WidgetHabitState(
                category = WidgetHabitCategory.MEAL,
                isRisk = mealRisk,
                title = "식사",
                message = if (mealRisk) speechBubbleText(WidgetHabitCategory.MEAL) else goodText(WidgetHabitCategory.MEAL),
                description = if (mealRisk) "식사를 놓친 것 같아요" else "식사 기록 완료",
                actionText = "식사 기록하기",
            ),
            WidgetHabitState(
                category = WidgetHabitCategory.WATER,
                isRisk = waterRisk,
                title = "수분",
                message = if (waterRisk) speechBubbleText(WidgetHabitCategory.WATER) else goodText(WidgetHabitCategory.WATER),
                description = "${waterTotalMl}ml / ${WATER_GOAL_ML}ml",
                actionText = "물 마시기",
            ),
            WidgetHabitState(
                category = WidgetHabitCategory.DIGITAL,
                isRisk = isDigitalOveruse,
                title = "디지털",
                message = if (isDigitalOveruse) speechBubbleText(WidgetHabitCategory.DIGITAL) else goodText(WidgetHabitCategory.DIGITAL),
                description = "${digitalUsageMinutes}분 사용",
                actionText = "사용 기록 보기",
            ),
            WidgetHabitState(
                category = WidgetHabitCategory.STRETCH,
                isRisk = stretchRisk,
                title = "스트레칭",
                message = if (stretchRisk) speechBubbleText(WidgetHabitCategory.STRETCH) else goodText(WidgetHabitCategory.STRETCH),
                description = "${stretchCount}회 / ${STRETCH_GOAL_COUNT}회",
                actionText = "스트레칭하기",
            ),
        )
    }
}
