// 경로: com/example/habittracker/domain/usecase/personalization/UpdatePersonalizationParamsUseCase.kt
package com.example.habittracker.domain.usecase.personalization

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.usecase.digital.AnalyzeDigitalPatternUseCase
import com.example.habittracker.domain.usecase.meal.AnalyzeMealPatternUseCase
import com.example.habittracker.domain.usecase.stretch.AnalyzeStretchPatternUseCase
import com.example.habittracker.domain.usecase.water.AnalyzeWaterPatternUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdatePersonalizationParamsUseCase @Inject constructor(
    private val analyzeWaterPatternUseCase: AnalyzeWaterPatternUseCase,
    private val analyzeMealPatternUseCase: AnalyzeMealPatternUseCase,
    private val analyzeDigitalPatternUseCase: AnalyzeDigitalPatternUseCase,
    private val analyzeStretchPatternUseCase: AnalyzeStretchPatternUseCase,
    private val userPreferenceManager: UserPreferenceManager,
) {
    suspend operator fun invoke() {
        val waterPattern = analyzeWaterPatternUseCase().first()
        val mealPattern = analyzeMealPatternUseCase().first()
        val digitalPattern = analyzeDigitalPatternUseCase().first()
        val stretchPattern = analyzeStretchPatternUseCase().first()

        // Water peak hours (comma-separated)
        userPreferenceManager.updateWeekdayWaterPeakHours(
            waterPattern.peakHours.joinToString(",")
        )
        userPreferenceManager.updateWeekendWaterPeakHours(
            waterPattern.weekendPattern.keys.toList().joinToString(",")
        )

        // Digital: preferred tone and intervention duration
        val preferredTone = digitalPattern.toneReactionRate.maxByOrNull { it.value }?.key
        if (preferredTone != null) {
            userPreferenceManager.updatePreferredMessageTone(preferredTone)
        }
        val avgReactionRate = digitalPattern.toneReactionRate.values
            .average().toFloat().takeIf { it.isFinite() }
        if (avgReactionRate != null) {
            userPreferenceManager.updateDigitalInterventionBaseDuration(
                if (avgReactionRate < 0.3f) 45 else 30
            )
        }

        // Stretch inactive hours (comma-separated)
        userPreferenceManager.updateStretchInactiveHours(
            stretchPattern.inactiveHours.joinToString(",")
        )

        // Meal time maps (key:value comma-separated)
        userPreferenceManager.updateWeekdayMealTimeMap(
            mealPattern.weekdayMealTimeMap.entries.joinToString(",") { "${it.key}:${it.value}" }
        )
        userPreferenceManager.updateWeekendMealTimeMap(
            mealPattern.weekendMealTimeMap.entries.joinToString(",") { "${it.key}:${it.value}" }
        )
    }
}
