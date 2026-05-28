// 경로: com/example/habittracker/domain/usecase/meal/GetMealHistoryUseCase.kt
package com.example.habittracker.domain.usecase.meal

import com.example.habittracker.domain.model.DailyMealSummary
import com.example.habittracker.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetMealHistoryUseCase @Inject constructor(
    private val mealRepository: MealRepository,
) {
    operator fun invoke(startDate: String, endDate: String): Flow<List<DailyMealSummary>> =
        mealRepository.getLogsBetween(startDate, endDate)
}
