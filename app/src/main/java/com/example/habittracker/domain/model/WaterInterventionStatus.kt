package com.example.habittracker.domain.model

data class WaterInterventionStatus(
    val baseGoalMl: Int,
    val effectiveGoalMl: Int,
    val recommendedAmountMl: Int,
    val currentAmountMl: Int,
    val shortageMl: Int,
    val isNeedWater: Boolean,
    val shortageLevel: WaterShortageLevel,
    val message: String,
)

enum class WaterShortageLevel {
    NONE,
    LIGHT,
    MEDIUM,
    SEVERE,
}
