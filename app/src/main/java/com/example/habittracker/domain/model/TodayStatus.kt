// 경로: com/example/habittracker/domain/model/TodayStatus.kt
package com.example.habittracker.domain.model

data class WaterTodayStatus(
    val totalMl: Int,
    val goalMl: Int,
    val achievementRate: Float,
    val lastDrankAt: Long?,
    val interventionCount: Int,
)

data class MealTodayStatus(
    val breakfastLogged: Boolean,
    val lunchLogged: Boolean,
    val dinnerLogged: Boolean,
    val lateNightCount: Int,
    val lateNightLogged: Boolean = false,
    val lastMealAt: Long?,
)

data class DigitalTodayStatus(
    val totalUsageMinutes: Int,
    val appUsageMap: Map<String, Int>,
    val interventionCount: Int,
    val reactedCount: Int,
    val topApp: String?,
)

data class StretchTodayStatus(
    val totalCount: Int,
    val lastStretchAt: Long?,
    val bodyPartMap: Map<String, Int>,
    val totalSeconds: Int,
    val avatarHealthScore: Float,
    val slotsLogged: List<String> = emptyList(),
)
