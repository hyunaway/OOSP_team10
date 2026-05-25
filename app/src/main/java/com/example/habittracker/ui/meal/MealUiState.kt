// 경로: com/example/habittracker/ui/meal/MealUiState.kt
package com.example.habittracker.ui.meal

import com.example.habittracker.domain.model.DailyMealSummary
import com.example.habittracker.domain.model.MealTodayStatus

data class MealUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val todayStatus: MealTodayStatus? = null,
    val history: List<DailyMealSummary> = emptyList(),
)
