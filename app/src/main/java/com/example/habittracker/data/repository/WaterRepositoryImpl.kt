// 경로: com/example/habittracker/data/repository/WaterRepositoryImpl.kt
package com.example.habittracker.data.repository

import android.content.Context
import com.example.habittracker.data.entity.WaterLogEntity
import com.example.habittracker.data.local.room.dao.WaterDao
import com.example.habittracker.domain.model.DailyWaterSummary
import com.example.habittracker.domain.model.WaterPatternResult
import com.example.habittracker.domain.model.WaterTodayStatus
import com.example.habittracker.domain.repository.WaterRepository
import com.example.habittracker.widget.WidgetUpdateHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterRepositoryImpl @Inject constructor(
    private val waterDao: WaterDao,
    @ApplicationContext private val context: Context,
) : WaterRepository {

    override fun getTodayStatus(): Flow<WaterTodayStatus> =
        combine(waterDao.getTodayLogs(), waterDao.getTodayTotal()) { logs, totalMl ->
            val total = totalMl ?: 0
            WaterTodayStatus(
                totalMl = total,
                goalMl = WATER_GOAL_ML,
                achievementRate = if (WATER_GOAL_ML > 0) total.toFloat() / WATER_GOAL_ML else 0f,
                lastDrankAt = logs.firstOrNull()?.timestamp,
                interventionCount = 0,
            )
        }

    override suspend fun addLog(amountMl: Int, source: String, timestamp: Long) {
        waterDao.insert(WaterLogEntity(timestamp = timestamp, amountMl = amountMl, source = source))
        WidgetUpdateHelper.updateAllWidgets(context)
    }

    override suspend fun updateLog(id: Long, amountMl: Int) {
        waterDao.updateAmountById(id, amountMl)
    }

    override suspend fun deleteLog(id: Long) {
        waterDao.deleteById(id)
    }

    override fun getLogsBetween(startDate: String, endDate: String): Flow<List<DailyWaterSummary>> {
        val start = dateToStartTimestamp(startDate)
        val end = dateToEndTimestamp(endDate)
        return waterDao.getLogsBetween(start, end).map { logs ->
            logs.groupBy { dateOfTimestamp(it.timestamp) }
                .map { (date, dayLogs) ->
                    val total = dayLogs.sumOf { it.amountMl }
                    DailyWaterSummary(
                        date = dateToStartTimestamp(date),
                        totalMl = total,
                        interventionCount = 0,
                        achievementRate = if (WATER_GOAL_ML > 0) total.toFloat() / WATER_GOAL_ML else 0f,
                    )
                }
                .sortedByDescending { it.date }
        }
    }

    override fun getPatternAnalysis(): Flow<WaterPatternResult> {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        return combine(
            waterDao.getHourlyDistribution(thirtyDaysAgo, now),
            waterDao.getWeekdayPattern(thirtyDaysAgo, now),
        ) { hourly, weekday ->
            val sortedByCount = hourly.sortedByDescending { it.count }
            WaterPatternResult(
                peakHours = sortedByCount.take(3).map { it.hour },
                lowResponseHours = sortedByCount.takeLast(3).map { it.hour },
                weekdayPattern = weekday.filter { it.weekday in 1..5 }.associate { it.weekday to it.count },
                weekendPattern = weekday.filter { it.weekday == 0 || it.weekday == 6 }.associate { it.weekday to it.count },
            )
        }
    }

    private fun dateToStartTimestamp(date: String): Long =
        LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun dateToEndTimestamp(date: String): Long =
        LocalDate.parse(date).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

    private fun dateOfTimestamp(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    companion object {
        private const val WATER_GOAL_ML = 2000
    }
}
