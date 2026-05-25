// 경로: com/example/habittracker/domain/repository/WaterRepository.kt
package com.example.habittracker.domain.repository

import com.example.habittracker.domain.model.DailyWaterSummary
import com.example.habittracker.domain.model.WaterPatternResult
import com.example.habittracker.domain.model.WaterTodayStatus
import kotlinx.coroutines.flow.Flow

interface WaterRepository {
    fun getTodayStatus(): Flow<WaterTodayStatus>
    suspend fun addLog(amountMl: Int, source: String, timestamp: Long)
    suspend fun updateLog(id: Long, amountMl: Int)
    suspend fun deleteLog(id: Long)
    fun getLogsBetween(startDate: String, endDate: String): Flow<List<DailyWaterSummary>>
    fun getPatternAnalysis(): Flow<WaterPatternResult>
}
