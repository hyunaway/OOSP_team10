// 경로: com/example/habittracker/domain/usecase/reports/GetMonthlyReportUseCase.kt
package com.example.habittracker.domain.usecase.reports

import com.example.habittracker.domain.model.DailyWaterSummary
import com.example.habittracker.domain.model.MonthlyReportState
import com.example.habittracker.domain.model.WeeklySnapshot
import com.example.habittracker.domain.repository.WaterRepository
import com.example.habittracker.domain.usecase.digital.AnalyzeDigitalPatternUseCase
import com.example.habittracker.domain.usecase.meal.AnalyzeMealPatternUseCase
import com.example.habittracker.domain.usecase.water.AnalyzeWaterPatternUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetMonthlyReportUseCase @Inject constructor(
    private val waterRepository: WaterRepository,
    private val analyzeWaterPatternUseCase: AnalyzeWaterPatternUseCase,
    private val analyzeMealPatternUseCase: AnalyzeMealPatternUseCase,
    private val analyzeDigitalPatternUseCase: AnalyzeDigitalPatternUseCase,
) {
    operator fun invoke(startDate: String, endDate: String): Flow<MonthlyReportState> =
        combine(
            waterRepository.getLogsBetween(startDate, endDate),
            analyzeWaterPatternUseCase(),
            analyzeMealPatternUseCase(),
            analyzeDigitalPatternUseCase(),
        ) { waterSummaries, waterPattern, mealPattern, digitalPattern ->
            val overallRate = waterSummaries.map { it.achievementRate }.average()
                .toFloat().takeIf { it.isFinite() } ?: 0f
            MonthlyReportState(
                monthLabel = startDate.take(7),
                weeklySnapshots = computeWeeklySnapshots(waterSummaries),
                waterPattern = waterPattern,
                mealPattern = mealPattern,
                digitalPattern = digitalPattern,
                stretchPattern = null,
                overallAchievementRate = overallRate,
            )
        }

    private fun computeWeeklySnapshots(summaries: List<DailyWaterSummary>): List<WeeklySnapshot> {
        if (summaries.isEmpty()) return emptyList()
        val cal = Calendar.getInstance()
        return summaries
            .groupBy { summary ->
                cal.timeInMillis = summary.date
                cal.get(Calendar.WEEK_OF_YEAR)
            }
            .map { (week, weekSummaries) ->
                WeeklySnapshot(
                    weekLabel = "Week $week",
                    achievementRate = weekSummaries.map { it.achievementRate }
                        .average().toFloat().takeIf { it.isFinite() } ?: 0f,
                )
            }
            .sortedBy { it.weekLabel }
    }
}
