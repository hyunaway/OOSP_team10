// 경로: com/example/habittracker/domain/repository/StretchRepository.kt
package com.example.habittracker.domain.repository

import com.example.habittracker.data.entity.StretchingRecord
import com.example.habittracker.data.model.BodyPartType
import com.example.habittracker.domain.model.DailyStretchSummary
import com.example.habittracker.domain.model.StretchPatternResult
import com.example.habittracker.domain.model.StretchTodayStatus
import kotlinx.coroutines.flow.Flow

interface StretchRepository {
    fun getTodayStatus(): Flow<StretchTodayStatus>
    suspend fun deleteLogBySlot(mealDate: String, stretchSlot: String): Int
    fun getLogsBetween(startDate: String, endDate: String, bodyPart: BodyPartType?): Flow<List<DailyStretchSummary>>
    fun getPatternAnalysis(): Flow<StretchPatternResult>

    // 신규 7대 DB 함수 인터페이스 추가
    suspend fun getTodayStretchCount(date: String): Int
    suspend fun getRecordByTimeSlot(date: String, timeSlot: String): StretchingRecord?
    suspend fun insertStretchRecord(date: String, timeSlot: String, bodyParts: String)
    suspend fun updateStretchRecord(id: Int, bodyParts: String)
    suspend fun deleteStretchRecord(id: Int)
    suspend fun isGoalAchieved(date: String): Boolean
    suspend fun calculateStreak(today: String): Int
}
