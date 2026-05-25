// 경로: com/example/habittracker/domain/usecase/dashboard/GetDashboardStateUseCase.kt
package com.example.habittracker.domain.usecase.dashboard

import com.example.habittracker.domain.model.DashboardState
import com.example.habittracker.domain.model.DigitalTodayStatus
import com.example.habittracker.domain.model.MealTodayStatus
import com.example.habittracker.domain.model.StretchTodayStatus
import com.example.habittracker.domain.model.WaterTodayStatus
import com.example.habittracker.domain.repository.DigitalRepository
import com.example.habittracker.domain.repository.MealRepository
import com.example.habittracker.domain.repository.StretchRepository
import com.example.habittracker.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetDashboardStateUseCase @Inject constructor(
    private val waterRepository: WaterRepository,
    private val mealRepository: MealRepository,
    private val stretchRepository: StretchRepository,
    private val digitalRepository: DigitalRepository,
) {
    operator fun invoke(): Flow<DashboardState> =
        combine(
            waterRepository.getTodayStatus(),
            mealRepository.getTodayStatus(),
            stretchRepository.getTodayStatus(),
            digitalRepository.getTodayStatus(),
        ) { water, meal, stretch, digital ->
            val overallScore = calculateOverallScore(water, meal, stretch, digital)
            DashboardState(
                waterStatus = water,
                mealStatus = meal,
                stretchStatus = stretch,
                digitalStatus = digital,
                overallScore = overallScore,
                motivationalMessage = generateMotivationalMessage(overallScore),
            )
        }

    private fun calculateOverallScore(
        water: WaterTodayStatus,
        meal: MealTodayStatus,
        stretch: StretchTodayStatus,
        digital: DigitalTodayStatus,
    ): Float {
        val waterScore = water.achievementRate.coerceIn(0f, 1f)
        val mealScore = listOf(meal.breakfastLogged, meal.lunchLogged, meal.dinnerLogged)
            .count { it } / 3f
        val stretchScore = stretch.avatarHealthScore.coerceIn(0f, 1f)
        val digitalScore = if (digital.interventionCount == 0) 1f
            else (digital.reactedCount.toFloat() / digital.interventionCount).coerceIn(0f, 1f)
        return (waterScore + mealScore + stretchScore + digitalScore) / 4f
    }

    private fun generateMotivationalMessage(score: Float): String = when {
        score >= 0.8f -> "훌륭해요! 오늘 습관을 잘 지키고 있어요."
        score >= 0.5f -> "잘 하고 있어요. 조금만 더 힘내봐요!"
        else -> "오늘 습관 관리를 시작해볼까요?"
    }
}
