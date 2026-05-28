// 경로: com/example/habittracker/domain/usecase/reports/GetWeeklyReportUseCase.kt
package com.example.habittracker.domain.usecase.reports

import com.example.habittracker.domain.model.WeeklyReportState
import com.example.habittracker.domain.repository.DigitalRepository
import com.example.habittracker.domain.repository.MealRepository
import com.example.habittracker.domain.repository.StretchRepository
import com.example.habittracker.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetWeeklyReportUseCase @Inject constructor(
    private val waterRepository: WaterRepository,
    private val mealRepository: MealRepository,
    private val digitalRepository: DigitalRepository,
    private val stretchRepository: StretchRepository,
) {
    operator fun invoke(startDate: String, endDate: String): Flow<WeeklyReportState> =
        combine(
            waterRepository.getLogsBetween(startDate, endDate),
            mealRepository.getLogsBetween(startDate, endDate),
            digitalRepository.getLogsBetween(startDate, endDate, null),
            stretchRepository.getLogsBetween(startDate, endDate, null),
        ) { water, meal, digital, stretch ->
            val overallRate = water.map { it.achievementRate }.average()
                .toFloat().takeIf { it.isFinite() } ?: 0f
            WeeklyReportState(
                weekLabel = "$startDate ~ $endDate",
                dailyWaterSummaries = water,
                dailyMealSummaries = meal,
                dailyDigitalSummaries = digital,
                dailyStretchSummaries = stretch,
                waterPattern = null,
                mealPattern = null,
                digitalPattern = null,
                stretchPattern = null,
                overallAchievementRate = overallRate,
            )
        }
}
