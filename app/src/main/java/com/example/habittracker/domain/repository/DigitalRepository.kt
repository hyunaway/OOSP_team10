// 경로: com/example/habittracker/domain/repository/DigitalRepository.kt
package com.example.habittracker.domain.repository

import com.example.habittracker.domain.model.DailyDigitalSummary
import com.example.habittracker.domain.model.DigitalPatternResult
import com.example.habittracker.domain.model.DigitalTodayStatus
import kotlinx.coroutines.flow.Flow

interface DigitalRepository {
    fun getTodayStatus(): Flow<DigitalTodayStatus>
    suspend fun saveSession(appPackage: String, startTime: Long, endTime: Long, durationMinutes: Int)
    suspend fun logIntervention(
        appPackage: String,
        triggerDuration: Int,
        messageTone: String,
        timestamp: Long,
        actionType: String = "",
    ): Long
    suspend fun getLatestInterventionTimestamp(appPackage: String): Long?
    suspend fun updateInterventionReaction(id: Long, reacted: Boolean, actionType: String)
    fun getLogsBetween(startDate: String, endDate: String, appPackage: String?): Flow<List<DailyDigitalSummary>>
    fun getPatternAnalysis(): Flow<DigitalPatternResult>
}
