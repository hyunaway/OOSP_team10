package com.example.habittracker.domain.usecase.water

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.model.WaterInterventionStatus
import com.example.habittracker.domain.repository.WaterRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckWaterInterventionNeededUseCase @Inject constructor(
    private val waterRepository: WaterRepository,
    private val userPreferenceManager: UserPreferenceManager,
    private val waterStatusCalculator: WaterStatusCalculator,
) {

    suspend operator fun invoke(
        nowMillis: Long = System.currentTimeMillis(),
    ): WaterInterventionStatus {
        val status = waterRepository.getTodayStatus().first()
        val wakeMinutes = userPreferenceManager.getWakeTimeAsMinutes().first()
        val bedMinutes = userPreferenceManager.getBedTimeAsMinutes().first()

        return waterStatusCalculator.calculate(
            wakeMinutes = wakeMinutes,
            bedMinutes = bedMinutes,
            goalMl = status.goalMl,
            currentAmountMl = status.totalMl,
            lastDrankAt = status.lastDrankAt,
            nowMillis = nowMillis,
        )
    }
}
