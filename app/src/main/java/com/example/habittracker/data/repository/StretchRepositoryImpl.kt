// 경로: com/example/habittracker/data/repository/StretchRepositoryImpl.kt
package com.example.habittracker.data.repository

import android.content.Context
import com.example.habittracker.data.entity.StretchLogEntity
import com.example.habittracker.data.local.room.dao.StretchDao
import com.example.habittracker.data.model.BodyPartType
import com.example.habittracker.domain.model.DailyStretchSummary
import com.example.habittracker.domain.model.StretchPatternResult
import com.example.habittracker.domain.model.StretchTodayStatus
import com.example.habittracker.domain.repository.StretchRepository
import com.example.habittracker.widget.WidgetUpdateHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StretchRepositoryImpl @Inject constructor(
    private val stretchDao: StretchDao,
    @ApplicationContext private val context: Context,
) : StretchRepository {

    override fun getTodayStatus(): Flow<StretchTodayStatus> =
        stretchDao.getTodayLogs().map { logs ->
            val totalSeconds = logs.sumOf { it.durationSeconds }
            StretchTodayStatus(
                totalCount = logs.size,
                lastStretchAt = logs.firstOrNull()?.timestamp,
                bodyPartMap = logs.groupBy { it.bodyPart.name }.mapValues { (_, list) -> list.size },
                totalSeconds = totalSeconds,
                avatarHealthScore = minOf(1.0f, totalSeconds / FULL_SCORE_SECONDS),
            )
        }

    override suspend fun addLog(
        bodyPart: BodyPartType,
        durationSeconds: Int,
        source: String,
        timestamp: Long,
    ) {
        stretchDao.insert(
            StretchLogEntity(
                timestamp = timestamp,
                bodyPart = bodyPart,
                durationSeconds = durationSeconds,
            )
        )
        WidgetUpdateHelper.updateAllWidgets(context)
    }

    override suspend fun updateLog(id: Long, bodyPart: BodyPartType) {
        stretchDao.updateBodyPartById(id, bodyPart.name)
    }

    override suspend fun deleteLog(id: Long) {
        stretchDao.deleteById(id)
    }

    override fun getLogsBetween(
        startDate: String,
        endDate: String,
        bodyPart: BodyPartType?,
    ): Flow<List<DailyStretchSummary>> {
        val start = dateToStartTimestamp(startDate)
        val end = dateToEndTimestamp(endDate)
        val logsFlow = if (bodyPart != null) {
            stretchDao.getLogsBetweenByBodyPart(start, end, bodyPart.name)
        } else {
            stretchDao.getLogsBetween(start, end)
        }
        return logsFlow.map { logs ->
            logs.groupBy { dateOfTimestamp(it.timestamp) }
                .map { (date, dayLogs) ->
                    val bodyPartCounts = dayLogs.groupBy { it.bodyPart.name }.mapValues { (_, list) -> list.size }
                    DailyStretchSummary(
                        date = dateToStartTimestamp(date),
                        count = dayLogs.size,
                        dominantBodyPart = bodyPartCounts.maxByOrNull { it.value }?.key,
                        totalSeconds = dayLogs.sumOf { it.durationSeconds },
                    )
                }
                .sortedByDescending { it.date }
        }
    }

    override fun getPatternAnalysis(): Flow<StretchPatternResult> {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        return combine(
            stretchDao.getHourlyDistribution(thirtyDaysAgo, now),
            stretchDao.getBodyPartDistribution(thirtyDaysAgo, now),
            stretchDao.getLogsBetween(thirtyDaysAgo, now),
        ) { hourly, bodyParts, logs ->
            val weekdayPattern = mutableMapOf<Int, Int>()
            val weekendPattern = mutableMapOf<Int, Int>()
            val cal = Calendar.getInstance()
            logs.forEach { log ->
                cal.timeInMillis = log.timestamp
                // DAY_OF_WEEK: 1=Sun,2=Mon,...,7=Sat → convert to 0=Sun,...,6=Sat
                val dow = cal.get(Calendar.DAY_OF_WEEK) - 1
                if (dow == 0 || dow == 6) {
                    weekendPattern[dow] = (weekendPattern[dow] ?: 0) + 1
                } else {
                    weekdayPattern[dow] = (weekdayPattern[dow] ?: 0) + 1
                }
            }
            StretchPatternResult(
                inactiveHours = (0..23).filter { hour -> hourly.none { it.hour == hour } },
                digitalTriggerConversionRate = 0f,
                preferredBodyPart = bodyParts.maxByOrNull { it.count }?.bodyPart,
                weekdayStretchPattern = weekdayPattern,
                weekendStretchPattern = weekendPattern,
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
        private const val FULL_SCORE_SECONDS = 300f
    }
}
