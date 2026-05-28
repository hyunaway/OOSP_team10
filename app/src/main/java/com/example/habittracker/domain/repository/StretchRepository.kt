// 경로: com/example/habittracker/domain/repository/StretchRepository.kt
package com.example.habittracker.domain.repository

import com.example.habittracker.data.model.BodyPartType
import com.example.habittracker.domain.model.DailyStretchSummary
import com.example.habittracker.domain.model.StretchPatternResult
import com.example.habittracker.domain.model.StretchTodayStatus
import kotlinx.coroutines.flow.Flow

interface StretchRepository {
    fun getTodayStatus(): Flow<StretchTodayStatus>
    suspend fun addLog(bodyPart: BodyPartType, durationSeconds: Int, source: String, timestamp: Long)
    suspend fun updateLog(id: Long, bodyPart: BodyPartType)
    suspend fun deleteLog(id: Long)
    fun getLogsBetween(startDate: String, endDate: String, bodyPart: BodyPartType?): Flow<List<DailyStretchSummary>>
    fun getPatternAnalysis(): Flow<StretchPatternResult>
}
