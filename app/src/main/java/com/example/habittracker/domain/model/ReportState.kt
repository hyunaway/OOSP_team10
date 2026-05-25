// 경로: com/example/habittracker/domain/model/ReportState.kt
package com.example.habittracker.domain.model

data class WeeklySnapshot(
    val weekLabel: String,
    val achievementRate: Float,
)

data class WeeklyReportState(
    val weekLabel: String,
    val dailyWaterSummaries: List<DailyWaterSummary>,
    val dailyMealSummaries: List<DailyMealSummary>,
    val dailyDigitalSummaries: List<DailyDigitalSummary>,
    val dailyStretchSummaries: List<DailyStretchSummary>,
    val waterPattern: WaterPatternResult?,
    val mealPattern: MealPatternResult?,
    val digitalPattern: DigitalPatternResult?,
    val stretchPattern: StretchPatternResult?,
    val overallAchievementRate: Float,
)

data class MonthlyReportState(
    val monthLabel: String,
    val weeklySnapshots: List<WeeklySnapshot>,
    val waterPattern: WaterPatternResult?,
    val mealPattern: MealPatternResult?,
    val digitalPattern: DigitalPatternResult?,
    val stretchPattern: StretchPatternResult?,
    val overallAchievementRate: Float,
)
