// 경로: com/example/habittracker/domain/repository/MealRepository.kt
package com.example.habittracker.domain.repository

import com.example.habittracker.data.entity.MealLogEntity
import com.example.habittracker.data.model.MealType
import com.example.habittracker.domain.model.DailyMealSummary
import com.example.habittracker.domain.model.MealPatternResult
import com.example.habittracker.domain.model.MealTodayStatus
import kotlinx.coroutines.flow.Flow

interface MealRepository {
    fun getTodayLogs(): Flow<List<MealLogEntity>>
    fun getTodayStatus(): Flow<MealTodayStatus>
    suspend fun addLog(type: MealType, timestamp: Long, isLateNight: Boolean, viaDeliveryApp: Boolean, source: String, mealDate: String, recordedTime: String, inputMethod: String = "", triggerType: String = "")
    suspend fun updateLog(id: Long, type: MealType)
    suspend fun deleteLog(id: Long)
    fun getLogsBetween(startDate: String, endDate: String): Flow<List<DailyMealSummary>>
    fun getPatternAnalysis(): Flow<MealPatternResult>
}
