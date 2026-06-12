// 경로: com/example/habittracker/domain/usecase/reports/GetMonthlyReportUseCase.kt
package com.example.habittracker.domain.usecase.reports

import com.example.habittracker.domain.model.DailyWaterSummary
import com.example.habittracker.domain.model.DailyMealSummary
import com.example.habittracker.domain.model.DailyDigitalSummary
import com.example.habittracker.domain.model.DailyStretchSummary
import com.example.habittracker.domain.model.MonthlyReportState
import com.example.habittracker.domain.model.WeeklySnapshot
import com.example.habittracker.domain.repository.WaterRepository
import com.example.habittracker.domain.repository.MealRepository
import com.example.habittracker.domain.repository.DigitalRepository
import com.example.habittracker.domain.repository.StretchRepository
import com.example.habittracker.domain.analysis.PersonalizationResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetMonthlyReportUseCase @Inject constructor(
    private val waterRepository: WaterRepository,
    private val mealRepository: MealRepository,
    private val digitalRepository: DigitalRepository,
    private val stretchRepository: StretchRepository,
    private val personalizationResolver: PersonalizationResolver,
) {
    operator fun invoke(startDate: String, endDate: String): Flow<MonthlyReportState> =
        combine(
            waterRepository.getLogsBetween(startDate, endDate),
            mealRepository.getLogsBetween(startDate, endDate),
            digitalRepository.getLogsBetween(startDate, endDate, null),
            stretchRepository.getLogsBetween(startDate, endDate),
        ) { water, meal, digital, stretch ->
            // 1. 각 카테고리별 주차별 스냅샷 연산
            val waterSnapshots = computeWaterWeeklySnapshots(water)
            val mealSnapshots = computeMealWeeklySnapshots(meal)
            val digitalSnapshots = computeDigitalWeeklySnapshots(digital)
            val stretchSnapshots = computeStretchWeeklySnapshots(stretch)

            // 2. 종합 주차별 스냅샷 (4대 카테고리 평균)
            val allWeekLabels = (waterSnapshots.map { it.weekLabel } +
                    mealSnapshots.map { it.weekLabel } +
                    digitalSnapshots.map { it.weekLabel } +
                    stretchSnapshots.map { it.weekLabel }).distinct().sorted()

            val weeklySnapshots = allWeekLabels.map { label ->
                val wRate = waterSnapshots.find { it.weekLabel == label }?.achievementRate ?: 0f
                val mRate = mealSnapshots.find { it.weekLabel == label }?.achievementRate ?: 0f
                val dRate = digitalSnapshots.find { it.weekLabel == label }?.achievementRate ?: 0f
                val sRate = stretchSnapshots.find { it.weekLabel == label }?.achievementRate ?: 0f
                WeeklySnapshot(
                    weekLabel = label,
                    achievementRate = (wRate + mRate + dRate + sRate) / 4f
                )
            }

            // 3. 카테고리별 한 달 평균 달성률
            val overallWater = water.map { it.achievementRate }.average().toFloat().takeIf { it.isFinite() } ?: 0f
            val overallMeal = meal.map {
                val logged = it.mealMap.values.count { isLogged -> isLogged }
                (logged.toFloat() / 3f).coerceIn(0f, 1f)
            }.average().toFloat().takeIf { it.isFinite() } ?: 0f
            
            val overallDigital = digital.map {
                if (it.interventionCount == 0) 1f
                else (it.reactedCount.toFloat() / it.interventionCount.toFloat()).coerceIn(0f, 1f)
            }.average().toFloat().takeIf { it.isFinite() } ?: 0f

            val stretchGoal = kotlinx.coroutines.runBlocking { personalizationResolver.resolveStretchGoalCount() }.coerceAtLeast(1)
            val overallStretch = stretch.map {
                (it.count.toFloat() / stretchGoal.toFloat()).coerceIn(0f, 1f)
            }.average().toFloat().takeIf { it.isFinite() } ?: 0f

            val overallRate = (overallWater + overallMeal + overallDigital + overallStretch) / 4f

            MonthlyReportState(
                monthLabel = startDate.take(7),
                weeklySnapshots = weeklySnapshots,
                waterPattern = null,
                mealPattern = null,
                digitalPattern = null,
                stretchPattern = null,
                overallAchievementRate = overallRate,
                weeklyWaterSnapshots = waterSnapshots,
                weeklyMealSnapshots = mealSnapshots,
                weeklyDigitalSnapshots = digitalSnapshots,
                weeklyStretchSnapshots = stretchSnapshots,
            )
        }

    private fun computeWaterWeeklySnapshots(summaries: List<DailyWaterSummary>): List<WeeklySnapshot> {
        if (summaries.isEmpty()) return emptyList()
        val cal = Calendar.getInstance()
        val groupedByYearWeek = summaries
            .groupBy { summary ->
                cal.timeInMillis = summary.date
                cal.get(Calendar.WEEK_OF_YEAR)
            }
            .toList()
            .sortedBy { it.first }

        return groupedByYearWeek.mapIndexed { index, (_, weekSummaries) ->
            WeeklySnapshot(
                weekLabel = "Week ${index + 1}",
                achievementRate = weekSummaries.map { it.achievementRate }
                    .average().toFloat().takeIf { it.isFinite() } ?: 0f,
            )
        }
    }

    private fun computeMealWeeklySnapshots(summaries: List<DailyMealSummary>): List<WeeklySnapshot> {
        if (summaries.isEmpty()) return emptyList()
        val cal = Calendar.getInstance()
        val groupedByYearWeek = summaries
            .groupBy { summary ->
                cal.timeInMillis = summary.date
                cal.get(Calendar.WEEK_OF_YEAR)
            }
            .toList()
            .sortedBy { it.first }

        return groupedByYearWeek.mapIndexed { index, (_, weekSummaries) ->
            WeeklySnapshot(
                weekLabel = "Week ${index + 1}",
                achievementRate = weekSummaries.map {
                    val logged = it.mealMap.values.count { isLogged -> isLogged }
                    (logged.toFloat() / 3f).coerceIn(0f, 1f)
                }.average().toFloat().takeIf { it.isFinite() } ?: 0f,
            )
        }
    }

    private fun computeDigitalWeeklySnapshots(summaries: List<DailyDigitalSummary>): List<WeeklySnapshot> {
        if (summaries.isEmpty()) return emptyList()
        val cal = Calendar.getInstance()
        val groupedByYearWeek = summaries
            .groupBy { summary ->
                cal.timeInMillis = summary.date
                cal.get(Calendar.WEEK_OF_YEAR)
            }
            .toList()
            .sortedBy { it.first }

        return groupedByYearWeek.mapIndexed { index, (_, weekSummaries) ->
            WeeklySnapshot(
                weekLabel = "Week ${index + 1}",
                achievementRate = weekSummaries.map {
                    if (it.interventionCount == 0) 1f
                    else (it.reactedCount.toFloat() / it.interventionCount.toFloat()).coerceIn(0f, 1f)
                }.average().toFloat().takeIf { it.isFinite() } ?: 0f,
            )
        }
    }

    private fun computeStretchWeeklySnapshots(summaries: List<DailyStretchSummary>): List<WeeklySnapshot> {
        if (summaries.isEmpty()) return emptyList()
        val cal = Calendar.getInstance()
        val groupedByYearWeek = summaries
            .groupBy { summary ->
                cal.timeInMillis = summary.date
                cal.get(Calendar.WEEK_OF_YEAR)
            }
            .toList()
            .sortedBy { it.first }

        val stretchGoal = kotlinx.coroutines.runBlocking { personalizationResolver.resolveStretchGoalCount() }.coerceAtLeast(1)

        return groupedByYearWeek.mapIndexed { index, (_, weekSummaries) ->
            WeeklySnapshot(
                weekLabel = "Week ${index + 1}",
                achievementRate = weekSummaries.map {
                    (it.count.toFloat() / stretchGoal.toFloat()).coerceIn(0f, 1f)
                }.average().toFloat().takeIf { it.isFinite() } ?: 0f,
            )
        }
    }
}
