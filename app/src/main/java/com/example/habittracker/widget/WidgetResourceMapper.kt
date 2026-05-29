// 경로: com/example/habittracker/widget/WidgetResourceMapper.kt
package com.example.habittracker.widget

import com.example.habittracker.R
import com.example.habittracker.domain.model.WaterShortageLevel

object WidgetResourceMapper {

    fun speechBubbleText(category: WidgetHabitCategory): String = when (category) {
        WidgetHabitCategory.MEAL -> "밥 먹을 시간이야! 오늘 식사는 챙겼어?"
        WidgetHabitCategory.WATER -> "물 한 잔 마시면 컨디션이 좋아질 거야!"
        WidgetHabitCategory.DIGITAL -> "눈이 피곤해 보여. 잠깐 쉬어볼까?"
        WidgetHabitCategory.STRETCH -> "몸이 굳었어! 가볍게 기지개 켜볼까?"
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
    ): WidgetHabitCategory {
        if (mealStatus == MealStatus.LACK) return WidgetHabitCategory.MEAL
        if (waterShortageLevel != WaterShortageLevel.NONE) return WidgetHabitCategory.WATER
        if (isDigitalOveruse) return WidgetHabitCategory.DIGITAL
        if (stretchStatus == StretchStatus.LACK) return WidgetHabitCategory.STRETCH
        return WidgetHabitCategory.GOOD
    }
}
