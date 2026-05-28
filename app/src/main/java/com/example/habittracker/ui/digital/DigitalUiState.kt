// 경로: com/example/habittracker/ui/digital/DigitalUiState.kt
package com.example.habittracker.ui.digital

import com.example.habittracker.domain.model.DigitalTodayStatus

data class DigitalUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val todayStatus: DigitalTodayStatus? = null,
    val selectedDigitalPackages: Set<String> = emptySet(),
)
