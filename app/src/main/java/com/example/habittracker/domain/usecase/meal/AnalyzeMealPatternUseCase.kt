package com.example.habittracker.domain.usecase.meal

import com.example.habittracker.data.entity.MealLogEntity
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.local.room.dao.MealDao
import com.example.habittracker.data.model.MealType
import com.example.habittracker.domain.analysis.AnalysisWindow
import com.example.habittracker.domain.analysis.GateEvaluator
import com.example.habittracker.domain.analysis.GateInput
import com.example.habittracker.domain.analysis.GateThresholds
import com.example.habittracker.domain.analysis.PersonalizationEngine
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [파이프라인] meal_logs 30일 → 타입별 timestamps 추출 → calcPeakWindow × 4,
 *            viaDeliveryApp의 mealDate → calcRecurrenceInterval
 *            → 게이트(MEAL 7·5)
 *            → 통과 시 meal*PeakJson / deliveryIntervalDays / mealReady 갱신.
 */
@Singleton
class AnalyzeMealPatternUseCase @Inject constructor(
    private val mealDao: MealDao,
    private val userPreferenceManager: UserPreferenceManager,
) {
    suspend operator fun invoke() {
        val window = AnalysisWindow.recent(AnalysisWindow.DEFAULT_DAYS)
        val logs = mealDao.getLogsBetween(window.first, window.last).first()

        // 엔티티 → 원시 타입 추출 (타입별 timestamps)
        val breakfastTs  = logs.timestampsOf(MealType.BREAKFAST)
        val lunchTs      = logs.timestampsOf(MealType.LUNCH)
        val dinnerTs     = logs.timestampsOf(MealType.DINNER)
        val lateNightTs  = logs.filter { it.isLateNight || it.type == MealType.LATE_NIGHT }
                               .map { it.timestamp }

        // 배달 앱 주문 날짜 목록 (isLateNight 무관, viaDeliveryApp 필터)
        val deliveryDates = logs.filter { it.viaDeliveryApp }.map { it.mealDate }.distinct()

        // 공용 엔진 계산
        val breakfastPeak  = PersonalizationEngine.calcPeakWindow(breakfastTs)
        val lunchPeak      = PersonalizationEngine.calcPeakWindow(lunchTs)
        val dinnerPeak     = PersonalizationEngine.calcPeakWindow(dinnerTs)
        val lateNightPeak  = PersonalizationEngine.calcPeakWindow(lateNightTs)
        val deliveryInterval = PersonalizationEngine.calcRecurrenceInterval(deliveryDates)

        // 게이트 입력 구성
        val distinctDays = logs.map { it.mealDate }.distinct().size
        val hasRecentRecord = logs.any { it.timestamp >= recentThreshold() }
        val gateInput = GateInput(
            daysObserved    = distinctDays,
            volume          = logs.size,
            hasRecentRecord = hasRecentRecord,
        )

        // 게이트 통과 시에만 DataStore 갱신
        if (GateEvaluator.evaluateGate(gateInput, GateThresholds.MEAL)) {
            userPreferenceManager.updateMealBreakfastPeak(breakfastPeak.takeUnless { it.isEmpty() })
            userPreferenceManager.updateMealLunchPeak(lunchPeak.takeUnless { it.isEmpty() })
            userPreferenceManager.updateMealDinnerPeak(dinnerPeak.takeUnless { it.isEmpty() })
            userPreferenceManager.updateMealLateNightPeak(lateNightPeak.takeUnless { it.isEmpty() })
            userPreferenceManager.updateDeliveryIntervalDays(deliveryInterval)
            userPreferenceManager.updateMealPersonalizationReady(true)
        }
    }

    private fun List<MealLogEntity>.timestampsOf(type: MealType): List<Long> =
        filter { it.type == type }.map { it.timestamp }

    private fun recentThreshold(): Long =
        System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000
}
