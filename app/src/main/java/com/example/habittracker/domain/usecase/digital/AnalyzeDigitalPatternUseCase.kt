package com.example.habittracker.domain.usecase.digital

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.local.room.dao.DigitalInterventionLogDao
import com.example.habittracker.data.local.room.dao.DigitalSessionDao
import com.example.habittracker.domain.analysis.AnalysisWindow
import com.example.habittracker.domain.model.AppProfile
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [파이프라인] digital_sessions 30일 → 앱별 평균 세션 × 0.8 임계값,
 *            digital_interventions → messageTone 반응률 → 최선 어조
 *            → 4조건 게이트(기간·누적·최근기록·관리앱 존재)
 *            → 통과 시 perAppProfileJson / preferredMessageTone / digitalReady 갱신.
 *
 * DIGITAL은 "앱별" 필터링이 필요해 공용 GateThresholds 미사용.
 * 4조건 게이트를 이 UseCase에서 직접 조합한다:
 *  1. daysObserved >= 7
 *  2. totalSessions >= 10
 *  3. hasRecentRecord (최근 3일 이내)
 *  4. 관리 중인 앱 프로필 >= 1개
 */
@Singleton
class AnalyzeDigitalPatternUseCase @Inject constructor(
    private val digitalSessionDao: DigitalSessionDao,
    private val interventionLogDao: DigitalInterventionLogDao,
    private val userPreferenceManager: UserPreferenceManager,
) {
    suspend operator fun invoke() {
        val window = AnalysisWindow.recent(AnalysisWindow.DEFAULT_DAYS)

        val sessions      = digitalSessionDao.getSessionsBetween(window.first, window.last).first()
        val interventions = interventionLogDao.getInterventionsBetween(window.first, window.last).first()

        // 앱별 평균 세션 → 임계값(×0.8)
        val appProfiles = sessions
            .groupBy { it.appPackage }
            .map { (pkg, list) ->
                val avg = list.map { it.durationMinutes }.average().toFloat()
                AppProfile(
                    packageName               = pkg,
                    suggestedThresholdMinutes = (avg * 0.8f).toInt().coerceAtLeast(1),
                    avgSessionMinutes         = avg,
                )
            }

        // messageTone 반응률 → 가장 높은 어조 선택 (없으면 "EMPATHY" 기본값)
        val bestTone = interventions
            .groupBy { it.messageTone }
            .mapValues { (_, list) -> list.count { it.reacted }.toFloat() / list.size.toFloat() }
            .maxByOrNull { it.value }
            ?.key
            ?: DEFAULT_TONE

        // 관찰 지표 계산
        val distinctDays    = sessions.map { dayKey(it.startTime) }.distinct().size
        val hasRecentRecord = sessions.any { it.startTime >= recentThreshold() }

        // 4조건 게이트 (DIGITAL 전용 조합)
        val gatePass = distinctDays    >= MIN_DAYS        &&
                       sessions.size   >= MIN_SESSIONS    &&
                       hasRecentRecord                    &&
                       appProfiles.isNotEmpty()

        // 게이트 통과 시에만 DataStore 갱신
        if (gatePass) {
            userPreferenceManager.updatePerAppProfileJson(AppProfile.listToJson(appProfiles))
            userPreferenceManager.updatePreferredMessageTone(bestTone)
            userPreferenceManager.updateDigitalPersonalizationReady(true)
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

    companion object {
        private const val MIN_DAYS     = 7
        private const val MIN_SESSIONS = 10
        private const val DEFAULT_TONE = "EMPATHY"
    }
}
