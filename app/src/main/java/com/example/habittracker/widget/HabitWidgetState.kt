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
)

enum class WidgetAbnormalStatusType {
    NONE,
    WATER,
    DIGITAL,
    STRETCH,
}
