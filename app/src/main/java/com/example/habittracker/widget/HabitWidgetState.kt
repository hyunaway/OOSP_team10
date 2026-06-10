package com.example.habittracker.widget

import java.time.LocalDateTime

data class MealWidgetData(
    val lastMealTime: LocalDateTime?,
    val todayRecordCount: Int,
    val targetMealCount: Int = 3,
    val breakfastLogged: Boolean = false,
    val lunchLogged: Boolean = false,
    val dinnerLogged: Boolean = false,
)

data class WaterWidgetData(
    val currentMl: Int,
    val goalMl: Int,
)

data class DigitalWidgetData(
    val usageMinutes: Int,
    val goalMinutes: Int = 120,
)

data class StretchTimerWidgetState(
    val isRunning: Boolean,
    val remainingSeconds: Int,
    val isCompleted: Boolean,
)

data class StretchWidgetData(
    val lastStretchAtMillis: Long?,
    val totalCount: Int,
    val personalizedGoalCount: Int = 5,
    val timerState: StretchTimerWidgetState,
)

data class HabitWidgetState(
    val dominantCategory: HabitCategory,
    val avatarResId: Int,
    val categoryCards: List<HabitCardState>,
)

data class HabitCardState(
    val category: HabitCategory,
    val iconResId: Int,
    val statusLabel: String,
    val description: String,
    val actionLabel: String,
    val riskLevel: RiskLevel,
    val isActionable: Boolean,
    val mealData: MealWidgetData? = null,
    val waterData: WaterWidgetData? = null,
    val digitalData: DigitalWidgetData? = null,
    val stretchData: StretchWidgetData? = null,
)

enum class HabitCategory { MEAL, WATER, DIGITAL, STRETCH, GOOD }

enum class RiskLevel { NORMAL, WARNING, DANGER }

enum class WidgetActionType { MEAL, WATER, STRETCH }
