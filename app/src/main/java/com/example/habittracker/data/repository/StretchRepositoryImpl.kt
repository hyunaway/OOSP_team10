// 경로: com/example/habittracker/data/repository/StretchRepositoryImpl.kt
package com.example.habittracker.data.repository

import android.content.Context
import com.example.habittracker.data.entity.StretchingRecord
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
    @param:ApplicationContext private val context: Context,
) : StretchRepository {

    override fun getTodayStatus(): Flow<StretchTodayStatus> {
        val todayStr = LocalDate.now().toString()
        return stretchDao.getTodayRecords(todayStr).map { records ->
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
            
            val bodyPartsList = records.flatMap { parseJsonBodyParts(it.bodyParts) }
            val bodyPartMap = bodyPartsList.groupBy { it }.mapValues { it.value.size }
            val totalSeconds = totalCount * 300 // 300 seconds (5 minutes) per stretch
            
            StretchTodayStatus(
                totalCount = totalCount,
                lastStretchAt = lastTimestamp,
                bodyPartMap = bodyPartMap,
                totalSeconds = totalSeconds,
                avatarHealthScore = minOf(1.0f, totalCount / 4f), // 4 is DAILY_STRETCH_GOAL
                slotsLogged = records.map { it.timeSlot }.filter { it.isNotEmpty() }.distinct(),
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
        bodyPart: BodyPartType?,
    ): Flow<List<DailyStretchSummary>> {
        return stretchDao.getRecordsBetween(startDate, endDate).map { records ->
            val filteredRecords = if (bodyPart != null) {
                val targetKorean = bodyPartToKorean(bodyPart)
                records.filter { parseJsonBodyParts(it.bodyParts).contains(targetKorean) }
            } else {
                records
            }
            
            filteredRecords.groupBy { it.date }
                .map { (date, dayRecords) ->
                    val bodyPartsList = dayRecords.flatMap { parseJsonBodyParts(it.bodyParts) }
                    val bodyPartCounts = bodyPartsList.groupBy { it }.mapValues { it.value.size }
                    val dominantKorean = bodyPartCounts.maxByOrNull { it.value }?.key
                    val dominantBodyPartStr = dominantKorean?.let { koreanToBodyPart(it).name }
                    
                    DailyStretchSummary(
                        date = try {
                            LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        } catch (_: Exception) {
                            0L
                        },
                        count = dayRecords.size,
                        dominantBodyPart = dominantBodyPartStr,
                        totalSeconds = dayRecords.size * 300,
                    )
                }
                .sortedByDescending { it.date }
        }
    }

    override fun getPatternAnalysis(): Flow<StretchPatternResult> {
        val thirtyDaysAgoStr = LocalDate.now().minusDays(30).toString()
        val todayStr = LocalDate.now().toString()
        
        return stretchDao.getRecordsBetween(thirtyDaysAgoStr, todayStr).map { records ->
            val hourlyDistribution = mutableMapOf<Int, Int>()
            val bodyPartDistribution = mutableMapOf<String, Int>()
            val weekdayPattern = mutableMapOf<Int, Int>()
            val weekendPattern = mutableMapOf<Int, Int>()
            
            val cal = Calendar.getInstance()
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            
            records.forEach { record ->
                try {
                    val localDateTime = java.time.LocalDateTime.parse(record.createdAt, formatter)
                    val hour = localDateTime.hour
                    hourlyDistribution[hour] = (hourlyDistribution[hour] ?: 0) + 1
                    
                    val timestamp = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    cal.timeInMillis = timestamp
                    val dow = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun, ..., 6=Sat
                    if (dow == 0 || dow == 6) {
                        weekendPattern[dow] = (weekendPattern[dow] ?: 0) + 1
                    } else {
                        weekdayPattern[dow] = (weekdayPattern[dow] ?: 0) + 1
                    }
                } catch (_: Exception) {}
                
                parseJsonBodyParts(record.bodyParts).forEach { part ->
                    bodyPartDistribution[part] = (bodyPartDistribution[part] ?: 0) + 1
                }
            }
            
            val preferredKorean = bodyPartDistribution.maxByOrNull { it.value }?.key
            val preferredBodyPartStr = preferredKorean?.let { koreanToBodyPart(it).name }
            
            StretchPatternResult(
                inactiveHours = (0..23).filter { hour -> !hourlyDistribution.containsKey(hour) },
                digitalTriggerConversionRate = 0f,
                preferredBodyPart = preferredBodyPartStr,
                weekdayStretchPattern = weekdayPattern,
                weekendStretchPattern = weekendPattern,
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

    override suspend fun insertStretchRecord(date: String, timeSlot: String, bodyParts: String) {
        stretchDao.insertStretchRecord(date, timeSlot, bodyParts)
    }

    override suspend fun updateStretchRecord(id: Int, bodyParts: String) {
        stretchDao.updateStretchRecord(id, bodyParts)
    }

    override suspend fun deleteStretchRecord(id: Int) {
        stretchDao.deleteStretchRecord(id)
    }

    override suspend fun isGoalAchieved(date: String): Boolean {
        return stretchDao.isGoalAchieved(date)
    }

    override suspend fun calculateStreak(today: String): Int {
        return stretchDao.calculateStreak(today)
    }

    // Helper functions
    private fun bodyPartToKorean(bodyPart: BodyPartType): String =
        when (bodyPart) {
            BodyPartType.NECK -> "목"
            BodyPartType.SHOULDER -> "어깨"
            BodyPartType.BACK -> "허리"
            BodyPartType.FULL -> "전신"
        }

    private fun koreanToBodyPart(korean: String): BodyPartType =
        when (korean) {
            "목" -> BodyPartType.NECK
            "어깨" -> BodyPartType.SHOULDER
            "허리" -> BodyPartType.BACK
            else -> BodyPartType.FULL
        }

    private fun parseJsonBodyParts(json: String): List<String> {
        return json.replace("[", "")
            .replace("]", "")
            .replace("\"", "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun toJsonBodyParts(parts: List<String>): String {
        return parts.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
    }
}
