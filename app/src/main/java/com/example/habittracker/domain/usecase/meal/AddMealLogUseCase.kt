// 경로: com/example/habittracker/domain/usecase/meal/AddMealLogUseCase.kt
package com.example.habittracker.domain.usecase.meal

import com.example.habittracker.data.model.MealType
import com.example.habittracker.domain.repository.MealRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddMealLogUseCase @Inject constructor(
    private val mealRepository: MealRepository,
) {
    suspend operator fun invoke(
        type: MealType,
        isLateNight: Boolean,
        viaDeliveryApp: Boolean,
        source: String,
        timestamp: Long = System.currentTimeMillis(),
    ) {
        mealRepository.addLog(type, timestamp, isLateNight, viaDeliveryApp, source)
    }
}
