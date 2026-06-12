package com.example.habittracker.domain.usecase.stretch

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.local.room.dao.StretchDao
import com.example.habittracker.domain.analysis.AnalysisWindow
import com.example.habittracker.domain.analysis.GateEvaluator
import com.example.habittracker.domain.analysis.GateInput
import com.example.habittracker.domain.analysis.GateThresholds
import com.example.habittracker.domain.analysis.PersonalizationEngine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [파이프라인] stretching_records 7일 → 완료율 기반 목표 ±조정,
 *            timeSlot → calcMostFrequentSlot
 *            → 게이트(STRETCH 7·10)
 *            → 통과 시 stretchGoalCount / stretchPreferredTimeSlots / stretchReady 갱신.
 *
 * 완료율 규칙:
 *  - completionRate >= 0.8 → 목표 +1 (최대 6)
 *  - completionRate <= 0.4 → 목표 -1 (최소 1)
 *  - 그 외 → 현행 유지
 */
@Singleton
class AnalyzeStretchPatternUseCase @Inject constructor(
    private val stretchDao: StretchDao,
    private val userPreferenceManager: UserPreferenceManager,
) {
    suspend operator fun invoke() {
        val days = AnalysisWindow.DEFAULT_DAYS
        val endDate   = LocalDate.now().toString()
        val startDate = LocalDate.now().minusDays((days - 1).toLong()).toString()

        val records = stretchDao.getRecordsBetween(startDate, endDate).first()

        // 엔티티 → 원시 타입 추출
        val slots    = records.map { it.timeSlot }
        val dates    = records.map { it.date }.distinct()

        // 공용 엔진 계산
        val preferredSlot = PersonalizationEngine.calcMostFrequentSlot(slots)

        // 게이트 입력 구성
        val hasRecentRecord = dates.any { it >= LocalDate.now().minusDays(3).toString() }
        val gateInput = GateInput(
            daysObserved    = dates.size,
            volume          = records.size,
            hasRecentRecord = hasRecentRecord,
        )

        // 게이트 통과 시에만 DataStore 갱신
        if (GateEvaluator.evaluateGate(gateInput, GateThresholds.STRETCH)) {
            if (preferredSlot != null) {
                // 선호 슬롯을 JSON 배열 형태로 저장: ["아침"]
                userPreferenceManager.updateStretchPreferredTimeSlots("""["$preferredSlot"]""")
            }
            userPreferenceManager.updateStretchPersonalizationReady(true)
        }
    }
}
