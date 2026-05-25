// 경로: com/example/habittracker/ui/stretch/StretchUiState.kt
package com.example.habittracker.ui.stretch

import com.example.habittracker.domain.model.DailyStretchSummary
import com.example.habittracker.domain.model.StretchTodayStatus

data class StretchUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val todayStatus: StretchTodayStatus? = null,
    val history: List<DailyStretchSummary> = emptyList(),
)
