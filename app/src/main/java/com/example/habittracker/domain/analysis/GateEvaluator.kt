package com.example.habittracker.domain.analysis

// ─────────────────────────────────────────────────────────────────────────────
// 게이트 입력 / 임계값
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 신뢰도 게이트 평가에 필요한 관찰 지표.
 *
 * @param daysObserved     분석 기간 내 실제 기록이 있었던 날 수
 * @param volume           분석 기간 내 총 로그 건수
 * @param hasRecentRecord  최근 3일 이내 로그가 1건 이상 있는지 (활동 지속 여부)
 */
data class GateInput(
    val daysObserved: Int,
    val volume: Int,
    val hasRecentRecord: Boolean,
)

/**
 * 카테고리별 신뢰도 게이트 임계값.
 *
 * @param minDays   최소 관찰 일수 (데이터가 충분히 쌓였는가)
 * @param minVolume 최소 로그 건수 (통계적 유의성)
 */
data class GateThreshold(
    val minDays: Int,
    val minVolume: Int,
)

// ─────────────────────────────────────────────────────────────────────────────
// 게이트 임계값 상수
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 카테고리별 기본 신뢰도 게이트 임계값.
 *
 * DIGITAL은 앱별 필터링이 추가로 필요하므로 여기서 정의하지 않고
 * UseCase(STEP 4)에서 조합한다.
 */
object GateThresholds {
    /** 물: 7일 이상 관찰, 20건 이상 (하루 평균 3잔) */
    val WATER   = GateThreshold(minDays = 7, minVolume = 20)
    /** 식사: 7일 이상 관찰, 5건 이상 (하루 1끼 이상) */
    val MEAL    = GateThreshold(minDays = 7, minVolume = 5)
    /** 스트레칭: 7일 이상 관찰, 10건 이상 (하루 평균 1.4회) */
    val STRETCH = GateThreshold(minDays = 7, minVolume = 10)
}

// ─────────────────────────────────────────────────────────────────────────────
// 게이트 평가 함수
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 신뢰도 게이트 평가기.
 * 3가지 조건을 AND로 결합해 개인화 적용 여부를 결정한다.
 *
 * 통과 조건:
 *  1. input.daysObserved >= threshold.minDays   (관찰 기간 충분)
 *  2. input.volume       >= threshold.minVolume  (누적 로그 충분)
 *  3. input.hasRecentRecord == true              (최근 3일 내 활동 있음)
 *
 * 순수 함수 — 외부 상태 접근 없음.
 */
object GateEvaluator {

    fun evaluateGate(input: GateInput, threshold: GateThreshold): Boolean =
        input.daysObserved   >= threshold.minDays   &&
        input.volume         >= threshold.minVolume &&
        input.hasRecentRecord
}
