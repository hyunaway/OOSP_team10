// 경로: com/example/habittracker/domain/model/DailySummary.kt
package com.example.habittracker.domain.model

data class DailyWaterSummary(
    val date: Long,
    val totalMl: Int,
    val interventionCount: Int,
    val achievementRate: Float,
)

data class DailyMealSummary(
    val date: Long,
    val mealMap: Map<String, Boolean>,
    val lateNightCount: Int,
    val deliveryAppCount: Int,
)

data class DailyDigitalSummary(
    val date: Long,
    val totalMinutes: Int,
    val appBreakdown: Map<String, Int>,
    val interventionCount: Int,
    val reactedCount: Int,
)

data class DailyStretchSummary(
    val date: Long,
    val count: Int,
    val dominantBodyPart: String?,
    val totalSeconds: Int,
)
