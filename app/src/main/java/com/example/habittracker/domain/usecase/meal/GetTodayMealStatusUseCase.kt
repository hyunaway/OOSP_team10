// 경로: com/example/habittracker/domain/usecase/meal/GetTodayMealStatusUseCase.kt
package com.example.habittracker.domain.usecase.meal

import com.example.habittracker.domain.model.MealTodayStatus
import com.example.habittracker.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetTodayMealStatusUseCase @Inject constructor(
    private val mealRepository: MealRepository,
) {
    operator fun invoke(): Flow<MealTodayStatus> = mealRepository.getTodayStatus()
}
