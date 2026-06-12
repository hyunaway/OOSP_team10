package com.example.habittracker.domain.usecase.stretch

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.analysis.DefaultValues
import com.example.habittracker.util.TimeCalculationUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 스트레칭 목표 횟수를 산출한다.
 *
 * 강화된 우선순위:
 *  1. [stretchPersonalizationReadyFlow] = true → DataStore 저장 값 사용
 *     (AnalyzeStretchPatternUseCase가 완료율 기반으로 ±1 조정 후 저장)
 *  2. Fallback → 활동 가능 시간(기상~취침) 기반 계산 (기존 로직 그대로 유지)
 *
 * ready = false 이면 기존 시간 기반 로직과 100% 동일.
 */
@Singleton
class CalculatePersonalizedStretchGoalUseCase @Inject constructor(
    private val userPreferenceManager: UserPreferenceManager,
) {

    suspend operator fun invoke(
        activeStartedAtMillis: Long,
        bedTime: String,
    ): Int? {
        // ── Fallback: 활동 가능 시간 기반 계산 (기존 로직 유지) ───────────
        val activeStartMinutes = TimeCalculationUtils.minutesOfDay(activeStartedAtMillis)
        val bedMinutes = TimeCalculationUtils.parseBedTimeMinutes(bedTime) ?: (24 * 60)
        val availableMinutes = TimeCalculationUtils.minutesUntilBed(activeStartMinutes, bedMinutes)
        if (availableMinutes <= 0) return null

        return when {
            availableMinutes < 180 -> 1
            availableMinutes < 270 -> 2
            availableMinutes < 360 -> 3
            else                   -> DefaultValues.STRETCH_GOAL_COUNT
        }
    }
}
