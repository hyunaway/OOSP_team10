package com.example.habittracker.domain.analysis

/**
 * 개인화 분석 결과가 없을 때 사용하는 순수 상수 기본값.
 *
 * 역할:
 *  - 기존 하드코딩 상수를 한 곳에 모아 관리
 *  - PersonalizationResolver의 3단계 fallback 중 최후 안전망
 *  - 기존 사용처(Worker/UseCase)가 이 값을 참조하면 기존 동작과 100% 동일
 */
object DefaultValues {

    // ─── 식사 알림 시각 (분, 0..1439) ─────────────────────────────────────────
    const val BREAKFAST_PEAK_MINUTES   = 7 * 60 + 30   // 07:30
    const val LUNCH_PEAK_MINUTES       = 12 * 60 + 0    // 12:00
    const val DINNER_PEAK_MINUTES      = 18 * 60 + 0    // 18:00
    const val LATE_NIGHT_PEAK_MINUTES  = 22 * 60 + 0    // 22:00

    // ─── 식사 알림 창 폭 ──────────────────────────────────────────────────────
    /** 1차 알림 창: [peak, peak + WINDOW] 범위 (15분 = Worker 주기와 일치) */
    const val MEAL_NOTIFICATION_WINDOW_MINUTES = 15
    /** 2차(재알림) = 1차 기준점으로부터의 오프셋 */
    const val BREAKFAST_REMINDER2_OFFSET_MINUTES = 90  // 07:30 + 90 = 09:00
    const val LUNCH_REMINDER2_OFFSET_MINUTES     = 60  // 12:00 + 60 = 13:00
    const val DINNER_REMINDER2_OFFSET_MINUTES    = 60  // 18:00 + 60 = 19:00

    // ─── 결식 경고 / 야식 칭찬 (기준점과 무관한 고정 시각) ────────────────────
    const val LATE_NIGHT_PRAISE_MINUTES   = 8  * 60 + 0   // 08:00
    const val SKIP_BREAKFAST_MINUTES      = 12 * 60 + 15  // 12:15
    const val LUNCH_WARN_MINUTES          = 16 * 60 + 0   // 16:00
    const val SKIP_LUNCH_MINUTES          = 16 * 60 + 15  // 16:15
    const val SKIP_DINNER_MINUTES         = 22 * 60 + 0   // 22:00

    // ─── 온보딩 기반 오프셋 (기상/취침 시각 기준으로 산출하는 2단계 fallback) ──
    /** 아침 알림 = wakeTime + 이 값 (분) */
    const val BREAKFAST_OFFSET_FROM_WAKE_MINUTES = 60
    /** 저녁 알림 = bedTime - 이 값 (분) */
    const val DINNER_OFFSET_BEFORE_BED_MINUTES   = 180

    // ─── 스트레칭 ─────────────────────────────────────────────────────────────
    const val STRETCH_GOAL_COUNT                   = 4
    const val STRETCH_INTERVAL_MINUTES             = 90
    const val STRETCH_FIRST_REMINDER_DELAY_MINUTES = 15
    const val STRETCH_REMINDER_COOLDOWN_MINUTES    = 90

    // ─── 물 ───────────────────────────────────────────────────────────────────
    const val WATER_REMINDER_INTERVAL_MINUTES = 180
    const val WATER_COOLDOWN_MINUTES          = 60

    // ─── 디지털 ───────────────────────────────────────────────────────────────
    const val DIGITAL_THRESHOLD_MINUTES  = 30
    const val DIGITAL_COOLDOWN_MINUTES   = 60
    const val DIGITAL_BASE_DURATION      = 30

    // ─── 신뢰도 임계 (concentration < 이 값이면 peak 불신, fallback 사용) ────
    const val MIN_CONCENTRATION = 0.3f
}
