// 경로: com/example/habittracker/data/repository/DigitalRepositoryImpl.kt
package com.example.habittracker.data.repository

import com.example.habittracker.data.entity.DigitalInterventionLogEntity
import com.example.habittracker.data.entity.DigitalSessionEntity
import com.example.habittracker.data.local.room.dao.DigitalInterventionLogDao
import com.example.habittracker.data.local.room.dao.DigitalSessionDao
import com.example.habittracker.domain.model.DailyDigitalSummary
import com.example.habittracker.domain.model.DigitalPatternResult
import com.example.habittracker.domain.model.DigitalTodayStatus
import com.example.habittracker.domain.repository.DigitalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DigitalRepositoryImpl @Inject constructor(
    private val digitalSessionDao: DigitalSessionDao,
    private val digitalInterventionLogDao: DigitalInterventionLogDao,
) : DigitalRepository {

    override fun getTodayStatus(): Flow<DigitalTodayStatus> =
        combine(
            digitalSessionDao.getTodaySessionsByApp(),
            digitalInterventionLogDao.getTodayInterventions(),
        ) { sessions, interventions ->
            DigitalTodayStatus(
                totalUsageMinutes = sessions.sumOf { it.total },
                appUsageMap = sessions.associate { it.appPackage to it.total },
                interventionCount = interventions.size,
                reactedCount = interventions.count { it.reacted },
                topApp = sessions.maxByOrNull { it.total }?.appPackage,
            )
        }

    override suspend fun saveSession(
        appPackage: String,
        startTime: Long,
        endTime: Long,
        durationMinutes: Int,
    ) {
        digitalSessionDao.insert(
            DigitalSessionEntity(
                appPackage = appPackage,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes,
            )
        )
    }

    override suspend fun logIntervention(
        appPackage: String,
        triggerDuration: Int,
        messageTone: String,
        timestamp: Long,
        actionType: String,
    ): Long = digitalInterventionLogDao.insert(
        DigitalInterventionLogEntity(
            appPackage = appPackage,
            triggerDurationMinutes = triggerDuration,
            timestamp = timestamp,
            messageTone = messageTone,
            actionType = actionType,
        )
    )

    override suspend fun getLatestInterventionTimestamp(appPackage: String): Long? =
        digitalInterventionLogDao.getLatestInterventionForApp(appPackage)?.timestamp

    override suspend fun updateInterventionReaction(id: Long, reacted: Boolean, actionType: String) {
        digitalInterventionLogDao.updateReaction(id, reacted, actionType)
    }

    override fun getLogsBetween(
        startDate: String,
        endDate: String,
        appPackage: String?,
    ): Flow<List<DailyDigitalSummary>> {
        val start = dateToStartTimestamp(startDate)
        val end = dateToEndTimestamp(endDate)
        val sessionsFlow = if (appPackage != null) {
            digitalSessionDao.getSessionsBetweenByApp(start, end, appPackage)
        } else {
            digitalSessionDao.getSessionsBetween(start, end)
        }
        return sessionsFlow.map { sessions ->
            sessions.groupBy { dateOfTimestamp(it.startTime) }
                .map { (date, daySessions) ->
                    DailyDigitalSummary(
                        date = dateToStartTimestamp(date),
                        totalMinutes = daySessions.sumOf { it.durationMinutes },
                        appBreakdown = daySessions.groupBy { it.appPackage }
                            .mapValues { (_, list) -> list.sumOf { it.durationMinutes } },
                        interventionCount = 0,
                        reactedCount = 0,
                    )
                }
                .sortedByDescending { it.date }
        }
    }

    override fun getPatternAnalysis(): Flow<DigitalPatternResult> {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        return combine(
            digitalSessionDao.getAvgSessionByApp(thirtyDaysAgo, now),
            digitalInterventionLogDao.getToneReactionRate(thirtyDaysAgo, now),
        ) { avgSessions, toneRates ->
            DigitalPatternResult(
                avgSessionByApp = avgSessions.associate { it.appPackage to it.avgDuration },
                bedtimeUsageScore = 0f,
                toneReactionRate = toneRates.associate { it.messageTone to it.rate },
                peakUsageHours = emptyList(),
                lowReactionHours = emptyList(),
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
