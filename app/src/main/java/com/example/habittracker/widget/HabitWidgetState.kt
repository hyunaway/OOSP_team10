// 경로: com/example/habittracker/widget/HabitWidgetState.kt
package com.example.habittracker.widget

import com.example.habittracker.domain.model.WaterShortageLevel

data class HabitWidgetState(
    val waterTotalMl: Int,
    val isNeedWater: Boolean,
    val waterShortageLevel: WaterShortageLevel,
    val waterStatusText: String,
    val speechBubbleMessage: String,
    val abnormalStatusText: String,
    val abnormalStatusType: WidgetAbnormalStatusType,
    val abnormalStatusColor: Int,
    val widgetMessage: String,
    val stretchCount: Int,
    val avatarHealthScore: Int,
    val avatarEmoji: String,
    // 4x2 정보형 위젯용 상태 필드
    val dominantCategory: WidgetHabitCategory = WidgetHabitCategory.GOOD,
    val gender: WidgetGender = WidgetGender.MALE,
    val mealStatus: MealStatus = MealStatus.NORMAL,
    val stretchStatus: StretchStatus = StretchStatus.NORMAL,
    val digitalUsageMinutes: Int = 0,
    // 카테고리별 상세 상태 목록 (StackView 등 다중 항목 표시용)
    val categoryStates: List<WidgetHabitState> = emptyList(),
)

/** 카테고리별 위험 상태를 담는 단위 모델 */
data class WidgetHabitState(
    val category: WidgetHabitCategory,
    val isRisk: Boolean,
    val title: String,
    val message: String,
    val description: String,
    val actionText: String,
)

enum class WidgetAbnormalStatusType {
    NONE,
    WATER,
    DIGITAL,
    STRETCH,
}

// 대표 상태 우선순위: MEAL > WATER > DIGITAL > STRETCH > GOOD
enum class WidgetHabitCategory {
    MEAL,
    WATER,
    DIGITAL,
    STRETCH,
    GOOD,
}

enum class WidgetGender {
    MALE,
    FEMALE,
}

enum class MealStatus {
    NORMAL,
    LACK,
}

enum class StretchStatus {
    NORMAL,
    LACK,
}
