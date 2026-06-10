// 경로: com/example/habittracker/data/repository/StretchRepositoryImpl.kt
package com.example.habittracker.data.repository

import android.content.Context
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.entity.StretchingRecord
import com.example.habittracker.data.local.room.dao.StretchDao
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StretchRepositoryImpl @Inject constructor(
    private val stretchDao: StretchDao,
    private val userPreferenceManager: UserPreferenceManager,
    @param:ApplicationContext private val context: Context,
) : StretchRepository {

    override fun getTodayStatus(): Flow<StretchTodayStatus> {
        val todayStr = LocalDate.now().toString()
        val recordsFlow = stretchDao.getTodayRecords(todayStr)
        val goalFlow = userPreferenceManager.stretchGoalCountFlow

        return combine(recordsFlow, goalFlow) { records, goal ->
            val totalCount = records.size
            val lastRecord = records.maxByOrNull { it.createdAt }
            val lastTimestamp = lastRecord?.let {
                try {
                    java.time.LocalDateTime.parse(it.createdAt, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } catch (_: Exception) {
                    null
                }
            }
            
            val totalSeconds = totalCount * 300 // 300 seconds (5 minutes) per stretch
            val safeGoal = if (goal in 1..6) goal else 4
            val score = minOf(1.0f, totalCount.toFloat() / safeGoal.toFloat())
            val streakVal = try {
                stretchDao.calculateStreak(todayStr, safeGoal)
            } catch (_: Exception) {
                0
            }
            
            StretchTodayStatus(
                totalCount = totalCount,
                lastStretchAt = lastTimestamp,
                totalSeconds = totalSeconds,
                avatarHealthScore = score,
                slotsLogged = records.map { it.timeSlot }.filter { it.isNotEmpty() }.distinct(),
                streak = streakVal,
            )
        }
    }

    override suspend fun deleteLogBySlot(mealDate: String, stretchSlot: String): Int {
        val timeSlot = when (stretchSlot) {
            "AM" -> "아침"
            "PM" -> "점심"
            "EVE" -> "저녁"
            "NIGHT" -> "기타"
            else -> if (stretchSlot.isNotEmpty()) stretchSlot else "기타"
        }
        val deletedRows = stretchDao.deleteBySlot(mealDate, timeSlot)
        WidgetUpdateHelper.updateAllWidgets(context)
        return deletedRows
    }

    override fun getLogsBetween(
        startDate: String,
        endDate: String,
    ): Flow<List<DailyStretchSummary>> {
        return stretchDao.getRecordsBetween(startDate, endDate).map { records ->
            records.groupBy { it.date }
                .map { (date, dayRecords) ->
                    DailyStretchSummary(
                        date = try {
                            LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        } catch (_: Exception) {
                            0L
                        },
                        count = dayRecords.size,
                        totalSeconds = dayRecords.size * 300,
                    )
                }
                .sortedByDescending { it.date }
        }
    }

    override fun getPatternAnalysis(): Flow<StretchPatternResult> {
        val thirtyDaysAgoStr = LocalDate.now().minusDays(30).toString()
        val yesterdayStr = LocalDate.now().minusDays(1).toString()
        
        return stretchDao.getRecordsBetween(thirtyDaysAgoStr, yesterdayStr).map { records ->
            val hourlyDistribution = mutableMapOf<Int, Int>()
            
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            
            records.forEach { record ->
                try {
                    val localDateTime = java.time.LocalDateTime.parse(record.createdAt, formatter)
                    val hour = localDateTime.hour
                    hourlyDistribution[hour] = (hourlyDistribution[hour] ?: 0) + 1
                } catch (_: Exception) {}
            }
            
            StretchPatternResult(
                inactiveHours = (0..23).filter { hour -> !hourlyDistribution.containsKey(hour) },
                digitalTriggerConversionRate = 0f,
            )
        }
    }

    // 7대 DB 함수 구현
    override suspend fun getTodayStretchCount(date: String): Int {
        return stretchDao.getTodayStretchCount(date)
    }

    override suspend fun getRecordByTimeSlot(date: String, timeSlot: String): StretchingRecord? {
        return stretchDao.getRecordByTimeSlot(date, timeSlot)
    }

    override suspend fun insertStretchRecord(date: String, timeSlot: String) {
        stretchDao.insertStretchRecord(date, timeSlot)
    }

    override suspend fun deleteStretchRecord(id: Int) {
        stretchDao.deleteStretchRecord(id)
    }

    override suspend fun isGoalAchieved(date: String, goal: Int): Boolean {
        return stretchDao.isGoalAchieved(date, goal)
    }

    override suspend fun calculateStreak(today: String, goal: Int): Int {
        return stretchDao.calculateStreak(today, goal)
    }

    // Helper functions
}
