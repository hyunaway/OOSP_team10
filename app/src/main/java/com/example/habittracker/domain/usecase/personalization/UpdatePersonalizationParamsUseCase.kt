package com.example.habittracker.domain.usecase.personalization

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.usecase.digital.AnalyzeDigitalPatternUseCase
import com.example.habittracker.domain.usecase.meal.AnalyzeMealPatternUseCase
import com.example.habittracker.domain.usecase.stretch.AnalyzeStretchPatternUseCase
import com.example.habittracker.domain.usecase.water.AnalyzeWaterPatternUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 4개 카테고리 분석 UseCase를 순차 실행하는 오케스트레이터.
 *
 * 각 UseCase가 [Room 읽기 → 엔진 계산 → 게이트 → DataStore 저장]을 자체 처리하므로
 * 이 클래스는 순서만 관리한다.
 *
 * 추가로:
 *  - 오늘 활동 일수(totalActiveDays) 증가
 *  - personalizationLevel 산출 (totalActiveDays 기반 5단계)
 *  - "첫 게이트 통과" 여부 반환 → Worker가 즉시 재실행 여부 결정
 */
@Singleton
class UpdatePersonalizationParamsUseCase @Inject constructor(
    private val analyzeWaterPatternUseCase: AnalyzeWaterPatternUseCase,
    private val analyzeMealPatternUseCase: AnalyzeMealPatternUseCase,
    private val analyzeDigitalPatternUseCase: AnalyzeDigitalPatternUseCase,
    private val analyzeStretchPatternUseCase: AnalyzeStretchPatternUseCase,
    private val userPreferenceManager: UserPreferenceManager,
) {
    /**
     * @return true = 이번 실행에서 최초로 게이트가 통과됨 (신규 사용자 첫 개인화 시점)
     */
    suspend operator fun invoke(): Boolean {
        // 실행 전 게이트 상태 스냅샷 (첫 통과 감지용)
        val wasAnyReady = anyGateReady()

        // 4개 카테고리 순차 분석 (각자 게이트 판정 후 DataStore 갱신)
        analyzeWaterPatternUseCase()
        analyzeMealPatternUseCase()
        analyzeStretchPatternUseCase()
        analyzeDigitalPatternUseCase()

        // 총 활동 일수 + 1
        val prevDays = userPreferenceManager.totalActiveDaysFlow.first()
        val newDays  = prevDays + 1
        userPreferenceManager.updateTotalActiveDays(newDays)

        // 성숙도 레벨 산출 (0~5)
        val level = when {
            newDays >= 90 -> 5
            newDays >= 60 -> 4
            newDays >= 30 -> 3
            newDays >= 14 -> 2
            newDays >= 7  -> 1
            else          -> 0
        }
        userPreferenceManager.updatePersonalizationLevel(level)

        // 첫 게이트 통과 여부 반환
        val isAnyReady = anyGateReady()
        return !wasAnyReady && isAnyReady
    }

    private suspend fun anyGateReady(): Boolean =
        userPreferenceManager.waterPersonalizationReadyFlow.first()    ||
        userPreferenceManager.mealPersonalizationReadyFlow.first()     ||
        userPreferenceManager.stretchPersonalizationReadyFlow.first()  ||
        userPreferenceManager.digitalPersonalizationReadyFlow.first()
}
