// 경로: com/example/habittracker/data/repository/WaterRepositoryImpl.kt
package com.example.habittracker.data.repository

import android.content.Context
import com.example.habittracker.data.entity.WaterLogEntity
import com.example.habittracker.data.local.room.dao.WaterDao
import com.example.habittracker.domain.model.DailyWaterSummary
import com.example.habittracker.domain.model.WaterPatternResult
import com.example.habittracker.domain.model.WaterTodayStatus
import com.example.habittracker.domain.repository.WaterRepository
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.widget.WidgetUpdateHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
class WaterRepositoryImpl @Inject constructor(
    private val waterDao: WaterDao,
    private val userPreferenceManager: UserPreferenceManager,
    @ApplicationContext private val context: Context,
) : WaterRepository {

    // 날짜 전환 관련 유틸리티 Flow

    /**
     오늘 자정(00:00:00) 타임스탬프를 30초마다 갱신하여 발행하는 Flow.
     사용자가 앱을 켠 상태로 자정을 넘겼을 때 오늘 날짜 기준점을 자동으로 업데이트하기 위해 사용됩니다.
     */
    private fun getCurrentDayStartFlow(): Flow<Long> = flow {
        while (true) {
            emit(getTodayStartMillis())
            delay(30000) // 30초 딜레이
        }
    }

    //오늘 자정(00:00:00.000)의 밀리초 타임스탬프를 가져옵니다.
    private fun getTodayStartMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // --- 수분 섭취 상태 및 로그 관련 주요 메서드 ---

    /**
     실시간 오늘 수분 섭취 상태 및 목표량 달성도를 관찰하는 Flow를 반환합니다.
     체중과 BMI를 기준으로 동적 수분 섭취 목표량을 계산하며, 실시간 섭취 총량 및 달성률을 제공합니다.
     */
    override fun getTodayStatus(): Flow<WaterTodayStatus> =
        getCurrentDayStartFlow().flatMapLatest { start ->
            combine(
                waterDao.getLogsBetween(start, Long.MAX_VALUE), // 오늘 자정 이후 기록된 물 로그 목록
                waterDao.getTotalBetween(start, Long.MAX_VALUE), // 오늘 마신 물의 총량 (ml)
                userPreferenceManager.waterPersonalizationReadyFlow, // 개인화 준비 상태 추가
                userPreferenceManager.userWeightKgFlow,         // 개인화 계산용 유저 체중
                userPreferenceManager.userBmiFlow              // 개인화 계산용 유저 BMI
            ) { logs, totalMl, ready, weight, bmi ->
                val total = totalMl ?: 0
                // 개인화가 준비되었고 체중/BMI가 유효한 경우에만 개인화 목표량 계산, 그렇지 않으면 기본 2000ml 적용
                val goal = if (ready && weight > 0f && bmi > 0f) {
                    com.example.habittracker.domain.analysis.PersonalizationEngine.calcWaterTarget(weight, bmi)
                } else {
                    WATER_GOAL_ML
                }
                WaterTodayStatus(
                    totalMl = total,
                    goalMl = goal,
                    achievementRate = if (goal > 0) total.toFloat() / goal else 0f, // 달성률 계산
                    lastDrankAt = logs.firstOrNull()?.timestamp, // 마지막으로 물을 마신 시간
                    interventionCount = 0,
                )
            }
        }

    /**
     * 새로운 수분 섭취 기록을 추가하고, 홈 화면의 홈 위젯(Widget) 데이터를 갱신합니다.
     */
    override suspend fun addLog(amountMl: Int, source: String, timestamp: Long) {
        waterDao.insert(WaterLogEntity(timestamp = timestamp, amountMl = amountMl, source = source))
    }

    /**
     * 특정 수분 섭취 기록의 마신 양(ml)을 수정합니다.
     */
    override suspend fun updateLog(id: Long, amountMl: Int) {
        waterDao.updateAmountById(id, amountMl)
    }

    /**
     * 특정 수분 섭취 기록을 삭제합니다.
     */
    override suspend fun deleteLog(id: Long) {
        waterDao.deleteById(id)
    }

    /**
     * 지정된 시작 날짜(startDate)와 종료 날짜(endDate) 사이의 일별 수분 섭취 통계 데이터를 반환합니다.
     * 날짜별로 그룹화하여 일별 총 섭취량과 목표 달성률을 내림차순으로 정렬하여 제공합니다.
     */
    override fun getLogsBetween(startDate: String, endDate: String): Flow<List<DailyWaterSummary>> {
        val start = dateToStartTimestamp(startDate)
        val end = dateToEndTimestamp(endDate)
        return combine(
            waterDao.getLogsBetween(start, end),
            userPreferenceManager.waterPersonalizationReadyFlow, // 개인화 준비 상태 추가
            userPreferenceManager.userWeightKgFlow,
            userPreferenceManager.userBmiFlow
        ) { logs, ready, weight, bmi ->
            val goal = if (ready && weight > 0f && bmi > 0f) {
                com.example.habittracker.domain.analysis.PersonalizationEngine.calcWaterTarget(weight, bmi)
            } else {
                WATER_GOAL_ML
            }
            logs.groupBy { dateOfTimestamp(it.timestamp) } // 날짜별로 그룹화 ("yyyy-MM-dd")
                .map { (date, dayLogs) ->
                    val total = dayLogs.sumOf { it.amountMl }
                    DailyWaterSummary(
                        date = dateToStartTimestamp(date),
                        totalMl = total,
                        interventionCount = 0,
                        achievementRate = if (goal > 0) total.toFloat() / goal else 0f,
                    )
                }
                .sortedByDescending { it.date } // 최신 날짜순 정렬
        }
    }

    /**
     * 완벽히 지나간 최근 30일(오늘 제외) 동안의 수분 섭취 데이터를 토대로 시간대별 섭취 패턴을 분석합니다.
     * 물을 가장 많이 마신 시간대(피크타임) 3개와 가장 적게 마신 시간대 3개를 추출하여 반환합니다.
     */
    override fun getPatternAnalysis(): Flow<WaterPatternResult> {
        val zoneId = ZoneId.systemDefault()
        val todayStart = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val thirtyDaysAgoStart = LocalDate.now().minusDays(30).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val yesterdayEnd = todayStart - 1
        return waterDao.getHourlyDistribution(thirtyDaysAgoStart, yesterdayEnd).map { hourly ->
            val sortedByCount = hourly.sortedByDescending { it.count }
            WaterPatternResult(
                peakHours = sortedByCount.take(3).map { it.hour }, // 가장 빈번한 3개 시간대
                lowResponseHours = sortedByCount.takeLast(3).map { it.hour }, // 가장 뜸했던 3개 시간대
            )
        }
    }

    // --- 시간 및 날짜 포맷 변환 헬퍼 메서드 ---

    /**
     * 문자열 날짜("yyyy-MM-dd")를 해당 날짜 시작 시각(00:00:00)의 밀리초 타임스탬프로 변환합니다.
     */
    private fun dateToStartTimestamp(date: String): Long =
        LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /**
     * 문자열 날짜("yyyy-MM-dd")를 해당 날짜 끝 시각(23:59:59.999)의 밀리초 타임스탬프로 변환합니다.
     */
    private fun dateToEndTimestamp(date: String): Long =
        LocalDate.parse(date).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

    /**
     * 밀리초 타임스탬프를 문자열 날짜("yyyy-MM-dd") 형태로 변환합니다.
     */
    private fun dateOfTimestamp(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    companion object {
        private const val WATER_GOAL_ML = 2000 // 기본 일일 목표 수분량 (ml)
    }
}
