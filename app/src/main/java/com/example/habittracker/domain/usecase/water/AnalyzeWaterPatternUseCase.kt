package com.example.habittracker.domain.usecase.water

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.local.room.dao.WaterDao
import com.example.habittracker.domain.analysis.AnalysisWindow
import com.example.habittracker.domain.analysis.GateEvaluator
import com.example.habittracker.domain.analysis.GateInput
import com.example.habittracker.domain.analysis.GateThresholds
import com.example.habittracker.domain.analysis.PersonalizationEngine
import com.example.habittracker.domain.analysis.DefaultValues
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [파이프라인] water_logs 30일 -> 전체 timestamps 추출
 *            -> calcPeakWindow -> 게이트(WATER 7·20)
 *            -> 통과 시 waterPeakJson / waterReady 갱신.
 *
 * 게이트 미통과 시 DataStore 기존 값 유지(후퇴 없음).
 */
@Singleton
class AnalyzeWaterPatternUseCase @Inject constructor(
    private val waterDao: WaterDao,
    private val userPreferenceManager: UserPreferenceManager,
    @ApplicationContext private val context: Context,
) {
    suspend operator fun invoke() {
        val window = AnalysisWindow.recent(AnalysisWindow.DEFAULT_DAYS)
        val logs = waterDao.getLogsBetween(window.first, window.last).first()

        // 엔티티 -> 원시 List 추출 (단일 피크 계산을 위해 전체 타임스탬프 사용)
        val allTs = logs.map { it.timestamp }

        // 공용 엔진 계산
        val waterPeak = PersonalizationEngine.calcPeakWindow(allTs)

        // 섭취 기록 간의 평균 간격 계산 (분 단위)
        val sortedLogs = logs.sortedBy { it.timestamp }
        val intervals = mutableListOf<Long>()
        for (i in 0 until sortedLogs.size - 1) {
            val diffMs = sortedLogs[i + 1].timestamp - sortedLogs[i].timestamp
            val diffMins = diffMs / 60000L
            // 수면이나 비정상 공백 제외 (10분 ~ 240분 사이의 간격만 수집)
            if (diffMins in 10..240) {
                intervals.add(diffMins)
            }
        }
        val calculatedInterval = if (intervals.size >= 2) {
            intervals.average().toInt().coerceIn(15, 240)
        } else {
            DefaultValues.WATER_REMINDER_INTERVAL_MINUTES
        }

        // 게이트 입력 구성
        val distinctDays = logs.map { dayKey(it.timestamp) }.distinct().size
        val hasRecentRecord = logs.any { it.timestamp >= recentThreshold() }
        val gateInput = GateInput(
            daysObserved    = distinctDays,
            volume          = logs.size,
            hasRecentRecord = hasRecentRecord,
        )

        // 게이트 통과 시에만 DataStore 갱신
        if (GateEvaluator.evaluateGate(gateInput, GateThresholds.WATER)) {
            userPreferenceManager.updateWaterPeakJson(waterPeak.toJson())
            
            val oldInterval = userPreferenceManager.waterReminderIntervalMinutesFlow.first()
            userPreferenceManager.updateWaterReminderIntervalMinutes(calculatedInterval)
            userPreferenceManager.updateWaterPersonalizationReady(true)

            // 알림 주기가 변경된 경우에만 백그라운드 WorkManager 재스케줄링
            if (oldInterval != calculatedInterval) {
                com.example.habittracker.worker.WorkScheduler.rescheduleAll(context, userPreferenceManager)
            }
        }
    }

    private fun dayKey(timestamp: Long): Int {
        val cal = Calendar.getInstance().also { it.timeInMillis = timestamp }
        return cal.get(Calendar.YEAR) * 10000 +
               cal.get(Calendar.MONTH) * 100 +
               cal.get(Calendar.DAY_OF_MONTH)
    }

    private fun recentThreshold(): Long =
        System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000
}
