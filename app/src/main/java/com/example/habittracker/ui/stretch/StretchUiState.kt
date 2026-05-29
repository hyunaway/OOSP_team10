// 경로: com/example/habittracker/ui/stretch/StretchUiState.kt
package com.example.habittracker.ui.stretch

import com.example.habittracker.domain.model.DailyStretchSummary
import com.example.habittracker.domain.model.StretchTodayStatus

enum class StretchButtonState {
    DISABLED_COMPLETED, // 오늘 횟수 >= 4 로 인한 비활성화
    EDITABLE,           // 오늘 횟수 < 4 이고 해당 시간대 기록 존재
    INPUTTABLE          // 오늘 횟수 < 4 이고 해당 시간대 기록 없음
}

data class StretchUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val todayStatus: StretchTodayStatus? = null,
    val history: List<DailyStretchSummary> = emptyList(),
    val streak: Int = 0,
    val buttonStates: Map<String, StretchButtonState> = emptyMap(),
    val todayCount: Int = 0,
    val isHalfGoalAchieved: Boolean = false
)
