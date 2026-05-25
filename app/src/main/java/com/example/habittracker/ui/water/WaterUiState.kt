// 경로: com/example/habittracker/ui/water/WaterUiState.kt
package com.example.habittracker.ui.water

import com.example.habittracker.domain.model.DailyWaterSummary
import com.example.habittracker.domain.model.WaterTodayStatus

data class WaterUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val todayStatus: WaterTodayStatus? = null,
    val history: List<DailyWaterSummary> = emptyList(),
)
