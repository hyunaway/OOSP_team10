// 경로: com/example/habittracker/data/repository/MealRepositoryImpl.kt
package com.example.habittracker.data.repository

import com.example.habittracker.data.entity.MealLogEntity
import com.example.habittracker.data.local.room.dao.MealDao
import com.example.habittracker.data.model.MealType
import com.example.habittracker.domain.model.DailyMealSummary
import com.example.habittracker.domain.model.MealPatternResult
import com.example.habittracker.domain.model.MealTodayStatus
import com.example.habittracker.domain.repository.MealRepository
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
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
) : MealRepository {

    private fun getCurrentDayStartFlow(): Flow<Long> = flow {
        while (true) {
            emit(getTodayStartMillis())
            delay(30000)
        }
    }

    private fun getTodayStartMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    override fun getTodayLogs(): Flow<List<MealLogEntity>> =
        getCurrentDayStartFlow().flatMapLatest {
            mealDao.observeLogsByMealDate(LocalDate.now().toString())
        }

    override fun getTodayStatus(): Flow<MealTodayStatus> =
        getTodayLogs().map { logs ->
            MealTodayStatus(
                breakfastLogged = logs.any { it.type == MealType.BREAKFAST },
                lunchLogged = logs.any { it.type == MealType.LUNCH },
                dinnerLogged = logs.any { it.type == MealType.DINNER },
                lateNightCount = logs.count { it.isLateNight || it.type == MealType.LATE_NIGHT },
                lateNightLogged = logs.any { it.type == MealType.LATE_NIGHT || it.isLateNight },
                lastMealAt = logs.firstOrNull()?.timestamp,
            )
        }

    override suspend fun getLogsByMealDate(mealDate: String): List<MealLogEntity> =
        mealDao.getLogsByMealDate(mealDate)

    override fun observeLogsByMealDate(mealDate: String): Flow<List<MealLogEntity>> =
        mealDao.observeLogsByMealDate(mealDate)

    override fun observeLogsForMealScreen(todayDate: String, previousDate: String): Flow<List<MealLogEntity>> =
        mealDao.observeLogsForMealScreen(todayDate, previousDate)

    override suspend fun addLog(
        type: MealType,
        timestamp: Long,
        isLateNight: Boolean,
        viaDeliveryApp: Boolean,
        source: String,
        mealDate: String,
        recordedTime: String,
        inputMethod: String,
        triggerType: String,
    ) {
        mealDao.insert(
            MealLogEntity(
                timestamp = timestamp,
                type = type,
                isLateNight = isLateNight,
                viaDeliveryApp = viaDeliveryApp,
                source = source,
                mealDate = mealDate,
                recordedTime = recordedTime,
                inputMethod = inputMethod,
                triggerType = triggerType,
            )
        )
    }

    override suspend fun updateLog(id: Long, type: MealType) {
        mealDao.updateTypeById(id, type.name)
    }

    override suspend fun deleteLog(id: Long) {
        mealDao.deleteById(id)
    }

    override fun getLogsBetween(startDate: String, endDate: String): Flow<List<DailyMealSummary>> {
        val start = dateToStartTimestamp(startDate)
        val end = dateToEndTimestamp(endDate)
        return mealDao.getLogsBetween(start, end).map { logs ->
            logs.groupBy { dateOfTimestamp(it.timestamp) }
                .map { (date, dayLogs) ->
                    DailyMealSummary(
                        date = dateToStartTimestamp(date),
                        mealMap = mapOf(
                            MealType.BREAKFAST.name to dayLogs.any { it.type == MealType.BREAKFAST },
                            MealType.LUNCH.name to dayLogs.any { it.type == MealType.LUNCH },
                            MealType.DINNER.name to dayLogs.any { it.type == MealType.DINNER },
                            MealType.LATE_NIGHT.name to dayLogs.any { it.type == MealType.LATE_NIGHT || it.isLateNight },
                        ),
                        lateNightCount = dayLogs.count { it.isLateNight || it.type == MealType.LATE_NIGHT },
                        deliveryAppCount = dayLogs.count { it.viaDeliveryApp },
                    )
                }
                .sortedByDescending { it.date }
        }
    }

    override fun getPatternAnalysis(): Flow<MealPatternResult> {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        return combine(
            mealDao.getSkippedMealPattern(thirtyDaysAgo, now),
            mealDao.getMealHourByType(MealType.DINNER.name, thirtyDaysAgo, now),
        ) { skipped, dinnerHours ->
            MealPatternResult(
                lateNightRiskHour = dinnerHours.maxByOrNull { it.count }?.hour,
                skippedMealPattern = skipped.associate { it.type to it.count },
                weekdayMealTimeMap = emptyMap(),
                weekendMealTimeMap = emptyMap(),
            )
        }
    }

    private fun dateToStartTimestamp(date: String): Long =
        LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun dateToEndTimestamp(date: String): Long =
        LocalDate.parse(date).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

    private fun dateOfTimestamp(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toString()
}
