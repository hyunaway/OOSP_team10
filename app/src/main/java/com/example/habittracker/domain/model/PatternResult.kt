// 경로: com/example/habittracker/domain/model/PatternResult.kt
package com.example.habittracker.domain.model

data class WaterPatternResult(
    val peakHours: List<Int>,
    val lowResponseHours: List<Int>,
    val weekdayPattern: Map<Int, Int>,
    val weekendPattern: Map<Int, Int>,
)

data class MealPatternResult(
    val lateNightRiskHour: Int?,
    val skippedMealPattern: Map<String, Int>,
    val weekdayMealTimeMap: Map<String, Int>,
    val weekendMealTimeMap: Map<String, Int>,
)

data class DigitalPatternResult(
    val avgSessionByApp: Map<String, Float>,
    val bedtimeUsageScore: Float,
    val toneReactionRate: Map<String, Float>,
    val peakUsageHours: List<Int>,
    val lowReactionHours: List<Int>,
)

data class StretchPatternResult(
    val inactiveHours: List<Int>,
    val digitalTriggerConversionRate: Float,
    val preferredBodyPart: String?,
    val weekdayStretchPattern: Map<Int, Int>,
    val weekendStretchPattern: Map<Int, Int>,
)
