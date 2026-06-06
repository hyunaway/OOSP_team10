// 경로: com/example/habittracker/domain/usecase/reports/GetMonthlyReportUseCase.kt
package com.example.habittracker.domain.usecase.reports

import com.example.habittracker.domain.model.DailyWaterSummary
import com.example.habittracker.domain.model.MonthlyReportState
import com.example.habittracker.domain.model.WeeklySnapshot
import com.example.habittracker.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 월간 리포트 UseCase.
 *
 * 패턴 분석 결과(waterPattern 등)는 DataStore에서 직접 소비하도록 분리되었으므로
 * 여기서는 null로 전달한다. 분석 결과가 필요한 UI는 UserPreferenceManager Flow를 참조.
 */
@Singleton
class GetMonthlyReportUseCase @Inject constructor(
    private val waterRepository: WaterRepository,
) {
    operator fun invoke(startDate: String, endDate: String): Flow<MonthlyReportState> =
        waterRepository.getLogsBetween(startDate, endDate).map { waterSummaries ->
            val overallRate = waterSummaries.map { it.achievementRate }.average()
                .toFloat().takeIf { it.isFinite() } ?: 0f
            MonthlyReportState(
                monthLabel            = startDate.take(7),
                weeklySnapshots       = computeWeeklySnapshots(waterSummaries),
                waterPattern          = null, // DataStore에서 직접 소비
                mealPattern           = null,
                digitalPattern        = null,
                stretchPattern        = null,
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
                    weekLabel       = "Week $week",
                    achievementRate = weekSummaries.map { it.achievementRate }
                        .average().toFloat().takeIf { it.isFinite() } ?: 0f,
                )
            }
            .sortedBy { it.weekLabel }
    }
}
