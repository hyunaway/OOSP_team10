// 경로: com/example/habittracker/domain/usecase/meal/AnalyzeMealPatternUseCase.kt
package com.example.habittracker.domain.usecase.meal

import com.example.habittracker.domain.model.MealPatternResult
import com.example.habittracker.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyzeMealPatternUseCase @Inject constructor(
    private val mealRepository: MealRepository,
) {
    operator fun invoke(): Flow<MealPatternResult> = mealRepository.getPatternAnalysis()
}
