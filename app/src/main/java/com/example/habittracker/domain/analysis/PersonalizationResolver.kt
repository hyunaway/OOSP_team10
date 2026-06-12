package com.example.habittracker.domain.analysis

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.model.AppProfile
import com.example.habittracker.domain.model.PeakWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 개인화 값을 안전하게 제공하는 단일 창구.
 *
 * 모든 메서드는 "어떤 경우에도 유효한 값"을 반환하며, 내부에서 다음 4가지를 흡수한다:
 *  ① 초기 상태 / 데이터 0건 (peak 필드 없음)
 *  ② 게이트 미통과 (ready = false)
 *  ③ peak JSON 파싱 실패 (try-catch, 절대 크래시 금지)
 *  ④ concentration < MIN_CONCENTRATION (불규칙 패턴, 신뢰 불가)
 *
 * Fallback 우선순위:
 *  ① 개인화 분석 데이터 (peak.center)
 *  ② 온보딩 기반 기본 설정 (기상 및 취침 시각 기준 동적 계산)
 *  ③ 순수 시스템 상수 기본값 (DefaultValues)
 *
 * ready = false 이면 반환값 = DefaultValues 상수 또는 온보딩 기반 시간 계산값을
 * 사용하여 기존 앱 동작과의 100% 일관성을 보장합니다.
 * 모든 Flow I/O 작업은 try-catch로 감싸져 디스크 오류나 빈 데이터 파싱 시에도 절대 앱 크래시가 나지 않고 안전한 기본값을 반환합니다.
 */
@Singleton
class PersonalizationResolver @Inject constructor(
    private val prefs: UserPreferenceManager,
) {

    // ─────────────────────────────────────────────────────────────────────────
    // 식사 peak 시각 (분, 0..1439)
    // ─────────────────────────────────────────────────────────────────────────

    /** 아침 1차 알림 기준점 (분). Fallback: wakeTime+60 → 07:30 */
    suspend fun resolveBreakfastPeakMinutes(): Int = safeMealPeak(
        readyFlow  = prefs.mealPersonalizationReadyFlow,
        peakFlow   = prefs.mealBreakfastPeakFlow,
        onboarding = { wakeMinutes() + DefaultValues.BREAKFAST_OFFSET_FROM_WAKE_MINUTES },
        constant   = DefaultValues.BREAKFAST_PEAK_MINUTES,
    )

    /** 점심 1차 알림 기준점 (분). Fallback: 12:00 */
    suspend fun resolveLunchPeakMinutes(): Int = safeMealPeak(
        readyFlow  = prefs.mealPersonalizationReadyFlow,
        peakFlow   = prefs.mealLunchPeakFlow,
        onboarding = { DefaultValues.LUNCH_PEAK_MINUTES },
        constant   = DefaultValues.LUNCH_PEAK_MINUTES,
    )

    /** 저녁 1차 알림 기준점 (분). Fallback: bedTime-180 → 18:00 */
    suspend fun resolveDinnerPeakMinutes(): Int = safeMealPeak(
        readyFlow  = prefs.mealPersonalizationReadyFlow,
        peakFlow   = prefs.mealDinnerPeakFlow,
        onboarding = {
            (bedMinutes() - DefaultValues.DINNER_OFFSET_BEFORE_BED_MINUTES)
                .coerceAtLeast(DefaultValues.DINNER_PEAK_MINUTES)
        },
        constant   = DefaultValues.DINNER_PEAK_MINUTES,
    )

    /** 야식 알림 기준점 (분). Fallback: 22:00 */
    suspend fun resolveLateNightPeakMinutes(): Int = safeMealPeak(
        readyFlow  = prefs.mealPersonalizationReadyFlow,
        peakFlow   = prefs.mealLateNightPeakFlow,
        onboarding = { DefaultValues.LATE_NIGHT_PEAK_MINUTES },
        constant   = DefaultValues.LATE_NIGHT_PEAK_MINUTES,
    )

    /**
     * 식사 알림 창 폭 (분). peak.range로 산출 — σ 작으면 좁게, 크면 넓게.
     * Fallback: [MEAL_NOTIFICATION_WINDOW_MINUTES].
     */
    suspend fun resolveMealWindowMinutes(peakFlow: Flow<PeakWindow?>): Int {
        return try {
            val peak = peakFlow.first()
            if (peak == null || peak.isEmpty() || peak.concentration < DefaultValues.MIN_CONCENTRATION) {
                DefaultValues.MEAL_NOTIFICATION_WINDOW_MINUTES
            } else {
                (peak.rangeEnd - peak.rangeStart)
                    .coerceIn(DefaultValues.MEAL_NOTIFICATION_WINDOW_MINUTES, 60)
            }
        } catch (_: Exception) {
            DefaultValues.MEAL_NOTIFICATION_WINDOW_MINUTES
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 스트레칭 목표 횟수
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 스트레칭 목표 횟수. (스트레칭 하루 목표치 개인화 비활성화로 항상 기본값 4를 반환)
     */
    suspend fun resolveStretchGoalCount(): Int {
        return DefaultValues.STRETCH_GOAL_COUNT
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 디지털 앱별 개입 임계값
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 특정 앱의 개인화 개입 임계값(분).
     * ready = true 이면 perAppProfileJson에서 앱별 값 추출.
     * Fallback: [DefaultValues.DIGITAL_THRESHOLD_MINUTES] = 30.
     */
    suspend fun resolveDigitalThresholdMinutes(packageName: String): Int {
        val ready = safeFirst(prefs.digitalPersonalizationReadyFlow, false)
        if (!ready) return DefaultValues.DIGITAL_THRESHOLD_MINUTES
        return try {
            val json = prefs.perAppProfileJsonFlow.first()
            AppProfile.listFromJson(json)
                .find { it.packageName == packageName }
                ?.suggestedThresholdMinutes
                ?.coerceAtLeast(1)
                ?: DefaultValues.DIGITAL_THRESHOLD_MINUTES
        } catch (_: Exception) {
            DefaultValues.DIGITAL_THRESHOLD_MINUTES
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 물 (Water)
    // ─────────────────────────────────────────────────────────────────────────

    /** BMI 및 몸무게 기반 일일 권장 물 섭취량 (ml). Fallback: 2000ml */
    suspend fun resolveWaterGoalMl(): Int {
        val weight = try { prefs.userWeightKgFlow.first() } catch (_: Exception) { 0f }
        val bmi = try { prefs.userBmiFlow.first() } catch (_: Exception) { 0f }
        if (weight <= 0f || bmi <= 0f) return 2000
        return PersonalizationEngine.calcWaterTarget(weight, bmi)
    }

    /** 물 피크 윈도우. Fallback: null */
    suspend fun resolveWaterPeakWindow(): PeakWindow? {
        val ready = try { prefs.waterPersonalizationReadyFlow.first() } catch (_: Exception) { false }
        if (!ready) return null
        return try {
            val json = prefs.waterPeakJsonFlow.first()
            if (json.isBlank()) return null
            val peak = PeakWindow.fromJson(json)
            if (peak == null || peak.isEmpty()) {
                null
            } else {
                peak
            }
        } catch (_: Exception) {
            null
        }
    }

    /** 스트레칭 선호 슬롯 (예: "아침", "점심", "저녁"). Fallback: null */
    suspend fun resolveStretchPreferredSlot(): String? {
        val ready = safeFirst(prefs.stretchPersonalizationReadyFlow, false)
        if (!ready) return null
        return try {
            val json = prefs.stretchPreferredTimeSlotsFlow.first()
            if (json.isBlank()) return null
            val slots = json.replace("[", "").replace("]", "").replace("\"", "").split(",")
            slots.firstOrNull()?.trim()?.ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 식사 peak 4단계 fallback 공통 구현.
     *
     * ① ready=false  → safeOnboarding → constant
     * ② peak null/empty → safeOnboarding → constant
     * ③ concentration < MIN → safeOnboarding → constant
     * ④ 정상: peak.centerMinutes (0..1439 clamp)
     */
    private suspend fun safeMealPeak(
        readyFlow : Flow<Boolean>,
        peakFlow  : Flow<PeakWindow?>,
        onboarding: suspend () -> Int,
        constant  : Int,
    ): Int {
        // ① 게이트 미통과
        val ready = safeFirst(readyFlow, false)
        if (!ready) return safeOnboarding(onboarding, constant)

        // ② JSON 파싱 실패 or 빈 값
        val peak = try { peakFlow.first() } catch (_: Exception) { null }
        if (peak == null || peak.isEmpty()) return safeOnboarding(onboarding, constant)

        // ③ 신뢰도 임계 미달 (불규칙 패턴)
        if (peak.concentration < DefaultValues.MIN_CONCENTRATION) return safeOnboarding(onboarding, constant)

        // ④ 정상: peak 중심 사용
        return peak.centerMinutes.coerceIn(0, 23 * 60 + 59)
    }

    private suspend fun safeOnboarding(block: suspend () -> Int, constant: Int): Int =
        try { block().coerceIn(0, 23 * 60 + 59) } catch (_: Exception) { constant }

    private suspend fun <T> safeFirst(flow: Flow<T>, default: T): T =
        try { flow.first() } catch (_: Exception) { default }

    private suspend fun wakeMinutes(): Int =
        prefs.getWakeTimeAsMinutes().first()

    private suspend fun bedMinutes(): Int =
        prefs.getBedTimeAsMinutes().first()
}
