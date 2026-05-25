// 경로: com/example/habittracker/domain/model/DashboardState.kt
package com.example.habittracker.domain.model

data class DashboardState(
    val waterStatus: WaterTodayStatus,
    val mealStatus: MealTodayStatus,
    val stretchStatus: StretchTodayStatus,
    val digitalStatus: DigitalTodayStatus,
    val overallScore: Float,
    val motivationalMessage: String,
)
